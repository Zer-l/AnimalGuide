package com.permissionx.animalguide.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Green800,
    onPrimary = SurfaceWhite,
    primaryContainer = Green200,
    onPrimaryContainer = Green800,

    secondary = Olive700,
    onSecondary = SurfaceWhite,
    secondaryContainer = Olive200,
    onSecondaryContainer = Olive700,

    background = SurfaceWhite,
    onBackground = Color(0xFF1A1A1A),

    surface = SurfaceWhite,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Green50,
    onSurfaceVariant = NeutralGray,

    error = ErrorRed,
    onError = SurfaceWhite,
    errorContainer = ErrorRedLight,
    onErrorContainer = ErrorRed
)

private val DarkColorScheme = darkColorScheme(
    primary = Green200,
    onPrimary = Green800,
    primaryContainer = Green600,
    onPrimaryContainer = Green50,

    secondary = Olive200,
    onSecondary = Olive700,
    secondaryContainer = Olive700,
    onSecondaryContainer = Olive200,

    background = Color(0xFF121212),
    onBackground = Color(0xFFE6E6E6),

    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFAAAAAA),

    error = ErrorRedLight,
    onError = ErrorRed
)

@Composable
fun AnimalGuideTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}