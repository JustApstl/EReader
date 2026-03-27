package com.dyu.ereader.data.model.library

enum class BookType(val label: String, val isEpub: Boolean) {
    EPUB("EPUB", true),
    PDF("PDF", false),
    EPUB3("EPUB3", true),
    AZW3("AZW3", false),
    MOBI("MOBI", false),
    CBZ("CBZ", false),
    CBR("CBR", false)
}

data class BookItem(
    val id: String,
    val uri: String,
    val fileName: String,
    val title: String,
    val author: String,
    val coverImage: ByteArray?,
    val isFavorite: Boolean = false,
    val lastOpened: Long = 0L,
    val dateAdded: Long = 0L,
    val type: BookType = BookType.EPUB,
    val progress: Float = 0f,
    // Detailed info fields
    val description: String? = null,
    val publisher: String? = null,
    val publishedDate: String? = null,
    val isbn: String? = null,
    val language: String? = null,
    val fileSize: Long = 0L,
    val year: String? = null,
    val genres: List<String> = emptyList(),
    val countries: List<String> = emptyList()
)
