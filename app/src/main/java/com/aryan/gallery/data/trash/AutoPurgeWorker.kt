package com.aryan.gallery.data.trash

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aryan.gallery.MainActivity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

const val ACTION_CONFIRM_PURGE = "com.aryan.gallery.action.CONFIRM_PURGE"
private const val CHANNEL_ID = "auto_purge_channel"
private const val NOTIFICATION_ID = 1001

/**
 * Worker that periodically checks for trashed items older than 30 days.
 *
 * Android OS Design Constraint Note:
 * MediaStore.createDeleteRequest requires user confirmation via a system intent sender dialog,
 * so background code cannot silently delete files without user interaction.
 * Therefore, this worker posts a notification directing the user to confirm permanent deletion in-app.
 */
@HiltWorker
class AutoPurgeWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val trashRepository: TrashRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val expiredItems = trashRepository.getExpiredTrashItems()
        if (expiredItems.isNotEmpty()) {
            sendAutoPurgeNotification(expiredItems.size)
        }
        return Result.success()
    }

    private fun sendAutoPurgeNotification(count: Int) {
        val hasNotificationPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasNotificationPermission) {
            // Notification permission denied or not yet granted; silently skip notify().
            // The in-app banner on TrashScreen remains the fallback prompt.
            return
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Auto-Purge Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications for media items ready to be auto-purged from Trash"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            action = ACTION_CONFIRM_PURGE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_delete)
            .setContentTitle("Trash Auto-Purge")
            .setContentText("$count item(s) in Trash are older than 30 days. Tap to confirm permanent deletion.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$count item(s) in Trash have exceeded the 30-day retention limit and are ready for permanent deletion. Tap to open Gallery and confirm.")
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
