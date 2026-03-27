package com.dyu.ereader.ui.reader.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.ui.home.viewmodel.HomeViewModel
import com.dyu.ereader.ui.reader.screens.ReaderScreen
import com.dyu.ereader.ui.reader.viewmodel.ReaderViewModel

@Composable
internal fun ReaderRouteContent(
    bookUri: String,
    bookType: BookType,
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit,
    readerViewModel: ReaderViewModel,
    homeViewModel: HomeViewModel
) {
    val state by readerViewModel.uiState.collectAsState()
    val searchResults by readerViewModel.searchResults.collectAsState()
    val isSearching by readerViewModel.isSearching.collectAsState()
    val autoReadEnabled by readerViewModel.autoReadEnabled.collectAsState()
    val isListenReady by readerViewModel.isTTSReady.collectAsState(initial = false)
    val isListenSpeaking by readerViewModel.isSpeaking.collectAsState(initial = false)
    val listenWordIndex by readerViewModel.ttsWordIndex.collectAsState(initial = null)
    val currentListenSentence by readerViewModel.currentTTSSentence.collectAsState()
    val listenSpeed by readerViewModel.ttsSpeed.collectAsState(initial = 1.0f)
    val sleepTimerMode by readerViewModel.sleepTimerMode.collectAsState()
    val sleepTimerRemainingMs by readerViewModel.sleepTimerRemainingMs.collectAsState()
    val showOnboarding by readerViewModel.showOnboarding.collectAsState()
    val homeState by homeViewModel.uiState.collectAsState()
    val stopListen = {
        readerViewModel.stopTTS()
        readerViewModel.setAutoReadEnabled(false)
    }

    LaunchedEffect(isDarkTheme) {
        readerViewModel.onThemeChanged(isDarkTheme)
    }

    LaunchedEffect(Unit) {
        readerViewModel.startReadingSession()
    }

    DisposableEffect(Unit) {
        onDispose {
            readerViewModel.endReadingSession()
        }
    }

    ReaderScreen(
        bookUri = bookUri,
        bookType = bookType,
        uiState = state,
        isDarkTheme = isDarkTheme,
        onBack = onBack,
        onHome = onNavigateHome,
        onReaderThemeChange = readerViewModel::setReaderTheme,
        onModeChanged = readerViewModel::setReadingMode,
        onFontColorThemeChange = readerViewModel::setFontColorTheme,
        onAutoFontColorToggle = readerViewModel::setAutoFontColor,
        onFocusTextToggle = readerViewModel::setFocusTextEnabled,
        onFocusTextBoldnessChange = readerViewModel::previewFocusTextBoldness,
        onFocusTextBoldnessChangeFinished = readerViewModel::setFocusTextBoldness,
        onFocusTextEmphasisChange = readerViewModel::previewFocusTextEmphasis,
        onFocusTextEmphasisChangeFinished = readerViewModel::setFocusTextEmphasis,
        onFocusTextColorChange = readerViewModel::setFocusTextColor,
        onFocusTextColorPreview = readerViewModel::previewFocusTextColor,
        onFocusModeToggle = readerViewModel::setFocusMode,
        onHideStatusBarToggle = readerViewModel::setHideStatusBar,
        onFontSizeChange = readerViewModel::previewReaderFontSize,
        onFontSizeChangeFinished = readerViewModel::setReaderFontSize,
        onLineSpacingChange = readerViewModel::previewLineSpacing,
        onLineSpacingChangeFinished = readerViewModel::setLineSpacing,
        onMarginChange = readerViewModel::previewMargins,
        onMarginChangeFinished = readerViewModel::setMargins,
        onFontChange = readerViewModel::setFont,
        onProgressChanged = readerViewModel::onProgressChanged,
        onProgressJumpRequest = readerViewModel::requestProgressJump,
        onPaginationChanged = readerViewModel::onPaginationChanged,
        onNextChapter = { /* EPUB.js handles this internally */ },
        onPreviousChapter = { /* EPUB.js handles this internally */ },
        onJumpToChapter = readerViewModel::jumpToChapter,
        onJumpConsumed = readerViewModel::consumeJumps,
        onPageTurnConsumed = readerViewModel::consumePageTurn,
        onAddHighlight = readerViewModel::addHighlight,
        onAddBookmark = readerViewModel::addBookmark,
        onAddBookmarkAtCurrent = readerViewModel::addBookmarkAtCurrentLocation,
        onAddMarginNote = readerViewModel::addMarginNote,
        onRemoveHighlight = readerViewModel::removeHighlight,
        onRemoveMarginNote = readerViewModel::removeMarginNote,
        onChaptersLoaded = readerViewModel::setChapters,
        onResetSettings = readerViewModel::resetSettings,
        onRestoreSettings = readerViewModel::applySettings,
        onApplyPreset = readerViewModel::applyPreset,
        onRetry = readerViewModel::retry,
        onTextSelected = readerViewModel::onTextSelected,
        onHighlightClick = readerViewModel::onHighlightClicked,
        onMarginNoteClick = readerViewModel::onMarginNoteClicked,
        onImageClick = readerViewModel::zoomImage,
        onDismissMenus = readerViewModel::dismissMenus,
        onCustomColorSelected = readerViewModel::setCustomBackgroundColor,
        onCustomColorPreview = readerViewModel::previewCustomBackgroundColor,
        onCustomFontColorSelected = readerViewModel::setCustomFontColor,
        onCustomFontColorPreview = readerViewModel::previewCustomFontColor,
        onCustomFontSelected = readerViewModel::setCustomFontUri,
        onClearCustomFont = readerViewModel::clearCustomFont,
        onBackgroundImageSelected = readerViewModel::setBackgroundImageUri,
        onBackgroundImageBlurChange = readerViewModel::previewBackgroundImageBlur,
        onBackgroundImageBlurChangeFinished = readerViewModel::setBackgroundImageBlur,
        onBackgroundImageOpacityChange = readerViewModel::previewBackgroundImageOpacity,
        onBackgroundImageOpacityChangeFinished = readerViewModel::setBackgroundImageOpacity,
        onBackgroundImageZoomChange = readerViewModel::previewBackgroundImageZoom,
        onBackgroundImageZoomChangeFinished = readerViewModel::setBackgroundImageZoom,
        onImageFilterChange = readerViewModel::setImageFilter,
        onUsePublisherStyleToggle = readerViewModel::setUsePublisherStyle,
        onUnderlineLinksToggle = readerViewModel::setUnderlineLinks,
        onTextShadowToggle = readerViewModel::setTextShadow,
        onTextShadowColorChange = readerViewModel::setTextShadowColor,
        onTextShadowColorPreview = readerViewModel::previewTextShadowColor,
        onAmbientModeToggle = readerViewModel::setAmbientMode,
        onTapZoneActionChange = readerViewModel::setTapZoneAction,
        onNavigationBarStyleChange = readerViewModel::setNavigationBarStyle,
        onPageTurn3dToggle = readerViewModel::setPageTurn3dEnabled,
        onInvertPageTurnsToggle = readerViewModel::setInvertPageTurns,
        onPageTransitionStyleChange = readerViewModel::setPageTransitionStyle,
        onTextAlignmentChange = readerViewModel::setTextAlignment,
        onElementStyleFontChange = readerViewModel::setElementStyleFont,
        onElementStyleColorChange = readerViewModel::setElementStyleColor,
        onElementStyleColorPreview = readerViewModel::previewElementStyleColor,
        onSearch = readerViewModel::searchInBook,
        searchResults = searchResults,
        isSearching = isSearching,
        onSearchResults = readerViewModel::onSearchResultsReceived,
        onSearchRequestConsumed = readerViewModel::consumeSearchRequest,
        onTextExtracted = readerViewModel::onTextExtracted,
        onStartListen = readerViewModel::startTTS,
        displayPrefs = homeState.display,
        onLoadingProgressChange = readerViewModel::setLoadingProgress,
        onSearchResultSelected = readerViewModel::navigateToSearchResult,
        autoReadEnabled = autoReadEnabled,
        isListenReady = isListenReady,
        onToggleAutoRead = { readerViewModel.setAutoReadEnabled(!autoReadEnabled) },
        onPauseListen = readerViewModel::pauseTTS,
        onResumeListen = readerViewModel::resumeTTS,
        onStopListen = stopListen,
        onResetListen = readerViewModel::resetTTS,
        onListenSpeedChange = readerViewModel::setTTSSpeed,
        listenSpeed = listenSpeed,
        currentListenSentence = currentListenSentence,
        sleepTimerMode = sleepTimerMode,
        sleepTimerRemainingMs = sleepTimerRemainingMs,
        onSleepTimerModeChange = readerViewModel::setSleepTimerMode,
        isListenSpeaking = isListenSpeaking,
        listenWordIndex = listenWordIndex,
        showOnboarding = showOnboarding,
        onDismissOnboarding = readerViewModel::dismissOnboarding,
        onRequestPageTurn = readerViewModel::requestPageTurn
    )
}
