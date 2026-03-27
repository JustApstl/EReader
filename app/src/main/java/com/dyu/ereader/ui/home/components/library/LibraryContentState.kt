package com.dyu.ereader.ui.home.components.library

import com.dyu.ereader.ui.home.state.HomeUiState

internal enum class LibraryFeedTab(val label: String) {
    ALL("All"),
    RECENT("Recent"),
    FAVORITES("Favorites"),
    COLLECTIONS("Collections")
}

internal data class LibraryFeedAvailability(
    val hasRecent: Boolean,
    val hasFavorites: Boolean,
    val hasCollections: Boolean
)

internal fun HomeUiState.hasLibrarySearchOrFilters(): Boolean {
    return searchQuery.isNotEmpty() ||
        selectedGenres.isNotEmpty() ||
        selectedTypes.isNotEmpty() ||
        selectedLanguages.isNotEmpty() ||
        selectedYears.isNotEmpty() ||
        selectedCountries.isNotEmpty() ||
        selectedStatuses.isNotEmpty()
}

internal fun HomeUiState.isBrowsingLibraryRoot(): Boolean {
    return searchQuery.isEmpty() &&
        selectedGenres.isEmpty() &&
        selectedTypes.isEmpty() &&
        selectedLanguages.isEmpty() &&
        selectedYears.isEmpty() &&
        selectedCountries.isEmpty() &&
        selectedStatuses.isEmpty()
}

internal fun resolveLibraryFeedTab(
    activeTab: LibraryFeedTab,
    hasSearchOrFilters: Boolean,
    availability: LibraryFeedAvailability
): LibraryFeedTab {
    if (hasSearchOrFilters) return LibraryFeedTab.ALL
    return when (activeTab) {
        LibraryFeedTab.ALL -> LibraryFeedTab.ALL
        LibraryFeedTab.RECENT -> if (availability.hasRecent) LibraryFeedTab.RECENT else LibraryFeedTab.ALL
        LibraryFeedTab.FAVORITES -> if (availability.hasFavorites) LibraryFeedTab.FAVORITES else LibraryFeedTab.ALL
        LibraryFeedTab.COLLECTIONS -> if (availability.hasCollections) LibraryFeedTab.COLLECTIONS else LibraryFeedTab.ALL
    }
}
