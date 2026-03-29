package com.dyu.ereader.ui.home.settings

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyu.ereader.data.model.reader.ReaderFont
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.ReaderTheme
import com.dyu.ereader.data.model.reader.TextAlignment
import com.dyu.ereader.data.model.reader.getBackgroundColor
import com.dyu.ereader.data.model.reader.getColor

internal data class ChoiceOption(
    val label: String,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
internal fun ReaderSettingSubhead(
    title: String,
    description: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun ReaderSettingsSectionCard(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
internal fun ReaderFontSelector(
    currentFont: ReaderFont,
    customFontUri: String?,
    onFontSelected: (ReaderFont) -> Unit,
    onPickCustomFont: () -> Unit,
    onClearCustomFont: () -> Unit
) {
    val customFontLabel = remember(customFontUri) { customFontUri.toReadableCustomFontName() }
    val hasCustomFont = !customFontUri.isNullOrBlank()

    Box(modifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures { _, _ -> }
    }) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(ReaderFont.entries) { font ->
                if (font != ReaderFont.CUSTOM || hasCustomFont) {
                    val selected = currentFont == font
                    val label = when {
                        font == ReaderFont.CUSTOM && hasCustomFont -> customFontLabel ?: "CUSTOM"
                        font == ReaderFont.DEFAULT -> "ORIGINAL"
                        else -> font.label.uppercase()
                    }
                    Surface(
                        onClick = {
                            if (font == ReaderFont.CUSTOM && !hasCustomFont) {
                                onPickCustomFont()
                            } else {
                                onFontSelected(font)
                            }
                        },
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                            } else {
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelLarge,
                                fontFamily = font.previewFontFamily(),
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            item("custom_font_action") {
                Surface(
                    onClick = onPickCustomFont,
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FontDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = if (hasCustomFont) "REPLACE CUSTOM" else "ADD CUSTOM",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (hasCustomFont) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = customFontLabel ?: "Custom font selected",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedButton(
                onClick = onClearCustomFont,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Remove Custom")
            }
        }
    }
}

@Composable
internal fun ChoiceButtons(
    options: List<ChoiceOption>,
    columns: Int
) {
    val safeColumns = columns.coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        options.chunked(safeColumns).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowOptions.forEach { option ->
                    if (option.selected) {
                        Button(
                            onClick = option.onClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(option.label)
                        }
                    } else {
                        OutlinedButton(
                            onClick = option.onClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text(option.label)
                        }
                    }
                }
                repeat(safeColumns - rowOptions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

internal fun ReaderFont.previewFontFamily(): FontFamily = when (this) {
    ReaderFont.SANS -> FontFamily.SansSerif
    ReaderFont.SERIF -> FontFamily.Serif
    ReaderFont.MONO -> FontFamily.Monospace
    ReaderFont.CURSIVE -> FontFamily.Cursive
    else -> FontFamily.Default
}

@Composable
internal fun ReaderThemeButtons(
    currentTheme: ReaderTheme,
    customBackgroundColor: Int?,
    onThemeSelected: (ReaderTheme) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            listOf(
                ReaderTheme.SYSTEM,
                ReaderTheme.WHITE,
                ReaderTheme.SEPIA,
                ReaderTheme.DARK,
                ReaderTheme.BLACK,
                ReaderTheme.CUSTOM
            )
        ) { theme ->
            ReaderThemeButton(
                theme = theme,
                customBackgroundColor = customBackgroundColor,
                isSelected = currentTheme == theme,
                onClick = { onThemeSelected(theme) }
            )
        }
    }
}

@Composable
internal fun ReaderThemeButton(
    theme: ReaderTheme,
    customBackgroundColor: Int?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val previewColor = when (theme) {
        ReaderTheme.SYSTEM -> theme.getBackgroundColor(isDarkTheme = false)
        ReaderTheme.WHITE -> theme.getBackgroundColor(false)
        ReaderTheme.SEPIA -> theme.getBackgroundColor(false)
        ReaderTheme.DARK -> theme.getBackgroundColor(true)
        ReaderTheme.BLACK -> theme.getBackgroundColor(true)
        ReaderTheme.DEFAULT -> Color.White
        ReaderTheme.CUSTOM -> customBackgroundColor?.let(::Color) ?: MaterialTheme.colorScheme.primaryContainer
        ReaderTheme.IMAGE -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier.size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                onClick = onClick,
                modifier = Modifier.matchParentSize(),
                shape = CircleShape,
                color = previewColor,
                border = BorderStroke(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f)
                    }
                )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = if (previewColor.luminance() > 0.55f) Color.Black else Color.White
                        )
                    }
                }
            }
        }
        Text(
            text = theme.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun ReaderPreviewCard(settings: ReaderSettings) {
    val previewBg = settings.readerTheme.getBackgroundColor(
        isDarkTheme = false,
        customColor = settings.customBackgroundColor
    )
    val previewTextColor = settings.fontColorTheme.getColor(settings.customFontColor)
        ?: MaterialTheme.colorScheme.onSurface
    val horizontalPadding = settings.horizontalMarginDp.coerceIn(8f, 44f).dp
    val bodyAlign = when (settings.textAlignment) {
        TextAlignment.LEFT -> TextAlign.Left
        TextAlignment.CENTER -> TextAlign.Center
        TextAlignment.RIGHT -> TextAlign.Right
        TextAlignment.JUSTIFY -> TextAlign.Justify
        else -> TextAlign.Start
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = previewBg,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Reading Preview",
                    style = MaterialTheme.typography.labelLarge,
                    color = previewTextColor.copy(alpha = 0.9f),
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "See how background, spacing, margin, and alignment work together.",
                    style = MaterialTheme.typography.bodySmall,
                    color = previewTextColor.copy(alpha = 0.76f)
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "The Quiet Chapter",
                    modifier = Modifier.fillMaxWidth(),
                    color = previewTextColor,
                    fontFamily = settings.font.previewFontFamily(),
                    fontWeight = FontWeight.Bold,
                    textAlign = bodyAlign,
                    fontSize = (settings.fontSizeSp + 3f).sp,
                    lineHeight = ((settings.fontSizeSp + 3f) * settings.lineSpacing).sp
                )
                Text(
                    text = "A thin breeze moved through the old shelves while pages turned softly. The room settled into the kind of silence that makes every line easier to follow.",
                    modifier = Modifier.fillMaxWidth(),
                    color = previewTextColor,
                    fontFamily = settings.font.previewFontFamily(),
                    textAlign = bodyAlign,
                    fontSize = settings.fontSizeSp.sp,
                    lineHeight = (settings.fontSizeSp * settings.lineSpacing).sp
                )
                Text(
                    text = "Reading should feel calm, balanced, and easy to track at a glance.",
                    modifier = Modifier.fillMaxWidth(),
                    color = previewTextColor.copy(alpha = 0.84f),
                    fontFamily = settings.font.previewFontFamily(),
                    textAlign = bodyAlign,
                    fontSize = (settings.fontSizeSp - 1f).coerceAtLeast(12f).sp,
                    lineHeight = ((settings.fontSizeSp - 1f).coerceAtLeast(12f) * settings.lineSpacing).sp
                )
            }
        }
    }
}

internal fun String?.toReadableCustomFontName(): String? {
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
