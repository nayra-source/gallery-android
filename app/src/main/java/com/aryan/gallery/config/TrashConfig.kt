package com.aryan.gallery.config

/**
 * Centralized configuration parameters for local Room trash retention and auto-purge WorkManager tasks.
 */
data class TrashConfig(
    val retentionPeriodMs: Long = 30L * 24L * 60L * 60L * 1000L, // 30 days
    val autoPurgeWorkerIntervalDays: Long = 1L,
    val autoPurgeWorkName: String = "AutoPurgeWorker"
)
