package com.chung.lexis.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = LexisDarkPrimary,
    onPrimary = LexisDarkOnPrimary,

    primaryContainer = LexisDarkPrimaryContainer,
    onPrimaryContainer = LexisDarkOnPrimaryContainer,

    secondaryContainer = LexisDarkSecondaryContainer,
    onSecondaryContainer = LexisDarkOnSecondaryContainer,

    background = LexisDarkSurface,
    onBackground = LexisDarkOnSurface,

    surface = LexisDarkSurface,
    onSurface = LexisDarkOnSurface,

    surfaceVariant = LexisDarkSurfaceContainer,
    onSurfaceVariant = LexisDarkOnSurfaceVariant,

    surfaceContainerLow = LexisDarkSurfaceContainerLow,
    surfaceContainer = LexisDarkSurfaceContainer,
    surfaceContainerHigh = LexisDarkSurfaceContainerHigh,
    surfaceContainerHighest = LexisDarkSurfaceContainerHighest,

    outline = LexisDarkOutline,
    outlineVariant = LexisDarkOutlineVariant,

    error = LexisDarkError,
    errorContainer = LexisDarkErrorContainer,
    onErrorContainer = LexisDarkOnErrorContainer,

    inverseSurface = LexisDarkInverseSurface,
    inverseOnSurface = LexisDarkInverseOnSurface,
    inversePrimary = LexisDarkInversePrimary,

    scrim = LexisDarkScrim,
)

private val LightColorScheme = lightColorScheme(
    primary = LexisPrimary,
    onPrimary = LexisOnPrimary,

    primaryContainer = LexisPrimaryContainer,
    onPrimaryContainer = LexisOnPrimaryContainer,

    secondaryContainer = LexisSecondaryContainer,
    onSecondaryContainer = LexisOnSecondaryContainer,

    background = LexisSurface,
    onBackground = LexisOnSurface,

    surface = LexisSurface,
    onSurface = LexisOnSurface,

    surfaceVariant = LexisSurfaceContainer,
    onSurfaceVariant = LexisOnSurfaceVariant,

    surfaceContainerLow = LexisSurfaceContainerLow,
    surfaceContainer = LexisSurfaceContainer,
    surfaceContainerHigh = LexisSurfaceContainerHigh,
    surfaceContainerHighest = LexisSurfaceContainerHighest,

    outline = LexisOutline,
    outlineVariant = LexisOutlineVariant,

    error = LexisError,
    errorContainer = LexisErrorContainer,
    onErrorContainer = LexisOnErrorContainer,

    inverseSurface = LexisInverseSurface,
    inverseOnSurface = LexisInverseOnSurface,
    inversePrimary = LexisInversePrimary,

    scrim = LexisScrim,
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


    val semanticColors =
        if (darkTheme) {
            DarkLexisSemanticColors
        } else {
            LightLexisSemanticColors
        }

    CompositionLocalProvider(
        LocalLexisSemanticColors provides semanticColors,
        LocalLexisTypography provides LexisExtraTypographyDefaults,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = LexisShapes,
            content = content,
        )
    }
}
