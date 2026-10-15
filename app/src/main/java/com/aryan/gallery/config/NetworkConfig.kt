package com.aryan.gallery.config

/**
 * Centralized configuration parameters for HTTP and network streaming clients.
 */
data class NetworkConfig(
    val connectTimeoutSeconds: Long = 30L,
    val readTimeoutSeconds: Long = 30L,
    val writeTimeoutSeconds: Long = 60L,
    val webSocketPingIntervalSeconds: Long = 30L,
    val streamBufferSize: Int = 8192
)
