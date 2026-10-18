package com.chung.lexis.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.chung.lexis.R

internal val LexisUiFontFamily = FontFamily(
    Font(
        resId = R.font.be_vietnam_pro_regular,
        weight = FontWeight.Normal,
    ),
    Font(
        resId = R.font.be_vietnam_pro_medium,
        weight = FontWeight.Medium,
    ),
    Font(
        resId = R.font.be_vietnam_pro_semibold,
        weight = FontWeight.SemiBold,
    ),
)

internal val LexisPronunciationFontFamily = FontFamily(
    Font(
        resId = R.font.noto_sans_regular,
        weight = FontWeight.Normal,
    ),
    Font(
        resId = R.font.noto_sans_medium,
        weight = FontWeight.Medium,
    ),
)

internal val LexisJapaneseFontFamily = FontFamily(
    Font(R.font.noto_sans_jp_regular, FontWeight.Normal),
    Font(R.font.noto_sans_jp_medium, FontWeight.Medium),
    Font(R.font.noto_sans_jp_semibold, FontWeight.SemiBold),
)

internal val LexisChineseFontFamily = FontFamily(
    Font(R.font.noto_sans_sc_regular, FontWeight.Normal),
    Font(R.font.noto_sans_sc_medium, FontWeight.Medium),
    Font(R.font.noto_sans_sc_semibold, FontWeight.SemiBold),
)

private val Baseline = Typography()

val Typography = Typography(

    displayLarge = Baseline.displayLarge.copy(
        fontFamily = LexisUiFontFamily,
    ),

    // Vocabulary hero — 600 · 40sp
    displayMedium = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = 0.sp,
    ),

    displaySmall = Baseline.displaySmall.copy(
        fontFamily = LexisUiFontFamily,
    ),

    headlineLarge = Baseline.headlineLarge.copy(
        fontFamily = LexisUiFontFamily,
    ),

    // Screen title — 600 · 28sp
    headlineMedium = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 35.sp,
        letterSpacing = 0.sp,
    ),

    // Top bar title — 600 · 22sp
    headlineSmall = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 29.sp,
        letterSpacing = 0.sp,
    ),

    // Section title — 600 · 18sp
    titleLarge = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),

    // Smaller strong title used by prototype
    titleMedium = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),

    titleSmall = Baseline.titleSmall.copy(
        fontFamily = LexisUiFontFamily,
    ),

    // Body — 400 · 16sp
    bodyLarge = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),

    // Supporting — 400 · 14sp
    bodyMedium = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),

    bodySmall = Baseline.bodySmall.copy(
        fontFamily = LexisUiFontFamily,
    ),

    // Label — 500 · 14sp
    labelLarge = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),

    // Small label — 500 · 12sp
    labelMedium = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),

    labelSmall = Baseline.labelSmall.copy(
        fontFamily = LexisUiFontFamily,
    ),
)


@Immutable
data class LexisExtraTypography(

    val vocabularyHero: TextStyle,
    val vocabularyHeroLarge: TextStyle,

    val japaneseHero: TextStyle,
    val japaneseHeroLarge: TextStyle,

    val chineseHero: TextStyle,
    val chineseHeroLarge: TextStyle,

    val meaning: TextStyle,

    val pronunciation: TextStyle,
    val pronunciationSmall: TextStyle,

    val example: TextStyle,
)

val LexisExtraTypographyDefaults = LexisExtraTypography(

    vocabularyHero = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = 0.sp,
    ),

    vocabularyHeroLarge = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 44.sp,
        lineHeight = 51.sp,
        letterSpacing = 0.sp,
    ),

    japaneseHero = TextStyle(
        fontFamily = LexisJapaneseFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 52.sp,
        lineHeight = 62.sp,
        letterSpacing = 0.sp,
    ),

    japaneseHeroLarge = TextStyle(
        fontFamily = LexisJapaneseFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 64.sp,
        lineHeight = 77.sp,
        letterSpacing = 0.sp,
    ),

    chineseHero = TextStyle(
        fontFamily = LexisChineseFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 52.sp,
        lineHeight = 62.sp,
        letterSpacing = 0.sp,
    ),

    chineseHeroLarge = TextStyle(
        fontFamily = LexisChineseFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 64.sp,
        lineHeight = 77.sp,
        letterSpacing = 0.sp,
    ),

    meaning = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
    ),

    pronunciation = TextStyle(
        fontFamily = LexisPronunciationFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),

    pronunciationSmall = TextStyle(
        fontFamily = LexisPronunciationFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),

    example = TextStyle(
        fontFamily = LexisUiFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
    ),
)

val LocalLexisTypography =
    staticCompositionLocalOf {
        LexisExtraTypographyDefaults
    }

val MaterialTheme.lexisTypography: LexisExtraTypography
    @Composable
    @ReadOnlyComposable
    get() = LocalLexisTypography.current