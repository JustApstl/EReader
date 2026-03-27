package com.dyu.ereader.core.locale

import java.util.Locale

fun displayLanguageName(languageTag: String?, locale: Locale = Locale.getDefault()): String? {
    val raw = languageTag?.trim().orEmpty()
    if (raw.isBlank()) return null
    val normalized = raw.replace('_', '-')
    val display = Locale.forLanguageTag(normalized).getDisplayLanguage(locale)
    if (display.isBlank() || display.equals("und", ignoreCase = true)) return raw
    return display.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

fun extractPublishedYear(rawDate: String?): String? {
    val text = rawDate?.trim().orEmpty()
    if (text.isBlank()) return null
    return Regex("\\b(\\d{4})\\b").find(text)?.value
}
