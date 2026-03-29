package com.dyu.ereader.ui.home.components

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.app.AppTheme
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.add.AddBookCard
import com.dyu.ereader.ui.components.add.AddBookListItem
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.cards.BookCard
import com.dyu.ereader.ui.components.cards.BookListItem
import com.dyu.ereader.ui.components.insets.stableStatusBarsPadding
import com.dyu.ereader.ui.components.refresh.EReaderPullRefreshIndicator
import com.dyu.ereader.ui.components.surfaces.SectionSurface
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

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    } else if (uiState.allBooks.isEmpty() && uiState.isScanning) {
        LibraryLoadingState()
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
                EReaderPullRefreshIndicator(
                    state = refreshState,
                    isRefreshing = uiState.isScanning,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .stableStatusBarsPadding()
                        .padding(top = 6.dp)
                )
            }
        ) {
            if (uiState.allBooks.isEmpty() && !uiState.isScanning) {
                EmptyLibraryState(onRefresh, { treePickerLauncher.launch(null) })
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    stickyHeader {
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
                            SectionSurface(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "No recent books yet. Open a book and it will appear here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (effectiveFeedTab == LibraryFeedTab.FAVORITES &&
                        sortedFavoriteBooks.isEmpty() &&
                        !hasSearchOrAdvancedFilters
                    ) {
                        item {
                            SectionSurface(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "No favorites yet. Mark books as favorite and they will appear here.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
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
                            LibrarySectionHeader(
                                title = "Favorites Shelf",
                                subtitle = "Quick picks you saved for later.",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
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
                            SectionSurface(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = shelf.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${shelf.books.size} book${if (shelf.books.size == 1) "" else "s"} in this collection",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    AppChromeIconButton(
                                        icon = Icons.Rounded.DeleteOutline,
                                        contentDescription = "Delete collection",
                                        onClick = { onDeleteCollection(shelf.name) },
                                        destructive = true,
                                        liquidGlassEnabled = liquidGlassEnabled,
                                        size = 38.dp,
                                        iconSize = 18.dp
                                    )
                                }
                                Spacer(Modifier.height(14.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(shelf.books, key = { it.id }) { book ->
                                        FavoriteBookCard(
                                            book = book,
                                            onClick = { onOpenBook(book) },
                                            onShowActions = onShowBookActions,
                                            modifier = Modifier.width(132.dp),
                                            liquidGlassEnabled = liquidGlassEnabled
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (effectiveFeedTab != LibraryFeedTab.COLLECTIONS) {
                        item {
                            LibraryVolumesToolbar(
                                title = when (effectiveFeedTab) {
                                    LibraryFeedTab.ALL -> "Your Library"
                                    LibraryFeedTab.RECENT -> "Recently Opened"
                                    LibraryFeedTab.FAVORITES -> "Favorited Books"
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
                            SectionSurface(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "No collections yet. Create one from a book actions menu.",
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
}

@Composable
private fun LibraryOverviewPanel(
    selectedTab: LibraryFeedTab,
    totalBooks: Int,
    visibleBooks: Int,
    recentCount: Int,
    favoritesCount: Int,
    collectionsCount: Int,
    searchActive: Boolean,
    onImportClick: () -> Unit,
    featuredBook: BookItem?,
    onOpenFeatured: (BookItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val headline = when {
        searchActive -> "Filtered view"
        selectedTab == LibraryFeedTab.RECENT -> "Pick up where you left off"
        selectedTab == LibraryFeedTab.FAVORITES -> "Your comfort shelf"
        selectedTab == LibraryFeedTab.COLLECTIONS -> "Curated by you"
        else -> "Built for long reading sessions"
    }
    val supporting = when {
        searchActive -> "$visibleBooks books match your current search and filters."
        selectedTab == LibraryFeedTab.RECENT -> "$recentCount books are ready to resume."
        selectedTab == LibraryFeedTab.FAVORITES -> "$favoritesCount favorites are one tap away."
        selectedTab == LibraryFeedTab.COLLECTIONS -> "$collectionsCount collections keep your library tidy."
        else -> "$totalBooks books, $favoritesCount favorites, and $collectionsCount personal collections."
    }

    SectionSurface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = supporting,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FilledTonalButton(
                    onClick = onImportClick,
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Import", fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                LibraryMetricChip("Books", totalBooks.toString(), Modifier.weight(1f))
                LibraryMetricChip("Visible", visibleBooks.toString(), Modifier.weight(1f))
                LibraryMetricChip("Favorites", favoritesCount.toString(), Modifier.weight(1f))
                LibraryMetricChip("Collections", collectionsCount.toString(), Modifier.weight(1f))
            }

            featuredBook?.let { book ->
                Surface(
                    onClick = { onOpenFeatured(book) },
                    shape = UiTokens.SmallCardShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(8.dp).size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Resume ${book.title}",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = book.author,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = UiTokens.TinyCardShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LibrarySectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
