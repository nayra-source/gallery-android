package com.aryan.gallery.data.media

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "media_overrides")
data class MediaOverrideEntity(
    @PrimaryKey val mediaId: Long,
    val customDisplayName: String,
    val updatedAt: Long = System.currentTimeMillis()
)
