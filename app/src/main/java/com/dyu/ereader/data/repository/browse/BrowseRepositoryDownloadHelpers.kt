package com.dyu.ereader.data.repository.browse

import com.dyu.ereader.data.model.browse.BrowseCatalog
import com.dyu.ereader.data.model.browse.BrowseDownloadOption
import com.google.gson.JsonArray
import org.jsoup.nodes.Element
import java.net.URL

internal fun inferBrowseFormat(downloadUrl: String, type: String, title: String? = null): String {
    val mime = type.lowercase()
    val href = downloadUrl.lowercase()
    val titleHint = title?.lowercase().orEmpty()
    val combined = "$mime $href $titleHint"
    val isEpub3 = combined.contains("epub3") ||
        combined.contains("epub-3") ||
        combined.contains("epub 3") ||
        combined.contains("epub/3") ||
        combined.contains("profile=epub3") ||
        combined.contains("profile=\"http://idpf.org/epub/30") ||
        combined.contains("profile=http://idpf.org/epub/30") ||
        combined.contains("version=3")
    return when {
        isEpub3 -> "epub3"
        mime.contains("epub") || href.contains(".epub") -> "epub"
        mime.contains("pdf") || href.contains(".pdf") -> "pdf"
        mime.contains("comicbook+zip") || mime.contains("x-cbz") || href.contains(".cbz") -> "cbz"
        mime.contains("comicbook-rar") || mime.contains("x-cbr") || href.contains(".cbr") -> "cbr"
        mime.contains("mobipocket") || href.contains(".mobi") -> "mobi"
        mime.contains("azw3") || href.contains(".azw3") -> "azw3"
        mime.contains("text/plain") || href.contains(".txt") -> "txt"
        mime.contains("text/html") || href.contains(".html") || href.contains(".htm") -> "html"
        else -> "epub"
    }
}

internal fun extractBrowseDownloadCount(text: String?): Int? {
    val raw = text?.replace('\u00A0', ' ')?.trim().orEmpty()
    if (raw.isBlank()) return null
    val match = Regex("(?i)downloads\\s*:?\\s*([\\d,]+)").find(raw) ?: return null
    return match.groupValues.getOrNull(1)?.replace(",", "")?.toIntOrNull()
}

internal fun resolveBrowseUrl(baseUrl: String, relativeUrl: String, profile: OpdsProfile? = null): String {
    if (relativeUrl.isBlank()) return ""
    val resolved = try {
        URL(URL(baseUrl), relativeUrl).toString()
    } catch (_: Exception) {
        relativeUrl
    }
    val shouldUpgrade = profile?.preferHttps == true || resolved.contains("gutenberg.org")
    return if (shouldUpgrade && resolved.startsWith("http://")) {
        resolved.replaceFirst("http://", "https://")
    } else {
        resolved
    }
}

internal fun selectBrowseCoverUrl(links: Collection<Element>, baseUrl: String, profile: OpdsProfile? = null): String? {
    val preferredRels = profile?.coverRelPriority.orEmpty()
    return links.mapNotNull { link ->
        val rel = link.attr("rel").lowercase()
        val type = link.attr("type").lowercase()
        val href = resolveBrowseUrl(baseUrl, link.attr("href"), profile)
        if (href.isBlank()) return@mapNotNull null
        val hrefLower = href.lowercase()

        val relScore = preferredRels.indexOfFirst { rel.contains(it) }.takeIf { it >= 0 }?.let { 200 - it } ?: 0
        val score = maxOf(
            relScore,
            when {
                rel.contains("image/thumbnail") || rel.contains("thumbnail") -> 100
                rel.contains("image") -> 90
                rel.contains("cover") -> 80
                type.startsWith("image/") -> 70
                hrefLower.contains("cover") -> 60
                hrefLower.endsWith(".jpg") || hrefLower.endsWith(".jpeg") || hrefLower.endsWith(".png") || hrefLower.endsWith(".webp") -> 50
                else -> 0
            }
        )

        if (score <= 0) return@mapNotNull null
        href to score
    }.maxByOrNull { it.second }?.first
}

internal fun isBrowseOpdsDetailLink(link: Element): Boolean {
    val rel = link.attr("rel").lowercase()
    val type = link.attr("type").lowercase()
    if (rel.contains("acquisition")) return false
    if (type.contains("epub") || type.contains("pdf")) return false
    if (rel.contains("subsection") || rel.contains("collection")) return true
    if (type.contains("opds-catalog")) return true
    if (type.contains("atom+xml") && !type.contains("text/html")) return true
    return false
}

internal fun selectBrowseDownloadLink(links: Collection<Element>, baseUrl: String, profile: OpdsProfile? = null): Element? {
    return links.mapNotNull { link ->
        val rel = link.attr("rel").lowercase()
        val type = link.attr("type").lowercase()
        val href = resolveBrowseUrl(baseUrl, link.attr("href"), profile)
        if (href.isBlank()) return@mapNotNull null

        val score = when {
            type.contains("application/epub+zip") -> 100
            href.lowercase().contains(".epub") -> 95
            type.contains("comicbook+zip") || type.contains("x-cbz") -> 92
            href.lowercase().contains(".cbz") -> 90
            type.contains("comicbook-rar") || type.contains("x-cbr") -> 88
            href.lowercase().contains(".cbr") -> 86
            type.contains("application/pdf") -> 80
            href.lowercase().contains(".pdf") -> 75
            rel.contains("acquisition") -> 60
            rel.contains("alternate") -> 30
            else -> 0
        }

        if (score <= 0) return@mapNotNull null
        if (!href.startsWith("http://") && !href.startsWith("https://")) return@mapNotNull null
        Triple(link, score, href)
    }.maxByOrNull { (_, score, _) -> score }?.first
}

internal fun selectBrowseDownloadOptions(links: Collection<Element>, baseUrl: String, profile: OpdsProfile? = null): List<BrowseDownloadOption> {
    val preferredRels = profile?.downloadRelPriority.orEmpty()
    val options = links.mapNotNull { link ->
        val rel = link.attr("rel").lowercase()
        val type = link.attr("type").lowercase()
        val href = link.attr("href")
        val hrefLower = href.lowercase()
        val relMatch = preferredRels.any { rel.contains(it) }
        val isCandidate = relMatch ||
            rel.contains("acquisition") ||
            type.contains("epub") ||
            type.contains("pdf") ||
            type.contains("comicbook+zip") ||
            type.contains("comicbook-rar") ||
            type.contains("x-cbz") ||
            type.contains("x-cbr") ||
            type.contains("mobipocket") ||
            type.contains("azw3") ||
            type.contains("text/plain") ||
            type.contains("text/html") ||
            hrefLower.contains(".epub") ||
            hrefLower.contains(".pdf") ||
            hrefLower.contains(".cbz") ||
            hrefLower.contains(".cbr") ||
            hrefLower.contains(".mobi") ||
            hrefLower.contains(".azw3") ||
            hrefLower.contains(".txt") ||
            hrefLower.contains(".html") ||
            hrefLower.contains(".htm")

        if (!isCandidate) return@mapNotNull null
        val resolvedHref = resolveBrowseUrl(baseUrl, href, profile)
        if (isExcludedBrowseDownloadLink(resolvedHref, type, rel)) return@mapNotNull null
        val option = buildBrowseDownloadOption(link, baseUrl, profile) ?: return@mapNotNull null
        val score = scoreBrowseDownloadOption(option, rel, type, resolvedHref, profile)
        option to score
    }.distinctBy { it.first.url }

    return options.sortedByDescending { it.second }.map { it.first }
}

internal fun isExcludedBrowseDownloadLink(href: String, type: String, rel: String): Boolean {
    val lower = href.lowercase()
    if (lower.startsWith("magnet:")) return true
    if (lower.endsWith(".torrent") || type.contains("bittorrent")) return true
    if (rel.contains("torrent") || rel.contains("magnet")) return true
    return false
}

internal fun scoreBrowseDownloadOption(
    option: BrowseDownloadOption,
    rel: String,
    type: String,
    href: String,
    profile: OpdsProfile?
): Int {
    val format = option.format.lowercase()
    val defaultFormatScore = when (format) {
        "epub", "epub3" -> 100
        "cbz" -> 95
        "cbr" -> 90
        "pdf" -> 80
        "azw3" -> 70
        "mobi" -> 60
        "txt" -> 45
        "html" -> 40
        else -> 50
    }
    val formatScore = profile?.formatScores?.get(format) ?: defaultFormatScore
    val relScore = when {
        rel.contains("open-access") -> 10
        rel.contains("acquisition") -> 6
        rel.contains("alternate") -> 2
        else -> 0
    }
    val tokenBoost = profile?.downloadTokenBoosts?.entries
        ?.firstOrNull { href.contains(it.key, ignoreCase = true) }
        ?.value ?: 0
    val typeBoost = if (type.contains("application/epub+zip")) 5 else 0
    val penalty = if (href.contains("sample", ignoreCase = true) || href.contains("preview", ignoreCase = true)) -10 else 0
    return formatScore + relScore + tokenBoost + typeBoost + penalty
}

internal fun gutenbergCoverFallback(entry: Element): String? {
    val id = extractGutenbergId(entry) ?: return null
    return "https://www.gutenberg.org/cache/epub/$id/pg$id.cover.medium.jpg"
}

internal fun extractGutenbergId(entry: Element): String? {
    val candidates = mutableListOf<String>()
    candidates += entry.select("id").text()
    candidates += entry.select("dc\\:identifier, dcterms\\:identifier, identifier").eachText()
    candidates += entry.select("link").eachAttr("href")
    val regexes = listOf(
        Regex("gutenberg\\.org/ebooks/(\\d+)", RegexOption.IGNORE_CASE),
        Regex("gutenberg\\.org/etext/(\\d+)", RegexOption.IGNORE_CASE),
        Regex("pg(\\d+)", RegexOption.IGNORE_CASE)
    )
    candidates.forEach { value ->
        regexes.forEach { regex ->
            val match = regex.find(value)
            if (match != null) return match.groupValues[1]
        }
    }
    return null
}

internal fun buildBrowseDownloadOption(link: Element?, baseUrl: String, profile: OpdsProfile? = null): BrowseDownloadOption? {
    if (link == null) return null
    val href = resolveBrowseUrl(baseUrl, link.attr("href"), profile)
    if (href.isBlank()) return null
    if (!href.startsWith("http://") && !href.startsWith("https://")) return null
    val type = link.attr("type").orEmpty()
    val titleHint = buildString {
        append(link.attr("title"))
        append(' ')
        append(link.attr("rel"))
    }
    val format = inferBrowseFormat(href, type, titleHint)
    val sizeBytes = link.attr("length").toLongOrNull()
    val label = link.attr("title").takeIf { it.isNotBlank() } ?: format.uppercase()
    return BrowseDownloadOption(
        url = href,
        format = format,
        mimeType = type.ifBlank { null },
        sizeBytes = sizeBytes,
        label = label
    )
}

internal fun selectBrowseCoverUrlFromJson(links: JsonArray, baseUrl: String, profile: OpdsProfile): String? {
    val preferred = profile.coverRelPriority
    return links.mapNotNull { element ->
        val obj = element.asJsonObject
        val href = resolveBrowseUrl(baseUrl, obj.get("href")?.asString.orEmpty(), profile)
        if (href.isBlank()) return@mapNotNull null
        val rel = obj.get("rel")?.asString.orEmpty().lowercase()
        val type = obj.get("type")?.asString.orEmpty().lowercase()
        val score = maxOf(
            preferred.indexOfFirst { rel.contains(it) }.takeIf { it >= 0 }?.let { 200 - it } ?: 0,
            when {
                rel.contains("thumbnail") -> 100
                rel.contains("image") -> 90
                type.startsWith("image/") -> 80
                href.lowercase().contains("cover") -> 70
                else -> 0
            }
        )
        if (score <= 0) return@mapNotNull null
        href to score
    }.maxByOrNull { it.second }?.first
}

internal fun selectBrowseDownloadOptionsFromJson(links: JsonArray, baseUrl: String, profile: OpdsProfile): List<BrowseDownloadOption> {
    val preferred = profile.downloadRelPriority
    val options = links.mapNotNull { element ->
        val obj = element.asJsonObject
        val rel = obj.get("rel")?.asString.orEmpty().lowercase()
        val type = obj.get("type")?.asString.orEmpty().lowercase()
        val href = resolveBrowseUrl(baseUrl, obj.get("href")?.asString.orEmpty(), profile)
        if (href.isBlank()) return@mapNotNull null

        val relMatch = preferred.any { rel.contains(it) }
        val isCandidate = relMatch ||
            rel.contains("acquisition") ||
            type.contains("epub") ||
            type.contains("pdf") ||
            type.contains("comicbook+zip") ||
            type.contains("comicbook-rar") ||
            type.contains("x-cbz") ||
            type.contains("x-cbr") ||
            type.contains("mobipocket") ||
            type.contains("azw3") ||
            type.contains("text/plain") ||
            type.contains("text/html") ||
            href.lowercase().contains(".epub") ||
            href.lowercase().contains(".pdf") ||
            href.lowercase().contains(".cbz") ||
            href.lowercase().contains(".cbr") ||
            href.lowercase().contains(".mobi") ||
            href.lowercase().contains(".azw3") ||
            href.lowercase().contains(".txt") ||
            href.lowercase().contains(".html") ||
            href.lowercase().contains(".htm")
        if (!isCandidate) return@mapNotNull null
        if (isExcludedBrowseDownloadLink(href, type, rel)) return@mapNotNull null

        val format = inferBrowseFormat(href, type, obj.get("title")?.asString)
        val sizeBytes = obj.get("length")?.asLong
        val label = obj.get("title")?.asString?.takeIf { it.isNotBlank() } ?: format.uppercase()
        val option = BrowseDownloadOption(
            url = href,
            format = format,
            mimeType = type.ifBlank { null },
            sizeBytes = sizeBytes,
            label = label
        )
        val score = scoreBrowseDownloadOption(option, rel, type, href, profile)
        option to score
    }.distinctBy { it.first.url }

    return options.sortedByDescending { it.second }.map { it.first }
}

internal fun buildBrowseCatalogError(catalog: BrowseCatalog, statusCode: Int, statusMessage: String?): String {
    val host = runCatching { URL(catalog.url).host.lowercase() }.getOrDefault("")
    return when {
        isUnsupportedAnnaArchiveCatalog(catalog.url) ->
            unsupportedAnnaArchiveOpdsMessage()
        catalog.id == "standard_ebooks" && (statusCode == 401 || statusCode == 403) ->
            "Standard Ebooks OPDS requires Patrons Circle login (HTTP $statusCode). Add your Patron email in a custom source or use another catalog."
        statusCode == 401 || statusCode == 403 ->
            "This OPDS source rejected the request (HTTP $statusCode). If it is self-hosted, add the correct username/password or API key."
        host.contains("gutenberg.org") && statusCode == 403 ->
            "Project Gutenberg rejected the request (HTTP 403). They require a custom User-Agent with contact info; try adding a custom source with your contact details."
        host.contains("feedbooks.com") && (statusCode == 403 || statusCode == 404) ->
            "Feedbooks OPDS appears unavailable (HTTP $statusCode). Try another catalog or add a custom source."
        else ->
            "Failed to load catalog (HTTP $statusCode${if (!statusMessage.isNullOrBlank()) ": $statusMessage" else ""})."
    }
}
