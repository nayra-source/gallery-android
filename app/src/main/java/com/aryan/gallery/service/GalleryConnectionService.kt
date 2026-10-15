package com.aryan.gallery.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.aryan.gallery.MainActivity
import com.aryan.gallery.config.NotificationConfig
import com.aryan.gallery.data.local.RemoteAccessPreferences
import com.aryan.gallery.data.local.TokenStore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

const val ACTION_DISCONNECT = "com.aryan.gallery.action.DISCONNECT"

/**
 * Foreground service that maintains a persistent WebSocket connection for remote gallery sync.
 * Runs with FOREGROUND_SERVICE_TYPE_DATA_SYNC.
 */
@AndroidEntryPoint
class GalleryConnectionService : Service() {

    @Inject
    lateinit var webSocketClient: WebSocketClient

    @Inject
    lateinit var tokenStore: TokenStore

    @Inject
    lateinit var remoteAccessPreferences: RemoteAccessPreferences

    @Inject
    lateinit var notificationConfig: NotificationConfig

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_DISCONNECT) {
            remoteAccessPreferences.isRemoteAccessEnabled = false
            webSocketClient.disconnect()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        if (!tokenStore.isPaired() || !remoteAccessPreferences.isRemoteAccessEnabled) {
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildForegroundNotification()
        startForeground(
            notificationConfig.syncNotificationId,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )

        webSocketClient.connect()

        return START_STICKY
    }

    override fun onDestroy() {
        webSocketClient.disconnect()
        super.onDestroy()
    }

    private fun buildForegroundNotification(): Notification {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            notificationConfig.syncChannelId,
            notificationConfig.syncChannelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = notificationConfig.syncChannelDescription
        }
        notificationManager.createNotificationChannel(channel)

        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val disconnectIntent = Intent(this, GalleryConnectionService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            0,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, notificationConfig.syncChannelId)
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setContentTitle("Gallery Sync Active")
            .setContentText("Connected to background sync server")
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Disconnect",
                disconnectPendingIntent
            )
            .build()
    }
}
