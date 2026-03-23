package com.permissionx.animalguide.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.permissionx.animalguide.ui.camera.CameraScreen
import com.permissionx.animalguide.ui.result.ResultScreen

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = "camera") {
        composable("camera") {
            CameraScreen(navController = navController)
        }
        composable("result/{imageUri}") { backStackEntry ->
            val imageUri = backStackEntry.arguments?.getString("imageUri") ?: return@composable
            ResultScreen(imageUri = imageUri, navController = navController)
        }
    }
}