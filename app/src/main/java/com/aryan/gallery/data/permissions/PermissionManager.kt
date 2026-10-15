package com.aryan.gallery.data.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.aryan.gallery.data.local.RemoteAccessPreferences
import com.aryan.gallery.data.local.TokenStore
import com.aryan.gallery.service.GalleryConnectionService
import com.aryan.gallery.ui.permissions.PermissionPreferences
import com.aryan.gallery.ui.permissions.PermissionState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SelfHeal"
private const val MAX_LOG_ENTRIES = 20

/**
 * Single source of truth for runtime permissions, capability states,
 * structured SelfHeal logging, and single idempotent recoverIfNeeded() execution.
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenStore: TokenStore,
    private val remoteAccessPreferences: RemoteAccessPreferences,
    private val permissionPreferences: PermissionPreferences
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val recoveryMutex = Mutex()

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val selfHealLogs = mutableListOf<String>()

    init {
        updatePermissionState()
    }

    /**
     * Evaluates current media permission state for the OS version.
     */
    fun checkMediaPermissionState(): PermissionState {
        val hasRequested = permissionPreferences.hasRequestedPermissions

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideos = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
            val hasSelected = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED

            when {
                hasImages && hasVideos -> PermissionState.Granted
                hasSelected -> PermissionState.PartiallyGranted
                !hasRequested -> PermissionState.NotRequested
                else -> PermissionState.Denied
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
            val hasVideos = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED

            when {
                hasImages && hasVideos -> PermissionState.Granted
                !hasRequested -> PermissionState.NotRequested
                else -> PermissionState.Denied
            }
        } else {
            val hasStorage = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            when {
                hasStorage -> PermissionState.Granted
                !hasRequested -> PermissionState.NotRequested
                else -> PermissionState.Denied
            }
        }
    }

    fun updatePermissionState(): PermissionState {
        val state = checkMediaPermissionState()
        _permissionState.value = state
        return state
    }

    /**
     * Returns true if media permissions are either Granted or Partially Granted.
     */
    fun isMediaAccessGranted(): Boolean {
        val state = checkMediaPermissionState()
        return state is PermissionState.Granted || state is PermissionState.PartiallyGranted
    }

    /**
     * Checks if POST_NOTIFICATIONS permission is granted (always true on API < 33).
     */
    fun isNotificationPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Single idempotent entry point used by all watchdog triggers
     * (Application.onCreate, Activity.onResume, BootReceiver, WorkManager).
     */
    suspend fun recoverIfNeeded(triggerReason: String) = recoveryMutex.withLock {
        logSelfHeal("Trigger: $triggerReason | Evaluating self-healing state...")
        val currentState = updatePermissionState()
        val isPaired = tokenStore.isPaired()
        val isEnabled = remoteAccessPreferences.isRemoteAccessEnabled
        val isGranted = isMediaAccessGranted()

        logSelfHeal("State -> Paired: $isPaired | Enabled: $isEnabled | Granted: $isGranted ($currentState)")

        if (!isPaired || !isEnabled) {
            logSelfHeal("Self-Heal Skipped: Device not paired or sync disabled.")
            return@withLock
        }

        if (!isGranted) {
            logSelfHeal("Self-Heal Paused: WAITING_FOR_PERMISSION. Stopping FGS gracefully.")
            runCatching {
                val serviceIntent = Intent(context, GalleryConnectionService::class.java)
                context.stopService(serviceIntent)
            }
            return@withLock
        }

        // Start Foreground Service safely
        try {
            logSelfHeal("Self-Heal Executing: Permissions OK. Starting GalleryConnectionService...")
            val serviceIntent = Intent(context, GalleryConnectionService::class.java)
            context.startForegroundService(serviceIntent)
            logSelfHeal("Self-Heal Success: GalleryConnectionService started via $triggerReason")
        } catch (e: Exception) {
            logSelfHeal("Self-Heal FGS Start Exception (${e.javaClass.simpleName}): ${e.localizedMessage}")
        }
    }

    /**
     * Logs structured SelfHeal events and keeps a 20-entry ring buffer.
     */
    @Synchronized
    fun logSelfHeal(message: String) {
        val time = System.currentTimeMillis()
        val entry = "[$time] $message"
        Log.d(TAG, message)
        selfHealLogs.add(entry)
        if (selfHealLogs.size > MAX_LOG_ENTRIES) {
            selfHealLogs.removeAt(0)
        }
    }

    @Synchronized
    fun getSelfHealLogs(): List<String> = selfHealLogs.toList()
}
