package com.dyu.ereader.data.format

import android.content.Context
import android.net.Uri
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType

interface BookFormatHandler {
    val type: BookType
    val extensions: Set<String>
    val mimeTypes: Set<String>

    suspend fun readMetadata(
        context: Context,
        uri: Uri,
        id: String,
        fileName: String,
        fileSize: Long,
        fallbackAuthor: String,
        dateAdded: Long
    ): BookItem?

    fun shouldRefreshMetadata(existing: BookItem): Boolean = false

    fun mergeWithExisting(existing: BookItem, refreshed: BookItem): BookItem {
        return refreshed.copy(
            isFavorite = existing.isFavorite,
            lastOpened = existing.lastOpened,
            dateAdded = existing.dateAdded
        )
    }
}
