package com.dyu.ereader.data.repository.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityManager
import com.dyu.ereader.data.model.accessibility.AccessibilityMode
import com.dyu.ereader.data.model.accessibility.AccessibilitySettings
import com.dyu.ereader.data.model.accessibility.DyslexiaFont
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AccessibilityRepository(
    private val context: Context
) {
    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private val _settings = MutableStateFlow(AccessibilitySettings())
    val settings: StateFlow<AccessibilitySettings> = _settings.asStateFlow()

    fun isAccessibilityServiceEnabled(): Boolean {
        return accessibilityManager.isEnabled
    }

    fun setMode(mode: AccessibilityMode) {
        _settings.value = _settings.value.copy(mode = mode)
    }

    fun setHighContrast(enabled: Boolean) {
        _settings.value = _settings.value.copy(highContrast = enabled)
    }

    fun setDyslexiaFont(font: DyslexiaFont) {
        _settings.value = _settings.value.copy(dyslexiaFont = font)
    }

    fun setEnhancedLineSpacing(enabled: Boolean) {
        _settings.value = _settings.value.copy(enhancedLineSpacing = enabled)
    }

    fun setLargerTextSize(enabled: Boolean) {
        _settings.value = _settings.value.copy(largerTextSize = enabled)
    }

    fun setReduceAnimations(enabled: Boolean) {
        _settings.value = _settings.value.copy(reduceAnimations = enabled)
    }

    fun setScreenReaderEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(screenReaderEnabled = enabled)
    }

    fun setLetterSpacing(spacing: Float) {
        _settings.value = _settings.value.copy(letterSpacing = spacing.coerceIn(0f, 1f))
    }

    fun setWordSpacing(spacing: Float) {
        _settings.value = _settings.value.copy(wordSpacing = spacing.coerceIn(0f, 1f))
    }
}
