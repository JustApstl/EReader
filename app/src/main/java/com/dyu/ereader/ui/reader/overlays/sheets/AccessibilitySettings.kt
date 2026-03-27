package com.dyu.ereader.ui.reader.overlays.sheets

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.FormatLineSpacing
import androidx.compose.material.icons.rounded.HistoryEdu
import androidx.compose.material.icons.rounded.Margin
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dyu.ereader.ui.reader.overlays.components.ReaderPanelScaffold
import com.dyu.ereader.ui.reader.overlays.components.ReaderSheetSection
import com.dyu.ereader.ui.reader.settings.components.SliderSetting
import com.dyu.ereader.ui.reader.settings.components.ToggleRow
import com.dyu.ereader.ui.reader.viewmodel.ReaderViewModel

@Composable
fun AccessibilitySettings(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val fontSize = uiState.settings.fontSizeSp
    val lineSpacing = uiState.settings.lineSpacing
    val horizontalMargin = uiState.settings.horizontalMarginDp

    ReaderPanelScaffold(
        title = "Accessibility",
        icon = Icons.Rounded.Visibility,
        onDismiss = onDismiss,
        closeContentDescription = "Close Accessibility",
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        ReaderSheetSection(
            title = "Readable Layout",
            icon = Icons.Rounded.TextFields
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                SliderSetting(
                    icon = Icons.Rounded.TextFields,
                    label = "Font Size",
                    value = fontSize,
                    onValueChange = { viewModel.setReaderFontSize(it) },
                    valueRange = 12f..36f,
                    onReset = {},
                    valueDisplay = { "${it.toInt()}sp" }
                )
                SliderSetting(
                    icon = Icons.Rounded.FormatLineSpacing,
                    label = "Line Spacing",
                    value = lineSpacing,
                    onValueChange = { viewModel.setLineSpacing(it) },
                    valueRange = 1.0f..3.0f,
                    onReset = {},
                    valueDisplay = { "${"%.1f".format(it)}x" }
                )
                SliderSetting(
                    icon = Icons.Rounded.Margin,
                    label = "Page Margins",
                    value = horizontalMargin,
                    onValueChange = { viewModel.setMargins(it) },
                    valueRange = 0f..80f,
                    onReset = {},
                    valueDisplay = { "${it.toInt()}dp" }
                )
            }
        }

        ReaderSheetSection(
            title = "Reading Assistance",
            icon = Icons.Rounded.CenterFocusStrong
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleRow(
                    icon = Icons.Rounded.CenterFocusStrong,
                    title = "Focus Mode",
                    desc = "Hide extra chrome until you tap the page again.",
                    checked = uiState.settings.focusMode,
                    onToggle = { viewModel.setFocusMode(it) }
                )
                ToggleRow(
                    icon = Icons.Rounded.HistoryEdu,
                    title = "Publisher Style",
                    desc = "Use the book's own typography, spacing, and layout.",
                    checked = uiState.settings.usePublisherStyle,
                    onToggle = { viewModel.setUsePublisherStyle(it) }
                )
            }
        }
    }
}
