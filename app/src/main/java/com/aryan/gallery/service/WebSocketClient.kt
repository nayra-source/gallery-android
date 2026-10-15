package com.aryan.gallery.service

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.aryan.gallery.config.UrlNormalizer
import com.aryan.gallery.config.WebSocketConfig
import com.aryan.gallery.data.local.RemoteAccessPreferences
import com.aryan.gallery.data.local.TokenStore
import com.aryan.gallery.data.remote.AuthEventBus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "GalleryWebSocket"

/**
 * Manages the persistent WebSocket connection to gallery-backend with exponential backoff,
 * network connectivity listeners, and command result framing.
 */
@Singleton
class WebSocketClient @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("WebSocketOkHttpClient") private val okHttpClient: OkHttpClient,
    private val tokenStore: TokenStore,
    private val remoteAccessPreferences: RemoteAccessPreferences,
    private val authEventBus: AuthEventBus,
    private val commandDispatcher: CommandDispatcher,
    private val webSocketConfig: WebSocketConfig,
    private val urlNormalizer: UrlNormalizer,
    private val json: Json
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var webSocket: WebSocket? = null

    @Volatile
    var isConnected = false
        private set
    @Volatile
    private var isManualDisconnect = false
    private var backoffDelayMs = webSocketConfig.initialBackoffMs
    private var reconnectJob: Job? = null

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            Log.d(TAG, "Network became available. Resetting backoff and reconnecting if needed.")
            if (!isConnected && !isManualDisconnect && tokenStore.isPaired()) {
                backoffDelayMs = webSocketConfig.initialBackoffMs
                reconnectJob?.cancel()
                connect()
            }
        }
    }

    init {
        registerNetworkCallback()
    }

    private fun registerNetworkCallback() {
        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        } catch (e: Throwable) {
            Log.w(TAG, "Failed to register network callback", e)
        }
    }

    @Synchronized
    fun connect() {
        isManualDisconnect = false
        if (isConnected) return

        val token = tokenStore.getDeviceToken()
        if (token.isNullOrEmpty()) {
            Log.d(TAG, "Cannot connect WebSocket: Device is not paired (no token).")
            return
        }

        val rawBaseUrl = remoteAccessPreferences.serverBaseUrl
        val wsUrl = urlNormalizer.toWebSocketUrl(rawBaseUrl, token)
        val request = Request.Builder().url(wsUrl).build()

        val safeLogUrl = wsUrl.substringBefore("?")
        Log.d(TAG, "Connecting to WebSocket at $safeLogUrl...")
        webSocket = okHttpClient.newWebSocket(request, createWebSocketListener())
    }

    @Synchronized
    fun disconnect() {
        isManualDisconnect = true
        reconnectJob?.cancel()
        webSocket?.close(1000, "Normal disconnect")
        webSocket = null
        isConnected = false
        Log.d(TAG, "WebSocket manually disconnected.")
    }

    private fun createWebSocketListener() = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WebSocket connected successfully.")
            isConnected = true
            backoffDelayMs = webSocketConfig.initialBackoffMs
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            Log.d(TAG, "Received WebSocket command message")
            scope.launch {
                try {
                    val command = json.decodeFromString<DeviceCommandEnvelope>(text)
                    Log.d(TAG, "Dispatching command: type=${command.type}, id=${command.cmdId}")

                    val resultEnvelope = commandDispatcher.dispatch(command)
                    val responseJson = json.encodeToString(resultEnvelope)
                    webSocket.send(responseJson)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse or dispatch incoming WebSocket frame: $text", e)
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closing: $code / $reason")
            handleSocketClosed(code, reason)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            Log.d(TAG, "WebSocket closed: $code / $reason")
            handleSocketClosed(code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WebSocket failure (response code: ${response?.code})", t)
            val isAuthFailure = response?.code == 401 ||
                    t.message?.contains("401") == true ||
                    t.message?.contains("invalid_token") == true ||
                    t.message?.contains("revoked") == true

            if (isAuthFailure) {
                handleAuthFailure("401 Unauthorized or revoked token")
            } else {
                handleSocketClosed(1006, t.message ?: "Unknown failure")
            }
        }
    }

    private fun handleSocketClosed(code: Int, reason: String) {
        isConnected = false
        val isAuthFailure = code == 1008 ||
                reason.contains("invalid_token", ignoreCase = true) ||
                reason.contains("revoked", ignoreCase = true)

        if (isAuthFailure) {
            handleAuthFailure("Close code $code / $reason")
        } else {
            scheduleReconnect()
        }
    }

    private fun handleAuthFailure(details: String) {
        Log.w(TAG, "Auth failure detected on WebSocket ($details). Clearing credentials.")
        tokenStore.clear()
        authEventBus.emitUnauthorized()
        disconnect()
    }

    private fun scheduleReconnect() {
        if (isManualDisconnect || !tokenStore.isPaired()) return

        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            Log.d(TAG, "Scheduling WebSocket reconnect in ${backoffDelayMs}ms...")
            delay(backoffDelayMs)
            backoffDelayMs = (backoffDelayMs * webSocketConfig.backoffMultiplier.toLong()).coerceAtMost(webSocketConfig.maxBackoffMs)
            connect()
        }
    }
}
