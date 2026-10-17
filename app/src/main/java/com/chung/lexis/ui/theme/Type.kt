package com.chung.lexis.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Change this one value to switch the whole app to another typeface
private val LexisFontFamily = FontFamily.SansSerif

// Material defaults, used as the base so untouched properties (e.g. letterSpacing) are kept
private val Baseline = Typography()

val Typography = Typography(
    displayLarge = Baseline.displayLarge.copy(fontFamily = LexisFontFamily),
    displayMedium = Baseline.displayMedium.copy(
        fontFamily = LexisFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
    ),
    displaySmall = Baseline.displaySmall.copy(fontFamily = LexisFontFamily),

    headlineLarge = Baseline.headlineLarge.copy(fontFamily = LexisFontFamily),
    headlineMedium = Baseline.headlineMedium.copy(
        fontFamily = LexisFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 35.sp,
    ),
    headlineSmall = Baseline.headlineSmall.copy(
        fontFamily = LexisFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 29.sp,
    ),

    titleLarge = Baseline.titleLarge.copy(fontFamily = LexisFontFamily),
    titleMedium = Baseline.titleMedium.copy(
        fontFamily = LexisFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleSmall = Baseline.titleSmall.copy(fontFamily = LexisFontFamily),

    bodyLarge = Baseline.bodyLarge.copy(
        fontFamily = LexisFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = Baseline.bodyMedium.copy(
        fontFamily = LexisFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = Baseline.bodySmall.copy(fontFamily = LexisFontFamily),

    labelLarge = Baseline.labelLarge.copy(
        fontFamily = LexisFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = Baseline.labelMedium.copy(fontFamily = LexisFontFamily),
    labelSmall = Baseline.labelSmall.copy(fontFamily = LexisFontFamily),
)
