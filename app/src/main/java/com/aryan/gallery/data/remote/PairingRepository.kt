package com.aryan.gallery.data.remote

import com.aryan.gallery.data.local.TokenStore
import com.aryan.gallery.data.remote.models.ApiErrorResponse
import com.aryan.gallery.data.remote.models.PairingExchangeRequest
import com.aryan.gallery.data.remote.models.PairingExchangeResponse
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

sealed class PairingException(message: String) : Exception(message) {
    data class InvalidCode(val detail: String?) : PairingException("invalid_or_expired_code")
    data class RateLimited(val detail: String?) : PairingException("rate_limited")
    data class ServerError(val code: Int, val detail: String?) : PairingException("http_$code")
}

/**
 * Repository performing backend pairing code exchange and persisting resulting credentials.
 */
@Singleton
class PairingRepository @Inject constructor(
    private val pairingApi: PairingApi,
    private val tokenStore: TokenStore,
    private val json: Json
) {

    /**
     * Exchanges a 6-digit dashboard code for a device token and device ID.
     */
    suspend fun exchangeCode(code: String, deviceName: String? = null): Result<PairingExchangeResponse> {
        return try {
            val response = pairingApi.exchangeCode(PairingExchangeRequest(code, deviceName))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                tokenStore.saveDeviceToken(body.deviceToken, body.deviceId)
                Result.success(body)
            } else {
                val errorBodyStr = response.errorBody()?.string()
                val apiError = try {
                    if (!errorBodyStr.isNullOrEmpty()) {
                        json.decodeFromString<ApiErrorResponse>(errorBodyStr)
                    } else null
                } catch (e: Exception) {
                    null
                }

                val statusCode = response.code()
                val apiErrorCode = apiError?.code ?: apiError?.error
                val exception = when {
                    statusCode == 429 || apiErrorCode == "rate_limited" -> {
                        PairingException.RateLimited(apiError?.message ?: "Rate limit exceeded (HTTP 429)")
                    }
                    statusCode == 401 || apiErrorCode == "invalid_or_expired_code" -> {
                        PairingException.InvalidCode(apiError?.message ?: "Invalid or expired code")
                    }
                    else -> {
                        PairingException.ServerError(
                            code = statusCode,
                            detail = apiErrorCode ?: apiError?.message ?: "HTTP $statusCode"
                        )
                    }
                }
                Result.failure(exception)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
