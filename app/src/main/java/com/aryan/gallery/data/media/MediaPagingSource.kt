package com.aryan.gallery.data.media

import androidx.paging.PagingSource
import androidx.paging.PagingState

/**
 * PagingSource wrapping MediaStore ContentResolver query using offset/limit pagination.
 */
class MediaPagingSource(
    private val dataSource: MediaStoreDataSource,
    private val bucketId: String? = null
) : PagingSource<Int, MediaItem>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MediaItem> {
        return try {
            val offset = params.key ?: 0
            val loadSize = params.loadSize
            val items = dataSource.getMediaItems(limit = loadSize, offset = offset, bucketId = bucketId)

            val prevKey = if (offset == 0) null else maxOf(0, offset - loadSize)
            val nextKey = if (items.isEmpty() || items.size < loadSize) null else offset + items.size

            LoadResult.Page(
                data = items,
                prevKey = prevKey,
                nextKey = nextKey
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, MediaItem>): Int? {
        return state.anchorPosition?.let { anchor ->
            val page = state.closestPageToPosition(anchor)
            page?.prevKey?.plus(state.config.pageSize) ?: page?.nextKey?.minus(state.config.pageSize)
        }
    }
}
