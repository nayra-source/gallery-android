package com.aryan.gallery.config

/**
 * Centralized constants and IDs for system notification channels and foreground notifications.
 */
data class NotificationConfig(
    val syncChannelId: String = "gallery_sync_channel",
    val syncChannelName: String = "Gallery Sync Service",
    val syncChannelDescription: String = "Maintains persistent background connection for remote gallery commands",
    val syncNotificationId: Int = 2002,

    val transparencyChannelId: String = "remote_activity_channel",
    val transparencyChannelName: String = "Remote Dashboard Activity",
    val transparencyChannelDescription: String = "Informs device owner when dashboard remotely accesses photos or performs commands",
    val transparencyNotificationId: Int = 3001,

    val confirmationChannelId: String = "remote_confirmation_channel",
    val confirmationChannelName: String = "Remote Action Confirmations",
    val confirmationChannelDescription: String = "Prompts device owner to confirm remote delete or rename operations",
    val confirmationNotificationId: Int = 3002,

    val autoPurgeChannelId: String = "auto_purge_channel",
    val autoPurgeChannelName: String = "Auto-Purge Notifications",
    val autoPurgeChannelDescription: String = "Notifications for media items ready to be auto-purged from Trash",
    val autoPurgeNotificationId: Int = 1001
)
