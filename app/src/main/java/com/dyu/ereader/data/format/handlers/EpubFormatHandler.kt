package com.dyu.ereader.data.format.handlers

import android.content.Context
import android.net.Uri
import com.dyu.ereader.data.format.epub.EpubMetadataReader
import com.dyu.ereader.data.format.BookFormatHandler
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType

object EpubFormatHandler : BookFormatHandler {
    override val type: BookType = BookType.EPUB
    override val extensions: Set<String> = setOf("epub")
    override val mimeTypes: Set<String> = setOf("application/epub+zip")

    override suspend fun readMetadata(
        context: Context,
        uri: Uri,
        id: String,
        fileName: String,
        fileSize: Long,
        fallbackAuthor: String,
        dateAdded: Long
    ): BookItem? {
        return EpubMetadataReader.readMetadata(
            context = context,
            uri = uri,
            id = id,
            fileName = fileName,
            fileSize = fileSize
        )
    }

    override fun shouldRefreshMetadata(existing: BookItem): Boolean {
        return existing.isbn.isNullOrBlank() ||
            existing.description.isNullOrBlank() ||
            existing.publisher.isNullOrBlank() ||
            existing.publishedDate.isNullOrBlank() ||
            existing.language.isNullOrBlank() ||
            existing.genres.isEmpty() ||
            existing.countries.isEmpty()
    }

    override fun mergeWithExisting(existing: BookItem, refreshed: BookItem): BookItem {
        return refreshed.copy(
            isFavorite = existing.isFavorite,
            lastOpened = existing.lastOpened,
            dateAdded = existing.dateAdded,
            coverImage = refreshed.coverImage ?: existing.coverImage,
            description = refreshed.description ?: existing.description,
            publisher = refreshed.publisher ?: existing.publisher,
            publishedDate = refreshed.publishedDate ?: existing.publishedDate,
            isbn = refreshed.isbn ?: existing.isbn,
            language = refreshed.language ?: existing.language,
            year = refreshed.year ?: existing.year,
            genres = if (refreshed.genres.isNotEmpty()) refreshed.genres else existing.genres,
            countries = if (refreshed.countries.isNotEmpty()) refreshed.countries else existing.countries
        )
    }
}
