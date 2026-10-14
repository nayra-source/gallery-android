package com.aryan.gallery.data.media

import android.net.Uri

/**
 * Model representing an album/bucket grouped by MediaStore BUCKET_ID.
 */
data class Album(
    val bucketId: String,
    val bucketDisplayName: String,
    val coverUri: Uri,
    val itemCount: Int
)
