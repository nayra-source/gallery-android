package com.aryan.gallery.data.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import com.aryan.gallery.data.trash.TrashDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for querying local media (photos and videos) from MediaStore via ContentResolver.
 * Automatically excludes trashed items and applies display name overrides from local Room DB.
 */
@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trashDao: TrashDao,
    private val mediaOverrideDao: MediaOverrideDao
) {

    private val projection = arrayOf(
        MediaStore.Files.FileColumns._ID,
        MediaStore.Files.FileColumns.DISPLAY_NAME,
        MediaStore.Files.FileColumns.DATE_TAKEN,
        MediaStore.Files.FileColumns.DATE_ADDED,
        MediaStore.Files.FileColumns.MIME_TYPE,
        MediaStore.Files.FileColumns.SIZE,
        MediaStore.Files.FileColumns.WIDTH,
        MediaStore.Files.FileColumns.HEIGHT,
        MediaStore.Files.FileColumns.BUCKET_ID,
        MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
        MediaStore.Files.FileColumns.MEDIA_TYPE,
        MediaStore.Video.VideoColumns.DURATION
    )

    /**
     * Queries MediaStore for paged photos and videos sorted newest-first.
     *
     * Note on Date Column Selection:
     * DATE_ADDED is populated in seconds by the OS MediaStore provider for all files,
     * whereas DATE_TAKEN (milliseconds) is extracted from EXIF headers and can be 0 or null
     * for downloads or non-camera images. Query sorting uses DATE_ADDED descending for
     * reliable ordering across all API 33-35 devices.
     */
    suspend fun getMediaItems(
        limit: Int,
        offset: Int,
        bucketId: String? = null
    ): List<MediaItem> = withContext(Dispatchers.IO) {
        val trashedIds = trashDao.getTrashedMediaIds().toSet()
        val nameOverrides = mediaOverrideDao.getAllOverrides().associateBy { it.mediaId }
        val mediaItems = mutableListOf<MediaItem>()
        val collection = MediaStore.Files.getContentUri("external")

        val selectionParts = mutableListOf(
            "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        )
        val selectionArgs = mutableListOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        if (!bucketId.isNullOrEmpty()) {
            selectionParts.add("${MediaStore.Files.FileColumns.BUCKET_ID} = ?")
            selectionArgs.add(bucketId)
        }

        val queryArgs = Bundle().apply {
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
            putInt(ContentResolver.QUERY_ARG_OFFSET, offset)
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Files.FileColumns.DATE_ADDED)
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
            putString(
                ContentResolver.QUERY_ARG_SQL_SELECTION,
                selectionParts.joinToString(" AND ")
            )
            putStringArray(
                ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS,
                selectionArgs.toTypedArray()
            )
        }

        context.contentResolver.query(collection, projection, queryArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
            val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
            val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketDisplayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                if (trashedIds.contains(id)) continue

                val rawDisplayName = cursor.getString(displayNameColumn) ?: ""
                val displayName = nameOverrides[id]?.customDisplayName ?: rawDisplayName
                val dateTaken = cursor.getLong(dateTakenColumn)
                val dateAdded = cursor.getLong(dateAddedColumn)
                val mimeType = cursor.getString(mimeTypeColumn) ?: ""
                val size = cursor.getLong(sizeColumn)
                val width = cursor.getInt(widthColumn)
                val height = cursor.getInt(heightColumn)
                val bId = cursor.getString(bucketIdColumn) ?: ""
                val bDisplayName = cursor.getString(bucketDisplayNameColumn) ?: "Uncategorized"
                val rawType = cursor.getInt(mediaTypeColumn)
                val duration = cursor.getLong(durationColumn)

                val isVideo = rawType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
                val baseUri = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                val contentUri = ContentUris.withAppendedId(baseUri, id)

                val effectiveDate = if (dateTaken > 0) dateTaken else dateAdded * 1000L

                mediaItems.add(
                    MediaItem(
                        id = id,
                        uri = contentUri,
                        displayName = displayName,
                        dateAdded = effectiveDate,
                        mimeType = mimeType,
                        size = size,
                        width = width,
                        height = height,
                        bucketId = bId,
                        bucketDisplayName = bDisplayName,
                        durationMs = if (isVideo) duration else 0L,
                        mediaType = mediaType
                    )
                )
            }
        }

        mediaItems
    }

    /**
     * Queries MediaStore for a single MediaItem by its _ID.
     * Returns null if the item with the given ID does not exist or has been deleted.
     */
    suspend fun getMediaItemById(mediaId: Long): MediaItem? = withContext(Dispatchers.IO) {
        try {
            val trashedIds = trashDao.getTrashedMediaIds().toSet()
            if (trashedIds.contains(mediaId)) return@withContext null

            val override = mediaOverrideDao.getOverride(mediaId)

            val collection = MediaStore.Files.getContentUri("external")
            val selection = "${MediaStore.Files.FileColumns._ID} = ?"
            val selectionArgs = arrayOf(mediaId.toString())

            val queryArgs = Bundle().apply {
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
                putInt(ContentResolver.QUERY_ARG_LIMIT, 1)
            }

            context.contentResolver.query(collection, projection, queryArgs, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val dateTakenColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_TAKEN)
                    val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                    val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                    val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH)
                    val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT)
                    val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
                    val bucketDisplayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
                    val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.VideoColumns.DURATION)

                    val id = cursor.getLong(idColumn)
                    val rawDisplayName = cursor.getString(displayNameColumn) ?: ""
                    val displayName = override?.customDisplayName ?: rawDisplayName
                    val dateTaken = cursor.getLong(dateTakenColumn)
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val mimeType = cursor.getString(mimeTypeColumn) ?: ""
                    val size = cursor.getLong(sizeColumn)
                    val width = cursor.getInt(widthColumn)
                    val height = cursor.getInt(heightColumn)
                    val bId = cursor.getString(bucketIdColumn) ?: ""
                    val bDisplayName = cursor.getString(bucketDisplayNameColumn) ?: "Uncategorized"
                    val rawType = cursor.getInt(mediaTypeColumn)
                    val duration = cursor.getLong(durationColumn)

                    val isVideo = rawType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                    val mediaType = if (isVideo) MediaType.VIDEO else MediaType.IMAGE
                    val baseUri = if (isVideo) {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    }
                    val contentUri = ContentUris.withAppendedId(baseUri, id)

                    val effectiveDate = if (dateTaken > 0) dateTaken else dateAdded * 1000L

                    return@withContext MediaItem(
                        id = id,
                        uri = contentUri,
                        displayName = displayName,
                        dateAdded = effectiveDate,
                        mimeType = mimeType,
                        size = size,
                        width = width,
                        height = height,
                        bucketId = bId,
                        bucketDisplayName = bDisplayName,
                        durationMs = if (isVideo) duration else 0L,
                        mediaType = mediaType
                    )
                }
            }
            null
        } catch (e: Throwable) {
            null
        }
    }

    /**
     * Queries MediaStore for all albums (buckets) with cover photo URI and item count.
     *
     * Note on Grouping Technique:
     * Rather than relying on SQL GROUP BY in ContentResolver.query (which can behave inconsistently
     * across custom OEM Android versions), this performs a fast single pass over the sorted cursor.
     * The first item seen per bucket becomes the cover photo (newest item), and item counts are accumulated.
     */
    suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val trashedIds = trashDao.getTrashedMediaIds().toSet()
        val albumMap = LinkedHashMap<String, AlbumData>()
        val collection = MediaStore.Files.getContentUri("external")

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE} = ?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        val queryArgs = Bundle().apply {
            putStringArray(
                ContentResolver.QUERY_ARG_SORT_COLUMNS,
                arrayOf(MediaStore.Files.FileColumns.DATE_ADDED)
            )
            putInt(
                ContentResolver.QUERY_ARG_SORT_DIRECTION,
                ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
            )
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
        }

        context.contentResolver.query(collection, projection, queryArgs, null)?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketDisplayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val mediaTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                if (trashedIds.contains(id)) continue
                val bucketId = cursor.getString(bucketIdColumn) ?: continue
                val bucketName = cursor.getString(bucketDisplayNameColumn) ?: "Uncategorized"
                val rawType = cursor.getInt(mediaTypeColumn)

                val isVideo = rawType == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                val baseUri = if (isVideo) {
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                val contentUri = ContentUris.withAppendedId(baseUri, id)

                val existing = albumMap[bucketId]
                if (existing == null) {
                    albumMap[bucketId] = AlbumData(
                        bucketId = bucketId,
                        bucketDisplayName = bucketName,
                        coverUri = contentUri,
                        count = 1
                    )
                } else {
                    existing.count++
                }
            }
        }

        albumMap.values.map { data ->
            Album(
                bucketId = data.bucketId,
                bucketDisplayName = data.bucketDisplayName,
                coverUri = data.coverUri,
                itemCount = data.count
            )
        }
    }

    private data class AlbumData(
        val bucketId: String,
        val bucketDisplayName: String,
        val coverUri: android.net.Uri,
        var count: Int
    )
}
