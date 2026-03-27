package com.dyu.ereader.data.model.library

data class ParsedEpub(
    val title: String,
    val author: String,
    val coverImage: ByteArray?
)
