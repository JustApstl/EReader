package com.dyu.ereader.data.format.handlers

import com.dyu.ereader.data.model.library.BookType

object Azw3FormatHandler : SimpleFormatHandler(
    type = BookType.AZW3,
    extensions = setOf("azw3"),
    mimeTypes = setOf("application/vnd.amazon.ebook")
)
