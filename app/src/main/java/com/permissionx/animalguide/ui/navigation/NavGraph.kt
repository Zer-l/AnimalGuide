package com.permissionx.animalguide.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.permissionx.animalguide.ui.camera.CameraScreen
import com.permissionx.animalguide.ui.history.HistoryScreen
import com.permissionx.animalguide.ui.pokedex.PokedexScreen
import com.permissionx.animalguide.ui.result.ResultScreen
import com.permissionx.animalguide.ui.pokedex.PokedexDetailScreen

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object Camera : BottomNavItem("camera", "拍摄", Icons.Default.CameraAlt)
    object Pokedex : BottomNavItem("pokedex", "图鉴", Icons.AutoMirrored.Filled.MenuBook)
    object History : BottomNavItem("history", "历史", Icons.Default.History)
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    val bottomNavItems = listOf(
        BottomNavItem.Camera,
        BottomNavItem.Pokedex,
        BottomNavItem.History
    )

    // 当前路由
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 结果页不显示底部导航
    val showBottomBar =
        currentRoute?.startsWith("result") == false && !currentRoute.startsWith("pokedex_detail")

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
                                        popUpTo("camera") { saveState = true }
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
            startDestination = BottomNavItem.Camera.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Camera.route) {
                CameraScreen(navController = navController)
            }
            composable(BottomNavItem.Pokedex.route) {
                PokedexScreen(navController = navController)
            }
            composable(BottomNavItem.History.route) {
                HistoryScreen(navController = navController)
            }
            composable("result/{imageUri}") { backStackEntry ->
                val imageUri = backStackEntry.arguments?.getString("imageUri") ?: return@composable
                ResultScreen(imageUri = imageUri, navController = navController)
            }
            composable("pokedex_detail/{animalName}") { backStackEntry ->
                val animalName =
                    backStackEntry.arguments?.getString("animalName") ?: return@composable
                PokedexDetailScreen(
                    animalName = animalName,
                    navController = navController
                )
            }
        }
    }
}