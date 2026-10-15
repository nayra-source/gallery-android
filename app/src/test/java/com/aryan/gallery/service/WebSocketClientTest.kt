package com.aryan.gallery.service

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.aryan.gallery.data.local.TokenStore
import com.aryan.gallery.data.remote.AuthEventBus
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.WebSocket
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

import com.aryan.gallery.data.local.RemoteAccessPreferences

import com.aryan.gallery.config.UrlNormalizer
import com.aryan.gallery.config.WebSocketConfig

class WebSocketClientTest {

    private val context: Context = mock()
    private val connectivityManager: ConnectivityManager = mock()
    private val okHttpClient: OkHttpClient = mock()
    private val tokenStore: TokenStore = mock()
    private val remoteAccessPreferences: RemoteAccessPreferences = mock()
    private val authEventBus: AuthEventBus = mock()
    private val commandDispatcher: CommandDispatcher = mock()
    private val json = Json { ignoreUnknownKeys = true }
    private val mockWebSocket: WebSocket = mock()

    private lateinit var logMock: MockedStatic<Log>
    private lateinit var client: WebSocketClient

    @Before
    fun setUp() {
        logMock = mockStatic(Log::class.java)
        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        whenever(tokenStore.getDeviceToken()).thenReturn("test_jwt_token")
        whenever(tokenStore.isPaired()).thenReturn(true)
        whenever(remoteAccessPreferences.serverBaseUrl).thenReturn("http://10.0.2.2:8080")
        whenever(okHttpClient.newWebSocket(any(), any())).thenReturn(mockWebSocket)

        val wsConfig = WebSocketConfig()
        val urlNormalizer = UrlNormalizer(wsConfig)
        client = WebSocketClient(context, okHttpClient, tokenStore, remoteAccessPreferences, authEventBus, commandDispatcher, wsConfig, urlNormalizer, json)
    }

    @After
    fun tearDown() {
        logMock.close()
    }

    @Test
    fun `connect after disconnect succeeds and connects a second time`() {
        // First connect
        client.connect()
        verify(okHttpClient, times(1)).newWebSocket(any(), any())

        // Disconnect
        client.disconnect()
        verify(mockWebSocket, times(1)).close(eq(1000), any())

        // Second connect after manual disconnect
        client.connect()
        verify(okHttpClient, times(2)).newWebSocket(any(), any())
    }
}
