package com.aryan.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.aryan.gallery.data.media.MediaStoreDataSource
import com.aryan.gallery.data.remote.PendingRemoteRequestStore
import com.aryan.gallery.data.trash.TrashRepository
import com.aryan.gallery.ui.navigation.GalleryNavigation
import com.aryan.gallery.ui.permissions.PermissionContainer
import com.aryan.gallery.ui.remote.PendingRemoteConfirmDialog
import com.aryan.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity app with true edge-to-edge Compose rendering, Hilt DI,
 * runtime permissions gating, and remote command confirmation dialogs.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var pendingRemoteRequestStore: PendingRemoteRequestStore

    @Inject
    lateinit var trashRepository: TrashRepository

    @Inject
    lateinit var mediaStoreDataSource: MediaStoreDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionContainer {
                        GalleryNavigation()
                        PendingRemoteConfirmDialog(
                            pendingStore = pendingRemoteRequestStore,
                            trashRepository = trashRepository,
                            dataSource = mediaStoreDataSource
                        )
                    }
                }
            }
        }
    }
}
