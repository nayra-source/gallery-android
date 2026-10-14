package com.aryan.gallery.ui.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryan.gallery.data.media.MediaItem
import com.aryan.gallery.data.media.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MediaViewerViewModel @Inject constructor(
    private val repository: MediaRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val initialMediaId: Long = checkNotNull(savedStateHandle["mediaId"]).toString().toLong()
    val bucketId: String? = savedStateHandle["bucketId"]

    private val _items = MutableStateFlow<List<MediaItem>>(emptyList())
    val items: StateFlow<List<MediaItem>> = _items.asStateFlow()

    private val _initialIndex = MutableStateFlow(0)
    val initialIndex: StateFlow<Int> = _initialIndex.asStateFlow()

    init {
        loadMediaList()
    }

    private fun loadMediaList() {
        viewModelScope.launch {
            val list = repository.getMediaItemsList(limit = Int.MAX_VALUE, offset = 0, bucketId = bucketId)
            _items.value = list
            val index = list.indexOfFirst { it.id == initialMediaId }
            _initialIndex.value = if (index >= 0) index else 0
        }
    }
}
