package com.dyu.ereader.ui.reader.overlays.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyu.ereader.data.local.db.BookmarkEntity
import com.dyu.ereader.data.local.db.HighlightEntity
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.ui.reader.overlays.components.ReaderControlBottomSheet
import com.dyu.ereader.ui.reader.overlays.components.ReaderPanelScaffold
import com.dyu.ereader.ui.reader.overlays.components.ReaderSheetSection
import com.dyu.ereader.ui.reader.settings.components.FilterChip
import com.dyu.ereader.ui.reader.state.Chapter

private enum class ReaderMenuSection(
    val title: String,
    val emptyMessage: String
) {
    CHAPTERS(
        title = "Chapters",
        emptyMessage = "No chapter list available for this book."
    ),
    BOOKMARKS(
        title = "Bookmarks",
        emptyMessage = "No bookmarks saved yet."
    ),
    NOTES(
        title = "Notes",
        emptyMessage = "No notes created yet."
    ),
    HIGHLIGHTS(
        title = "Annotations",
        emptyMessage = "No highlights saved yet."
    )
}

@Composable
internal fun TableOfContentsPanelContent(
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    bookmarks: List<BookmarkEntity>,
    highlights: List<HighlightEntity>,
    marginNotes: List<MarginNoteEntity>,
    onLocationSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by remember { mutableStateOf(ReaderMenuSection.CHAPTERS) }

    ReaderPanelScaffold(
        title = "Reader Menu",
        icon = Icons.Rounded.Menu,
        onDismiss = onDismiss,
        closeContentDescription = "Close Reader Menu",
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ReaderMenuSection.entries.forEach { section ->
                val count = when (section) {
                    ReaderMenuSection.CHAPTERS -> chapters.count { it.href.isNotBlank() }
                    ReaderMenuSection.BOOKMARKS -> bookmarks.size
                    ReaderMenuSection.NOTES -> marginNotes.size
                    ReaderMenuSection.HIGHLIGHTS -> highlights.size
                }
                FilterChip(
                    selected = selectedSection == section,
                    onClick = { selectedSection = section },
                    label = "${section.title} $count",
                    showSelectionIcon = false
                )
            }
        }

        ReaderSheetSection(
            title = selectedSection.title,
            icon = Icons.Rounded.Menu
        ) {
            when (selectedSection) {
                ReaderMenuSection.CHAPTERS -> {
                    if (chapters.isEmpty()) {
                        ReaderMenuEmptyState(selectedSection.emptyMessage)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(chapters, key = { _, chapter ->
                                buildString {
                                    append(chapter.depth)
                                    append(':')
                                    append(chapter.href)
                                    append(':')
                                    append(chapter.label)
                                }
                            }) { index, chapter ->
                                ReaderMenuItem(
                                    title = chapterDisplayLabel(chapter),
                                    selected = index == currentChapterIndex,
                                    onClick = {
                                        if (chapter.href.isNotBlank()) {
                                            onLocationSelected(chapter.href)
                                        }
                                    },
                                    indentLevel = chapter.depth,
                                    enabled = chapter.href.isNotBlank(),
                                    prominent = chapter.depth == 0 || chapter.hasChildren
                                )
                            }
                        }
                    }
                }

                ReaderMenuSection.BOOKMARKS -> {
                    if (bookmarks.isEmpty()) {
                        ReaderMenuEmptyState(selectedSection.emptyMessage)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(bookmarks.sortedByDescending { it.updatedAt }) { bookmark ->
                                ReaderMenuItem(
                                    title = bookmark.title?.takeIf { it.isNotBlank() } ?: "Saved bookmark",
                                    selected = false,
                                    onClick = {
                                        onLocationSelected(
                                            bookmark.cfi.takeIf { it.isNotBlank() }
                                                ?: bookmark.chapterAnchor
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                ReaderMenuSection.NOTES -> {
                    if (marginNotes.isEmpty()) {
                        ReaderMenuEmptyState(selectedSection.emptyMessage)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(marginNotes.sortedByDescending { it.updatedAt }) { note ->
                                ReaderMenuItem(
                                    title = note.content.lineSequence().firstOrNull()?.trim().orEmpty()
                                        .ifBlank { "Reader note" },
                                    selected = false,
                                    onClick = {
                                        onLocationSelected(
                                            note.cfi.takeIf { it.isNotBlank() }
                                                ?: note.chapterAnchor
                                        )
                                    }
                                )
                            }
                        }
                    }
                }

                ReaderMenuSection.HIGHLIGHTS -> {
                    if (highlights.isEmpty()) {
                        ReaderMenuEmptyState(selectedSection.emptyMessage)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(highlights.sortedByDescending { it.createdAt }) { highlight ->
                                ReaderMenuItem(
                                    title = highlight.selectedText.trim().ifBlank { "Highlighted passage" },
                                    selected = false,
                                    onClick = {
                                        onLocationSelected(
                                            highlight.selectionJson.takeIf { it.isNotBlank() }
                                                ?: highlight.chapterAnchor
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TableOfContentsSheet(
    chapters: List<Chapter>,
    currentChapterIndex: Int,
    bookmarks: List<BookmarkEntity>,
    highlights: List<HighlightEntity>,
    marginNotes: List<MarginNoteEntity>,
    onLocationSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ReaderControlBottomSheet(
            onDismissRequest = onDismiss
    ) {
        TableOfContentsPanelContent(
            chapters = chapters,
            currentChapterIndex = currentChapterIndex,
            bookmarks = bookmarks,
            highlights = highlights,
            marginNotes = marginNotes,
            onLocationSelected = onLocationSelected,
            onDismiss = onDismiss
        )
    }
}

@Composable
private fun ReaderMenuEmptyState(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ReaderMenuItem(
    title: String,
    subtitle: String? = null,
    underlined: Boolean = false,
    selected: Boolean,
    onClick: () -> Unit,
    indentLevel: Int = 0,
    enabled: Boolean = true,
    prominent: Boolean = false
) {
    Surface(
        onClick = {
            if (enabled) {
                onClick()
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (!enabled) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.56f)
        } else if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.84f)
        },
        border = BorderStroke(
            1.dp,
            if (!enabled) {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f)
            } else if (selected) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (indentLevel > 0) {
                Spacer(modifier = Modifier.width((indentLevel * 14).dp))
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = when {
                            selected -> FontWeight.ExtraBold
                            prominent -> FontWeight.Bold
                            else -> FontWeight.Medium
                        },
                        fontSize = if (prominent) 15.5.sp else 15.sp
                    ),
                    color = if (!enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f)
                    } else if (selected) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    textDecoration = if (underlined) TextDecoration.Underline else null,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

private fun chapterDisplayLabel(chapter: Chapter): String {
    val normalized = chapter.label.trim()
    return normalized.ifBlank { "Untitled section" }
}
