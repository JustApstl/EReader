package com.dyu.ereader.ui.reader.viewmodel

import com.dyu.ereader.data.model.export.ExportData
import com.dyu.ereader.data.model.export.ExportFormat
import com.dyu.ereader.data.model.export.ExportedBookmark
import com.dyu.ereader.data.model.export.ExportedHighlight
import com.dyu.ereader.data.model.export.ExportedNote
import com.dyu.ereader.ui.reader.state.ReaderUiState

internal fun ReaderUiState.toExportData(
    format: ExportFormat,
    includeAnnotations: Boolean,
    includeBookmarks: Boolean
): ExportData {
    return ExportData(
        bookTitle = title,
        bookAuthor = author,
        exportDate = System.currentTimeMillis(),
        format = format,
        highlights = if (includeAnnotations) {
            highlights.map {
                ExportedHighlight(
                    text = it.selectedText,
                    color = it.color,
                    chapter = it.chapterAnchor,
                    context = "",
                    timestamp = it.createdAt
                )
            }
        } else {
            emptyList()
        },
        bookmarks = if (includeBookmarks) {
            bookmarks.map {
                ExportedBookmark(
                    title = it.title ?: "Bookmark",
                    chapter = it.chapterAnchor,
                    note = it.note,
                    timestamp = it.createdAt
                )
            }
        } else {
            emptyList()
        },
        notes = marginNotes.map {
            ExportedNote(
                content = it.content,
                chapter = it.chapterAnchor,
                position = it.position,
                timestamp = it.createdAt
            )
        }
    )
}
