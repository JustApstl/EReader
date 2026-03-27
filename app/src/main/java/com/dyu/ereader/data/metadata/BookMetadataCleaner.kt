package com.dyu.ereader.data.metadata

import java.util.Locale

data class ParsedFileNameMetadata(
    val title: String,
    val author: String? = null
)

object BookMetadataCleaner {
    fun cleanTitle(rawTitle: String?, fallbackFileName: String? = null): String {
        val candidate = rawTitle?.takeIf { it.isNotBlank() }
            ?: parseFileNameMetadata(fallbackFileName).title
        return candidate
            .normalizeWhitespace()
            .removeKnownPrefix("title:")
            .ifBlank { "Untitled" }
    }

    fun cleanAuthor(rawAuthor: String?, fallbackFileName: String? = null, fallback: String = "Unknown Author"): String {
        val candidate = rawAuthor?.takeIf { it.isNotBlank() && !it.equals("unknown", true) }
            ?: parseFileNameMetadata(fallbackFileName).author
        return candidate
            ?.normalizeWhitespace()
            ?.removeKnownPrefix("author:")
            ?.takeIf { it.isNotBlank() && !it.equals("unknown", true) }
            ?: fallback
    }

    fun cleanDescription(rawDescription: String?): String? {
        return rawDescription
            ?.replace(Regex("\\s+"), " ")
            ?.replace("�", "")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun cleanPublisher(rawPublisher: String?): String? {
        return rawPublisher?.normalizeWhitespace()?.takeIf { it.isNotBlank() }
    }

    fun cleanPublishedDate(rawPublishedDate: String?): String? {
        val normalized = rawPublishedDate?.normalizeWhitespace()?.takeIf { it.isNotBlank() } ?: return null
        val year = Regex("(\\d{4})").find(normalized)?.groupValues?.getOrNull(1)
        return year ?: normalized
    }

    fun cleanLanguageTag(rawLanguage: String?): String? {
        return rawLanguage
            ?.trim()
            ?.replace('_', '-')
            ?.takeIf { it.isNotBlank() }
    }

    fun cleanValues(values: List<String>): List<String> {
        return values
            .map { it.normalizeWhitespace() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }
            .sortedBy { it.lowercase(Locale.ROOT) }
    }

    fun parseFileNameMetadata(fileName: String?): ParsedFileNameMetadata {
        val baseName = fileName
            ?.substringAfterLast('/')
            ?.substringBeforeLast('.', missingDelimiterValue = fileName)
            ?.replace('_', ' ')
            ?.replace('.', ' ')
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            .orEmpty()

        if (baseName.isBlank()) {
            return ParsedFileNameMetadata(title = "Untitled")
        }

        val separators = listOf(" - ", " — ", " – ")
        val parts = separators.firstNotNullOfOrNull { separator ->
            baseName.split(separator).takeIf { it.size == 2 }
        }

        if (parts != null) {
            val first = parts[0].normalizeWhitespace()
            val second = parts[1].normalizeWhitespace()
            val firstLooksLikeAuthor = first.contains(",") || first.split(' ').size <= 4
            return if (firstLooksLikeAuthor) {
                ParsedFileNameMetadata(title = second, author = first)
            } else {
                ParsedFileNameMetadata(title = first, author = second)
            }
        }

        return ParsedFileNameMetadata(title = baseName.normalizeWhitespace())
    }

    private fun String.normalizeWhitespace(): String {
        return replace(Regex("\\s+"), " ").trim()
    }

    private fun String.removeKnownPrefix(prefix: String): String {
        return removePrefix(prefix).trim()
    }
}
