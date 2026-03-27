package com.dyu.ereader.ui.components.surfaces

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

data class LiquidGlassStyle(
    val containerColor: Color,
    val border: BorderStroke,
    val secondaryBorder: BorderStroke,
    val iconContainerColor: Color
)

@Composable
fun rememberLiquidGlassStyle(
    strong: Boolean = false
): LiquidGlassStyle {
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = if (strong) 0.78f else 0.66f)
    val borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (strong) 0.22f else 0.16f)
    val secondaryBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = if (strong) 0.1f else 0.07f)
    val iconContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (strong) 0.88f else 0.76f)
    return LiquidGlassStyle(
        containerColor = containerColor,
        border = BorderStroke(1.dp, borderColor),
        secondaryBorder = BorderStroke(1.dp, secondaryBorderColor),
        iconContainerColor = iconContainerColor
    )
}

@Composable
fun BoxScope.LiquidGlassOverlay(
    shape: Shape,
    modifier: Modifier = Modifier,
    intensity: Float = 1f
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceTint.copy(alpha = 0.12f * intensity),
                        MaterialTheme.colorScheme.surfaceBright.copy(alpha = 0.08f * intensity),
                        Color.Transparent
                    )
                )
            )
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f * intensity),
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.07f * intensity)
                    )
                )
            )
    )
}

@Composable
fun glassCircleShape(): Shape = CircleShape
