package com.dyu.ereader.ui.app.theme

import com.dyu.ereader.R
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val EditorialDisplayFont = FontFamily(
    Font(R.font.newsreader_variable, FontWeight.ExtraLight),
    Font(R.font.newsreader_variable, FontWeight.Light),
    Font(R.font.newsreader_variable, FontWeight.Normal),
    Font(R.font.newsreader_variable, FontWeight.Medium),
    Font(R.font.newsreader_variable, FontWeight.SemiBold),
    Font(R.font.newsreader_variable, FontWeight.Bold),
    Font(R.font.newsreader_variable, FontWeight.ExtraBold)
)

val EditorialBodyFont = FontFamily(
    Font(R.font.manrope_variable, FontWeight.ExtraLight),
    Font(R.font.manrope_variable, FontWeight.Light),
    Font(R.font.manrope_variable, FontWeight.Normal),
    Font(R.font.manrope_variable, FontWeight.Medium),
    Font(R.font.manrope_variable, FontWeight.SemiBold),
    Font(R.font.manrope_variable, FontWeight.Bold),
    Font(R.font.manrope_variable, FontWeight.ExtraBold)
)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 50.sp,
        lineHeight = 54.sp,
        letterSpacing = (-1.0).sp
    ),
    displayMedium = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 42.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.8).sp
    ),
    displaySmall = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.32).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.22).sp
    ),
    titleLarge = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.1).sp
    ),
    titleMedium = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.08.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.08.sp
    ),
    bodySmall = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.08.sp
    ),
    labelLarge = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.08.sp
    ),
    labelMedium = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.18.sp
    ),
    labelSmall = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.16.sp
    )
)
