package com.dyu.ereader.ui.reader.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.app.NavigationBarStyle
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
import com.dyu.ereader.ui.components.dialogs.ColorPickerDialog
import kotlinx.coroutines.launch

private fun defaultElementPickerColor(target: ReaderTextElement, settings: ReaderSettings): Int {
    return when (target) {
        ReaderTextElement.EXTERNAL_LINK,
        ReaderTextElement.INTERNAL_LINK -> settings.elementStyles.styleFor(target).color ?: 0xFF2F6BFF.toInt()
        else -> settings.elementStyles.styleFor(target).color
            ?: settings.customFontColor
            ?: 0xFF000000.toInt()
    }
}

@Composable
internal fun ReaderSettingsPanelContent(
    settings: ReaderSettings,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onReadingModeChange: (ReadingMode) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onFontColorThemeChange: (FontColorTheme) -> Unit,
    onAutoFontColorToggle: (Boolean) -> Unit,
    onFocusTextToggle: (Boolean) -> Unit,
    onFocusTextBoldnessChange: (Int) -> Unit,
    onFocusTextBoldnessChangeFinished: (Int) -> Unit,
    onFocusTextEmphasisChange: (Float) -> Unit,
    onFocusTextEmphasisChangeFinished: (Float) -> Unit,
    onFocusTextColorChange: (Int?) -> Unit,
    onFocusTextColorPreview: (Int?) -> Unit,
    onFocusModeToggle: (Boolean) -> Unit,
    onHideStatusBarToggle: (Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontSizeChangeFinished: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onLineSpacingChangeFinished: (Float) -> Unit,
    onMarginChange: (Float) -> Unit,
    onMarginChangeFinished: (Float) -> Unit,
    onFontChange: (ReaderFont) -> Unit,
    onResetSettings: () -> Unit,
    onRestoreSettings: (ReaderSettings) -> Unit,
    onApplyPreset: (ReadingPreset) -> Unit,
    onCustomColorSelected: (Int) -> Unit,
    onCustomColorPreview: (Int) -> Unit,
    onCustomFontColorSelected: (Int) -> Unit,
    onCustomFontColorPreview: (Int) -> Unit,
    onPickCustomFont: () -> Unit,
    onClearCustomFont: () -> Unit,
    onPickBackgroundImage: () -> Unit,
    onBackgroundImageBlurChange: (Float) -> Unit,
    onBackgroundImageBlurChangeFinished: (Float) -> Unit,
    onBackgroundImageOpacityChange: (Float) -> Unit,
    onBackgroundImageOpacityChangeFinished: (Float) -> Unit,
    onBackgroundImageZoomChange: (Float) -> Unit,
    onBackgroundImageZoomChangeFinished: (Float) -> Unit,
    onImageFilterChange: (ImageFilter) -> Unit,
    onUsePublisherStyleToggle: (Boolean) -> Unit,
    onUnderlineLinksToggle: (Boolean) -> Unit,
    onTextShadowToggle: (Boolean) -> Unit,
    onTextShadowColorChange: (Int?) -> Unit,
    onTextShadowColorPreview: (Int?) -> Unit,
    onAmbientModeToggle: (Boolean) -> Unit,
    onTapZoneActionChange: (String, ReaderTapZoneAction) -> Unit,
    onNavigationBarStyleChange: (NavigationBarStyle) -> Unit,
    onPageTurn3dToggle: (Boolean) -> Unit = {},
    onInvertPageTurnsToggle: (Boolean) -> Unit = {},
    onPageTransitionStyleChange: (PageTransitionStyle) -> Unit = {},
    onTextAlignmentChange: (TextAlignment) -> Unit = {},
    onElementStyleFontChange: (ReaderTextElement, ReaderFont) -> Unit = { _, _ -> },
    onElementStyleColorChange: (ReaderTextElement, Int?) -> Unit = { _, _ -> },
    onElementStyleColorPreview: (ReaderTextElement, Int?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var showColorPicker by remember { mutableStateOf(false) }
    var showFontColorPicker by remember { mutableStateOf(false) }
    var showBionicColorPicker by remember { mutableStateOf(false) }
    var showTextShadowColorPicker by remember { mutableStateOf(false) }
    var activeElementColorTarget by remember { mutableStateOf<ReaderTextElement?>(null) }
    var settingsSnapshot by remember { mutableStateOf<ReaderSettings?>(null) }
    val styleEditable = !settings.usePublisherStyle

    fun captureSnapshot() {
        settingsSnapshot = settings
    }

    fun restoreSnapshot() {
        settingsSnapshot?.let(onRestoreSettings)
        settingsSnapshot = null
    }

    if (showColorPicker) {
        ColorPickerDialog(
            onDismiss = { showColorPicker = false },
            onColorSelected = {
                onCustomColorSelected(it)
                settingsSnapshot = null
            },
            initialColor = settingsSnapshot?.customBackgroundColor ?: settings.customBackgroundColor,
            onColorPreview = onCustomColorPreview,
            onCancel = { restoreSnapshot() }
        )
    }

    if (showFontColorPicker) {
        ColorPickerDialog(
            onDismiss = { showFontColorPicker = false },
            onColorSelected = {
                onCustomFontColorSelected(it)
                settingsSnapshot = null
            },
            initialColor = settingsSnapshot?.customFontColor ?: settings.customFontColor ?: 0xFF000000.toInt(),
            onColorPreview = onCustomFontColorPreview,
            onCancel = { restoreSnapshot() }
        )
    }

    if (showBionicColorPicker) {
        ColorPickerDialog(
            onDismiss = { showBionicColorPicker = false },
            onColorSelected = {
                onFocusTextColorChange(it)
                settingsSnapshot = null
            },
            initialColor = settingsSnapshot?.focusTextColor ?: settings.focusTextColor ?: 0xFF6650a4.toInt(),
            onColorPreview = onFocusTextColorPreview,
            onCancel = { restoreSnapshot() }
        )
    }

    if (showTextShadowColorPicker) {
        ColorPickerDialog(
            onDismiss = { showTextShadowColorPicker = false },
            onColorSelected = {
                onTextShadowColorChange(it)
                settingsSnapshot = null
            },
            initialColor = settingsSnapshot?.textShadowColor ?: settings.textShadowColor ?: 0x66000000,
            onColorPreview = onTextShadowColorPreview,
            onCancel = { restoreSnapshot() }
        )
    }

    activeElementColorTarget?.let { target ->
        ColorPickerDialog(
            onDismiss = { activeElementColorTarget = null },
            onColorSelected = {
                onElementStyleColorChange(target, it)
                settingsSnapshot = null
                activeElementColorTarget = null
            },
            initialColor = settingsSnapshot?.elementStyles?.styleFor(target)?.color
                ?: defaultElementPickerColor(target, settings),
            onColorPreview = { onElementStyleColorPreview(target, it) },
            onCancel = {
                activeElementColorTarget = null
                restoreSnapshot()
            }
        )
    }

    val pagerState = rememberPagerState(pageCount = { 3 })
    val configuration = LocalConfiguration.current
    val maxPanelHeight = (configuration.screenHeightDp * 0.78f).dp
    val defaultSettings = remember { ReaderSettings() }
    val hasSettingsChanges = settings != defaultSettings
    val fontSizeChanged = settings.fontSizeSp != defaultSettings.fontSizeSp
    val lineSpacingChanged = settings.lineSpacing != defaultSettings.lineSpacing
    val marginChanged = settings.horizontalMarginDp != defaultSettings.horizontalMarginDp
    val tabs = remember { readerSettingsTabs() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = maxPanelHeight),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ReaderSettingsSheetHeader(
            hasSettingsChanges = hasSettingsChanges,
            onResetSettings = onResetSettings,
            onDismiss = onDismiss
        )

        ReaderSettingsTabStrip(
            tabs = tabs,
            currentPage = pagerState.currentPage,
            onSelectTab = { index ->
                scope.launch { pagerState.animateScrollToPage(index) }
            }
        )

        ReaderSettingsSheetBody(
            modifier = Modifier.weight(1f, fill = true),
            pagerState = pagerState,
            settings = settings,
            isDarkTheme = isDarkTheme,
            styleEditable = styleEditable,
            fontSizeChanged = fontSizeChanged,
            lineSpacingChanged = lineSpacingChanged,
            marginChanged = marginChanged,
            onReadingModeChange = onReadingModeChange,
            onApplyPreset = onApplyPreset,
            onPageTurn3dToggle = onPageTurn3dToggle,
            onInvertPageTurnsToggle = onInvertPageTurnsToggle,
            onPageTransitionStyleChange = onPageTransitionStyleChange,
            onThemeChange = onThemeChange,
            onPickCustomColor = {
                captureSnapshot()
                showColorPicker = true
            },
            onPickBackgroundImage = onPickBackgroundImage,
            onBackgroundImageBlurChange = onBackgroundImageBlurChange,
            onBackgroundImageBlurChangeFinished = onBackgroundImageBlurChangeFinished,
            onBackgroundImageOpacityChange = onBackgroundImageOpacityChange,
            onBackgroundImageOpacityChangeFinished = onBackgroundImageOpacityChangeFinished,
            onBackgroundImageZoomChange = onBackgroundImageZoomChange,
            onBackgroundImageZoomChangeFinished = onBackgroundImageZoomChangeFinished,
            onImageFilterChange = onImageFilterChange,
            onUsePublisherStyleToggle = onUsePublisherStyleToggle,
            onPickCustomFont = onPickCustomFont,
            onFontChange = onFontChange,
            onClearCustomFont = onClearCustomFont,
            onFontColorThemeChange = onFontColorThemeChange,
            onPickCustomFontColor = {
                captureSnapshot()
                showFontColorPicker = true
            },
            onFontSizeChange = onFontSizeChange,
            onFontSizeChangeFinished = onFontSizeChangeFinished,
            onLineSpacingChange = onLineSpacingChange,
            onLineSpacingChangeFinished = onLineSpacingChangeFinished,
            onMarginChange = onMarginChange,
            onMarginChangeFinished = onMarginChangeFinished,
            onTextAlignmentChange = onTextAlignmentChange,
            onElementStyleFontChange = onElementStyleFontChange,
            onElementStyleColorChange = onElementStyleColorChange,
            onOpenElementColorPicker = { target ->
                captureSnapshot()
                activeElementColorTarget = target
            },
            onFocusModeToggle = onFocusModeToggle,
            onHideStatusBarToggle = onHideStatusBarToggle,
            onUnderlineLinksToggle = onUnderlineLinksToggle,
            onTextShadowToggle = onTextShadowToggle,
            onTextShadowColorChange = onTextShadowColorChange,
            onPickTextShadowColor = {
                captureSnapshot()
                showTextShadowColorPicker = true
            },
            onAmbientModeToggle = onAmbientModeToggle,
            onTapZoneActionChange = onTapZoneActionChange,
            onFocusTextToggle = onFocusTextToggle,
            onFocusTextBoldnessChange = onFocusTextBoldnessChange,
            onFocusTextBoldnessChangeFinished = onFocusTextBoldnessChangeFinished,
            onFocusTextEmphasisChange = onFocusTextEmphasisChange,
            onFocusTextEmphasisChangeFinished = onFocusTextEmphasisChangeFinished,
            onFocusTextColorChange = onFocusTextColorChange,
            onPickBionicColor = {
                captureSnapshot()
                showBionicColorPicker = true
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsSheet(
    settings: ReaderSettings,
    isDarkTheme: Boolean,
    onDismiss: () -> Unit,
    onReadingModeChange: (ReadingMode) -> Unit,
    onThemeChange: (ReaderTheme) -> Unit,
    onFontColorThemeChange: (FontColorTheme) -> Unit,
    onAutoFontColorToggle: (Boolean) -> Unit,
    onFocusTextToggle: (Boolean) -> Unit,
    onFocusTextBoldnessChange: (Int) -> Unit,
    onFocusTextBoldnessChangeFinished: (Int) -> Unit,
    onFocusTextEmphasisChange: (Float) -> Unit,
    onFocusTextEmphasisChangeFinished: (Float) -> Unit,
    onFocusTextColorChange: (Int?) -> Unit,
    onFocusTextColorPreview: (Int?) -> Unit,
    onFocusModeToggle: (Boolean) -> Unit,
    onHideStatusBarToggle: (Boolean) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onFontSizeChangeFinished: (Float) -> Unit,
    onLineSpacingChange: (Float) -> Unit,
    onLineSpacingChangeFinished: (Float) -> Unit,
    onMarginChange: (Float) -> Unit,
    onMarginChangeFinished: (Float) -> Unit,
    onFontChange: (ReaderFont) -> Unit,
    onResetSettings: () -> Unit,
    onRestoreSettings: (ReaderSettings) -> Unit,
    onApplyPreset: (ReadingPreset) -> Unit,
    onCustomColorSelected: (Int) -> Unit,
    onCustomColorPreview: (Int) -> Unit,
    onCustomFontColorSelected: (Int) -> Unit,
    onCustomFontColorPreview: (Int) -> Unit,
    onPickCustomFont: () -> Unit,
    onClearCustomFont: () -> Unit,
    onPickBackgroundImage: () -> Unit,
    onBackgroundImageBlurChange: (Float) -> Unit,
    onBackgroundImageBlurChangeFinished: (Float) -> Unit,
    onBackgroundImageOpacityChange: (Float) -> Unit,
    onBackgroundImageOpacityChangeFinished: (Float) -> Unit,
    onBackgroundImageZoomChange: (Float) -> Unit,
    onBackgroundImageZoomChangeFinished: (Float) -> Unit,
    onImageFilterChange: (ImageFilter) -> Unit,
    onUsePublisherStyleToggle: (Boolean) -> Unit,
    onUnderlineLinksToggle: (Boolean) -> Unit,
    onTextShadowToggle: (Boolean) -> Unit,
    onTextShadowColorChange: (Int?) -> Unit,
    onTextShadowColorPreview: (Int?) -> Unit,
    onAmbientModeToggle: (Boolean) -> Unit,
    onTapZoneActionChange: (String, ReaderTapZoneAction) -> Unit,
    onNavigationBarStyleChange: (NavigationBarStyle) -> Unit,
    onPageTurn3dToggle: (Boolean) -> Unit = {},
    onInvertPageTurnsToggle: (Boolean) -> Unit = {},
    onPageTransitionStyleChange: (PageTransitionStyle) -> Unit = {},
    onTextAlignmentChange: (TextAlignment) -> Unit = {},
    onElementStyleFontChange: (ReaderTextElement, ReaderFont) -> Unit = { _, _ -> },
    onElementStyleColorChange: (ReaderTextElement, Int?) -> Unit = { _, _ -> },
    onElementStyleColorPreview: (ReaderTextElement, Int?) -> Unit = { _, _ -> }
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { true }
    )

    LaunchedEffect(Unit) {
        sheetState.expand()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = { WindowInsets(0.dp) },
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.22f),
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)
    ) {
        ReaderSettingsPanelContent(
            settings = settings,
            isDarkTheme = isDarkTheme,
            onDismiss = onDismiss,
            onReadingModeChange = onReadingModeChange,
            onThemeChange = onThemeChange,
            onFontColorThemeChange = onFontColorThemeChange,
            onAutoFontColorToggle = onAutoFontColorToggle,
            onFocusTextToggle = onFocusTextToggle,
            onFocusTextBoldnessChange = onFocusTextBoldnessChange,
            onFocusTextBoldnessChangeFinished = onFocusTextBoldnessChangeFinished,
            onFocusTextEmphasisChange = onFocusTextEmphasisChange,
            onFocusTextEmphasisChangeFinished = onFocusTextEmphasisChangeFinished,
            onFocusTextColorChange = onFocusTextColorChange,
            onFocusTextColorPreview = onFocusTextColorPreview,
            onFocusModeToggle = onFocusModeToggle,
            onHideStatusBarToggle = onHideStatusBarToggle,
            onFontSizeChange = onFontSizeChange,
            onFontSizeChangeFinished = onFontSizeChangeFinished,
            onLineSpacingChange = onLineSpacingChange,
            onLineSpacingChangeFinished = onLineSpacingChangeFinished,
            onMarginChange = onMarginChange,
            onMarginChangeFinished = onMarginChangeFinished,
            onFontChange = onFontChange,
            onResetSettings = onResetSettings,
            onRestoreSettings = onRestoreSettings,
            onApplyPreset = onApplyPreset,
            onCustomColorSelected = onCustomColorSelected,
            onCustomColorPreview = onCustomColorPreview,
            onCustomFontColorSelected = onCustomFontColorSelected,
            onCustomFontColorPreview = onCustomFontColorPreview,
            onPickCustomFont = onPickCustomFont,
            onClearCustomFont = onClearCustomFont,
            onPickBackgroundImage = onPickBackgroundImage,
            onBackgroundImageBlurChange = onBackgroundImageBlurChange,
            onBackgroundImageBlurChangeFinished = onBackgroundImageBlurChangeFinished,
            onBackgroundImageOpacityChange = onBackgroundImageOpacityChange,
            onBackgroundImageOpacityChangeFinished = onBackgroundImageOpacityChangeFinished,
            onBackgroundImageZoomChange = onBackgroundImageZoomChange,
            onBackgroundImageZoomChangeFinished = onBackgroundImageZoomChangeFinished,
            onImageFilterChange = onImageFilterChange,
            onUsePublisherStyleToggle = onUsePublisherStyleToggle,
            onUnderlineLinksToggle = onUnderlineLinksToggle,
            onTextShadowToggle = onTextShadowToggle,
            onTextShadowColorChange = onTextShadowColorChange,
            onTextShadowColorPreview = onTextShadowColorPreview,
            onAmbientModeToggle = onAmbientModeToggle,
            onTapZoneActionChange = onTapZoneActionChange,
            onNavigationBarStyleChange = onNavigationBarStyleChange,
            onPageTurn3dToggle = onPageTurn3dToggle,
            onInvertPageTurnsToggle = onInvertPageTurnsToggle,
            onPageTransitionStyleChange = onPageTransitionStyleChange,
            onTextAlignmentChange = onTextAlignmentChange,
            onElementStyleFontChange = onElementStyleFontChange,
            onElementStyleColorChange = onElementStyleColorChange,
            onElementStyleColorPreview = onElementStyleColorPreview,
            modifier = Modifier
                .fillMaxWidth()
                .height((LocalConfiguration.current.screenHeightDp * 0.78f).dp)
                .navigationBarsPadding()
                .padding(bottom = 8.dp)
        )
    }
}
