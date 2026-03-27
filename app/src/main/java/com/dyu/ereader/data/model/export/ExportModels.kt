package com.dyu.ereader.data.model.export

enum class ExportFormat {
    PDF, MARKDOWN, JSON, EMAIL
}

data class ExportOptions(
    val format: ExportFormat,
    val includeHighlights: Boolean = true,
    val includeBookmarks: Boolean = true,
    val includeNotes: Boolean = true,
    val includeMetadata: Boolean = true,
    val sortBy: String = "date" // date, chapter, color
)

data class ExportData(
    val bookTitle: String,
    val bookAuthor: String,
    val exportDate: Long,
    val format: ExportFormat,
    val highlights: List<ExportedHighlight> = emptyList(),
    val bookmarks: List<ExportedBookmark> = emptyList(),
    val notes: List<ExportedNote> = emptyList()
)

data class ExportedHighlight(
    val text: String,
    val color: String,
    val chapter: String,
    val context: String,
    val timestamp: Long
)

data class ExportedBookmark(
    val title: String,
    val chapter: String,
    val note: String? = null,
    val timestamp: Long
)

data class ExportedNote(
    val content: String,
    val chapter: String,
    val position: String,
    val timestamp: Long
)
