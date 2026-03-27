package com.dyu.ereader.ui.components.inputs

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable

@Composable
fun appSwitchColors(): SwitchColors {
    return SwitchDefaults.colors()
}

@Composable
fun appSegmentedButtonColors(): SegmentedButtonColors {
    return SegmentedButtonDefaults.colors(
        activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
        activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        inactiveContentColor = MaterialTheme.colorScheme.onSurface,
        inactiveBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
    )
}
