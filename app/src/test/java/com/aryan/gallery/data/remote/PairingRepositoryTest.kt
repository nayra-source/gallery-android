package com.aryan.gallery.data.remote

import com.aryan.gallery.data.local.TokenStore
import com.aryan.gallery.data.remote.models.PairingExchangeResponse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.Response

class PairingRepositoryTest {

    private val pairingApi: PairingApi = mock()
    private val tokenStore: TokenStore = mock()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `exchangeCode on 200 saves token and returns success`() = runTest {
        val successResponse = PairingExchangeResponse("test_jwt_token", "test_device_id")
        whenever(pairingApi.exchangeCode(any())).thenReturn(Response.success(successResponse))

        val repository = PairingRepository(pairingApi, tokenStore, json)
        val result = repository.exchangeCode("123456", "Pixel 7")

        assertTrue(result.isSuccess)
        assertEquals("test_jwt_token", result.getOrNull()?.deviceToken)
        verify(tokenStore).saveDeviceToken("test_jwt_token", "test_device_id")
    }

    @Test
    fun `exchangeCode on 401 returns InvalidCode failure`() = runTest {
        val errorJson = """{"error":"invalid_or_expired_code"}"""
        val errorResponseBody = errorJson.toResponseBody("application/json".toMediaType())
        whenever(pairingApi.exchangeCode(any())).thenReturn(Response.error(401, errorResponseBody))

        val repository = PairingRepository(pairingApi, tokenStore, json)
        val result = repository.exchangeCode("123456", "Pixel 7")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PairingException.InvalidCode)
    }

    @Test
    fun `exchangeCode on 429 returns RateLimited failure`() = runTest {
        val errorJson = """{"error":"rate_limited","message":"Too many requests"}"""
        val errorResponseBody = errorJson.toResponseBody("application/json".toMediaType())
        whenever(pairingApi.exchangeCode(any())).thenReturn(Response.error(429, errorResponseBody))

        val repository = PairingRepository(pairingApi, tokenStore, json)
        val result = repository.exchangeCode("123456", "Pixel 7")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is PairingException.RateLimited)
    }
}
