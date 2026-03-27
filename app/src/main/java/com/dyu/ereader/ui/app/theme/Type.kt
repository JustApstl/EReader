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
        fontFamily = EditorialDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.2).sp
    ),
    displayMedium = TextStyle(
        fontFamily = EditorialDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 46.sp,
        lineHeight = 50.sp,
        letterSpacing = (-0.95).sp
    ),
    displaySmall = TextStyle(
        fontFamily = EditorialDisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.75).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = EditorialDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.6).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = EditorialDisplayFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 33.sp,
        letterSpacing = (-0.48).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = EditorialDisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.34).sp
    ),
    titleLarge = TextStyle(
        fontFamily = EditorialDisplayFont,
        fontWeight = FontWeight.Medium,
        fontSize = 21.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.22).sp
    ),
    titleMedium = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.12.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.12.sp
    ),
    bodySmall = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.16.sp
    ),
    labelMedium = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.45.sp
    ),
    labelSmall = TextStyle(
        fontFamily = EditorialBodyFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.6.sp
    )
)
