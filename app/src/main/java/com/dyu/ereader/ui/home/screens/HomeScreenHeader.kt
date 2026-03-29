package com.dyu.ereader.ui.home.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.inputs.AppSearchBar
import com.dyu.ereader.ui.home.state.HomeUiState

@Composable
internal fun HomeScreenHeader(
    currentTab: Int,
    uiState: HomeUiState,
    onFocusSearch: () -> Unit,
    librarySearchVisible: Boolean,
    onLibrarySearchVisibilityChange: (Boolean) -> Unit,
    onLibrarySearchClear: () -> Unit,
    browseSearchQuery: String,
    browseSearchVisible: Boolean,
    onBrowseSearchVisibilityChange: (Boolean) -> Unit,
    onBrowseSearchChanged: (String) -> Unit,
    onBrowseSearch: (String) -> Unit,
    settingsSearchQuery: String,
    settingsSearchVisible: Boolean,
    onSettingsSearchVisibilityChange: (Boolean) -> Unit,
    onSettingsSearchChanged: (String) -> Unit,
    onShowFilter: () -> Unit,
    hasActiveFilters: Boolean,
    onClearLogs: () -> Unit,
    liquidGlassEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        val title = when (currentTab) {
            0 -> "Library"
            1 -> "Browse"
            2 -> "Settings"
            3 -> "Cloud Backup"
            else -> "System Logs"
        }
        val subtitle = when (currentTab) {
            0 -> "Your books, organized the One UI way."
            1 -> "Discover new titles and curated catalogs."
            2 -> "Tune the app to your reading style."
            3 -> "Protect your reading life across devices."
            else -> "Recent activity and troubleshooting details."
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f),
            tonalElevation = 0.dp,
            shadowElevation = 8.dp
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentTab == 0) {
                            AppChromeIconButton(
                                icon = Icons.Rounded.FilterAlt,
                                contentDescription = "Filter",
                                onClick = onShowFilter,
                                selected = hasActiveFilters,
                                liquidGlassEnabled = liquidGlassEnabled
                            )
                            AppChromeIconButton(
                                icon = if (librarySearchVisible || uiState.searchQuery.isNotBlank()) {
                                    Icons.Rounded.Close
                                } else {
                                    Icons.Rounded.Search
                                },
                                contentDescription = if (librarySearchVisible || uiState.searchQuery.isNotBlank()) {
                                    "Close library search"
                                } else {
                                    "Search library"
                                },
                                onClick = {
                                    if (librarySearchVisible || uiState.searchQuery.isNotBlank()) {
                                        onLibrarySearchClear()
                                        onLibrarySearchVisibilityChange(false)
                                    } else {
                                        onLibrarySearchVisibilityChange(true)
                                        onFocusSearch()
                                    }
                                },
                                selected = librarySearchVisible || uiState.searchQuery.isNotBlank(),
                                liquidGlassEnabled = liquidGlassEnabled
                            )
                        } else if (currentTab == 1) {
                            AppChromeIconButton(
                                icon = if (browseSearchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = if (browseSearchVisible) "Close browse search" else "Search browse",
                                onClick = {
                                    if (browseSearchVisible) {
                                        onBrowseSearchChanged("")
                                    }
                                    onBrowseSearchVisibilityChange(!browseSearchVisible)
                                },
                                selected = browseSearchVisible || browseSearchQuery.isNotBlank(),
                                liquidGlassEnabled = liquidGlassEnabled
                            )
                        } else if (currentTab == 4) {
                            AppChromeIconButton(
                                icon = Icons.Rounded.Delete,
                                contentDescription = "Clear Logs",
                                onClick = onClearLogs,
                                destructive = true,
                                liquidGlassEnabled = liquidGlassEnabled
                            )
                        } else if (currentTab == 2) {
                            AppChromeIconButton(
                                icon = if (settingsSearchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
                                contentDescription = if (settingsSearchVisible) "Close settings search" else "Search settings",
                                onClick = {
                                    if (settingsSearchVisible) {
                                        onSettingsSearchChanged("")
                                    }
                                    onSettingsSearchVisibilityChange(!settingsSearchVisible)
                                },
                                selected = settingsSearchVisible || settingsSearchQuery.isNotBlank(),
                                liquidGlassEnabled = liquidGlassEnabled
                            )
                        }
                    }
                }
            }
        }

        if (currentTab == 1 && browseSearchVisible) {
            Spacer(Modifier.height(12.dp))
            AppSearchBar(
                query = browseSearchQuery,
                placeholder = "Search Browse",
                onQueryChange = onBrowseSearchChanged,
                onSearch = onBrowseSearch,
                liquidGlassEnabled = liquidGlassEnabled,
                autoFocus = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (currentTab == 2 && settingsSearchVisible) {
            Spacer(Modifier.height(12.dp))
            AppSearchBar(
                query = settingsSearchQuery,
                placeholder = "Search Settings",
                onQueryChange = onSettingsSearchChanged,
                onSearch = { _ -> },
                liquidGlassEnabled = liquidGlassEnabled,
                autoFocus = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
