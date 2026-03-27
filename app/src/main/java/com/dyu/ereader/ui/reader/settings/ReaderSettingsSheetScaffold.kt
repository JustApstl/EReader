package com.dyu.ereader.ui.reader.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.reader.FontColorTheme
import com.dyu.ereader.data.model.reader.ImageFilter
import com.dyu.ereader.data.model.reader.PageTransitionStyle
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.data.model.reader.ReadingPreset
import com.dyu.ereader.data.model.reader.ReaderFont
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.ReaderTapZoneAction
import com.dyu.ereader.data.model.reader.ReaderTextElement
import com.dyu.ereader.data.model.reader.ReaderTheme
import com.dyu.ereader.data.model.reader.TextAlignment
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.surfaces.SectionSurface
import com.dyu.ereader.ui.reader.overlays.components.ReaderPanelHeader
import com.dyu.ereader.ui.reader.settings.tabs.ExperienceTabContent
import com.dyu.ereader.ui.reader.settings.tabs.LayoutTabContent
import com.dyu.ereader.ui.reader.settings.tabs.TypefaceTabContent

internal data class ReaderSettingsTab(
    val title: String,
    val icon: ImageVector
)

internal fun readerSettingsTabs(): List<ReaderSettingsTab> = listOf(
    ReaderSettingsTab("Layout", Icons.Rounded.ViewAgenda),
    ReaderSettingsTab("Typeface", Icons.Rounded.TextFields),
    ReaderSettingsTab("Experience", Icons.Rounded.Tune)
)

@Composable
internal fun ReaderSettingsSheetHeader(
    hasSettingsChanges: Boolean,
    onResetSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    ReaderPanelHeader(
        title = "Reader Settings",
        icon = Icons.Rounded.Tune,
        modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
        trailing = {
            if (hasSettingsChanges) {
                AppChromeIconButton(
                    icon = Icons.Rounded.RestartAlt,
                    contentDescription = "Reset Reading Settings",
                    onClick = onResetSettings,
                    size = 34.dp,
                    iconSize = 17.dp
                )
            }
            AppChromeIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "Close",
                onClick = onDismiss,
                size = 34.dp,
                iconSize = 17.dp
            )
        }
    )
}

@Composable
internal fun ReaderSettingsTabStrip(
    tabs: List<ReaderSettingsTab>,
    currentPage: Int,
    onSelectTab: (Int) -> Unit
) {
    SectionSurface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        contentPadding = PaddingValues(3.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val selected = currentPage == index
                Surface(
                    onClick = { onSelectTab(index) },
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    shape = RoundedCornerShape(14.dp),
                    border = if (selected) null else BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f)
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ReaderSettingsSheetBody(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    settings: ReaderSettings,
    isDarkTheme: Boolean,
    styleEditable: Boolean,
    fontSizeChanged: Boolean,
    lineSpacingChanged: Boolean,
    marginChanged: Boolean,
    onReadingModeChange: (ReadingMode) -> Unit,
    onApplyPreset: (ReadingPreset) -> Unit,
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
    onImageFilterChange: (ImageFilter) -> Unit,
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
    onOpenElementColorPicker: (ReaderTextElement) -> Unit,
    onFocusModeToggle: (Boolean) -> Unit,
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
    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            userScrollEnabled = false
        ) { page ->
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (page) {
                    0 -> {
                        LayoutTabContent(
                            settings = settings,
                            isDarkTheme = isDarkTheme,
                            styleEditable = styleEditable,
                            onApplyPreset = onApplyPreset,
                            onReadingModeChange = onReadingModeChange,
                            onPageTurn3dToggle = onPageTurn3dToggle,
                            onInvertPageTurnsToggle = onInvertPageTurnsToggle,
                            onPageTransitionStyleChange = onPageTransitionStyleChange,
                            onThemeChange = onThemeChange,
                            onPickCustomColor = onPickCustomColor,
                            onPickBackgroundImage = onPickBackgroundImage,
                            onBackgroundImageBlurChange = onBackgroundImageBlurChange,
                            onBackgroundImageBlurChangeFinished = onBackgroundImageBlurChangeFinished,
                            onBackgroundImageOpacityChange = onBackgroundImageOpacityChange,
                            onBackgroundImageOpacityChangeFinished = onBackgroundImageOpacityChangeFinished,
                            onBackgroundImageZoomChange = onBackgroundImageZoomChange,
                            onBackgroundImageZoomChangeFinished = onBackgroundImageZoomChangeFinished,
                            onImageFilterChange = onImageFilterChange
                        )
                    }

                    1 -> {
                        TypefaceTabContent(
                            settings = settings,
                            styleEditable = styleEditable,
                            fontSizeChanged = fontSizeChanged,
                            lineSpacingChanged = lineSpacingChanged,
                            marginChanged = marginChanged,
                            onUsePublisherStyleToggle = onUsePublisherStyleToggle,
                            onPickCustomFont = onPickCustomFont,
                            onFontChange = onFontChange,
                            onClearCustomFont = onClearCustomFont,
                            onFontColorThemeChange = onFontColorThemeChange,
                            onPickCustomFontColor = onPickCustomFontColor,
                            onFontSizeChange = onFontSizeChange,
                            onFontSizeChangeFinished = onFontSizeChangeFinished,
                            onLineSpacingChange = onLineSpacingChange,
                            onLineSpacingChangeFinished = onLineSpacingChangeFinished,
                            onMarginChange = onMarginChange,
                            onMarginChangeFinished = onMarginChangeFinished,
                            onTextAlignmentChange = onTextAlignmentChange,
                            onElementStyleFontChange = onElementStyleFontChange,
                            onElementStyleColorChange = onElementStyleColorChange,
                            onOpenElementColorPicker = onOpenElementColorPicker
                        )
                    }

                    2 -> {
                        ExperienceTabContent(
                            settings = settings,
                            onFocusModeToggle = onFocusModeToggle,
                            onUsePublisherStyleToggle = onUsePublisherStyleToggle,
                            onHideStatusBarToggle = onHideStatusBarToggle,
                            onUnderlineLinksToggle = onUnderlineLinksToggle,
                            onTextShadowToggle = onTextShadowToggle,
                            onTextShadowColorChange = onTextShadowColorChange,
                            onPickTextShadowColor = onPickTextShadowColor,
                            onAmbientModeToggle = onAmbientModeToggle,
                            onTapZoneActionChange = onTapZoneActionChange,
                            onFocusTextToggle = onFocusTextToggle,
                            onFocusTextBoldnessChange = onFocusTextBoldnessChange,
                            onFocusTextBoldnessChangeFinished = onFocusTextBoldnessChangeFinished,
                            onFocusTextEmphasisChange = onFocusTextEmphasisChange,
                            onFocusTextEmphasisChangeFinished = onFocusTextEmphasisChangeFinished,
                            onFocusTextColorChange = onFocusTextColorChange,
                            onPickBionicColor = onPickBionicColor
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}
