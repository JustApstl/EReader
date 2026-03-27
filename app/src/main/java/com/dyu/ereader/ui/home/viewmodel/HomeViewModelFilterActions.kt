package com.dyu.ereader.ui.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.data.model.reader.ReaderControl
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.ui.home.state.BookFilter
import com.dyu.ereader.ui.home.state.LibraryLayout
import com.dyu.ereader.ui.home.state.ReadingStatus
import com.dyu.ereader.ui.home.state.SortOrder
import com.dyu.ereader.ui.home.state.filterBooks
import com.dyu.ereader.ui.home.state.recentBooks
import com.dyu.ereader.ui.home.state.sortBooks
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun HomeViewModel.handleSearchChanged(query: String) {
    homeUiStateFlow.update { it.copy(searchQuery = query) }
    applyFiltersAndSortInternal()
}

internal fun HomeViewModel.handleSearchFilterChanged(filter: BookFilter) {
    homeUiStateFlow.update { it.copy(searchFilter = filter) }
    applyFiltersAndSortInternal()
}

internal fun HomeViewModel.handleToggleTypeFilter(type: BookType) {
    homeUiStateFlow.update { current ->
        val next = if (current.selectedTypes.contains(type)) current.selectedTypes - type else current.selectedTypes + type
        current.copy(selectedTypes = next)
    }
    applyFiltersAndSortInternal()
}

internal fun HomeViewModel.handleToggleGenreFilter(genre: String) {
    homeUiStateFlow.update { current ->
        val next = if (current.selectedGenres.contains(genre)) current.selectedGenres - genre else current.selectedGenres + genre
        current.copy(selectedGenres = next)
    }
    applyFiltersAndSortInternal()
}

internal fun HomeViewModel.handleToggleLanguageFilter(language: String) {
    homeUiStateFlow.update { current ->
        val next = if (current.selectedLanguages.contains(language)) current.selectedLanguages - language else current.selectedLanguages + language
        current.copy(selectedLanguages = next)
    }
    applyFiltersAndSortInternal()
}

internal fun HomeViewModel.handleToggleYearFilter(year: String) {
    homeUiStateFlow.update { current ->
        val next = if (current.selectedYears.contains(year)) current.selectedYears - year else current.selectedYears + year
        current.copy(selectedYears = next)
    }
    applyFiltersAndSortInternal()
}

internal fun HomeViewModel.handleToggleCountryFilter(country: String) {
    homeUiStateFlow.update { current ->
        val next = if (current.selectedCountries.contains(country)) current.selectedCountries - country else current.selectedCountries + country
        current.copy(selectedCountries = next)
    }
    applyFiltersAndSortInternal()
}

internal fun HomeViewModel.handleToggleReadingStatus(status: ReadingStatus) {
    homeUiStateFlow.update { current ->
        val next = if (current.selectedStatuses.contains(status)) current.selectedStatuses - status else current.selectedStatuses + status
        current.copy(selectedStatuses = next)
    }
    applyFiltersAndSortInternal()
}

internal fun HomeViewModel.handleClearAdvancedFilters() {
    homeUiStateFlow.update {
        it.copy(
            selectedTypes = emptySet(),
            selectedGenres = emptySet(),
            selectedLanguages = emptySet(),
            selectedYears = emptySet(),
            selectedCountries = emptySet(),
            selectedStatuses = emptySet()
        )
    }
    applyFiltersAndSortInternal()
}

internal fun HomeViewModel.handleSortOrderChanged(order: SortOrder) =
    viewModelScope.launch { prefsStore.setSortOrder(order.name) }

internal fun HomeViewModel.handleShowBookTypeChanged(show: Boolean) =
    viewModelScope.launch { prefsStore.setShowBookType(show) }

internal fun HomeViewModel.handleShowRecentReadingChanged(show: Boolean) =
    viewModelScope.launch { prefsStore.setShowRecentReading(show) }

internal fun HomeViewModel.handleShowFavoritesChanged(show: Boolean) =
    viewModelScope.launch { prefsStore.setShowFavorites(show) }

internal fun HomeViewModel.handleShowGenresChanged(show: Boolean) =
    viewModelScope.launch { prefsStore.setShowGenres(show) }

internal fun HomeViewModel.handleHideStatusBarChanged(hide: Boolean) =
    viewModelScope.launch { prefsStore.setHideStatusBar(hide) }

internal fun HomeViewModel.handleGridColumnsChanged(columns: Int) =
    viewModelScope.launch { prefsStore.setGridColumns(columns) }

internal fun HomeViewModel.handleAnimationsToggle(enabled: Boolean) =
    viewModelScope.launch { prefsStore.setAnimationsEnabled(enabled) }

internal fun HomeViewModel.handleHapticsToggle(enabled: Boolean) =
    viewModelScope.launch { prefsStore.setHapticsEnabled(enabled) }

internal fun HomeViewModel.handleTextScrollerToggle(enabled: Boolean) =
    viewModelScope.launch { prefsStore.setTextScrollerEnabled(enabled) }

internal fun HomeViewModel.handleHideBetaFeaturesChanged(hidden: Boolean) =
    viewModelScope.launch { prefsStore.setHideBetaFeatures(hidden) }

internal fun HomeViewModel.handleDeveloperOptionsChanged(enabled: Boolean) =
    viewModelScope.launch { prefsStore.setDeveloperOptionsEnabled(enabled) }

internal fun HomeViewModel.handleAppTextScaleChange(scale: Float) =
    viewModelScope.launch { prefsStore.setAppTextScale(scale) }

internal fun HomeViewModel.handleToggleReaderSearch(show: Boolean) =
    viewModelScope.launch { prefsStore.setShowReaderSearch(show) }

internal fun HomeViewModel.handleToggleReaderListen(show: Boolean) =
    viewModelScope.launch { prefsStore.setShowReaderListen(show) }

internal fun HomeViewModel.handleToggleReaderAccessibility(show: Boolean) =
    viewModelScope.launch { prefsStore.setShowReaderAccessibility(show) }

internal fun HomeViewModel.handleToggleReaderAnalytics(show: Boolean) = viewModelScope.launch {
    prefsStore.setShowReaderAnalytics(show)
    analyticsRepo.enableAnalytics(show)
}

internal fun HomeViewModel.handleToggleReaderExport(show: Boolean) =
    viewModelScope.launch { prefsStore.setShowReaderExport(show) }

internal fun HomeViewModel.handleReaderControlOrderChanged(order: List<ReaderControl>) =
    viewModelScope.launch { prefsStore.setReaderControlOrder(order) }

internal fun HomeViewModel.handleReaderSettingsChanged(settings: ReaderSettings) =
    viewModelScope.launch { prefsStore.setReaderSettings(settings) }

internal fun HomeViewModel.handleToggleLayout() {
    homeUiStateFlow.update {
        val newLayout = if (it.display.layout == LibraryLayout.GRID) LibraryLayout.LIST else LibraryLayout.GRID
        it.copy(display = it.display.copy(layout = newLayout))
    }
}

internal fun HomeViewModel.handleToggleFavorite(bookId: String, isFavorite: Boolean) {
    viewModelScope.launch {
        libraryRepo.toggleFavorite(bookId, isFavorite)
    }
}

internal fun HomeViewModel.applyFiltersAndSortInternal() {
    val current = homeUiStateFlow.value
    val filteredBooks = filterBooks(current)
    val newIds = current.newDownloadIds
    val (newBooks, existingBooks) = if (newIds.isEmpty()) {
        emptyList<BookItem>() to filteredBooks
    } else {
        filteredBooks.partition { newIds.contains(it.id) }
    }
    val sortedBooks = sortBooks(newBooks, current.sortOrder) + sortBooks(existingBooks, current.sortOrder)
    val recent = recentBooks(current.allBooks)

    homeUiStateFlow.update { it.copy(visibleBooks = sortedBooks, recentBooks = recent) }
}
