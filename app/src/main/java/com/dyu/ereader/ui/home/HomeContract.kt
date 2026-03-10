package com.dyu.ereader.ui.home

import com.dyu.ereader.data.model.AppTheme
import com.dyu.ereader.data.model.BookItem
import com.dyu.ereader.data.model.BookType
import com.dyu.ereader.data.model.ReaderControl

enum class BookFilter {
    ALL, TITLE, AUTHOR, LANGUAGE, YEAR, EXTENSION
}

enum class SortOrder {
    TITLE, AUTHOR, DATE_ADDED
}

enum class LibraryLayout {
    GRID, LIST
}

data class HomeDisplayPreferences(
    val showBookType: Boolean = true,
    val hideStatusBar: Boolean = false,
    val showRecentReading: Boolean = true,
    val showFavorites: Boolean = true,
    val showGenres: Boolean = true,
    val gridColumns: Int = 3,
    val layout: LibraryLayout = LibraryLayout.GRID,
    val animationsEnabled: Boolean = true,
    val theme: AppTheme = AppTheme.SYSTEM,
    val liquidGlassEffect: Boolean = false,
    // Reader feature toggles
    val showReaderSearch: Boolean = true,
    val showReaderTTS: Boolean = true,
    val showReaderAccessibility: Boolean = true,
    val showReaderAnalytics: Boolean = true,
    val showReaderExport: Boolean = true,
    val readerControlOrder: List<ReaderControl> = ReaderControl.defaultOrder()
)

data class HomeUiState(
    val libraryUri: String? = null,
    val allBooks: List<BookItem> = emptyList(),
    val visibleBooks: List<BookItem> = emptyList(),
    val recentBooks: List<BookItem> = emptyList(),
    val searchQuery: String = "",
    val searchFilter: BookFilter = BookFilter.ALL,
    val sortOrder: SortOrder = SortOrder.TITLE,
    val isScanning: Boolean = false,
    val errorMessage: String? = null,
    val display: HomeDisplayPreferences = HomeDisplayPreferences(),
    val selectedTypes: Set<BookType> = emptySet(),
    val selectedGenres: Set<String> = emptySet(),
    val availableGenres: List<String> = emptyList()
) {
    val hasLibraryAccess: Boolean get() = !libraryUri.isNullOrBlank()
}
