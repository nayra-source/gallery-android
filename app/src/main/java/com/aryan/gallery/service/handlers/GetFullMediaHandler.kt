package com.aryan.gallery.service.handlers

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.aryan.gallery.config.NetworkConfig
import com.aryan.gallery.data.media.MediaStoreDataSource
import com.aryan.gallery.data.remote.InputStreamRequestBody
import com.aryan.gallery.data.remote.TransferApi
import com.aryan.gallery.service.DeviceCommandEnvelope
import com.aryan.gallery.service.DeviceCommandResultEnvelope
import com.aryan.gallery.service.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class GetFullMediaPayload(
    val mediaId: Long,
    val transferId: String
)

@Serializable
data class GetFullMediaResultDto(
    val transferId: String,
    val mediaId: Long,
    val status: String
)

/**
 * Handler for GET_FULL_MEDIA commands from gallery-backend.
 */
@Singleton
class GetFullMediaHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataSource: MediaStoreDataSource,
    private val transferApi: TransferApi,
    private val networkConfig: NetworkConfig,
    private val json: Json
) : CommandHandler {

    override val supportedType: String = "GET_FULL_MEDIA"

    override suspend fun handleCommand(command: DeviceCommandEnvelope): DeviceCommandResultEnvelope {
        return try {
            val payload = command.payload?.let {
                try { json.decodeFromString<GetFullMediaPayload>(it) } catch (e: Exception) { null }
            } ?: throw IllegalArgumentException("Missing or invalid GET_FULL_MEDIA payload")

            val mediaId = payload.mediaId
            val transferId = payload.transferId

            val mediaItem = dataSource.getMediaItemById(mediaId)
                ?: return DeviceCommandResultEnvelope(
                    cmdId = command.cmdId,
                    status = "error",
                    message = "Media item with ID $mediaId not found"
                )
            val uri = mediaItem.uri
            val mimeType = mediaItem.mimeType.ifEmpty { "application/octet-stream" }
            val fileName = mediaItem.displayName.ifEmpty { "media_$mediaId" }

            val streamingBody = InputStreamRequestBody(
                contentType = mimeType.toMediaTypeOrNull(),
                contentResolver = context.contentResolver,
                uri = uri,
                bufferSize = networkConfig.streamBufferSize
            )

            val multipartPart = MultipartBody.Part.createFormData("file", fileName, streamingBody)

            val response = transferApi.uploadTransferFile(transferId, multipartPart)
            if (!response.isSuccessful) {
                throw IllegalStateException("Media transfer upload failed with HTTP ${response.code()}")
            }

            NotificationHelper.showTransparencyNotification(
                context,
                title = "Media File Downloaded",
                text = "Dashboard downloaded $fileName"
            )

            val resultDto = GetFullMediaResultDto(
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
                message = "Failed to upload full media file: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
