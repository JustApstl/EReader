package com.dyu.ereader.ui.reader.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.dyu.ereader.data.local.db.BookmarkEntity
import com.dyu.ereader.data.local.db.HighlightEntity
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.ui.reader.state.HighlightMenuState
import com.dyu.ereader.ui.reader.state.MarginNoteMenuState
import com.dyu.ereader.ui.reader.state.SelectionMenuState
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun ReaderViewModel.handleAddHighlight(
    chapterAnchor: String,
    selectionJson: String,
    text: String,
    color: String
) {
    viewModelScope.launch {
        val existingHighlight = uiStateFlow.value.highlights.firstOrNull { it.selectionJson == selectionJson }
        val existingColor = existingHighlight?.color?.trim()?.uppercase()
        val targetColor = color.trim().uppercase()
        if (existingColor == targetColor) {
            return@launch
        }

        readerRepository.removeHighlightsBySelection(readerBookId, selectionJson)
        readerRepository.addHighlight(
            HighlightEntity(
                bookId = readerBookId,
                chapterAnchor = chapterAnchor,
                selectionJson = selectionJson,
                selectedText = text,
                color = color,
                createdAt = System.currentTimeMillis()
            )
        )
        if (existingHighlight == null) {
            analyticsRepo.recordHighlight(readerBookId)
        }
    }
}

internal fun ReaderViewModel.handleRemoveHighlight(id: Long) {
    viewModelScope.launch {
        readerRepository.removeHighlight(id)
        dismissMenus()
    }
}

internal fun ReaderViewModel.handleAddBookmark(
    chapterAnchor: String,
    cfi: String,
    title: String?
) {
    if (uiStateFlow.value.settings.readingMode != ReadingMode.PAGE) {
        return
    }
    viewModelScope.launch {
        uiStateFlow.value.bookmarks
            .firstOrNull { it.cfi == cfi || it.chapterAnchor == chapterAnchor }
            ?.let { readerRepository.removeBookmark(it) }

        readerRepository.addBookmark(
            BookmarkEntity(
                bookId = readerBookId,
                chapterAnchor = chapterAnchor,
                cfi = cfi,
                title = title
            )
        )
        analyticsRepo.recordBookmark(readerBookId)
    }
}

internal fun ReaderViewModel.handleAddBookmarkAtCurrentLocation() {
    if (uiStateFlow.value.settings.readingMode != ReadingMode.PAGE) {
        return
    }
    val state = uiStateFlow.value
    val progressPercent = (state.progress * 100f).toInt().coerceIn(0, 100)
    val currentCfi = state.savedCfi?.takeIf { it.isNotBlank() }
        ?: "progress:$progressPercent"

    viewModelScope.launch {
        val existingBookmark = state.bookmarks.firstOrNull {
            it.cfi == currentCfi || it.chapterAnchor == currentCfi
        }
        if (existingBookmark != null) {
            readerRepository.removeBookmark(existingBookmark)
            return@launch
        }

        val pageTitle = if (state.totalPages > 0) {
            "Page ${state.currentPage.coerceIn(1, state.totalPages)}"
        } else {
            "${progressPercent}%"
        }
        readerRepository.addBookmark(
            BookmarkEntity(
                bookId = readerBookId,
                chapterAnchor = currentCfi,
                cfi = currentCfi,
                title = "Bookmark $pageTitle"
            )
        )
        analyticsRepo.recordBookmark(readerBookId)
    }
}

internal fun ReaderViewModel.handleRemoveBookmark(bookmark: BookmarkEntity) {
    viewModelScope.launch {
        readerRepository.removeBookmark(bookmark)
    }
}

internal fun ReaderViewModel.handleAddMarginNote(
    chapterAnchor: String,
    cfi: String,
    content: String,
    color: String
) {
    if (content.isBlank()) return
    viewModelScope.launch {
        readerRepository.removeMarginNotesByCfi(readerBookId, cfi)
        readerRepository.addMarginNote(
            MarginNoteEntity(
                bookId = readerBookId,
                chapterAnchor = chapterAnchor,
                cfi = cfi,
                position = "RIGHT",
                content = content.trim(),
                color = color
            )
        )
        analyticsRepo.recordNoteAdded(readerBookId)
        dismissMenus()
    }
}

internal fun ReaderViewModel.handleRemoveMarginNote(note: MarginNoteEntity) {
    viewModelScope.launch {
        readerRepository.deleteMarginNote(note)
        dismissMenus()
    }
}

internal fun ReaderViewModel.handleTextSelected(
    anchor: String,
    json: String,
    text: String,
    x: Float,
    y: Float
) {
    Log.d("ReaderViewModel", "onTextSelected: $text at ($x, $y)")
    uiStateFlow.update {
        it.copy(
            selectionMenu = SelectionMenuState(anchor, json, text, x, y),
            highlightMenu = null,
            marginNoteMenu = null
        )
    }
}

internal fun ReaderViewModel.handleHighlightClicked(id: Long, x: Float, y: Float) {
    Log.d("ReaderViewModel", "onHighlightClicked: $id at ($x, $y)")
    uiStateFlow.update {
        it.copy(highlightMenu = HighlightMenuState(id, x, y), selectionMenu = null, marginNoteMenu = null)
    }
}

internal fun ReaderViewModel.handleMarginNoteClicked(id: Long, x: Float, y: Float) {
    Log.d("ReaderViewModel", "onMarginNoteClicked: $id at ($x, $y)")
    uiStateFlow.update {
        it.copy(marginNoteMenu = MarginNoteMenuState(id, x, y), selectionMenu = null, highlightMenu = null)
    }
}
