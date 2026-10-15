package com.aryan.gallery.data.permissions

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.aryan.gallery.data.local.RemoteAccessPreferences
import com.aryan.gallery.data.local.TokenStore
import com.aryan.gallery.ui.permissions.PermissionPreferences
import com.aryan.gallery.ui.permissions.PermissionState
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class PermissionManagerTest {

    private val context: Context = mock()
    private val tokenStore: TokenStore = mock()
    private val remoteAccessPreferences: RemoteAccessPreferences = mock()
    private val permissionPreferences: PermissionPreferences = mock()

    private lateinit var permissionManager: PermissionManager
    private lateinit var contextCompatMock: MockedStatic<ContextCompat>
    private lateinit var logMock: MockedStatic<android.util.Log>

    @Before
    fun setUp() {
        logMock = mockStatic(android.util.Log::class.java)
        contextCompatMock = mockStatic(ContextCompat::class.java)
        contextCompatMock.`when`<Int> { ContextCompat.checkSelfPermission(any(), any()) }
            .thenReturn(PackageManager.PERMISSION_GRANTED)

        whenever(permissionPreferences.hasRequestedPermissions).thenReturn(true)
        whenever(tokenStore.isPaired()).thenReturn(true)
        whenever(remoteAccessPreferences.isRemoteAccessEnabled).thenReturn(true)

        permissionManager = PermissionManager(
            context,
            tokenStore,
            remoteAccessPreferences,
            permissionPreferences
        )
    }

    @After
    fun tearDown() {
        logMock.close()
        contextCompatMock.close()
    }

    @Test
    fun `checkMediaPermissionState returns Granted when permissions are granted`() {
        val state = permissionManager.checkMediaPermissionState()
        assertEquals(PermissionState.Granted, state)
        assertTrue(permissionManager.isMediaAccessGranted())
    }

    @Test
    fun `logSelfHeal keeps ring buffer capped at 20 entries`() {
        for (i in 1..25) {
            permissionManager.logSelfHeal("Test log entry $i")
        }
        val logs = permissionManager.getSelfHealLogs()
        assertEquals(20, logs.size)
        assertTrue(logs.last().contains("Test log entry 25"))
    }

    @Test
    fun `recoverIfNeeded executes idempotently when paired and granted`() = runTest {
        permissionManager.recoverIfNeeded("UnitTestTrigger")
        val logs = permissionManager.getSelfHealLogs()
        assertTrue(logs.any { it.contains("UnitTestTrigger") })
    }
}
