package com.aryan.gallery.ui.settings

import com.aryan.gallery.data.local.RemoteAccessPreferences
import com.aryan.gallery.data.local.TokenStore
import com.aryan.gallery.data.remote.AuthEventBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val tokenStore: TokenStore = mock()
    private val remoteAccessPreferences: RemoteAccessPreferences = mock()
    private val authEventBus = AuthEventBus()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        whenever(tokenStore.isPaired()).thenReturn(true)
        whenever(tokenStore.getDeviceId()).thenReturn("uuid-1234")
        whenever(remoteAccessPreferences.isRemoteAccessEnabled).thenReturn(true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `unpair clears tokenStore and updates state`() = runTest {
        val viewModel = SettingsViewModel(tokenStore, remoteAccessPreferences, authEventBus)
        whenever(tokenStore.isPaired()).thenReturn(false)

        viewModel.unpair()

        verify(tokenStore).clear()
        assertFalse(viewModel.isPaired.value)
    }

    @Test
    fun `authEventBus unauthorized event clears pairing state`() = runTest {
        val viewModel = SettingsViewModel(tokenStore, remoteAccessPreferences, authEventBus)
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.isPaired.value)

        whenever(tokenStore.isPaired()).thenReturn(false)
        authEventBus.emitUnauthorized()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.isPaired.value)
    }
}
