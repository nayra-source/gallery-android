package com.aryan.gallery.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryan.gallery.data.local.RemoteAccessPreferences
import com.aryan.gallery.data.local.TokenStore
import com.aryan.gallery.data.remote.AuthEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val tokenStore: TokenStore,
    private val remoteAccessPreferences: RemoteAccessPreferences,
    authEventBus: AuthEventBus
) : ViewModel() {

    private val _isPaired = MutableStateFlow(tokenStore.isPaired())
    val isPaired: StateFlow<Boolean> = _isPaired.asStateFlow()

    private val _deviceId = MutableStateFlow(tokenStore.getDeviceId())
    val deviceId: StateFlow<String?> = _deviceId.asStateFlow()

    private val _serverHost = MutableStateFlow(remoteAccessPreferences.serverBaseUrl)
    val serverHost: StateFlow<String> = _serverHost.asStateFlow()

    private val _showBatteryOptimizationDialog = MutableStateFlow(false)
    val showBatteryOptimizationDialog: StateFlow<Boolean> = _showBatteryOptimizationDialog.asStateFlow()

    init {
        viewModelScope.launch {
            authEventBus.unauthorizedEvents.collect {
                refreshState()
            }
        }
    }

    fun refreshState() {
        _isPaired.value = tokenStore.isPaired()
        _deviceId.value = tokenStore.getDeviceId()
        _serverHost.value = remoteAccessPreferences.serverBaseUrl
    }

    fun updateServerHost(newHost: String) {
        _serverHost.value = newHost
        remoteAccessPreferences.serverBaseUrl = newHost
    }

    fun dismissBatteryOptimizationDialog() {
        remoteAccessPreferences.hasHandledBatteryOptimizationPrompt = true
        _showBatteryOptimizationDialog.value = false
    }

    fun unpair() {
        tokenStore.clear()
        refreshState()
    }
}
