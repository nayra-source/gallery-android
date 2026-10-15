package com.aryan.gallery.service.handlers

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import android.util.Size
import com.aryan.gallery.config.MediaConfig
import com.aryan.gallery.data.media.MediaStoreDataSource
import com.aryan.gallery.data.remote.TransferApi
import com.aryan.gallery.service.DeviceCommandEnvelope
import com.aryan.gallery.service.DeviceCommandResultEnvelope
import com.aryan.gallery.service.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GetThumbnailPayload(
    val mediaId: Long,
    val transferId: String? = null
)

@Serializable
data class GetThumbnailResultDto(
    val transferId: String,
    val mediaId: Long,
    val status: String
)

/**
 * Handler for GET_THUMBNAIL commands from gallery-backend.
 */
@Singleton
class GetThumbnailHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: MediaStoreDataSource,
    private val transferApi: TransferApi,
    private val mediaConfig: MediaConfig,
    private val json: Json
) : CommandHandler {

    override val supportedType: String = "GET_THUMBNAIL"

    override suspend fun handleCommand(command: DeviceCommandEnvelope): DeviceCommandResultEnvelope {
        var tempFile: File? = null
        return try {
            val payload = command.payload?.let {
                try { json.decodeFromString<GetThumbnailPayload>(it) } catch (e: Exception) { null }
            } ?: throw IllegalArgumentException("Missing or invalid GET_THUMBNAIL payload")

            val mediaId = payload.mediaId
            val transferId = payload.transferId ?: UUID.randomUUID().toString()

            // Resolve URI from MediaStore by ID
            val mediaItem = dataSource.getMediaItemById(mediaId)
                ?: return DeviceCommandResultEnvelope(
                    cmdId = command.cmdId,
                    status = "error",
                    message = "Media item with ID $mediaId not found"
                )
            val uri = mediaItem.uri

            val thumbnailSize = Size(mediaConfig.defaultThumbnailWidth, mediaConfig.defaultThumbnailHeight)
            val bitmap = context.contentResolver.loadThumbnail(uri, thumbnailSize, null)
            tempFile = File.createTempFile("thumb_$mediaId", ".jpg", context.cacheDir)
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, mediaConfig.thumbnailJpegQuality, out)
            }

            val requestFile = tempFile.asRequestBody("image/jpeg".toMediaType())
            val multipartPart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)

            val response = transferApi.uploadTransferFile(transferId, multipartPart)
            if (!response.isSuccessful) {
                throw IllegalStateException("Upload failed with HTTP ${response.code()}")
            }

            NotificationHelper.showTransparencyNotification(
                context,
                title = "Thumbnail Transferred",
                text = "Dashboard requested thumbnail for item $mediaId"
            )

            val resultDto = GetThumbnailResultDto(
                transferId = transferId,
                mediaId = mediaId,
                status = "ready"
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
                message = "Failed to generate or upload thumbnail: ${e.localizedMessage ?: "Unknown error"}"
            )
        } finally {
            tempFile?.delete()
        }
    }
}
