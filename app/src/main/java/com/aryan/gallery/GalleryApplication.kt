package com.aryan.gallery

import android.app.Application
import android.content.Intent
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.aryan.gallery.config.TrashConfig
import com.aryan.gallery.data.local.RemoteAccessPreferences
import com.aryan.gallery.data.local.TokenStore
import com.aryan.gallery.data.trash.AutoPurgeWorker
import com.aryan.gallery.service.GalleryConnectionService
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Hilt's generated component graph is rooted here. Every @AndroidEntryPoint
 * (activities, services, etc.) in the app depends on this being present and
 * declared as android:name=".GalleryApplication" in the manifest.
 */
@HiltAndroidApp
class GalleryApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var tokenStore: TokenStore

    @Inject
    lateinit var remoteAccessPreferences: RemoteAccessPreferences

    @Inject
    lateinit var trashConfig: TrashConfig

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleAutoPurgeWorker()
        startConnectionServiceIfPaired()
    }

    private fun scheduleAutoPurgeWorker() {
        val purgeWorkRequest = PeriodicWorkRequestBuilder<AutoPurgeWorker>(
            trashConfig.autoPurgeWorkerIntervalDays,
            TimeUnit.DAYS
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            trashConfig.autoPurgeWorkName,
            ExistingPeriodicWorkPolicy.KEEP,
            purgeWorkRequest
        )
    }

    private fun startConnectionServiceIfPaired() {
        if (tokenStore.isPaired() && remoteAccessPreferences.isRemoteAccessEnabled) {
            val serviceIntent = Intent(this, GalleryConnectionService::class.java)
            startForegroundService(serviceIntent)
        }
    }
}
