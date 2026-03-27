package com.dyu.ereader.ui.components.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AppChromeIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    destructive: Boolean = false,
    liquidGlassEnabled: Boolean = false,
    size: Dp = 40.dp,
    iconSize: Dp = 18.dp
) {
    val containerColor = when {
        destructive -> MaterialTheme.colorScheme.errorContainer.copy(alpha = if (liquidGlassEnabled) 0.22f else 0.4f)
        selected -> if (liquidGlassEnabled) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        }
        liquidGlassEnabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.76f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val borderColor = when {
        destructive -> MaterialTheme.colorScheme.error.copy(alpha = 0.24f)
        selected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
        liquidGlassEnabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
    }
    val iconTint = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
        destructive -> MaterialTheme.colorScheme.error
        selected -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(size),
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(
            modifier = Modifier.size(size),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = iconTint
            )
        }
    }
}
