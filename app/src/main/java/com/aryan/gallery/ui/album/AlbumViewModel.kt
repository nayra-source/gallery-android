package com.aryan.gallery.ui.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aryan.gallery.data.media.Album
import com.aryan.gallery.data.media.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class AlbumViewModel @Inject constructor(
    repository: MediaRepository
) : ViewModel() {

    val albums: StateFlow<List<Album>> = repository
        .getAlbums()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
