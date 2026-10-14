package com.aryan.gallery.ui.pairing

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryan.gallery.data.remote.PairingException
import com.aryan.gallery.data.remote.PairingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.aryan.gallery.data.local.RemoteAccessPreferences

sealed class PairingUiState {
    data object Idle : PairingUiState()
    data object Loading : PairingUiState()
    data class Error(val message: String) : PairingUiState()
    data object Success : PairingUiState()
}

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val pairingRepository: PairingRepository,
    private val remoteAccessPreferences: RemoteAccessPreferences
) : ViewModel() {

    val code = MutableStateFlow("")
    val deviceName = MutableStateFlow(Build.MODEL ?: "Android Phone")
    val serverHost = MutableStateFlow(remoteAccessPreferences.serverBaseUrl)

    private val _uiState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    @Volatile
    private var isSubmitting = false

    fun updateCode(newCode: String) {
        if (newCode.length <= 6 && newCode.all { it.isDigit() }) {
            code.value = newCode
            clearError()
        }
    }

    fun updateDeviceName(newName: String) {
        deviceName.value = newName
    }

    fun updateServerHost(newHost: String) {
        serverHost.value = newHost
        remoteAccessPreferences.serverBaseUrl = newHost
        clearError()
    }

    fun submitPairing() {
        if (isSubmitting) return

        val currentCode = code.value.trim()
        if (currentCode.length != 6) {
            _uiState.value = PairingUiState.Error("Please enter a valid 6-digit numeric code.")
            return
        }

        // Ensure host is saved
        remoteAccessPreferences.serverBaseUrl = serverHost.value

        isSubmitting = true
        _uiState.value = PairingUiState.Loading

        viewModelScope.launch {
            try {
                val result = pairingRepository.exchangeCode(
                    code = currentCode,
                    deviceName = deviceName.value.ifBlank { null }
                )

                result.fold(
                    onSuccess = {
                        _uiState.value = PairingUiState.Success
                    },
                    onFailure = { error ->
                        val currentHost = remoteAccessPreferences.serverBaseUrl
                        val userMsg = when {
                            error is PairingException.InvalidCode -> {
                                "The pairing code is invalid or has expired. Please generate a new code on your dashboard and try again."
                            }
                            error is PairingException.RateLimited -> {
                                "Too many pairing attempts (HTTP 429). Please wait a moment before trying again."
                            }
                            error is PairingException.ServerError -> {
                                "Server error (HTTP ${error.code}): ${error.detail}"
                            }
                            error.localizedMessage?.contains("10.0.2.2") == true || currentHost.contains("10.0.2.2") -> {
                                "Cannot connect to 10.0.2.2 on a physical phone.\n\nTip: 10.0.2.2 only works in Android Emulators. When testing on a real phone over Wi-Fi, change the Server Host field above to your PC's Wi-Fi IP address (e.g. http://10.84.187.xxx:8080 or http://192.168.1.xxx:8080)."
                            }
                            else -> {
                                "Network error: Could not connect to $currentHost (${error.localizedMessage ?: "Check connection"})."
                            }
                        }
                        _uiState.value = PairingUiState.Error(userMsg)
                    }
                )
            } finally {
                isSubmitting = false
            }
        }
    }

    private fun clearError() {
        if (_uiState.value is PairingUiState.Error) {
            _uiState.value = PairingUiState.Idle
        }
    }
}
