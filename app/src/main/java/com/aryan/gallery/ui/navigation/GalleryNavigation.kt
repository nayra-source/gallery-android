package com.aryan.gallery.ui.navigation

import android.net.Uri
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aryan.gallery.ui.album.AlbumDetailScreen
import com.aryan.gallery.ui.album.AlbumGridScreen
import com.aryan.gallery.ui.gallery.GalleryScreen
import com.aryan.gallery.ui.pairing.PairingScreen
import com.aryan.gallery.ui.settings.SettingsScreen
import com.aryan.gallery.ui.trash.TrashScreen
import com.aryan.gallery.ui.viewer.MediaViewerScreen

sealed class NavRoute(val route: String) {
    data object Gallery : NavRoute("gallery")
    data object Albums : NavRoute("albums")
    data object Trash : NavRoute("trash")
    data object Settings : NavRoute("settings")
    data object Pairing : NavRoute("pairing")

    data object AlbumDetail : NavRoute("album_detail/{bucketId}/{bucketName}") {
        fun createRoute(bucketId: String, bucketName: String): String =
            "album_detail/${Uri.encode(bucketId)}/${Uri.encode(bucketName)}"
    }

    data object Viewer : NavRoute("viewer/{mediaId}?bucketId={bucketId}") {
        fun createRoute(mediaId: Long, bucketId: String? = null): String {
            return if (!bucketId.isNullOrEmpty()) {
                "viewer/$mediaId?bucketId=$bucketId"
            } else {
                "viewer/$mediaId"
            }
        }
    }
}

/**
 * Top-level navigation host for the Gallery application with:
 * - Fluid screen entry/exit slide & fade transitions
 * - Filled vs outlined iconography state for bottom tabs
 * - Tonal surface elevation on NavigationBar
 */
@Composable
fun GalleryNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in listOf(
        NavRoute.Gallery.route,
        NavRoute.Albums.route,
        NavRoute.Trash.route,
        NavRoute.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp
                ) {
                    val isPhotosSelected = currentRoute == NavRoute.Gallery.route
                    NavigationBarItem(
                        selected = isPhotosSelected,
                        onClick = {
                            navController.navigate(NavRoute.Gallery.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isPhotosSelected) Icons.Filled.PhotoLibrary else Icons.Outlined.PhotoLibrary,
                                contentDescription = "Photos"
                            )
                        },
                        label = { Text("Photos") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    val isAlbumsSelected = currentRoute == NavRoute.Albums.route
                    NavigationBarItem(
                        selected = isAlbumsSelected,
                        onClick = {
                            navController.navigate(NavRoute.Albums.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isAlbumsSelected) Icons.Filled.CollectionsBookmark else Icons.Outlined.CollectionsBookmark,
                                contentDescription = "Albums"
                            )
                        },
                        label = { Text("Albums") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    val isTrashSelected = currentRoute == NavRoute.Trash.route
                    NavigationBarItem(
                        selected = isTrashSelected,
                        onClick = {
                            navController.navigate(NavRoute.Trash.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isTrashSelected) Icons.Filled.Delete else Icons.Outlined.Delete,
                                contentDescription = "Trash"
                            )
                        },
                        label = { Text("Trash") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    val isSettingsSelected = currentRoute == NavRoute.Settings.route
                    NavigationBarItem(
                        selected = isSettingsSelected,
                        onClick = {
                            navController.navigate(NavRoute.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (isSettingsSelected) Icons.Filled.Settings else Icons.Outlined.Settings,
                                contentDescription = "Settings"
                            )
                        },
                        label = { Text("Settings") },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Gallery.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(220)) +
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(220))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(180)) +
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Start, tween(180))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(220)) +
                    slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(220))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(180)) +
                    slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.End, tween(180))
            }
        ) {
            composable(NavRoute.Gallery.route) {
                GalleryScreen(
                    onNavigateToViewer = { mediaId, bucketId ->
                        navController.navigate(NavRoute.Viewer.createRoute(mediaId, bucketId))
                    }
                )
            }

            composable(NavRoute.Albums.route) {
                AlbumGridScreen(
                    onAlbumClick = { bucketId, bucketName ->
                        navController.navigate(NavRoute.AlbumDetail.createRoute(bucketId, bucketName))
                    }
                )
            }

            composable(NavRoute.Trash.route) {
                TrashScreen()
            }

            composable(NavRoute.Settings.route) {
                SettingsScreen(
                    onNavigateToPairing = {
                        navController.navigate(NavRoute.Pairing.route)
                    }
                )
            }

            composable(NavRoute.Pairing.route) {
                PairingScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPairingSuccess = { navController.popBackStack() }
                )
            }

            composable(
                route = NavRoute.AlbumDetail.route,
                arguments = listOf(
                    navArgument("bucketId") { type = NavType.StringType },
                    navArgument("bucketName") { type = NavType.StringType }
                )
            ) {
                AlbumDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToViewer = { mediaId, bucketId ->
                        navController.navigate(NavRoute.Viewer.createRoute(mediaId, bucketId))
                    }
                )
            }

            composable(
                route = NavRoute.Viewer.route,
                arguments = listOf(
                    navArgument("mediaId") { type = NavType.LongType },
                    navArgument("bucketId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                MediaViewerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
