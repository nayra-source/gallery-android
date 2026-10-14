package com.aryan.gallery.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the device's JWT (obtained once at pairing time, see Phase 6) behind
 * Android Keystore-backed encryption. Never store this token in plain
 * SharedPreferences, a Room column, or anywhere it could be read by pulling
 * the app's files off a rooted/unlocked device.
 *
 * File name "secure_prefs.xml" is referenced explicitly in
 * res/xml/data_extraction_rules.xml to exclude it from backup/transfer -
 * keep those two in sync if you ever rename it.
 */
@Singleton
class TokenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveDeviceToken(token: String, deviceId: String) {
        prefs.edit()
            .putString(KEY_DEVICE_TOKEN, token)
            .putString(KEY_DEVICE_ID, deviceId)
            .apply()
    }

    fun getDeviceToken(): String? = prefs.getString(KEY_DEVICE_TOKEN, null)

    fun getDeviceId(): String? = prefs.getString(KEY_DEVICE_ID, null)

    fun isPaired(): Boolean = getDeviceToken() != null

    /** Called on 401/revoked-device responses, or when the user manually unpairs. */
    fun clear() {
        prefs.edit().clear().apply()
    }

    private companion object {
        const val KEY_DEVICE_TOKEN = "device_token"
        const val KEY_DEVICE_ID = "device_id"
    }
}
