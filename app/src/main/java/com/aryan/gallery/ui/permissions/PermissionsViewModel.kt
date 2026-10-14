package com.aryan.gallery.ui.permissions

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * Manages runtime permission state evaluation and request flow tracking.
 */
@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionPreferences: PermissionPreferences
) : ViewModel() {

    private val _permissionState = MutableStateFlow<PermissionState>(PermissionState.NotRequested)
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val _isPartialAccessBannerVisible = MutableStateFlow(true)
    val isPartialAccessBannerVisible: StateFlow<Boolean> = _isPartialAccessBannerVisible.asStateFlow()

    fun updatePermissionState(activity: Activity?) {
        val state = PermissionUtils.checkPermissionState(
            context = context,
            activity = activity,
            hasRequested = permissionPreferences.hasRequestedPermissions
        )
        _permissionState.value = state
    }

    fun onPermissionResult(activity: Activity?) {
        permissionPreferences.hasRequestedPermissions = true
        updatePermissionState(activity)
    }

    fun dismissPartialAccessBanner() {
        _isPartialAccessBannerVisible.value = false
    }
}
