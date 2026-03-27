package com.dyu.ereader.ui.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.dyu.ereader.data.model.browse.BrowseBook
import com.dyu.ereader.data.model.browse.BrowseCatalog
import com.dyu.ereader.data.model.browse.BrowseDownloadOption
import com.dyu.ereader.data.model.browse.BrowseDownloadState
import com.dyu.ereader.data.model.browse.BrowseDownloadTask
import com.dyu.ereader.data.model.browse.BrowseTransferProgress
import com.dyu.ereader.data.model.library.BookItem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

internal fun HomeViewModel.handleLoadBrowseCatalog(catalog: BrowseCatalog, updateCurrentUrl: Boolean = true) {
    if (updateCurrentUrl) {
        browseCurrentCatalogUrlFlow.value = catalog.url
    }
    viewModelScope.launch {
        browseRepo.loadCatalog(catalog)
    }
}

internal fun HomeViewModel.handleLoadBrowseNextPage() {
    viewModelScope.launch {
        browseRepo.loadNextPage()
    }
}

internal fun HomeViewModel.handleClearBrowseFeed() {
    browseRepo.clearFeed()
}

internal fun HomeViewModel.handleSetBrowseCurrentCatalogUrl(url: String) {
    browseCurrentCatalogUrlFlow.value = url
}

internal fun HomeViewModel.handleRefreshBrowseCatalogHealth() {
    viewModelScope.launch {
        browseRepo.refreshCatalogHealth()
    }
}

internal fun HomeViewModel.handleSearchBrowse(query: String) {
    viewModelScope.launch {
        browseRepo.searchCatalog(query)
    }
}

internal fun HomeViewModel.handleAddBrowseSavedSearch(query: String) {
    viewModelScope.launch {
        prefsStore.addBrowseSavedSearch(query)
    }
}

internal suspend fun HomeViewModel.getBrowseFormatPreferenceInternal(url: String): String? =
    prefsStore.getBrowseFormatPreference(url)

internal fun HomeViewModel.handleSetBrowseFormatPreference(url: String, format: String) {
    viewModelScope.launch {
        prefsStore.setBrowseFormatPreference(url, format)
    }
}

internal suspend fun HomeViewModel.getBrowseLastVisitInternal(url: String): Long? =
    prefsStore.getBrowseLastVisit(url)

internal fun HomeViewModel.handleSetBrowseLastVisit(url: String, timestamp: Long) {
    viewModelScope.launch {
        prefsStore.setBrowseLastVisit(url, timestamp)
    }
}

internal suspend fun HomeViewModel.resolveBrowseDownloadOptionsInternal(book: BrowseBook): List<BrowseDownloadOption> =
    browseRepo.resolveDownloadOptions(book)

internal fun HomeViewModel.isBrowseBookAlreadyInLibraryInternal(book: BrowseBook): Boolean =
    findLibraryMatchInternal(book) != null

internal fun HomeViewModel.queuedBrowseDownloadInternal(book: BrowseBook): BrowseDownloadTask? {
    val targetKey = downloadIdentityKeyInternal(book = book)
    return browseDownloadQueueFlow.value.firstOrNull { task ->
        task.state != BrowseDownloadState.COMPLETED &&
            task.state != BrowseDownloadState.CANCELED &&
            downloadIdentityKeyInternal(task = task) == targetKey
    }
}

internal fun HomeViewModel.handleAddBrowseCatalog(catalog: BrowseCatalog) {
    browseRepo.addCatalog(catalog)
}

internal fun HomeViewModel.handleRemoveBrowseCatalog(catalogId: String) {
    browseRepo.removeCatalog(catalogId)
}

internal fun HomeViewModel.handleDownloadBrowseBook(book: BrowseBook) {
    enqueueDownloadInternal(
        BrowseDownloadTask(
            id = "${book.id}_${System.currentTimeMillis()}",
            title = book.title,
            author = book.author,
            format = book.format,
            coverUrl = book.coverUrl,
            book = book
        )
    )
}

internal fun HomeViewModel.handleDownloadFromDirectUrl(url: String) {
    val trimmed = url.trim()
    if (trimmed.isBlank()) return
    enqueueDownloadInternal(
        BrowseDownloadTask(
            id = "direct_${trimmed.hashCode()}_${System.currentTimeMillis()}",
            title = "Direct download",
            author = "Unknown Author",
            format = "UNKNOWN",
            coverUrl = null,
            directUrl = trimmed
        )
    )
}

internal fun HomeViewModel.handleCancelBrowseDownload(taskId: String) {
    val task = browseDownloadQueueFlow.value.firstOrNull { it.id == taskId } ?: return
    if (task.state == BrowseDownloadState.DOWNLOADING) {
        browseRepo.cancelDownload()
    }
    browseDownloadQueueFlow.update { list ->
        list.map { item ->
            if (item.id == taskId) item.copy(state = BrowseDownloadState.CANCELED) else item
        }
    }
}

internal fun HomeViewModel.handlePauseBrowseDownload(taskId: String) {
    val task = browseDownloadQueueFlow.value.firstOrNull { it.id == taskId } ?: return
    if (task.state == BrowseDownloadState.DOWNLOADING) {
        browseRepo.cancelDownload()
    }
    browseDownloadQueueFlow.update { list ->
        list.map { item ->
            if (item.id == taskId) item.copy(state = BrowseDownloadState.PAUSED, error = null) else item
        }
    }
}

internal fun HomeViewModel.handleResumeBrowseDownload(taskId: String) {
    browseDownloadQueueFlow.update { list ->
        list.map { item ->
            if (item.id == taskId) {
                item.copy(
                    state = BrowseDownloadState.QUEUED,
                    error = null,
                    speedBytesPerSecond = null
                )
            } else {
                item
            }
        }
    }
    processDownloadQueueInternal()
}

internal fun HomeViewModel.handleRetryBrowseDownload(taskId: String) {
    browseDownloadQueueFlow.update { list ->
        list.map { item ->
            if (item.id == taskId) {
                item.copy(
                    state = BrowseDownloadState.QUEUED,
                    progress = 0f,
                    downloadedBytes = 0L,
                    totalBytes = null,
                    speedBytesPerSecond = null,
                    error = null
                )
            } else {
                item
            }
        }
    }
    processDownloadQueueInternal()
}

internal fun HomeViewModel.consumeBrowseDownloadMessageInternal() {
    browseDownloadMessageFlow.value = null
}

private fun HomeViewModel.enqueueDownloadInternal(task: BrowseDownloadTask) {
    val preparedTask = enrichDownloadTaskInternal(task)
    if (preparedTask.alreadyInLibrary) {
        browseDownloadMessageFlow.value = "\"${preparedTask.title}\" is already in your library"
        return
    }
    val existing = browseDownloadQueueFlow.value.firstOrNull { queued ->
        queued.state != BrowseDownloadState.COMPLETED &&
            queued.state != BrowseDownloadState.FAILED &&
            queued.state != BrowseDownloadState.CANCELED &&
            downloadIdentityKeyInternal(task = queued) == downloadIdentityKeyInternal(task = preparedTask)
    }
    if (existing != null) {
        browseDownloadMessageFlow.value = when (existing.state) {
            BrowseDownloadState.DOWNLOADING -> "\"${preparedTask.title}\" is already downloading"
            BrowseDownloadState.PAUSED -> "\"${preparedTask.title}\" is paused in your queue"
            BrowseDownloadState.QUEUED -> "\"${preparedTask.title}\" is already queued"
            else -> "\"${preparedTask.title}\" is already in your downloads"
        }
        return
    }
    browseDownloadQueueFlow.update { it + preparedTask }
    processDownloadQueueInternal()
}

private fun HomeViewModel.processDownloadQueueInternal() {
    if (downloadQueueJobState?.isActive == true) return
    downloadQueueJobState = viewModelScope.launch {
        while (true) {
            val next = browseDownloadQueueFlow.value.firstOrNull { it.state == BrowseDownloadState.QUEUED } ?: break
            browseDownloadQueueFlow.update { list ->
                list.map { item ->
                    if (item.id == next.id) {
                        item.copy(
                            state = BrowseDownloadState.DOWNLOADING,
                            progress = item.progress.coerceIn(0f, 100f),
                            error = null,
                            speedBytesPerSecond = null
                        )
                    } else {
                        item
                    }
                }
            }

            val result = if (next.book != null) {
                browseRepo.downloadBookToAppStorage(next.book) { progress ->
                    updateDownloadProgressInternal(next.id, progress)
                }
            } else {
                val directUrl = next.directUrl ?: ""
                browseRepo.downloadDirectUrl(directUrl) { progress ->
                    updateDownloadProgressInternal(next.id, progress)
                }
            }

            val stillDownloading = browseDownloadQueueFlow.value.firstOrNull { it.id == next.id }?.state == BrowseDownloadState.DOWNLOADING
            if (!stillDownloading) continue

            result
                .onSuccess { file ->
                    val libraryUri = prefsStore.libraryTreeUriFlow.first()
                    val importResult = libraryRepo.importBookToLibrary(file, libraryUri)
                    importResult
                        .onSuccess { book ->
                            prefsStore.addNewDownload(book.id)
                            browseDownloadMessageFlow.value = "Imported \"${next.title}\" into your library"
                        }
                        .onFailure { error ->
                            browseDownloadMessageFlow.value = "Downloaded but import failed: ${error.localizedMessage ?: "Unknown error"}"
                        }
                    browseDownloadQueueFlow.update { list ->
                        list.map { item ->
                            if (item.id == next.id) {
                                item.copy(
                                    state = BrowseDownloadState.COMPLETED,
                                    progress = 100f,
                                    downloadedBytes = file.length(),
                                    totalBytes = item.totalBytes ?: file.length(),
                                    speedBytesPerSecond = null,
                                    alreadyInLibrary = true
                                )
                            } else {
                                item
                            }
                        }
                    }
                }
                .onFailure { error ->
                    browseDownloadMessageFlow.value = "Download failed: ${error.localizedMessage ?: "Unknown error"}"
                    browseDownloadQueueFlow.update { list ->
                        list.map { item ->
                            if (item.id == next.id) {
                                item.copy(
                                    state = BrowseDownloadState.FAILED,
                                    error = error.localizedMessage,
                                    speedBytesPerSecond = null
                                )
                            } else {
                                item
                            }
                        }
                    }
                }
        }
    }
}

private fun HomeViewModel.updateDownloadProgressInternal(taskId: String, progress: BrowseTransferProgress) {
    browseDownloadQueueFlow.update { list ->
        list.map { item ->
            if (item.id == taskId && item.state == BrowseDownloadState.DOWNLOADING) {
                item.copy(
                    progress = progress.progress.coerceIn(0f, 100f),
                    downloadedBytes = progress.downloadedBytes,
                    totalBytes = progress.totalBytes,
                    speedBytesPerSecond = progress.speedBytesPerSecond
                )
            } else {
                item
            }
        }
    }
}

private fun HomeViewModel.enrichDownloadTaskInternal(task: BrowseDownloadTask): BrowseDownloadTask {
    val alreadyInLibrary = task.book?.let(::isBrowseBookAlreadyInLibraryInternal)
        ?: task.directUrl?.let { directUrl ->
            homeUiStateFlow.value.allBooks.any { libraryBook ->
                libraryBook.uri.equals(directUrl, ignoreCase = true)
            }
        } ?: false
    return task.copy(alreadyInLibrary = alreadyInLibrary)
}

private fun HomeViewModel.findLibraryMatchInternal(book: BrowseBook): BookItem? {
    val targetTitle = normalizeTextTokenInternal(book.title)
    val targetAuthor = normalizeTextTokenInternal(book.author)
    return homeUiStateFlow.value.allBooks.firstOrNull { libraryBook ->
        val sameTitle = normalizeTextTokenInternal(libraryBook.title) == targetTitle
        val sameAuthor = targetAuthor.isBlank() ||
            targetAuthor == normalizeTextTokenInternal(libraryBook.author) ||
            targetAuthor == normalizeTextTokenInternal("Unknown Author")
        sameTitle && sameAuthor
    }
}

private fun HomeViewModel.downloadIdentityKeyInternal(
    task: BrowseDownloadTask? = null,
    book: BrowseBook? = null
): String {
    val taskBook = task?.book
    val resolvedBook = book ?: taskBook
    val directUrl = task?.directUrl?.trim()?.lowercase(Locale.ROOT)
    if (!directUrl.isNullOrBlank()) return "url:$directUrl"
    if (resolvedBook != null) {
        val bookId = resolvedBook.id.trim().lowercase(Locale.ROOT).takeIf { it.isNotBlank() }
        if (!bookId.isNullOrBlank()) return "book:$bookId"
        return "book:${normalizeTextTokenInternal(resolvedBook.title)}|${normalizeTextTokenInternal(resolvedBook.author)}|${resolvedBook.format.lowercase(Locale.ROOT)}"
    }
    return "task:${normalizeTextTokenInternal(task?.title.orEmpty())}|${normalizeTextTokenInternal(task?.author.orEmpty())}|${task?.format?.lowercase(Locale.ROOT).orEmpty()}"
}

private fun normalizeTextTokenInternal(value: String): String {
    return value
        .trim()
        .lowercase(Locale.ROOT)
        .replace("[^a-z0-9]+".toRegex(), " ")
        .trim()
}
