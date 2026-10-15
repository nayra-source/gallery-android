package com.aryan.gallery.service.handlers

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import com.aryan.gallery.data.media.MediaOverrideDao
import com.aryan.gallery.data.media.MediaOverrideEntity
import com.aryan.gallery.data.media.MediaStoreDataSource
import com.aryan.gallery.data.media.MediaStoreObserver
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
data class RenameMediaPayload(
    val mediaId: Long,
    val newName: String
)

@Serializable
data class RenameMediaResultDto(
    val status: String = "ok",
    val success: Boolean = true,
    val mediaId: Long,
    val newName: String
)

/**
 * Handler for RENAME_MEDIA commands from gallery-backend.
 * Directly renames requested media items via ContentResolver and records Room name overrides.
 */
@Singleton
class RenameMediaHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val mediaOverrideDao: MediaOverrideDao,
    private val mediaStoreObserver: MediaStoreObserver,
    private val json: Json
) : CommandHandler {

    override val supportedType: String = "RENAME_MEDIA"

    override suspend fun handleCommand(command: DeviceCommandEnvelope): DeviceCommandResultEnvelope = withContext(Dispatchers.IO) {
        return@withContext try {
            val payload = command.payload?.let {
                try { json.decodeFromString<RenameMediaPayload>(it) } catch (e: Exception) { null }
            } ?: throw IllegalArgumentException("Missing or invalid RENAME_MEDIA payload")

            val mediaId = payload.mediaId
            val newName = payload.newName.trim()
            if (newName.isEmpty()) {
                throw IllegalArgumentException("newName is empty")
            }

            val mediaItem = mediaStoreDataSource.getMediaItemById(mediaId)
            val targetUri = mediaItem?.uri ?: ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId)

            val updatedRows = try {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, newName)
                }
                context.contentResolver.update(targetUri, values, null, null)
            } catch (e: Exception) {
                0
            }

            mediaOverrideDao.insertOrUpdate(
                MediaOverrideEntity(
                    mediaId = mediaId,
                    customDisplayName = newName
                )
            )

            if (updatedRows > 0) {
                context.contentResolver.notifyChange(targetUri, null)
            }
            mediaStoreObserver.notifyMediaChanged()

            val resultDto = RenameMediaResultDto(
                status = "ok",
                success = updatedRows > 0 || mediaItem != null,
                mediaId = mediaId,
                newName = newName
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
                message = "Failed to execute direct media rename: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
