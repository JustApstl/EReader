package com.dyu.ereader.ui.home.components

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.app.AppTheme
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.ui.components.add.AddBookCard
import com.dyu.ereader.ui.components.add.AddBookListItem
import com.dyu.ereader.ui.components.cards.BookCard
import com.dyu.ereader.ui.components.cards.BookListItem
import com.dyu.ereader.ui.components.insets.stableStatusBarsPadding
import com.dyu.ereader.ui.home.components.library.ExploreGenresSection
import com.dyu.ereader.ui.home.components.library.LibraryFeedAvailability
import com.dyu.ereader.ui.home.components.library.LibraryFeedTab
import com.dyu.ereader.ui.home.components.library.LibrarySearchSection
import com.dyu.ereader.ui.home.components.library.LibraryVolumesToolbar
import com.dyu.ereader.ui.home.components.library.hasLibrarySearchOrFilters
import com.dyu.ereader.ui.home.components.library.isBrowsingLibraryRoot
import com.dyu.ereader.ui.home.components.library.resolveLibraryFeedTab
import com.dyu.ereader.ui.home.components.library.sortLibraryTabBooks
import com.dyu.ereader.ui.home.state.HomeUiState
import com.dyu.ereader.ui.home.state.LibraryLayout
import com.dyu.ereader.ui.home.state.ReadingStatus
import com.dyu.ereader.ui.home.state.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryContent(
    uiState: HomeUiState,
    appTheme: AppTheme,
    liquidGlassEnabled: Boolean,
    librarySearchVisible: Boolean,
    listState: LazyListState,
    treePickerLauncher: ActivityResultLauncher<Uri?>,
    onSearchChanged: (String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onOpenBook: (BookItem) -> Unit,
    onRefresh: () -> Unit,
    onToggleTypeFilter: (BookType) -> Unit,
    onToggleGenreFilter: (String) -> Unit,
    onToggleLanguageFilter: (String) -> Unit,
    onToggleYearFilter: (String) -> Unit,
    onToggleCountryFilter: (String) -> Unit,
    onToggleReadingStatus: (ReadingStatus) -> Unit,
    onClearAdvancedFilters: () -> Unit,
    onShowBookInfo: (BookItem) -> Unit,
    onDeleteBook: (BookItem) -> Unit,
    onShowBookActions: (BookItem) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onToggleLayout: () -> Unit,
    focusSearchRequestKey: Int = 0
) {
    val favoriteBooks = remember(uiState.allBooks) {
        uiState.allBooks.filter { it.isFavorite }
    }
    val sortedFavoriteBooks = remember(favoriteBooks, uiState.sortOrder) {
        sortLibraryTabBooks(favoriteBooks, uiState.sortOrder)
    }
    val sortedRecentBooks = remember(uiState.recentBooks, uiState.sortOrder) {
        sortLibraryTabBooks(uiState.recentBooks, uiState.sortOrder)
    }
    var activeFeedTab by rememberSaveable { mutableStateOf(LibraryFeedTab.ALL) }
    val searchFocusRequester = remember { FocusRequester() }
    val hasSearchOrAdvancedFilters = remember(uiState) { uiState.hasLibrarySearchOrFilters() }
    val isBrowsingLibraryRoot = remember(uiState) { uiState.isBrowsingLibraryRoot() }
    val feedAvailability = remember(sortedRecentBooks, sortedFavoriteBooks, uiState.collections) {
        LibraryFeedAvailability(
            hasRecent = sortedRecentBooks.isNotEmpty(),
            hasFavorites = sortedFavoriteBooks.isNotEmpty(),
            hasCollections = uiState.collections.isNotEmpty()
        )
    }
    val effectiveFeedTab = remember(activeFeedTab, hasSearchOrAdvancedFilters, feedAvailability) {
        resolveLibraryFeedTab(
            activeTab = activeFeedTab,
            hasSearchOrFilters = hasSearchOrAdvancedFilters,
            availability = feedAvailability
        )
    }
    val currentTabBooks = remember(effectiveFeedTab, uiState.visibleBooks, sortedRecentBooks, sortedFavoriteBooks) {
        when (effectiveFeedTab) {
            LibraryFeedTab.ALL -> uiState.visibleBooks
            LibraryFeedTab.RECENT -> sortedRecentBooks
            LibraryFeedTab.FAVORITES -> sortedFavoriteBooks
            LibraryFeedTab.COLLECTIONS -> emptyList()
        }
    }

    LaunchedEffect(focusSearchRequestKey, librarySearchVisible) {
        if (focusSearchRequestKey > 0 && librarySearchVisible) {
            searchFocusRequester.requestFocus()
        }
    }

    if (!uiState.hasLibraryAccess) {
        EmptyPermissionState { treePickerLauncher.launch(null) }
    } else {
        val refreshState = rememberPullToRefreshState()
        val haptic = LocalHapticFeedback.current
        val onRefreshAction = {
            if (uiState.display.hapticsEnabled) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            }
            onRefresh()
        }
        PullToRefreshBox(
            state = refreshState,
            isRefreshing = uiState.isScanning,
            onRefresh = onRefreshAction,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = refreshState,
                    isRefreshing = uiState.isScanning,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .stableStatusBarsPadding()
                        .padding(top = 4.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        ) {
            if (uiState.allBooks.isEmpty() && uiState.isScanning) {
                LibraryLoadingState()
            } else if (uiState.allBooks.isEmpty() && !uiState.isScanning) {
                EmptyLibraryState(onRefresh, { treePickerLauncher.launch(null) })
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    item {
                        LibrarySearchSection(
                            uiState = uiState,
                            liquidGlassEnabled = liquidGlassEnabled,
                            searchVisible = librarySearchVisible,
                            selectedTab = effectiveFeedTab,
                            onTabSelected = { activeFeedTab = it },
                            allCount = uiState.visibleBooks.size,
                            recentCount = sortedRecentBooks.size,
                            favoritesCount = sortedFavoriteBooks.size,
                            collectionsCount = uiState.collections.size,
                            hasRecent = feedAvailability.hasRecent,
                            hasFavorites = feedAvailability.hasFavorites,
                            hasCollections = feedAvailability.hasCollections,
                            searchFocusRequester = searchFocusRequester,
                            onSearchChanged = onSearchChanged,
                            onToggleTypeFilter = onToggleTypeFilter,
                            onToggleGenreFilter = onToggleGenreFilter,
                            onToggleLanguageFilter = onToggleLanguageFilter,
                            onToggleYearFilter = onToggleYearFilter,
                            onToggleCountryFilter = onToggleCountryFilter,
                            onToggleReadingStatus = onToggleReadingStatus
                        )
                    }

                    if (isBrowsingLibraryRoot &&
                        uiState.display.showRecentReading &&
                        uiState.recentBooks.isNotEmpty() &&
                        (effectiveFeedTab == LibraryFeedTab.ALL || effectiveFeedTab == LibraryFeedTab.RECENT)
                    ) {
                        item {
                            FeaturedReadingCard(
                                book = uiState.recentBooks.first(),
                                onClick = { onOpenBook(uiState.recentBooks.first()) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (effectiveFeedTab == LibraryFeedTab.RECENT &&
                        sortedRecentBooks.isEmpty() &&
                        !hasSearchOrAdvancedFilters
                    ) {
                        item {
                            Text(
                                text = "No recent books yet. Open a book and it will appear here.",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (effectiveFeedTab == LibraryFeedTab.FAVORITES &&
                        sortedFavoriteBooks.isEmpty() &&
                        !hasSearchOrAdvancedFilters
                    ) {
                        item {
                            Text(
                                text = "No favorites yet. Mark books as favorite and they will appear here.",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isBrowsingLibraryRoot &&
                        uiState.display.showGenres &&
                        uiState.availableGenres.isNotEmpty() &&
                        effectiveFeedTab == LibraryFeedTab.ALL
                    ) {
                        item {
                            ExploreGenresSection(
                                genres = uiState.availableGenres,
                                selectedGenres = uiState.selectedGenres,
                                onGenreClick = onToggleGenreFilter
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }

                    if (isBrowsingLibraryRoot &&
                        uiState.display.showFavorites &&
                        favoriteBooks.isNotEmpty() &&
                        effectiveFeedTab == LibraryFeedTab.ALL
                    ) {
                        item {
                            Text(
                                "Favorites",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 20.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(favoriteBooks, key = { it.id }) { book ->
                                    FavoriteBookCard(
                                        book = book,
                                        onClick = { onOpenBook(book) },
                                        onShowActions = onShowBookActions,
                                        modifier = Modifier.width(130.dp),
                                        liquidGlassEnabled = liquidGlassEnabled
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                        }
                    }

                    if (isBrowsingLibraryRoot &&
                        uiState.collections.isNotEmpty() &&
                        (effectiveFeedTab == LibraryFeedTab.ALL || effectiveFeedTab == LibraryFeedTab.COLLECTIONS)
                    ) {
                        items(uiState.collections, key = { it.name }) { shelf ->
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = shelf.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${shelf.books.size} book${if (shelf.books.size == 1) "" else "s"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Surface(
                                        onClick = { onDeleteCollection(shelf.name) },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.DeleteOutline,
                                                contentDescription = "Delete collection",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(shelf.books, key = { it.id }) { book ->
                                        FavoriteBookCard(
                                            book = book,
                                            onClick = { onOpenBook(book) },
                                            onShowActions = onShowBookActions,
                                            modifier = Modifier.width(130.dp),
                                            liquidGlassEnabled = liquidGlassEnabled
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }

                    if (effectiveFeedTab != LibraryFeedTab.COLLECTIONS) {
                        item {
                            LibraryVolumesToolbar(
                                title = when (effectiveFeedTab) {
                                    LibraryFeedTab.ALL -> "All Volumes"
                                    LibraryFeedTab.RECENT -> "Recent"
                                    LibraryFeedTab.FAVORITES -> "Favorites"
                                    LibraryFeedTab.COLLECTIONS -> "Collections"
                                },
                                layout = uiState.display.layout,
                                sortOrder = uiState.sortOrder,
                                onToggleLayout = onToggleLayout,
                                onSortOrderChanged = onSortOrderChanged
                            )
                        }
                    }

                    if (effectiveFeedTab != LibraryFeedTab.COLLECTIONS && uiState.display.layout == LibraryLayout.GRID) {
                        item {
                            val columns = uiState.display.gridColumns
                            val books = currentTabBooks
                            val showAddButton = effectiveFeedTab == LibraryFeedTab.ALL && isBrowsingLibraryRoot

                            val allItems = if (showAddButton) books.map { it as Any? } + listOf(null) else books.map { it as Any? }

                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                allItems.chunked(columns).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        row.forEach { item ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                if (item == null) {
                                                    AddBookCard(appTheme, liquidGlassEnabled) { treePickerLauncher.launch(null) }
                                                } else {
                                                    val book = item as BookItem
                                                    BookCard(
                                                        book = book,
                                                        onClick = { onOpenBook(book) },
                                                        onToggleFavorite = onToggleFavorite,
                                                        onShowActions = onShowBookActions,
                                                        onShowInfo = onShowBookInfo,
                                                        onDelete = onDeleteBook,
                                                        showBookType = uiState.display.showBookType,
                                                        showFavoriteButton = uiState.display.showFavorites,
                                                        showProgress = false,
                                                        isNew = uiState.newDownloadIds.contains(book.id),
                                                        gridColumns = columns,
                                                        appTheme = appTheme,
                                                        liquidGlassEnabled = liquidGlassEnabled,
                                                        textScrollerEnabled = uiState.display.textScrollerEnabled
                                                    )
                                                }
                                            }
                                        }
                                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                                    }
                                }
                            }
                        }
                    } else if (effectiveFeedTab != LibraryFeedTab.COLLECTIONS) {
                        items(currentTabBooks, key = { it.id }) { book ->
                            BookListItem(
                                book = book,
                                onClick = { onOpenBook(book) },
                                onToggleFavorite = onToggleFavorite,
                                onShowActions = onShowBookActions,
                                onShowInfo = onShowBookInfo,
                                onDelete = onDeleteBook,
                                showBookType = uiState.display.showBookType,
                                showFavoriteButton = uiState.display.showFavorites,
                                showProgress = false,
                                isNew = uiState.newDownloadIds.contains(book.id),
                                appTheme = appTheme,
                                liquidGlassEnabled = liquidGlassEnabled,
                                textScrollerEnabled = uiState.display.textScrollerEnabled,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            )
                        }
                        if (effectiveFeedTab == LibraryFeedTab.ALL &&
                            isBrowsingLibraryRoot
                        ) {
                            item {
                                AddBookListItem(
                                    appTheme = appTheme,
                                    liquidGlassEnabled = liquidGlassEnabled,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                                ) { treePickerLauncher.launch(null) }
                            }
                        }
                    }

                    if (effectiveFeedTab == LibraryFeedTab.COLLECTIONS &&
                        uiState.collections.isEmpty() &&
                        !hasSearchOrAdvancedFilters
                    ) {
                        item {
                            Text(
                                text = "No collections yet. Create one from a book actions menu.",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
