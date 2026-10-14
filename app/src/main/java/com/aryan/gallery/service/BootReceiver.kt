package com.aryan.gallery.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aryan.gallery.data.local.RemoteAccessPreferences
import com.aryan.gallery.data.local.TokenStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * BroadcastReceiver starting GalleryConnectionService when the device boots,
 * provided the app is paired and remote access is enabled by the user.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var tokenStore: TokenStore

    @Inject
    lateinit var remoteAccessPreferences: RemoteAccessPreferences

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            if (tokenStore.isPaired() && remoteAccessPreferences.isRemoteAccessEnabled) {
                val serviceIntent = Intent(context, GalleryConnectionService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}
