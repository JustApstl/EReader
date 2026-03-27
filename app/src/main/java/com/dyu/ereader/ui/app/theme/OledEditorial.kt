package com.dyu.ereader.ui.app.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp

@Composable
fun isDarkEditorialTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.42f

@Composable
fun isOledTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.01f

private fun elevatedSurface(
    surface: Color,
    tint: Color,
    amount: Float
): Color = lerp(surface, tint, amount).compositeOver(surface)

@Composable
fun editorialSurfaceColor(
    level: EditorialSurfaceLevel = EditorialSurfaceLevel.BASE
): Color {
    val colors = MaterialTheme.colorScheme
    return if (isOledTheme()) {
        when (level) {
            EditorialSurfaceLevel.LOWEST -> colors.background
            EditorialSurfaceLevel.BASE -> colors.surface
            EditorialSurfaceLevel.LOW -> colors.surfaceContainerLow
            EditorialSurfaceLevel.HIGH -> colors.surfaceContainerHigh
            EditorialSurfaceLevel.HIGHEST -> colors.surfaceContainerHighest
        }
    } else {
        when (level) {
            EditorialSurfaceLevel.LOWEST -> elevatedSurface(colors.surface, colors.surfaceTint, 0.01f)
            EditorialSurfaceLevel.BASE -> colors.surface
            EditorialSurfaceLevel.LOW -> elevatedSurface(colors.surface, colors.surfaceTint, 0.04f)
            EditorialSurfaceLevel.HIGH -> elevatedSurface(colors.surface, colors.surfaceTint, 0.08f)
            EditorialSurfaceLevel.HIGHEST -> elevatedSurface(colors.surface, colors.surfaceTint, 0.12f)
        }
    }
}

@Composable
fun editorialGhostBorder(
    alpha: Float = 0.15f
): BorderStroke = BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha)
)

@Composable
fun editorialAmbientGlow(): Color = MaterialTheme.colorScheme.primary.copy(
    alpha = if (isOledTheme()) 0.05f else 0.04f
)

enum class EditorialSurfaceLevel {
    LOWEST,
    BASE,
    LOW,
    HIGH,
    HIGHEST
}
