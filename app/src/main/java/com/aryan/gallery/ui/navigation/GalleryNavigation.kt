package com.aryan.gallery.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

import android.net.Uri

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
 * Top-level navigation host for the Gallery application.
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
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == NavRoute.Gallery.route,
                        onClick = {
                            navController.navigate(NavRoute.Gallery.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Photos") },
                        label = { Text("Photos") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == NavRoute.Albums.route,
                        onClick = {
                            navController.navigate(NavRoute.Albums.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Albums") },
                        label = { Text("Albums") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == NavRoute.Trash.route,
                        onClick = {
                            navController.navigate(NavRoute.Trash.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Delete, contentDescription = "Trash") },
                        label = { Text("Trash") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == NavRoute.Settings.route,
                        onClick = {
                            navController.navigate(NavRoute.Settings.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text("Settings") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavRoute.Gallery.route,
            modifier = Modifier.padding(innerPadding)
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
