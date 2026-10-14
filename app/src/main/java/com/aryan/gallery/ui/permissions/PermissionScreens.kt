package com.aryan.gallery.ui.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aryan.gallery.ui.permissions.PermissionUtils.findActivity
import com.aryan.gallery.ui.theme.GalleryTheme

/**
 * Top-level container that evaluates runtime permission state and displays the
 * appropriate rationale, permanent denial screen, or the main gallery content with
 * a partial access banner.
 */
@Composable
fun PermissionContainer(
    viewModel: PermissionsViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val permissionState by viewModel.permissionState.collectAsState()
    val isBannerVisible by viewModel.isPartialAccessBannerVisible.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.onPermissionResult(activity)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.updatePermissionState(activity)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    when (permissionState) {
        PermissionState.NotRequested, PermissionState.Denied -> {
            PermissionRationaleScreen(
                onRequestPermission = {
                    permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
                }
            )
        }

        PermissionState.PermanentlyDenied -> {
            PermanentlyDeniedScreen(
                onOpenSettings = {
                    PermissionUtils.openAppSettings(context)
                }
            )
        }

        PermissionState.Granted -> {
            content()
        }

        PermissionState.PartiallyGranted -> {
            Column(modifier = Modifier.fillMaxSize()) {
                if (isBannerVisible) {
                    PartialAccessBanner(
                        onManageSelection = {
                            permissionLauncher.launch(PermissionUtils.getRequiredPermissions())
                        },
                        onDismiss = {
                            viewModel.dismissPartialAccessBanner()
                        }
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        }
    }
}

/**
 * Screen shown before requesting permission or after a non-permanent denial,
 * explaining why photo/video access is required in plain language.
 */
@Composable
fun PermissionRationaleScreen(
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Access Your Photos & Videos",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Gallery needs access to your photos and videos to show them here. Nothing leaves your device without your say-so.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onRequestPermission,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(text = "Grant Access")
            }
        }
    }
}

/**
 * Screen shown when permissions have been permanently denied, offering a direct link
 * to the app's System Settings page.
 */
@Composable
fun PermanentlyDeniedScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Permission Required",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Access to your photos and videos has been disabled. To view your media, please enable permission in System Settings.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text(text = "Open Settings")
            }
        }
    }
}

/**
 * Persistent-but-dismissible banner shown when the user grants partial media access
 * (Android 14+ "Select photos" state).
 */
@Composable
fun PartialAccessBanner(
    onManageSelection: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Given access to selected photos.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            TextButton(onClick = onManageSelection) {
                Text("Manage Selection")
            }
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PermissionRationaleScreenPreview() {
    GalleryTheme {
        PermissionRationaleScreen(onRequestPermission = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PermanentlyDeniedScreenPreview() {
    GalleryTheme {
        PermanentlyDeniedScreen(onOpenSettings = {})
    }
}

@Preview(showBackground = true)
@Composable
private fun PartialAccessBannerPreview() {
    GalleryTheme {
        PartialAccessBanner(onManageSelection = {}, onDismiss = {})
    }
}
