package com.aryan.gallery.ui.gallery

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.aryan.gallery.data.media.MediaItem

/**
 * Main Gallery Screen displaying a paged grid of photos and videos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateToViewer: (mediaId: Long, bucketId: String?) -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val lazyPagingItems = viewModel.mediaFlow.collectAsLazyPagingItems()
    val selectionState = viewModel.selectionState
    val pendingIntentSender by viewModel.pendingIntentSender.collectAsState()

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onTrashResultSuccess()
        } else {
            viewModel.onTrashResultCanceled()
        }
    }

    LaunchedEffect(pendingIntentSender) {
        pendingIntentSender?.let { sender ->
            intentSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    Scaffold(
        topBar = {
            if (selectionState.isSelectionMode) {
                TopAppBar(
                    title = {
                        Text(
                            text = "${selectionState.selectedIds.size} selected",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { selectionState.clearSelection() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Exit selection mode"
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                val selectedList = mutableListOf<MediaItem>()
                                for (i in 0 until lazyPagingItems.itemCount) {
                                    val item = lazyPagingItems.peek(i)
                                    if (item != null && selectionState.isSelected(item.id)) {
                                        selectedList.add(item)
                                    }
                                }
                                viewModel.trashSelectedItems(selectedList)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete selected items"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Text(
                            text = "Photos",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            MediaGrid(
                lazyPagingItems = lazyPagingItems,
                selectionState = selectionState,
                onMediaClick = { mediaItem, _ ->
                    onNavigateToViewer(mediaItem.id, null)
                }
            )
        }
    }
}
