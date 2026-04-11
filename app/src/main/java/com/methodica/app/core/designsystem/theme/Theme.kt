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
    primary = ScholarlyPrimary,
    onPrimary = ScholarlyOnPrimary,
    primaryContainer = ScholarlyPrimaryContainer,
    onPrimaryContainer = ScholarlyOnPrimaryContainer,
    secondary = ScholarlySecondary,
    onSecondary = ScholarlyOnSecondary,
    tertiary = ScholarlyTertiary,
    onTertiary = ScholarlyOnTertiary,
    tertiaryContainer = ScholarlyTertiaryContainer,
    onTertiaryContainer = ScholarlyOnTertiaryContainer,
    error = ScholarlyError,
    onError = ScholarlyOnError,
    errorContainer = ScholarlyErrorContainer,
    onErrorContainer = ScholarlyOnErrorContainer,
    background = ScholarlySurface,
    onBackground = ScholarlyOnSurface,
    surface = ScholarlySurface,
    onSurface = ScholarlyOnSurface,
    onSurfaceVariant = ScholarlyOnSurfaceVariant,
    surfaceVariant = ScholarlySurfaceVariant,
    surfaceContainerLowest = ScholarlySurfaceContainerLowest,
    surfaceContainerLow = ScholarlySurfaceContainerLow,
    surfaceContainer = ScholarlySurfaceContainer,
    surfaceContainerHigh = ScholarlySurfaceContainerHigh,
    surfaceContainerHighest = ScholarlySurfaceContainerHighest,
    outline = ScholarlyOutline,
    outlineVariant = ScholarlyOutlineVariant
)

private val DarkColorScheme = darkColorScheme(
    primary = ScholarlyDarkPrimary,
    onPrimary = ScholarlyDarkOnPrimary,
    primaryContainer = ScholarlyDarkPrimaryContainer,
    onPrimaryContainer = ScholarlyDarkOnPrimaryContainer,
    secondary = ScholarlyDarkSecondary,
    onSecondary = ScholarlyDarkOnSecondary,
    tertiary = ScholarlyDarkTertiary,
    onTertiary = ScholarlyDarkOnTertiary,
    tertiaryContainer = ScholarlyDarkTertiaryContainer,
    onTertiaryContainer = ScholarlyDarkOnTertiaryContainer,
    error = ScholarlyDarkError,
    onError = ScholarlyDarkOnError,
    errorContainer = ScholarlyDarkErrorContainer,
    onErrorContainer = ScholarlyDarkOnErrorContainer,
    background = ScholarlyDarkSurface,
    onBackground = ScholarlyDarkOnSurface,
    surface = ScholarlyDarkSurface,
    onSurface = ScholarlyDarkOnSurface,
    onSurfaceVariant = ScholarlyDarkOnSurfaceVariant,
    surfaceVariant = ScholarlyDarkSurfaceVariant,
    surfaceContainerLowest = ScholarlyDarkSurfaceContainerLowest,
    surfaceContainerLow = ScholarlyDarkSurfaceContainerLow,
    surfaceContainer = ScholarlyDarkSurfaceContainer,
    surfaceContainerHigh = ScholarlyDarkSurfaceContainerHigh,
    surfaceContainerHighest = ScholarlyDarkSurfaceContainerHighest,
    outline = ScholarlyDarkOutline,
    outlineVariant = ScholarlyDarkOutlineVariant
)

@Composable
fun MethodicaTheme(
    darkTheme:    Boolean = isSystemInDarkTheme(),
    // Disabled by default to keep the editorial identity fixed.
    dynamicColor: Boolean = false,
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
