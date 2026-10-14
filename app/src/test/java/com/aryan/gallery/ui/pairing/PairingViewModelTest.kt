package com.aryan.gallery.ui.pairing

import com.aryan.gallery.data.remote.PairingException
import com.aryan.gallery.data.remote.PairingRepository
import com.aryan.gallery.data.remote.models.PairingExchangeResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

import com.aryan.gallery.data.local.RemoteAccessPreferences

@OptIn(ExperimentalCoroutinesApi::class)
class PairingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val pairingRepository: PairingRepository = mock()
    private val remoteAccessPreferences: RemoteAccessPreferences = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(remoteAccessPreferences.serverBaseUrl).thenReturn("http://10.0.2.2:8080")
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `submitPairing with invalid length sets error`() = runTest {
        val viewModel = PairingViewModel(pairingRepository, remoteAccessPreferences)
        viewModel.updateCode("123")
        viewModel.submitPairing()

        assertTrue(viewModel.uiState.value is PairingUiState.Error)
    }

    @Test
    fun `submitPairing on InvalidCode failure sets clear invalid code error message`() = runTest {
        whenever(pairingRepository.exchangeCode(any(), any()))
            .thenReturn(Result.failure(PairingException.InvalidCode("Invalid or expired code")))

        val viewModel = PairingViewModel(pairingRepository, remoteAccessPreferences)
        viewModel.updateCode("123456")
        viewModel.submitPairing()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is PairingUiState.Error)
        val errorState = viewModel.uiState.value as PairingUiState.Error
        assertTrue(errorState.message.contains("invalid or has expired"))
    }

    @Test
    fun `submitPairing on RateLimited failure sets 429 rate limit error message`() = runTest {
        whenever(pairingRepository.exchangeCode(any(), any()))
            .thenReturn(Result.failure(PairingException.RateLimited("Rate limit exceeded")))

        val viewModel = PairingViewModel(pairingRepository, remoteAccessPreferences)
        viewModel.updateCode("123456")
        viewModel.submitPairing()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is PairingUiState.Error)
        val errorState = viewModel.uiState.value as PairingUiState.Error
        assertTrue(errorState.message.contains("HTTP 429"))
    }

    @Test
    fun `submitPairing double submission is guarded and only fires exchangeCode once`() = runTest {
        whenever(pairingRepository.exchangeCode(any(), any()))
            .thenReturn(Result.success(PairingExchangeResponse("token", "id")))

        val viewModel = PairingViewModel(pairingRepository, remoteAccessPreferences)
        viewModel.updateCode("123456")

        // First submit call
        viewModel.submitPairing()
        // Concurrent second submit call before first call completes
        viewModel.submitPairing()

        testDispatcher.scheduler.advanceUntilIdle()

        // Verify exchangeCode was called exactly ONCE
        verify(pairingRepository, times(1)).exchangeCode(any(), any())
        assertEquals(PairingUiState.Success, viewModel.uiState.value)
    }
}
