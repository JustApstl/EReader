package com.dyu.ereader.ui.components.menus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dyu.ereader.ui.app.theme.UiTokens

@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    liquidGlassEnabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(18.dp)
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier.wrapContentWidth(),
        shape = shape,
        containerColor = Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        AppDropdownSurface(
            width = Dp.Unspecified,
            maxHeight = 280.dp,
            liquidGlassEnabled = liquidGlassEnabled
        ) {
            if (!title.isNullOrBlank()) {
                androidx.compose.material3.Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
private fun AppDropdownSurface(
    modifier: Modifier = Modifier,
    width: Dp = 196.dp,
    maxHeight: Dp = 248.dp,
    liquidGlassEnabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = UiTokens.SettingsCardShape
    val scrollState = rememberScrollState()
    val widthModifier = if (width == Dp.Unspecified) {
        Modifier
            .width(androidx.compose.foundation.layout.IntrinsicSize.Min)
            .widthIn(min = 184.dp, max = 300.dp)
    } else {
        Modifier.width(width)
    }
    val surfaceColor = when {
        liquidGlassEnabled -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val borderColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
    }

    Surface(
        modifier = modifier
            .then(widthModifier)
            .shadow(6.dp, shape, clip = false),
        shape = shape,
        color = surfaceColor,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

@Composable
fun AppDropdownMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color? = null,
    iconContainerColor: Color? = null,
    trailingIcon: ImageVector? = null,
    trailingTint: Color = MaterialTheme.colorScheme.primary,
    supportingText: String? = null,
    badgeText: String? = null,
    enableMarquee: Boolean = false,
    isDestructive: Boolean = false,
    enabled: Boolean = true
) {
    val destructiveText = MaterialTheme.colorScheme.error
    val destructiveContainer = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.56f)
    val destructiveIconContainer = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)
    val resolvedContainerColor = if (isDestructive) {
        destructiveContainer
    } else {
        containerColor ?: MaterialTheme.colorScheme.surfaceContainerLow
    }
    val resolvedIconContainerColor = if (isDestructive) {
        destructiveIconContainer
    } else {
        iconContainerColor ?: MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.88f)
    }
    val resolvedIconTint = if (isDestructive) destructiveText else iconTint
    val resolvedTextColor = if (isDestructive) destructiveText else textColor
    val resolvedTrailingTint = if (isDestructive) destructiveText else trailingTint
    val marqueeModifier = if (enableMarquee) {
        Modifier.basicMarquee(
            iterations = Int.MAX_VALUE,
            animationMode = MarqueeAnimationMode.Immediately,
            initialDelayMillis = 700,
            repeatDelayMillis = 1200,
            velocity = 28.dp
        )
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(resolvedContainerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 46.dp)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(resolvedIconContainerColor),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(13.dp),
                tint = resolvedIconTint
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) resolvedTextColor else resolvedTextColor.copy(alpha = 0.6f),
                modifier = marqueeModifier,
                maxLines = 1
            )
            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.92f else 0.6f),
                    maxLines = 1
                )
            }
        }
        if (!badgeText.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(resolvedIconContainerColor)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = resolvedTrailingTint,
                    maxLines = 1
                )
            }
        }
        if (trailingIcon != null) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(resolvedIconContainerColor),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = resolvedTrailingTint,
                    modifier = Modifier.size(13.dp)
                )
            }
        } else {
            Spacer(Modifier.width(4.dp))
        }
    }
}
