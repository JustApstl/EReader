package com.dyu.ereader.ui.home.components.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.ui.home.state.LibraryLayout
import com.dyu.ereader.ui.home.state.SortOrder

@Composable
internal fun LibraryVolumesToolbar(
    title: String,
    layout: LibraryLayout,
    sortOrder: SortOrder,
    onToggleLayout: () -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    val isLightSurface = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val activeIconContainer = MaterialTheme.colorScheme.primary.copy(alpha = if (isLightSurface) 0.16f else 0.28f)
    val nextLayoutIcon = if (layout == LibraryLayout.GRID) {
        Icons.AutoMirrored.Rounded.List
    } else {
        Icons.Rounded.GridView
    }
    val nextLayoutLabel = if (layout == LibraryLayout.GRID) {
        "Switch to list layout"
    } else {
        "Switch to grid layout"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Surface(
            onClick = onToggleLayout,
            shape = RoundedCornerShape(8.dp),
            color = activeIconContainer
        ) {
            Icon(
                imageVector = nextLayoutIcon,
                contentDescription = nextLayoutLabel,
                modifier = Modifier.padding(8.dp).size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Box {
            Surface(
                onClick = { showSortMenu = true },
                shape = RoundedCornerShape(10.dp),
                color = Color.Transparent
            ) {
                Text(
                    text = "SORT: ${shortSortLabel(sortOrder)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                SortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(order.label) },
                        onClick = {
                            showSortMenu = false
                            onSortOrderChanged(order)
                        },
                        trailingIcon = {
                            if (order == sortOrder) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun LibraryFeedTabs(
    selectedTab: LibraryFeedTab,
    onTabSelected: (LibraryFeedTab) -> Unit,
    allCount: Int,
    recentCount: Int,
    favoritesCount: Int,
    collectionsCount: Int,
    hasRecent: Boolean,
    hasFavorites: Boolean,
    hasCollections: Boolean
) {
    val tabs = remember(allCount, recentCount, favoritesCount, collectionsCount, hasRecent, hasFavorites, hasCollections) {
        buildList {
            add(LibraryFeedTabUiState(LibraryFeedTab.ALL, allCount))
            if (hasRecent) add(LibraryFeedTabUiState(LibraryFeedTab.RECENT, recentCount))
            if (hasFavorites) add(LibraryFeedTabUiState(LibraryFeedTab.FAVORITES, favoritesCount))
            if (hasCollections) add(LibraryFeedTabUiState(LibraryFeedTab.COLLECTIONS, collectionsCount))
        }
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        tabs.forEach { tabState ->
            val selected = tabState.tab == selectedTab
            Surface(
                onClick = { onTabSelected(tabState.tab) },
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp),
                shape = RoundedCornerShape(20.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                border = if (selected) null else androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tabState.tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tabState.count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Immutable
private data class LibraryFeedTabUiState(
    val tab: LibraryFeedTab,
    val count: Int
)

internal fun sortLibraryTabBooks(books: List<BookItem>, sortOrder: SortOrder): List<BookItem> {
    return when (sortOrder) {
        SortOrder.TITLE -> books.sortedBy { it.title.lowercase() }
        SortOrder.AUTHOR -> books.sortedBy { it.author.lowercase() }
        SortOrder.DATE_ADDED -> books.sortedByDescending { it.dateAdded }
        SortOrder.LAST_OPENED -> books.sortedByDescending { it.lastOpened }
        SortOrder.PROGRESS -> books.sortedByDescending { it.progress }
        SortOrder.FILE_SIZE -> books.sortedByDescending { it.fileSize }
    }
}

private fun shortSortLabel(sortOrder: SortOrder): String = when (sortOrder) {
    SortOrder.TITLE -> "A-Z"
    SortOrder.AUTHOR -> "AUTHOR"
    SortOrder.DATE_ADDED -> "NEWEST"
    SortOrder.LAST_OPENED -> "RECENT"
    SortOrder.PROGRESS -> "PROGRESS"
    SortOrder.FILE_SIZE -> "SIZE"
}
