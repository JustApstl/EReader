package com.dyu.ereader.data.format.handlers

import com.dyu.ereader.data.model.library.BookType

object CbrFormatHandler : SimpleFormatHandler(
    type = BookType.CBR,
    extensions = setOf("cbr"),
    mimeTypes = setOf(
        "application/vnd.comicbook-rar",
        "application/x-cbr",
        "application/x-rar-compressed"
    )
)
