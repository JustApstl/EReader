package com.dyu.ereader.ui.reader.settings.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FilterCenterFocus
import androidx.compose.material.icons.rounded.FormatBold
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.TextRotationNone
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.reader.ReaderTapZoneAction
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.reader.settings.components.*
import kotlin.math.abs

@Composable
internal fun ExperienceTabContent(
    settings: ReaderSettings,
    onFocusModeToggle: (Boolean) -> Unit,
    onUsePublisherStyleToggle: (Boolean) -> Unit,
    onHideStatusBarToggle: (Boolean) -> Unit,
    onUnderlineLinksToggle: (Boolean) -> Unit,
    onTextShadowToggle: (Boolean) -> Unit,
    onTextShadowColorChange: (Int?) -> Unit,
    onPickTextShadowColor: () -> Unit,
    onAmbientModeToggle: (Boolean) -> Unit,
    onTapZoneActionChange: (String, ReaderTapZoneAction) -> Unit,
    onFocusTextToggle: (Boolean) -> Unit,
    onFocusTextBoldnessChange: (Int) -> Unit,
    onFocusTextBoldnessChangeFinished: (Int) -> Unit,
    onFocusTextEmphasisChange: (Float) -> Unit,
    onFocusTextEmphasisChangeFinished: (Float) -> Unit,
    onFocusTextColorChange: (Int?) -> Unit,
    onPickBionicColor: () -> Unit
) {
    val defaultSettings = remember { ReaderSettings() }

    SettingsCard {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SettingsSection(title = "Reader Experience", icon = Icons.Rounded.FilterCenterFocus) {
                ToggleRow(
                    title = "Focus Mode",
                    desc = "Hide extra chrome until you tap the page again.",
                    checked = settings.focusMode,
                    icon = Icons.Rounded.FilterCenterFocus,
                    onToggle = onFocusModeToggle
                )

                ToggleRow(
                    title = "Publisher Style",
                    desc = "Use the book's own typography, spacing, and layout.",
                    checked = settings.usePublisherStyle,
                    icon = Icons.Rounded.AutoFixHigh,
                    onToggle = onUsePublisherStyleToggle,
                    beta = true
                )

                ToggleRow(
                    title = "Hide Status Bar",
                    desc = "Use more of the screen while reading.",
                    checked = settings.hideStatusBar,
                    icon = Icons.Rounded.Fullscreen,
                    onToggle = onHideStatusBarToggle
                )

                ToggleRow(
                    title = "Ambient Mode",
                    desc = "Dim the reader chrome for a calmer reading view.",
                    checked = settings.ambientMode,
                    icon = Icons.Rounded.AutoFixHigh,
                    onToggle = onAmbientModeToggle
                )
            }

            SettingsSection(title = "Text Effects", icon = Icons.Rounded.TextRotationNone) {
                ToggleRow(
                    title = "Underline Links",
                    desc = "Make chapter and reference links easier to spot.",
                    checked = settings.underlineLinks,
                    icon = Icons.Rounded.Link,
                    onToggle = onUnderlineLinksToggle,
                    beta = true
                )

                ToggleRow(
                    title = "Text Shadow",
                    desc = "Add subtle contrast between text and the background.",
                    checked = settings.textShadow,
                    icon = Icons.Rounded.TextRotationNone,
                    onToggle = onTextShadowToggle
                )

                if (settings.textShadow) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Palette,
                                null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Shadow Color",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (settings.textShadowColor != null) {
                                AppChromeIconButton(
                                    icon = Icons.Rounded.RestartAlt,
                                    contentDescription = "Reset shadow color",
                                    onClick = { onTextShadowColorChange(null) },
                                    size = 32.dp,
                                    iconSize = 18.dp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(settings.textShadowColor ?: 0x66000000), CircleShape)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    .clickable { onPickTextShadowColor() }
                            )
                        }
                    }
                }

                ToggleRow(
                    title = "Focus Reading",
                    desc = "Emphasize the first part of words for assisted reading.",
                    checked = settings.focusText,
                    icon = Icons.Rounded.Bolt,
                    onToggle = onFocusTextToggle
                )

                if (settings.focusText) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Palette, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                Text("Focus Color", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (settings.focusTextColor != null) {
                                    AppChromeIconButton(
                                        icon = Icons.Rounded.RestartAlt,
                                        contentDescription = "Reset focus color",
                                        onClick = { onFocusTextColorChange(null) },
                                        size = 32.dp,
                                        iconSize = 18.dp
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(settings.focusTextColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primary, CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                        .clickable { onPickBionicColor() }
                                )
                            }
                        }

                        SliderSetting(
                            label = "Focus Boldness",
                            value = settings.focusTextBoldness.toFloat(),
                            valueRange = 400f..900f,
                            onValueChange = { onFocusTextBoldnessChange(it.toInt()) },
                            onValueChangeFinished = { onFocusTextBoldnessChangeFinished(it.toInt()) },
                            onReset = { onFocusTextBoldnessChangeFinished(defaultSettings.focusTextBoldness) },
                            icon = Icons.Rounded.FormatBold,
                            valueDisplay = { it.toInt().toString() },
                            showReset = settings.focusTextBoldness != defaultSettings.focusTextBoldness,
                            applyWhileDragging = true
                        )

                        SliderSetting(
                            label = "Emphasis Length",
                            value = settings.focusTextEmphasis,
                            valueRange = 0.2f..0.8f,
                            onValueChange = onFocusTextEmphasisChange,
                            onValueChangeFinished = onFocusTextEmphasisChangeFinished,
                            onReset = { onFocusTextEmphasisChangeFinished(defaultSettings.focusTextEmphasis) },
                            icon = Icons.Rounded.Bolt,
                            valueDisplay = { "${(it * 100).toInt()}%" },
                            showReset = abs(settings.focusTextEmphasis - defaultSettings.focusTextEmphasis) > 0.001f,
                            applyWhileDragging = true
                        )
                    }
                }
            }
        }
    }
}
