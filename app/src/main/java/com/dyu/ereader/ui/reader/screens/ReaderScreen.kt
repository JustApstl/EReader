package com.dyu.ereader.ui.reader.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.local.db.BookmarkEntity
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.data.model.reader.FontColorTheme
import com.dyu.ereader.data.model.reader.ImageFilter
import com.dyu.ereader.data.model.reader.PageTransitionStyle
import com.dyu.ereader.data.model.reader.ReadingPreset
import com.dyu.ereader.data.model.reader.ReaderFont
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.ReaderTapZoneAction
import com.dyu.ereader.data.model.reader.ReaderTextElement
import com.dyu.ereader.data.model.reader.ReaderTheme
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.data.model.reader.TextAlignment
import com.dyu.ereader.data.model.reader.getBackgroundColor
import com.dyu.ereader.data.model.search.SearchResult
import com.dyu.ereader.data.model.tts.ListenSleepTimerMode
import com.dyu.ereader.ui.home.state.HomeDisplayPreferences
import com.dyu.ereader.ui.reader.chrome.ReaderBottomChrome
import com.dyu.ereader.ui.reader.chrome.ReaderTopChrome
import com.dyu.ereader.ui.reader.overlays.components.ListenMiniPlayer
import com.dyu.ereader.ui.reader.overlays.components.ReaderOnboardingOverlay
import com.dyu.ereader.ui.reader.overlays.components.ListenDockedPanel
import com.dyu.ereader.ui.reader.overlays.sheets.AccessibilitySettings
import com.dyu.ereader.ui.reader.overlays.sheets.AnalyticsDashboard
import com.dyu.ereader.ui.reader.overlays.sheets.SearchPanelContent
import com.dyu.ereader.ui.reader.overlays.sheets.TableOfContentsPanelContent
import com.dyu.ereader.ui.reader.overlays.dialogs.ExportDialog
import com.dyu.ereader.ui.reader.settings.ReaderSettingsPanelContent
import com.dyu.ereader.ui.reader.state.PageTurnDirection
import com.dyu.ereader.ui.reader.state.Chapter
import com.dyu.ereader.ui.reader.state.ReaderUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    bookUri: String,
    bookType: BookType,
    uiState: ReaderUiState,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onReaderThemeChange: (ReaderTheme) -> Unit,
    onModeChanged: (ReadingMode) -> Unit,
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
    onProgressChanged: (Float, String?) -> Unit,
    onProgressJumpRequest: (Float) -> Unit,
    onPaginationChanged: (Int, Int) -> Unit,
    onNextChapter: () -> Unit,
    onPreviousChapter: () -> Unit,
    onJumpToChapter: (String) -> Unit,
    onJumpConsumed: () -> Unit,
    onPageTurnConsumed: () -> Unit,
    onAddHighlight: (chapterAnchor: String, selectionJson: String, text: String, color: String) -> Unit,
    onAddBookmark: (chapterAnchor: String, cfi: String, title: String?) -> Unit,
    onAddBookmarkAtCurrent: () -> Unit,
    onAddMarginNote: (chapterAnchor: String, cfi: String, content: String, color: String) -> Unit,
    onRemoveHighlight: (Long) -> Unit,
    onRemoveMarginNote: (MarginNoteEntity) -> Unit,
    onChaptersLoaded: (List<Chapter>) -> Unit,
    onResetSettings: () -> Unit,
    onRestoreSettings: (ReaderSettings) -> Unit,
    onApplyPreset: (ReadingPreset) -> Unit,
    onRetry: () -> Unit,
    onTextSelected: (String, String, String, Float, Float) -> Unit,
    onHighlightClick: (Long, Float, Float) -> Unit,
    onMarginNoteClick: (Long, Float, Float) -> Unit,
    onImageClick: (String?) -> Unit,
    onDismissMenus: () -> Unit,
    onCustomColorSelected: (Int) -> Unit,
    onCustomColorPreview: (Int) -> Unit,
    onCustomFontColorSelected: (Int) -> Unit,
    onCustomFontColorPreview: (Int) -> Unit,
    onCustomFontSelected: (String?) -> Unit,
    onClearCustomFont: () -> Unit = {},
    onBackgroundImageSelected: (String?) -> Unit,
    onBackgroundImageBlurChange: (Float) -> Unit,
    onBackgroundImageBlurChangeFinished: (Float) -> Unit,
    onBackgroundImageOpacityChange: (Float) -> Unit,
    onBackgroundImageOpacityChangeFinished: (Float) -> Unit,
    onBackgroundImageZoomChange: (Float) -> Unit = {},
    onBackgroundImageZoomChangeFinished: (Float) -> Unit = {},
    onImageFilterChange: (ImageFilter) -> Unit,
    onUsePublisherStyleToggle: (Boolean) -> Unit,
    onUnderlineLinksToggle: (Boolean) -> Unit,
    onTextShadowToggle: (Boolean) -> Unit,
    onTextShadowColorChange: (Int?) -> Unit,
    onTextShadowColorPreview: (Int?) -> Unit,
    onAmbientModeToggle: (Boolean) -> Unit,
    onTapZoneActionChange: (String, ReaderTapZoneAction) -> Unit,
    onNavigationBarStyleChange: (NavigationBarStyle) -> Unit,
    onSearch: (String) -> Unit,
    searchResults: List<SearchResult>,
    isSearching: Boolean,
    onSearchResults: (List<SearchResult>) -> Unit,
    onSearchRequestConsumed: () -> Unit,
    onTextExtracted: (String) -> Unit = {},
    onPageTurn3dToggle: (Boolean) -> Unit = {},
    onInvertPageTurnsToggle: (Boolean) -> Unit = {},
    onPageTransitionStyleChange: (PageTransitionStyle) -> Unit = {},
    onTextAlignmentChange: (TextAlignment) -> Unit = {},
    onElementStyleFontChange: (ReaderTextElement, ReaderFont) -> Unit = { _, _ -> },
    onElementStyleColorChange: (ReaderTextElement, Int?) -> Unit = { _, _ -> },
    onElementStyleColorPreview: (ReaderTextElement, Int?) -> Unit = { _, _ -> },
    onLoadingProgressChange: (Float) -> Unit = {},
    autoReadEnabled: Boolean = false,
    isListenReady: Boolean = false,
    onToggleAutoRead: () -> Unit = {},
    onPauseListen: () -> Unit = {},
    onResumeListen: () -> Unit = {},
    onStopListen: () -> Unit = {},
    onResetListen: () -> Unit = {},
    onListenSpeedChange: (Float) -> Unit = {},
    listenSpeed: Float = 1f,
    currentListenSentence: String = "",
    sleepTimerMode: ListenSleepTimerMode = ListenSleepTimerMode.OFF,
    sleepTimerRemainingMs: Long? = null,
    onSleepTimerModeChange: (ListenSleepTimerMode) -> Unit = {},
    isListenSpeaking: Boolean = false,
    listenWordIndex: Int? = null,
    displayPrefs: HomeDisplayPreferences = HomeDisplayPreferences(),
    modifier: Modifier = Modifier,
    onHighlightContextClick: (Long, Float, Float) -> Unit = { _, _, _ -> },
    onStartListen: (String) -> Unit = {},
    onSearchResultSelected: (SearchResult) -> Unit = {},
    showOnboarding: Boolean = false,
    onDismissOnboarding: () -> Unit = {},
    onRequestPageTurn: (PageTurnDirection) -> Unit = {}
) {
    val localState = rememberReaderScreenLocalState()

    val isFocusMode = uiState.settings.focusMode
    val hideStatusBar = uiState.settings.hideStatusBar
    val navBarStyle = uiState.settings.navBarStyle
    val effectiveBookUri = uiState.resolvedBookUri ?: bookUri
    val effectiveBookType = uiState.resolvedBookType ?: bookType
    val isPageMode = uiState.settings.readingMode == ReadingMode.PAGE
    val hasBookmarkOnPage = currentBookmarkFor(uiState, isPageMode) != null

    val readerBg = uiState.settings.readerTheme.getBackgroundColor(isDarkTheme, uiState.settings.customBackgroundColor)
    val isLightBg = readerBg.luminance() > 0.5f
    val contrastingContentColor = if (isLightBg) Color.Black else Color.White
    val statusBarColor = MaterialTheme.colorScheme.background
    val pickers = rememberReaderPickerLaunchers(
        onBackgroundImageSelected = onBackgroundImageSelected,
        onCustomFontSelected = onCustomFontSelected
    )

    ReaderScreenWindowEffects(
        statusBarColor = statusBarColor,
        hideStatusBar = hideStatusBar
    )

    LaunchedEffect(isFocusMode) {
        if (isFocusMode) {
            localState.showChrome = false
            localState.dismissDockedPanel()
        }
    }

    val baseHighlightColors = listOf(
        "#FFF176", // Yellow
        "#A5D6A7", // Green
        "#80DEEA", // Cyan
        "#CE93D8"  // Purple
    )
    val highlightColors = baseHighlightColors

    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val screenHeightDp = LocalConfiguration.current.screenHeightDp.dp
    val liquidGlassEnabled = displayPrefs.liquidGlassEffect
    val showReaderChrome = localState.showChrome || localState.hasDockedPanel
    val activeReaderActionId = when (localState.dockedPanel) {
        ReaderDockedPanel.SETTINGS -> "settings"
        ReaderDockedPanel.SEARCH -> "search"
        ReaderDockedPanel.LISTEN -> "listen"
        ReaderDockedPanel.ACCESSIBILITY -> "accessibility"
        ReaderDockedPanel.ANALYTICS -> "analytics"
        ReaderDockedPanel.EXPORT -> "export"
        else -> null
    }

    fun toggleDockedPanel(panel: ReaderDockedPanel) {
        localState.toggleDockedPanel(panel)
        localState.showChrome = true
    }

    val handleNoteDismiss = {
        localState.clearNoteEditor()
    }
    val handleNoteSave = {
        val target = localState.noteTargetSelection
        if (target != null && localState.noteDraft.isNotBlank()) {
            onAddMarginNote(
                target.chapterAnchor,
                target.selectionJson,
                localState.noteDraft.trim(),
                "#FFF59D"
            )
        }
        localState.clearNoteEditor()
        onDismissMenus()
    }
    val handleNoteCancel = {
        localState.clearNoteEditor()
    }

    val handleTapZone: (String) -> Unit = { zone ->
        val action = when (zone.uppercase()) {
            "LEFT" -> uiState.settings.leftTapAction
            "RIGHT" -> uiState.settings.rightTapAction
            "TOP" -> uiState.settings.topTapAction
            "BOTTOM" -> uiState.settings.bottomTapAction
            else -> ReaderTapZoneAction.TOGGLE_UI
        }
        when (action) {
            ReaderTapZoneAction.NEXT_PAGE -> {
                if (isPageMode) onRequestPageTurn(PageTurnDirection.NEXT)
            }
            ReaderTapZoneAction.PREVIOUS_PAGE -> {
                if (isPageMode) onRequestPageTurn(PageTurnDirection.PREV)
            }
            ReaderTapZoneAction.TOGGLE_UI -> {
                localState.showChrome = !localState.showChrome
            }
            ReaderTapZoneAction.LISTEN -> {
                if (isListenSpeaking || autoReadEnabled) {
                    onStopListen()
                } else {
                    onToggleAutoRead()
                    localState.showDockedPanel(ReaderDockedPanel.LISTEN)
                    localState.showChrome = true
                }
            }
            ReaderTapZoneAction.BOOKMARK -> {
                if (isPageMode) onAddBookmarkAtCurrent()
            }
            ReaderTapZoneAction.NONE -> Unit
        }
    }

    val dockedPanelContent: (@Composable () -> Unit)? = when (localState.dockedPanel) {
        ReaderDockedPanel.SETTINGS -> ({
            ReaderSettingsPanelContent(
                settings = uiState.settings,
                isDarkTheme = isDarkTheme,
                onDismiss = { localState.dismissDockedPanel() },
                onReadingModeChange = onModeChanged,
                onThemeChange = onReaderThemeChange,
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
                onPickCustomFont = pickers.launchFontPicker,
                onClearCustomFont = onClearCustomFont,
                onPickBackgroundImage = pickers.launchImagePicker,
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
                    .padding(bottom = 8.dp)
            )
        })

        ReaderDockedPanel.MENU -> ({
            TableOfContentsPanelContent(
                chapters = uiState.chapters,
                currentChapterIndex = uiState.currentChapterIndex,
                bookmarks = uiState.bookmarks,
                highlights = uiState.highlights,
                marginNotes = uiState.marginNotes,
                onLocationSelected = { location ->
                    onJumpToChapter(location)
                    localState.dismissDockedPanel()
                },
                onDismiss = { localState.dismissDockedPanel() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        })

        ReaderDockedPanel.SEARCH -> ({
            SearchPanelContent(
                onDismiss = { localState.dismissDockedPanel() },
                onResultSelected = { result ->
                    onSearchResultSelected(result)
                    localState.dismissDockedPanel()
                },
                onSearch = onSearch,
                results = searchResults,
                isSearching = isSearching,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        })

        ReaderDockedPanel.LISTEN -> ({
            ListenDockedPanel(
                isSpeaking = isListenSpeaking,
                playbackSpeed = listenSpeed,
                isReady = isListenReady,
                autoReadEnabled = autoReadEnabled,
                currentSentence = currentListenSentence,
                sleepTimerMode = sleepTimerMode,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                onSpeedChange = onListenSpeedChange,
                onPause = onPauseListen,
                onResume = onResumeListen,
                onStartAutoRead = onToggleAutoRead,
                onStop = onStopListen,
                onReset = onResetListen,
                onSleepTimerModeChange = onSleepTimerModeChange,
                onDismiss = { localState.dismissDockedPanel() }
            )
        })

        ReaderDockedPanel.ACCESSIBILITY -> ({
            AccessibilitySettings(
                onDismiss = { localState.dismissDockedPanel() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        })

        ReaderDockedPanel.ANALYTICS -> ({
            AnalyticsDashboard(
                onDismiss = { localState.dismissDockedPanel() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
        })

        ReaderDockedPanel.EXPORT -> ({
            ExportDialog(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                onDismiss = { localState.dismissDockedPanel() }
            )
        })

        null -> null
    }

    ReaderScreenOverlays(
        showSettings = false,
        settings = uiState.settings,
        isDarkTheme = isDarkTheme,
        onDismissSettings = { localState.dismissDockedPanel() },
        onReadingModeChange = onModeChanged,
        onThemeChange = onReaderThemeChange,
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
        onPickCustomFont = pickers.launchFontPicker,
        onClearCustomFont = onClearCustomFont,
        onPickBackgroundImage = pickers.launchImagePicker,
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
        showChapterSheet = false,
        chapters = uiState.chapters,
        currentChapterIndex = uiState.currentChapterIndex,
        bookmarks = uiState.bookmarks,
        highlights = uiState.highlights,
        marginNotes = uiState.marginNotes,
        onLocationSelected = { location ->
            onJumpToChapter(location)
            localState.dismissDockedPanel()
        },
        onDismissChapterSheet = { localState.dismissDockedPanel() },
        zoomImageUrl = uiState.zoomImageUrl,
        onDismissZoomImage = { onImageClick(null) },
        showSearchDialog = false,
        onDismissSearch = { localState.dismissDockedPanel() },
        onSearch = onSearch,
        searchResults = searchResults,
        isSearching = isSearching,
        onSearchResultSelected = { result ->
            onSearchResultSelected(result)
            localState.dismissDockedPanel()
        },
        showAccessibilitySettings = false,
        onDismissAccessibility = { localState.dismissDockedPanel() },
        showAnalytics = false,
        onDismissAnalytics = { localState.dismissDockedPanel() },
        showExportDialog = false,
        onDismissExport = { localState.dismissDockedPanel() },
        showAddNoteDialog = localState.showAddNoteDialog,
        noteDraft = localState.noteDraft,
        onNoteDraftChange = { localState.noteDraft = it },
        onNoteDismiss = handleNoteDismiss,
        onNoteSave = handleNoteSave,
        onNoteCancel = handleNoteCancel,
        showHighlightColorPicker = localState.showHighlightColorPicker,
        onDismissHighlightColorPicker = { localState.showHighlightColorPicker = false },
        onHighlightColorSelected = {
            localState.customHighlightColorInt = it
            localState.highlightOriginalColor = null
        },
        initialHighlightColor = localState.highlightOriginalColor ?: localState.customHighlightColorInt ?: 0xFFF176,
        onHighlightColorPreview = { localState.customHighlightColorInt = it },
        onHighlightColorCancel = {
            localState.customHighlightColorInt = localState.highlightOriginalColor
            localState.highlightOriginalColor = null
        }
    )

    Box(modifier = modifier.fillMaxSize()) {
        ReaderScreenContent(
            modifier = Modifier.fillMaxSize(),
            uiState = uiState,
            bookUri = effectiveBookUri,
            bookType = effectiveBookType,
            isDarkTheme = isDarkTheme,
            isFocusMode = isFocusMode,
            hideStatusBar = hideStatusBar,
            showChrome = localState.showChrome,
            navBarStyle = navBarStyle,
            readerBackground = readerBg,
            contrastingContentColor = contrastingContentColor,
            isPageMode = isPageMode,
            hasBookmarkOnPage = hasBookmarkOnPage,
            screenWidthDp = screenWidthDp,
            screenHeightDp = screenHeightDp,
            highlightColors = highlightColors,
            customHighlightColorInt = localState.customHighlightColorInt,
            onCustomColorClick = {
                localState.highlightOriginalColor = localState.customHighlightColorInt
                localState.showHighlightColorPicker = true
            },
            onProgressChanged = onProgressChanged,
            onPaginationChanged = onPaginationChanged,
            onRetry = onRetry,
            onBack = onBack,
            onToggleChrome = { localState.showChrome = !localState.showChrome },
            onTextSelected = onTextSelected,
            onHighlightClick = onHighlightClick,
            onMarginNoteClick = onMarginNoteClick,
            onImageClick = onImageClick,
            onChaptersLoaded = onChaptersLoaded,
            onTextExtracted = onTextExtracted,
            onStartListen = onStartListen,
            onTapZone = handleTapZone,
            isListenSpeaking = isListenSpeaking,
            autoReadEnabled = autoReadEnabled,
            listenWordIndex = listenWordIndex,
            onLoadingProgressChange = onLoadingProgressChange,
            onSearchResults = onSearchResults,
            onSearchRequestConsumed = onSearchRequestConsumed,
            onJumpConsumed = onJumpConsumed,
            onPageTurnConsumed = onPageTurnConsumed,
            onRequestScrollMode = { onModeChanged(ReadingMode.SCROLL) },
            onAddHighlight = onAddHighlight,
            onRemoveHighlight = onRemoveHighlight,
            onRequestAddNote = { selection, draft ->
                localState.noteTargetSelection = selection
                localState.noteDraft = draft
                localState.showAddNoteDialog = true
            },
            onRequestEditNote = { selection ->
                localState.noteTargetSelection = selection
                localState.noteDraft = selection.text
                localState.showAddNoteDialog = true
            },
            onRemoveMarginNote = onRemoveMarginNote,
            onStartListenFromSelection = { text ->
                onStartListen(text)
                localState.showDockedPanel(ReaderDockedPanel.LISTEN)
                localState.showChrome = true
            },
            onDismissMenus = onDismissMenus
        )

        if (!localState.hasDockedPanel) {
            ListenMiniPlayer(
                currentSentence = currentListenSentence,
                speed = listenSpeed,
                isSpeaking = isListenSpeaking,
                autoReadEnabled = autoReadEnabled,
                sleepTimerMode = sleepTimerMode,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                readerBackground = readerBg,
                onPlayPause = {
                    if (isListenSpeaking) {
                        onPauseListen()
                    } else if (autoReadEnabled) {
                        onResumeListen()
                    } else {
                        onToggleAutoRead()
                    }
                },
                onStop = onStopListen,
                onSpeedChange = onListenSpeedChange,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = if (showReaderChrome) 148.dp else 24.dp
                    )
            )
        }

        ReaderTopChrome(
            visible = showReaderChrome,
            navBarStyle = navBarStyle,
            uiState = uiState,
            onBack = onBack,
            showSearchAction = displayPrefs.showReaderSearch,
            onShowSearch = { toggleDockedPanel(ReaderDockedPanel.SEARCH) },
            onShowChapters = { toggleDockedPanel(ReaderDockedPanel.MENU) },
            onAddBookmark = onAddBookmarkAtCurrent,
            autoReadEnabled = autoReadEnabled,
            isListenReady = isListenReady,
            onToggleAutoRead = onToggleAutoRead,
            onShowListen = { toggleDockedPanel(ReaderDockedPanel.LISTEN) },
            isPageMode = isPageMode,
            hasBookmarkOnPage = hasBookmarkOnPage,
            ambientMode = uiState.settings.ambientMode,
            readerBackground = readerBg
        )

        ReaderBottomChrome(
            visible = showReaderChrome,
            navBarStyle = navBarStyle,
            liquidGlassEnabled = liquidGlassEnabled,
            uiState = uiState,
            displayPrefs = displayPrefs,
            activeActionId = activeReaderActionId,
            onHome = onHome,
            onShowSettings = { toggleDockedPanel(ReaderDockedPanel.SETTINGS) },
            onShowSearch = { toggleDockedPanel(ReaderDockedPanel.SEARCH) },
            onShowListen = { toggleDockedPanel(ReaderDockedPanel.LISTEN) },
            onShowAccessibility = { toggleDockedPanel(ReaderDockedPanel.ACCESSIBILITY) },
            onShowAnalytics = { toggleDockedPanel(ReaderDockedPanel.ANALYTICS) },
            onShowExport = { toggleDockedPanel(ReaderDockedPanel.EXPORT) },
            onProgressChange = onProgressJumpRequest,
            dockedPanelContent = dockedPanelContent,
            ambientMode = uiState.settings.ambientMode,
            readerBackground = readerBg,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        ReaderOnboardingOverlay(
            visible = showOnboarding,
            onDismiss = onDismissOnboarding
        )
    }
}

private fun currentBookmarkFor(
    uiState: ReaderUiState,
    isPageMode: Boolean
): BookmarkEntity? {
    if (!isPageMode) return null
    val progressPercent = (uiState.progress * 100f).toInt().coerceIn(0, 100)
    val currentCfi = uiState.savedCfi?.takeIf { it.isNotBlank() } ?: "progress:$progressPercent"
    return uiState.bookmarks.firstOrNull { bookmark ->
        bookmark.cfi == currentCfi || bookmark.chapterAnchor == currentCfi
    }
}
