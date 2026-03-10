package com.dyu.ereader.data.model

enum class DyslexiaFont {
    ROBOTO, OPENDYSLEXIC, ATKINSON_HYPERLEGIBLE
}

enum class AccessibilityMode {
    NORMAL, HIGH_CONTRAST, DYSLEXIA_FRIENDLY, SCREEN_READER, CUSTOM
}

data class AccessibilitySettings(
    val mode: AccessibilityMode = AccessibilityMode.NORMAL,
    val dyslexiaFont: DyslexiaFont = DyslexiaFont.ROBOTO,
    val highContrast: Boolean = false,
    val enhancedLineSpacing: Boolean = false,
    val largerTextSize: Boolean = false,
    val reduceAnimations: Boolean = false,
    val screenReaderEnabled: Boolean = false,
    val letterSpacing: Float = 0f,
    val wordSpacing: Float = 0f
)
