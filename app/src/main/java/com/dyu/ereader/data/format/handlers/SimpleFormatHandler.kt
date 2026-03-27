package com.dyu.ereader.data.format.handlers

import android.content.Context
import android.net.Uri
import com.dyu.ereader.data.format.BookFormatHandler
import com.dyu.ereader.data.metadata.BookMetadataCleaner
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType

abstract class SimpleFormatHandler(
    override val type: BookType,
    override val extensions: Set<String>,
    override val mimeTypes: Set<String>
) : BookFormatHandler {
    override suspend fun readMetadata(
        context: Context,
        uri: Uri,
        id: String,
        fileName: String,
        fileSize: Long,
        fallbackAuthor: String,
        dateAdded: Long
    ): BookItem? {
        val parsed = BookMetadataCleaner.parseFileNameMetadata(fileName)
        return BookItem(
            id = id,
            uri = uri.toString(),
            fileName = fileName,
            title = BookMetadataCleaner.cleanTitle(parsed.title, fileName),
            author = BookMetadataCleaner.cleanAuthor(parsed.author ?: fallbackAuthor, fileName, fallbackAuthor),
            coverImage = null,
            type = type,
            fileSize = fileSize,
            dateAdded = dateAdded
        )
    }
}
