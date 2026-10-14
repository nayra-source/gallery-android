package com.aryan.gallery.data.remote.models

import kotlinx.serialization.Serializable

/**
 * Response model for POST /api/transfers/{id}/upload
 */
@Serializable
data class TransferUploadResponse(
    val status: String
)
