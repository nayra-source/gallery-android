package com.aryan.gallery.ui.permissions

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class PermissionsViewModelTest {

    private val context: Context = mock()
    private val permissionPreferences: PermissionPreferences = mock()
    private lateinit var viewModel: PermissionsViewModel

    private lateinit var contextCompatMock: MockedStatic<ContextCompat>
    private lateinit var activityCompatMock: MockedStatic<ActivityCompat>

    @Before
    fun setUp() {
        contextCompatMock = mockStatic(ContextCompat::class.java)
        activityCompatMock = mockStatic(ActivityCompat::class.java)

        contextCompatMock.`when`<Int> { ContextCompat.checkSelfPermission(any(), any()) }
            .thenReturn(PackageManager.PERMISSION_DENIED)
        activityCompatMock.`when`<Boolean> { ActivityCompat.shouldShowRequestPermissionRationale(any(), any()) }
            .thenReturn(true)

        whenever(permissionPreferences.hasRequestedPermissions).thenReturn(false)
        viewModel = PermissionsViewModel(context, permissionPreferences)
    }

    @After
    fun tearDown() {
        contextCompatMock.close()
        activityCompatMock.close()
    }

    @Test
    fun `initial permission state is NotRequested`() {
        assertEquals(PermissionState.NotRequested, viewModel.permissionState.value)
    }

    @Test
    fun `initial partial access banner is visible`() {
        assertTrue(viewModel.isPartialAccessBannerVisible.value)
    }

    @Test
    fun `dismissPartialAccessBanner hides banner`() {
        viewModel.dismissPartialAccessBanner()
        assertFalse(viewModel.isPartialAccessBannerVisible.value)
    }

    @Test
    fun `onPermissionResult marks permissions as requested in preferences`() {
        viewModel.onPermissionResult(null)
        verify(permissionPreferences).hasRequestedPermissions = true
    }
}
