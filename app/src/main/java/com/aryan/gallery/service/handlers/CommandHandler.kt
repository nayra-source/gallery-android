package com.aryan.gallery.service.handlers

import com.aryan.gallery.service.DeviceCommandEnvelope
import com.aryan.gallery.service.DeviceCommandResultEnvelope

/**
 * Interface implemented by individual remote WebSocket command handlers.
 */
interface CommandHandler {
    val supportedType: String
    suspend fun handleCommand(command: DeviceCommandEnvelope): DeviceCommandResultEnvelope
}
