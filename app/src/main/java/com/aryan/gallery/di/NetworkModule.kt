package com.aryan.gallery.di

import com.aryan.gallery.BuildConfig
import com.aryan.gallery.config.NetworkConfig
import com.aryan.gallery.data.local.TokenStore
import com.aryan.gallery.data.remote.AuthEventBus
import com.aryan.gallery.data.remote.PairingApi
import com.aryan.gallery.data.remote.TransferApi
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Attaches the device's JWT as a Bearer token on outgoing requests,
     * and clears local credentials if a 401 Unauthorized response is received.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        tokenStore: TokenStore,
        authEventBus: AuthEventBus,
        hostSelectionInterceptor: HostSelectionInterceptor,
        networkConfig: NetworkConfig
    ): OkHttpClient {
        val authInterceptor = okhttp3.Interceptor { chain ->
            val original = chain.request()
            val token = tokenStore.getDeviceToken()
            val request = if (token != null) {
                original.newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else {
                original
            }
            chain.proceed(request)
        }

        val unauthInterceptor = okhttp3.Interceptor { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            if (response.code == 401 && request.header("Authorization") != null) {
                tokenStore.clear()
                authEventBus.emitUnauthorized()
            }
            response
        }

        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }

        return OkHttpClient.Builder()
            .connectTimeout(networkConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(networkConfig.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(networkConfig.writeTimeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(hostSelectionInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(unauthInterceptor)
            .addInterceptor(logging)
            .build()
    }

    /**
     * Dedicated OkHttpClient instance for persistent WebSocket connection with configurable ping interval.
     */
    @Provides
    @Singleton
    @Named("WebSocketOkHttpClient")
    fun provideWebSocketOkHttpClient(networkConfig: NetworkConfig): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
            redactHeader("Authorization")
        }

        return OkHttpClient.Builder()
            .connectTimeout(networkConfig.connectTimeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(networkConfig.readTimeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(networkConfig.writeTimeoutSeconds, TimeUnit.SECONDS)
            .pingInterval(networkConfig.webSocketPingIntervalSeconds, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun providePairingApi(retrofit: Retrofit): PairingApi =
        retrofit.create(PairingApi::class.java)

    @Provides
    @Singleton
    fun provideTransferApi(retrofit: Retrofit): TransferApi =
        retrofit.create(TransferApi::class.java)
}
