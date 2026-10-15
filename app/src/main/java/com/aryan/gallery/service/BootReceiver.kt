package com.aryan.gallery.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aryan.gallery.data.permissions.PermissionManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver triggering PermissionManager.recoverIfNeeded when the device boots or package is updated.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var permissionManager: PermissionManager

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    permissionManager.recoverIfNeeded("BootReceiver.$action")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
