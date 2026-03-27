package com.dyu.ereader.ui.home.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.reader.ReaderControl

internal data class ReaderControlRowData(
    val title: String,
    val icon: ImageVector,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

internal fun mergeReaderControlOrder(current: List<ReaderControl>): List<ReaderControl> {
    val defaults = ReaderControl.defaultOrder()
    val merged = (current + defaults).distinct()
    return merged.filter { defaults.contains(it) }
}

@Composable
internal fun ReaderControlRow(
    title: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    liquidGlassEnabled: Boolean = false,
    isDragging: Boolean = false,
    dragHandleModifier: Modifier = Modifier,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (isDragging) 1.05f else 1f)
    val elevation by animateFloatAsState(if (isDragging) 4f else 0f)
    val rowColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = if (isDragging) 0.88f else 0.78f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val rowBorderColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDragging) 0.24f else 0.18f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDragging) 0.34f else 0.24f)
    }
    val iconChipColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHighest
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation = elevation.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        color = rowColor,
        border = BorderStroke(1.dp, rowBorderColor),
        onClick = { onCheckedChange(!checked) },
        enabled = !isDragging
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = !isDragging,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.outline,
                    checkmarkColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            Surface(
                modifier = Modifier.size(34.dp),
                shape = RoundedCornerShape(12.dp),
                color = iconChipColor
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (liquidGlassEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box(
                modifier = Modifier
                    .then(dragHandleModifier)
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (isDragging) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.DragHandle,
                    contentDescription = "Hold and drag to reorder",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDragging) 1f else 0.78f),
                    modifier = Modifier.alpha(if (isDragging) 1f else 0.78f)
                )
            }
        }
    }
}
