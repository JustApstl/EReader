package com.dyu.ereader.ui.reader.settings.tabs

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatAlignLeft
import androidx.compose.material.icons.automirrored.rounded.FormatIndentIncrease
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.FormatLineSpacing
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.reader.FontColorTheme
import com.dyu.ereader.data.model.reader.ReaderFont
import com.dyu.ereader.data.model.reader.ReaderTextElement
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.TextAlignment
import com.dyu.ereader.ui.reader.settings.components.*

@Composable
internal fun TypefaceTabContent(
    settings: ReaderSettings,
    styleEditable: Boolean,
    fontSizeChanged: Boolean,
    lineSpacingChanged: Boolean,
    marginChanged: Boolean,
    onUsePublisherStyleToggle: (Boolean) -> Unit,
    onPickCustomFont: () -> Unit,
    onFontChange: (ReaderFont) -> Unit,
    onClearCustomFont: () -> Unit,
    onFontColorThemeChange: (FontColorTheme) -> Unit,
    onPickCustomFontColor: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontSizeChangeFinished: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onLineSpacingChangeFinished: (Float) -> Unit,
    onMarginChange: (Float) -> Unit,
    onMarginChangeFinished: (Float) -> Unit,
    onTextAlignmentChange: (TextAlignment) -> Unit,
    onElementStyleFontChange: (ReaderTextElement, ReaderFont) -> Unit,
    onElementStyleColorChange: (ReaderTextElement, Int?) -> Unit,
    onOpenElementColorPicker: (ReaderTextElement) -> Unit
) {
    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (!styleEditable) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                Icons.Rounded.Lock,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Column {
                                Text(
                                    "Publisher Style is On",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Turn it off to change fonts and colors.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        TextButton(onClick = { onUsePublisherStyleToggle(false) }) {
                            Text("Turn Off")
                        }
                    }
                }
            }

            SettingsSection(title = "Font", icon = Icons.Rounded.FontDownload) {
                FontSelector(
                    currentFont = settings.font,
                    hasCustomFont = !settings.customFontUri.isNullOrBlank(),
                    customFontLabel = settings.customFontUri.toReadableCustomFontName(),
                    onPickCustomFont = onPickCustomFont,
                    onFontChange = onFontChange,
                    onClearCustomFont = onClearCustomFont,
                    enabled = true
                )
            }

            SettingsSection(title = "Font Color", icon = Icons.Rounded.FormatColorText) {
                FontColorSelector(
                    currentTheme = settings.fontColorTheme,
                    customColor = settings.customFontColor,
                    enabled = true,
                    onThemeChange = onFontColorThemeChange,
                    onPickCustomColor = onPickCustomFontColor
                )
            }

            SettingsSection(title = "Typography", icon = Icons.Rounded.FormatSize) {
                SliderSetting(
                    label = "Font Size",
                    value = settings.fontSizeSp,
                    valueRange = 12f..36f,
                    onValueChange = onFontSizeChange,
                    onValueChangeFinished = onFontSizeChangeFinished,
                    onReset = { onFontSizeChangeFinished(15f) },
                    icon = Icons.Rounded.FormatSize,
                    valueDisplay = { "${it.toInt()}" },
                    enabled = true,
                    showReset = fontSizeChanged
                )

                SliderSetting(
                    label = "Line Spacing",
                    value = settings.lineSpacing,
                    valueRange = 1.0f..2.5f,
                    onValueChange = onLineSpacingChange,
                    onValueChangeFinished = onLineSpacingChangeFinished,
                    onReset = { onLineSpacingChangeFinished(1.55f) },
                    icon = Icons.Rounded.FormatLineSpacing,
                    valueDisplay = { "%.1f".format(it) },
                    enabled = true,
                    showReset = lineSpacingChanged
                )

                SliderSetting(
                    label = "Horizontal Margins",
                    value = settings.horizontalMarginDp,
                    valueRange = 0f..60f,
                    onValueChange = onMarginChange,
                    onValueChangeFinished = onMarginChangeFinished,
                    onReset = { onMarginChangeFinished(20f) },
                    icon = Icons.AutoMirrored.Rounded.FormatIndentIncrease,
                    valueDisplay = { "${it.toInt()}" },
                    enabled = true,
                    showReset = marginChanged
                )
            }

            SettingsSection(title = "Text Alignment", icon = Icons.AutoMirrored.Rounded.FormatAlignLeft) {
                Box(modifier = Modifier.pointerInput(Unit) {
                    detectHorizontalDragGestures { _, _ -> }
                }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TextAlignment.entries) { alignment ->
                            FilterChip(
                                selected = settings.textAlignment == alignment,
                                onClick = { onTextAlignmentChange(alignment) },
                                label = alignment.label,
                                enabled = true
                            )
                        }
                    }
                }
            }

            SettingsSection(title = "Advanced Element Styling", icon = Icons.Rounded.FormatColorText) {
                ElementStylingEditor(
                    settings = settings,
                    enabled = styleEditable,
                    onElementStyleFontChange = onElementStyleFontChange,
                    onElementStyleColorChange = onElementStyleColorChange,
                    onOpenElementColorPicker = onOpenElementColorPicker
                )
            }
        }
    }
}

@Composable
private fun ElementStylingEditor(
    settings: ReaderSettings,
    enabled: Boolean,
    onElementStyleFontChange: (ReaderTextElement, ReaderFont) -> Unit,
    onElementStyleColorChange: (ReaderTextElement, Int?) -> Unit,
    onOpenElementColorPicker: (ReaderTextElement) -> Unit
) {
    var selectedElementName by rememberSaveable { mutableStateOf(ReaderTextElement.PARAGRAPH.name) }
    val selectedElement = remember(selectedElementName) {
        ReaderTextElement.entries.firstOrNull { it.name == selectedElementName } ?: ReaderTextElement.PARAGRAPH
    }
    val selectedStyle = settings.elementStyles.styleFor(selectedElement)
    val availableFonts = remember(settings.customFontUri) {
        ReaderFont.entries.filter { font ->
            font != ReaderFont.CUSTOM || !settings.customFontUri.isNullOrBlank()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "Give paragraphs, headings, and links their own font and color without changing the whole page.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(modifier = Modifier.pointerInput(Unit) {
            detectHorizontalDragGestures { _, _ -> }
        }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ReaderTextElement.entries) { element ->
                    FilterChip(
                        selected = selectedElement == element,
                        onClick = { selectedElementName = element.name },
                        label = element.label,
                        enabled = enabled
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "${selectedElement.label} Font",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Box(modifier = Modifier.pointerInput(Unit) {
                detectHorizontalDragGestures { _, _ -> }
            }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(availableFonts) { font ->
                        FilterChip(
                            selected = selectedStyle.font == font,
                            onClick = { onElementStyleFontChange(selectedElement, font) },
                            label = if (font == ReaderFont.DEFAULT) "Original" else font.label,
                            enabled = enabled
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${selectedElement.label} Color",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(22.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = selectedStyle.color?.let(::Color) ?: MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                    ) {}
                    Text(
                        text = if (selectedStyle.color != null) "Custom color active" else "Original color",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        enabled = enabled,
                        onClick = { onOpenElementColorPicker(selectedElement) }
                    ) {
                        Text(if (selectedStyle.color != null) "Change" else "Choose")
                    }
                    if (selectedStyle.color != null) {
                        TextButton(
                            enabled = enabled,
                            onClick = { onElementStyleColorChange(selectedElement, null) }
                        ) {
                            Text("Original")
                        }
                    }
                }
            }
        }
    }
}

private fun String?.toReadableCustomFontName(): String? {
    val raw = this ?: return null
    val decoded = runCatching { Uri.decode(raw) }.getOrDefault(raw)
    val lastSegment = decoded.substringAfterLast('/').substringBefore('?').substringBefore('#')
    if (lastSegment.isBlank()) return null

    return lastSegment
        .substringBeforeLast('.')
        .replace('_', ' ')
        .replace('-', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(22)
        .ifBlank { null }
}
