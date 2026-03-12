package com.methodica.app.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColorScheme = lightColorScheme(
    primary   = MethodicaPurple,
    secondary = MethodicaTeal,
    tertiary  = MethodicaPurpleLight
)

private val DarkColorScheme = darkColorScheme(
    primary   = MethodicaPurpleLight,
    secondary = MethodicaTeal
)

@Composable
fun MethodicaTheme(
    darkTheme:    Boolean = isSystemInDarkTheme(),
    // Dynamic color uses the Android 12+ wallpaper-based palette when available
    dynamicColor: Boolean = true,
    content:      @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else      -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = MethodicaTypography,
        content     = content
    )
}
