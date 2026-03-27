package com.dyu.ereader.ui.reader.screens

import androidx.compose.runtime.Composable
import com.dyu.ereader.data.local.db.BookmarkEntity
import com.dyu.ereader.data.local.db.HighlightEntity
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.data.model.reader.FontColorTheme
import com.dyu.ereader.data.model.reader.ImageFilter
import com.dyu.ereader.data.model.reader.ReaderFont
import com.dyu.ereader.data.model.reader.ReadingPreset
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.ReaderTapZoneAction
import com.dyu.ereader.data.model.reader.ReaderTheme
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.data.model.reader.PageTransitionStyle
import com.dyu.ereader.data.model.reader.TextAlignment
import com.dyu.ereader.data.model.search.SearchResult
import com.dyu.ereader.ui.reader.overlays.ReaderBottomSheetOverlays
import com.dyu.ereader.ui.reader.overlays.ReaderChapterOverlay
import com.dyu.ereader.ui.reader.overlays.ReaderExportOverlay
import com.dyu.ereader.ui.reader.overlays.ReaderHighlightColorPicker
import com.dyu.ereader.ui.reader.overlays.ReaderImageZoomOverlay
import com.dyu.ereader.ui.reader.overlays.ReaderNoteDialog
import com.dyu.ereader.ui.reader.overlays.ReaderSearchOverlay
import com.dyu.ereader.ui.reader.overlays.ReaderSettingsOverlay
import com.dyu.ereader.ui.reader.state.Chapter

@Composable
internal fun ReaderScreenOverlays(
    showSettings: Boolean,
    settings: ReaderSettings,
    isDarkTheme: Boolean,
    onDismissSettings: () -> Unit,
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
    onTextAlignmentChange: (TextAlignment) -> Unit,
    showChapterSheet: Boolean,
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    bookmarks: List<BookmarkEntity>,
    highlights: List<HighlightEntity>,
    marginNotes: List<MarginNoteEntity>,
    onLocationSelected: (String) -> Unit,
    onDismissChapterSheet: () -> Unit,
    zoomImageUrl: String?,
    onDismissZoomImage: () -> Unit,
    showSearchDialog: Boolean,
    onDismissSearch: () -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<SearchResult>,
    isSearching: Boolean,
    onSearchResultSelected: (SearchResult) -> Unit,
    showAccessibilitySettings: Boolean,
    onDismissAccessibility: () -> Unit,
    showAnalytics: Boolean,
    onDismissAnalytics: () -> Unit,
    showExportDialog: Boolean,
    onDismissExport: () -> Unit,
    showAddNoteDialog: Boolean,
    noteDraft: String,
    onNoteDraftChange: (String) -> Unit,
    onNoteDismiss: () -> Unit,
    onNoteSave: () -> Unit,
    onNoteCancel: () -> Unit,
    showHighlightColorPicker: Boolean,
    onDismissHighlightColorPicker: () -> Unit,
    onHighlightColorSelected: (Int) -> Unit,
    initialHighlightColor: Int,
    onHighlightColorPreview: (Int) -> Unit,
    onHighlightColorCancel: () -> Unit
) {
    ReaderSettingsOverlay(
        show = showSettings,
        settings = settings,
        isDarkTheme = isDarkTheme,
        onDismiss = onDismissSettings,
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

    ReaderChapterOverlay(
        show = showChapterSheet,
        chapters = chapters,
        currentChapterIndex = currentChapterIndex,
        bookmarks = bookmarks,
        highlights = highlights,
        marginNotes = marginNotes,
        onLocationSelected = onLocationSelected,
        onDismiss = onDismissChapterSheet
    )

    ReaderImageZoomOverlay(
        zoomImageUrl = zoomImageUrl,
        onDismiss = onDismissZoomImage
    )

    ReaderSearchOverlay(
        show = showSearchDialog,
        onDismiss = onDismissSearch,
        onResultSelected = onSearchResultSelected,
        onSearch = onSearch,
        results = searchResults,
        isSearching = isSearching
    )

    ReaderBottomSheetOverlays(
        showAccessibilitySettings = showAccessibilitySettings,
        onDismissAccessibility = onDismissAccessibility,
        showAnalytics = showAnalytics,
        onDismissAnalytics = onDismissAnalytics
    )

    ReaderExportOverlay(
        show = showExportDialog,
        onDismiss = onDismissExport
    )

    ReaderNoteDialog(
        show = showAddNoteDialog,
        noteDraft = noteDraft,
        onNoteDraftChange = onNoteDraftChange,
        canSave = noteDraft.isNotBlank(),
        onDismiss = onNoteDismiss,
        onSave = onNoteSave,
        onCancel = onNoteCancel
    )

    ReaderHighlightColorPicker(
        show = showHighlightColorPicker,
        onDismiss = onDismissHighlightColorPicker,
        onColorSelected = onHighlightColorSelected,
        initialColor = initialHighlightColor,
        onColorPreview = onHighlightColorPreview,
        onCancel = onHighlightColorCancel
    )
}
