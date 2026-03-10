package com.dyu.ereader.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dyu.ereader.data.model.BookItem
import com.dyu.ereader.data.model.BookType
import com.dyu.ereader.data.repository.LibraryRepository
import com.dyu.ereader.data.repository.AnalyticsRepository
import com.dyu.ereader.data.repository.BrowseRepository
import com.dyu.ereader.data.repository.CloudSyncRepository
import com.dyu.ereader.data.model.BrowseCatalog
import com.dyu.ereader.data.model.BrowseBook
import com.dyu.ereader.data.model.CloudProvider
import com.dyu.ereader.data.model.ReaderControl
import com.dyu.ereader.data.storage.ReaderPreferencesStore
import com.dyu.ereader.util.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val preferencesStore: ReaderPreferencesStore,
    private val analyticsRepository: AnalyticsRepository,
    private val browseRepository: BrowseRepository,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // Browse functionality
    val browseCatalogs = browseRepository.catalogs
    val currentFeed = browseRepository.currentFeed
    val isLoadingBrowse = browseRepository.isLoading
    val browseError = browseRepository.error
    val isDownloadingBrowse = browseRepository.isDownloading
    val browseDownloadProgress = browseRepository.downloadProgress
    private val _browseDownloadMessage = MutableStateFlow<String?>(null)
    val browseDownloadMessage: StateFlow<String?> = _browseDownloadMessage.asStateFlow()

    // Cloud sync functionality
    val isCloudSignedIn = cloudSyncRepository.syncSettings.map { it.provider != CloudProvider.NONE }
    val cloudProvider = cloudSyncRepository.syncSettings.map { it.provider }
    val cloudSyncStatus = cloudSyncRepository.syncError.map { it ?: "Synced" }
    private val syncTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val lastSyncTime = cloudSyncRepository.syncSettings.map { settings ->
        settings.lastSyncTime.takeIf { it > 0L }?.let { syncTimeFormat.format(Date(it)) }
    }
    val isSyncing = cloudSyncRepository.isSyncing

    // Analytics functionality
    val libraryStats = analyticsRepository.libraryStatistics

    private var scanJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                preferencesStore.libraryTreeUriFlow,
                preferencesStore.showBookTypeFlow,
                preferencesStore.sortOrderFlow,
                preferencesStore.hideStatusBarFlow,
                preferencesStore.showRecentReadingFlow,
                preferencesStore.showFavoritesFlow,
                preferencesStore.showGenresFlow,
                preferencesStore.gridColumnsFlow,
                preferencesStore.animationsEnabledFlow,
                preferencesStore.showReaderSearchFlow,
                preferencesStore.showReaderTTSFlow,
                preferencesStore.showReaderAccessibilityFlow,
                preferencesStore.showReaderAnalyticsFlow,
                preferencesStore.showReaderExportFlow,
                preferencesStore.readerControlOrderFlow
            ) { params: Array<Any?> ->
                val uri = params[0] as String?
                val showType = params[1] as Boolean
                val sortOrder = params[2] as String
                val hideStatusBar = params[3] as Boolean
                val showRecent = params[4] as Boolean
                val showFavorites = params[5] as Boolean
                val showGenres = params[6] as Boolean
                val gridColumns = params[7] as Int
                val animationsEnabled = params[8] as Boolean
                val showReaderSearch = params[9] as Boolean
                val showReaderTTS = params[10] as Boolean
                val showReaderAccessibility = params[11] as Boolean
                val showReaderAnalytics = params[12] as Boolean
                val showReaderExport = params[13] as Boolean
                val readerControlOrder = params[14] as List<ReaderControl>

                val order = SortOrder.entries.find { it.name == sortOrder } ?: SortOrder.TITLE
                _uiState.update {
                    it.copy(
                        libraryUri = uri,
                        sortOrder = order,
                        display = it.display.copy(
                            showBookType = showType,
                            hideStatusBar = hideStatusBar,
                            showRecentReading = showRecent,
                            showFavorites = showFavorites,
                            showGenres = showGenres,
                            gridColumns = gridColumns,
                            animationsEnabled = animationsEnabled,
                            showReaderSearch = showReaderSearch,
                            showReaderTTS = showReaderTTS,
                            showReaderAccessibility = showReaderAccessibility,
                            showReaderAnalytics = showReaderAnalytics,
                            showReaderExport = showReaderExport,
                            readerControlOrder = readerControlOrder
                        )
                    )
                }
                if (!uri.isNullOrBlank()) {
                    refreshLibrary()
                }
                applyFiltersAndSort()
            }.collectLatest { }
        }

        viewModelScope.launch {
            libraryRepository.getBooksFlow().collectLatest { books ->
                val booksWithProgress = books.map { book ->
                    val progress = preferencesStore.bookProgressFlow(book.uri).first()
                    book.copy(progress = progress)
                }
                val allGenres = books.flatMap { it.genres }.distinct().sorted()
                _uiState.update { it.copy(allBooks = booksWithProgress, availableGenres = allGenres) }
                applyFiltersAndSort()
            }
        }
    }

    fun onSearchChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFiltersAndSort()
    }

    fun onSearchFilterChanged(filter: BookFilter) {
        _uiState.update { it.copy(searchFilter = filter) }
        applyFiltersAndSort()
    }

    fun onToggleTypeFilter(type: BookType) {
        _uiState.update { current ->
            val next = if (current.selectedTypes.contains(type)) {
                current.selectedTypes - type
            } else {
                current.selectedTypes + type
            }
            current.copy(selectedTypes = next)
        }
        applyFiltersAndSort()
    }

    fun onToggleGenreFilter(genre: String) {
        _uiState.update { current ->
            val next = if (current.selectedGenres.contains(genre)) {
                current.selectedGenres - genre
            } else {
                current.selectedGenres + genre
            }
            current.copy(selectedGenres = next)
        }
        applyFiltersAndSort()
    }

    fun clearAdvancedFilters() {
        _uiState.update { it.copy(selectedTypes = emptySet(), selectedGenres = emptySet()) }
        applyFiltersAndSort()
    }

    fun onSortOrderChanged(order: SortOrder) {
        viewModelScope.launch {
            preferencesStore.setSortOrder(order.name)
        }
    }

    fun onShowBookTypeChanged(show: Boolean) {
        viewModelScope.launch {
            preferencesStore.setShowBookType(show)
        }
    }

    fun onShowRecentReadingChanged(show: Boolean) {
        viewModelScope.launch {
            preferencesStore.setShowRecentReading(show)
        }
    }

    fun onShowFavoritesChanged(show: Boolean) {
        viewModelScope.launch {
            preferencesStore.setShowFavorites(show)
        }
    }

    fun onShowGenresChanged(show: Boolean) {
        viewModelScope.launch {
            preferencesStore.setShowGenres(show)
        }
    }

    fun onHideStatusBarChanged(hide: Boolean) {
        viewModelScope.launch {
            preferencesStore.setHideStatusBar(hide)
        }
    }

    fun onGridColumnsChanged(columns: Int) {
        viewModelScope.launch {
            preferencesStore.setGridColumns(columns)
        }
    }

    fun onAnimationsToggle(enabled: Boolean) {
        viewModelScope.launch {
            preferencesStore.setAnimationsEnabled(enabled)
        }
    }

    fun onToggleReaderSearch(show: Boolean) = viewModelScope.launch { preferencesStore.setShowReaderSearch(show) }
    fun onToggleReaderTTS(show: Boolean) = viewModelScope.launch { preferencesStore.setShowReaderTTS(show) }
    fun onToggleReaderAccessibility(show: Boolean) = viewModelScope.launch { preferencesStore.setShowReaderAccessibility(show) }
    fun onToggleReaderAnalytics(show: Boolean) = viewModelScope.launch { preferencesStore.setShowReaderAnalytics(show) }
    fun onToggleReaderExport(show: Boolean) = viewModelScope.launch { preferencesStore.setShowReaderExport(show) }
    fun onReaderControlOrderChanged(order: List<ReaderControl>) = viewModelScope.launch {
        preferencesStore.setReaderControlOrder(order)
    }

    fun toggleLayout() {
        _uiState.update {
            val newLayout = if (it.display.layout == LibraryLayout.GRID) LibraryLayout.LIST else LibraryLayout.GRID
            it.copy(display = it.display.copy(layout = newLayout))
        }
    }

    fun toggleFavorite(bookId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            libraryRepository.toggleFavorite(bookId, isFavorite)
        }
    }

    private fun applyFiltersAndSort() {
        val current = _uiState.value
        val filteredBooks = filterBooks(current)
        val sortedBooks = sortBooks(filteredBooks, current.sortOrder)
        val recentBooks = getRecentBooks(current.allBooks)

        _uiState.update { it.copy(visibleBooks = sortedBooks, recentBooks = recentBooks) }
    }

    private fun filterBooks(state: HomeUiState): List<BookItem> {
        val query = state.searchQuery.lowercase()
        var filtered = state.allBooks

        if (query.isNotBlank()) {
            filtered = filtered.filter { book ->
                when (state.searchFilter) {
                    BookFilter.ALL -> book.matchesQuery(query)
                    BookFilter.TITLE -> book.title.lowercase().contains(query)
                    BookFilter.AUTHOR -> book.author.lowercase().contains(query)
                    BookFilter.LANGUAGE -> book.language?.lowercase()?.contains(query) == true
                    BookFilter.YEAR -> book.year?.contains(query) == true
                    BookFilter.EXTENSION -> book.fileName.lowercase().contains(query)
                }
            }
        }

        if (state.selectedTypes.isNotEmpty()) {
            filtered = filtered.filter { state.selectedTypes.contains(it.type) }
        }

        if (state.selectedGenres.isNotEmpty()) {
            filtered = filtered.filter { book ->
                book.genres.any { state.selectedGenres.contains(it) }
            }
        }

        return filtered
    }

    private fun BookItem.matchesQuery(query: String): Boolean {
        return title.lowercase().contains(query) ||
                author.lowercase().contains(query) ||
                language?.lowercase()?.contains(query) == true ||
                year?.contains(query) == true ||
                fileName.lowercase().contains(query)
    }

    private fun sortBooks(books: List<BookItem>, sortOrder: SortOrder): List<BookItem> {
        return when (sortOrder) {
            SortOrder.TITLE -> books.sortedBy { it.title }
            SortOrder.AUTHOR -> books.sortedBy { it.author }
            SortOrder.DATE_ADDED -> books.sortedByDescending { it.dateAdded }
        }
    }

    private fun getRecentBooks(books: List<BookItem>): List<BookItem> {
        return books
            .filter { it.lastOpened > 0 }
            .sortedByDescending { it.lastOpened }
            .take(10)
    }

    fun onLibraryAccessGranted(uri: Uri) {
        viewModelScope.launch {
            libraryRepository.onLibraryAccessGranted(uri)
            preferencesStore.setLibraryTreeUri(uri.toString())
            refreshLibrary(uri.toString())
        }
    }

    fun revokeLibraryAccess() {
        viewModelScope.launch {
            val uriString = _uiState.value.libraryUri
            if (uriString.isNullOrBlank()) {
                preferencesStore.setLibraryTreeUri("")
                return@launch
            }
            libraryRepository.revokeLibraryAccess(uriString)
            preferencesStore.setLibraryTreeUri("")
        }
    }

    fun refreshLibrary(forcedUri: String? = null) {
        val uriString = forcedUri ?: _uiState.value.libraryUri ?: return
        val treeUri = Uri.parse(uriString)

        if (scanJob?.isActive == true) {
            scanJob?.cancel()
        }
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true, errorMessage = null) }

            runCatching { libraryRepository.scanBooks(treeUri) }
                .onSuccess {
                    _uiState.update { it.copy(isScanning = false) }
                }
                .onFailure { error ->
                    if (error !is CancellationException && error.cause !is CancellationException) {
                        Logger.log("Library Scan Failed", error)
                    }
                    _uiState.update {
                        it.copy(isScanning = false)
                    }
                }
        }
    }

    suspend fun exportSettings(): String {
        return preferencesStore.exportPreferencesJson()
    }

    fun importSettings(json: String) {
        viewModelScope.launch {
            preferencesStore.importPreferencesJson(json)
            // Trigger a refresh of the library if the URI changed
            _uiState.update { it.copy(libraryUri = preferencesStore.libraryTreeUriFlow.first()) }
            refreshLibrary()
        }
    }

    // Browse functionality
    fun loadBrowseCatalog(catalog: BrowseCatalog) {
        viewModelScope.launch {
            browseRepository.loadCatalog(catalog)
        }
    }

    fun clearBrowseFeed() {
        browseRepository.clearFeed()
    }

    fun searchBrowse(query: String) {
        viewModelScope.launch {
            browseRepository.searchCatalog(query)
        }
    }

    fun addBrowseCatalog(catalog: BrowseCatalog) {
        browseRepository.addCatalog(catalog)
    }

    fun removeBrowseCatalog(catalogId: String) {
        browseRepository.removeCatalog(catalogId)
    }

    fun downloadBrowseBook(book: BrowseBook) {
        viewModelScope.launch {
            _browseDownloadMessage.value = null

            val downloadedFile = browseRepository.downloadBookToAppStorage(book).getOrElse { error ->
                _browseDownloadMessage.value = "Download failed: ${error.localizedMessage ?: "Unknown error"}"
                return@launch
            }

            val importResult = libraryRepository.importBook(downloadedFile)
            importResult
                .onSuccess {
                    _browseDownloadMessage.value = "Imported \"${book.title}\" into your library"
                }
                .onFailure { error ->
                    _browseDownloadMessage.value = "Downloaded but import failed: ${error.localizedMessage ?: "Unknown error"}"
                }
        }
    }

    fun consumeBrowseDownloadMessage() {
        _browseDownloadMessage.value = null
    }

    // Cloud sync functionality
    fun signInToCloud(provider: CloudProvider) {
        viewModelScope.launch {
            cloudSyncRepository.setOAuthToken(provider, "local_token_${System.currentTimeMillis()}")
            cloudSyncRepository.setProvider(provider)
        }
    }

    fun signOutFromCloud() {
        viewModelScope.launch {
            cloudSyncRepository.clearAuthentication()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            cloudSyncRepository.performSync()
        }
    }

    fun clearSyncError() {
        cloudSyncRepository.clearSyncError()
    }

    // Analytics functionality
    fun clearAnalytics() {
        analyticsRepository.clearAnalytics()
    }
}
