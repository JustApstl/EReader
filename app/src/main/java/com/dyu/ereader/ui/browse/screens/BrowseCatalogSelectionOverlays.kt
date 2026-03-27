package com.dyu.ereader.ui.browse.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.dyu.ereader.data.repository.browse.normalizeBrowseCatalogUrl
import com.dyu.ereader.data.model.browse.BrowseBook
import com.dyu.ereader.data.model.browse.BrowseCatalog
import com.dyu.ereader.data.model.browse.BrowseDownloadOption
import com.dyu.ereader.data.model.browse.BrowseDownloadState
import com.dyu.ereader.data.model.browse.BrowseDownloadTask
import com.dyu.ereader.ui.browse.components.AddSourceDialog
import com.dyu.ereader.ui.browse.sheets.BrowseBookDetailSheet
import com.dyu.ereader.ui.browse.sheets.BrowseDownloadOptionsDialog
import com.dyu.ereader.ui.home.viewmodel.HomeViewModel

@Composable
internal fun BrowseCatalogAddSourceOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    onAddCatalog: (BrowseCatalog) -> Unit
) {
    if (!visible) return
    AddSourceDialog(
        onDismiss = onDismiss,
        onAdd = { title, url, username, password, apiKey ->
            val normalizedUrl = normalizeBrowseCatalogUrl(url) ?: return@AddSourceDialog
            onAddCatalog(
                BrowseCatalog(
                    id = normalizedUrl.hashCode().toString(),
                    title = title,
                    url = normalizedUrl,
                    description = "Custom OPDS source",
                    icon = "https://www.google.com/s2/favicons?domain_url=$normalizedUrl&sz=64",
                    isCustom = true,
                    username = username,
                    password = password,
                    apiKey = apiKey
                )
            )
            onDismiss()
        }
    )
}

@Composable
internal fun BrowseCatalogSelectionOverlays(
    selectedBook: BrowseBook?,
    currentUrl: String,
    preferredFormat: String?,
    downloadQueue: List<BrowseDownloadTask>,
    liquidGlassEnabled: Boolean,
    viewModel: HomeViewModel,
    onSelectedBookChange: (BrowseBook?) -> Unit,
    onPreferredFormatChange: (String?) -> Unit
) {
    var showDownloadOptions by remember(selectedBook?.id) { mutableStateOf(false) }
    var resolvedOptions by remember(selectedBook?.id) { mutableStateOf<List<BrowseDownloadOption>>(emptyList()) }
    var isResolvingOptions by remember(selectedBook?.id) { mutableStateOf(false) }

    LaunchedEffect(selectedBook) {
        val book = selectedBook
        if (book == null) {
            resolvedOptions = emptyList()
            isResolvingOptions = false
            return@LaunchedEffect
        }
        isResolvingOptions = true
        val initial = book.downloadOptions
        resolvedOptions = initial
        val resolved = viewModel.resolveBrowseDownloadOptions(book)
        resolvedOptions = if (resolved.isNotEmpty()) resolved else initial
        isResolvingOptions = false
    }

    selectedBook?.let { book ->
        val queuedTask = remember(book, downloadQueue) { viewModel.queuedBrowseDownload(book) }
        val queuedMessage = remember(queuedTask) {
            when (queuedTask?.state) {
                BrowseDownloadState.QUEUED -> "This book is already queued for download."
                BrowseDownloadState.DOWNLOADING -> "This book is downloading now."
                BrowseDownloadState.PAUSED -> "This book is paused in your downloads."
                BrowseDownloadState.COMPLETED -> "This book has already finished downloading."
                else -> null
            }
        }
        BrowseBookDetailSheet(
            book = book,
            downloadOptions = resolvedOptions,
            isResolvingOptions = isResolvingOptions,
            alreadyInLibrary = viewModel.isBrowseBookAlreadyInLibrary(book),
            queuedDownloadLabel = queuedMessage,
            liquidGlassEnabled = liquidGlassEnabled,
            onDismiss = {
                onSelectedBookChange(null)
                showDownloadOptions = false
            },
            onDownload = {
                if (isResolvingOptions) return@BrowseBookDetailSheet
                if (resolvedOptions.size <= 1) {
                    val option = resolvedOptions.firstOrNull()
                    val downloadBook = if (option != null) {
                        book.copy(downloadUrl = option.url, format = option.format)
                    } else {
                        book
                    }
                    viewModel.downloadBrowseBook(downloadBook)
                    onPreferredFormatChange(option?.format ?: preferredFormat)
                    if (option != null && currentUrl.isNotBlank()) {
                        viewModel.setBrowseFormatPreference(currentUrl, option.format)
                    }
                    onSelectedBookChange(null)
                } else {
                    val preferred = preferredFormat?.lowercase()
                    val preferredOption = preferred?.let { target ->
                        resolvedOptions.firstOrNull { option ->
                            option.format.equals(target, ignoreCase = true) ||
                                option.label?.contains(target, ignoreCase = true) == true
                        }
                    }
                    if (preferredOption != null) {
                        viewModel.downloadBrowseBook(
                            book.copy(
                                downloadUrl = preferredOption.url,
                                format = preferredOption.format
                            )
                        )
                        if (currentUrl.isNotBlank()) {
                            viewModel.setBrowseFormatPreference(currentUrl, preferredOption.format)
                        }
                        onSelectedBookChange(null)
                    } else {
                        showDownloadOptions = true
                    }
                }
            }
        )
    }

    if (showDownloadOptions && !isResolvingOptions && selectedBook != null && resolvedOptions.isNotEmpty()) {
        val activeBook = selectedBook
        BrowseDownloadOptionsDialog(
            title = "Choose format",
            options = resolvedOptions,
            onSelect = { option ->
                viewModel.downloadBrowseBook(activeBook.copy(downloadUrl = option.url, format = option.format))
                onPreferredFormatChange(option.format)
                if (currentUrl.isNotBlank()) {
                    viewModel.setBrowseFormatPreference(currentUrl, option.format)
                }
                showDownloadOptions = false
                onSelectedBookChange(null)
            },
            onDismiss = { showDownloadOptions = false }
        )
    }
}
