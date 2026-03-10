package com.dyu.ereader.data.model

data class BrowseCatalog(
    val id: String,
    val title: String,
    val url: String,
    val description: String? = null,
    val icon: String? = null
)

data class BrowseBook(
    val id: String,
    val title: String,
    val author: String,
    val summary: String? = null,
    val coverUrl: String? = null,
    val downloadUrl: String,
    val format: String, // EPUB, PDF, etc.
    val rights: String? = null,
    val publisher: String? = null,
    val published: String? = null
)

data class BrowseFeed(
    val id: String,
    val title: String,
    val entries: List<BrowseBook>,
    val links: List<BrowseLink>
)

data class BrowseLink(
    val rel: String, // "next", "prev", etc.
    val href: String,
    val type: String
)
