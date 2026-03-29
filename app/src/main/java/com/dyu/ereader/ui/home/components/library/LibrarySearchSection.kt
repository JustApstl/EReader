package com.dyu.ereader.ui.home.components.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.ui.components.inputs.AppSearchBar
import com.dyu.ereader.ui.home.state.HomeUiState
import com.dyu.ereader.ui.home.state.ReadingStatus
import com.dyu.ereader.core.locale.displayLanguageName

@Composable
internal fun LibrarySearchSection(
    uiState: HomeUiState,
    liquidGlassEnabled: Boolean,
    searchVisible: Boolean,
    selectedTab: LibraryFeedTab,
    onTabSelected: (LibraryFeedTab) -> Unit,
    allCount: Int,
    recentCount: Int,
    favoritesCount: Int,
    collectionsCount: Int,
    hasRecent: Boolean,
    hasFavorites: Boolean,
    hasCollections: Boolean,
    searchFocusRequester: FocusRequester,
    onSearchChanged: (String) -> Unit,
    onToggleTypeFilter: (BookType) -> Unit,
    onToggleGenreFilter: (String) -> Unit,
    onToggleLanguageFilter: (String) -> Unit,
    onToggleYearFilter: (String) -> Unit,
    onToggleCountryFilter: (String) -> Unit,
    onToggleReadingStatus: (ReadingStatus) -> Unit
) {
    val hasSearchOrAdvancedFilters = uiState.hasLibrarySearchOrFilters()

    Surface(
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.98f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            if (searchVisible || uiState.searchQuery.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AppSearchBar(
                        query = uiState.searchQuery,
                        placeholder = "Search Library",
                        onQueryChange = onSearchChanged,
                        onSearch = { _ -> },
                        liquidGlassEnabled = liquidGlassEnabled,
                        focusRequester = searchFocusRequester,
                        autoFocus = searchVisible,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (!hasSearchOrAdvancedFilters) {
                Spacer(Modifier.height(12.dp))
                LibraryFeedTabs(
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    allCount = allCount,
                    recentCount = recentCount,
                    favoritesCount = favoritesCount,
                    collectionsCount = collectionsCount,
                    hasRecent = hasRecent,
                    hasFavorites = hasFavorites,
                    hasCollections = hasCollections
                )
            }

            if (hasSearchOrAdvancedFilters) {
                Spacer(Modifier.height(10.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(uiState.selectedGenres.toList()) { genre ->
                        ActiveLibraryFilterChip(
                            label = genre,
                            onClick = { onToggleGenreFilter(genre) }
                        )
                    }
                    items(uiState.selectedLanguages.toList()) { language ->
                        ActiveLibraryFilterChip(
                            label = displayLanguageName(language) ?: language,
                            onClick = { onToggleLanguageFilter(language) }
                        )
                    }
                    items(uiState.selectedYears.toList()) { year ->
                        ActiveLibraryFilterChip(
                            label = year,
                            onClick = { onToggleYearFilter(year) }
                        )
                    }
                    items(uiState.selectedCountries.toList()) { country ->
                        ActiveLibraryFilterChip(
                            label = country,
                            onClick = { onToggleCountryFilter(country) }
                        )
                    }
                    items(uiState.selectedTypes.toList()) { type ->
                        ActiveLibraryFilterChip(
                            label = type.label,
                            onClick = { onToggleTypeFilter(type) }
                        )
                    }
                    items(uiState.selectedStatuses.toList()) { status ->
                        ActiveLibraryFilterChip(
                            label = status.label,
                            onClick = { onToggleReadingStatus(status) }
                        )
                    }
                    if (uiState.searchQuery.isNotEmpty()) {
                        item {
                            ActiveLibraryFilterChip(
                                label = "Query: ${uiState.searchQuery}",
                                onClick = { onSearchChanged("") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveLibraryFilterChip(
    label: String,
    onClick: () -> Unit
) {
    FilterChip(
        selected = true,
        onClick = onClick,
        label = { Text(label) },
        trailingIcon = { Icon(Icons.Rounded.Close, null, Modifier.size(16.dp)) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}
