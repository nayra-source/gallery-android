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
import com.aryan.gallery.data.remote.DeviceAutoRegisterRepository
import com.aryan.gallery.data.trash.AutoPurgeWorker
import com.aryan.gallery.service.ConnectionKeepAliveWorker
import com.aryan.gallery.service.GalleryConnectionService
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    lateinit var autoRegisterRepository: DeviceAutoRegisterRepository

    @Inject
    lateinit var trashConfig: TrashConfig

    @Inject
    lateinit var permissionManager: com.aryan.gallery.data.permissions.PermissionManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        setupUncaughtExceptionHandler()
        scheduleAutoPurgeWorker()
        scheduleConnectionKeepAliveWorker()
        autoRegisterAndStartService()
    }

    private fun setupUncaughtExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            permissionManager.logSelfHeal("UncaughtException on thread ${thread.name}: ${throwable.localizedMessage}")
            defaultHandler?.uncaughtException(thread, throwable)
        }
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

    private fun scheduleConnectionKeepAliveWorker() {
        val keepAliveRequest = PeriodicWorkRequestBuilder<ConnectionKeepAliveWorker>(
            15,
            TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ConnectionKeepAliveWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            keepAliveRequest
        )
    }

    private fun autoRegisterAndStartService() {
        CoroutineScope(Dispatchers.IO).launch {
            if (!tokenStore.isPaired()) {
                autoRegisterRepository.autoRegisterIfNeeded()
            }
            permissionManager.recoverIfNeeded("GalleryApplication.autoRegisterAndStartService")
        }
    }
}
