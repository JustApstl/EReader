package com.dyu.ereader.ui.reader.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.reader.FontColorTheme
import com.dyu.ereader.data.model.reader.getColor

@Composable
internal fun FontColorSelector(
    currentTheme: FontColorTheme,
    customColor: Int?,
    enabled: Boolean,
    onThemeChange: (FontColorTheme) -> Unit,
    onPickCustomColor: () -> Unit
) {
    val themes = FontColorTheme.entries
    Box(modifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures { _, _ -> }
    }) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(themes) { theme ->
                val isSelected = currentTheme == theme
                val color = theme.getColor(customColor) ?: MaterialTheme.colorScheme.onSurface

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(color.copy(alpha = if (enabled) 1f else 0.5f), CircleShape)
                            .border(
                                width = if (isSelected) { 3.dp } else { 1.dp },
                                color = if (isSelected) { MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) } else { MaterialTheme.colorScheme.outlineVariant },
                                shape = CircleShape
                            )
                            .clickable(enabled = enabled) {
                                if (!enabled) return@clickable
                                if (theme == FontColorTheme.CUSTOM) {
                                    if (isSelected) onPickCustomColor()
                                    else if (customColor != null) onThemeChange(FontColorTheme.CUSTOM)
                                    else onPickCustomColor()
                                } else onThemeChange(theme)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                Icons.Rounded.Check,
                                null,
                                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        } else if (theme == FontColorTheme.CUSTOM) {
                            Icon(
                                Icons.Rounded.ColorLens,
                                null,
                                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Text(
                        theme.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.45f)
                    )
                }
            }
        }
    }
}
