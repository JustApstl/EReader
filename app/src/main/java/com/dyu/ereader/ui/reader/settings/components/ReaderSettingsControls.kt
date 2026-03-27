package com.dyu.ereader.ui.reader.settings.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyu.ereader.ui.components.badges.BetaBadge
import com.dyu.ereader.ui.components.badges.LocalBetaFeaturesEnabled
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.inputs.appSwitchColors
import com.dyu.ereader.ui.components.inputs.appSliderColors

@Composable
internal fun ModeChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 0.dp else 2.dp,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

@Composable
internal fun SliderSetting(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    onReset: () -> Unit,
    icon: ImageVector,
    valueDisplay: (Float) -> String,
    enabled: Boolean = true,
    showReset: Boolean = false,
    applyWhileDragging: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsPressedAsState()
    var internalValue by remember { mutableFloatStateOf(value.coerceIn(valueRange.start, valueRange.endInclusive)) }

    LaunchedEffect(value, isDragging) {
        if (!isDragging) {
            internalValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showReset) {
                    AppChromeIconButton(
                        icon = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset $label",
                        onClick = onReset,
                        size = 32.dp,
                        iconSize = 18.dp
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        valueDisplay(if (isDragging) internalValue else value),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        Slider(
            value = internalValue,
            onValueChange = {
                internalValue = it.coerceIn(valueRange.start, valueRange.endInclusive)
                if (applyWhileDragging) {
                    onValueChange(internalValue)
                }
            },
            onValueChangeFinished = {
                onValueChangeFinished?.invoke(internalValue)
            },
            valueRange = valueRange,
            modifier = Modifier.padding(top = 0.dp),
            interactionSource = interactionSource,
            colors = appSliderColors(),
            enabled = enabled
        )
    }
}

@Composable
internal fun ToggleRow(
    title: String,
    desc: String? = null,
    checked: Boolean,
    icon: ImageVector,
    onToggle: (Boolean) -> Unit,
    beta: Boolean = false
) {
    if (beta && !LocalBetaFeaturesEnabled.current) return
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        title,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                    if (beta) {
                        BetaBadge(modifier = Modifier.widthIn(min = 0.dp))
                    }
                }
                if (!desc.isNullOrBlank()) {
                    Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onToggle,
            thumbContent = if (checked) {
                { Icon(Icons.Rounded.Check, null, Modifier.size(SwitchDefaults.IconSize)) }
            } else null,
            colors = appSwitchColors()
        )
    }
}
