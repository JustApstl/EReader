package com.dyu.ereader.ui.home.state

import com.dyu.ereader.data.model.app.AppTheme
import com.dyu.ereader.data.model.library.BookCollectionShelf
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.data.model.reader.ReaderControl
import com.dyu.ereader.data.model.reader.ReaderSettings

enum class BookFilter {
    ALL, TITLE, AUTHOR, LANGUAGE, YEAR, EXTENSION
}

enum class SortOrder(val label: String) {
    TITLE("Title"),
    AUTHOR("Author"),
    DATE_ADDED("Date Added"),
    LAST_OPENED("Recently Read"),
    PROGRESS("Reading Progress"),
    FILE_SIZE("File Size")
}

enum class LibraryLayout {
    GRID, LIST
}

enum class ReadingStatus(val label: String) {
    UNREAD("Unread"),
    IN_PROGRESS("In Progress"),
    FINISHED("Finished")
}

data class HomeDisplayPreferences(
    val showBookType: Boolean = true,
    val hideStatusBar: Boolean = false,
    val showRecentReading: Boolean = true,
    val showFavorites: Boolean = true,
    val showGenres: Boolean = true,
    val gridColumns: Int = 2,
    val layout: LibraryLayout = LibraryLayout.GRID,
    val animationsEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true,
    val textScrollerEnabled: Boolean = true,
    val hideBetaFeatures: Boolean = false,
    val developerOptionsEnabled: Boolean = false,
    val theme: AppTheme = AppTheme.SYSTEM,
    val liquidGlassEffect: Boolean = false,
    val appTextScale: Float = 1f,
    // Reader feature toggles
    val showReaderSearch: Boolean = true,
    val showReaderListen: Boolean = true,
    val showReaderAccessibility: Boolean = true,
    val showReaderAnalytics: Boolean = true,
    val showReaderExport: Boolean = true,
    val readerControlOrder: List<ReaderControl> = ReaderControl.defaultOrder(),
    val notificationsEnabled: Boolean = false,
    val readingReminderEnabled: Boolean = false,
    val readingReminderHour: Int = 20,
    val readingReminderMinute: Int = 0
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
    val availableGenres: List<String> = emptyList(),
    val selectedLanguages: Set<String> = emptySet(),
    val selectedYears: Set<String> = emptySet(),
    val availableLanguages: List<String> = emptyList(),
    val availableYears: List<String> = emptyList(),
    val selectedCountries: Set<String> = emptySet(),
    val availableCountries: List<String> = emptyList(),
    val collections: List<BookCollectionShelf> = emptyList(),
    val newDownloadIds: Set<String> = emptySet(),
    val selectedStatuses: Set<ReadingStatus> = emptySet(),
    val readerSettings: ReaderSettings = ReaderSettings(),
    val lastLocalBackupExportAt: Long? = null,
    val lastLocalBackupImportAt: Long? = null
) {
    val hasLibraryAccess: Boolean get() = !libraryUri.isNullOrBlank()
}
