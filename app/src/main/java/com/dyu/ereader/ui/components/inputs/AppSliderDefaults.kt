package com.dyu.ereader.ui.components.inputs

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable

@Composable
fun appSliderColors(): SliderColors {
    return SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        activeTickColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
        inactiveTickColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.48f)
    )
}
