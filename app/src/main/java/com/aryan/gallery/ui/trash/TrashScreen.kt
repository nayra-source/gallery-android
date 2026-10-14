package com.aryan.gallery.ui.trash

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aryan.gallery.data.trash.TrashEntity
import com.aryan.gallery.ui.gallery.EmptyMediaState

/**
 * Screen displaying local trashed items with options to restore, delete permanently, or empty trash.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val trashItems by viewModel.trashItems.collectAsState()
    val expiredItems by viewModel.expiredItems.collectAsState()
    val pendingIntentSender by viewModel.pendingIntentSender.collectAsState()
    val selectionState = viewModel.selectionState

    var showConfirmDeletePermanentlyDialog by remember { mutableStateOf(false) }
    var showConfirmEmptyTrashDialog by remember { mutableStateOf(false) }

    val intentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onIntentSenderResultSuccess()
        } else {
            viewModel.onIntentSenderResultCanceled()
        }
    }

    LaunchedEffect(pendingIntentSender) {
        pendingIntentSender?.let { sender ->
            intentSenderLauncher.launch(IntentSenderRequest.Builder(sender).build())
        }
    }

    if (showConfirmDeletePermanentlyDialog) {
        ConfirmDialog(
            title = "Delete Permanently?",
            text = "Selected item(s) will be permanently deleted from your device. This action cannot be undone.",
            confirmButtonText = "Delete Forever",
            onConfirm = {
                showConfirmDeletePermanentlyDialog = false
                viewModel.deletePermanentlySelected(selectionState.selectedIds)
            },
            onDismiss = { showConfirmDeletePermanentlyDialog = false }
        )
    }

    if (showConfirmEmptyTrashDialog) {
        ConfirmDialog(
            title = "Empty Trash?",
            text = "All items in Trash will be permanently deleted from your device. This action cannot be undone.",
            confirmButtonText = "Empty Trash",
            onConfirm = {
                showConfirmEmptyTrashDialog = false
                viewModel.emptyTrash()
            },
            onDismiss = { showConfirmEmptyTrashDialog = false }
        )
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
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Exit selection mode")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.restoreSelected(selectionState.selectedIds) }) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restore selected")
                        }
                        IconButton(onClick = { showConfirmDeletePermanentlyDialog = true }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete permanently")
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
                            text = "Trash",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    actions = {
                        if (trashItems.isNotEmpty()) {
                            TextButton(onClick = { showConfirmEmptyTrashDialog = true }) {
                                Text("Empty Trash", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (expiredItems.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = "${expiredItems.size} item(s) in Trash are older than 30 days.",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Button(onClick = { viewModel.purgeExpiredItems() }) {
                            Text("Purge Now")
                        }
                    }
                }
            }

            if (trashItems.isEmpty()) {
                EmptyMediaState(message = "Trash is empty")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 120.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(trashItems, key = { it.mediaId }) { item ->
                        val isSelected = selectionState.isSelected(item.mediaId)
                        TrashTile(
                            item = item,
                            isSelectionMode = selectionState.isSelectionMode,
                            isSelected = isSelected,
                            onClick = {
                                if (selectionState.isSelectionMode) {
                                    selectionState.toggleSelection(item.mediaId)
                                } else {
                                    selectionState.enterSelectionMode(item.mediaId)
                                }
                            },
                            onLongClick = {
                                selectionState.enterSelectionMode(item.mediaId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrashTile(
    item: TrashEntity,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val context = LocalContext.current
    val imageRequest = remember(item.uriString) {
        ImageRequest.Builder(context)
            .data(item.uriString)
            .size(300, 300)
            .crossfade(true)
            .build()
    }

    val daysLeft = remember(item.purgeAt) {
        val remainingMs = item.purgeAt - System.currentTimeMillis()
        val days = (remainingMs / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
        "$days days left"
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                )
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = item.displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(4.dp),
                shape = RoundedCornerShape(4.dp),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Text(
                    text = daysLeft,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }

            if (isSelectionMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isSelected) Color.Black.copy(alpha = 0.4f) else Color.Transparent)
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(22.dp)
                                .background(Color.White, CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                                .size(22.dp)
                                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}
