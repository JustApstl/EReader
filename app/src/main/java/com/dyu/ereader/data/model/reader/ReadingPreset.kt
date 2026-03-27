package com.dyu.ereader.data.model.reader

import com.dyu.ereader.data.model.library.BookType

enum class ReadingPreset(val label: String) {
    NOVEL("Novel"),
    MANGA_PDF("Manga/PDF"),
    NIGHT("Night"),
    FOCUS("Focus"),
    PUBLISHER("Publisher")
}

fun ReaderSettings.withPreset(
    preset: ReadingPreset,
    bookType: BookType
): ReaderSettings {
    return when (preset) {
        ReadingPreset.NOVEL -> copy(
            readingMode = ReadingMode.PAGE,
            readerTheme = ReaderTheme.SYSTEM,
            font = ReaderFont.SERIF,
            fontSizeSp = 17f,
            lineSpacing = 1.7f,
            horizontalMarginDp = 24f,
            fontColorTheme = FontColorTheme.DEFAULT,
            autoFontColor = true,
            focusText = false,
            focusMode = false,
            pageTurn3d = false,
            usePublisherStyle = false,
            textAlignment = TextAlignment.JUSTIFY
        )

        ReadingPreset.MANGA_PDF -> copy(
            readingMode = ReadingMode.PAGE,
            readerTheme = if (bookType == BookType.PDF) ReaderTheme.BLACK else ReaderTheme.DARK,
            font = ReaderFont.SYSTEM,
            fontSizeSp = 15f,
            lineSpacing = 1.3f,
            horizontalMarginDp = if (bookType == BookType.PDF) 8f else 12f,
            fontColorTheme = FontColorTheme.DEFAULT,
            autoFontColor = true,
            focusText = false,
            focusMode = false,
            pageTurn3d = false,
            usePublisherStyle = false,
            textAlignment = TextAlignment.DEFAULT
        )

        ReadingPreset.NIGHT -> copy(
            readerTheme = ReaderTheme.BLACK,
            fontColorTheme = FontColorTheme.WHITE,
            autoFontColor = false,
            focusText = false,
            focusMode = false,
            usePublisherStyle = false,
            textShadow = false
        )

        ReadingPreset.FOCUS -> copy(
            readerTheme = ReaderTheme.SEPIA,
            font = ReaderFont.SERIF,
            fontSizeSp = 18f,
            lineSpacing = 1.8f,
            horizontalMarginDp = 24f,
            fontColorTheme = FontColorTheme.SEPIA_DARK,
            autoFontColor = false,
            focusText = true,
            focusMode = true,
            usePublisherStyle = false
        )

        ReadingPreset.PUBLISHER -> copy(
            readerTheme = ReaderTheme.DEFAULT,
            font = ReaderFont.DEFAULT,
            fontColorTheme = FontColorTheme.DEFAULT,
            autoFontColor = true,
            focusText = false,
            focusMode = false,
            usePublisherStyle = true,
            textAlignment = TextAlignment.DEFAULT
        )
    }
}
