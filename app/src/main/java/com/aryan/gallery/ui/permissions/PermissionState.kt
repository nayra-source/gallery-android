package com.aryan.gallery.ui.permissions

/**
 * Represents the current runtime permission state for media access.
 */
sealed class PermissionState {

    /** Permissions have not been requested yet. */
    data object NotRequested : PermissionState()

    /** Full access to both photos and videos has been granted. */
    data object Granted : PermissionState()

    /**
     * Partial access granted on Android 14+ (API 34+) via
     * [android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED].
     */
    data object PartiallyGranted : PermissionState()

    /** Permissions denied after request; rationale can be shown. */
    data object Denied : PermissionState()

    /** Permissions permanently denied; user must enable via System Settings. */
    data object PermanentlyDenied : PermissionState()
}
