package com.dyu.ereader.data.format.handlers

import com.dyu.ereader.data.model.library.BookType

object PdfFormatHandler : SimpleFormatHandler(
    type = BookType.PDF,
    extensions = setOf("pdf"),
    mimeTypes = setOf("application/pdf")
)
