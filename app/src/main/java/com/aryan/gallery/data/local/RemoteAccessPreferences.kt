package com.aryan.gallery.data.local

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import com.aryan.gallery.BuildConfig

/**
 * Persists non-sensitive user settings for remote background connection, server host, and battery optimization.
 */
@Singleton
class RemoteAccessPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("remote_access_prefs", Context.MODE_PRIVATE)

    var serverBaseUrl: String
        get() = prefs.getString(KEY_SERVER_BASE_URL, null) ?: BuildConfig.BASE_URL
        set(value) {
            val trimmed = value.trim()
            val normalized = when {
                trimmed.isEmpty() -> BuildConfig.BASE_URL
                !trimmed.startsWith("http://") && !trimmed.startsWith("https://") -> "http://$trimmed"
                else -> trimmed
            }
            prefs.edit { putString(KEY_SERVER_BASE_URL, normalized) }
        }

    var isRemoteAccessEnabled: Boolean
        get() = prefs.getBoolean(KEY_REMOTE_ACCESS_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_REMOTE_ACCESS_ENABLED, value) }

    var hasHandledBatteryOptimizationPrompt: Boolean
        get() = prefs.getBoolean(KEY_BATTERY_PROMPT_HANDLED, false)
        set(value) = prefs.edit { putBoolean(KEY_BATTERY_PROMPT_HANDLED, value) }

    private companion object {
        const val KEY_SERVER_BASE_URL = "server_base_url"
        const val KEY_REMOTE_ACCESS_ENABLED = "remote_access_enabled"
        const val KEY_BATTERY_PROMPT_HANDLED = "battery_prompt_handled"
    }
}
