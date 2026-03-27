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
import androidx.compose.material.icons.rounded.Image
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
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.ReaderTheme
import com.dyu.ereader.data.model.reader.getBackgroundColor

@Composable
internal fun ThemeSelector(
    settings: ReaderSettings,
    isDarkTheme: Boolean,
    onThemeChange: (ReaderTheme) -> Unit,
    onPickCustomColor: () -> Unit,
    onPickBackgroundImage: () -> Unit,
    enabled: Boolean
) {
    val themes = listOf(
        ReaderTheme.SYSTEM,
        ReaderTheme.WHITE,
        ReaderTheme.SEPIA,
        ReaderTheme.DARK,
        ReaderTheme.BLACK,
        ReaderTheme.CUSTOM,
        ReaderTheme.IMAGE
    )

    Box(modifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures { _, _ -> }
    }) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(themes) { theme ->
                val isSelected = settings.readerTheme == theme
                val themeBg = theme.getBackgroundColor(isDarkTheme, settings.customBackgroundColor)

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .background(
                                if (theme == ReaderTheme.IMAGE) {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                } else {
                                    themeBg
                                },
                                CircleShape
                            )
                            .border(
                                width = if (isSelected) { 3.dp } else { 1.dp },
                                color = if (isSelected) { MaterialTheme.colorScheme.primary.copy(alpha = 0.8f) } else { MaterialTheme.colorScheme.outlineVariant },
                                shape = CircleShape
                            )
                            .clickable(enabled = enabled) {
                                if (!enabled) return@clickable
                                when (theme) {
                                    ReaderTheme.CUSTOM -> {
                                        if (isSelected) onPickCustomColor()
                                        else if (settings.customBackgroundColor != null) onThemeChange(ReaderTheme.CUSTOM)
                                        else onPickCustomColor()
                                    }
                                    ReaderTheme.IMAGE -> {
                                        if (isSelected) onPickBackgroundImage()
                                        else if (settings.backgroundImageUri != null) onThemeChange(ReaderTheme.IMAGE)
                                        else onPickBackgroundImage()
                                    }
                                    else -> onThemeChange(theme)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (theme) {
                            ReaderTheme.CUSTOM -> Icon(Icons.Rounded.ColorLens, null, tint = if (themeBg.luminance() > 0.5f) Color.Black else Color.White)
                            ReaderTheme.IMAGE -> Icon(Icons.Rounded.Image, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            else -> {
                                if (isSelected) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        null,
                                        tint = if (themeBg.luminance() > 0.5f) Color.Black else Color.White
                                    )
                                }
                            }
                        }
                    }
                    Text(theme.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
