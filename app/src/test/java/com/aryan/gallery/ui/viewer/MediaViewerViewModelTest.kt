package com.aryan.gallery.ui.viewer

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.aryan.gallery.data.media.MediaItem
import com.aryan.gallery.data.media.MediaType
import com.aryan.gallery.data.media.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class MediaViewerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val repository: MediaRepository = mock()
    private val mockUri: Uri = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadMediaList correctly finds index past position 500 in large library`() = runTest {
        // Generate a 600 item media list (IDs 1L..600L)
        val largeList = (1..600).map { id ->
            MediaItem(
                id = id.toLong(),
                uri = mockUri,
                displayName = "Photo $id",
                dateAdded = 1000L * id,
                mimeType = "image/jpeg",
                size = 1024L,
                width = 1080,
                height = 1920,
                bucketId = "camera",
                bucketDisplayName = "Camera",
                mediaType = MediaType.IMAGE
            )
        }

        whenever(repository.getMediaItemsList(limit = Int.MAX_VALUE, offset = 0, bucketId = null))
            .thenReturn(largeList)

        val savedStateHandle = SavedStateHandle(mapOf("mediaId" to 520L))
        val viewModel = MediaViewerViewModel(repository, savedStateHandle)

        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(600, viewModel.items.value.size)
        // Item ID 520 is at index 519 in 0-indexed list
        assertEquals(519, viewModel.initialIndex.value)
    }
}
