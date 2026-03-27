package com.dyu.ereader.ui.home.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.ReaderTheme
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.data.model.reader.PageTransitionStyle
import com.dyu.ereader.data.model.reader.ReaderFont
import com.dyu.ereader.data.model.reader.TextAlignment
import com.dyu.ereader.data.model.reader.getBackgroundColor
import com.dyu.ereader.data.model.reader.getColor
import com.dyu.ereader.ui.components.dialogs.ColorPickerDialog
import com.dyu.ereader.ui.components.inputs.appSegmentedButtonColors
import com.dyu.ereader.ui.home.state.HomeUiState
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectHorizontalDragGestures

@Composable
internal fun ReaderDefaultsSection(
    uiState: HomeUiState,
    liquidGlassEnabled: Boolean,
    onReaderSettingsChanged: (ReaderSettings) -> Unit
) {
    val context = LocalContext.current
    val settings = uiState.readerSettings
    val defaults = ReaderSettings()
    var showCustomBackgroundPicker by remember { mutableStateOf(false) }
    fun update(transform: (ReaderSettings) -> ReaderSettings) {
        onReaderSettingsChanged(transform(settings))
    }
    val customFontPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            update {
                it.copy(
                    customFontUri = uri.toString(),
                    font = ReaderFont.CUSTOM,
                    usePublisherStyle = false
                )
            }
        }
    }
    if (showCustomBackgroundPicker) {
        ColorPickerDialog(
            onDismiss = { showCustomBackgroundPicker = false },
            onColorSelected = { color ->
                update {
                    it.copy(
                        customBackgroundColor = color,
                        readerTheme = ReaderTheme.CUSTOM,
                        usePublisherStyle = false
                    )
                }
                showCustomBackgroundPicker = false
            },
            initialColor = settings.customBackgroundColor,
            onCancel = { showCustomBackgroundPicker = false }
        )
    }

    SettingsCard(
        title = "Reader Settings",
        icon = Icons.AutoMirrored.Rounded.MenuBook,
        liquidGlassEnabled = liquidGlassEnabled
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Text(
                "Choose the defaults newly opened books should use in the reader.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ReaderSettingsSectionCard(
                title = "Layout",
                description = "Control reading flow, background, and page transitions."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Reading Mode",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        ChoiceButtons(
                            options = listOf(
                                ChoiceOption("Scroll", settings.readingMode == ReadingMode.SCROLL) {
                                    update { it.copy(readingMode = ReadingMode.SCROLL) }
                                },
                                ChoiceOption("Page", settings.readingMode == ReadingMode.PAGE) {
                                    update { it.copy(readingMode = ReadingMode.PAGE) }
                                }
                            ),
                            columns = 2
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Background",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        ReaderThemeButtons(
                            currentTheme = settings.readerTheme,
                            customBackgroundColor = settings.customBackgroundColor,
                            onThemeSelected = { theme -> update { it.copy(readerTheme = theme) } }
                        )
                        OutlinedButton(
                            onClick = { showCustomBackgroundPicker = true },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ColorLens,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                if (settings.customBackgroundColor == null) {
                                    "Set Custom Background"
                                } else {
                                    "Change Custom Background"
                                }
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "Page Transition",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.ExtraBold
                        )
                        ChoiceButtons(
                            options = listOf(
                                ChoiceOption("Default", settings.pageTransitionStyle == PageTransitionStyle.DEFAULT) {
                                    update { it.copy(pageTransitionStyle = PageTransitionStyle.DEFAULT) }
                                },
                                ChoiceOption("Tilt", settings.pageTransitionStyle == PageTransitionStyle.TILT) {
                                    update { it.copy(pageTransitionStyle = PageTransitionStyle.TILT) }
                                },
                                ChoiceOption("Card", settings.pageTransitionStyle == PageTransitionStyle.CARD) {
                                    update { it.copy(pageTransitionStyle = PageTransitionStyle.CARD) }
                                },
                                ChoiceOption("Flip", settings.pageTransitionStyle == PageTransitionStyle.FLIP) {
                                    update { it.copy(pageTransitionStyle = PageTransitionStyle.FLIP) }
                                }
                            ),
                            columns = 2
                        )
                    }

                    SettingSwitch(
                        title = "3D Page Turn",
                        desc = "Enable depth effect when paging",
                        checked = settings.pageTurn3d,
                        onCheckedChange = { checked -> update { it.copy(pageTurn3d = checked) } }
                    )
                    SettingSwitch(
                        title = "Invert Page Turns",
                        desc = "Reverse swipe and page direction",
                        checked = settings.invertPageTurns,
                        onCheckedChange = { checked -> update { it.copy(invertPageTurns = checked) } }
                    )
                }
            }

            ReaderSettingsSectionCard(
                title = "Typeface",
                description = "Choose a reading font, tune typography, and preview the page."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderSettingSubhead(
                            title = "Reader Font",
                            description = "Use the system set, a built-in family, or your own custom typeface."
                        )
                        ReaderFontSelector(
                            currentFont = settings.font,
                            customFontUri = settings.customFontUri,
                            onFontSelected = { font ->
                                update { it.copy(font = font, usePublisherStyle = false) }
                            },
                            onPickCustomFont = {
                                customFontPicker.launch(
                                    arrayOf(
                                        "font/ttf",
                                        "font/otf",
                                        "application/x-font-ttf",
                                        "application/x-font-opentype",
                                        "application/octet-stream"
                                    )
                                )
                            },
                            onClearCustomFont = {
                                update {
                                    it.copy(
                                        customFontUri = null,
                                        font = if (it.font == ReaderFont.CUSTOM) ReaderFont.SYSTEM else it.font,
                                        usePublisherStyle = false
                                    )
                                }
                            }
                        )
                    }

                    ReaderPreviewCard(settings = settings)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReaderSettingSubhead(
                            title = "Typography",
                            description = "Adjust text size, spacing, alignment, and page margins."
                        )
                        SettingSlider(
                            title = "Font Size",
                            value = settings.fontSizeSp,
                            valueRange = 12f..28f,
                            onValueChange = {},
                            onValueChangeFinished = { update { current -> current.copy(fontSizeSp = it, usePublisherStyle = false) } },
                            valueLabel = { "${it.roundToInt()}sp" },
                            steps = 15,
                            showReset = settings.fontSizeSp != defaults.fontSizeSp,
                            onReset = { update { it.copy(fontSizeSp = defaults.fontSizeSp) } }
                        )
                        SettingSlider(
                            title = "Line Spacing",
                            value = settings.lineSpacing,
                            valueRange = 1.1f..2.2f,
                            onValueChange = {},
                            onValueChangeFinished = { update { current -> current.copy(lineSpacing = it, usePublisherStyle = false) } },
                            valueLabel = { String.format("%.2f", it) },
                            steps = 10,
                            showReset = settings.lineSpacing != defaults.lineSpacing,
                            onReset = { update { it.copy(lineSpacing = defaults.lineSpacing) } }
                        )
                        SettingSlider(
                            title = "Horizontal Margin",
                            value = settings.horizontalMarginDp,
                            valueRange = 8f..56f,
                            onValueChange = {},
                            onValueChangeFinished = { update { current -> current.copy(horizontalMarginDp = it, usePublisherStyle = false) } },
                            valueLabel = { "${it.roundToInt()}dp" },
                            steps = 11,
                            showReset = settings.horizontalMarginDp != defaults.horizontalMarginDp,
                            onReset = { update { it.copy(horizontalMarginDp = defaults.horizontalMarginDp) } }
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val options = listOf(
                                TextAlignment.DEFAULT,
                                TextAlignment.LEFT,
                                TextAlignment.RIGHT,
                                TextAlignment.JUSTIFY
                            )
                            options.forEachIndexed { index, alignment ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                                    onClick = { update { it.copy(textAlignment = alignment, usePublisherStyle = false) } },
                                    selected = settings.textAlignment == alignment,
                                    colors = appSegmentedButtonColors(),
                                    label = {
                                        Text(
                                            when (alignment) {
                                                TextAlignment.DEFAULT -> "Original"
                                                TextAlignment.LEFT -> "Left"
                                                TextAlignment.RIGHT -> "Right"
                                                TextAlignment.JUSTIFY -> "Justify"
                                                else -> alignment.label
                                            }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    SettingSwitch(
                        title = "Use Publisher Style",
                        desc = "Respect embedded EPUB typography by default",
                        checked = settings.usePublisherStyle,
                        onCheckedChange = { checked -> update { it.copy(usePublisherStyle = checked) } }
                    )
                }
            }

            ReaderSettingsSectionCard(
                title = "Experience",
                description = "Tune focus, immersion, and readability while reading."
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SettingSwitch(
                        title = "Focus Mode",
                        desc = "Reduce distractions while reading",
                        checked = settings.focusMode,
                        onCheckedChange = { checked -> update { it.copy(focusMode = checked) } }
                    )
                    SettingSwitch(
                        title = "Hide Status Bar While Reading",
                        desc = "Use immersive mode in reader screens",
                        checked = settings.hideStatusBar,
                        onCheckedChange = { checked -> update { it.copy(hideStatusBar = checked) } }
                    )
                    SettingSwitch(
                        title = "Ambient Mode",
                        desc = "Dim surroundings for focused night reading",
                        checked = settings.ambientMode,
                        onCheckedChange = { checked -> update { it.copy(ambientMode = checked) } }
                    )
                    SettingSwitch(
                        title = "Underline Links",
                        desc = "Show link underline in reading content",
                        checked = settings.underlineLinks,
                        onCheckedChange = { checked -> update { it.copy(underlineLinks = checked) } }
                    )
                    SettingSwitch(
                        title = "Text Shadow",
                        desc = "Add subtle text shadow for readability",
                        checked = settings.textShadow,
                        onCheckedChange = { checked -> update { it.copy(textShadow = checked) } }
                    )
                    SettingSwitch(
                        title = "Focus Text",
                        desc = "Highlight text focus and bionic-style emphasis",
                        checked = settings.focusText,
                        onCheckedChange = { checked -> update { it.copy(focusText = checked) } }
                    )
                }
            }
        }
    }
}
