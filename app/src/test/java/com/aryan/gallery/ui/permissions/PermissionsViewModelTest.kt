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

import com.aryan.gallery.data.permissions.PermissionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class PermissionsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val context: Context = mock()
    private val permissionPreferences: PermissionPreferences = mock()
    private val permissionManager: PermissionManager = mock()
    private lateinit var viewModel: PermissionsViewModel

    private lateinit var contextCompatMock: MockedStatic<ContextCompat>
    private lateinit var activityCompatMock: MockedStatic<ActivityCompat>

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        contextCompatMock = mockStatic(ContextCompat::class.java)
        activityCompatMock = mockStatic(ActivityCompat::class.java)

        contextCompatMock.`when`<Int> { ContextCompat.checkSelfPermission(any(), any()) }
            .thenReturn(PackageManager.PERMISSION_DENIED)
        activityCompatMock.`when`<Boolean> { ActivityCompat.shouldShowRequestPermissionRationale(any(), any()) }
            .thenReturn(true)

        whenever(permissionPreferences.hasRequestedPermissions).thenReturn(false)
        whenever(permissionManager.permissionState).thenReturn(MutableStateFlow(PermissionState.NotRequested))
        viewModel = PermissionsViewModel(context, permissionPreferences, permissionManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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
