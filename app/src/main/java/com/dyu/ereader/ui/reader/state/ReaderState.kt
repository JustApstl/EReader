package com.dyu.ereader.ui.reader.state

import com.dyu.ereader.data.local.db.BookmarkEntity
import com.dyu.ereader.data.local.db.HighlightEntity
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.data.model.reader.ReaderSettings


data class Chapter(
    val label: String,
    val href: String,
    val depth: Int = 0,
    val hasChildren: Boolean = false
)

data class SelectionMenuState(
    val chapterAnchor: String,
    val selectionJson: String,
    val text: String,
    val x: Float,
    val y: Float
)

data class HighlightMenuState(
    val highlightId: Long,
    val x: Float,
    val y: Float
)

data class MarginNoteMenuState(
    val noteId: Long,
    val x: Float,
    val y: Float
)

enum class PageTurnDirection {
    NEXT,
    PREV
}

data class ReaderUiState(
    val isLoading: Boolean = true,
    val loadingProgress: Float = 0f,
    val title: String = "",
    val author: String = "",
    val settings: ReaderSettings = ReaderSettings(),
    val progress: Float = 0f,
    val savedCfi: String? = null,
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val currentChapterIndex: Int = 0,
    val chapters: List<Chapter> = emptyList(),
    val highlights: List<HighlightEntity> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val marginNotes: List<MarginNoteEntity> = emptyList(),
    val errorMessage: String? = null,
    val selectionMenu: SelectionMenuState? = null,
    val highlightMenu: HighlightMenuState? = null,
    val marginNoteMenu: MarginNoteMenuState? = null,
    val zoomImageUrl: String? = null,
    val pendingAnchorJump: String? = null,
    val pendingProgressJump: Float? = null,
    val pendingPageTurn: PageTurnDirection? = null,
    val requestTextExtraction: Boolean = false,
    val lastExtractedText: String = "",
    val pendingSearchQuery: String? = null,
    val searchRequestId: Long = 0L,
    val resolvedBookUri: String? = null,
    val resolvedBookType: BookType? = null
)
