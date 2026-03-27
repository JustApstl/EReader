package com.dyu.ereader.data.repository.browse

import com.dyu.ereader.core.net.normalizeSourceUrl
import java.net.URL

internal fun normalizeBrowseCatalogUrl(raw: String): String? = normalizeSourceUrl(raw)

internal fun validateBrowseCatalogUrl(raw: String): String? {
    val normalizedUrl = normalizeBrowseCatalogUrl(raw) ?: return "Enter a valid OPDS catalog URL."
    return unsupportedBrowseCatalogMessage(normalizedUrl)
}

internal fun unsupportedBrowseCatalogMessage(url: String): String? {
    return when {
        isUnsupportedAnnaArchiveCatalog(url) -> unsupportedAnnaArchiveOpdsMessage()
        else -> null
    }
}

internal fun isUnsupportedAnnaArchiveCatalog(url: String): Boolean {
    val host = runCatching { URL(url).host.lowercase() }.getOrDefault(url.lowercase())
    return host.contains("annas-archive.")
}

internal fun unsupportedAnnaArchiveOpdsMessage(): String {
    return "Anna's Archive does not appear to expose an official OPDS catalog, so this source can't be loaded here. Please use another OPDS source."
}
