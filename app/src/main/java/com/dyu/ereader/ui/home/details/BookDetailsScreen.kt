package com.dyu.ereader.ui.home.details

import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Business
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FilePresent
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.books.BookAboutDetailRow
import com.dyu.ereader.ui.components.books.BookAboutTopBar
import com.dyu.ereader.ui.components.books.BookMetadataChip
import com.dyu.ereader.ui.components.books.BookAboutSection
import com.dyu.ereader.ui.components.books.BookMetadataHtmlText
import com.dyu.ereader.ui.components.books.BookPrimaryActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.format.epub.EpubMetadataReader
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.core.locale.displayLanguageName
import com.dyu.ereader.core.locale.extractPublishedYear
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BookDetailsScreen(
    book: BookItem,
    liquidGlassEnabled: Boolean,
    onClose: () -> Unit,
    onRead: () -> Unit
) {
    BackHandler(onBack = onClose)
    val context = LocalContext.current
    val coverBitmap = remember(book.id) {
        book.coverImage?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() }
    }
    val fileSize = remember(book.fileSize) { formatFileSize(book.fileSize) }
    val publishedYear = extractPublishedYear(book.publishedDate)
    val languageLabel = displayLanguageName(book.language)
    val (isbn10, isbn13) = remember(book.isbn) { extractIsbnCandidates(book.isbn) }
    val pageCount by produceState<Int?>(initialValue = null, book.id, book.uri, book.type) {
        value = withContext(Dispatchers.IO) {
            val uri = Uri.parse(book.uri)
            when (book.type) {
                BookType.PDF -> readPdfPageCount(context, uri)
                BookType.EPUB, BookType.EPUB3 -> EpubMetadataReader.readPageCount(context, uri)
                else -> null
            }
        }
    }
    val pagesLabel = pageCount?.takeIf { it > 0 }?.toString() ?: "Unknown"

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                BookAboutTopBar(
                    title = "About",
                    subtitle = book.type.label,
                    onBack = onClose
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    BookAboutSection(liquidGlassEnabled = liquidGlassEnabled) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)),
                                modifier = Modifier
                                    .width(120.dp)
                                    .height(180.dp)
                            ) {
                                if (coverBitmap != null) {
                                    Image(
                                        bitmap = coverBitmap,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.AutoMirrored.Rounded.MenuBook,
                                            null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f)
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        book.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        book.author,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    BookAboutSection(
                        title = "Description",
                        icon = Icons.AutoMirrored.Rounded.MenuBook,
                        liquidGlassEnabled = liquidGlassEnabled
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (book.description.isNullOrBlank()) {
                                Text(
                                    "No description available.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                BookMetadataHtmlText(book.description.orEmpty(), modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }

                item {
                    BookAboutSection(
                        title = "About",
                        icon = Icons.Rounded.Info,
                        liquidGlassEnabled = liquidGlassEnabled
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BookAboutDetailRow(Icons.Rounded.FilePresent, "File Type", book.type.label)
                            BookAboutDetailRow(Icons.Rounded.AutoStories, "Pages", pagesLabel)
                            BookAboutDetailRow(Icons.Rounded.Fingerprint, "ISBN 10", isbn10 ?: "Unknown")
                            BookAboutDetailRow(Icons.Rounded.Fingerprint, "ISBN 13", isbn13 ?: "Unknown")
                            BookAboutDetailRow(Icons.Rounded.Business, "Publisher", book.publisher?.takeIf { it.isNotBlank() } ?: "Unknown")
                            BookAboutDetailRow(Icons.Rounded.Event, "Published", publishedYear ?: "Unknown")
                            BookAboutDetailRow(Icons.Rounded.Language, "Language", languageLabel ?: "Unknown")
                            BookAboutDetailRow(Icons.Rounded.FilePresent, "File Size", fileSize)
                        }
                    }
                }

                if (book.genres.isNotEmpty()) {
                    item {
                        BookAboutSection(
                            title = "Genres",
                            icon = Icons.Rounded.Palette,
                            liquidGlassEnabled = liquidGlassEnabled
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    book.genres.forEach { genre ->
                                        BookMetadataChip(label = genre)
                                    }
                                }
                            }
                        }
                    }
                }

                if (book.countries.isNotEmpty()) {
                    item {
                        BookAboutSection(
                            title = "Countries",
                            icon = Icons.Rounded.Language,
                            liquidGlassEnabled = liquidGlassEnabled
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    book.countries.forEach { country ->
                                        BookMetadataChip(label = country)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    BookPrimaryActionButton(
                        label = "Read Now",
                        onClick = onRead,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (kotlin.math.log10(bytes.toDouble()) / kotlin.math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

private fun readPdfPageCount(context: android.content.Context, uri: Uri): Int? {
    return try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { fd ->
            PdfRenderer(fd).use { renderer -> renderer.pageCount }
        }
    } catch (_: Exception) {
        null
    }
}

private fun extractIsbnCandidates(raw: String?): Pair<String?, String?> {
    if (raw.isNullOrBlank()) return null to null
    val cleaned = raw.uppercase().replace(Regex("[^0-9X]"), "")
    val isbn13 = Regex("\\d{13}").find(cleaned)?.value
    val isbn10 = Regex("\\d{9}[0-9X]").find(cleaned)?.value
    return isbn10 to isbn13
}
