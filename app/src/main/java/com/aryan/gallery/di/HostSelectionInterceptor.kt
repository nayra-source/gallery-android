package com.aryan.gallery.di

import com.aryan.gallery.config.UrlNormalizer
import com.aryan.gallery.data.local.RemoteAccessPreferences
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dynamically reroutes Retrofit HTTP requests to the server host configured in RemoteAccessPreferences.
 */
@Singleton
class HostSelectionInterceptor @Inject constructor(
    private val preferences: RemoteAccessPreferences,
    private val urlNormalizer: UrlNormalizer
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val customBaseUrl = preferences.serverBaseUrl
        val normalizedUrl = urlNormalizer.normalizeHttpUrl(customBaseUrl)
        val newHostUri = normalizedUrl.toHttpUrlOrNull()

        if (newHostUri != null) {
            val newUrl = request.url.newBuilder()
                .scheme(newHostUri.scheme)
                .host(newHostUri.host)
                .port(newHostUri.port)
                .build()
            request = request.newBuilder().url(newUrl).build()
        }

        return chain.proceed(request)
    }
}
