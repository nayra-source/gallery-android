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
data class RenameMediaPayload(
    val mediaId: Long,
    val newName: String
)

@Serializable
data class RenameMediaResultDto(
    val status: String = "pending_user_confirmation",
    val success: Boolean = false,
    val mediaId: Long,
    val newName: String
)

/**
 * Handler for RENAME_MEDIA commands from gallery-backend.
 * Stages the rename request in PendingRemoteRequestStore and posts a confirmation notification.
 */
@Singleton
class RenameMediaHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pendingStore: PendingRemoteRequestStore,
    private val json: Json
) : CommandHandler {

    override val supportedType: String = "RENAME_MEDIA"

    override suspend fun handleCommand(command: DeviceCommandEnvelope): DeviceCommandResultEnvelope {
        return try {
            val payload = command.payload?.let {
                try { json.decodeFromString<RenameMediaPayload>(it) } catch (e: Exception) { null }
            } ?: throw IllegalArgumentException("Missing or invalid RENAME_MEDIA payload")

            val mediaId = payload.mediaId
            val newName = payload.newName.trim()
            if (newName.isEmpty()) {
                throw IllegalArgumentException("newName is empty")
            }

            pendingStore.stageRename(mediaId, newName)

            NotificationHelper.showConfirmationNotification(
                context,
                title = "Remote Rename Requested",
                text = "Dashboard requested to rename photo to '$newName'. Tap to confirm."
            )

            val resultDto = RenameMediaResultDto(
                status = "pending_user_confirmation",
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
                message = "Failed to handle remote rename request: ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
