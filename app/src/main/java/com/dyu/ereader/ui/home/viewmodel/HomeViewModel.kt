package com.dyu.ereader.ui.home.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import com.dyu.ereader.data.local.db.BookDatabase
import com.dyu.ereader.data.local.prefs.ReaderPreferencesStore
import com.dyu.ereader.data.model.browse.BrowseBook
import com.dyu.ereader.data.model.browse.BrowseCatalog
import com.dyu.ereader.data.model.browse.BrowseDownloadOption
import com.dyu.ereader.data.model.browse.BrowseDownloadTask
import com.dyu.ereader.data.model.cloud.CloudBackupScope
import com.dyu.ereader.data.model.cloud.CloudProvider
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.data.model.reader.ReaderControl
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.repository.analytics.AnalyticsRepository
import com.dyu.ereader.data.repository.browse.BrowseRepository
import com.dyu.ereader.data.repository.cloud.CloudSyncRepository
import com.dyu.ereader.data.repository.export.ExportRepository
import com.dyu.ereader.data.repository.library.LibraryRepository
import com.dyu.ereader.data.repository.notifications.ReadingReminderRepository
import com.dyu.ereader.ui.home.state.BookFilter
import com.dyu.ereader.ui.home.state.HomeUiState
import com.dyu.ereader.ui.home.state.ReadingStatus
import com.dyu.ereader.ui.home.state.SortOrder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
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
    private val cloudSyncRepository: CloudSyncRepository,
    private val exportRepository: ExportRepository,
    private val database: BookDatabase,
    private val readingReminderRepository: ReadingReminderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val browseCatalogs = browseRepository.catalogs
    val currentFeed = browseRepository.currentFeed
    val isLoadingBrowse = browseRepository.isLoading
    val isLoadingBrowseNext = browseRepository.isLoadingNext
    val browseError = browseRepository.error
    val browseCatalogHealth = browseRepository.catalogHealth
    val isDownloadingBrowse = browseRepository.isDownloading
    val browseDownloadProgress = browseRepository.downloadProgress
    val browseSavedSearches = preferencesStore.browseSavedSearchesFlow

    private val _browseDownloadQueue = MutableStateFlow<List<BrowseDownloadTask>>(emptyList())
    val browseDownloadQueue: StateFlow<List<BrowseDownloadTask>> = _browseDownloadQueue.asStateFlow()
    private val _browseDownloadMessage = MutableStateFlow<String?>(null)
    val browseDownloadMessage: StateFlow<String?> = _browseDownloadMessage.asStateFlow()
    private val _browseCurrentCatalogUrl = MutableStateFlow("")
    val browseCurrentCatalogUrl: StateFlow<String> = _browseCurrentCatalogUrl.asStateFlow()

    private val _libraryMessage = MutableStateFlow<String?>(null)
    val libraryMessage: StateFlow<String?> = _libraryMessage.asStateFlow()
    private val _pendingExportUri = MutableStateFlow<Uri?>(null)
    val pendingExportUri: StateFlow<Uri?> = _pendingExportUri.asStateFlow()
    private val _cloudMessage = MutableStateFlow<String?>(null)
    val cloudMessage: StateFlow<String?> = _cloudMessage.asStateFlow()

    val linkedCloudAccounts = cloudSyncRepository.linkedAccounts
    val cloudStorageSummary = cloudSyncRepository.storageSummary
    val isCloudSignedIn = linkedCloudAccounts.map { it.isNotEmpty() }
    val cloudProvider = linkedCloudAccounts.map { accounts ->
        accounts.firstOrNull()?.provider ?: CloudProvider.NONE
    }
    val cloudSyncStatus = cloudSyncRepository.syncError.map { it ?: "Ready" }
    private val syncTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val lastSyncTime = cloudSyncRepository.syncSettings.map { settings ->
        settings.lastSyncTime.takeIf { it > 0L }?.let { syncTimeFormat.format(Date(it)) }
    }
    val isSyncing = cloudSyncRepository.isSyncing
    val backupSnapshots = cloudSyncRepository.backupSnapshots

    val libraryStats = analyticsRepository.libraryStatistics

    private var scanJob: Job? = null
    private var downloadQueueJob: Job? = null

    internal val homeUiStateFlow get() = _uiState
    internal val browseDownloadQueueFlow get() = _browseDownloadQueue
    internal val browseDownloadMessageFlow get() = _browseDownloadMessage
    internal val browseCurrentCatalogUrlFlow get() = _browseCurrentCatalogUrl
    internal val libraryMessageFlow get() = _libraryMessage
    internal val pendingExportUriFlow get() = _pendingExportUri
    internal val cloudMessageFlow get() = _cloudMessage

    internal var scanJobState: Job?
        get() = scanJob
        set(value) {
            scanJob = value
        }

    internal var downloadQueueJobState: Job?
        get() = downloadQueueJob
        set(value) {
            downloadQueueJob = value
        }

    internal val libraryRepo get() = libraryRepository
    internal val prefsStore get() = preferencesStore
    internal val analyticsRepo get() = analyticsRepository
    internal val browseRepo get() = browseRepository
    internal val cloudRepo get() = cloudSyncRepository
    internal val exportRepo get() = exportRepository
    internal val bookDatabase get() = database
    internal val notificationRepo get() = readingReminderRepository

    init {
        observeHomePreferencesInternal()
        observeLibraryContentInternal()
    }

    fun onSearchChanged(query: String) = handleSearchChanged(query)
    fun onSearchFilterChanged(filter: BookFilter) = handleSearchFilterChanged(filter)
    fun onToggleTypeFilter(type: BookType) = handleToggleTypeFilter(type)
    fun onToggleGenreFilter(genre: String) = handleToggleGenreFilter(genre)
    fun onToggleLanguageFilter(language: String) = handleToggleLanguageFilter(language)
    fun onToggleYearFilter(year: String) = handleToggleYearFilter(year)
    fun onToggleCountryFilter(country: String) = handleToggleCountryFilter(country)
    fun onToggleReadingStatus(status: ReadingStatus) = handleToggleReadingStatus(status)
    fun clearAdvancedFilters() = handleClearAdvancedFilters()

    fun onSortOrderChanged(order: SortOrder) = handleSortOrderChanged(order)
    fun onShowBookTypeChanged(show: Boolean) = handleShowBookTypeChanged(show)
    fun onShowRecentReadingChanged(show: Boolean) = handleShowRecentReadingChanged(show)
    fun onShowFavoritesChanged(show: Boolean) = handleShowFavoritesChanged(show)
    fun onShowGenresChanged(show: Boolean) = handleShowGenresChanged(show)
    fun onHideStatusBarChanged(hide: Boolean) = handleHideStatusBarChanged(hide)
    fun onGridColumnsChanged(columns: Int) = handleGridColumnsChanged(columns)
    fun onAnimationsToggle(enabled: Boolean) = handleAnimationsToggle(enabled)
    fun onHapticsToggle(enabled: Boolean) = handleHapticsToggle(enabled)
    fun onTextScrollerToggle(enabled: Boolean) = handleTextScrollerToggle(enabled)
    fun onHideBetaFeaturesChanged(hidden: Boolean) = handleHideBetaFeaturesChanged(hidden)
    fun onDeveloperOptionsChanged(enabled: Boolean) = handleDeveloperOptionsChanged(enabled)
    fun onAppTextScaleChange(scale: Float) = handleAppTextScaleChange(scale)
    fun onToggleReaderSearch(show: Boolean) = handleToggleReaderSearch(show)
    fun onToggleReaderListen(show: Boolean) = handleToggleReaderListen(show)
    fun onToggleReaderAccessibility(show: Boolean) = handleToggleReaderAccessibility(show)
    fun onToggleReaderAnalytics(show: Boolean) = handleToggleReaderAnalytics(show)
    fun onToggleReaderExport(show: Boolean) = handleToggleReaderExport(show)
    fun onReaderControlOrderChanged(order: List<ReaderControl>) = handleReaderControlOrderChanged(order)
    fun onReaderSettingsChanged(settings: ReaderSettings) = handleReaderSettingsChanged(settings)
    fun onNotificationsEnabledChanged(enabled: Boolean) = handleNotificationsEnabledChanged(enabled)
    fun onReadingReminderEnabledChanged(enabled: Boolean) = handleReadingReminderEnabledChanged(enabled)
    fun onReadingReminderTimeChanged(hour: Int, minute: Int) = handleReadingReminderTimeChanged(hour, minute)
    fun sendTestNotification() = handleSendTestNotification()
    fun recordLocalBackupExport() = handleRecordLocalBackupExport()
    fun recordLocalBackupImport() = handleRecordLocalBackupImport()
    fun toggleLayout() = handleToggleLayout()

    fun toggleFavorite(bookId: String, isFavorite: Boolean) = handleToggleFavorite(bookId, isFavorite)
    fun onLibraryAccessGranted(uri: Uri) = handleOnLibraryAccessGranted(uri)
    fun revokeLibraryAccess() = handleRevokeLibraryAccess()
    fun refreshLibrary(forcedUri: String? = null) = handleRefreshLibrary(forcedUri)
    suspend fun exportSettings(): String = cloudRepo.exportBackupJson()
    fun importSettings(json: String) = handleImportSettings(json)

    fun loadBrowseCatalog(catalog: BrowseCatalog, updateCurrentUrl: Boolean = true) =
        handleLoadBrowseCatalog(catalog, updateCurrentUrl)

    fun loadBrowseNextPage() = handleLoadBrowseNextPage()
    fun clearBrowseFeed() = handleClearBrowseFeed()
    fun setBrowseCurrentCatalogUrl(url: String) = handleSetBrowseCurrentCatalogUrl(url)
    fun refreshBrowseCatalogHealth() = handleRefreshBrowseCatalogHealth()
    fun searchBrowse(query: String) = handleSearchBrowse(query)
    fun addBrowseSavedSearch(query: String) = handleAddBrowseSavedSearch(query)
    suspend fun getBrowseFormatPreference(url: String): String? = getBrowseFormatPreferenceInternal(url)
    fun setBrowseFormatPreference(url: String, format: String) = handleSetBrowseFormatPreference(url, format)
    suspend fun getBrowseLastVisit(url: String): Long? = getBrowseLastVisitInternal(url)
    fun setBrowseLastVisit(url: String, timestamp: Long) = handleSetBrowseLastVisit(url, timestamp)
    suspend fun resolveBrowseDownloadOptions(book: BrowseBook): List<BrowseDownloadOption> =
        resolveBrowseDownloadOptionsInternal(book)

    fun isBrowseBookAlreadyInLibrary(book: BrowseBook): Boolean = isBrowseBookAlreadyInLibraryInternal(book)
    fun queuedBrowseDownload(book: BrowseBook): BrowseDownloadTask? = queuedBrowseDownloadInternal(book)
    fun addBrowseCatalog(catalog: BrowseCatalog) = handleAddBrowseCatalog(catalog)
    fun removeBrowseCatalog(catalogId: String) = handleRemoveBrowseCatalog(catalogId)
    fun downloadBrowseBook(book: BrowseBook) = handleDownloadBrowseBook(book)
    fun downloadFromDirectUrl(url: String) = handleDownloadFromDirectUrl(url)
    fun cancelBrowseDownload(taskId: String) = handleCancelBrowseDownload(taskId)
    fun pauseBrowseDownload(taskId: String) = handlePauseBrowseDownload(taskId)
    fun resumeBrowseDownload(taskId: String) = handleResumeBrowseDownload(taskId)
    fun retryBrowseDownload(taskId: String) = handleRetryBrowseDownload(taskId)
    fun consumeBrowseDownloadMessage() = consumeBrowseDownloadMessageInternal()

    fun consumeLibraryMessage() = consumeLibraryMessageInternal()
    fun consumePendingExportUri() = consumePendingExportUriInternal()
    fun deleteBook(book: BookItem) = handleDeleteBook(book)
    fun createLibraryCollection(name: String) = handleCreateLibraryCollection(name)
    fun createLibraryCollection(name: String, book: BookItem) = handleCreateLibraryCollection(name, book)
    fun toggleBookInCollection(collectionName: String, book: BookItem) =
        handleToggleBookInCollection(collectionName, book)

    fun deleteLibraryCollection(collectionName: String) = handleDeleteLibraryCollection(collectionName)
    fun exportBookAnnotations(book: BookItem) = handleExportBookAnnotations(book)

    fun signInToCloud(provider: CloudProvider) = handleSignInToCloud(provider)
    fun onCloudAuthComplete(
        provider: CloudProvider,
        accessToken: String,
        displayName: String? = null,
        email: String? = null,
        photoUrl: String? = null,
        refreshToken: String? = null,
        accessTokenExpiresAt: Long? = null,
        accountId: String? = null,
        serverUrl: String? = null,
        usedBytes: Long? = null,
        totalBytes: Long? = null
    ) = handleOnCloudAuthComplete(
        provider = provider,
        accessToken = accessToken,
        displayName = displayName,
        email = email,
        photoUrl = photoUrl,
        refreshToken = refreshToken,
        accessTokenExpiresAt = accessTokenExpiresAt,
        accountId = accountId,
        serverUrl = serverUrl,
        usedBytes = usedBytes,
        totalBytes = totalBytes
    )

    fun signOutFromCloud() = handleSignOutFromCloud()
    fun removeCloudAccount(accountId: String) = handleRemoveCloudAccount(accountId)
    fun syncNow() = handleSyncNow()
    fun syncCloudAccount(accountId: String) = handleSyncCloudAccount(accountId)
    fun restoreCloudAccount(accountId: String) = handleRestoreCloudAccount(accountId)
    fun clearSyncError() = handleClearSyncError()
    fun setCloudAccountBackupScope(accountId: String, scope: CloudBackupScope, enabled: Boolean) =
        handleSetCloudAccountBackupScope(accountId, scope, enabled)
    fun createBackupSnapshot(label: String) = handleCreateBackupSnapshot(label)
    fun restoreBackupSnapshot(snapshotId: String, label: String) = handleRestoreBackupSnapshot(snapshotId, label)
    fun deleteBackupSnapshot(snapshotId: String, label: String) = handleDeleteBackupSnapshot(snapshotId, label)
    fun consumeCloudMessage() = consumeCloudMessageInternal()
    fun clearAnalytics() = handleClearAnalytics()
}
