package com.aryan.gallery.data.remote

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

data class PendingDeleteRequest(
    val id: String = UUID.randomUUID().toString(),
    val mediaIds: List<Long>,
    val timestamp: Long = System.currentTimeMillis()
)

data class PendingRenameRequest(
    val id: String = UUID.randomUUID().toString(),
    val mediaId: Long,
    val newName: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Singleton store tracking remote operations requiring in-app user confirmation.
 */
@Singleton
class PendingRemoteRequestStore @Inject constructor() {

    private val _pendingDelete = MutableStateFlow<PendingDeleteRequest?>(null)
    val pendingDelete: StateFlow<PendingDeleteRequest?> = _pendingDelete.asStateFlow()

    private val _pendingRename = MutableStateFlow<PendingRenameRequest?>(null)
    val pendingRename: StateFlow<PendingRenameRequest?> = _pendingRename.asStateFlow()

    fun stageDelete(mediaIds: List<Long>): PendingDeleteRequest {
        val request = PendingDeleteRequest(mediaIds = mediaIds)
        _pendingDelete.value = request
        return request
    }

    fun stageRename(mediaId: Long, newName: String): PendingRenameRequest {
        val request = PendingRenameRequest(mediaId = mediaId, newName = newName)
        _pendingRename.value = request
        return request
    }

    fun clearDelete() {
        _pendingDelete.value = null
    }

    fun clearRename() {
        _pendingRename.value = null
    }
}
