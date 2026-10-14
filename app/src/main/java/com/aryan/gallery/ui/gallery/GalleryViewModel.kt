package com.aryan.gallery.ui.gallery

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.aryan.gallery.data.media.MediaItem
import com.aryan.gallery.data.media.MediaRepository
import com.aryan.gallery.data.trash.TrashRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GalleryViewModel @Inject constructor(
    repository: MediaRepository,
    private val trashRepository: TrashRepository
) : ViewModel() {

    val mediaFlow: Flow<PagingData<MediaItem>> = repository
        .getMediaPaged()
        .cachedIn(viewModelScope)

    val selectionState = SelectionState()

    private val _pendingIntentSender = MutableStateFlow<IntentSender?>(null)
    val pendingIntentSender: StateFlow<IntentSender?> = _pendingIntentSender.asStateFlow()

    private var pendingMediaItems = listOf<MediaItem>()

    fun trashSelectedItems(selectedItems: List<MediaItem>) {
        if (selectedItems.isEmpty()) return
        val uris = selectedItems.map { it.uri }
        pendingMediaItems = selectedItems
        val intentSender = trashRepository.createTrashRequestIntentSender(uris, trashed = true)
        _pendingIntentSender.value = intentSender
    }

    fun onTrashResultSuccess() {
        viewModelScope.launch {
            trashRepository.recordTrashedItems(pendingMediaItems)
            selectionState.clearSelection()
            resetPendingState()
        }
    }

    fun onTrashResultCanceled() {
        resetPendingState()
    }

    private fun resetPendingState() {
        _pendingIntentSender.value = null
        pendingMediaItems = emptyList()
    }
}
