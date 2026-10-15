package com.aryan.gallery.data.trash

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.provider.MediaStore
import com.aryan.gallery.config.TrashConfig
import com.aryan.gallery.data.media.MediaItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository coordinating Room trash metadata persistence and MediaStore system intent generation.
 */
@Singleton
class TrashRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trashDao: TrashDao,
    private val trashConfig: TrashConfig
) {

    val trashItemsFlow: Flow<List<TrashEntity>> = trashDao.getAllTrashItems()

    suspend fun getExpiredTrashItems(): List<TrashEntity> {
        return trashDao.getExpiredTrashItems()
    }

    /**
     * Creates an IntentSender for MediaStore.createTrashRequest.
     * @param trashed true to move to trash, false to restore from trash.
     */
    fun createTrashRequestIntentSender(uris: List<Uri>, trashed: Boolean): IntentSender {
        return MediaStore.createTrashRequest(context.contentResolver, uris, trashed).intentSender
    }

    /**
     * Creates an IntentSender for MediaStore.createDeleteRequest (permanent delete).
     */
    fun createDeleteRequestIntentSender(uris: List<Uri>): IntentSender {
        return MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
    }

    /**
     * Records trashed metadata in Room upon user confirmation of a soft-delete trash request.
     */
    suspend fun recordTrashedItems(items: List<MediaItem>) {
        val now = System.currentTimeMillis()
        val entities = items.map { item ->
            TrashEntity(
                mediaId = item.id,
                displayName = item.displayName,
                uriString = item.uri.toString(),
                mediaType = item.mediaType.name,
                durationMs = item.durationMs,
                trashedAt = now,
                purgeAt = now + trashConfig.retentionPeriodMs
            )
        }
        trashDao.insertAll(entities)
    }

    /**
     * Removes local Room trash metadata when items are restored or permanently deleted.
     */
    suspend fun removeTrashMetadata(mediaIds: List<Long>) {
        trashDao.deleteByIds(mediaIds)
    }

    /**
     * Removes all local Room trash metadata when trash is emptied.
     */
    suspend fun clearAllTrashMetadata() {
        trashDao.clearAll()
    }
}
