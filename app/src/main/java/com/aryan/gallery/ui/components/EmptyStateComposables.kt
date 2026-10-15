package com.aryan.gallery.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A polished, animated empty-state composable for screens with no content.
 * Fades and scales in with a subtle entrance animation.
 *
 * @param icon         Icon to display (defaults to PhotoLibrary for media screens)
 * @param title        Primary heading text
 * @param description  Optional secondary description text
 * @param actionLabel  Optional button label — if provided, renders a CTA button
 * @param onAction     Callback for the CTA button
 */
@Composable
fun GalleryEmptyState(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.PhotoLibrary,
    title: String = "No photos or videos found",
    description: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100) // tiny stagger for smoother entrance
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            if (description != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                )
            }

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

/**
 * A polished error-state composable with retry action.
 */
@Composable
fun GalleryErrorState(
    modifier: Modifier = Modifier,
    title: String = "Something went wrong",
    message: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    GalleryEmptyState(
        modifier = modifier,
        icon = Icons.Outlined.ErrorOutline,
        title = title,
        description = message,
        actionLabel = if (onRetry != null) "Retry" else null,
        onAction = onRetry,
    )
}

/**
 * Preset empty states for common screens.
 */
object EmptyStates {
    @Composable
    fun Media(modifier: Modifier = Modifier) {
        GalleryEmptyState(
            modifier = modifier,
            icon = Icons.Outlined.PhotoLibrary,
            title = "No photos or videos",
            description = "Your captured moments will appear here",
        )
    }

    @Composable
    fun Albums(modifier: Modifier = Modifier) {
        GalleryEmptyState(
            modifier = modifier,
            icon = Icons.Outlined.CollectionsBookmark,
            title = "No albums found",
            description = "Albums are organized automatically from your media folders",
        )
    }

    @Composable
    fun Trash(modifier: Modifier = Modifier) {
        GalleryEmptyState(
            modifier = modifier,
            icon = Icons.Outlined.DeleteSweep,
            title = "Trash is empty",
            description = "Deleted items will appear here for 30 days before permanent removal",
        )
    }
}
