package com.aryan.gallery.data.remote

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.aryan.gallery.data.local.TokenStore
import com.aryan.gallery.data.remote.models.DeviceRegisterRequest
import com.aryan.gallery.data.remote.models.DeviceRegisterResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository performing zero-touch auto-registration of the device on application launch.
 */
@Singleton
class DeviceAutoRegisterRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pairingApi: PairingApi,
    private val tokenStore: TokenStore
) {
    private val registrationMutex = Mutex()

    /**
     * Obtains hardware ANDROID_ID or model name and registers with backend.
     * Thread-safe via Mutex; includes automatic retry attempts for resilient fast auto-connect.
     * On success, saves deviceToken and deviceId in TokenStore.
     */
    suspend fun autoRegisterIfNeeded(): Result<DeviceRegisterResponse> = registrationMutex.withLock {
        withContext(Dispatchers.IO) {
            if (tokenStore.isPaired()) {
                return@withContext Result.failure(IllegalStateException("Device is already registered/paired."))
            }

        val hardwareId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: Build.FINGERPRINT ?: "android_${Build.MODEL}"
        val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
        val osVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"

        val maxAttempts = 5
        var lastException: Throwable? = null

        for (attempt in 1..maxAttempts) {
            try {
                val response = pairingApi.registerDevice(
                    DeviceRegisterRequest(
                        hardwareId = hardwareId,
                        deviceName = deviceName,
                        osVersion = osVersion
                    )
                )

                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    tokenStore.saveDeviceToken(body.deviceToken, body.deviceId)
                    return@withContext Result.success(body)
                } else {
                    lastException = Exception("Registration failed with HTTP ${response.code()}")
                }
            } catch (e: Exception) {
                lastException = e
            }

            if (attempt < maxAttempts) {
                delay(2000L)
            }
        }

        Result.failure(lastException ?: Exception("Device registration failed after $maxAttempts attempts"))
    }
}
}
