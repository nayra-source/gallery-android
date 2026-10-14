package com.aryan.gallery.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.aryan.gallery.MainActivity

private const val TRANSPARENCY_CHANNEL_ID = "remote_activity_channel"
private const val CONFIRMATION_CHANNEL_ID = "remote_confirmation_channel"
private const val TRANSPARENCY_NOTIFICATION_ID = 3001
private const val CONFIRMATION_NOTIFICATION_ID = 3002

object NotificationHelper {

    fun showTransparencyNotification(context: Context, title: String, text: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            TRANSPARENCY_CHANNEL_ID,
            "Remote Dashboard Activity",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Informs device owner when dashboard remotely accesses photos or performs commands"
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, TRANSPARENCY_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(TRANSPARENCY_NOTIFICATION_ID, notification)
    }

    fun showConfirmationNotification(context: Context, title: String, text: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CONFIRMATION_CHANNEL_ID,
            "Remote Action Confirmations",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Prompts device owner to confirm remote delete or rename operations"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CONFIRMATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(CONFIRMATION_NOTIFICATION_ID, notification)
    }
}
