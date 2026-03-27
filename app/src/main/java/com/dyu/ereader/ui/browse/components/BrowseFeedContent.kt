package com.dyu.ereader.ui.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.browse.BrowseBook
import com.dyu.ereader.data.model.browse.BrowseFeed
import com.dyu.ereader.data.model.browse.BrowseNavigationGroup
import com.dyu.ereader.core.locale.extractPublishedYear
import com.dyu.ereader.ui.components.menus.AppDropdownMenu
import com.dyu.ereader.ui.components.menus.AppDropdownMenuItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune

private enum class BrowseSection {
    NEW,
    POPULAR,
    RECENT,
    ALL
}

private enum class SearchScope {
    ALL,
    TITLE,
    AUTHOR,
    SUBJECT
}

private data class BrowseShelf(
    val title: String,
    val books: List<BrowseBook>,
    val badge: String? = null,
    val cardWidth: Int = 148
)

private enum class BrowseSourceFlavor {
    GENERIC,
    GUTENBERG,
    ANARCHIST_LIBRARY,
    WIKISOURCE,
    STANDARD_EBOOKS
}

@Composable
internal fun FeedContent(
    feed: BrowseFeed,
    sourceTitle: String?,
    liquidGlassEnabled: Boolean,
    textScrollerEnabled: Boolean,
    savedSearches: List<String>,
    lastVisitedTimestamp: Long?,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onBookClick: (BrowseBook) -> Unit,
    onSearch: (String) -> Unit,
    searchKey: String,
    navigationGroups: List<BrowseNavigationGroup>,
    hasNextPage: Boolean,
    isLoadingNextPage: Boolean,
    onLoadNextPage: () -> Unit,
    onNavigateGroup: (BrowseNavigationGroup) -> Unit
) {
    val sourceFlavor = remember(sourceTitle, feed.id, feed.title) {
        detectSourceFlavor(sourceTitle = sourceTitle, feedId = feed.id, feedTitle = feed.title)
    }
    val pullState = rememberPullToRefreshState()
    val defaultSection = BrowseSection.ALL
    var selectedSection by remember(feed.id) { mutableStateOf(defaultSection) }
    val sections = remember {
        listOf(
            BrowseSection.ALL,
            BrowseSection.POPULAR,
            BrowseSection.NEW,
            BrowseSection.RECENT
        )
    }
    val sectionEntries = remember(feed.entries, selectedSection) {
        when (selectedSection) {
            BrowseSection.NEW -> feed.entries
            BrowseSection.POPULAR -> feed.entries
            BrowseSection.RECENT -> feed.entries
            BrowseSection.ALL -> feed.entries
        }
    }
    val sortedEntries = remember(sectionEntries, selectedSection) {
        sortEntries(sectionEntries, selectedSection)
    }
    var searchScope by rememberSaveable(searchKey) { mutableStateOf(SearchScope.ALL) }
    var activeQuery by rememberSaveable(searchKey) { mutableStateOf("") }
    val shelves = remember(sortedEntries, lastVisitedTimestamp, sourceFlavor) {
        buildShelves(sortedEntries, lastVisitedTimestamp, sourceFlavor)
    }
    val filteredEntries = remember(sortedEntries, activeQuery, searchScope) {
        if (activeQuery.isBlank()) {
            sortedEntries
        } else {
            sortedEntries.filter { matchesScope(it, activeQuery, searchScope) }
        }
    }
    val filteredShelves = remember(filteredEntries, lastVisitedTimestamp, sourceFlavor) {
        buildShelves(filteredEntries, lastVisitedTimestamp, sourceFlavor)
    }
    val usedIds = remember(filteredShelves) { filteredShelves.flatMap { shelf -> shelf.books.map { it.id } }.toSet() }
    val remainder = remember(filteredEntries, usedIds) { filteredEntries.filterNot { usedIds.contains(it.id) } }
    val remainderRows = remember(remainder) { remainder.chunked(2) }
    var searchQuery by rememberSaveable(searchKey) { mutableStateOf("") }
    var showSectionsMenu by remember(feed.id) { mutableStateOf(false) }
    var showScopeMenu by remember(searchKey) { mutableStateOf(false) }
    var showSavedSearchMenu by remember(searchKey) { mutableStateOf(false) }

    val listState = rememberLazyListState()
    var lastRequestCount by remember { mutableIntStateOf(-1) }
    var lastRequestTime by remember { mutableLongStateOf(0L) }
    LaunchedEffect(listState, hasNextPage, isLoadingNextPage) {
        snapshotFlow { listState.layoutInfo }
            .collect { layoutInfo ->
                val totalItems = layoutInfo.totalItemsCount
                val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                if (hasNextPage && !isLoadingNextPage && totalItems > 0 && lastVisible >= totalItems - 4 && totalItems != lastRequestCount) {
                    lastRequestCount = totalItems
                    onLoadNextPage()
                }
            }
    }
    LaunchedEffect(listState, hasNextPage, isLoadingNextPage) {
        snapshotFlow { listState.canScrollForward }
            .collect { canScrollForward ->
                if (!canScrollForward && hasNextPage && !isLoadingNextPage) {
                    val now = System.currentTimeMillis()
                    if (now - lastRequestTime > 900) {
                        lastRequestTime = now
                        onLoadNextPage()
                    }
                }
            }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = pullState,
        onRefresh = onRefresh,
        indicator = {
            PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = isRefreshing,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 100.dp, top = 0.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrowseSearchBar(
                        query = searchQuery,
                        placeholder = "Search Browse",
                        onQueryChange = {
                            searchQuery = it
                            if (it.isBlank()) {
                                activeQuery = ""
                            }
                        },
                        onSearch = { query ->
                            val trimmed = query.trim()
                            if (trimmed.isNotBlank()) {
                                activeQuery = trimmed
                                onSearch(trimmed)
                            }
                        },
                        liquidGlassEnabled = liquidGlassEnabled
                    )
                    CatalogContextHeader(
                        sourceTitle = sourceTitle,
                        feedTitle = feed.title,
                        activeQuery = activeQuery,
                        resultCount = filteredEntries.size
                    )
                    CatalogActionRow(
                        currentScope = searchScope,
                        navigationGroups = navigationGroups,
                        savedSearches = savedSearches,
                        onScopeClick = { showScopeMenu = true },
                        onSectionsClick = { showSectionsMenu = true },
                        onSavedSearchClick = { showSavedSearchMenu = true }
                    )
                    BrowseSegmentedTabs(
                        tabs = sections.map { section ->
                            browseSectionLabel(section)
                        },
                        selectedIndex = sections.indexOf(selectedSection),
                        onSelect = { index ->
                            selectedSection = sections[index]
                        }
                    )
                    if (navigationGroups.isNotEmpty()) {
                        AppDropdownMenu(
                            expanded = showSectionsMenu,
                            onDismissRequest = { showSectionsMenu = false },
                            title = "Sections"
                        ) {
                            navigationGroups.forEach { group ->
                                AppDropdownMenuItem(
                                    label = group.title.ifBlank { "Section" },
                                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                                    enableMarquee = true,
                                    onClick = {
                                        showSectionsMenu = false
                                        onNavigateGroup(group)
                                    }
                                )
                            }
                        }
                    }
                    AppDropdownMenu(
                        expanded = showScopeMenu,
                        onDismissRequest = { showScopeMenu = false },
                        title = "Search In"
                    ) {
                        listOf(
                            SearchScope.ALL to "Everything",
                            SearchScope.TITLE to "Titles",
                            SearchScope.AUTHOR to "Authors",
                            SearchScope.SUBJECT to "Topics"
                        ).forEach { (scope, label) ->
                            AppDropdownMenuItem(
                                label = label,
                                icon = Icons.Rounded.Search,
                                badgeText = if (scope == searchScope) "Current" else null,
                                onClick = {
                                    showScopeMenu = false
                                    searchScope = scope
                                }
                            )
                        }
                    }
                    if (savedSearches.isNotEmpty()) {
                        AppDropdownMenu(
                            expanded = showSavedSearchMenu,
                            onDismissRequest = { showSavedSearchMenu = false },
                            title = "Recent Searches"
                        ) {
                            savedSearches.forEach { query ->
                                AppDropdownMenuItem(
                                    label = query,
                                    icon = Icons.Rounded.History,
                                    enableMarquee = true,
                                    onClick = {
                                        showSavedSearchMenu = false
                                        searchQuery = query
                                        activeQuery = query
                                        onSearch(query)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            filteredShelves.forEach { shelf ->
                if (shelf.books.isNotEmpty()) {
                    item(key = "shelf_${shelf.title}") {
                        ShelfHeader(title = shelf.title, badge = shelf.badge)
                    }
                    item(key = "shelf_row_${shelf.title}") {
                        BrowseShelfRow(
                            books = shelf.books,
                            liquidGlassEnabled = liquidGlassEnabled,
                            textScrollerEnabled = textScrollerEnabled,
                            onBookClick = onBookClick,
                            cardWidth = shelf.cardWidth.dp
                        )
                    }
                }
            }

            if (remainderRows.isNotEmpty()) {
                item {
                    ShelfHeader(title = "Browse All")
                }
            }
            items(remainderRows) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { book ->
                        BrowseBookCard(
                            book = book,
                            liquidGlassEnabled = liquidGlassEnabled,
                            textScrollerEnabled = textScrollerEnabled,
                            onClick = { onBookClick(book) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }

            if (hasNextPage || isLoadingNextPage) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoadingNextPage) {
                            androidx.compose.material3.CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = "Scroll to load more",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelfHeader(
    title: String,
    badge: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (!badge.isNullOrBlank()) {
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Text(
                    text = badge.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CatalogContextHeader(
    sourceTitle: String?,
    feedTitle: String,
    activeQuery: String,
    resultCount: Int
) {
    val trimmedSource = sourceTitle?.trim().orEmpty()
    val trimmedFeed = feedTitle.trim()
    val primaryTitle = when {
        trimmedFeed.isNotBlank() -> trimmedFeed
        trimmedSource.isNotBlank() -> trimmedSource
        else -> "Catalog"
    }
    val metaParts = buildList {
        if (trimmedSource.isNotBlank() && !trimmedSource.equals(primaryTitle, ignoreCase = true)) {
            add(trimmedSource)
        }
        if (activeQuery.isNotBlank()) {
            add("Searching \"$activeQuery\"")
        }
        add("$resultCount titles")
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = primaryTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = metaParts.joinToString(" • "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun CatalogActionRow(
    currentScope: SearchScope,
    navigationGroups: List<BrowseNavigationGroup>,
    savedSearches: List<String>,
    onScopeClick: () -> Unit,
    onSectionsClick: () -> Unit,
    onSavedSearchClick: () -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (navigationGroups.isNotEmpty()) {
            item {
                CatalogMenuButton(
                    label = "Sections",
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    onClick = onSectionsClick
                )
            }
        }
        item {
            CatalogMenuButton(
                label = searchScopeLabel(currentScope),
                icon = Icons.Rounded.Tune,
                onClick = onScopeClick
            )
        }
        if (savedSearches.isNotEmpty()) {
            item {
                CatalogMenuButton(
                    label = "Recent",
                    icon = Icons.Rounded.History,
                    onClick = onSavedSearchClick
                )
            }
        }
    }
}

@Composable
private fun CatalogMenuButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun browseSectionLabel(section: BrowseSection): String = when (section) {
    BrowseSection.ALL -> "All"
    BrowseSection.POPULAR -> "Popular"
    BrowseSection.NEW -> "New"
    BrowseSection.RECENT -> "Recent"
}

private fun searchScopeLabel(scope: SearchScope): String = when (scope) {
    SearchScope.ALL -> "Search: All"
    SearchScope.TITLE -> "Search: Title"
    SearchScope.AUTHOR -> "Search: Author"
    SearchScope.SUBJECT -> "Search: Topic"
}

private fun sortEntries(entries: List<BrowseBook>, section: BrowseSection?): List<BrowseBook> {
    return when (section) {
        BrowseSection.POPULAR -> entries.sortedWith(
            compareByDescending<BrowseBook> { it.downloads ?: 0 }
                .thenByDescending { bestFormatScore(it) }
        )
        BrowseSection.NEW, BrowseSection.RECENT -> entries.sortedWith(
            compareByDescending<BrowseBook> { publishedYear(it) ?: 0 }
                .thenByDescending { bestFormatScore(it) }
        )
        BrowseSection.ALL, null -> entries.sortedWith(
            compareByDescending<BrowseBook> { bestFormatScore(it) }
                .thenByDescending { it.downloads ?: 0 }
        )
    }
}

private fun buildShelves(
    entries: List<BrowseBook>,
    lastVisitedTimestamp: Long?,
    sourceFlavor: BrowseSourceFlavor
): List<BrowseShelf> {
    if (entries.isEmpty()) return emptyList()
    val popularSorted = entries.sortedByDescending { it.downloads ?: 0 }
    val spotlight = popularSorted.take(6)
    val highlights = popularSorted.drop(6).take(10)

    val lastVisitedYear = lastVisitedTimestamp?.let {
        java.util.Calendar.getInstance().apply { timeInMillis = it }.get(java.util.Calendar.YEAR)
    }
    val newSinceLast = if (lastVisitedYear != null) {
        entries.filter { (publishedYear(it) ?: 0) >= lastVisitedYear }.take(10)
    } else emptyList()

    val epub3Picks = entries.filter { hasFormat(it, "epub3") }.take(10)
    val shortReads = entries.filter { estimateSizeBytes(it)?.let { size -> size in 1..1_000_000 } == true }.take(10)

    val subjectCounts = entries.flatMap { it.subjects.take(3) }
        .groupingBy { it.trim() }
        .eachCount()
        .filterKeys { it.isNotBlank() }
        .toList()
        .sortedByDescending { it.second }
        .take(2)

    return when (sourceFlavor) {
        BrowseSourceFlavor.GUTENBERG -> buildList {
            if (spotlight.isNotEmpty()) add(BrowseShelf("Popular Right Now", spotlight, badge = "Top", cardWidth = 170))
            val recentReleases = entries.sortedWith(
                compareByDescending<BrowseBook> { publishedYear(it) ?: 0 }
                    .thenByDescending { it.downloads ?: 0 }
            ).take(10)
            if (recentReleases.isNotEmpty()) add(BrowseShelf("Recently Added", recentReleases, badge = "New", cardWidth = 146))
            if (epub3Picks.isNotEmpty()) add(BrowseShelf("EPUB3 Picks", epub3Picks, badge = "EPUB3", cardWidth = 142))
            val pdfShelf = entries.filter { hasFormat(it, "pdf") }.take(10)
            if (pdfShelf.isNotEmpty()) add(BrowseShelf("PDF Editions", pdfShelf, badge = "PDF", cardWidth = 140))
            val languageShelves = entries.flatMap { it.languages.take(2) }
                .groupingBy { it.trim() }
                .eachCount()
                .filterKeys { it.isNotBlank() }
                .toList()
                .sortedByDescending { it.second }
                .take(2)
            languageShelves.forEach { (language, _) ->
                val books = entries.filter { it.languages.any { tag -> tag.equals(language, ignoreCase = true) } }.take(10)
                if (books.isNotEmpty()) add(BrowseShelf(language.uppercase(), books, badge = "Language", cardWidth = 142))
            }
            subjectCounts.take(3).forEach { (subject, _) ->
                val books = entries.filter { it.subjects.any { tag -> tag.equals(subject, ignoreCase = true) } }.take(10)
                if (books.isNotEmpty()) add(BrowseShelf(subject, books, badge = "Topic", cardWidth = 140))
            }
        }
        BrowseSourceFlavor.ANARCHIST_LIBRARY -> buildList {
            val newestTexts = entries.sortedWith(
                compareByDescending<BrowseBook> { publishedYear(it) ?: 0 }
                    .thenByDescending { it.downloads ?: 0 }
            ).take(10)
            if (newestTexts.isNotEmpty()) add(BrowseShelf("Latest Texts", newestTexts, badge = "New", cardWidth = 148))
            if (spotlight.isNotEmpty()) add(BrowseShelf("Most Read", spotlight, badge = "Popular", cardWidth = 166))
            val essayShelf = entries.filter { book ->
                book.subjects.any { subject ->
                    val normalized = subject.lowercase()
                    normalized.contains("essay") || normalized.contains("pamphlet") || normalized.contains("intro")
                }
            }.take(10)
            if (essayShelf.isNotEmpty()) add(BrowseShelf("Essays & Pamphlets", essayShelf, badge = "Quick", cardWidth = 148))
            val topAuthors = entries.map { it.author.trim() }
                .filter { it.isNotBlank() && !it.equals("Unknown Author", ignoreCase = true) }
                .groupingBy { it }
                .eachCount()
                .toList()
                .sortedByDescending { it.second }
                .take(2)
            topAuthors.forEach { (author, _) ->
                val books = entries.filter { it.author.equals(author, ignoreCase = true) }.take(10)
                if (books.isNotEmpty()) add(BrowseShelf(author, books, badge = "Author", cardWidth = 150))
            }
            subjectCounts.take(3).forEach { (subject, _) ->
                val books = entries.filter { it.subjects.any { tag -> tag.equals(subject, ignoreCase = true) } }.take(10)
                if (books.isNotEmpty()) add(BrowseShelf(subject, books, badge = "Topic", cardWidth = 144))
            }
        }
        BrowseSourceFlavor.WIKISOURCE -> buildList {
            val exportedRecently = entries.sortedByDescending { publishedYear(it) ?: 0 }.take(10)
            if (exportedRecently.isNotEmpty()) add(BrowseShelf("Recently Exported", exportedRecently, badge = "Latest", cardWidth = 148))
            if (highlights.isNotEmpty()) add(BrowseShelf("Featured Texts", highlights, badge = "Curated", cardWidth = 150))
            val pdfReady = entries.filter { hasFormat(it, "pdf") }.take(10)
            if (pdfReady.isNotEmpty()) add(BrowseShelf("PDF Ready", pdfReady, badge = "PDF", cardWidth = 138))
            subjectCounts.take(2).forEach { (subject, _) ->
                val books = entries.filter { it.subjects.any { tag -> tag.equals(subject, ignoreCase = true) } }.take(10)
                if (books.isNotEmpty()) add(BrowseShelf(subject, books, badge = "Topic", cardWidth = 140))
            }
        }
        BrowseSourceFlavor.STANDARD_EBOOKS -> buildList {
            if (epub3Picks.isNotEmpty()) add(BrowseShelf("Standout Editions", epub3Picks, badge = "EPUB3", cardWidth = 150))
            if (newSinceLast.isNotEmpty()) add(BrowseShelf("New since last visit", newSinceLast, badge = "New", cardWidth = 144))
            if (shortReads.isNotEmpty()) add(BrowseShelf("Short Reads", shortReads, badge = "Quick", cardWidth = 136))
            if (highlights.isNotEmpty()) add(BrowseShelf("Highlights", highlights, cardWidth = 148))
        }
        BrowseSourceFlavor.GENERIC -> buildList {
            if (spotlight.isNotEmpty()) add(BrowseShelf("Spotlight", spotlight, badge = "Popular", cardWidth = 172))
            if (highlights.isNotEmpty()) add(BrowseShelf("Highlights", highlights, cardWidth = 148))
            if (newSinceLast.isNotEmpty()) add(BrowseShelf("New since last visit", newSinceLast, badge = "New", cardWidth = 144))
            if (epub3Picks.isNotEmpty()) add(BrowseShelf("EPUB3 Picks", epub3Picks, badge = "EPUB3", cardWidth = 140))
            if (shortReads.isNotEmpty()) add(BrowseShelf("Short Reads", shortReads, badge = "Quick", cardWidth = 132))
            subjectCounts.forEach { (subject, _) ->
                val books = entries.filter { it.subjects.any { tag -> tag.equals(subject, ignoreCase = true) } }.take(10)
                if (books.isNotEmpty()) add(BrowseShelf(subject, books, badge = "Topic", cardWidth = 140))
            }
        }
    }
}

private fun detectSourceFlavor(
    sourceTitle: String?,
    feedId: String,
    feedTitle: String
): BrowseSourceFlavor {
    val combined = listOf(sourceTitle, feedId, feedTitle).joinToString(" ").lowercase()
    return when {
        combined.contains("gutenberg") -> BrowseSourceFlavor.GUTENBERG
        combined.contains("anarchist") -> BrowseSourceFlavor.ANARCHIST_LIBRARY
        combined.contains("wikisource") -> BrowseSourceFlavor.WIKISOURCE
        combined.contains("standard ebooks") || combined.contains("standardebooks") -> BrowseSourceFlavor.STANDARD_EBOOKS
        else -> BrowseSourceFlavor.GENERIC
    }
}

private fun publishedYear(book: BrowseBook): Int? {
    val yearLabel = extractPublishedYear(book.published)
    return yearLabel?.toIntOrNull()
}

private fun estimateSizeBytes(book: BrowseBook): Long? {
    return book.downloadOptions.mapNotNull { it.sizeBytes }.minOrNull()
}

private fun bestFormatScore(book: BrowseBook): Int {
    val formats = book.downloadOptions.mapNotNull { it.format.ifBlank { null } } + book.format
    val format = formats.firstOrNull { it.isNotBlank() }?.lowercase().orEmpty()
    return when {
        format.contains("epub3") -> 100
        format.contains("epub") -> 90
        format.contains("pdf") -> 80
        format.contains("azw3") -> 70
        format.contains("mobi") -> 60
        format.contains("txt") -> 40
        format.contains("html") -> 30
        else -> 50
    }
}

private fun hasFormat(book: BrowseBook, target: String): Boolean {
    val lower = target.lowercase()
    if (book.format.lowercase().contains(lower)) return true
    return book.downloadOptions.any { it.format.lowercase().contains(lower) || (it.label?.lowercase()?.contains(lower) == true) }
}

private fun matchesScope(book: BrowseBook, query: String, scope: SearchScope): Boolean {
    val needle = query.lowercase()
    return when (scope) {
        SearchScope.ALL -> {
            book.title.lowercase().contains(needle) ||
                book.author.lowercase().contains(needle) ||
                book.subjects.any { it.lowercase().contains(needle) }
        }
        SearchScope.TITLE -> book.title.lowercase().contains(needle)
        SearchScope.AUTHOR -> book.author.lowercase().contains(needle)
        SearchScope.SUBJECT -> book.subjects.any { it.lowercase().contains(needle) }
    }
}
