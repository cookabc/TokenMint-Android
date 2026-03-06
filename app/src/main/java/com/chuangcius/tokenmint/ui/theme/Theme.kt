package com.chuangcius.tokenmint.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = TokenMintAccent,
    onPrimary = Color.White,
    primaryContainer = TokenMintAccentLight,
    secondary = TokenMintAccent,
    background = TokenMintBackgroundLight,
    surface = TokenMintSurfaceLight,
    onBackground = TokenMintOnSurfaceLight,
    onSurface = TokenMintOnSurfaceLight,
    error = TokenMintError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = TokenMintAccent,
    onPrimary = Color.White,
    primaryContainer = TokenMintAccentLight,
    secondary = TokenMintAccent,
    background = TokenMintBackgroundDark,
    surface = TokenMintSurfaceDark,
    onBackground = TokenMintOnSurfaceDark,
    onSurface = TokenMintOnSurfaceDark,
    error = TokenMintError,
    onError = Color.White
)

@Composable
fun TokenMintTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = TokenMintTypography,
        content = content
    )
}
