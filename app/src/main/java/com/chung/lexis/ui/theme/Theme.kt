package com.chung.lexis.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LexisDarkPrimary,
    onPrimary = LexisDarkOnPrimary,

    primaryContainer = LexisDarkPrimaryContainer,
    onPrimaryContainer = LexisDarkOnPrimaryContainer,

    secondaryContainer = LexisDarkSecondaryContainer,

    background = LexisDarkSurface,
    onBackground = LexisDarkOnSurface,

    surface = LexisDarkSurface,
    onSurface = LexisDarkOnSurface,

    surfaceVariant = LexisDarkSurfaceContainer,
    onSurfaceVariant = LexisDarkOnSurfaceVariant,

    surfaceContainerLow = LexisDarkSurfaceContainerLow,
    surfaceContainer = LexisDarkSurfaceContainer,
    surfaceContainerHigh = LexisDarkSurfaceContainerHigh,

    outlineVariant = LexisDarkOutlineVariant,

    error = LexisDarkError,
)

private val LightColorScheme = lightColorScheme(
    primary = LexisPrimary,
    onPrimary = LexisOnPrimary,

    primaryContainer = LexisPrimaryContainer,
    onPrimaryContainer = LexisOnPrimaryContainer,

    secondaryContainer = LexisSecondaryContainer,

    background = LexisSurface,
    onBackground = LexisOnSurface,

    surface = LexisSurface,
    onSurface = LexisOnSurface,

    surfaceVariant = LexisSurfaceContainer,
    onSurfaceVariant = LexisOnSurfaceVariant,

    surfaceContainerLow = LexisSurfaceContainerLow,
    surfaceContainer = LexisSurfaceContainer,
    surfaceContainerHigh = LexisSurfaceContainerHigh,

    outlineVariant = LexisOutlineVariant,

    error = LexisError,
)

@Composable
fun LexisTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+, but it replaces the Lexis brand palette
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = LexisShapes,
        content = content
    )
}
