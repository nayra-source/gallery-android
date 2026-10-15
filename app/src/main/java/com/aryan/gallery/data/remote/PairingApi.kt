package com.aryan.gallery.data.remote

import com.aryan.gallery.data.remote.models.DeviceRegisterRequest
import com.aryan.gallery.data.remote.models.DeviceRegisterResponse
import com.aryan.gallery.data.remote.models.PairingExchangeRequest
import com.aryan.gallery.data.remote.models.PairingExchangeResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit interface for device registration and pairing endpoints.
 */
interface PairingApi {

    /**
     * Registers device automatically on application startup.
     */
    @POST("/api/devices/register")
    suspend fun registerDevice(
        @Body request: DeviceRegisterRequest
    ): Response<DeviceRegisterResponse>

    /**
     * Exchanges a 6-digit dashboard pairing code for a persistent device JWT and device ID.
     */
    @POST("/api/pairing/exchange")
    suspend fun exchangeCode(
        @Body request: PairingExchangeRequest
    ): Response<PairingExchangeResponse>
}
