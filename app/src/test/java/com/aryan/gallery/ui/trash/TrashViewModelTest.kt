package com.aryan.gallery.ui.trash

import android.content.IntentSender
import android.net.Uri
import com.aryan.gallery.data.trash.TrashEntity
import com.aryan.gallery.data.trash.TrashRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TrashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val trashRepository: TrashRepository = mock()
    private val mockIntentSender: IntentSender = mock()
    private val mockUri: Uri = mock()

    private lateinit var uriMock: MockedStatic<Uri>

    private val sampleEntity = TrashEntity(
        mediaId = 100L,
        displayName = "photo.jpg",
        uriString = "content://media/external/images/media/100",
        mediaType = "IMAGE",
        trashedAt = System.currentTimeMillis() - 31L * 24 * 60 * 60 * 1000,
        purgeAt = System.currentTimeMillis() - 1L * 24 * 60 * 60 * 1000
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        uriMock = mockStatic(Uri::class.java)
        uriMock.`when`<Uri> { Uri.parse(any()) }.thenReturn(mockUri)
        whenever(trashRepository.trashItemsFlow).thenReturn(flowOf(listOf(sampleEntity)))
    }

    @After
    fun tearDown() {
        uriMock.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `checkExpiredItems loads expired trash items`() = runTest {
        whenever(trashRepository.getExpiredTrashItems()).thenReturn(listOf(sampleEntity))

        val viewModel = TrashViewModel(trashRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(1, viewModel.expiredItems.value.size)
        assertEquals(100L, viewModel.expiredItems.value.first().mediaId)
    }

    @Test
    fun `restoreSelected triggers intent sender and sets pending state`() = runTest {
        whenever(trashRepository.createTrashRequestIntentSender(any(), eq(false)))
            .thenReturn(mockIntentSender)

        val viewModel = TrashViewModel(trashRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.restoreSelected(setOf(100L))
        assertEquals(mockIntentSender, viewModel.pendingIntentSender.value)
    }

    @Test
    fun `onIntentSenderResultSuccess deletes metadata and resets pending state`() = runTest {
        whenever(trashRepository.createTrashRequestIntentSender(any(), eq(false)))
            .thenReturn(mockIntentSender)

        val viewModel = TrashViewModel(trashRepository)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.restoreSelected(setOf(100L))
        viewModel.onIntentSenderResultSuccess()
        testDispatcher.scheduler.advanceUntilIdle()

        verify(trashRepository).removeTrashMetadata(listOf(100L))
        assertNull(viewModel.pendingIntentSender.value)
    }
}
