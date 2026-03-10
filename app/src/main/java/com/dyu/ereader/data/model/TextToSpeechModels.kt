package com.dyu.ereader.data.model

data class VoiceInfo(
    val name: String,
    val locale: String,
    val isDefault: Boolean = false
)

data class TextToSpeechSettings(
    val speed: Float = 1.0f, // 0.5 to 2.0
    val pitch: Float = 1.0f, // 0.5 to 2.0
    val volume: Float = 1.0f, // 0.0 to 1.0
    val language: String = "en-US",
    val voice: String? = null,
    val isEnabled: Boolean = false
)
