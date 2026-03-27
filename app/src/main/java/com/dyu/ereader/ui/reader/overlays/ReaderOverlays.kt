package com.dyu.ereader.ui.reader.overlays

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dyu.ereader.data.model.reader.FontColorTheme
import com.dyu.ereader.data.model.reader.ImageFilter
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.data.model.reader.PageTransitionStyle
import com.dyu.ereader.data.model.reader.ReadingPreset
import com.dyu.ereader.data.model.reader.ReaderFont
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.ReaderTapZoneAction
import com.dyu.ereader.data.model.reader.ReaderTheme
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.data.model.search.SearchResult
import com.dyu.ereader.data.model.reader.TextAlignment
import com.dyu.ereader.data.local.db.BookmarkEntity
import com.dyu.ereader.data.local.db.HighlightEntity
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.ui.reader.controls.dialogs.ImageZoomDialog
import com.dyu.ereader.ui.reader.overlays.components.ReaderControlBottomSheet
import com.dyu.ereader.ui.reader.settings.ReaderSettingsSheet
import com.dyu.ereader.ui.components.dialogs.ColorPickerDialog
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.dialogs.appDialogContainerColor
import com.dyu.ereader.ui.components.dialogs.appDialogTextFieldColors
import com.dyu.ereader.ui.components.dialogs.appTextFieldShape
import com.dyu.ereader.ui.reader.overlays.dialogs.ExportDialog
import com.dyu.ereader.ui.reader.overlays.sheets.AccessibilitySettings
import com.dyu.ereader.ui.reader.overlays.sheets.AnalyticsDashboard
import com.dyu.ereader.ui.reader.overlays.sheets.SearchDialog
import com.dyu.ereader.ui.reader.overlays.sheets.TableOfContentsSheet
import com.dyu.ereader.ui.reader.state.Chapter

@Composable
internal fun ReaderSettingsOverlay(
    show: Boolean,
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
    onPageTurn3dToggle: (Boolean) -> Unit,
    onInvertPageTurnsToggle: (Boolean) -> Unit,
    onPageTransitionStyleChange: (PageTransitionStyle) -> Unit,
    onTextAlignmentChange: (TextAlignment) -> Unit
) {
    if (!show) return

    ReaderSettingsSheet(
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
        onTextAlignmentChange = onTextAlignmentChange
    )
}

@Composable
internal fun ReaderChapterOverlay(
    show: Boolean,
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    bookmarks: List<BookmarkEntity>,
    highlights: List<HighlightEntity>,
    marginNotes: List<MarginNoteEntity>,
    onLocationSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!show) return

    TableOfContentsSheet(
        chapters = chapters,
        currentChapterIndex = currentChapterIndex,
        bookmarks = bookmarks,
        highlights = highlights,
        marginNotes = marginNotes,
        onLocationSelected = onLocationSelected,
        onDismiss = onDismiss
    )
}

@Composable
internal fun ReaderImageZoomOverlay(
    zoomImageUrl: String?,
    onDismiss: () -> Unit
) {
    zoomImageUrl?.let { url ->
        ImageZoomDialog(
            url = url,
            onDismiss = onDismiss
        )
    }
}

@Composable
internal fun ReaderSearchOverlay(
    show: Boolean,
    onDismiss: () -> Unit,
    onResultSelected: (SearchResult) -> Unit,
    onSearch: (String) -> Unit,
    results: List<SearchResult>,
    isSearching: Boolean
) {
    if (!show) return

    SearchDialog(
        onDismiss = onDismiss,
        onResultSelected = onResultSelected,
        onSearch = onSearch,
        results = results,
        isSearching = isSearching
    )
}

@Composable
internal fun ReaderBottomSheetOverlays(
    showAccessibilitySettings: Boolean,
    onDismissAccessibility: () -> Unit,
    showAnalytics: Boolean,
    onDismissAnalytics: () -> Unit
) {
    if (showAccessibilitySettings) {
        ReaderControlBottomSheet(
            onDismissRequest = onDismissAccessibility
        ) {
            AccessibilitySettings(
                modifier = Modifier
            )
        }
    }

    if (showAnalytics) {
        ReaderControlBottomSheet(
            onDismissRequest = onDismissAnalytics
        ) {
            AnalyticsDashboard(
                modifier = Modifier
            )
        }
    }
}

@Composable
internal fun ReaderExportOverlay(
    show: Boolean,
    onDismiss: () -> Unit
) {
    if (!show) return

    ReaderControlBottomSheet(
        onDismissRequest = onDismiss
    ) {
        ExportDialog(
            onDismiss = onDismiss
        )
    }
}

@Composable
internal fun ReaderNoteDialog(
    show: Boolean,
    noteDraft: String,
    onNoteDraftChange: (String) -> Unit,
    canSave: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    if (!show) return

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = UiTokens.SettingsCardShape,
        containerColor = appDialogContainerColor(),
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text("Add Note") },
        text = {
            OutlinedTextField(
                value = noteDraft,
                onValueChange = onNoteDraftChange,
                placeholder = { Text("Write your note...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 6,
                shape = appTextFieldShape(multiline = true),
                colors = appDialogTextFieldColors()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = canSave
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun ReaderHighlightColorPicker(
    show: Boolean,
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
    initialColor: Int,
    onColorPreview: (Int) -> Unit,
    onCancel: () -> Unit
) {
    if (!show) return

    ColorPickerDialog(
        onDismiss = onDismiss,
        onColorSelected = onColorSelected,
        initialColor = initialColor,
        onColorPreview = onColorPreview,
        onCancel = onCancel
    )
}
