package com.chung.lexis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class LexisSemanticColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,

    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,

    val hover: Color,
)


val LightLexisSemanticColors = LexisSemanticColors(
    success = LexisSuccess,
    successContainer = LexisSuccessContainer,
    onSuccessContainer = LexisOnSuccessContainer,

    warning = LexisWarning,
    warningContainer = LexisWarningContainer,
    onWarningContainer = LexisOnWarningContainer,

    hover = LexisHover,
)

val DarkLexisSemanticColors = LexisSemanticColors(
    success = LexisDarkSuccess,
    successContainer = LexisDarkSuccessContainer,
    onSuccessContainer = LexisDarkOnSuccessContainer,

    warning = LexisDarkWarning,
    warningContainer = LexisDarkWarningContainer,
    onWarningContainer = LexisDarkOnWarningContainer,

    hover = LexisDarkHover,
)

val LocalLexisSemanticColors =
    staticCompositionLocalOf {
        LightLexisSemanticColors
    }


val MaterialTheme.lexisColors: LexisSemanticColors
    @Composable
    @ReadOnlyComposable
    get() = LocalLexisSemanticColors.current