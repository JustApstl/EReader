package com.dyu.ereader.ui.home.settings

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.app.AppAccent
import com.dyu.ereader.data.model.app.AppFont
import com.dyu.ereader.data.model.app.AppTheme
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.ui.app.theme.previewFontFamily
import com.dyu.ereader.ui.app.theme.resolveAccentColor
import com.dyu.ereader.ui.app.theme.systemAccentPreviewColor
import com.dyu.ereader.ui.components.inputs.appSegmentedButtonColors
import com.dyu.ereader.ui.home.state.HomeUiState
import kotlin.math.roundToInt

@Composable
internal fun AppearanceSection(
    uiState: HomeUiState,
    appTheme: AppTheme,
    appFont: AppFont,
    appAccent: AppAccent,
    customAccentColor: Int?,
    navBarStyle: NavigationBarStyle,
    liquidGlassEnabled: Boolean,
    onShowAccentColorPicker: () -> Unit,
    events: SettingsEvents
) {
    SettingsCard(title = "Appearance", icon = Icons.Rounded.Palette, liquidGlassEnabled = liquidGlassEnabled) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column {
                Text(
                    "App Theme",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AppTheme.entries.forEach { theme ->
                        ThemeItem(
                            theme = theme,
                            isSelected = appTheme == theme,
                            onClick = { events.onAppThemeChange(theme) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column {
                Text(
                    "App Font",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(14.dp))
                AppFontSelector(
                    currentFont = appFont,
                    onFontChange = events.onAppFontChange
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column {
                Text(
                    "Accent Color",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(14.dp))
                AccentColorSelector(
                    currentAccent = appAccent,
                    customAccentColor = customAccentColor,
                    onAccentChange = events.onAppAccentChange,
                    onPickCustomColor = onShowAccentColorPicker
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            ThemePreviewPanel(
                appAccent = appAccent,
                customAccentColor = customAccentColor
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "App Text Size",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                SettingSlider(
                    title = "Scale",
                    value = uiState.display.appTextScale,
                    valueRange = 0.85f..1.25f,
                    onValueChange = events.onAppTextScaleChange,
                    valueLabel = { "${(it * 100).roundToInt()}%" },
                    steps = 7,
                    showReset = uiState.display.appTextScale != 1f,
                    onReset = { events.onAppTextScaleChange(1f) }
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column {
                Text(
                    "Navigation Bar Style",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(12.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    NavigationBarStyle.entries.forEachIndexed { index, style ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = NavigationBarStyle.entries.size),
                            onClick = { events.onNavigationBarStyleChange(style) },
                            selected = navBarStyle == style,
                            colors = appSegmentedButtonColors(),
                            label = {
                                Text(style.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            SettingSwitch(
                title = "Liquid Glass",
                beta = true,
                desc = "Enable frosted glass effect",
                checked = liquidGlassEnabled,
                onCheckedChange = events.onLiquidGlassToggle
            )

            SettingSwitch(
                title = "Disable Animation",
                desc = "Turn off custom UI animations",
                checked = !uiState.display.animationsEnabled,
                onCheckedChange = { events.onAnimationsToggle(!it) }
            )

            SettingSwitch(
                title = "Disable Haptics",
                desc = "Turn off vibration feedback",
                checked = !uiState.display.hapticsEnabled,
                onCheckedChange = { events.onHapticsToggle(!it) }
            )

            SettingSwitch(
                title = "Disable Text Scroller",
                desc = "Stop auto-scrolling long titles and authors",
                checked = !uiState.display.textScrollerEnabled,
                onCheckedChange = { events.onTextScrollerToggle(!it) },
                beta = true
            )
        }
    }
}

@Composable
private fun AppFontSelector(
    currentFont: AppFont,
    onFontChange: (AppFont) -> Unit
) {
    Box(modifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures { _, _ -> }
    }) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(AppFont.entries) { font ->
                val selected = currentFont == font
                Surface(
                    onClick = { onFontChange(font) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    } else {
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                    },
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                        } else {
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = font.label.uppercase(),
                            style = MaterialTheme.typography.labelLarge,
                            fontFamily = font.previewFontFamily(),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AccentColorSelector(
    currentAccent: AppAccent,
    customAccentColor: Int? = null,
    onAccentChange: (AppAccent) -> Unit,
    onPickCustomColor: () -> Unit
) {
    val systemAccentPreview = systemAccentPreviewColor(MaterialTheme.colorScheme.primary)
    Box(modifier = Modifier.pointerInput(Unit) {
        detectHorizontalDragGestures { _, _ -> }
    }) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(AppAccent.entries) { accent ->
                val isSelected = currentAccent == accent
                val previewColor = if (accent == AppAccent.SYSTEM) {
                    systemAccentPreview
                } else {
                    accent.resolveAccentColor(
                        customAccentColor = customAccentColor,
                        fallback = systemAccentPreview
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(previewColor, CircleShape)
                            .clip(CircleShape)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .clickable {
                                if (accent == AppAccent.CUSTOM) {
                                    if (isSelected || customAccentColor == null) {
                                        onPickCustomColor()
                                    } else {
                                        onAccentChange(AppAccent.CUSTOM)
                                    }
                                } else {
                                    onAccentChange(accent)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isSelected -> {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (previewColor.luminance() > 0.5f) Color.Black else Color.White
                                )
                            }
                            accent == AppAccent.CUSTOM -> {
                                Icon(
                                    Icons.Rounded.ColorLens,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (previewColor.luminance() > 0.5f) Color.Black else Color.White
                                )
                            }
                        }
                    }
                    Text(
                        text = accent.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
