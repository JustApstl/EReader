package com.dyu.ereader.data.model.app

enum class AppTheme {
    SYSTEM, LIGHT, DARK, BLACK
}

enum class AppFont(val label: String) {
    SYSTEM("System"),
    EDITORIAL("Editorial"),
    SANS("Sans Serif"),
    SERIF("Serif"),
    MONO("Monospace")
}

enum class NavigationBarStyle {
    DEFAULT, FLOATING
}
