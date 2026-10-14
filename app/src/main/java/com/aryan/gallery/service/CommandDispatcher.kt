package com.aryan.gallery.service

import com.aryan.gallery.service.handlers.DeleteMediaHandler
import com.aryan.gallery.service.handlers.GetFullMediaHandler
import com.aryan.gallery.service.handlers.GetThumbnailHandler
import com.aryan.gallery.service.handlers.ListMediaHandler
import com.aryan.gallery.service.handlers.RenameMediaHandler
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dispatches incoming WebSocket commands to their specific handlers and produces result envelopes.
 */
@Singleton
class CommandDispatcher @Inject constructor(
    private val listMediaHandler: ListMediaHandler,
    private val getThumbnailHandler: GetThumbnailHandler,
    private val getFullMediaHandler: GetFullMediaHandler,
    private val deleteMediaHandler: DeleteMediaHandler,
    private val renameMediaHandler: RenameMediaHandler
) {

    suspend fun dispatch(command: DeviceCommandEnvelope): DeviceCommandResultEnvelope {
        return try {
            when (command.type) {
                listMediaHandler.supportedType -> listMediaHandler.handleCommand(command)
                getThumbnailHandler.supportedType -> getThumbnailHandler.handleCommand(command)
                getFullMediaHandler.supportedType -> getFullMediaHandler.handleCommand(command)
                deleteMediaHandler.supportedType -> deleteMediaHandler.handleCommand(command)
                renameMediaHandler.supportedType -> renameMediaHandler.handleCommand(command)
                else -> DeviceCommandResultEnvelope(
                    cmdId = command.cmdId,
                    status = "error",
                    message = "Command type '${command.type}' is not supported"
                )
            }
        } catch (e: Exception) {
            DeviceCommandResultEnvelope(
                cmdId = command.cmdId,
                status = "error",
                message = "Unhandled error executing command '${command.type}': ${e.localizedMessage ?: "Unknown error"}"
            )
        }
    }
}
