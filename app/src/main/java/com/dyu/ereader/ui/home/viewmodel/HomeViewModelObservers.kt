package com.dyu.ereader.ui.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.dyu.ereader.data.model.library.BookCollectionShelf
import com.dyu.ereader.data.model.reader.ReaderControl
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.ui.home.state.SortOrder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun HomeViewModel.observeHomePreferencesInternal() {
    viewModelScope.launch {
        combine(
            prefsStore.libraryTreeUriFlow,
            prefsStore.showBookTypeFlow,
            prefsStore.sortOrderFlow,
            prefsStore.hideStatusBarFlow,
            prefsStore.showRecentReadingFlow,
            prefsStore.showFavoritesFlow,
            prefsStore.showGenresFlow,
            prefsStore.gridColumnsFlow,
            prefsStore.animationsEnabledFlow,
            prefsStore.hapticsEnabledFlow,
            prefsStore.textScrollerEnabledFlow,
            prefsStore.hideBetaFeaturesFlow,
            prefsStore.developerOptionsEnabledFlow,
            prefsStore.appTextScaleFlow,
            prefsStore.showReaderSearchFlow,
            prefsStore.showReaderListenFlow,
            prefsStore.showReaderAccessibilityFlow,
            prefsStore.showReaderAnalyticsFlow,
            prefsStore.showReaderExportFlow,
            prefsStore.readerControlOrderFlow,
            prefsStore.newDownloadIdsFlow,
            prefsStore.readerSettingsFlow,
            prefsStore.lastLocalBackupExportAtFlow,
            prefsStore.lastLocalBackupImportAtFlow,
            prefsStore.notificationsEnabledFlow,
            prefsStore.updateNotificationsEnabledFlow,
            prefsStore.readingReminderEnabledFlow,
            prefsStore.readingReminderHourFlow,
            prefsStore.readingReminderMinuteFlow
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
            val hapticsEnabled = params[9] as Boolean
            val textScrollerEnabled = params[10] as Boolean
            val hideBetaFeatures = params[11] as Boolean
            val developerOptionsEnabled = params[12] as Boolean
            val appTextScale = params[13] as Float
            val showReaderSearch = params[14] as Boolean
            val showReaderListen = params[15] as Boolean
            val showReaderAccessibility = params[16] as Boolean
            val showReaderAnalytics = params[17] as Boolean
            val showReaderExport = params[18] as Boolean
            val readerControlOrder = (params[19] as? List<*>)
                ?.mapNotNull { it as? ReaderControl }
                ?.takeIf { it.isNotEmpty() }
                ?: ReaderControl.defaultOrder()
            val newDownloadIds = params[20] as? Set<*> ?: emptySet<String>()
            val readerSettings = params[21] as ReaderSettings
            val lastLocalBackupExportAt = params[22] as Long?
            val lastLocalBackupImportAt = params[23] as Long?
            val notificationsEnabled = params[24] as Boolean
            val updateNotificationsEnabled = params[25] as Boolean
            val readingReminderEnabled = params[26] as Boolean
            val readingReminderHour = params[27] as Int
            val readingReminderMinute = params[28] as Int

            analyticsRepo.enableAnalytics(showReaderAnalytics)

            val resolvedSortOrder = SortOrder.entries.find { it.name == sortOrder } ?: SortOrder.TITLE
            homeUiStateFlow.update {
                it.copy(
                    libraryUri = uri,
                    sortOrder = resolvedSortOrder,
                    display = it.display.copy(
                        showBookType = showType,
                        hideStatusBar = hideStatusBar,
                        showRecentReading = showRecent,
                        showFavorites = showFavorites,
                        showGenres = showGenres,
                        gridColumns = gridColumns,
                        animationsEnabled = animationsEnabled,
                        hapticsEnabled = hapticsEnabled,
                        textScrollerEnabled = textScrollerEnabled,
                        hideBetaFeatures = hideBetaFeatures,
                        developerOptionsEnabled = developerOptionsEnabled,
                        appTextScale = appTextScale,
                        showReaderSearch = showReaderSearch,
                        showReaderListen = showReaderListen,
                        showReaderAccessibility = showReaderAccessibility,
                        showReaderAnalytics = showReaderAnalytics,
                        showReaderExport = showReaderExport,
                        readerControlOrder = readerControlOrder,
                        notificationsEnabled = notificationsEnabled,
                        updateNotificationsEnabled = updateNotificationsEnabled,
                        readingReminderEnabled = readingReminderEnabled,
                        readingReminderHour = readingReminderHour,
                        readingReminderMinute = readingReminderMinute
                    ),
                    newDownloadIds = newDownloadIds.filterIsInstance<String>().toSet(),
                    readerSettings = readerSettings,
                    lastLocalBackupExportAt = lastLocalBackupExportAt,
                    lastLocalBackupImportAt = lastLocalBackupImportAt
                )
            }
            if (!uri.isNullOrBlank()) {
                handleRefreshLibrary()
            }
            applyFiltersAndSortInternal()
        }.collectLatest { }
    }
}

internal fun HomeViewModel.observeLibraryContentInternal() {
    viewModelScope.launch {
        combine(
            libraryRepo.getBooksFlow(),
            prefsStore.libraryCollectionsFlow
        ) { books, collections ->
            books to collections
        }.collectLatest { (books, collections) ->
            val booksWithProgress = books.map { book ->
                val progress = prefsStore.bookProgressFlow(book.uri).first()
                book.copy(progress = progress)
            }
            val allGenres = books.flatMap { it.genres }.distinct().sorted()
            val allLanguages = books.mapNotNull { it.language?.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
            val allYears = books.mapNotNull { it.year?.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
            val allCountries = books.flatMap { it.countries }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinct()
                .sorted()
            val collectionShelves = collections.mapNotNull { collection ->
                val shelfBooks = collection.bookIds.mapNotNull { id ->
                    booksWithProgress.firstOrNull { it.id == id }
                }
                shelfBooks.takeIf { it.isNotEmpty() }?.let {
                    BookCollectionShelf(
                        name = collection.name,
                        books = it,
                        createdAt = collection.createdAt
                    )
                }
            }

            homeUiStateFlow.update {
                it.copy(
                    allBooks = booksWithProgress,
                    availableGenres = allGenres,
                    availableLanguages = allLanguages,
                    availableYears = allYears,
                    availableCountries = allCountries,
                    collections = collectionShelves
                )
            }
            applyFiltersAndSortInternal()
        }
    }
}
