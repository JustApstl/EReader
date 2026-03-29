package com.dyu.ereader.ui.home.components.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.Sort
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
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.surfaces.SectionSurface
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
    val currentSortLabel = sortOrder.label

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
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolbarActionChip(
                label = if (layout == LibraryLayout.GRID) "Grid" else "List",
                icon = nextLayoutIcon,
                contentDescription = nextLayoutLabel,
                containerColor = activeIconContainer,
                onClick = onToggleLayout
            )
            Box {
                ToolbarActionChip(
                    label = currentSortLabel,
                    icon = Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = "Sort books. Current sort: $currentSortLabel",
                    containerColor = activeIconContainer,
                    onClick = { showSortMenu = true }
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
private fun ToolbarActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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
        listOfNotNull(
            LibraryFeedTabUiState(LibraryFeedTab.ALL, allCount),
            LibraryFeedTabUiState(LibraryFeedTab.RECENT, recentCount).takeIf { hasRecent },
            LibraryFeedTabUiState(LibraryFeedTab.FAVORITES, favoritesCount).takeIf { hasFavorites },
            LibraryFeedTabUiState(LibraryFeedTab.COLLECTIONS, collectionsCount).takeIf { hasCollections }
        )
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
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.84f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selected) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.36f)
                    }
                ),
                shadowElevation = UiTokens.SectionShadowElevation
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = tabState.tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        modifier = Modifier.padding(start = 8.dp),
                        shape = CircleShape,
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        }
                    ) {
                        Text(
                            text = tabState.count.toString(),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
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
