package com.aryan.gallery.ui.permissions

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    /**
     * Returns the array of permissions to request based on the SDK version.
     * Includes photo/video media permissions and non-blocking POST_NOTIFICATIONS (API 33+).
     * On Android 14+ (API 34+), includes READ_MEDIA_VISUAL_USER_SELECTED for partial access support.
     */
    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    /**
     * Evaluates the current runtime permission state.
     */
    fun checkPermissionState(
        context: Context,
        activity: Activity?,
        hasRequested: Boolean
    ): PermissionState {
        val hasImages = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED

        val hasVideos = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_MEDIA_VIDEO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasImages && hasVideos) {
            return PermissionState.Granted
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val hasSelected = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            ) == PackageManager.PERMISSION_GRANTED
            if (hasSelected) {
                return PermissionState.PartiallyGranted
            }
        }

        if (!hasRequested) {
            return PermissionState.NotRequested
        }

        if (activity != null) {
            val shouldShowImages = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                android.Manifest.permission.READ_MEDIA_IMAGES
            )
            val shouldShowVideos = ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                android.Manifest.permission.READ_MEDIA_VIDEO
            )
            val shouldShowSelected = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                )
            } else false

            return if (shouldShowImages || shouldShowVideos || shouldShowSelected) {
                PermissionState.Denied
            } else {
                PermissionState.PermanentlyDenied
            }
        }

        return PermissionState.Denied
    }

    /**
     * Opens the app's System Settings page so the user can grant permissions manually.
     */
    fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    /**
     * Traverses the Context chain to resolve the host Activity.
     */
    tailrec fun Context.findActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
