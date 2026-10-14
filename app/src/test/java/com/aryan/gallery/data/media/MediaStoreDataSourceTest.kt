package com.aryan.gallery.data.media

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class MediaStoreDataSourceTest {

    private val context: Context = mock()
    private val contentResolver: ContentResolver = mock()
    private val cursor: Cursor = mock()
    private val mockUri: Uri = mock()

    private lateinit var mediaStoreFilesMock: MockedStatic<MediaStore.Files>
    private lateinit var dataSource: MediaStoreDataSource

    @Before
    fun setUp() {
        mediaStoreFilesMock = mockStatic(MediaStore.Files::class.java)
        mediaStoreFilesMock.`when`<Uri> { MediaStore.Files.getContentUri(any()) }.thenReturn(mockUri)

        whenever(context.contentResolver).thenReturn(contentResolver)
        dataSource = MediaStoreDataSource(context)
    }

    @After
    fun tearDown() {
        mediaStoreFilesMock.close()
    }

    @Test
    fun `getMediaItemById returns null when cursor is empty or nonexistent id`() = runTest {
        whenever(contentResolver.query(any(), any(), any(), any())).thenReturn(cursor)
        whenever(cursor.moveToFirst()).thenReturn(false)

        val result = dataSource.getMediaItemById(9999L)

        assertNull(result)
    }

    @Test
    fun `getMediaItemById returns null when query returns null cursor`() = runTest {
        whenever(contentResolver.query(any(), any(), any(), any())).thenReturn(null)

        val result = dataSource.getMediaItemById(9999L)

        assertNull(result)
    }
}
