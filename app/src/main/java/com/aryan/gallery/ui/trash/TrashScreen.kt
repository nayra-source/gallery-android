package com.aryan.gallery.ui.trash

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.aryan.gallery.R
import com.aryan.gallery.data.trash.TrashEntity
import com.aryan.gallery.ui.components.EmptyStates
import com.aryan.gallery.ui.theme.Dimens

/**
 * Screen displaying local trashed items with options to restore, delete permanently, or empty trash.
 * Features animated TopAppBar selection mode, lifecycle-aware observation, M3 elevation, and polished badges.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val trashItems by viewModel.trashItems.collectAsStateWithLifecycle()
    val expiredItems by viewModel.expiredItems.collectAsStateWithLifecycle()
    val pendingIntentSender by viewModel.pendingIntentSender.collectAsStateWithLifecycle()
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
            title = stringResource(R.string.trash_delete_dialog_title),
            text = stringResource(R.string.trash_delete_dialog_text),
            confirmButtonText = stringResource(R.string.trash_delete_dialog_confirm),
            onConfirm = {
                showConfirmDeletePermanentlyDialog = false
                viewModel.deletePermanentlySelected(selectionState.selectedIds)
            },
            onDismiss = { showConfirmDeletePermanentlyDialog = false }
        )
    }

    if (showConfirmEmptyTrashDialog) {
        ConfirmDialog(
            title = stringResource(R.string.trash_empty_dialog_title),
            text = stringResource(R.string.trash_empty_dialog_text),
            confirmButtonText = stringResource(R.string.trash_empty_button),
            onConfirm = {
                showConfirmEmptyTrashDialog = false
                viewModel.emptyTrash()
            },
            onDismiss = { showConfirmEmptyTrashDialog = false }
        )
    }

    Scaffold(
        topBar = {
            AnimatedContent(
                targetState = selectionState.isSelectionMode,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "trashTopBarTransition"
            ) { isSelection ->
                if (isSelection) {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.gallery_selected_count_fmt, selectionState.selectedIds.size),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectionState.clearSelection() }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.gallery_exit_selection_desc),
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { viewModel.restoreSelected(selectionState.selectedIds) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Restore,
                                    contentDescription = stringResource(R.string.trash_restore_selected_desc),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { showConfirmDeletePermanentlyDialog = true }) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteForever,
                                    contentDescription = stringResource(R.string.trash_delete_permanently_desc),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                } else {
                    TopAppBar(
                        title = {
                            Text(
                                text = stringResource(R.string.trash_title),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        },
                        actions = {
                            if (trashItems.isNotEmpty()) {
                                TextButton(
                                    onClick = { showConfirmEmptyTrashDialog = true },
                                    colors = ButtonDefaults.textButtonColors(
                                        contentColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text(stringResource(R.string.trash_empty_button))
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (expiredItems.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spaceMedium, vertical = Dimens.spaceMediumSmall),
                    shape = RoundedCornerShape(Dimens.cornerLarge),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.spaceMedium + 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier.size(Dimens.iconMedium),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = stringResource(R.string.trash_purge_banner_fmt, expiredItems.size),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(start = Dimens.spaceMediumSmall + 2.dp)
                            )
                        }
                        Button(
                            onClick = { viewModel.purgeExpiredItems() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(stringResource(R.string.trash_purge_now_button))
                        }
                    }
                }
            }

            if (trashItems.isEmpty()) {
                EmptyStates.Trash(modifier = Modifier.weight(1f))
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
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
            .crossfade(200)
            .build()
    }

    val daysLeft = remember(item.purgeAt) {
        val remainingMs = item.purgeAt - System.currentTimeMillis()
        val days = (remainingMs / (1000 * 60 * 60 * 24)).coerceAtLeast(0)
        "$days d"
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        label = "trashTilePress"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
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

        // Countdown Days Left Pill
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(5.dp),
            shape = RoundedCornerShape(6.dp),
            color = Color.Black.copy(alpha = 0.72f)
        ) {
            Text(
                text = daysLeft,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        // Selection Overlay & Checkmark
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                        else Color.Black.copy(alpha = 0.15f)
                    )
            ) {
                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn() + scaleIn(initialScale = 0.6f),
                    exit = fadeOut() + scaleOut(targetScale = 0.6f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(24.dp)
                            .background(Color.White, CircleShape)
                    )
                }

                if (!isSelected) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.35f))
                    )
                }
            }
        }
    }
}
