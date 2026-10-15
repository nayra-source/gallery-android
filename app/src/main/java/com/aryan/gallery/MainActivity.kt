package com.aryan.gallery

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.aryan.gallery.data.local.RemoteAccessPreferences
import com.aryan.gallery.data.local.TokenStore
import com.aryan.gallery.data.media.MediaStoreDataSource
import com.aryan.gallery.data.remote.DeviceAutoRegisterRepository
import com.aryan.gallery.data.trash.TrashRepository
import com.aryan.gallery.service.GalleryConnectionService
import com.aryan.gallery.ui.navigation.GalleryNavigation
import com.aryan.gallery.ui.permissions.PermissionContainer
import com.aryan.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single-activity app with true edge-to-edge Compose rendering and Hilt DI.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var trashRepository: TrashRepository

    @Inject
    lateinit var mediaStoreDataSource: MediaStoreDataSource

    @Inject
    lateinit var autoRegisterRepository: DeviceAutoRegisterRepository

    @Inject
    lateinit var tokenStore: TokenStore

    @Inject
    lateinit var remoteAccessPreferences: RemoteAccessPreferences

    @Inject
    lateinit var permissionManager: com.aryan.gallery.data.permissions.PermissionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        lifecycleScope.launch(Dispatchers.IO) {
            permissionManager.recoverIfNeeded("MainActivity.onCreate")
        }
        setContent {
            GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionContainer {
                        GalleryNavigation()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch(Dispatchers.IO) {
            permissionManager.recoverIfNeeded("MainActivity.onResume")
        }
    }
}
