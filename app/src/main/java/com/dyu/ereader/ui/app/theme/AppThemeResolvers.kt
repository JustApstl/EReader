package com.dyu.ereader.ui.app.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.dyu.ereader.data.model.app.AppAccent
import com.dyu.ereader.data.model.app.AppFont
import com.dyu.ereader.data.model.app.AppTheme

fun AppTheme.resolveDarkTheme(isSystemDark: Boolean): Boolean {
    return when (this) {
        AppTheme.SYSTEM -> isSystemDark
        AppTheme.LIGHT -> false
        AppTheme.DARK, AppTheme.BLACK -> true
    }
}

fun AppAccent.resolveAccentColor(
    customAccentColor: Int?,
    fallback: Color
): Color {
    return when (this) {
        AppAccent.SYSTEM -> fallback
        AppAccent.CUSTOM -> customAccentColor?.let(::Color) ?: fallback
        else -> seedColor
    }
}

@Composable
fun systemAccentPreviewColor(fallback: Color): Color {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isSystemInDarkTheme()) {
            dynamicDarkColorScheme(context).primary
        } else {
            dynamicLightColorScheme(context).primary
        }
    } else {
        fallback
    }
}

fun AppAccent.resolveAccentSeed(
    customAccentColor: Int?,
    systemAccentSeed: Color?
): Color? {
    return when (this) {
        AppAccent.SYSTEM -> systemAccentSeed
        AppAccent.CUSTOM -> customAccentColor?.let(::Color)
        else -> seedColor
    }
}

fun AppFont.previewFontFamily(): FontFamily {
    return when (this) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.EDITORIAL -> EditorialDisplayFont
        AppFont.SANS -> FontFamily.SansSerif
        AppFont.SERIF -> FontFamily.Serif
        AppFont.MONO -> FontFamily.Monospace
    }
}

fun Typography.withAppFont(appFont: AppFont): Typography {
    val family = when (appFont) {
        AppFont.SYSTEM -> FontFamily.Default
        AppFont.EDITORIAL -> return this
        AppFont.SANS -> FontFamily.SansSerif
        AppFont.SERIF -> FontFamily.Serif
        AppFont.MONO -> FontFamily.Monospace
    }

    fun TextStyle.withFamily() = copy(fontFamily = family)

    return copy(
        displayLarge = displayLarge.withFamily(),
        displayMedium = displayMedium.withFamily(),
        displaySmall = displaySmall.withFamily(),
        headlineLarge = headlineLarge.withFamily(),
        headlineMedium = headlineMedium.withFamily(),
        headlineSmall = headlineSmall.withFamily(),
        titleLarge = titleLarge.withFamily(),
        titleMedium = titleMedium.withFamily(),
        titleSmall = titleSmall.withFamily(),
        bodyLarge = bodyLarge.withFamily(),
        bodyMedium = bodyMedium.withFamily(),
        bodySmall = bodySmall.withFamily(),
        labelLarge = labelLarge.withFamily(),
        labelMedium = labelMedium.withFamily(),
        labelSmall = labelSmall.withFamily()
    )
}
