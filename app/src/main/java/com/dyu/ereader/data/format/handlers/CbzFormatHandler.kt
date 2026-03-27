package com.dyu.ereader.data.format.handlers

import com.dyu.ereader.data.model.library.BookType

object CbzFormatHandler : SimpleFormatHandler(
    type = BookType.CBZ,
    extensions = setOf("cbz"),
    mimeTypes = setOf(
        "application/vnd.comicbook+zip",
        "application/x-cbz",
        "application/zip"
    )
)
