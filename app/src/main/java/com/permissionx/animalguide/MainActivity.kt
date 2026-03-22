package com.permissionx.animalguide

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.permissionx.animalguide.ui.navigation.AppNavGraph
import com.permissionx.animalguide.ui.theme.AnimalGuideTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AnimalGuideTheme {
                AppNavGraph()
            }
        }
    }
}