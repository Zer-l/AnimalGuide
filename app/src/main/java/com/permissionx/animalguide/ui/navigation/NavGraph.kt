package com.permissionx.animalguide.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.*
import androidx.navigation.compose.*
import com.permissionx.animalguide.ui.camera.CameraScreen
import com.permissionx.animalguide.ui.camera.CameraViewModel
import com.permissionx.animalguide.ui.history.HistoryDetailScreen
import com.permissionx.animalguide.ui.history.HistoryScreen
import com.permissionx.animalguide.ui.pokedex.PokedexDetailScreen
import com.permissionx.animalguide.ui.pokedex.PokedexScreen
import com.permissionx.animalguide.ui.result.ResultScreen
import androidx.core.net.toUri

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Camera : BottomNavItem(Routes.CAMERA, "拍摄", Icons.Default.CameraAlt)
    object Pokedex : BottomNavItem(Routes.POKEDEX, "图鉴", Icons.AutoMirrored.Filled.MenuBook)
    object History : BottomNavItem(Routes.HISTORY, "历史", Icons.Default.History)
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val bottomNavItems = listOf(
        BottomNavItem.Camera,
        BottomNavItem.Pokedex,
        BottomNavItem.History
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar =
        currentRoute?.startsWith(Routes.RESULT_NO_PARAM) == false && !currentRoute.startsWith(Routes.RESULT_FROM_HISTORY) && !currentRoute.startsWith(
            Routes.POKEDEX_DETAIL
        ) && !currentRoute.startsWith(Routes.HISTORY_DETAIL)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.CAMERA) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CAMERA,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(
                route = Routes.CAMERA,
                enterTransition = { fadeIn(animationSpec = tween(200)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) }
            ) { entry ->
                CameraScreen(navController = navController)
            }

            composable(
                route = Routes.POKEDEX,
                enterTransition = { fadeIn(animationSpec = tween(200)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) }
            ) {
                PokedexScreen(navController = navController)
            }

            composable(
                route = Routes.HISTORY,
                enterTransition = { fadeIn(animationSpec = tween(200)) },
                exitTransition = { fadeOut(animationSpec = tween(200)) }
            ) {
                HistoryScreen(navController = navController)
            }

            composable(
                route = Routes.RESULT_NO_PARAM,
                enterTransition = {
                    slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) +
                            fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) +
                            fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) +
                            fadeOut(animationSpec = tween(300))
                }
            ) { entry ->
                // 从 camera 路由的 backStackEntry 获取 CameraViewModel
                val cameraBackStackEntry = remember(entry) {
                    navController.getBackStackEntry(Routes.CAMERA)
                }
                val cameraViewModel: CameraViewModel = hiltViewModel(cameraBackStackEntry)
                val uri by cameraViewModel.pendingImageUri.collectAsState()

                uri?.let {
                    ResultScreen(
                        imageUri = it,
                        navController = navController,
                        onFinished = { cameraViewModel.clearPendingImageUri() }
                    )
                }
            }

            composable(
                route = Routes.POKEDEX_DETAIL,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) { backStackEntry ->
                val animalName =
                    backStackEntry.arguments?.getString("animalName") ?: return@composable
                PokedexDetailScreen(
                    animalName = animalName,
                    navController = navController
                )
            }

            composable(
                route = Routes.HISTORY_DETAIL,
                enterTransition = {
                    slideInHorizontally(
                        initialOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { -it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutHorizontally(
                        targetOffsetX = { it },
                        animationSpec = tween(300)
                    ) + fadeOut(animationSpec = tween(300))
                }
            ) { backStackEntry ->
                val historyId = backStackEntry.arguments?.getString("historyId")?.toIntOrNull()
                    ?: return@composable
                HistoryDetailScreen(
                    historyId = historyId,
                    navController = navController
                )
            }

            composable(
                route = Routes.RESULT_FROM_HISTORY,
                enterTransition = {
                    slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) +
                            fadeIn(animationSpec = tween(300))
                },
                exitTransition = {
                    slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) +
                            fadeOut(animationSpec = tween(300))
                },
                popExitTransition = {
                    slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) +
                            fadeOut(animationSpec = tween(300))
                }
            ) { backStackEntry ->
                val imageUri = backStackEntry.arguments?.getString("imageUri") ?: return@composable
                val uri = remember { imageUri.toUri() }
                ResultScreen(
                    imageUri = uri,
                    navController = navController
                )
            }
        }
    }
}