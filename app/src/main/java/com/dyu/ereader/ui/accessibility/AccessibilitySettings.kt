package com.dyu.ereader.ui.accessibility

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dyu.ereader.ui.reader.ReaderViewModel

@Composable
fun AccessibilitySettings(
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val fontSize = uiState.settings.fontSizeSp
    val lineSpacing = uiState.settings.lineSpacing
    val horizontalMargin = uiState.settings.horizontalMarginDp

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Reading Accessibility",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Font Size
            AccessibilitySlider(
                icon = Icons.Rounded.TextFormat,
                label = "Font Size",
                value = fontSize,
                onValueChange = { viewModel.setReaderFontSize(it) },
                valueRange = 12f..36f,
                steps = 11,
                displayValue = "${fontSize.toInt()}sp"
            )

            // Line Spacing
            AccessibilitySlider(
                icon = Icons.Rounded.FormatLineSpacing,
                label = "Line Spacing",
                value = lineSpacing,
                onValueChange = { viewModel.setLineSpacing(it) },
                valueRange = 1.0f..3.0f,
                steps = 19,
                displayValue = "${"%.1f".format(lineSpacing)}x"
            )

            // Horizontal Margin
            AccessibilitySlider(
                icon = Icons.Rounded.Margin,
                label = "Page Margins",
                value = horizontalMargin,
                onValueChange = { viewModel.setMargins(it) },
                valueRange = 0f..80f,
                steps = 15,
                displayValue = "${horizontalMargin.toInt()}dp"
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Toggles
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AccessibilityToggle(
                    icon = Icons.Rounded.CenterFocusStrong,
                    title = "Focus Mode",
                    desc = "Minimize distractions while reading",
                    checked = uiState.settings.focusMode,
                    onCheckedChange = { viewModel.setFocusMode(it) }
                )

                AccessibilityToggle(
                    icon = Icons.Rounded.HistoryEdu,
                    title = "Publisher Style",
                    desc = "Use the book's original styling",
                    checked = uiState.settings.usePublisherStyle,
                    onCheckedChange = { viewModel.setUsePublisherStyle(it) }
                )
            }
        }
    }
}

@Composable
private fun AccessibilitySlider(
    icon: ImageVector,
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    displayValue: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            }
            Text(displayValue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        )
    }
}

@Composable
private fun AccessibilityToggle(
    icon: ImageVector,
    title: String,
    desc: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}
