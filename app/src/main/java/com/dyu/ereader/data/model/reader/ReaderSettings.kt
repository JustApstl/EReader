package com.dyu.ereader.data.model.reader

import androidx.compose.ui.graphics.Color
import com.dyu.ereader.data.model.app.NavigationBarStyle

enum class ReadingMode {
    SCROLL,
    PAGE
}

enum class ReaderTheme(val label: String) {
    DEFAULT("Default"),
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
    AUTO,
    NONE,
    INVERT,
    DARKEN,
    BW
}

enum class PageTransitionStyle {
    DEFAULT,
    TILT,
    CARD,
    FLIP,
    CUBE,
    ROLL,
    PAPER
}

enum class TextAlignment(val label: String, val cssValue: String) {
    DEFAULT("Original", ""),
    LEFT("Left", "left"),
    CENTER("Center", "center"),
    RIGHT("Right", "right"),
    JUSTIFY("Justify", "justify")
}

enum class ReaderTapZoneAction(val label: String) {
    NEXT_PAGE("Next Page"),
    PREVIOUS_PAGE("Previous Page"),
    TOGGLE_UI("Toggle UI"),
    LISTEN("Listen"),
    BOOKMARK("Bookmark"),
    NONE("None")
}

fun ReaderTheme.getBackgroundColor(isDarkTheme: Boolean, customColor: Int? = null): Color {
    return when (this) {
        ReaderTheme.DEFAULT -> if (isDarkTheme) Color(0xFF121212) else Color.White
        ReaderTheme.WHITE -> Color(0xFFFFFFFF)
        ReaderTheme.SEPIA -> Color(0xFFF4ECD8)
        ReaderTheme.BLACK -> Color(0xFF000000)
        ReaderTheme.DARK -> Color(0xFF1A1A1A)
        ReaderTheme.SYSTEM -> if (isDarkTheme) Color(0xFF121212) else Color(0xFFFDFCFB)
        ReaderTheme.CUSTOM -> customColor?.let { Color(it) } ?: (if (isDarkTheme) Color(0xFF121212) else Color(0xFFFDFCFB))
        ReaderTheme.IMAGE -> Color.Transparent
    }
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
    DEFAULT("Default", "inherit"),
    SYSTEM("System", "system-ui, -apple-system, sans-serif"),
    SANS("Sans Serif", "sans-serif"),
    SERIF("Serif", "serif"),
    MONO("Monospace", "monospace"),
    CURSIVE("Cursive", "cursive"),
    CUSTOM("Custom", "custom")
}

enum class ReaderTextElement(val label: String) {
    PARAGRAPH("Paragraphs"),
    HEADING_1("Heading 1"),
    HEADING_2("Heading 2"),
    HEADING_3("Heading 3"),
    HEADING_4("Heading 4"),
    HEADING_5("Heading 5"),
    HEADING_6("Heading 6"),
    EXTERNAL_LINK("Web Links"),
    INTERNAL_LINK("Book Links")
}

data class ReaderElementStyle(
    val font: ReaderFont = ReaderFont.DEFAULT,
    val color: Int? = null
)

data class ReaderElementStyles(
    val paragraph: ReaderElementStyle = ReaderElementStyle(),
    val heading1: ReaderElementStyle = ReaderElementStyle(),
    val heading2: ReaderElementStyle = ReaderElementStyle(),
    val heading3: ReaderElementStyle = ReaderElementStyle(),
    val heading4: ReaderElementStyle = ReaderElementStyle(),
    val heading5: ReaderElementStyle = ReaderElementStyle(),
    val heading6: ReaderElementStyle = ReaderElementStyle(),
    val externalLink: ReaderElementStyle = ReaderElementStyle(),
    val internalLink: ReaderElementStyle = ReaderElementStyle()
) {
    fun styleFor(element: ReaderTextElement): ReaderElementStyle = when (element) {
        ReaderTextElement.PARAGRAPH -> paragraph
        ReaderTextElement.HEADING_1 -> heading1
        ReaderTextElement.HEADING_2 -> heading2
        ReaderTextElement.HEADING_3 -> heading3
        ReaderTextElement.HEADING_4 -> heading4
        ReaderTextElement.HEADING_5 -> heading5
        ReaderTextElement.HEADING_6 -> heading6
        ReaderTextElement.EXTERNAL_LINK -> externalLink
        ReaderTextElement.INTERNAL_LINK -> internalLink
    }

    fun update(element: ReaderTextElement, transform: (ReaderElementStyle) -> ReaderElementStyle): ReaderElementStyles =
        when (element) {
            ReaderTextElement.PARAGRAPH -> copy(paragraph = transform(paragraph))
            ReaderTextElement.HEADING_1 -> copy(heading1 = transform(heading1))
            ReaderTextElement.HEADING_2 -> copy(heading2 = transform(heading2))
            ReaderTextElement.HEADING_3 -> copy(heading3 = transform(heading3))
            ReaderTextElement.HEADING_4 -> copy(heading4 = transform(heading4))
            ReaderTextElement.HEADING_5 -> copy(heading5 = transform(heading5))
            ReaderTextElement.HEADING_6 -> copy(heading6 = transform(heading6))
            ReaderTextElement.EXTERNAL_LINK -> copy(externalLink = transform(externalLink))
            ReaderTextElement.INTERNAL_LINK -> copy(internalLink = transform(internalLink))
        }
}

data class ReaderSettings(
    val readingMode: ReadingMode = ReadingMode.SCROLL,
    val readerTheme: ReaderTheme = ReaderTheme.SYSTEM,
    val focusText: Boolean = false,
    val focusTextBoldness: Int = 700,
    val focusTextEmphasis: Float = 0.45f,
    val focusTextColor: Int? = null,
    val fontSizeSp: Float = 15f,
    val lineSpacing: Float = 1.55f,
    val horizontalMarginDp: Float = 20f,
    val font: ReaderFont = ReaderFont.DEFAULT,
    val focusMode: Boolean = false,
    val hideStatusBar: Boolean = false,
    val customBackgroundColor: Int? = null,
    val backgroundImageUri: String? = null,
    val backgroundImageBlur: Float = 0f,
    val backgroundImageOpacity: Float = 1f,
    val backgroundImageZoom: Float = 1f,
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
    val invertPageTurns: Boolean = false,
    val pageTransitionStyle: PageTransitionStyle = PageTransitionStyle.DEFAULT,
    val textAlignment: TextAlignment = TextAlignment.DEFAULT,
    val elementStyles: ReaderElementStyles = ReaderElementStyles(),
    val ambientMode: Boolean = false,
    val leftTapAction: ReaderTapZoneAction = ReaderTapZoneAction.PREVIOUS_PAGE,
    val rightTapAction: ReaderTapZoneAction = ReaderTapZoneAction.NEXT_PAGE,
    val topTapAction: ReaderTapZoneAction = ReaderTapZoneAction.TOGGLE_UI,
    val bottomTapAction: ReaderTapZoneAction = ReaderTapZoneAction.TOGGLE_UI
)
