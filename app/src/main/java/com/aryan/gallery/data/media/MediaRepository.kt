package com.aryan.gallery.data.media

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val dataSource: MediaStoreDataSource
) {

    /**
     * Returns a stream of paged media items (photos and videos).
     *
     * @param bucketId Optional album filter ID.
     */
    fun getMediaPaged(bucketId: String? = null): Flow<PagingData<MediaItem>> {
        return Pager(
            config = PagingConfig(
                pageSize = 60,
                prefetchDistance = 20,
                enablePlaceholders = false
            )
        ) {
            MediaPagingSource(dataSource, bucketId)
        }.flow
    }

    /**
     * Returns a stream of albums (media buckets with covers and item counts).
     */
    fun getAlbums(): Flow<List<Album>> = flow {
        emit(dataSource.getAlbums())
    }

    /**
     * Fetches media items list directly for detail viewer navigation.
     * Defaults to Int.MAX_VALUE limit so all items in the current collection/album are loaded
     * for seamless swipe-through navigation without truncation.
     */
    suspend fun getMediaItemsList(limit: Int = Int.MAX_VALUE, offset: Int = 0, bucketId: String? = null): List<MediaItem> {
        return dataSource.getMediaItems(limit = limit, offset = offset, bucketId = bucketId)
    }

    /**
     * Looks up a single MediaItem by its ID, returning null if not found.
     */
    suspend fun getMediaItemById(mediaId: Long): MediaItem? {
        return dataSource.getMediaItemById(mediaId)
    }
}
