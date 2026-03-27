package com.dyu.ereader.ui.app.theme

import android.app.Activity
import android.content.Context
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.core.view.WindowCompat
import com.dyu.ereader.data.model.app.AppAccent
import com.dyu.ereader.data.model.app.AppFont
import com.dyu.ereader.data.model.app.AppTheme

private val NeutralLightColorScheme = lightColorScheme(
    primary = Color(0xFF425F91),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8E2FF),
    onPrimaryContainer = Color(0xFF001A43),
    secondary = Color(0xFF565F71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF705575),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFAD8FD),
    onTertiaryContainer = Color(0xFF28132E),
    background = Color(0xFFF9F9FF),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF9F9FF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFE1E2EC),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    outlineVariant = Color(0xFFC4C6D0)
)

private val NeutralDarkColorScheme = darkColorScheme(
    primary = Color(0xFFAEC6FF),
    onPrimary = Color(0xFF0C2F60),
    primaryContainer = Color(0xFF294777),
    onPrimaryContainer = Color(0xFFD8E2FF),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3E4758),
    onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFFDDBCE0),
    onTertiary = Color(0xFF3F2844),
    tertiaryContainer = Color(0xFF573E5C),
    onTertiaryContainer = Color(0xFFFAD8FD),
    background = Color(0xFF111318),
    onBackground = Color(0xFFE2E2E9),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFE2E2E9),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = Color(0xFF8E9099),
    outlineVariant = Color(0xFF44474F)
)

@Composable
fun EReaderTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    appFont: AppFont = AppFont.SYSTEM,
    appAccent: AppAccent = AppAccent.SYSTEM,
    customAccentColor: Int? = null,
    appTextScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = appTheme.resolveDarkTheme(isSystemInDarkTheme())
    val baseColorScheme = resolveBaseColorScheme(
        appTheme = appTheme,
        darkTheme = darkTheme,
        context = context
    )
    val colorScheme = baseColorScheme.withAccent(
        accent = appAccent.resolveAccentSeed(
            customAccentColor = customAccentColor,
            systemAccentSeed = if (appAccent == AppAccent.SYSTEM) baseColorScheme.primary else null
        ),
        darkTheme = darkTheme
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val scaledTypography = scaleTypography(
        AppTypography.withAppFont(appFont),
        appTextScale
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = scaledTypography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
private fun resolveBaseColorScheme(
    appTheme: AppTheme,
    darkTheme: Boolean,
    context: Context
): ColorScheme {
    val dynamicBase = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) NeutralDarkColorScheme else NeutralLightColorScheme
    }

    return when {
        appTheme == AppTheme.BLACK -> dynamicBase.toAmoledScheme()
        else -> dynamicBase
    }
}

private fun ColorScheme.toAmoledScheme(): ColorScheme {
    val black = Color(0xFF000000)
    val surfaceBase = Color(0xFF0A0A0A)
    val surfaceLow = Color(0xFF111111)
    val surfaceHigh = Color(0xFF1B1B1B)
    return copy(
        background = black,
        onBackground = Color(0xFFE6E1E5),
        surface = surfaceBase,
        onSurface = Color(0xFFE6E1E5),
        surfaceBright = surfaceHigh,
        surfaceDim = black,
        surfaceContainerLowest = black,
        surfaceContainerLow = surfaceBase,
        surfaceContainer = surfaceLow,
        surfaceContainerHigh = surfaceHigh,
        surfaceContainerHighest = Color(0xFF232323),
        surfaceVariant = surfaceHigh,
        onSurfaceVariant = Color(0xFF9FA3AA),
        outline = Color(0xFF676B73),
        outlineVariant = Color(0xFF35383E)
    )
}

private fun androidx.compose.material3.ColorScheme.withAccent(
    accent: Color?,
    darkTheme: Boolean,
): androidx.compose.material3.ColorScheme {
    accent ?: return this
    val primary = accent
    val primaryContainer = lerp(
        accent,
        if (darkTheme) Color.Black else Color.White,
        if (darkTheme) 0.68f else 0.8f
    ).compositeOver(surface)
    val secondary = lerp(accent, secondary, 0.35f)
    val secondaryContainer = lerp(primaryContainer, secondaryContainer, 0.45f).compositeOver(surface)
    val tertiary = lerp(accent, tertiary, 0.55f)
    val tertiaryContainer = lerp(primaryContainer, tertiaryContainer, 0.55f).compositeOver(surface)

    return copy(
        primary = primary,
        onPrimary = primary.bestOnColor(),
        primaryContainer = primaryContainer,
        onPrimaryContainer = primaryContainer.bestOnColor(),
        secondary = secondary,
        onSecondary = secondary.bestOnColor(),
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = secondaryContainer.bestOnColor(),
        tertiary = tertiary,
        onTertiary = tertiary.bestOnColor(),
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = tertiaryContainer.bestOnColor(),
        surfaceTint = primary
    )
}

private fun Color.bestOnColor(): Color {
    return if (luminance() > 0.42f) Color(0xFF0F1418) else Color.White
}

private fun scaleTypography(base: androidx.compose.material3.Typography, scale: Float): androidx.compose.material3.Typography {
    if (scale == 1f) return base
    fun TextStyle.scaleText(): TextStyle {
        return if (fontSize != TextUnit.Unspecified) copy(fontSize = fontSize * scale) else this
    }
    return base.copy(
        displayLarge = base.displayLarge.scaleText(),
        displayMedium = base.displayMedium.scaleText(),
        displaySmall = base.displaySmall.scaleText(),
        headlineLarge = base.headlineLarge.scaleText(),
        headlineMedium = base.headlineMedium.scaleText(),
        headlineSmall = base.headlineSmall.scaleText(),
        titleLarge = base.titleLarge.scaleText(),
        titleMedium = base.titleMedium.scaleText(),
        titleSmall = base.titleSmall.scaleText(),
        bodyLarge = base.bodyLarge.scaleText(),
        bodyMedium = base.bodyMedium.scaleText(),
        bodySmall = base.bodySmall.scaleText(),
        labelLarge = base.labelLarge.scaleText(),
        labelMedium = base.labelMedium.scaleText(),
        labelSmall = base.labelSmall.scaleText()
    )
}
