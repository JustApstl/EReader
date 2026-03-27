package com.dyu.ereader.data.model.app

import androidx.compose.ui.graphics.Color

enum class AppAccent(
    val label: String,
    private val seedArgb: Long
) {
    SYSTEM("System", 0x00000000),
    BLUE("Blue", 0xFF2C5B7D),
    TEAL("Teal", 0xFF177E89),
    GREEN("Green", 0xFF2E7D5B),
    AMBER("Amber", 0xFFB2833E),
    ROSE("Rose", 0xFFB65D7A),
    VIOLET("Violet", 0xFF6F6CCF),
    CUSTOM("Custom", 0x00000000);

    val seedColor: Color
        get() = if (this == SYSTEM || this == CUSTOM) Color.Unspecified else Color(seedArgb)

    companion object {
        fun fromName(name: String?): AppAccent {
            return when (name) {
                null -> SYSTEM
                "DEFAULT", "SYSTEM" -> SYSTEM
                else -> entries.find { it.name == name } ?: SYSTEM
            }
        }
    }
}
