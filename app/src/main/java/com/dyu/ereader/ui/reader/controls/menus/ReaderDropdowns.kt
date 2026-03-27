package com.dyu.ereader.ui.reader.controls.menus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton

@Composable
fun SelectionActionRow(
    label: String,
    icon: ImageVector,
    iconTint: Color = MaterialTheme.colorScheme.onSurface,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    iconContainerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    supportingText: String? = null,
    selected: Boolean = false,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val resolvedContainerColor = if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        containerColor
    }
    val resolvedIconContainerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        iconContainerColor
    }
    val resolvedIconTint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else iconTint
    val resolvedTextColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else textColor
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(resolvedContainerColor)
            .clickable(enabled = enabled, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(resolvedIconContainerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(12.dp),
                tint = if (enabled) resolvedIconTint else resolvedIconTint.copy(alpha = 0.55f)
            )
        }
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (enabled) resolvedTextColor else resolvedTextColor.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!supportingText.isNullOrBlank()) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 0.82f else 0.48f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ReaderDropdownHeader(
    title: String,
    subtitle: String? = null,
    onClose: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (onClose != null) {
            AppChromeIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Close",
                onClick = onClose,
                size = 24.dp,
                iconSize = 14.dp
            )
        }
    }
}

@Composable
fun ReaderDropdownSectionLabel(
    label: String,
    trailingLabel: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!trailingLabel.isNullOrBlank()) {
            Text(
                text = trailingLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ReaderDropdownSurface(
    modifier: Modifier = Modifier,
    width: Dp = 196.dp,
    maxHeight: Dp = 248.dp,
    liquidGlassEnabled: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val scrollState = rememberScrollState()
    val widthModifier = if (width == Dp.Unspecified) {
        Modifier
            .width(androidx.compose.foundation.layout.IntrinsicSize.Min)
            .widthIn(min = 184.dp, max = 300.dp)
    } else {
        Modifier.width(width)
    }
    val surfaceColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val surfaceBorder = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
    }
    Surface(
        modifier = modifier
            .then(widthModifier)
            .shadow(if (liquidGlassEnabled) 6.dp else 4.dp, shape, clip = false),
        shape = shape,
        color = surfaceColor,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, surfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .verticalScroll(scrollState)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
            content = content
        )
    }
}

fun dropdownWidthForLabels(labels: List<String>): Dp {
    val longest = labels.maxOfOrNull { it.length } ?: 10
    val estimated = (longest * 6.6f + 86f).dp
    return estimated.coerceIn(184.dp, 288.dp)
}

fun anchoredDropdownPositionProvider(
    anchorX: Int,
    anchorY: Int,
    marginPx: Int,
    verticalGapPx: Int
): PopupPositionProvider {
    return object : PopupPositionProvider {
        override fun calculatePosition(
            anchorBounds: IntRect,
            windowSize: IntSize,
            layoutDirection: LayoutDirection,
            popupContentSize: IntSize
        ): IntOffset {
            val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
            val centeredX = (anchorX - popupContentSize.width / 2).coerceIn(marginPx, maxX)

            val maxY = (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
            val preferredBelowY = anchorY + verticalGapPx
            val preferredAboveY = anchorY - popupContentSize.height - verticalGapPx
            val bestY = when {
                preferredBelowY + popupContentSize.height <= windowSize.height - marginPx -> preferredBelowY
                preferredAboveY >= marginPx -> preferredAboveY
                else -> preferredBelowY.coerceIn(marginPx, maxY)
            }

            return IntOffset(centeredX, bestY)
        }
    }
}
