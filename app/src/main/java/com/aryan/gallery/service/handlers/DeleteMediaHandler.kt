package com.aryan.gallery.service.handlers

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.aryan.gallery.data.media.MediaStoreDataSource
import com.aryan.gallery.data.media.MediaStoreObserver
import com.aryan.gallery.data.trash.TrashRepository
import com.aryan.gallery.service.DeviceCommandEnvelope
import com.aryan.gallery.service.DeviceCommandResultEnvelope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DeleteMediaPayload(
    val mediaIds: List<Long>
)

@Serializable
data class DeleteMediaResultDto(
    val status: String = "ok",
    val trashed: List<Long> = emptyList(),
    val failed: List<Long> = emptyList(),
    val requestedIds: List<Long>
)

/**
 * Handler for DELETE_MEDIA commands from gallery-backend.
 * Directly trashes/deletes the requested media items from MediaStore without prompting in-app user confirmation dialogs.
 */
@Singleton
class DeleteMediaHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val trashRepository: TrashRepository,
    private val mediaStoreObserver: MediaStoreObserver,
    private val json: Json
) : CommandHandler {

    override val supportedType: String = "DELETE_MEDIA"

    override suspend fun handleCommand(command: DeviceCommandEnvelope): DeviceCommandResultEnvelope = withContext(Dispatchers.IO) {
        return@withContext try {
            val payload = command.payload?.let {
                try { json.decodeFromString<DeleteMediaPayload>(it) } catch (e: Exception) { null }
            } ?: throw IllegalArgumentException("Missing or invalid DELETE_MEDIA payload")

            val mediaIds = payload.mediaIds
            if (mediaIds.isEmpty()) {
                throw IllegalArgumentException("mediaIds list is empty")
            }

            val trashedIds = mutableListOf<Long>()
            val failedIds = mutableListOf<Long>()

            for (mediaId in mediaIds) {
                try {
                    val mediaItem = mediaStoreDataSource.getMediaItemById(mediaId)
                    if (mediaItem != null) {
                        trashRepository.recordTrashedItems(listOf(mediaItem))
                    }

                    val imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)
                    val targetUri = mediaItem?.uri ?: imageUri

                    var deletedRows = try {
                        context.contentResolver.delete(targetUri, null, null)
                    } catch (e: Exception) {
                        0
                    }

                    if (deletedRows == 0) {
                        val videoUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaId)
                        deletedRows = try {
                            context.contentResolver.delete(videoUri, null, null)
                        } catch (e: Exception) {
                            0
                        }
                    }

                    if (deletedRows > 0 || mediaItem != null) {
                        trashedIds.add(mediaId)
                        context.contentResolver.notifyChange(targetUri, null)
                    } else {
                        failedIds.add(mediaId)
                    }
                } catch (e: Exception) {
                    failedIds.add(mediaId)
                }
            }

            mediaStoreObserver.notifyMediaChanged()

            val resultDto = DeleteMediaResultDto(
                status = "ok",
                trashed = trashedIds,
                failed = failedIds,
                requestedIds = mediaIds
            )

            DeviceCommandResultEnvelope(
                cmdId = command.cmdId,
                status = "ok",
                result = json.encodeToString(resultDto)
            )
        } catch (e: Exception) {
            DeviceCommandResultEnvelope(
                cmdId = command.cmdId,
                status = "error",
                message = "Failed to execute direct media deletion: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
