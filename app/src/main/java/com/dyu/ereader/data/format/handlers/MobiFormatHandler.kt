package com.dyu.ereader.data.format.handlers

import com.dyu.ereader.data.model.library.BookType

object MobiFormatHandler : SimpleFormatHandler(
    type = BookType.MOBI,
    extensions = setOf("mobi"),
    mimeTypes = setOf("application/x-mobipocket-ebook")
)
