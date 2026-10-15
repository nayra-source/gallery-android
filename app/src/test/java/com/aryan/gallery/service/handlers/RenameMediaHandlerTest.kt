package com.aryan.gallery.service.handlers

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.aryan.gallery.data.media.MediaItem
import com.aryan.gallery.data.media.MediaOverrideDao
import com.aryan.gallery.data.media.MediaType
import com.aryan.gallery.data.media.MediaStoreDataSource
import com.aryan.gallery.data.media.MediaStoreObserver
import com.aryan.gallery.service.DeviceCommandEnvelope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RenameMediaHandlerTest {

    private val context: Context = mock()
    private val contentResolver: ContentResolver = mock()
    private val dataSource: MediaStoreDataSource = mock()
    private val mediaOverrideDao: MediaOverrideDao = mock()
    private val mediaStoreObserver: MediaStoreObserver = mock()
    private val mockUri: Uri = mock()
    private val json = Json { ignoreUnknownKeys = true }

    private lateinit var handler: RenameMediaHandler

    @Before
    fun setUp() {
        whenever(context.contentResolver).thenReturn(contentResolver)
        handler = RenameMediaHandler(
            context,
            dataSource,
            mediaOverrideDao,
            mediaStoreObserver,
            json
        )
    }

    @Test
    fun `handleCommand records name override and notifies observer`() = runTest {
        val mockItem = MediaItem(
            id = 202L,
            uri = mockUri,
            displayName = "old_photo_name.jpg",
            dateAdded = System.currentTimeMillis(),
            mimeType = "image/jpeg",
            size = 1024L,
            width = 800,
            height = 600,
            bucketId = "camera",
            bucketDisplayName = "Camera",
            durationMs = 0L,
            mediaType = MediaType.IMAGE
        )
        whenever(dataSource.getMediaItemById(202L)).thenReturn(mockItem)

        val envelope = DeviceCommandEnvelope(
            cmdId = "test_cmd_2",
            type = "RENAME_MEDIA",
            payload = json.encodeToString(RenameMediaPayload(mediaId = 202L, newName = "new_photo_name.jpg"))
        )

        val result = handler.handleCommand(envelope)

        assertEquals("test_cmd_2", result.cmdId)
        assertEquals("ok", result.status)
        assertNotNull(result.result)
        verify(mediaOverrideDao).insertOrUpdate(any())
        verify(mediaStoreObserver).notifyMediaChanged()
    }
}
