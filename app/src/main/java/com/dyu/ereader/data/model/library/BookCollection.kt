package com.dyu.ereader.data.model.library

data class BookCollection(
    val name: String,
    val bookIds: Set<String> = emptySet(),
    val createdAt: Long = System.currentTimeMillis()
)

data class BookCollectionShelf(
    val name: String,
    val books: List<BookItem>,
    val createdAt: Long = System.currentTimeMillis()
)
