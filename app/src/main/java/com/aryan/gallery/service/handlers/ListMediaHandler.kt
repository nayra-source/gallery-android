package com.aryan.gallery.service.handlers

import android.content.Context
import com.aryan.gallery.data.media.MediaStoreDataSource
import com.aryan.gallery.service.DeviceCommandEnvelope
import com.aryan.gallery.service.DeviceCommandResultEnvelope
import com.aryan.gallery.service.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ListMediaPayload(
    val album: String? = null,
    val cursor: String? = null
)

@Serializable
data class ListMediaItemDto(
    val mediaId: Long,
    val displayName: String,
    val mimeType: String,
    val takenAt: String
)

@Serializable
data class ListMediaResultDto(
    val items: List<ListMediaItemDto>,
    val nextCursor: String?
)

/**
 * Handler for LIST_MEDIA commands from gallery-backend.
 */
@Singleton
class ListMediaHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: MediaStoreDataSource,
    private val json: Json
) : CommandHandler {

    override val supportedType: String = "LIST_MEDIA"

    override suspend fun handleCommand(command: DeviceCommandEnvelope): DeviceCommandResultEnvelope {
        return try {
            val payload = command.payload?.let {
                try { json.decodeFromString<ListMediaPayload>(it) } catch (e: Exception) { null }
            }

            val pageLimit = 50
            val currentOffset = payload?.cursor?.toIntOrNull()?.coerceAtLeast(0) ?: 0
            val albumBucketId = payload?.album

            val mediaItems = dataSource.getMediaItems(
                limit = pageLimit,
                offset = currentOffset,
                bucketId = albumBucketId
            )

            val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }

            val dtoList = mediaItems.map { item ->
                ListMediaItemDto(
                    mediaId = item.id,
                    displayName = item.displayName,
                    mimeType = item.mimeType,
                    takenAt = dateFormat.format(Date(item.dateAdded))
                )
            }

            val nextCursor = if (mediaItems.size >= pageLimit) {
                (currentOffset + mediaItems.size).toString()
            } else {
                null
            }

            val resultDto = ListMediaResultDto(items = dtoList, nextCursor = nextCursor)
            val resultJson = json.encodeToString(resultDto)

            NotificationHelper.showTransparencyNotification(
                context,
                title = "Dashboard Browsed Photos",
                text = "Dashboard requested photo list (${mediaItems.size} items)"
            )

            DeviceCommandResultEnvelope(
                cmdId = command.cmdId,
                status = "ok",
                result = resultJson
            )
        } catch (e: Exception) {
            DeviceCommandResultEnvelope(
                cmdId = command.cmdId,
                status = "error",
                message = "Failed to query media store: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
