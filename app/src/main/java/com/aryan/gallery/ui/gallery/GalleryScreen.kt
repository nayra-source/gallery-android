package com.aryan.gallery.ui.gallery

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import com.aryan.gallery.R
import com.aryan.gallery.data.media.MediaItem
import com.aryan.gallery.ui.theme.Dimens

/**
 * Main Gallery Screen displaying a paged grid of photos and videos.
 * Features:
 * - Material 3 TopAppBar titled 'Gallery' with search and grid view actions
 * - Horizontal filter chips row ('Today', 'Yesterday', 'Videos', 'Favorites')
 * - Animated TopAppBar transition into multi-selection mode
 * - Lifecycle-aware state observation and clean edge-to-edge layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    onNavigateToViewer: (mediaId: Long, bucketId: String?) -> Unit,
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val lazyPagingItems = viewModel.mediaFlow.collectAsLazyPagingItems()
    val selectionState = viewModel.selectionState
    val pendingIntentSender by viewModel.pendingIntentSender.collectAsStateWithLifecycle()

    val todayStr = stringResource(R.string.gallery_filter_today)
    val yesterdayStr = stringResource(R.string.gallery_filter_yesterday)
    val videosStr = stringResource(R.string.gallery_filter_videos)
    val favoritesStr = stringResource(R.string.gallery_filter_favorites)

    var selectedFilterChip by remember { mutableStateOf(todayStr) }
    val filterChips = remember(todayStr, yesterdayStr, videosStr, favoritesStr) {
        listOf(todayStr, yesterdayStr, videosStr, favoritesStr)
    }

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
            AnimatedContent(
                targetState = selectionState.isSelectionMode,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "topAppBarTransition"
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
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.gallery_delete_selected_desc),
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
                                text = stringResource(R.string.gallery_title),
                                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        },
                        actions = {
                            IconButton(onClick = { /* Search action */ }) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = stringResource(R.string.gallery_search_desc),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { /* Grid view options */ }) {
                                Icon(
                                    imageVector = Icons.Outlined.GridView,
                                    contentDescription = stringResource(R.string.gallery_view_options_desc),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
            // Horizontal Filter Chips Row
            if (!selectionState.isSelectionMode) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.spaceLarge, vertical = Dimens.spaceSmall + 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.spaceMediumSmall)
                ) {
                    items(filterChips) { chip ->
                        val isSelected = selectedFilterChip == chip
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFilterChip = chip },
                            label = {
                                Text(
                                    text = chip,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            shape = RoundedCornerShape(Dimens.cornerLarge),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                selectedLabelColor = MaterialTheme.colorScheme.primary,
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                enabled = true,
                                selected = isSelected
                            )
                        )
                    }
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
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
}
