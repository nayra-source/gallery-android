package com.aryan.gallery.service

import kotlinx.serialization.Serializable

/**
 * Incoming command envelope from gallery-backend via WebSocket.
 */
@Serializable
data class DeviceCommandEnvelope(
    val cmdId: String,
    val type: String,
    val payload: String? = null
)

/**
 * Outgoing result envelope returned to gallery-backend via WebSocket.
 */
@Serializable
data class DeviceCommandResultEnvelope(
    val cmdId: String,
    val status: String,
    val result: String? = null,
    val message: String? = null
)
