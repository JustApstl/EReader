package com.dyu.ereader.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyu.ereader.ui.components.surfaces.rememberLiquidGlassStyle

@Composable
fun CustomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    animationsEnabled: Boolean = true,
    showIndicator: Boolean = true,
    liquidGlassEnabled: Boolean = false,
    badge: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val alpha by animateFloatAsState(if (selected) 1f else 0.56f, label = "floating_nav_alpha")
    val scale by animateFloatAsState(if (selected && animationsEnabled) 1.06f else 1f, label = "floating_nav_scale")
    val glassStyle = rememberLiquidGlassStyle(strong = true)
    val contentColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.primary
            liquidGlassEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "floating_nav_item_content"
    )
    val iconContainerColor by animateColorAsState(
        targetValue = when {
            selected -> MaterialTheme.colorScheme.surface.copy(alpha = if (liquidGlassEnabled) 0.84f else 1f)
            showIndicator && liquidGlassEnabled -> glassStyle.iconContainerColor.copy(alpha = 0.72f)
            else -> Color.Transparent
        },
        label = "floating_nav_icon_container"
    )
    val iconBorder = if (liquidGlassEnabled && (selected || showIndicator)) {
        BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (selected) 0.38f else 0.26f)
        )
    } else {
        null
    }

    Surface(
        modifier = modifier
            .height(58.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        border = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val iconContent: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(iconContainerColor, CircleShape)
                        .let { base ->
                            if (iconBorder != null) base.then(Modifier.border(iconBorder, CircleShape)) else base
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        tint = contentColor,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer(scaleX = scale, scaleY = scale)
                            .alpha(alpha)
                    )
                }
            }

            if (badge != null) {
                BadgedBox(badge = { badge() }) { iconContent() }
            } else {
                iconContent()
            }

            Spacer(Modifier.height(3.dp))

            Text(
                text = label,
                style = if (selected) {
                    MaterialTheme.typography.labelMedium.copy(fontSize = 10.sp)
                } else {
                    MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp)
                },
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                modifier = Modifier.alpha(if (selected) 1f else 0.92f),
                maxLines = 1
            )
            if (selected) {
                Spacer(Modifier.height(1.5.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.34f)
                        .height(1.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.48f),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}
