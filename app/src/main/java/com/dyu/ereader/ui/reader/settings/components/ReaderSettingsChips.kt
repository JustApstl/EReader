package com.dyu.ereader.ui.reader.settings.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    showSelectionIcon: Boolean = true,
    enabled: Boolean = true
) {
    val colors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        containerColor = MaterialTheme.colorScheme.surface,
        labelColor = MaterialTheme.colorScheme.onSurface,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        iconColor = MaterialTheme.colorScheme.primary
    )
    androidx.compose.material3.FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = when {
            icon != null -> { { Icon(icon, null, Modifier.size(FilterChipDefaults.IconSize)) } }
            showSelectionIcon && selected -> { { Icon(Icons.Rounded.Check, null, Modifier.size(FilterChipDefaults.IconSize)) } }
            else -> null
        },
        modifier = modifier,
        colors = colors
    )
}
