package com.aryan.gallery.data.remote

import com.aryan.gallery.data.remote.models.PairingExchangeRequest
import com.aryan.gallery.data.remote.models.PairingExchangeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for device pairing endpoints matching gallery-backend/API_CONTRACTS.md.
 */
interface PairingApi {

    /**
     * Exchanges a 6-digit dashboard pairing code for a persistent device JWT and device ID.
     */
    @POST("/api/pairing/exchange")
    suspend fun exchangeCode(
        @Body request: PairingExchangeRequest
    ): Response<PairingExchangeResponse>
}
