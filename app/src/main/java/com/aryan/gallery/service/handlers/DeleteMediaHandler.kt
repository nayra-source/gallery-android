package com.aryan.gallery.service.handlers

import android.content.Context
import com.aryan.gallery.data.remote.PendingRemoteRequestStore
import com.aryan.gallery.service.DeviceCommandEnvelope
import com.aryan.gallery.service.DeviceCommandResultEnvelope
import com.aryan.gallery.service.NotificationHelper
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val status: String = "pending_user_confirmation",
    val trashed: List<Long> = emptyList(),
    val failed: List<Long> = emptyList(),
    val requestedIds: List<Long>
)

/**
 * Handler for DELETE_MEDIA commands from gallery-backend.
 * Stages the delete request in PendingRemoteRequestStore and posts a confirmation notification.
 */
@Singleton
class DeleteMediaHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingStore: PendingRemoteRequestStore,
    private val json: Json
) : CommandHandler {

    override val supportedType: String = "DELETE_MEDIA"

    override suspend fun handleCommand(command: DeviceCommandEnvelope): DeviceCommandResultEnvelope {
        return try {
            val payload = command.payload?.let {
                try { json.decodeFromString<DeleteMediaPayload>(it) } catch (e: Exception) { null }
            } ?: throw IllegalArgumentException("Missing or invalid DELETE_MEDIA payload")

            val mediaIds = payload.mediaIds
            if (mediaIds.isEmpty()) {
                throw IllegalArgumentException("mediaIds list is empty")
            }

            pendingStore.stageDelete(mediaIds)

            NotificationHelper.showConfirmationNotification(
                context,
                title = "Remote Delete Requested",
                text = "Dashboard requested to delete ${mediaIds.size} photo(s). Tap to confirm."
            )

            val resultDto = DeleteMediaResultDto(
                status = "pending_user_confirmation",
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
                message = "Failed to handle remote delete request: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
