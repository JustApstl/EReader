package com.dyu.ereader.ui.home.state

import com.dyu.ereader.data.model.library.BookItem

internal fun filterBooks(state: HomeUiState): List<BookItem> {
    val query = state.searchQuery.lowercase()
    var filtered = state.allBooks

    if (query.isNotBlank()) {
        filtered = filtered.filter { book ->
            when (state.searchFilter) {
                BookFilter.ALL -> book.matchesQuery(query)
                BookFilter.TITLE -> book.title.lowercase().contains(query)
                BookFilter.AUTHOR -> book.author.lowercase().contains(query)
                BookFilter.LANGUAGE -> book.language?.lowercase()?.contains(query) == true
                BookFilter.YEAR -> book.year?.contains(query) == true
                BookFilter.EXTENSION -> book.fileName.lowercase().contains(query)
            }
        }
    }

    if (state.selectedTypes.isNotEmpty()) {
        filtered = filtered.filter { state.selectedTypes.contains(it.type) }
    }

    if (state.selectedGenres.isNotEmpty()) {
        filtered = filtered.filter { book ->
            book.genres.any { state.selectedGenres.contains(it) }
        }
    }

    if (state.selectedLanguages.isNotEmpty()) {
        filtered = filtered.filter { book ->
            book.language?.let { state.selectedLanguages.contains(it) } == true
        }
    }

    if (state.selectedYears.isNotEmpty()) {
        filtered = filtered.filter { book ->
            book.year?.let { state.selectedYears.contains(it) } == true
        }
    }

    if (state.selectedCountries.isNotEmpty()) {
        filtered = filtered.filter { book ->
            book.countries.any { state.selectedCountries.contains(it) }
        }
    }

    if (state.selectedStatuses.isNotEmpty()) {
        filtered = filtered.filter { book ->
            state.selectedStatuses.any { status ->
                when (status) {
                    ReadingStatus.UNREAD -> book.progress <= 0f
                    ReadingStatus.IN_PROGRESS -> book.progress > 0f && book.progress < 0.98f
                    ReadingStatus.FINISHED -> book.progress >= 0.98f
                }
            }
        }
    }

    return filtered
}

internal fun sortBooks(books: List<BookItem>, sortOrder: SortOrder): List<BookItem> {
    return when (sortOrder) {
        SortOrder.TITLE -> books.sortedBy { it.title }
        SortOrder.AUTHOR -> books.sortedBy { it.author }
        SortOrder.DATE_ADDED -> books.sortedByDescending { it.dateAdded }
        SortOrder.LAST_OPENED -> books.sortedByDescending { it.lastOpened }
        SortOrder.PROGRESS -> books.sortedByDescending { it.progress }
        SortOrder.FILE_SIZE -> books.sortedByDescending { it.fileSize }
    }
}

internal fun recentBooks(books: List<BookItem>): List<BookItem> {
    return books
        .filter { it.lastOpened > 0 }
        .sortedWith(
            compareByDescending<BookItem> { it.progress in 0.01f..0.97f }
                .thenByDescending { it.lastOpened }
                .thenByDescending { it.progress }
                .thenByDescending { it.dateAdded }
        )
}

private fun BookItem.matchesQuery(query: String): Boolean {
    return title.lowercase().contains(query) ||
        author.lowercase().contains(query) ||
        language?.lowercase()?.contains(query) == true ||
        year?.contains(query) == true ||
        fileName.lowercase().contains(query) ||
        countries.any { it.lowercase().contains(query) }
}
