package com.dyu.ereader.ui.home.screens

import com.dyu.ereader.ui.home.state.HomeUiState

internal enum class HomeRootTab {
    LIBRARY,
    BROWSE,
    SETTINGS,
    CLOUD,
    LOGS
}

internal fun HomeRootTab.backDestination(): HomeRootTab {
    return when (this) {
        HomeRootTab.CLOUD, HomeRootTab.LOGS -> HomeRootTab.SETTINGS
        HomeRootTab.BROWSE, HomeRootTab.SETTINGS, HomeRootTab.LIBRARY -> HomeRootTab.LIBRARY
    }
}

internal fun HomeUiState.hasActiveLibraryFilters(): Boolean {
    return searchQuery.isNotEmpty() ||
        selectedGenres.isNotEmpty() ||
        selectedTypes.isNotEmpty() ||
        selectedLanguages.isNotEmpty() ||
        selectedYears.isNotEmpty() ||
        selectedCountries.isNotEmpty() ||
        selectedStatuses.isNotEmpty()
}
