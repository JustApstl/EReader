package com.dyu.ereader.ui.browse.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.browse.BrowseBook
import com.dyu.ereader.data.model.browse.BrowseDownloadOption
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.browse.components.BrowseCoverImage
import com.dyu.ereader.ui.components.books.BookAboutDetailRow
import com.dyu.ereader.ui.components.books.BookAboutTopBar
import com.dyu.ereader.ui.components.books.BookMetadataChip
import com.dyu.ereader.ui.components.books.BookAboutSection
import com.dyu.ereader.ui.components.books.BookMetadataHtmlText
import com.dyu.ereader.ui.components.books.BookPrimaryActionButton
import com.dyu.ereader.ui.components.dialogs.appDialogBorderColor
import com.dyu.ereader.ui.components.dialogs.appDialogCardColor
import com.dyu.ereader.ui.components.dialogs.appDialogContainerColor
import com.dyu.ereader.core.locale.displayLanguageName
import com.dyu.ereader.core.locale.extractPublishedYear
import java.text.NumberFormat

@Composable
internal fun BrowseBookDetailSheet(
    book: BrowseBook,
    downloadOptions: List<BrowseDownloadOption>,
    isResolvingOptions: Boolean,
    alreadyInLibrary: Boolean,
    queuedDownloadLabel: String?,
    liquidGlassEnabled: Boolean,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    val primaryOption = remember(book.id, downloadOptions) { downloadOptions.firstOrNull() }
    val languageLabel = remember(book.languages) {
        book.languages.mapNotNull { displayLanguageName(it) }.distinct().ifEmpty {
            book.languages.map { it.trim() }.filter { it.isNotBlank() }
        }
    }
    val downloadLabel = remember(primaryOption) { "Download" }
    val formatBadges = remember(downloadOptions, book.format) {
        val fromOptions = downloadOptions.mapNotNull { it.format.trim().takeIf { format -> format.isNotBlank() } }
        val fallback = book.format.trim().takeIf { it.isNotBlank() }?.let { listOf(it) }.orEmpty()
        (fromOptions + fallback).map { it.uppercase() }.distinct()
    }
    val fileOptions = remember(downloadOptions, book.format) {
        if (downloadOptions.isNotEmpty()) {
            downloadOptions.map { option ->
                val label = option.format.uppercase()
                val sizeLabel = option.sizeBytes?.let { formatFileSize(it) } ?: "Unknown"
                label to sizeLabel
            }.distinctBy { it.first }
        } else {
            val fallback = book.format.trim().takeIf { it.isNotBlank() }?.uppercase()
            fallback?.let { listOf(it to "Unknown") }.orEmpty()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    BookAboutTopBar(
                        title = "About",
                        onBack = onDismiss
                    )
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(padding)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    BookAboutSection(liquidGlassEnabled = liquidGlassEnabled) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)),
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(180.dp)
                            ) {
                                BrowseCoverImage(
                                    coverUrl = book.coverUrl,
                                    title = book.title,
                                    author = book.author,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = book.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!book.author.equals("Unknown Author", ignoreCase = true)) {
                                        Text(
                                            text = book.author,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                if (formatBadges.isNotEmpty()) {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(end = 8.dp)
                                    ) {
                                        items(formatBadges) { badge ->
                                            BookMetadataChip(label = badge)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    BookAboutSection(
                        title = "Description",
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        liquidGlassEnabled = liquidGlassEnabled
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            val summary = book.summary?.ifBlank { null }
                            if (summary == null) {
                                Text(
                                    text = "No description available.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                BookMetadataHtmlText(summary, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }

                    if (fileOptions.isNotEmpty() || isResolvingOptions) {
                        BookAboutSection(
                            title = "File Types",
                            icon = Icons.Rounded.Info,
                            liquidGlassEnabled = liquidGlassEnabled
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (isResolvingOptions) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(16.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Loading formats…",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (fileOptions.isNotEmpty()) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        fileOptions.forEach { (label, sizeLabel) ->
                                            BookAboutDetailRow(
                                                icon = Icons.Rounded.Download,
                                                label = label,
                                                value = sizeLabel
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val detailItems = remember(book, downloadOptions, languageLabel) {
                        val publishedYear = extractPublishedYear(book.published)
                        buildList {
                            book.downloads?.let { count ->
                                add(DetailItem("Downloads", formatDownloads(count), Icons.Rounded.Download))
                            }
                            if (languageLabel.isNotEmpty()) {
                                add(DetailItem("Language", languageLabel.joinToString(", "), Icons.Rounded.Language))
                            }
                            if (!book.publisher.isNullOrBlank()) add(DetailItem("Publisher", book.publisher, Icons.Rounded.Business))
                            if (!publishedYear.isNullOrBlank()) add(DetailItem("Published", publishedYear, Icons.Rounded.Event))
                            if (!book.rights.isNullOrBlank()) add(DetailItem("Rights", book.rights, Icons.Rounded.Gavel))
                        }
                    }
                    if (detailItems.isNotEmpty()) {
                        BookAboutSection(
                            title = "About",
                            icon = Icons.Rounded.Info,
                            liquidGlassEnabled = liquidGlassEnabled
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                detailItems.forEach { item ->
                                    BookAboutDetailRow(item.icon, item.label, item.value)
                                }
                            }
                        }
                    }

                    if (book.subjects.isNotEmpty()) {
                        BookAboutSection(
                            title = "Topics",
                            icon = Icons.Rounded.Info,
                            liquidGlassEnabled = liquidGlassEnabled
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    book.subjects.forEach { subject ->
                                        BookMetadataChip(label = subject)
                                    }
                                }
                            }
                        }
                    }

                    val statusMessage = when {
                        alreadyInLibrary -> "This book is already in your library."
                        !queuedDownloadLabel.isNullOrBlank() -> queuedDownloadLabel
                        else -> null
                    }
                    if (!statusMessage.isNullOrBlank()) {
                        BookAboutSection(
                            title = "Status",
                            icon = Icons.Rounded.Info,
                            liquidGlassEnabled = liquidGlassEnabled
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = statusMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

    BookPrimaryActionButton(
        label = downloadLabel,
        onClick = onDownload,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isResolvingOptions && !alreadyInLibrary && queuedDownloadLabel.isNullOrBlank()
            ,
        icon = Icons.Rounded.Download,
        loading = isResolvingOptions,
        loadingLabel = "Loading formats…"
    )
                }
            }
        }
    }
}

@Composable
internal fun BrowseDownloadOptionsDialog(
    title: String,
    options: List<BrowseDownloadOption>,
    onSelect: (BrowseDownloadOption) -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        shape = UiTokens.SettingsCardShape,
        containerColor = appDialogContainerColor(),
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            val mobiOptions = options.filter { it.format.equals("mobi", ignoreCase = true) }
            val basicMobiUrl = mobiOptions
                .takeIf { it.size > 1 }
                ?.let { candidates ->
                    candidates.firstOrNull { option ->
                        option.label?.contains("mobi7", ignoreCase = true) == true ||
                            option.label?.contains("basic", ignoreCase = true) == true ||
                            option.label?.contains("old", ignoreCase = true) == true
                    }?.url ?: candidates.filter { it.sizeBytes != null }
                        .minByOrNull { it.sizeBytes ?: Long.MAX_VALUE }
                        ?.url
                }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                options.forEach { option ->
                    val optionLabelHint = option.label?.lowercase().orEmpty()
                    val mimeHint = option.mimeType?.lowercase().orEmpty()
                    val displayFormat = when {
                        option.format.equals("epub3", ignoreCase = true) ||
                            optionLabelHint.contains("epub3") ||
                            optionLabelHint.contains("epub 3") ||
                            mimeHint.contains("epub3") ||
                            mimeHint.contains("profile=epub3") -> "EPUB3"
                        option.format.equals("mobi", ignoreCase = true) &&
                            basicMobiUrl != null &&
                            option.url == basicMobiUrl -> "MOBI Basic"
                        else -> option.format.uppercase()
                    }
                    Surface(
                        onClick = { onSelect(option) },
                        shape = RoundedCornerShape(12.dp),
                        color = appDialogCardColor(),
                        border = BorderStroke(1.dp, appDialogBorderColor())
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = displayFormat,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val sizeLabel = option.sizeBytes?.let { formatFileSize(it) } ?: "Unknown size"
                                Text(
                                    text = sizeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Rounded.Download,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private data class DetailItem(val label: String, val value: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

@Composable
private fun DetailRow(item: DetailItem) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(item.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Text(
            "${item.label}:",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            item.value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun formatDownloads(count: Int): String {
    return NumberFormat.getIntegerInstance().format(count)
}
