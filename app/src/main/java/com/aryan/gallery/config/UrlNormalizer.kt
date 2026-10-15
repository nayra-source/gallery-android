package com.aryan.gallery.config

import com.aryan.gallery.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Standalone single-source-of-truth utility for validating, formatting, and normalizing
 * backend server host URLs and WebSocket URIs.
 */
@Singleton
class UrlNormalizer @Inject constructor(
    private val webSocketConfig: WebSocketConfig
) {

    /**
     * Ensures an input URL string is well-formed with a valid scheme (http:// or https://)
     * and trimmed trailing slashes. Returns fallback if input is blank or unparseable.
     */
    fun normalizeHttpUrl(rawUrl: String?, fallbackUrl: String = BuildConfig.BASE_URL): String {
        val trimmed = rawUrl?.trim() ?: ""
        if (trimmed.isEmpty()) return fallbackUrl

        val withScheme = when {
            trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> "http://$trimmed"
        }

        val parsed = withScheme.toHttpUrlOrNull() ?: return fallbackUrl
        return parsed.toString().removeSuffix("/")
    }

    /**
     * Converts a normalized HTTP server base URL into a WebSocket endpoint URI
     * matching the specified [webSocketConfig.wsEndpointPath] and optional authentication token query parameter.
     */
    fun toWebSocketUrl(rawBaseUrl: String, token: String? = null): String {
        val normalizedBase = normalizeHttpUrl(rawBaseUrl)
        val httpUrl = normalizedBase.toHttpUrlOrNull() ?: BuildConfig.BASE_URL.toHttpUrlOrNull()!!

        val wsScheme = if (httpUrl.scheme.equals("https", ignoreCase = true)) "wss" else "ws"
        val hostAndPort = if (httpUrl.port == 80 || httpUrl.port == 443) {
            httpUrl.host
        } else {
            "${httpUrl.host}:${httpUrl.port}"
        }

        val cleanEndpoint = webSocketConfig.wsEndpointPath.ensurePrefix("/")
        val baseWsUrl = "$wsScheme://$hostAndPort$cleanEndpoint"

        return if (!token.isNullOrEmpty()) {
            "$baseWsUrl?token=$token"
        } else {
            baseWsUrl
        }
    }

    private fun String.ensurePrefix(prefix: String): String =
        if (this.startsWith(prefix)) this else "$prefix$this"
}
