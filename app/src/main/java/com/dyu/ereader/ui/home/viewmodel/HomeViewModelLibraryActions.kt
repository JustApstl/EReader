package com.dyu.ereader.ui.home.viewmodel

import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.dyu.ereader.core.logging.AppLogger
import com.dyu.ereader.data.model.export.ExportData
import com.dyu.ereader.data.model.export.ExportFormat
import com.dyu.ereader.data.model.export.ExportOptions
import com.dyu.ereader.data.model.export.ExportedBookmark
import com.dyu.ereader.data.model.export.ExportedHighlight
import com.dyu.ereader.data.model.export.ExportedNote
import com.dyu.ereader.data.model.library.BookItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal fun HomeViewModel.handleOnLibraryAccessGranted(uri: Uri) {
    homeUiStateFlow.update {
        it.copy(
            libraryUri = uri.toString(),
            isScanning = true,
            errorMessage = null
        )
    }
    viewModelScope.launch {
        libraryRepo.onLibraryAccessGranted(uri)
        prefsStore.setLibraryTreeUri(uri.toString())
        handleRefreshLibrary(uri.toString())
    }
}

internal fun HomeViewModel.handleRevokeLibraryAccess() {
    viewModelScope.launch {
        val uriString = homeUiStateFlow.value.libraryUri
        if (uriString.isNullOrBlank()) {
            prefsStore.setLibraryTreeUri("")
            return@launch
        }
        libraryRepo.revokeLibraryAccess(uriString)
        prefsStore.setLibraryTreeUri("")
    }
}

internal fun HomeViewModel.handleRefreshLibrary(forcedUri: String? = null) {
    val uriString = forcedUri ?: homeUiStateFlow.value.libraryUri ?: return
    val treeUri = Uri.parse(uriString)

    if (scanJobState?.isActive == true) {
        scanJobState?.cancel()
    }
    homeUiStateFlow.update { it.copy(isScanning = true, errorMessage = null) }
    scanJobState = viewModelScope.launch {
        runCatching { libraryRepo.scanBooks(treeUri) }
            .onSuccess {
                homeUiStateFlow.update { it.copy(isScanning = false) }
            }
            .onFailure { error ->
                if (error !is CancellationException && error.cause !is CancellationException) {
                    AppLogger.log("Library Scan Failed", error)
                }
                homeUiStateFlow.update { it.copy(isScanning = false) }
            }
    }
}

internal fun HomeViewModel.handleImportSettings(json: String) {
    viewModelScope.launch {
        cloudRepo.createSnapshot("Before import")
        cloudRepo.importBackupJson(json)
            .onSuccess {
                homeUiStateFlow.update { it.copy(libraryUri = prefsStore.libraryTreeUriFlow.first()) }
                handleRefreshLibrary()
                cloudMessageFlow.value = "Imported backup and saved a snapshot first"
            }
            .onFailure { error ->
                cloudMessageFlow.value = "Import failed: ${error.localizedMessage ?: "Unknown error"}"
            }
    }
}

internal fun HomeViewModel.consumeLibraryMessageInternal() {
    libraryMessageFlow.value = null
}

internal fun HomeViewModel.consumePendingExportUriInternal() {
    pendingExportUriFlow.value = null
}

internal fun HomeViewModel.handleDeleteBook(book: BookItem) {
    viewModelScope.launch {
        libraryRepo.deleteBook(book)
            .onSuccess {
                prefsStore.removeBookFromAllCollections(book.id)
                libraryMessageFlow.value = "Deleted \"${book.title}\""
            }
            .onFailure { error ->
                libraryMessageFlow.value = "Delete failed: ${error.localizedMessage ?: "Unknown error"}"
            }
    }
}

internal fun HomeViewModel.handleCreateLibraryCollection(name: String) {
    viewModelScope.launch {
        val created = prefsStore.createLibraryCollection(name)
        libraryMessageFlow.value = if (created) {
            "Created collection \"$name\""
        } else {
            "Collection already exists"
        }
    }
}

internal fun HomeViewModel.handleCreateLibraryCollection(name: String, book: BookItem) {
    viewModelScope.launch {
        val normalized = name.trim()
        if (normalized.isBlank()) return@launch
        val created = prefsStore.createLibraryCollection(normalized)
        prefsStore.toggleBookInCollection(normalized, book.id)
        libraryMessageFlow.value = if (created) {
            "Created \"$normalized\" and added \"${book.title}\""
        } else {
            "Added \"${book.title}\" to $normalized"
        }
    }
}

internal fun HomeViewModel.handleToggleBookInCollection(collectionName: String, book: BookItem) {
    viewModelScope.launch {
        val added = prefsStore.toggleBookInCollection(collectionName, book.id)
        libraryMessageFlow.value = if (added) {
            "Added \"${book.title}\" to $collectionName"
        } else {
            "Removed \"${book.title}\" from $collectionName"
        }
    }
}

internal fun HomeViewModel.handleDeleteLibraryCollection(collectionName: String) {
    viewModelScope.launch {
        prefsStore.deleteLibraryCollection(collectionName)
        libraryMessageFlow.value = "Deleted collection \"$collectionName\""
    }
}

internal fun HomeViewModel.handleExportBookAnnotations(book: BookItem) {
    viewModelScope.launch {
        val highlights = bookDatabase.highlightDao().getHighlightsForBook(book.id).first()
        val bookmarks = bookDatabase.bookmarkDao().getBookmarksForBook(book.id).first()
        val notes = bookDatabase.marginNoteDao().getMarginalNotesForBook(book.id).first()
        if (highlights.isEmpty() && bookmarks.isEmpty() && notes.isEmpty()) {
            libraryMessageFlow.value = "No highlights, bookmarks, or notes to export for \"${book.title}\""
            return@launch
        }
        val exportData = ExportData(
            bookTitle = book.title,
            bookAuthor = book.author,
            exportDate = System.currentTimeMillis(),
            format = ExportFormat.MARKDOWN,
            highlights = highlights.map { highlight ->
                ExportedHighlight(
                    text = highlight.selectedText,
                    color = highlight.color,
                    chapter = highlight.chapterAnchor,
                    context = highlight.selectionJson,
                    timestamp = highlight.createdAt
                )
            },
            bookmarks = bookmarks.map { bookmark ->
                ExportedBookmark(
                    title = bookmark.title ?: "Bookmark",
                    chapter = bookmark.chapterAnchor,
                    note = bookmark.note,
                    timestamp = bookmark.createdAt
                )
            },
            notes = notes.map { note ->
                ExportedNote(
                    content = note.content,
                    chapter = note.chapterAnchor,
                    position = note.position,
                    timestamp = note.createdAt
                )
            }
        )
        val exportUri = exportRepo.exportData(
            data = exportData,
            options = ExportOptions(format = ExportFormat.MARKDOWN)
        )
        if (exportUri == null) {
            libraryMessageFlow.value = "Export failed for \"${book.title}\""
        } else {
            pendingExportUriFlow.value = exportUri
            libraryMessageFlow.value = "Prepared export for \"${book.title}\""
        }
    }
}
