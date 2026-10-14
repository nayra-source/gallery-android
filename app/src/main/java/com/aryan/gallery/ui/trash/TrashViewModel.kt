package com.aryan.gallery.ui.trash

import android.content.IntentSender
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryan.gallery.data.trash.TrashEntity
import com.aryan.gallery.data.trash.TrashRepository
import com.aryan.gallery.ui.gallery.SelectionState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class PendingActionType {
    NONE,
    RESTORE,
    DELETE_PERMANENT,
    EMPTY_TRASH,
    PURGE_EXPIRED
}

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val trashRepository: TrashRepository
) : ViewModel() {

    val trashItems: StateFlow<List<TrashEntity>> = trashRepository
        .trashItemsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    private val _expiredItems = MutableStateFlow<List<TrashEntity>>(emptyList())
    val expiredItems: StateFlow<List<TrashEntity>> = _expiredItems.asStateFlow()

    private val _pendingIntentSender = MutableStateFlow<IntentSender?>(null)
    val pendingIntentSender: StateFlow<IntentSender?> = _pendingIntentSender.asStateFlow()

    private var pendingAction = PendingActionType.NONE
    private var pendingIds = listOf<Long>()

    val selectionState = SelectionState()

    init {
        checkExpiredItems()
    }

    fun checkExpiredItems() {
        viewModelScope.launch {
            _expiredItems.value = trashRepository.getExpiredTrashItems()
        }
    }

    fun restoreSelected(selectedIds: Set<Long>) {
        val currentItems = trashItems.value.filter { selectedIds.contains(it.mediaId) }
        if (currentItems.isEmpty()) return

        val uris = currentItems.map { it.uriString.toUri() }
        val intentSender = trashRepository.createTrashRequestIntentSender(uris, trashed = false)

        pendingAction = PendingActionType.RESTORE
        pendingIds = currentItems.map { it.mediaId }
        _pendingIntentSender.value = intentSender
    }

    fun deletePermanentlySelected(selectedIds: Set<Long>) {
        val currentItems = trashItems.value.filter { selectedIds.contains(it.mediaId) }
        if (currentItems.isEmpty()) return

        val uris = currentItems.map { it.uriString.toUri() }
        val intentSender = trashRepository.createDeleteRequestIntentSender(uris)

        pendingAction = PendingActionType.DELETE_PERMANENT
        pendingIds = currentItems.map { it.mediaId }
        _pendingIntentSender.value = intentSender
    }

    fun emptyTrash() {
        val currentItems = trashItems.value
        if (currentItems.isEmpty()) return

        val uris = currentItems.map { it.uriString.toUri() }
        val intentSender = trashRepository.createDeleteRequestIntentSender(uris)

        pendingAction = PendingActionType.EMPTY_TRASH
        pendingIds = currentItems.map { it.mediaId }
        _pendingIntentSender.value = intentSender
    }

    fun purgeExpiredItems() {
        val currentExpired = expiredItems.value
        if (currentExpired.isEmpty()) return

        val uris = currentExpired.map { Uri.parse(it.uriString) }
        val intentSender = trashRepository.createDeleteRequestIntentSender(uris)

        pendingAction = PendingActionType.PURGE_EXPIRED
        pendingIds = currentExpired.map { it.mediaId }
        _pendingIntentSender.value = intentSender
    }

    fun onIntentSenderResultSuccess() {
        viewModelScope.launch {
            when (pendingAction) {
                PendingActionType.RESTORE, PendingActionType.DELETE_PERMANENT -> {
                    trashRepository.removeTrashMetadata(pendingIds)
                }
                PendingActionType.EMPTY_TRASH -> {
                    trashRepository.clearAllTrashMetadata()
                }
                PendingActionType.PURGE_EXPIRED -> {
                    trashRepository.removeTrashMetadata(pendingIds)
                    checkExpiredItems()
                }
                PendingActionType.NONE -> {}
            }
            selectionState.clearSelection()
            resetPendingState()
        }
    }

    fun onIntentSenderResultCanceled() {
        resetPendingState()
    }

    private fun resetPendingState() {
        _pendingIntentSender.value = null
        pendingAction = PendingActionType.NONE
        pendingIds = emptyList()
    }
}
