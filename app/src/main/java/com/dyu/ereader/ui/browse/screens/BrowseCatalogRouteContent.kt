package com.dyu.ereader.ui.browse.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dyu.ereader.data.model.browse.BrowseBook
import com.dyu.ereader.data.model.browse.BrowseCatalog
import com.dyu.ereader.data.model.browse.CatalogHealthStatus
import com.dyu.ereader.ui.browse.components.BrowseCatalogHeader
import com.dyu.ereader.ui.browse.components.CatalogList
import com.dyu.ereader.ui.browse.components.ErrorState
import com.dyu.ereader.ui.browse.components.FeedContent
import com.dyu.ereader.ui.home.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BrowseCatalogRouteContent(
    modifier: Modifier = Modifier,
    liquidGlassEnabled: Boolean = false,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val browseCatalogs by viewModel.browseCatalogs.collectAsState(initial = emptyList())
    val homeState by viewModel.uiState.collectAsState()
    val catalogHealth by viewModel.browseCatalogHealth.collectAsState(initial = emptyMap())
    val currentFeed by viewModel.currentFeed.collectAsState()
    val isLoading by viewModel.isLoadingBrowse.collectAsState(initial = false)
    val isLoadingNext by viewModel.isLoadingBrowseNext.collectAsState(initial = false)
    val downloadQueue by viewModel.browseDownloadQueue.collectAsState(initial = emptyList())
    val downloadMessage by viewModel.browseDownloadMessage.collectAsState(initial = null)
    val error by viewModel.browseError.collectAsState(initial = null)
    val currentUrl by viewModel.browseCurrentCatalogUrl.collectAsState(initial = "")
    val savedSearches by viewModel.browseSavedSearches.collectAsState(initial = emptyList())

    var showAddSourceDialog by remember { mutableStateOf(false) }
    var showDownloads by remember { mutableStateOf(true) }
    var selectedBook by remember { mutableStateOf<BrowseBook?>(null) }
    var preferredFormat by remember { mutableStateOf<String?>(null) }
    var lastVisited by remember { mutableStateOf<Long?>(null) }
    var isRefreshingCatalogs by remember { mutableStateOf(false) }
    var lastAutoNavigatedFeedId by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pullState = rememberPullToRefreshState()

    val sortedCatalogs = remember(browseCatalogs, catalogHealth) {
        val statusRank: (CatalogHealthStatus?) -> Int = { status ->
            when (status) {
                CatalogHealthStatus.ONLINE -> 0
                CatalogHealthStatus.CHECKING -> 1
                CatalogHealthStatus.UNKNOWN -> 2
                CatalogHealthStatus.ERROR -> 3
                null -> 2
            }
        }
        browseCatalogs.sortedWith(
            compareBy<BrowseCatalog> { statusRank(catalogHealth[it.id]) }
                .thenBy { it.title.lowercase() }
        )
    }

    LaunchedEffect(downloadMessage) {
        val message = downloadMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.consumeBrowseDownloadMessage()
    }

    LaunchedEffect(error) {
        val message = error ?: return@LaunchedEffect
        if (currentFeed != null) {
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(downloadQueue.size) {
        if (downloadQueue.isEmpty()) {
            showDownloads = false
        }
    }

    LaunchedEffect(currentFeed?.id) {
        if (!currentUrl.isBlank() && currentFeed != null) {
            viewModel.setBrowseLastVisit(currentUrl, System.currentTimeMillis())
        }
    }

    LaunchedEffect(currentUrl) {
        if (currentUrl.isBlank()) {
            preferredFormat = null
            lastVisited = null
        } else {
            preferredFormat = viewModel.getBrowseFormatPreference(currentUrl)
            lastVisited = viewModel.getBrowseLastVisit(currentUrl)
        }
    }

    LaunchedEffect(currentFeed?.id, currentFeed?.entries?.size, currentFeed?.navigationGroups?.size) {
        val feed = currentFeed ?: return@LaunchedEffect
        if (feed.entries.isNotEmpty()) return@LaunchedEffect
        if (feed.navigationGroups.isEmpty()) return@LaunchedEffect
        if (lastAutoNavigatedFeedId == feed.id) return@LaunchedEffect
        val targetGroup = feed.navigationGroups.firstOrNull { group ->
            val title = group.title.lowercase()
            title.contains("new") || title.contains("latest") || title.contains("titles") || title.contains("all")
        } ?: feed.navigationGroups.first()
        lastAutoNavigatedFeedId = feed.id
        val baseCatalog = browseCatalogs.firstOrNull { it.url == currentUrl }
        viewModel.loadBrowseCatalog(
            BrowseCatalog(
                id = "auto_${targetGroup.href.hashCode()}",
                title = targetGroup.title,
                url = targetGroup.href,
                description = "OPDS section",
                icon = baseCatalog?.icon,
                username = baseCatalog?.username,
                password = baseCatalog?.password,
                apiKey = baseCatalog?.apiKey
            ),
            updateCurrentUrl = false
        )
    }

    LaunchedEffect(browseCatalogs, currentUrl, catalogHealth) {
        if (currentUrl.isBlank() && browseCatalogs.isNotEmpty()) {
            viewModel.setBrowseCurrentCatalogUrl(browseCatalogs.first().url)
        }
        val shouldRefreshHealth = browseCatalogs.isNotEmpty() && (
            catalogHealth.isEmpty() || catalogHealth.values.all { it == CatalogHealthStatus.UNKNOWN }
        )
        if (shouldRefreshHealth) {
            viewModel.refreshBrowseCatalogHealth()
        }
    }

    BackHandler(enabled = currentFeed != null || error != null) {
        viewModel.clearBrowseFeed()
    }

    val currentCatalog = remember(currentUrl, browseCatalogs) {
        browseCatalogs.firstOrNull { it.url == currentUrl }
    }
    val isInFeed = currentFeed != null
    val headerTitle = if (isInFeed) currentCatalog?.title ?: "Browse" else "Browse"

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            if (isInFeed || error != null) {
                BrowseCatalogHeader(
                    title = headerTitle,
                    iconUrl = currentCatalog?.icon,
                    downloadQueue = downloadQueue,
                    showDownloads = showDownloads,
                    liquidGlassEnabled = liquidGlassEnabled,
                    onBack = { viewModel.clearBrowseFeed() },
                    onToggleDownloads = { showDownloads = !showDownloads },
                    onDismissDownloads = { showDownloads = false },
                    onDownloadTaskAction = { task ->
                        when (task.state) {
                            com.dyu.ereader.data.model.browse.BrowseDownloadState.QUEUED -> viewModel.cancelBrowseDownload(task.id)
                            com.dyu.ereader.data.model.browse.BrowseDownloadState.DOWNLOADING -> viewModel.pauseBrowseDownload(task.id)
                            com.dyu.ereader.data.model.browse.BrowseDownloadState.PAUSED -> viewModel.resumeBrowseDownload(task.id)
                            com.dyu.ereader.data.model.browse.BrowseDownloadState.FAILED,
                            com.dyu.ereader.data.model.browse.BrowseDownloadState.CANCELED -> viewModel.retryBrowseDownload(task.id)
                            com.dyu.ereader.data.model.browse.BrowseDownloadState.COMPLETED -> Unit
                        }
                    }
                )
            }

            Spacer(Modifier.height(if (isInFeed || error != null) 16.dp else 8.dp))

            val contentState = when {
                isLoading && currentFeed == null -> "loading"
                error != null && currentFeed == null -> "error"
                currentFeed != null -> "feed"
                else -> "catalogs"
            }

            AnimatedContent(
                targetState = contentState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "BrowseContent"
            ) { state ->
                when (state) {
                    "loading" -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    "error" -> {
                        ErrorState(
                            error = error ?: "Unknown error",
                            onRetry = {
                                viewModel.loadBrowseCatalog(
                                    BrowseCatalog(id = "retry", title = "Retry", url = currentUrl)
                                )
                            }
                        )
                    }
                    "feed" -> {
                        currentFeed?.let { feed ->
                            val hasNextPage = feed.links.any { link ->
                                link.rel.contains("next", ignoreCase = true)
                            }
                            FeedContent(
                                feed = feed,
                                sourceTitle = currentCatalog?.title,
                                liquidGlassEnabled = liquidGlassEnabled,
                                textScrollerEnabled = homeState.display.textScrollerEnabled,
                                savedSearches = savedSearches,
                                lastVisitedTimestamp = lastVisited,
                                onRefresh = {
                                    viewModel.loadBrowseCatalog(
                                        BrowseCatalog(id = "refresh", title = "Refresh", url = currentUrl)
                                    )
                                },
                                isRefreshing = isLoading,
                                onBookClick = { book -> selectedBook = book },
                                onSearch = { query ->
                                    viewModel.searchBrowse(query)
                                    viewModel.addBrowseSavedSearch(query)
                                },
                                searchKey = currentUrl,
                                navigationGroups = feed.navigationGroups,
                                hasNextPage = hasNextPage,
                                isLoadingNextPage = isLoadingNext,
                                onLoadNextPage = { viewModel.loadBrowseNextPage() },
                                onNavigateGroup = { group ->
                                    val baseCatalog = currentCatalog
                                    viewModel.loadBrowseCatalog(
                                        BrowseCatalog(
                                            id = "nav_${group.href.hashCode()}",
                                            title = group.title,
                                            url = group.href,
                                            description = "OPDS section",
                                            icon = baseCatalog?.icon
                                        ),
                                        updateCurrentUrl = false
                                    )
                                }
                            )
                        }
                    }
                    else -> {
                        PullToRefreshBox(
                            isRefreshing = isRefreshingCatalogs,
                            state = pullState,
                            onRefresh = {
                                isRefreshingCatalogs = true
                                viewModel.refreshBrowseCatalogHealth()
                                scope.launch {
                                    delay(700)
                                    isRefreshingCatalogs = false
                                }
                            },
                            indicator = {
                                PullToRefreshDefaults.Indicator(
                                    state = pullState,
                                    isRefreshing = isRefreshingCatalogs,
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .padding(top = 8.dp),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        ) {
                            CatalogList(
                                catalogs = sortedCatalogs,
                                liquidGlassEnabled = liquidGlassEnabled,
                                catalogHealth = catalogHealth,
                                onCatalogClick = { catalog ->
                                    viewModel.setBrowseCurrentCatalogUrl(catalog.url)
                                    viewModel.loadBrowseCatalog(catalog)
                                },
                                onAddSourceClick = { showAddSourceDialog = true },
                                onRemoveCustomSource = { catalog -> viewModel.removeBrowseCatalog(catalog.id) }
                            )
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        )

        BrowseCatalogAddSourceOverlay(
            visible = showAddSourceDialog,
            onDismiss = { showAddSourceDialog = false },
            onAddCatalog = { catalog -> viewModel.addBrowseCatalog(catalog) }
        )

        BrowseCatalogSelectionOverlays(
            selectedBook = selectedBook,
            currentUrl = currentUrl,
            preferredFormat = preferredFormat,
            downloadQueue = downloadQueue,
            liquidGlassEnabled = liquidGlassEnabled,
            viewModel = viewModel,
            onSelectedBookChange = { selectedBook = it },
            onPreferredFormatChange = { preferredFormat = it }
        )
    }
}
