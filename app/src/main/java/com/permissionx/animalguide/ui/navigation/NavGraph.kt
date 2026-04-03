package com.permissionx.animalguide.ui.navigation

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import com.permissionx.animalguide.ui.auth.LoginScreen
import com.permissionx.animalguide.ui.auth.LoginViewModel
import com.permissionx.animalguide.ui.auth.SetPasswordScreen
import com.permissionx.animalguide.ui.me.EditProfileScreen
import com.permissionx.animalguide.ui.me.MeScreen
import com.permissionx.animalguide.ui.me.SettingsScreen
import com.permissionx.animalguide.ui.social.SocialScreen
import com.permissionx.animalguide.ui.social.detail.PostDetailScreen
import com.permissionx.animalguide.ui.social.publish.PublishScreen

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Camera : BottomNavItem(Routes.CAMERA, "拍摄", Icons.Default.CameraAlt)
    object Pokedex : BottomNavItem(Routes.POKEDEX, "图鉴", Icons.AutoMirrored.Filled.MenuBook)
    object History : BottomNavItem(Routes.HISTORY, "历史", Icons.Default.History)
    object Social : BottomNavItem(Routes.SOCIAL, "社区", Icons.Default.People)
    object Me : BottomNavItem(Routes.ME, "我的", Icons.Default.Person)
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val bottomNavItems = listOf(
        BottomNavItem.Camera,
        BottomNavItem.Pokedex,
        BottomNavItem.Social,
        BottomNavItem.Me
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute?.startsWith(Routes.RESULT_NO_PARAM) == false
            && currentRoute?.startsWith(Routes.RESULT_FROM_HISTORY) == false
            && currentRoute?.startsWith(Routes.POKEDEX_DETAIL) == false
            && currentRoute?.startsWith(Routes.HISTORY_DETAIL) == false
            && currentRoute?.startsWith(Routes.POST_DETAIL) == false
            && currentRoute?.startsWith(Routes.USER_PROFILE) == false
            && currentRoute != Routes.PUBLISH
            && currentRoute != Routes.LOGIN
            && currentRoute?.startsWith("publish") == false
            && currentRoute != Routes.SETTINGS
            && currentRoute != Routes.EDIT_PROFILE
            && currentRoute != Routes.HISTORY
            && currentRoute?.startsWith(Routes.HISTORY_DETAIL) == false
            && currentRoute?.startsWith("set_password") == false

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

            composable(route = Routes.LOGIN) {
                LoginScreen(navController = navController)
            }

            composable(route = Routes.SOCIAL) {
                SocialScreen(navController = navController)
            }

            composable(route = Routes.ME) {
                MeScreen(navController = navController)
            }

            composable(route = Routes.SEARCH_SOCIAL) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("搜索功能开发中...")
                }
            }

            composable(route = Routes.PUBLISH) {
                PublishScreen(navController = navController)
            }

            composable(route = Routes.PUBLISH_WITH_ANIMAL) { backStackEntry ->
                val animalName = backStackEntry.arguments?.getString("animalName") ?: ""
                PublishScreen(
                    navController = navController,
                    animalName = Uri.decode(animalName)
                )
            }

            composable(route = Routes.POST_DETAIL) { backStackEntry ->
                val postId = backStackEntry.arguments?.getString("postId") ?: return@composable
                PostDetailScreen(
                    postId = postId,
                    navController = navController
                )
            }

            composable(route = Routes.SETTINGS) {
                SettingsScreen(navController = navController)
            }

            composable(route = Routes.EDIT_PROFILE) {
                EditProfileScreen(navController = navController)
            }

            composable(route = Routes.SET_PASSWORD) { backStackEntry ->
                val phone = Uri.decode(
                    backStackEntry.arguments?.getString("phone") ?: ""
                )
                val verificationToken = Uri.decode(
                    backStackEntry.arguments?.getString("verificationToken") ?: ""
                )
                SetPasswordScreen(
                    phone = phone,
                    verificationToken = verificationToken,
                    navController = navController
                )
            }
        }
    }
}