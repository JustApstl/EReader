package com.dyu.ereader.data.model

enum class ReaderControl {
    SEARCH,
    TTS,
    ACCESSIBILITY,
    ANALYTICS,
    EXPORT_HIGHLIGHT;

    companion object {
        fun defaultOrder(): List<ReaderControl> = listOf(
            SEARCH,
            TTS,
            ACCESSIBILITY,
            ANALYTICS,
            EXPORT_HIGHLIGHT
        )
    }
}
