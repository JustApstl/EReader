package com.dyu.ereader.ui.home.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyu.ereader.ui.components.badges.BetaBadge
import com.dyu.ereader.ui.components.badges.LocalBetaFeaturesEnabled
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.inputs.appSwitchColors
import com.dyu.ereader.ui.components.inputs.appSliderColors

@Composable
internal fun SettingSwitch(
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    beta: Boolean = false
) {
    if (beta && !LocalBetaFeaturesEnabled.current) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                if (beta) {
                    BetaBadge(Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = (-4).dp))
                }
            }
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(modifier = Modifier.padding(top = 2.dp)) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = appSwitchColors()
            )
        }
    }
}

@Composable
internal fun SettingSlider(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueLabel: (Float) -> String,
    steps: Int = 0,
    showReset: Boolean = false,
    onReset: () -> Unit = {},
    onValueChangeFinished: ((Float) -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsPressedAsState()
    var internalValue by remember { mutableFloatStateOf(value.coerceIn(valueRange.start, valueRange.endInclusive)) }

    LaunchedEffect(value, isDragging) {
        if (!isDragging) {
            internalValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (showReset) {
                    AppChromeIconButton(
                        icon = Icons.Rounded.RestartAlt,
                        contentDescription = "Reset $title",
                        onClick = onReset,
                        size = 32.dp,
                        iconSize = 18.dp
                    )
                }
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        valueLabel(internalValue),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Slider(
            value = internalValue,
            onValueChange = {
                internalValue = it.coerceIn(valueRange.start, valueRange.endInclusive)
                onValueChange(internalValue)
            },
            valueRange = valueRange,
            interactionSource = interactionSource,
            steps = steps.coerceAtLeast(0),
            onValueChangeFinished = { onValueChangeFinished?.invoke(internalValue) },
            colors = appSliderColors()
        )
    }
}
