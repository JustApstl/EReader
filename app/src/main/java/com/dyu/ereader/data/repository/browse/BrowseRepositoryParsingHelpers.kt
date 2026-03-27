package com.dyu.ereader.data.repository.browse

import com.dyu.ereader.data.metadata.BookMetadataCleaner
import com.dyu.ereader.data.model.browse.BrowseBook
import com.dyu.ereader.data.model.browse.BrowseLink
import com.dyu.ereader.data.model.browse.BrowseNavigationGroup
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.net.URL

internal fun extractBrowseAuthor(entry: Element, ignoredAuthorTokens: Set<String>): String? {
    val authorNames = entry.select("author > name").eachText()
        .map { it.trim() }
        .filter { it.isNotBlank() && !ignoredAuthorTokens.contains(it.lowercase()) }
    val creatorNames = entry.select("dc\\:creator, dcterms\\:creator, creator").eachText()
        .map { it.trim() }
        .filter { it.isNotBlank() && !ignoredAuthorTokens.contains(it.lowercase()) }
    val author = (authorNames + creatorNames).distinct().joinToString(", ").ifBlank {
        entry.select("author").text().trim()
    }
    return author.takeIf { it.isNotBlank() && !ignoredAuthorTokens.contains(it.lowercase()) }
}

internal fun parseBrowseEntry(
    entry: Element,
    baseUrl: String,
    profile: OpdsProfile,
    ignoredAuthorTokens: Set<String>
): BrowseBook {
    val id = entry.select("id").text().ifBlank {
        entry.select("guid").text().ifBlank {
            entry.select("link").first()?.attr("href") ?: "unknown"
        }
    }
    val title = BookMetadataCleaner.cleanTitle(entry.select("title").text().ifBlank { "Untitled" })
    val links = entry.select("link")
    val detailUrl = if (profile.hostHint.contains("wikisource")) {
        links.firstOrNull { link ->
            link.attr("rel").lowercase().contains("alternate") &&
                link.attr("type").lowercase().contains("text/html")
        }?.attr("href")?.let { resolveBrowseUrl(baseUrl, it, profile) }
            ?: entry.select("dc\\:source, dc\\:identifier").firstOrNull()?.text()?.let { resolveBrowseUrl(baseUrl, it, profile) }
    } else {
        links.firstOrNull(::isBrowseOpdsDetailLink)
            ?.attr("href")
            ?.let { resolveBrowseUrl(baseUrl, it, profile) }
    }
    val contentText = entry.selectFirst("content")?.text()?.trim().orEmpty()
    val inferredAuthor = extractBrowseAuthor(entry, ignoredAuthorTokens)
        ?: inferBrowseAuthorFromContent(entry, contentText, detailUrl)
    val author = BookMetadataCleaner.cleanAuthor(inferredAuthor ?: "Unknown Author")
    val subjects = BookMetadataCleaner.cleanValues(entry.select("category[term]").mapNotNull { element ->
        element.attr("term").trim().takeIf { it.isNotBlank() }
    })
    val languages = BookMetadataCleaner.cleanValues(
        entry.select("dc\\:language, dcterms\\:language, language").eachText()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { BookMetadataCleaner.cleanLanguageTag(it) }
    )
    val summary = BookMetadataCleaner.cleanDescription(
        entry.select("summary, description").text().ifBlank {
            contentText.takeIf {
                it.isNotBlank() &&
                    !it.equals(inferredAuthor, ignoreCase = true) &&
                    !it.equals(author, ignoreCase = true)
            }
        }
    )
    val downloads = extractBrowseDownloadCount(entry.select("content").text().ifBlank { summary })
    val rights = entry.select("rights").text().ifBlank { null }
    val publisher = BookMetadataCleaner.cleanPublisher(entry.select("dc\\:publisher, publisher").text().ifBlank { null })
    val published = BookMetadataCleaner.cleanPublishedDate(
        entry.select("published, dc\\:date, dcterms\\:date, dcterms\\:issued, dc\\:issued").text().ifBlank { null }
    )

    var coverUrl = selectBrowseCoverUrl(links, baseUrl, profile)
        ?: entry.select("media\\:thumbnail, media\\:content").firstOrNull()?.attr("url")?.let { resolveBrowseUrl(baseUrl, it, profile) }
        ?: entry.select("content").firstOrNull()?.let { content ->
            runCatching {
                val rawHtml = content.html().ifBlank { content.text() }
                val html = Jsoup.parse(rawHtml)
                html.selectFirst("img")?.attr("src")
            }.getOrNull()?.let { resolveBrowseUrl(baseUrl, it, profile) }
        }
    if (profile.hostHint.contains("gutenberg")) {
        val fallback = gutenbergCoverFallback(entry)
        if (coverUrl.isNullOrBlank() || coverUrl.startsWith("data:image")) {
            coverUrl = fallback ?: coverUrl
        }
    }

    val downloadOptions = selectBrowseDownloadOptions(links, baseUrl, profile)
    val downloadLink = selectBrowseDownloadLink(links, baseUrl, profile)
        ?: entry.select("enclosure[type*=epub], enclosure[type*=pdf]").first()

    val primaryOption = downloadOptions.firstOrNull()
    val downloadUrl = primaryOption?.url ?: resolveBrowseUrl(baseUrl, downloadLink?.attr("href") ?: "", profile)
    val downloadType = primaryOption?.mimeType ?: downloadLink?.attr("type").orEmpty()

    return BrowseBook(
        id = id,
        title = title,
        author = author,
        summary = summary?.ifBlank { null },
        coverUrl = coverUrl,
        downloadUrl = downloadUrl,
        detailUrl = detailUrl,
        format = primaryOption?.format ?: inferBrowseFormat(downloadUrl, downloadType),
        rights = rights,
        publisher = publisher,
        published = published,
        downloadOptions = if (downloadOptions.isNotEmpty()) {
            downloadOptions
        } else {
            buildBrowseDownloadOption(downloadLink, baseUrl, profile)?.let { listOf(it) }.orEmpty()
        },
        subjects = subjects,
        languages = languages,
        downloads = downloads
    )
}

internal fun shouldKeepBrowseBook(book: BrowseBook, catalogUrl: String): Boolean {
    val title = book.title.trim()
    if (title.isBlank()) return false
    val hasDownload = book.downloadOptions.isNotEmpty() ||
        (book.downloadUrl.isNotBlank() && book.downloadUrl != catalogUrl)
    if (hasDownload) return true
    val hasDetail = !book.detailUrl.isNullOrBlank()
    if (!hasDetail) return false
    val lowered = title.lowercase()
    val blockedTokens = listOf(
        "follow",
        "subscribe",
        "newsletter",
        "social",
        "facebook",
        "twitter",
        "instagram",
        "mastodon"
    )
    if (blockedTokens.any { lowered.contains(it) }) return false
    if (book.author.equals("Unknown Author", ignoreCase = true) && book.summary.isNullOrBlank()) return false
    return true
}

internal fun parseBrowseJsonLinks(links: JsonArray?, baseUrl: String, profile: OpdsProfile): List<BrowseLink> {
    if (links == null) return emptyList()
    return links.mapNotNull { element ->
        val obj = element.asJsonObject
        val href = resolveBrowseUrl(baseUrl, obj.get("href")?.asString.orEmpty(), profile)
        if (href.isBlank()) return@mapNotNull null
        BrowseLink(
            rel = obj.get("rel")?.asString.orEmpty(),
            href = href,
            type = obj.get("type")?.asString.orEmpty()
        )
    }
}

internal fun parseBrowseGroupPublications(groups: JsonArray?, baseUrl: String, profile: OpdsProfile): List<BrowseBook> {
    if (groups == null) return emptyList()
    return groups.flatMap { group ->
        val groupObj = group.asJsonObject
        val publications = groupObj.getAsJsonArray("publications") ?: JsonArray()
        publications.mapNotNull { publication ->
            parseBrowsePublication(publication, baseUrl, profile)
        }
    }
}

internal fun parseBrowseNavigationGroupsFromJson(root: JsonObject, baseUrl: String, profile: OpdsProfile): List<BrowseNavigationGroup> {
    val groups = mutableListOf<BrowseNavigationGroup>()
    groups += parseBrowseNavigationLinks(root.getAsJsonArray("navigation"), baseUrl, profile, null)
    val linkGroups = parseBrowseNavigationLinks(root.getAsJsonArray("links"), baseUrl, profile, null)
        .filter { link ->
            val rel = link.rel.lowercase()
            rel.contains("collection") || rel.contains("subsection") || rel.contains("related") || rel.contains("facet")
        }
    groups += linkGroups

    val facets = root.getAsJsonArray("facets") ?: JsonArray()
    facets.forEach { facet ->
        val facetObj = facet.asJsonObject
        val facetTitle = facetObj.getAsJsonObject("metadata")?.get("title")?.asString
        val facetLinks = facetObj.getAsJsonArray("links")
        if (facetLinks != null) {
            groups += parseBrowseNavigationLinks(facetLinks, baseUrl, profile, facetTitle)
        }
    }

    val opdsGroups = root.getAsJsonArray("groups") ?: JsonArray()
    opdsGroups.forEach { group ->
        val groupObj = group.asJsonObject
        val groupTitle = groupObj.getAsJsonObject("metadata")?.get("title")?.asString
        val navigation = groupObj.getAsJsonArray("navigation")
        if (navigation != null) {
            groups += parseBrowseNavigationLinks(navigation, baseUrl, profile, groupTitle)
        }
    }

    return groups.distinctBy { it.href }
}

internal fun parseBrowseNavigationLinks(
    links: JsonArray?,
    baseUrl: String,
    profile: OpdsProfile,
    fallbackTitle: String?
): List<BrowseNavigationGroup> {
    if (links == null) return emptyList()
    return links.mapNotNull { element ->
        val obj = element.asJsonObject
        val href = resolveBrowseUrl(baseUrl, obj.get("href")?.asString.orEmpty(), profile)
        if (href.isBlank()) return@mapNotNull null
        val title = obj.get("title")?.asString?.ifBlank { null }
            ?: obj.getAsJsonObject("metadata")?.get("title")?.asString
            ?: fallbackTitle
        BrowseNavigationGroup(
            title = title ?: "Section",
            href = href,
            rel = obj.get("rel")?.asString.orEmpty(),
            type = obj.get("type")?.asString.orEmpty()
        )
    }
}

internal fun parseBrowseNavigationGroupsFromXml(doc: Document, baseUrl: String, profile: OpdsProfile): List<BrowseNavigationGroup> {
    val navLinks = doc.select("feed > link[rel*=subsection], feed > link[rel*=collection], feed > link[rel*=related], feed > link[rel*=facet]")
    if (navLinks.isEmpty()) return emptyList()
    return navLinks.mapNotNull { link ->
        val href = resolveBrowseUrl(baseUrl, link.attr("href"), profile)
        if (href.isBlank() || href == baseUrl) return@mapNotNull null
        val rel = link.attr("rel").lowercase()
        if (rel.contains("acquisition")) return@mapNotNull null
        val title = link.attr("title").ifBlank { link.attr("rel") }
        val type = link.attr("type").lowercase()
        val looksLikeOpds = type.contains("atom+xml") ||
            type.contains("opds") ||
            type.contains("application/xml") ||
            href.contains(".opds")
        if (title.isBlank()) return@mapNotNull null
        if (!looksLikeOpds && !rel.contains("subsection")) return@mapNotNull null
        BrowseNavigationGroup(
            title = if (title.isBlank()) "Section" else title,
            href = href,
            rel = link.attr("rel"),
            type = link.attr("type")
        )
    }.distinctBy { it.href }
}

internal fun parseBrowseNavigationGroupsFromXmlEntries(
    entryElements: List<Element>,
    baseUrl: String,
    profile: OpdsProfile
): List<BrowseNavigationGroup> {
    return entryElements.mapNotNull { entry ->
        val navLink = entry.select("link").firstOrNull { link ->
            isBrowseNavigationLink(link, baseUrl, profile)
        } ?: return@mapNotNull null
        val href = resolveBrowseUrl(baseUrl, navLink.attr("href"), profile)
        if (href.isBlank()) return@mapNotNull null
        BrowseNavigationGroup(
            title = entry.selectFirst("title")?.text()?.ifBlank { navLink.attr("title") } ?: "Section",
            href = href,
            rel = navLink.attr("rel"),
            type = navLink.attr("type")
        )
    }.distinctBy { it.href }
}

internal fun isBrowseNavigationEntry(
    entry: Element,
    baseUrl: String,
    profile: OpdsProfile,
    parsedBook: BrowseBook
): Boolean {
    val links = entry.select("link")
    if (links.isEmpty()) return false
    val hasDownload = links.any { link ->
        val rel = link.attr("rel").lowercase()
        val type = link.attr("type").lowercase()
        val href = link.attr("href").lowercase()
        rel.contains("acquisition") &&
            (type.contains("epub") ||
                type.contains("pdf") ||
                type.contains("comicbook") ||
                type.contains("x-cbz") ||
                type.contains("x-cbr") ||
                href.contains(".epub") ||
                href.contains(".pdf") ||
                href.contains(".cbz") ||
                href.contains(".cbr"))
    }
    if (hasDownload) return false
    if (looksLikeBrowseBookDetailEntry(entry, parsedBook)) return false
    val navigationLinks = links.filter { isBrowseNavigationLink(it, baseUrl, profile) }
    if (navigationLinks.isEmpty()) return false
    val hasNonNavigationLinks = links.any { !isBrowseNavigationLink(it, baseUrl, profile) }
    return !hasNonNavigationLinks
}

internal fun isBrowseNavigationLink(link: Element, baseUrl: String, profile: OpdsProfile): Boolean {
    val rel = link.attr("rel").lowercase()
    val type = link.attr("type").lowercase()
    val href = resolveBrowseUrl(baseUrl, link.attr("href"), profile).lowercase()
    val looksLikeOpds = type.contains("opds-catalog") ||
        type.contains("atom+xml") ||
        type.contains("application/xml") ||
        href.contains("/opds")
    return (rel.contains("subsection") ||
        rel.contains("collection") ||
        rel.contains("related") ||
        rel.contains("facet") ||
        rel.contains("sort/")) && looksLikeOpds
}

internal fun looksLikeBrowseBookDetailEntry(entry: Element, parsedBook: BrowseBook): Boolean {
    if (parsedBook.downloadOptions.isNotEmpty()) return true
    if (!parsedBook.coverUrl.isNullOrBlank()) return true
    if (!parsedBook.detailUrl.isNullOrBlank() && isLikelyBrowseBookDetailUrl(parsedBook.detailUrl)) return true
    val contentType = entry.selectFirst("content")?.attr("type")?.lowercase().orEmpty()
    val contentText = entry.selectFirst("content")?.text()?.trim().orEmpty()
    if (contentType == "text" && contentText.isNotBlank()) return true
    return false
}

internal fun isLikelyBrowseBookDetailUrl(url: String?): Boolean {
    val target = url ?: return false
    val path = runCatching { URL(target).path.lowercase() }.getOrDefault(target.lowercase())
    val segments = path.split('/').filter { it.isNotBlank() }
    val lastSegment = segments.lastOrNull().orEmpty()
    val lastToken = lastSegment.substringBefore('.')
    val reservedTokens = setOf(
        "opds", "search", "titles", "title", "authors", "author", "topics", "topic",
        "new", "recent", "latest", "catalog", "category", "categories", "sections", "section",
        "collections", "collection", "tags", "tag"
    )
    if (lastToken in reservedTokens) return false
    if (lastToken.all { it.isDigit() } && lastToken.isNotBlank()) return true
    return segments.any { it == "ebooks" || it == "books" || it == "works" || it == "book" }
}

internal fun inferBrowseAuthorFromContent(entry: Element, contentText: String, detailUrl: String?): String? {
    if (contentText.isBlank()) return null
    val contentType = entry.selectFirst("content")?.attr("type")?.lowercase().orEmpty()
    if (contentType != "text") return null
    val hasBookSignals = !detailUrl.isNullOrBlank() || entry.select("link[rel*=thumbnail], link[rel*=image], media\\:thumbnail").isNotEmpty()
    if (!hasBookSignals) return null
    if (contentText.length > 120) return null
    return contentText
}

internal fun gutenbergNavigationGroupsFor(baseUrl: String): List<BrowseNavigationGroup> {
    val base = baseUrl.substringBefore('?').ifBlank { baseUrl }
    val root = if (base.endsWith('/')) base else "$base/"
    fun build(label: String, sortOrder: String): BrowseNavigationGroup {
        val href = "${root}?sort_order=$sortOrder"
        return BrowseNavigationGroup(title = label, href = href, rel = "collection", type = "application/atom+xml")
    }
    return listOf(
        build("Popular", "downloads"),
        build("New", "release_date"),
        build("Title", "title"),
        build("Author", "author")
    ).distinctBy { it.href }
}

internal fun standardEbooksNavigationGroupsFor(doc: Document, baseUrl: String, profile: OpdsProfile): List<BrowseNavigationGroup> {
    val groups = parseBrowseNavigationGroupsFromXml(doc, baseUrl, profile)
    if (groups.isEmpty()) return groups
    fun score(title: String): Int {
        val normalized = title.lowercase()
        return when {
            normalized.contains("all") -> 0
            normalized.contains("popular") -> 1
            normalized.contains("new") || normalized.contains("recent") -> 2
            normalized.contains("updated") -> 3
            normalized.contains("collections") || normalized.contains("series") -> 4
            else -> 5
        }
    }
    return groups.distinctBy { it.href }.sortedWith(compareBy<BrowseNavigationGroup> { score(it.title) }.thenBy { it.title })
}

internal fun parseBrowsePublication(publication: JsonElement, baseUrl: String, profile: OpdsProfile): BrowseBook? {
    if (!publication.isJsonObject) return null
    val obj = publication.asJsonObject
    val metadata = obj.getAsJsonObject("metadata")
    val rawTitle = metadata?.get("title")?.asString ?: return null
    val title = BookMetadataCleaner.cleanTitle(rawTitle)
    val identifier = metadata.get("identifier")?.asString ?: title
    val authors = metadata.get("authors")?.asJsonArray?.mapNotNull {
        it.asJsonObject.get("name")?.asString
    }.orEmpty()
    val author = BookMetadataCleaner.cleanAuthor(authors.joinToString(", "))
    val summary = BookMetadataCleaner.cleanDescription(
        metadata.get("description")?.asString ?: metadata.get("subtitle")?.asString
    )
    val publisher = BookMetadataCleaner.cleanPublisher(metadata.get("publisher")?.asString)
    val published = BookMetadataCleaner.cleanPublishedDate(
        metadata.get("published")?.asString ?: metadata.get("modified")?.asString
    )
    val rights = metadata.get("rights")?.asString ?: metadata.get("license")?.asString
    val languages = buildList {
        metadata.get("language")?.let { element ->
            runCatching { element.asString }.getOrNull()?.let(::add)
        }
        metadata.getAsJsonArray("languages")?.forEach { element ->
            runCatching { element.asString }.getOrNull()?.let(::add)
        }
    }.mapNotNull { BookMetadataCleaner.cleanLanguageTag(it) }.distinct()
    val subjects = buildList {
        metadata.get("subject")?.let { element ->
            runCatching { element.asString }.getOrNull()?.let(::add)
        }
        metadata.getAsJsonArray("subjects")?.forEach { element ->
            runCatching { element.asString }.getOrNull()?.let(::add)
        }
    }.let(BookMetadataCleaner::cleanValues)
    val downloads = runCatching { metadata.get("downloads")?.asInt }.getOrNull()
        ?: runCatching { metadata.get("downloadCount")?.asInt }.getOrNull()
        ?: runCatching { metadata.get("download_count")?.asInt }.getOrNull()
        ?: extractBrowseDownloadCount(summary)

    val links = obj.getAsJsonArray("links") ?: JsonArray()
    val imageLinks = obj.getAsJsonArray("images") ?: JsonArray()
    val allLinks = JsonArray().apply {
        imageLinks.forEach { add(it) }
        links.forEach { add(it) }
    }
    val coverUrl = selectBrowseCoverUrlFromJson(allLinks, baseUrl, profile)

    val downloadOptions = selectBrowseDownloadOptionsFromJson(links, baseUrl, profile)
    val primaryOption = downloadOptions.firstOrNull()
    val downloadUrl = primaryOption?.url.orEmpty()
    val format = primaryOption?.format ?: inferBrowseFormat(downloadUrl, primaryOption?.mimeType.orEmpty())
    val detailUrl = links.firstOrNull { element ->
        val rel = element.asJsonObject.get("rel")?.asString.orEmpty().lowercase()
        !rel.contains("acquisition") && (rel.contains("self") || rel.contains("alternate"))
    }?.let { resolveBrowseUrl(baseUrl, it.asJsonObject.get("href")?.asString.orEmpty(), profile) }

    return BrowseBook(
        id = identifier,
        title = title,
        author = author,
        summary = summary,
        coverUrl = coverUrl,
        downloadUrl = downloadUrl,
        detailUrl = detailUrl,
        format = format,
        rights = rights,
        publisher = publisher,
        published = published,
        downloadOptions = downloadOptions,
        subjects = subjects,
        languages = languages,
        downloads = downloads
    )
}
