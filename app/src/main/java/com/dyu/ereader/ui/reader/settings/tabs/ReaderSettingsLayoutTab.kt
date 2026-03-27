package com.dyu.ereader.ui.reader.settings.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.DisplaySettings
import androidx.compose.material.icons.rounded.Filter
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material.icons.rounded.ViewInAr
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.reader.ImageFilter
import com.dyu.ereader.data.model.reader.PageTransitionStyle
import com.dyu.ereader.data.model.reader.ReadingPreset
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.ReaderTheme
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.ui.reader.settings.components.*

@Composable
internal fun LayoutTabContent(
    settings: ReaderSettings,
    isDarkTheme: Boolean,
    styleEditable: Boolean,
    onApplyPreset: (ReadingPreset) -> Unit,
    onReadingModeChange: (ReadingMode) -> Unit,
    onPageTurn3dToggle: (Boolean) -> Unit,
    onInvertPageTurnsToggle: (Boolean) -> Unit,
    onPageTransitionStyleChange: (PageTransitionStyle) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onPickCustomColor: () -> Unit,
    onPickBackgroundImage: () -> Unit,
    onBackgroundImageBlurChange: (Float) -> Unit,
    onBackgroundImageBlurChangeFinished: (Float) -> Unit,
    onBackgroundImageOpacityChange: (Float) -> Unit,
    onBackgroundImageOpacityChangeFinished: (Float) -> Unit,
    onBackgroundImageZoomChange: (Float) -> Unit,
    onBackgroundImageZoomChangeFinished: (Float) -> Unit,
    onImageFilterChange: (ImageFilter) -> Unit
) {
    val defaultSettings = remember { ReaderSettings() }
    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            SettingsSection(title = "Display Mode", icon = Icons.Rounded.DisplaySettings) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip(
                        selected = settings.readingMode == ReadingMode.SCROLL,
                        onClick = { onReadingModeChange(ReadingMode.SCROLL) },
                        label = "Scrolled",
                        icon = Icons.Rounded.VerticalAlignBottom,
                        modifier = Modifier.weight(1f)
                    )
                    ModeChip(
                        selected = settings.readingMode == ReadingMode.PAGE,
                        onClick = { onReadingModeChange(ReadingMode.PAGE) },
                        label = "Paged",
                        icon = Icons.Rounded.AutoStories,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (settings.readingMode == ReadingMode.PAGE) {
                    ToggleRow(
                        title = "3D Page Turn",
                        desc = "Add depth-style page-turn animation in paged mode.",
                        checked = settings.pageTurn3d,
                        icon = Icons.Rounded.ViewInAr,
                        onToggle = onPageTurn3dToggle,
                        beta = true
                    )

                    ToggleRow(
                        title = "Invert Pagination",
                        desc = "Reverse swipe direction for previous and next pages.",
                        checked = settings.invertPageTurns,
                        icon = Icons.Rounded.AutoStories,
                        onToggle = onInvertPageTurnsToggle,
                        beta = true
                    )

                    if (settings.pageTurn3d) {
                        SettingsSection(
                            title = "Page Transition",
                            icon = Icons.Rounded.Animation
                        ) {
                            val transitionStyles = listOf(
                                PageTransitionStyle.DEFAULT,
                                PageTransitionStyle.TILT,
                                PageTransitionStyle.CARD,
                                PageTransitionStyle.FLIP,
                                PageTransitionStyle.CUBE,
                                PageTransitionStyle.ROLL,
                                PageTransitionStyle.PAPER
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(transitionStyles) { style ->
                                    FilterChip(
                                        selected = settings.pageTransitionStyle == style,
                                        onClick = { onPageTransitionStyleChange(style) },
                                        label = style.name.lowercase().replaceFirstChar { it.uppercase() }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            SettingsSection(title = "Background Theme", icon = Icons.Rounded.Palette) {
                ThemeSelector(
                    settings = settings,
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange,
                    onPickCustomColor = onPickCustomColor,
                    onPickBackgroundImage = onPickBackgroundImage,
                    enabled = styleEditable
                )
            }

            if (settings.readerTheme == ReaderTheme.IMAGE && settings.backgroundImageUri != null) {
                SettingsSection(title = "Image Background", icon = Icons.Rounded.BlurOn) {
                    SliderSetting(
                        label = "Background Blur",
                        value = settings.backgroundImageBlur,
                        valueRange = 0f..20f,
                        onValueChange = onBackgroundImageBlurChange,
                        onValueChangeFinished = onBackgroundImageBlurChangeFinished,
                        onReset = { onBackgroundImageBlurChangeFinished(0f) },
                        icon = Icons.Rounded.BlurOn,
                        valueDisplay = { "${it.toInt()}px" },
                        enabled = styleEditable,
                        showReset = settings.backgroundImageBlur != defaultSettings.backgroundImageBlur
                    )
                    SliderSetting(
                        label = "Background Opacity",
                        value = settings.backgroundImageOpacity,
                        valueRange = 0.1f..1f,
                        onValueChange = onBackgroundImageOpacityChange,
                        onValueChangeFinished = onBackgroundImageOpacityChangeFinished,
                        onReset = { onBackgroundImageOpacityChangeFinished(1f) },
                        icon = Icons.Rounded.Opacity,
                        valueDisplay = { "${(it * 100).toInt()}%" },
                        enabled = styleEditable,
                        showReset = settings.backgroundImageOpacity != defaultSettings.backgroundImageOpacity
                    )
                    SliderSetting(
                        label = "Background Zoom",
                        value = settings.backgroundImageZoom,
                        valueRange = 0.5f..3f,
                        onValueChange = onBackgroundImageZoomChange,
                        onValueChangeFinished = onBackgroundImageZoomChangeFinished,
                        onReset = { onBackgroundImageZoomChangeFinished(1f) },
                        icon = Icons.Rounded.ZoomIn,
                        valueDisplay = { "x${"%.1f".format(it)}" },
                        enabled = styleEditable,
                        showReset = settings.backgroundImageZoom != defaultSettings.backgroundImageZoom
                    )
                }
            }

            SettingsSection(title = "Image Filter", icon = Icons.Rounded.Filter) {
                Box(modifier = Modifier.pointerInput(Unit) {
                    detectHorizontalDragGestures { _, _ -> }
                }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            FilterChip(
                                selected = settings.imageFilter == ImageFilter.NONE,
                                onClick = { onImageFilterChange(ImageFilter.NONE) },
                                label = "Original",
                                enabled = styleEditable
                            )
                        }
                        item {
                            FilterChip(
                                selected = settings.imageFilter == ImageFilter.AUTO,
                                onClick = { onImageFilterChange(ImageFilter.AUTO) },
                                label = "Auto",
                                enabled = styleEditable
                            )
                        }
                        item {
                            FilterChip(
                                selected = settings.imageFilter == ImageFilter.BW,
                                onClick = { onImageFilterChange(ImageFilter.BW) },
                                label = "B&W",
                                enabled = styleEditable
                            )
                        }
                        item {
                            FilterChip(
                                selected = settings.imageFilter == ImageFilter.INVERT,
                                onClick = { onImageFilterChange(ImageFilter.INVERT) },
                                label = "Invert",
                                enabled = styleEditable
                            )
                        }
                        item {
                            FilterChip(
                                selected = settings.imageFilter == ImageFilter.DARKEN,
                                onClick = { onImageFilterChange(ImageFilter.DARKEN) },
                                label = "Darken",
                                enabled = styleEditable
                            )
                        }
                    }
                }
            }
        }
    }
}
