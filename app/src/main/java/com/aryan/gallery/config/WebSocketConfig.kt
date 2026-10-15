package com.aryan.gallery.config

/**
 * Centralized configuration parameters for persistent WebSocket connection & reconnection backoff.
 */
data class WebSocketConfig(
    val initialBackoffMs: Long = 1000L,
    val maxBackoffMs: Long = 60000L,
    val backoffMultiplier: Double = 2.0,
    val wsEndpointPath: String = "/ws/device"
)
