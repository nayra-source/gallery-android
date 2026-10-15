package com.aryan.gallery.service

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aryan.gallery.data.local.RemoteAccessPreferences
import com.aryan.gallery.data.local.TokenStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

private const val TAG = "KeepAliveWorker"

/**
 * WorkManager periodic safety-net worker that ensures GalleryConnectionService stays active
 * even if killed by the OS under low memory or prolonged Doze mode.
 */
@HiltWorker
class ConnectionKeepAliveWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val permissionManager: com.aryan.gallery.data.permissions.PermissionManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        Log.d(TAG, "Submarine Keep-Alive Worker: Triggering permissionManager.recoverIfNeeded...")
        permissionManager.recoverIfNeeded("ConnectionKeepAliveWorker")
        return Result.success()
    }
}
