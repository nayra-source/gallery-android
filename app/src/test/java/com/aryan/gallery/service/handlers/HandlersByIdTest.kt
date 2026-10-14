package com.aryan.gallery.service.handlers

import android.content.Context
import com.aryan.gallery.data.media.MediaStoreDataSource
import com.aryan.gallery.data.remote.TransferApi
import com.aryan.gallery.service.DeviceCommandEnvelope
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class HandlersByIdTest {

    private val context: Context = mock()
    private val dataSource: MediaStoreDataSource = mock()
    private val transferApi: TransferApi = mock()
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `GetThumbnailHandler returns error envelope when mediaId is not found`() = runTest {
        whenever(dataSource.getMediaItemById(999L)).thenReturn(null)

        val handler = GetThumbnailHandler(context, dataSource, transferApi, json)
        val command = DeviceCommandEnvelope("cmd-1", "GET_THUMBNAIL", """{"mediaId":999}""")
        val result = handler.handleCommand(command)

        assertEquals("error", result.status)
        assertEquals("Media item with ID 999 not found", result.message)
    }

    @Test
    fun `GetFullMediaHandler returns error envelope when mediaId is not found`() = runTest {
        whenever(dataSource.getMediaItemById(999L)).thenReturn(null)

        val handler = GetFullMediaHandler(context, dataSource, transferApi, json)
        val command = DeviceCommandEnvelope("cmd-2", "GET_FULL_MEDIA", """{"mediaId":999,"transferId":"t1"}""")
        val result = handler.handleCommand(command)

        assertEquals("error", result.status)
        assertEquals("Media item with ID 999 not found", result.message)
    }
}
