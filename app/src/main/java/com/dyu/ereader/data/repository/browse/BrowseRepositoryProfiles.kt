package com.dyu.ereader.data.repository.browse

import com.dyu.ereader.data.model.browse.BrowseCatalog

internal val DEFAULT_CATALOGS = listOf(
    BrowseCatalog(
        id = "standard_ebooks",
        title = "Standard Ebooks",
        url = "https://standardebooks.org/feeds/opds",
        description = "OPDS catalog (Patrons Circle login required for full access)",
        icon = "https://standardebooks.org/favicon.ico",
        languages = listOf("en"),
        formats = listOf("epub")
    ),
    BrowseCatalog(
        id = "project_gutenberg",
        title = "Project Gutenberg",
        url = "https://www.gutenberg.org/ebooks/search.opds/",
        description = "OPDS catalog for free ebooks",
        icon = "https://www.gutenberg.org/favicon.ico",
        languages = listOf("en"),
        formats = listOf("epub", "pdf")
    ),
    BrowseCatalog(
        id = "wikisource_en",
        title = "Wikisource (EN)",
        url = "https://ws-export.wmcloud.org/opds/en/Ready_for_export.xml",
        description = "Community-sourced public domain texts",
        icon = "https://wikisource.org/favicon.ico",
        languages = listOf("en"),
        formats = listOf("epub", "pdf")
    ),
    BrowseCatalog(
        id = "anarchist_library",
        title = "The Anarchist Library",
        url = "https://bookshelf.theanarchistlibrary.org/opds",
        description = "OPDS catalog of public texts",
        icon = "https://theanarchistlibrary.org/favicon.ico",
        languages = listOf("en"),
        formats = listOf("epub", "pdf")
    ),
    BrowseCatalog(
        id = "feedbooks",
        title = "Feedbooks",
        url = "https://www.feedbooks.com/catalog.atom",
        description = "Public catalog via OPDS",
        icon = "https://www.feedbooks.com/favicon.ico",
        languages = listOf("en"),
        formats = listOf("epub")
    ),
    BrowseCatalog(
        id = "open_library_bookserver",
        title = "Open Library (BookServer)",
        url = "https://bookserver.archive.org/catalog/",
        description = "Internet Archive OPDS catalog (BookServer)",
        icon = "https://openlibrary.org/favicon.ico",
        languages = listOf("en"),
        formats = listOf("epub", "pdf", "mobi")
    )
)

internal val DEFAULT_CATALOG_IDS = DEFAULT_CATALOGS.map { it.id }.toSet()

internal val OPDS_PROFILES = listOf(
    OpdsProfile(
        id = "standard_ebooks",
        hostHint = "standardebooks.org",
        coverRelPriority = listOf(
            "http://opds-spec.org/image",
            "http://opds-spec.org/image/thumbnail",
            "image",
            "thumbnail"
        )
    ),
    OpdsProfile(
        id = "project_gutenberg",
        hostHint = "gutenberg.org",
        coverRelPriority = listOf(
            "http://opds-spec.org/image",
            "http://opds-spec.org/image/thumbnail",
            "image",
            "thumbnail",
            "cover"
        )
    ),
    OpdsProfile(
        id = "feedbooks",
        hostHint = "feedbooks.com"
    ),
    OpdsProfile(
        id = "open_library_bookserver",
        hostHint = "bookserver.archive.org",
        coverRelPriority = listOf(
            "http://opds-spec.org/image",
            "http://opds-spec.org/image/thumbnail",
            "image",
            "thumbnail",
            "cover"
        ),
        formatScores = mapOf("epub" to 110, "pdf" to 90, "mobi" to 80)
    ),
    OpdsProfile(
        id = "wikisource_en",
        hostHint = "wikisource.org"
    ),
    OpdsProfile(
        id = "anarchist_library",
        hostHint = "theanarchistlibrary.org"
    )
)
