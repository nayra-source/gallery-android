package com.aryan.gallery.di

import com.aryan.gallery.config.MediaConfig
import com.aryan.gallery.config.NetworkConfig
import com.aryan.gallery.config.NotificationConfig
import com.aryan.gallery.config.TrashConfig
import com.aryan.gallery.config.WebSocketConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideNetworkConfig(): NetworkConfig = NetworkConfig()

    @Provides
    @Singleton
    fun provideWebSocketConfig(): WebSocketConfig = WebSocketConfig()

    @Provides
    @Singleton
    fun provideMediaConfig(): MediaConfig = MediaConfig()

    @Provides
    @Singleton
    fun provideNotificationConfig(): NotificationConfig = NotificationConfig()

    @Provides
    @Singleton
    fun provideTrashConfig(): TrashConfig = TrashConfig()
}
