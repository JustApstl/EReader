package com.dyu.ereader.ui.reader.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.data.model.search.SearchResult
import com.dyu.ereader.ui.reader.overlays.menus.ReaderSelectionMenus
import com.dyu.ereader.ui.reader.state.Chapter
import com.dyu.ereader.ui.reader.state.ReaderUiState
import com.dyu.ereader.ui.reader.state.SelectionMenuState

@Composable
internal fun ReaderScreenContent(
    modifier: Modifier,
    uiState: ReaderUiState,
    bookUri: String,
    bookType: BookType,
    isDarkTheme: Boolean,
    isFocusMode: Boolean,
    hideStatusBar: Boolean,
    showChrome: Boolean,
    navBarStyle: NavigationBarStyle,
    readerBackground: Color,
    contrastingContentColor: Color,
    isPageMode: Boolean,
    hasBookmarkOnPage: Boolean,
    screenWidthDp: Dp,
    screenHeightDp: Dp,
    highlightColors: List<String>,
    customHighlightColorInt: Int?,
    onCustomColorClick: () -> Unit,
    onProgressChanged: (Float, String?) -> Unit,
    onPaginationChanged: (Int, Int) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    onToggleChrome: () -> Unit,
    onTextSelected: (String, String, String, Float, Float) -> Unit,
    onHighlightClick: (Long, Float, Float) -> Unit,
    onMarginNoteClick: (Long, Float, Float) -> Unit,
    onImageClick: (String?) -> Unit,
    onChaptersLoaded: (List<Chapter>) -> Unit,
    onTextExtracted: (String) -> Unit,
    onStartListen: (String) -> Unit,
    onTapZone: (String) -> Unit,
    isListenSpeaking: Boolean,
    autoReadEnabled: Boolean,
    listenWordIndex: Int?,
    onLoadingProgressChange: (Float) -> Unit,
    onSearchResults: (List<SearchResult>) -> Unit,
    onSearchRequestConsumed: () -> Unit,
    onJumpConsumed: () -> Unit,
    onPageTurnConsumed: () -> Unit,
    onRequestScrollMode: () -> Unit,
    onAddHighlight: (String, String, String, String) -> Unit,
    onRemoveHighlight: (Long) -> Unit,
    onRequestAddNote: (SelectionMenuState, String) -> Unit,
    onRequestEditNote: (SelectionMenuState) -> Unit,
    onRemoveMarginNote: (MarginNoteEntity) -> Unit,
    onStartListenFromSelection: (String) -> Unit,
    onDismissMenus: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(readerBackground)) {
        Box(modifier = Modifier.fillMaxSize()) {
            ReaderBackgroundImage(uiState)

            when {
                uiState.isLoading -> {
                    ReaderLoadingState(
                        progress = uiState.loadingProgress,
                        readerBackground = readerBackground,
                        contrastingContentColor = contrastingContentColor
                    )
                }

                uiState.errorMessage != null -> {
                    ReaderErrorState(
                        message = uiState.errorMessage,
                        onRetry = onRetry
                    )
                }

                else -> {
                    ReaderFormatContent(
                        bookUri = bookUri,
                        bookType = bookType,
                        uiState = uiState,
                        isDarkTheme = isDarkTheme,
                        isFocusMode = isFocusMode,
                        hideStatusBar = hideStatusBar,
                        onProgressChanged = onProgressChanged,
                        onPaginationChanged = onPaginationChanged,
                        onToggleChrome = onToggleChrome,
                        onTextSelected = onTextSelected,
                        onHighlightClick = onHighlightClick,
                        onMarginNoteClick = onMarginNoteClick,
                        onImageClick = onImageClick,
                        onChaptersLoaded = onChaptersLoaded,
                        onTextExtracted = onTextExtracted,
                        onStartListen = onStartListen,
                        onTapZone = onTapZone,
                        isListenSpeaking = isListenSpeaking,
                        autoReadEnabled = autoReadEnabled,
                        listenWordIndex = listenWordIndex,
                        onLoadingProgressChange = onLoadingProgressChange,
                        onSearchResults = onSearchResults,
                        onSearchRequestConsumed = onSearchRequestConsumed,
                        onJumpConsumed = onJumpConsumed,
                        onPageTurnConsumed = onPageTurnConsumed,
                        onRequestScrollMode = onRequestScrollMode,
                        onBack = onBack
                    )
                }
            }

            ReaderBookmarkIndicator(
                show = hasBookmarkOnPage,
                showChrome = showChrome
            )

            ReaderSelectionMenus(
                selection = uiState.selectionMenu,
                highlightMenu = uiState.highlightMenu,
                marginNoteMenu = uiState.marginNoteMenu,
                highlights = uiState.highlights,
                marginNotes = uiState.marginNotes,
                screenWidthDp = screenWidthDp,
                screenHeightDp = screenHeightDp,
                highlightColors = highlightColors,
                customHighlightColorInt = customHighlightColorInt,
                onCustomColorClick = onCustomColorClick,
                onAddHighlight = onAddHighlight,
                onRemoveHighlight = onRemoveHighlight,
                onRequestAddNote = onRequestAddNote,
                onRequestEditNote = onRequestEditNote,
                onRemoveMarginNote = onRemoveMarginNote,
                onStartListen = onStartListenFromSelection,
                onDismissMenus = onDismissMenus
            )
        }

        ReaderHiddenChromeProgress(
            visible = !showChrome && !isFocusMode && !uiState.isLoading,
            navBarStyle = navBarStyle,
            hideStatusBar = hideStatusBar,
            isPageMode = uiState.settings.readingMode == ReadingMode.PAGE,
            progress = uiState.progress,
            currentPage = uiState.currentPage,
            totalPages = uiState.totalPages
        )

        ReaderFocusHandle(
            visible = isFocusMode,
            showChrome = showChrome,
            onToggleChrome = onToggleChrome
        )
    }
}
