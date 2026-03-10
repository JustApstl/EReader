package com.dyu.ereader.data.model

import androidx.compose.ui.graphics.Color

enum class ReadingMode {
    SCROLL,
    PAGE
}

enum class ReaderTheme(val label: String) {
    SYSTEM("System"),
    WHITE("White"),
    SEPIA("Sepia"),
    DARK("Dark"),
    BLACK("Black"),
    CUSTOM("Custom"),
    IMAGE("Image")
}

enum class FontColorTheme(val label: String) {
    DEFAULT("Default"),
    BLACK("Black"),
    WHITE("White"),
    GRAY("Gray"),
    SEPIA_DARK("Sepia Dark"),
    CUSTOM("Custom")
}

enum class ImageFilter {
    NONE,
    INVERT,
    DARKEN
}

enum class PageTransitionStyle {
    DEFAULT,
    TILT,
    CARD,
    FLIP,
    CUBE,
    ROLL
}

fun ReaderTheme.getBackgroundColor(isDarkTheme: Boolean, customColor: Int? = null): Color {
    val baseColor = when (this) {
        ReaderTheme.WHITE -> Color(0xFFFFFFFF)
        ReaderTheme.SEPIA -> Color(0xFFF4ECD8)
        ReaderTheme.BLACK -> Color(0xFF000000)
        ReaderTheme.DARK -> Color(0xFF1A1A1A)
        ReaderTheme.SYSTEM -> if (isDarkTheme) Color(0xFF121212) else Color(0xFFFDFCFB)
        ReaderTheme.CUSTOM -> customColor?.let { Color(it) } ?: (if (isDarkTheme) Color(0xFF121212) else Color(0xFFFDFCFB))
        ReaderTheme.IMAGE -> Color.Transparent // Background image will be visible
    }
    return baseColor
}

fun FontColorTheme.getColor(customColor: Int? = null): Color? {
    return when (this) {
        FontColorTheme.DEFAULT -> null
        FontColorTheme.BLACK -> Color(0xFF000000)
        FontColorTheme.WHITE -> Color(0xFFFFFFFF)
        FontColorTheme.GRAY -> Color(0xFF808080)
        FontColorTheme.SEPIA_DARK -> Color(0xFF5B4636)
        FontColorTheme.CUSTOM -> customColor?.let { Color(it) }
    }
}

enum class ReaderFont(val label: String, val cssFamily: String) {
    SYSTEM("System", "system-ui, -apple-system, sans-serif"),
    SANS("Sans Serif", "sans-serif"),
    SERIF("Serif", "serif"),
    MONO("Monospace", "monospace"),
    CURSIVE("Cursive", "cursive"),
    ROBOTO("Roboto", "\"Roboto\", \"Noto Sans\", sans-serif"),
    NOTO_SERIF("Noto Serif", "\"Noto Serif\", \"Droid Serif\", serif"),
    GEORGIA("Georgia", "Georgia, \"Times New Roman\", serif"),
    BOOK_STYLE("Book Style", "\"Literata\", \"Merriweather\", serif"),
    CUSTOM("Custom", "custom")
}

data class ReaderSettings(
    val readingMode: ReadingMode = ReadingMode.SCROLL,
    val readerTheme: ReaderTheme = ReaderTheme.SYSTEM,
    val focusText: Boolean = false,
    val focusTextBoldness: Int = 700,
    val focusTextColor: Int? = null,
    val fontSizeSp: Float = 15f,
    val lineSpacing: Float = 1.55f,
    val horizontalMarginDp: Float = 20f,
    val font: ReaderFont = ReaderFont.SERIF,
    val focusMode: Boolean = false,
    val hideStatusBar: Boolean = false,
    val customBackgroundColor: Int? = null,
    val backgroundImageUri: String? = null,
    val backgroundImageBlur: Float = 0f,
    val backgroundImageOpacity: Float = 1f,
    val fontColorTheme: FontColorTheme = FontColorTheme.DEFAULT,
    val autoFontColor: Boolean = true,
    val customFontColor: Int? = null,
    val customFontUri: String? = null,
    val imageFilter: ImageFilter = ImageFilter.NONE,
    val usePublisherStyle: Boolean = false,
    val underlineLinks: Boolean = false,
    val textShadow: Boolean = false,
    val textShadowColor: Int? = null,
    val navBarStyle: NavigationBarStyle = NavigationBarStyle.DEFAULT,
    val pageTurn3d: Boolean = false,
    val pageTransitionStyle: PageTransitionStyle = PageTransitionStyle.DEFAULT
)
