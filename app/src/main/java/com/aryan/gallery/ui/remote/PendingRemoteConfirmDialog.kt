package com.aryan.gallery.ui.remote

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.aryan.gallery.data.media.MediaItem
import com.aryan.gallery.data.media.MediaStoreDataSource
import com.aryan.gallery.data.remote.PendingRemoteRequestStore
import com.aryan.gallery.data.trash.TrashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Top-level composable observing pending remote delete/rename requests staged by background WebSocket commands.
 * Presents confirmation dialogs and triggers IntentSender system prompts for MediaStore operations.
 */
@Composable
fun PendingRemoteConfirmDialog(
    pendingStore: PendingRemoteRequestStore,
    trashRepository: TrashRepository,
    dataSource: MediaStoreDataSource
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pendingDelete by pendingStore.pendingDelete.collectAsState()
    val pendingRename by pendingStore.pendingRename.collectAsState()

    var pendingDeleteItems by remember { mutableStateOf<List<MediaItem>>(emptyList()) }

    val deleteIntentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch {
                trashRepository.recordTrashedItems(pendingDeleteItems)
                pendingStore.clearDelete()
            }
        } else {
            pendingStore.clearDelete()
        }
    }

    val renameIntentSenderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            scope.launch(Dispatchers.IO) {
                pendingRename?.let { renameReq ->
                    runCatching {
                        val item = dataSource.getMediaItemById(renameReq.mediaId)
                        if (item != null) {
                            val values = ContentValues().apply {
                                put(MediaStore.Files.FileColumns.DISPLAY_NAME, renameReq.newName)
                            }
                            context.contentResolver.update(item.uri, values, null, null)
                        }
                    }
                }
                pendingStore.clearRename()
            }
        } else {
            pendingStore.clearRename()
        }
    }

    pendingDelete?.let { deleteReq ->
        AlertDialog(
            onDismissRequest = { pendingStore.clearDelete() },
            title = { Text("Remote Delete Requested") },
            text = {
                Text("Dashboard requested to delete ${deleteReq.mediaIds.size} photo(s). Move to Trash?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            val items = deleteReq.mediaIds.mapNotNull { id ->
                                dataSource.getMediaItemById(id)
                            }
                            if (items.isNotEmpty()) {
                                pendingDeleteItems = items
                                val uris = items.map { it.uri }
                                val intentSender = trashRepository.createTrashRequestIntentSender(uris, trashed = true)
                                withContext(Dispatchers.Main) {
                                    deleteIntentSenderLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                }
                            } else {
                                pendingStore.clearDelete()
                            }
                        }
                    }
                ) {
                    Text("Confirm Move to Trash")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStore.clearDelete() }) {
                    Text("Cancel")
                }
            }
        )
    }

    pendingRename?.let { renameReq ->
        AlertDialog(
            onDismissRequest = { pendingStore.clearRename() },
            title = { Text("Remote Rename Requested") },
            text = {
                Text("Dashboard requested to rename photo ID ${renameReq.mediaId} to '${renameReq.newName}'. Confirm?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            try {
                                val item = dataSource.getMediaItemById(renameReq.mediaId)
                                if (item != null) {
                                    val values = ContentValues().apply {
                                        put(MediaStore.Files.FileColumns.DISPLAY_NAME, renameReq.newName)
                                    }
                                    context.contentResolver.update(item.uri, values, null, null)
                                    pendingStore.clearRename()
                                } else {
                                    pendingStore.clearRename()
                                }
                            } catch (e: Exception) {
                                if (e is RecoverableSecurityException) {
                                    val intentSender = e.userAction.actionIntent.intentSender
                                    withContext(Dispatchers.Main) {
                                        renameIntentSenderLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                                    }
                                } else {
                                    pendingStore.clearRename()
                                }
                            }
                        }
                    }
                ) {
                    Text("Confirm Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingStore.clearRename() }) {
                    Text("Cancel")
                }
            }
        )
    }
}
