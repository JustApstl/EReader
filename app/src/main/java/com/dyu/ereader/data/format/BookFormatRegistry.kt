package com.dyu.ereader.data.format

import com.dyu.ereader.data.format.handlers.Azw3FormatHandler
import com.dyu.ereader.data.format.handlers.CbrFormatHandler
import com.dyu.ereader.data.format.handlers.CbzFormatHandler
import com.dyu.ereader.data.format.handlers.EpubFormatHandler
import com.dyu.ereader.data.format.handlers.MobiFormatHandler
import com.dyu.ereader.data.format.handlers.PdfFormatHandler
import com.dyu.ereader.data.model.library.BookType

object BookFormatRegistry {
    private val handlers: List<BookFormatHandler> = listOf(
        EpubFormatHandler,
        PdfFormatHandler,
        Azw3FormatHandler,
        MobiFormatHandler,
        CbzFormatHandler,
        CbrFormatHandler
    )

    val supportedExtensions: Set<String> = handlers.flatMap { it.extensions }.toSet()

    fun handlerForExtension(extension: String): BookFormatHandler? {
        val normalized = extension.trim().lowercase()
        return handlers.firstOrNull { normalized in it.extensions }
    }

    fun handlerForType(type: BookType): BookFormatHandler? {
        return handlers.firstOrNull { it.type == type }
    }

    fun resolveTypeFromPath(pathOrUri: String, fallback: BookType = BookType.EPUB): BookType {
        val extension = pathOrUri
            .substringBefore('#')
            .substringBefore('?')
            .substringAfterLast('.', "")
            .lowercase()
        return handlerForExtension(extension)?.type ?: fallback
    }

    fun resolveMimeType(extension: String, fallback: String = "application/octet-stream"): String {
        val handler = handlerForExtension(extension) ?: return fallback
        return handler.mimeTypes.firstOrNull() ?: fallback
    }
}
