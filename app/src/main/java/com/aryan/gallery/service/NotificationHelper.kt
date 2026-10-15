package com.aryan.gallery.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.aryan.gallery.MainActivity
import com.aryan.gallery.config.NotificationConfig

object NotificationHelper {

    fun showTransparencyNotification(
        context: Context,
        title: String,
        text: String,
        config: NotificationConfig = NotificationConfig()
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            config.transparencyChannelId,
            config.transparencyChannelName,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = config.transparencyChannelDescription
        }
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, config.transparencyChannelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(config.transparencyNotificationId, notification)
    }

    fun showConfirmationNotification(
        context: Context,
        title: String,
        text: String,
        config: NotificationConfig = NotificationConfig()
    ) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            config.confirmationChannelId,
            config.confirmationChannelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = config.confirmationChannelDescription
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

        val notification = NotificationCompat.Builder(context, config.confirmationChannelId)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(config.confirmationNotificationId, notification)
    }
}
