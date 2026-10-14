package com.aryan.gallery.data.media

import android.net.Uri

enum class MediaType {
    IMAGE,
    VIDEO
}

/**
 * Shared model representing a photo or video retrieved from MediaStore.
 */
data class MediaItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAdded: Long,
    val mimeType: String,
    val size: Long,
    val width: Int,
    val height: Int,
    val bucketId: String,
    val bucketDisplayName: String,
    val durationMs: Long = 0L,
    val mediaType: MediaType
)
