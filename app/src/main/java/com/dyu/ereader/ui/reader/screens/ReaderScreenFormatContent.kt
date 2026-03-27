package com.dyu.ereader.ui.reader.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.data.model.search.SearchResult
import com.dyu.ereader.ui.components.insets.stableStatusBarsPadding
import com.dyu.ereader.ui.reader.formats.azw3.Azw3ReaderScreen
import com.dyu.ereader.ui.reader.formats.epub.EpubJsReader
import com.dyu.ereader.ui.reader.formats.mobi.MobiReaderScreen
import com.dyu.ereader.ui.reader.formats.pdf.PdfReaderScreen
import com.dyu.ereader.ui.reader.formats.unsupported.UnsupportedFormatScreen
import com.dyu.ereader.ui.reader.state.Chapter
import com.dyu.ereader.ui.reader.state.ReaderUiState

@Composable
internal fun ReaderFormatContent(
    bookUri: String,
    bookType: BookType,
    uiState: ReaderUiState,
    isDarkTheme: Boolean,
    isFocusMode: Boolean,
    hideStatusBar: Boolean,
    onProgressChanged: (Float, String?) -> Unit,
    onPaginationChanged: (Int, Int) -> Unit,
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
    onBack: () -> Unit
) {
    when {
        bookType == BookType.PDF -> {
            PdfReaderScreen(
                uri = bookUri,
                settings = uiState.settings,
                initialProgress = uiState.progress,
                onProgressChanged = { p -> onProgressChanged(p, null) },
                onPaginationChanged = onPaginationChanged,
                modifier = Modifier
                    .fillMaxSize()
                    .stableStatusBarsPadding()
            )
        }

        bookType.isEpub -> {
            EpubJsReader(
                bookUri = bookUri,
                initialProgress = uiState.progress,
                initialCfi = uiState.savedCfi,
                settings = uiState.settings,
                highlights = uiState.highlights,
                marginNotes = uiState.marginNotes,
                isDarkTheme = isDarkTheme,
                onProgressChanged = onProgressChanged,
                onToggleMenu = onToggleChrome,
                onTextSelected = onTextSelected,
                onHighlightClicked = onHighlightClick,
                onMarginNoteClicked = onMarginNoteClick,
                onImageClick = { url -> onImageClick(url) },
                isSelectionMenuVisible = uiState.selectionMenu != null,
                isHighlightMenuVisible = uiState.highlightMenu != null,
                isMarginNoteMenuVisible = uiState.marginNoteMenu != null,
                onChaptersLoaded = onChaptersLoaded,
                onTextExtracted = onTextExtracted,
                onPaginationChanged = onPaginationChanged,
                onStartListen = onStartListen,
                onTapZone = onTapZone,
                isListenSpeaking = isListenSpeaking,
                listenActive = isListenSpeaking || autoReadEnabled || uiState.requestTextExtraction,
                listenWordIndex = listenWordIndex,
                onLoadingProgressChange = onLoadingProgressChange,
                pendingSearchQuery = uiState.pendingSearchQuery,
                searchRequestId = uiState.searchRequestId,
                onSearchRequestConsumed = onSearchRequestConsumed,
                onSearchResults = onSearchResults,
                pendingJumpHref = uiState.pendingAnchorJump,
                pendingProgressJump = uiState.pendingProgressJump,
                pendingPageTurn = uiState.pendingPageTurn,
                requestTextExtraction = uiState.requestTextExtraction,
                onJumpConsumed = onJumpConsumed,
                onPageTurnConsumed = onPageTurnConsumed,
                modifier = Modifier
                    .fillMaxSize()
                    .stableStatusBarsPadding()
            )
        }

        bookType == BookType.AZW3 -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Azw3ReaderScreen(onBack = onBack)
            }
        }

        bookType == BookType.MOBI -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                MobiReaderScreen(
                    uri = bookUri,
                    onBack = onBack,
                    isPageMode = uiState.settings.readingMode == ReadingMode.PAGE,
                    onRequestScrollMode = onRequestScrollMode
                )
            }
        }

        else -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                UnsupportedFormatScreen(onBack = onBack)
            }
        }
    }
}
