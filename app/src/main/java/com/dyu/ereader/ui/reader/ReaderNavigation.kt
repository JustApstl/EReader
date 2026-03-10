package com.dyu.ereader.ui.reader

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dyu.ereader.data.model.BookType
import com.dyu.ereader.ui.home.HomeViewModel
import com.dyu.ereader.util.decodeNavArg
import com.dyu.ereader.util.encodeNavArg

const val READER_ROUTE = "reader/{bookUriArg}/{bookTypeArg}"
private const val READER_BASE = "reader"

fun NavController.navigateToReader(uri: String, typeName: String) {
    this.navigate("${READER_BASE}/${encodeNavArg(uri)}/${typeName}")
}

fun NavGraphBuilder.readerScreen(
    isDarkTheme: Boolean,
    onBack: () -> Unit,
    onNavigateHome: () -> Unit
) {
    composable(
        route = READER_ROUTE,
        arguments = listOf(
            navArgument("bookUriArg") { type = NavType.StringType },
            navArgument("bookTypeArg") { type = NavType.StringType }
        )
    ) { backStackEntry ->
        val bookArg = backStackEntry.arguments?.getString("bookUriArg") ?: return@composable
        val typeArg = backStackEntry.arguments?.getString("bookTypeArg") ?: BookType.EPUB.name
        val bookUri = decodeNavArg(bookArg)
        val bookType = BookType.valueOf(typeArg)

        val readerViewModel: ReaderViewModel = hiltViewModel(
            key = "reader_${bookArg}"
        )
        
        val homeViewModel: HomeViewModel = hiltViewModel()

        val state by readerViewModel.uiState.collectAsState()
        val searchResults by readerViewModel.searchResults.collectAsState()
        val isSearching by readerViewModel.isSearching.collectAsState()
        val homeState by homeViewModel.uiState.collectAsState()

        LaunchedEffect(isDarkTheme) {
            readerViewModel.onThemeChanged(isDarkTheme)
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
            onFocusTextBoldnessChange = readerViewModel::setFocusTextBoldness,
            onFocusTextColorChange = readerViewModel::setFocusTextColor,
            onFocusModeToggle = readerViewModel::setFocusMode,
            onHideStatusBarToggle = readerViewModel::setHideStatusBar,
            onFontSizeChange = readerViewModel::setReaderFontSize,
            onLineSpacingChange = readerViewModel::setLineSpacing,
            onMarginChange = readerViewModel::setMargins,
            onFontChange = readerViewModel::setFont,
            onProgressChanged = readerViewModel::onProgressChanged,
            onProgressJumpRequest = readerViewModel::requestProgressJump,
            onPaginationChanged = readerViewModel::onPaginationChanged,
            onNextChapter = { /* EPUB.js handles this internally */ },
            onPreviousChapter = { /* EPUB.js handles this internally */ },
            onJumpToChapter = readerViewModel::jumpToChapter,
            onJumpConsumed = readerViewModel::consumeJumps,
            onAddHighlight = readerViewModel::addHighlight,
            onAddBookmark = readerViewModel::addBookmark,
            onAddBookmarkAtCurrent = readerViewModel::addBookmarkAtCurrentLocation,
            onAddMarginNote = readerViewModel::addMarginNote,
            onRemoveHighlight = readerViewModel::removeHighlight,
            onRemoveMarginNote = readerViewModel::removeMarginNote,
            onChaptersLoaded = readerViewModel::setChapters,
            onResetSettings = readerViewModel::resetSettings,
            onRetry = readerViewModel::retry,
            onTextSelected = readerViewModel::onTextSelected,
            onHighlightClick = readerViewModel::onHighlightClicked,
            onMarginNoteClick = readerViewModel::onMarginNoteClicked,
            onImageClick = readerViewModel::zoomImage,
            onDismissMenus = readerViewModel::dismissMenus,
            onCustomColorSelected = readerViewModel::setCustomBackgroundColor,
            onCustomFontColorSelected = readerViewModel::setCustomFontColor,
            onCustomFontSelected = readerViewModel::setCustomFontUri,
            onClearCustomFont = readerViewModel::clearCustomFont,
            onBackgroundImageSelected = readerViewModel::setBackgroundImageUri,
            onBackgroundImageBlurChange = readerViewModel::setBackgroundImageBlur,
            onBackgroundImageOpacityChange = readerViewModel::setBackgroundImageOpacity,
            onImageFilterChange = readerViewModel::setImageFilter,
            onUsePublisherStyleToggle = readerViewModel::setUsePublisherStyle,
            onUnderlineLinksToggle = readerViewModel::setUnderlineLinks,
            onTextShadowToggle = readerViewModel::setTextShadow,
            onTextShadowColorChange = readerViewModel::setTextShadowColor,
            onNavigationBarStyleChange = readerViewModel::setNavigationBarStyle,
            onPageTurn3dToggle = readerViewModel::setPageTurn3dEnabled,
            onPageTransitionStyleChange = readerViewModel::setPageTransitionStyle,
            onSearch = readerViewModel::searchInBook,
            searchResults = searchResults,
            isSearching = isSearching,
            onTextExtracted = readerViewModel::onTextExtracted,
            onStartTTS = readerViewModel::startTTS,
            displayPrefs = homeState.display,
            onLoadingProgressChange = readerViewModel::setLoadingProgress
        )
    }
}
