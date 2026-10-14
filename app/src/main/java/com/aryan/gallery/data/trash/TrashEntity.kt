package com.aryan.gallery.data.trash

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Default auto-purge threshold: 30 days in milliseconds.
 */
const val DEFAULT_PURGE_DELAY_MS: Long = 30L * 24L * 60L * 60L * 1000L

/**
 * Room entity tracking local metadata for items moved to Trash.
 */
@Entity(tableName = "trash_metadata")
data class TrashEntity(
    @PrimaryKey val mediaId: Long,
    val displayName: String,
    val uriString: String,
    val mediaType: String,
    val durationMs: Long = 0L,
    val trashedAt: Long = System.currentTimeMillis(),
    val purgeAt: Long = System.currentTimeMillis() + DEFAULT_PURGE_DELAY_MS
)
