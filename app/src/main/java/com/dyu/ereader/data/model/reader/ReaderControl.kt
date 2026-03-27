package com.dyu.ereader.data.model.reader

enum class ReaderControl {
    SEARCH,
    LISTEN,
    ACCESSIBILITY,
    ANALYTICS,
    EXPORT_HIGHLIGHT;

    companion object {
        fun defaultOrder(): List<ReaderControl> = listOf(
            SEARCH,
            LISTEN,
            ACCESSIBILITY,
            ANALYTICS,
            EXPORT_HIGHLIGHT
        )
    }
}
