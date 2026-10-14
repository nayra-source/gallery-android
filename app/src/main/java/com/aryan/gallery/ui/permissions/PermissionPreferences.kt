package com.aryan.gallery.ui.permissions

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

import androidx.core.content.edit

/**
 * Tracks whether the user has previously initiated a permission request.
 * Needed to distinguish between [PermissionState.NotRequested] and
 * [PermissionState.PermanentlyDenied] when shouldShowRequestPermissionRationale returns false.
 */
@Singleton
class PermissionPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences("permission_prefs", Context.MODE_PRIVATE)

    var hasRequestedPermissions: Boolean
        get() = prefs.getBoolean(KEY_HAS_REQUESTED, false)
        set(value) = prefs.edit { putBoolean(KEY_HAS_REQUESTED, value) }

    companion object {
        private const val KEY_HAS_REQUESTED = "has_requested_media_permissions"
    }
}
