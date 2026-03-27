package com.dyu.ereader.data.model.search

data class SearchResult(
    val chapterHref: String,
    val chapterTitle: String,
    val textContext: String, // surrounding context
    val matchStart: Int, // position in context
    val matchEnd: Int,
    val percentage: Float // book progress percentage
)

data class SearchQuery(
    val bookUri: String,
    val query: String,
    val caseSensitive: Boolean = false,
    val wholeWord: Boolean = false
)
