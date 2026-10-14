package com.aryan.gallery.data.remote.models

import kotlinx.serialization.Serializable

/**
 * Request body for POST /api/pairing/exchange
 */
@Serializable
data class PairingExchangeRequest(
    val code: String,
    val deviceName: String? = null
)

/**
 * Response body for 200 OK from POST /api/pairing/exchange
 */
@Serializable
data class PairingExchangeResponse(
    val deviceToken: String,
    val deviceId: String
)

/**
 * Error body returned by backend endpoints
 */
@Serializable
data class ApiErrorResponse(
    val code: String? = null,
    val error: String? = null,
    val message: String? = null
)
