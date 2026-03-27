package com.dyu.ereader.data.repository.browse

import android.content.Context
import android.util.Base64
import com.dyu.ereader.data.metadata.BookMetadataCleaner
import com.dyu.ereader.data.model.browse.BrowseBook
import com.dyu.ereader.data.model.browse.BrowseCatalog
import com.dyu.ereader.data.model.browse.BrowseDownloadOption
import com.dyu.ereader.data.model.browse.BrowseFeed
import com.dyu.ereader.data.model.browse.BrowseLink
import com.dyu.ereader.data.model.browse.BrowseNavigationGroup
import com.dyu.ereader.data.model.browse.BrowseTransferProgress
import com.dyu.ereader.data.model.browse.CatalogHealthStatus
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.parser.Parser
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.io.File

class BrowseRepository(
    private val context: Context
) {
    private val gson = Gson()
    private val defaultCatalogIds = DEFAULT_CATALOG_IDS
    private val prefs by lazy { context.getSharedPreferences("browse_sources", Context.MODE_PRIVATE) }

    private val _catalogs = MutableStateFlow(DEFAULT_CATALOGS + loadCustomCatalogs())
    val catalogs: StateFlow<List<BrowseCatalog>> = _catalogs.asStateFlow()

    private val _currentFeed = MutableStateFlow<BrowseFeed?>(null)
    val currentFeed: StateFlow<BrowseFeed?> = _currentFeed.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private val _catalogHealth = MutableStateFlow<Map<String, CatalogHealthStatus>>(emptyMap())
    val catalogHealth: StateFlow<Map<String, CatalogHealthStatus>> = _catalogHealth.asStateFlow()
    private val _isLoadingNext = MutableStateFlow(false)
    val isLoadingNext: StateFlow<Boolean> = _isLoadingNext.asStateFlow()

    private val DETAIL_ENRICH_LIMIT = 40
    private val IGNORED_AUTHOR_TOKENS = setOf("unknown", "unknown author")
    private var currentCatalog: BrowseCatalog? = null
    private var currentSearchTemplate: String? = null
    private var currentProfile: OpdsProfile? = null
    private val HEALTH_CHECK_TIMEOUT_MS = 6000
    @Volatile
    private var cancelDownloadRequested = false

    private val opdsProfiles = OPDS_PROFILES

    private fun browseRequest(
        url: String,
        profile: OpdsProfile,
        catalog: BrowseCatalog? = currentCatalog,
        acceptHeader: String = profile.acceptHeader
    ) = Jsoup.connect(url)
        .userAgent(profile.userAgent)
        .header("Accept", acceptHeader)
        .header("Accept-Language", "en-US,en;q=0.9")
        .ignoreContentType(true)
        .ignoreHttpErrors(true)
        .followRedirects(true)
        .timeout(15000)
        .apply { applyCatalogAuthHeaders(this, catalog) }

    private fun applyCatalogAuthHeaders(connection: org.jsoup.Connection, catalog: BrowseCatalog?) {
        buildCatalogAuthHeaders(catalog).forEach { (name, value) ->
            connection.header(name, value)
        }
    }

    private fun applyCatalogAuthHeaders(connection: HttpURLConnection, catalog: BrowseCatalog?) {
        buildCatalogAuthHeaders(catalog).forEach { (name, value) ->
            connection.setRequestProperty(name, value)
        }
    }

    private fun buildCatalogAuthHeaders(catalog: BrowseCatalog?): Map<String, String> {
        if (catalog == null) return emptyMap()
        val headers = linkedMapOf<String, String>()
        catalog.apiKey?.trim()?.takeIf { it.isNotBlank() }?.let { headers["X-API-Key"] = it }
        val directUsername = catalog.username?.trim().orEmpty()
        val directPassword = catalog.password.orEmpty()
        val userInfo = runCatching { URL(catalog.url).userInfo }.getOrNull()
        val basicPair = when {
            directUsername.isNotBlank() -> directUsername to directPassword
            !userInfo.isNullOrBlank() -> {
                val parts = userInfo.split(':', limit = 2)
                parts.firstOrNull().orEmpty() to parts.getOrNull(1).orEmpty()
            }
            else -> null
        }
        basicPair?.let { (username, password) ->
            val token = Base64.encodeToString("$username:$password".toByteArray(), Base64.NO_WRAP)
            headers["Authorization"] = "Basic $token"
        }
        return headers
    }

    suspend fun loadCatalog(catalog: BrowseCatalog) = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _error.value = null
        if (isUnsupportedAnnaArchiveCatalog(catalog.url)) {
            _error.value = unsupportedAnnaArchiveOpdsMessage()
            _catalogHealth.value = _catalogHealth.value + (catalog.id to CatalogHealthStatus.ERROR)
            _isLoading.value = false
            return@withContext
        }
        currentCatalog = catalog
        val profile = profileFor(catalog)
        currentProfile = profile
        _catalogHealth.value = _catalogHealth.value + (catalog.id to CatalogHealthStatus.CHECKING)
        try {
            val response = browseRequest(catalog.url, profile, catalog).execute()

            if (response.statusCode() >= 400) {
                _error.value = buildCatalogError(catalog, response.statusCode(), response.statusMessage())
                _catalogHealth.value = _catalogHealth.value + (catalog.id to CatalogHealthStatus.ERROR)
                return@withContext
            }
            
            val body = response.body()
            val isJson = response.contentType()?.contains("json", ignoreCase = true) == true ||
                body.trimStart().startsWith("{")

            val initialFeed = if (isJson) {
                parseOpdsJson(body, catalog, profile)
            } else {
                parseOpdsXml(body, catalog, profile)
            }
            _currentFeed.value = initialFeed
            _catalogHealth.value = _catalogHealth.value + (catalog.id to CatalogHealthStatus.ONLINE)

            val enrichedEntries = enrichEntries(initialFeed.entries, catalog.url)
            if (enrichedEntries != initialFeed.entries && currentCatalog?.id == catalog.id) {
                _currentFeed.value = initialFeed.copy(entries = enrichedEntries)
            }
        } catch (e: Exception) {
            _error.value = "Failed to load catalog: ${e.localizedMessage ?: e.javaClass.simpleName}"
            _catalogHealth.value = _catalogHealth.value + (catalog.id to CatalogHealthStatus.ERROR)
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    private fun profileFor(catalog: BrowseCatalog): OpdsProfile {
        val host = runCatching { URL(catalog.url).host.lowercase() }.getOrDefault("")
        return opdsProfiles.firstOrNull { profile ->
            profile.id == catalog.id || host.contains(profile.hostHint)
        } ?: OpdsProfile(id = catalog.id, hostHint = host)
    }

    private fun parseOpdsXml(body: String, catalog: BrowseCatalog, profile: OpdsProfile): BrowseFeed {
        val doc = Jsoup.parse(body, "", Parser.xmlParser())

        val entryElements = doc.select("entry")
        val parsedEntries = entryElements.map { entry -> entry to parseEntry(entry, catalog.url, profile) }
        val navigationEntries = parsedEntries.filter { (entry, book) ->
            isNavigationEntry(entry, catalog.url, profile, book)
        }.map { it.first }
        val navigationEntryGroups = parseNavigationGroupsFromXmlEntries(navigationEntries, catalog.url, profile)
        val entries = parsedEntries
            .filterNot { (entry, book) -> isNavigationEntry(entry, catalog.url, profile, book) }
            .map { it.second }
            .filter { isBrowseBook(it, catalog.url) }

        val linkElements = doc.select("link")
        val links = linkElements.map { link ->
            BrowseLink(
                rel = link.attr("rel"),
                href = resolveUrl(catalog.url, link.attr("href"), profile),
                type = link.attr("type")
            )
        }

        currentSearchTemplate = profile.searchTemplateOverride ?: resolveSearchTemplate(doc, catalog.url, profile)

        val feedId = doc.select("id").first()?.text() ?: catalog.id
        val feedTitle = doc.select("title").first()?.text() ?: catalog.title
        val navigationGroups = when {
            profile.hostHint.contains("gutenberg") -> gutenbergNavigationGroups(catalog.url)
            profile.hostHint.contains("standardebooks") -> standardEbooksNavigationGroups(doc, catalog.url, profile)
            else -> (parseNavigationGroupsFromXml(doc, catalog.url, profile) + navigationEntryGroups).distinctBy { it.href }
        }
        return BrowseFeed(
            id = feedId,
            title = feedTitle,
            entries = entries,
            links = links,
            navigationGroups = navigationGroups
        )
    }

    private fun parseOpdsJson(body: String, catalog: BrowseCatalog, profile: OpdsProfile): BrowseFeed {
        val root = gson.fromJson(body, JsonObject::class.java) ?: JsonObject()
        val metadata = root.getAsJsonObject("metadata")
        val feedTitle = metadata?.get("title")?.asString ?: catalog.title
        val feedId = metadata?.get("identifier")?.asString ?: catalog.id
        val links = parseJsonLinks(root.getAsJsonArray("links"), catalog.url, profile)
        val publications = root.getAsJsonArray("publications") ?: JsonArray()
        val groupEntries = parseGroupPublications(root.getAsJsonArray("groups"), catalog.url, profile)
        val entries = if (publications.size() > 0) {
            publications.mapNotNull { publication ->
                parsePublication(publication, catalog.url, profile)
            }
        } else {
            groupEntries
        }.filter { isBrowseBook(it, catalog.url) }
        val navigationGroups = parseNavigationGroupsFromJson(root, catalog.url, profile)
        currentSearchTemplate = profile.searchTemplateOverride ?: resolveSearchTemplateFromJson(root, catalog.url, profile)
        return BrowseFeed(
            id = feedId,
            title = feedTitle,
            entries = entries,
            links = links,
            navigationGroups = navigationGroups
        )
    }

    fun clearFeed() {
        _currentFeed.value = null
        _error.value = null
        currentCatalog = null
        currentSearchTemplate = null
    }

    suspend fun refreshCatalogHealth(catalogs: List<BrowseCatalog> = _catalogs.value) = withContext(Dispatchers.IO) {
        val updated = mutableMapOf<String, CatalogHealthStatus>()
        catalogs.forEach { catalog ->
            if (isUnsupportedAnnaArchiveCatalog(catalog.url)) {
                updated[catalog.id] = CatalogHealthStatus.ERROR
                _catalogHealth.value = _catalogHealth.value + (catalog.id to CatalogHealthStatus.ERROR)
                return@forEach
            }
            updated[catalog.id] = CatalogHealthStatus.CHECKING
            _catalogHealth.value = _catalogHealth.value + (catalog.id to CatalogHealthStatus.CHECKING)
            val status = runCatching {
                val connection = URL(catalog.url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", BROWSE_USER_AGENT)
                connection.connectTimeout = HEALTH_CHECK_TIMEOUT_MS
                connection.readTimeout = HEALTH_CHECK_TIMEOUT_MS
                connection.instanceFollowRedirects = true
                applyCatalogAuthHeaders(connection, catalog)
                val code = connection.responseCode
                if (code in 200..399) CatalogHealthStatus.ONLINE else CatalogHealthStatus.ERROR
            }.getOrElse { CatalogHealthStatus.ERROR }
            updated[catalog.id] = status
            _catalogHealth.value = _catalogHealth.value + (catalog.id to status)
        }
    }

    private fun extractAuthor(entry: Element): String? =
        extractBrowseAuthor(entry, IGNORED_AUTHOR_TOKENS)

    private fun parseEntry(entry: Element, baseUrl: String, profile: OpdsProfile): BrowseBook =
        parseBrowseEntry(entry, baseUrl, profile, IGNORED_AUTHOR_TOKENS)

    private fun isBrowseBook(book: BrowseBook, catalogUrl: String): Boolean =
        shouldKeepBrowseBook(book, catalogUrl)

    private suspend fun enrichEntries(entries: List<BrowseBook>, baseUrl: String): List<BrowseBook> {
        val targets = entries.withIndex()
            .filter { (_, book) ->
                val needsCover = book.coverUrl.isNullOrBlank()
                val needsAuthor = book.author.equals("Unknown Author", ignoreCase = true)
                val hasDetail = !book.detailUrl.isNullOrBlank()
                hasDetail && (needsCover || needsAuthor)
            }
            .take(DETAIL_ENRICH_LIMIT)

        if (targets.isEmpty()) return entries

        val updated = entries.toMutableList()
        for ((index, book) in targets) {
            val detailUrl = book.detailUrl ?: continue
            val details = resolveEntryDetails(detailUrl) ?: continue
            val updatedBook = book.copy(
                coverUrl = if (book.coverUrl.isNullOrBlank()) details.coverUrl ?: book.coverUrl else book.coverUrl,
                author = if (book.author.equals("Unknown Author", ignoreCase = true) && !details.author.isNullOrBlank()) {
                    details.author
                } else {
                    book.author
                },
                summary = if (book.summary.isNullOrBlank()) details.summary ?: book.summary else book.summary,
                rights = book.rights ?: details.rights,
                publisher = book.publisher ?: details.publisher,
                published = book.published ?: details.published,
                subjects = if (book.subjects.isEmpty()) details.subjects else book.subjects,
                languages = if (book.languages.isEmpty()) details.languages else book.languages,
                downloads = book.downloads ?: details.downloads
            )
            updated[index] = updatedBook
        }

        return updated
    }

    private suspend fun resolveEntryDetails(detailUrl: String): EntryDetails? {
        val wikisourceDetails = resolveWikisourceDetails(detailUrl)
        if (wikisourceDetails != null) return wikisourceDetails
        val profile = currentProfile
        return try {
            val response = browseRequest(
                url = detailUrl,
                profile = profile ?: OpdsProfile("detail", detailUrl),
                catalog = currentCatalog,
                acceptHeader = "application/atom+xml,application/xml;q=0.9,*/*;q=0.8"
            ).execute()

            if (response.statusCode() >= 400) {
                return null
            }

            val body = response.body()
            if (response.contentType()?.contains("json", ignoreCase = true) == true || body.trimStart().startsWith("{")) {
                val publication = runCatching { gson.fromJson(body, JsonObject::class.java) }.getOrNull()
                    ?.let { parsePublication(it, detailUrl, profile ?: OpdsProfile("detail", detailUrl)) }
                return publication?.let {
                    EntryDetails(
                        author = it.author,
                        coverUrl = it.coverUrl,
                        summary = it.summary,
                        rights = it.rights,
                        publisher = it.publisher,
                        published = it.published,
                        subjects = it.subjects,
                        languages = it.languages
                    )
                }
            }

            val doc = Jsoup.parse(body, "", Parser.xmlParser())
            val entry = doc.select("entry").firstOrNull() ?: doc
            val links = entry.select("link")

            var coverUrl = selectCoverUrl(links, detailUrl, profile)
                ?: entry.select("media\\:thumbnail, media\\:content").firstOrNull()?.attr("url")?.let { resolveUrl(detailUrl, it, profile) }
                ?: entry.select("content").firstOrNull()?.let { content ->
                    runCatching {
                        val rawHtml = content.html().ifBlank { content.text() }
                        val html = Jsoup.parse(rawHtml)
                        html.selectFirst("img")?.attr("src")
                    }.getOrNull()?.let { resolveUrl(detailUrl, it, profile) }
                }
            if (profile?.hostHint?.contains("gutenberg") == true) {
                val fallback = gutenbergCoverFallback(entry)
                if (coverUrl.isNullOrBlank() || coverUrl.startsWith("data:image")) {
                    coverUrl = fallback ?: coverUrl
                }
            }

            val author = extractAuthor(entry)
            val summary = BookMetadataCleaner.cleanDescription(entry.select("summary, description, content").text().ifBlank {
                entry.select("content").text()
            })?.takeIf { it.isNotBlank() }
            val downloads = extractDownloadCount(entry.select("content").text().ifBlank { summary })
            val rights = entry.select("rights").text().ifBlank { null }
            val publisher = BookMetadataCleaner.cleanPublisher(entry.select("dc\\:publisher, publisher").text().ifBlank { null })
            val published = BookMetadataCleaner.cleanPublishedDate(entry.select("published, dc\\:date, dcterms\\:date").text().ifBlank { null })
            val subjects = BookMetadataCleaner.cleanValues(entry.select("category[term]").mapNotNull { element ->
                element.attr("term").trim().takeIf { it.isNotBlank() }
            })
            val languages = BookMetadataCleaner.cleanValues(entry.select("dc\\:language, dcterms\\:language, language").eachText()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { BookMetadataCleaner.cleanLanguageTag(it) })

            EntryDetails(
                author = author?.let { BookMetadataCleaner.cleanAuthor(it) },
                coverUrl = coverUrl,
                summary = summary,
                rights = rights,
                publisher = publisher,
                published = published,
                subjects = subjects,
                languages = languages,
                downloads = downloads
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveWikisourceDetails(detailUrl: String): EntryDetails? {
        if (!detailUrl.contains("wikisource.org/wiki/")) return null
        val host = runCatching { URL(detailUrl).host }.getOrNull() ?: return null
        val title = detailUrl.substringAfter("/wiki/").substringBefore('#').substringBefore('?')
        if (title.isBlank()) return null
        val decoded = runCatching { URLDecoder.decode(title, "UTF-8") }.getOrDefault(title)
        val encoded = java.net.URLEncoder.encode(decoded, "UTF-8").replace("+", "%20")
        val summaryUrl = "https://$host/api/rest_v1/page/summary/$encoded"
        return runCatching {
            val response = browseRequest(
                url = summaryUrl,
                profile = currentProfile ?: OpdsProfile("summary", summaryUrl),
                catalog = currentCatalog,
                acceptHeader = "application/json"
            ).execute()
            if (response.statusCode() >= 400) return null
            val body = response.body()
            val json = gson.fromJson(body, JsonObject::class.java) ?: return null
            val thumbnail = json.getAsJsonObject("thumbnail")?.get("source")?.asString
            val extract = json.get("extract")?.asString
            EntryDetails(
                author = null,
                coverUrl = thumbnail,
                summary = extract,
                rights = null,
                publisher = null,
                published = null,
                subjects = emptyList(),
                languages = emptyList(),
                downloads = null
            )
        }.getOrNull()
    }

    private fun inferFormat(downloadUrl: String, type: String, title: String? = null): String =
        inferBrowseFormat(downloadUrl, type, title)

    private fun extractDownloadCount(text: String?): Int? =
        extractBrowseDownloadCount(text)

    private fun resolveUrl(baseUrl: String, relativeUrl: String, profile: OpdsProfile? = currentProfile): String =
        resolveBrowseUrl(baseUrl, relativeUrl, profile)

    private fun selectCoverUrl(links: Collection<Element>, baseUrl: String, profile: OpdsProfile? = currentProfile): String? =
        selectBrowseCoverUrl(links, baseUrl, profile)

    private fun isOpdsDetailLink(link: Element): Boolean =
        isBrowseOpdsDetailLink(link)

    private fun selectDownloadLink(links: Collection<Element>, baseUrl: String, profile: OpdsProfile? = currentProfile): Element? =
        selectBrowseDownloadLink(links, baseUrl, profile)

    private fun selectDownloadOptions(links: Collection<Element>, baseUrl: String, profile: OpdsProfile? = currentProfile): List<BrowseDownloadOption> =
        selectBrowseDownloadOptions(links, baseUrl, profile)

    private fun isExcludedDownloadLink(href: String, type: String, rel: String): Boolean =
        isExcludedBrowseDownloadLink(href, type, rel)

    private fun scoreDownloadOption(
        option: BrowseDownloadOption,
        rel: String,
        type: String,
        href: String,
        profile: OpdsProfile?
    ): Int =
        scoreBrowseDownloadOption(option, rel, type, href, profile)

    private fun gutenbergCoverFallback(entry: Element): String? =
        com.dyu.ereader.data.repository.browse.gutenbergCoverFallback(entry)

    private fun extractGutenbergId(entry: Element): String? =
        com.dyu.ereader.data.repository.browse.extractGutenbergId(entry)

    private fun buildDownloadOption(link: Element?, baseUrl: String, profile: OpdsProfile? = currentProfile): BrowseDownloadOption? =
        buildBrowseDownloadOption(link, baseUrl, profile)

    private suspend fun resolveDownloadInfo(book: BrowseBook): DownloadInfo? {
        if (book.downloadUrl.isNotBlank()) {
            return DownloadInfo(book.downloadUrl, null)
        }
        if (book.downloadOptions.isNotEmpty()) {
            val first = book.downloadOptions.first()
            return DownloadInfo(first.url, first.mimeType)
        }
        val detailUrl = book.detailUrl?.takeIf { it.isNotBlank() } ?: return null

        return try {
            val response = browseRequest(
                url = detailUrl,
                profile = currentProfile ?: OpdsProfile("download", detailUrl),
                catalog = currentCatalog,
                acceptHeader = "application/atom+xml,application/xml;q=0.9,*/*;q=0.8"
            ).execute()

            if (response.statusCode() >= 400) {
                return null
            }

            val doc = Jsoup.parse(response.body(), "", Parser.xmlParser())
            val linkElements = doc.select("entry link").ifEmpty { doc.select("link") }
            val downloadLink = selectDownloadLink(linkElements, detailUrl)
                ?: doc.select("enclosure[type*=epub], enclosure[type*=pdf]").first()

            val resolvedUrl = resolveUrl(detailUrl, downloadLink?.attr("href") ?: "")
            if (resolvedUrl.isBlank()) return null

            DownloadInfo(resolvedUrl, downloadLink?.attr("type"))
        } catch (_: Exception) {
            null
        }
    }

    private fun resolveSearchTemplate(doc: org.jsoup.nodes.Document, baseUrl: String, profile: OpdsProfile? = currentProfile): String? {
        val searchLink = doc.select("link")
            .firstOrNull { link ->
                val rel = link.attr("rel").lowercase()
                rel == "search" || rel.contains("search")
            }
            ?: return null

        val href = resolveUrl(baseUrl, searchLink.attr("href"), profile)
        if (href.contains("{searchTerms}")) {
            return href
        }

        val type = searchLink.attr("type").lowercase()
        if (type.contains("opensearchdescription+xml") || href.endsWith(".xml")) {
            return runCatching {
                val response = browseRequest(
                    url = href,
                    profile = profile ?: OpdsProfile("search", href),
                    catalog = currentCatalog,
                    acceptHeader = "application/opensearchdescription+xml,application/xml;q=0.9,*/*;q=0.8"
                ).execute()

                if (response.statusCode() >= 400) return@runCatching null
                val docXml = Jsoup.parse(response.body(), "", Parser.xmlParser())
                val urlElement = docXml.select("Url[template]")
                    .firstOrNull { element ->
                        val t = element.attr("type").lowercase()
                        t.contains("opds") || t.contains("atom+xml") || t.contains("application/xml")
                    }
                    ?: docXml.selectFirst("Url[template]")

                urlElement?.attr("template")
            }.getOrNull()
        }

        return null
    }

    private fun resolveSearchTemplateFromJson(root: JsonObject, baseUrl: String, profile: OpdsProfile): String? {
        val links = root.getAsJsonArray("links") ?: JsonArray()
        val searchLink = links.firstOrNull { link ->
            val rel = link.asJsonObject.get("rel")?.asString.orEmpty().lowercase()
            rel == "search" || rel.contains("search")
        }?.asJsonObject ?: return null

        val href = resolveUrl(baseUrl, searchLink.get("href")?.asString.orEmpty(), profile)
        if (href.contains("{searchTerms}")) return href
        val template = searchLink.get("template")?.asString
        return template?.takeIf { it.contains("{searchTerms}") }
    }

    private fun parseJsonLinks(links: JsonArray?, baseUrl: String, profile: OpdsProfile): List<BrowseLink> =
        parseBrowseJsonLinks(links, baseUrl, profile)

    private fun parseGroupPublications(groups: JsonArray?, baseUrl: String, profile: OpdsProfile): List<BrowseBook> =
        parseBrowseGroupPublications(groups, baseUrl, profile)

    private fun parseNavigationGroupsFromJson(root: JsonObject, baseUrl: String, profile: OpdsProfile): List<BrowseNavigationGroup> =
        parseBrowseNavigationGroupsFromJson(root, baseUrl, profile)

    private fun parseNavigationLinks(
        links: JsonArray?,
        baseUrl: String,
        profile: OpdsProfile,
        fallbackTitle: String?
    ): List<BrowseNavigationGroup> =
        parseBrowseNavigationLinks(links, baseUrl, profile, fallbackTitle)

    private fun parseNavigationGroupsFromXml(doc: org.jsoup.nodes.Document, baseUrl: String, profile: OpdsProfile): List<BrowseNavigationGroup> =
        parseBrowseNavigationGroupsFromXml(doc, baseUrl, profile)

    private fun parseNavigationGroupsFromXmlEntries(
        entryElements: List<Element>,
        baseUrl: String,
        profile: OpdsProfile
    ): List<BrowseNavigationGroup> =
        parseBrowseNavigationGroupsFromXmlEntries(entryElements, baseUrl, profile)

    private fun isNavigationEntry(
        entry: Element,
        baseUrl: String,
        profile: OpdsProfile,
        parsedBook: BrowseBook
    ): Boolean =
        isBrowseNavigationEntry(entry, baseUrl, profile, parsedBook)

    private fun isNavigationLink(link: Element, baseUrl: String, profile: OpdsProfile): Boolean =
        isBrowseNavigationLink(link, baseUrl, profile)

    private fun looksLikeBookDetailEntry(entry: Element, parsedBook: BrowseBook): Boolean =
        looksLikeBrowseBookDetailEntry(entry, parsedBook)

    private fun isLikelyBookDetailUrl(url: String?): Boolean =
        isLikelyBrowseBookDetailUrl(url)

    private fun inferAuthorFromContent(entry: Element, contentText: String, detailUrl: String?): String? =
        inferBrowseAuthorFromContent(entry, contentText, detailUrl)

    private fun gutenbergNavigationGroups(baseUrl: String): List<BrowseNavigationGroup> =
        gutenbergNavigationGroupsFor(baseUrl)

    private fun standardEbooksNavigationGroups(
        doc: org.jsoup.nodes.Document,
        baseUrl: String,
        profile: OpdsProfile
    ): List<BrowseNavigationGroup> =
        standardEbooksNavigationGroupsFor(doc, baseUrl, profile)

    private fun parsePublication(publication: JsonElement, baseUrl: String, profile: OpdsProfile): BrowseBook? =
        parseBrowsePublication(publication, baseUrl, profile)

    suspend fun loadNextPage() = withContext(Dispatchers.IO) {
        if (_isLoadingNext.value || _isLoading.value) return@withContext
        val feed = _currentFeed.value ?: return@withContext
        val nextLink = feed.links.firstOrNull { it.rel.contains("next", ignoreCase = true) }?.href ?: return@withContext
        val catalog = currentCatalog ?: BrowseCatalog(
            id = "next_page",
            title = feed.title,
            url = nextLink,
            description = "Next page"
        )
        val profile = currentProfile ?: profileFor(catalog)
        _isLoadingNext.value = true
        try {
            val response = browseRequest(nextLink, profile, catalog).execute()

            if (response.statusCode() >= 400) return@withContext
            val body = response.body()
            val isJson = response.contentType()?.contains("json", ignoreCase = true) == true ||
                body.trimStart().startsWith("{")

            val nextCatalog = catalog.copy(url = nextLink)
            val nextFeed = if (isJson) {
                parseOpdsJson(body, nextCatalog, profile)
            } else {
                parseOpdsXml(body, nextCatalog, profile)
            }
            val mergedEntries = (feed.entries + nextFeed.entries).distinctBy { it.id }
            val mergedNav = if (feed.navigationGroups.isNotEmpty()) feed.navigationGroups else nextFeed.navigationGroups
            _currentFeed.value = feed.copy(entries = mergedEntries, links = nextFeed.links, navigationGroups = mergedNav)
        } catch (_: Exception) {
            // Ignore pagination errors for now.
        } finally {
            _isLoadingNext.value = false
        }
    }

    suspend fun resolveDownloadOptions(book: BrowseBook): List<BrowseDownloadOption> = withContext(Dispatchers.IO) {
        val existing = book.downloadOptions
        val isGutenberg = (currentProfile?.hostHint?.contains("gutenberg") == true) ||
            book.detailUrl?.contains("gutenberg.org", ignoreCase = true) == true ||
            book.id.contains("gutenberg.org", ignoreCase = true)
        if (!isGutenberg) {
            return@withContext if (existing.isNotEmpty()) {
                existing
            } else if (book.downloadUrl.isNotBlank()) {
                val inferred = inferFormat(book.downloadUrl, "")
                listOf(
                    BrowseDownloadOption(
                        url = book.downloadUrl,
                        format = book.format.ifBlank { inferred },
                        mimeType = null,
                        sizeBytes = null,
                        label = (book.format.ifBlank { inferred }).uppercase()
                    )
                )
            } else {
                emptyList()
            }
        }

        val id = extractGutenbergIdFromBook(book) ?: return@withContext existing
        val candidates = listOf(
            GutenbergCandidate("EPUB (Images)", "epub", "https://www.gutenberg.org/ebooks/$id.epub.images"),
            GutenbergCandidate("EPUB (Text only)", "epub", "https://www.gutenberg.org/ebooks/$id.epub.noimages"),
            GutenbergCandidate("Kindle", "mobi", "https://www.gutenberg.org/ebooks/$id.kindle.noimages"),
            GutenbergCandidate("Plain Text", "txt", "https://www.gutenberg.org/ebooks/$id.txt.utf-8"),
            GutenbergCandidate("HTML", "html", "https://www.gutenberg.org/ebooks/$id.html.images")
        )

        val resolved = candidates.mapNotNull { candidate ->
            val connection = runCatching {
                val url = URL(candidate.url)
                (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "HEAD"
                    setRequestProperty("User-Agent", BROWSE_USER_AGENT)
                    connectTimeout = 6000
                    readTimeout = 6000
                    instanceFollowRedirects = true
                    applyCatalogAuthHeaders(this, currentCatalog)
                }
            }.getOrNull() ?: return@mapNotNull null
            val code = runCatching { connection.responseCode }.getOrDefault(0)
            if (code !in 200..399) return@mapNotNull null
            val sizeBytes = connection.getHeaderField("Content-Length")?.toLongOrNull()
            BrowseDownloadOption(
                url = candidate.url,
                format = candidate.format,
                mimeType = connection.contentType,
                sizeBytes = sizeBytes,
                label = candidate.label
            )
        }

        val merged = (existing + resolved).distinctBy { it.url }
        if (merged.isNotEmpty()) return@withContext merged

        // Fallback: show common Gutenberg formats even if HEAD fails.
        return@withContext (existing + candidates.map { candidate ->
            BrowseDownloadOption(
                url = candidate.url,
                format = candidate.format,
                mimeType = null,
                sizeBytes = null,
                label = candidate.label
            )
        }).distinctBy { it.url }
    }

    private fun selectCoverUrlFromJson(links: JsonArray, baseUrl: String, profile: OpdsProfile): String? =
        selectBrowseCoverUrlFromJson(links, baseUrl, profile)

    private fun selectDownloadOptionsFromJson(links: JsonArray, baseUrl: String, profile: OpdsProfile): List<BrowseDownloadOption> =
        selectBrowseDownloadOptionsFromJson(links, baseUrl, profile)

    private fun buildCatalogError(catalog: BrowseCatalog, statusCode: Int, statusMessage: String?): String =
        buildBrowseCatalogError(catalog, statusCode, statusMessage)

    suspend fun searchCatalog(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) {
            _error.value = "Enter a search term"
            return
        }

        val template = currentSearchTemplate
        val catalog = currentCatalog
        if (template.isNullOrBlank() || catalog == null) {
            _error.value = "Search is not available for this catalog"
            return
        }

        val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
        val url = template
            .replace("{searchTerms}", encoded)
            .replace("{searchTerms?}", encoded)

        if (!url.contains(encoded)) {
            _error.value = "Search is not available for this catalog"
            return
        }

        loadCatalog(
            BrowseCatalog(
                id = "${catalog.id}_search",
                title = "${catalog.title} Search",
                url = url,
                description = "Search results",
                icon = catalog.icon
            )
        )
    }

    private val _downloadProgress = MutableStateFlow<Float>(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    /**
     * Download an arbitrary EPUB/PDF URL without requiring an OPDS feed entry.
     */
    suspend fun downloadDirectUrl(
        url: String,
        suggestedTitle: String? = null,
        onProgress: (BrowseTransferProgress) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val title = suggestedTitle?.takeIf { it.isNotBlank() } ?: URL(url).path.substringAfterLast('/').ifBlank {
            "downloaded_book"
        }
        val format = when {
            url.lowercase().contains(".pdf") -> "pdf"
            url.lowercase().contains(".cbz") -> "cbz"
            url.lowercase().contains(".cbr") -> "cbr"
            url.lowercase().contains(".azw3") -> "azw3"
            url.lowercase().contains(".mobi") -> "mobi"
            else -> "epub"
        }
        val pseudoBook = BrowseBook(
            id = url,
            title = title,
            author = "Unknown",
            summary = null,
            coverUrl = null,
            downloadUrl = url,
            format = format,
            downloadOptions = listOf(
                BrowseDownloadOption(
                    url = url,
                    format = format,
                    mimeType = null,
                    sizeBytes = null,
                    label = format.uppercase()
                )
            )
        )
        downloadBookToAppStorage(pseudoBook, onProgress)
    }

    suspend fun downloadBook(
        book: BrowseBook,
        destFile: File,
        onProgress: (BrowseTransferProgress) -> Unit = {}
    ): Result<File> =
        withContext(Dispatchers.IO) {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            cancelDownloadRequested = false
            try {
                val resolved = resolveDownloadInfo(book)
                if (resolved == null || resolved.url.isBlank()) {
                    _isDownloading.value = false
                    return@withContext Result.failure(Exception("Book has no download URL"))
                }

                val url = URL(resolved.url)
            val connection = url.openConnection().apply {
                setRequestProperty("User-Agent", BROWSE_USER_AGENT)
                setRequestProperty("Accept", "application/epub+zip,application/pdf,application/octet-stream,*/*")
                setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                connectTimeout = 30000
                readTimeout = 30000
            }
            if (connection is HttpURLConnection) {
                connection.instanceFollowRedirects = true
                applyCatalogAuthHeaders(connection, currentCatalog)
            }

                val totalSize = connection.contentLength.toLong().takeIf { it > 0L }

                val inputStream = connection.getInputStream()
                var downloadedSize: Long = 0
                var lastEmissionTime = System.currentTimeMillis()
                var lastEmissionBytes = 0L

                destFile.outputStream().use { fileOutput ->
                    inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (cancelDownloadRequested) {
                                throw java.io.InterruptedIOException("Download canceled")
                            }
                            fileOutput.write(buffer, 0, bytesRead)
                            downloadedSize += bytesRead
                            val now = System.currentTimeMillis()
                            val elapsed = (now - lastEmissionTime).coerceAtLeast(1L)
                            val bytesSinceLast = downloadedSize - lastEmissionBytes
                            val speedBytesPerSecond = ((bytesSinceLast * 1000L) / elapsed).takeIf { it > 0L }
                            val progress = totalSize?.let { size ->
                                (downloadedSize.toFloat() / size) * 100f
                            } ?: 0f
                            _downloadProgress.value = progress
                            onProgress(
                                BrowseTransferProgress(
                                    progress = progress,
                                    downloadedBytes = downloadedSize,
                                    totalBytes = totalSize,
                                    speedBytesPerSecond = speedBytesPerSecond
                                )
                            )
                            lastEmissionTime = now
                            lastEmissionBytes = downloadedSize
                        }
                    }
                }

                _isDownloading.value = false
                _downloadProgress.value = 100f
                onProgress(
                    BrowseTransferProgress(
                        progress = 100f,
                        downloadedBytes = downloadedSize,
                        totalBytes = totalSize,
                        speedBytesPerSecond = null
                    )
                )
                Result.success(destFile)
            } catch (e: Exception) {
                _isDownloading.value = false
                _downloadProgress.value = 0f
                destFile.delete()
                Result.failure(e)
            } finally {
                cancelDownloadRequested = false
            }
        }

    suspend fun downloadBookToAppStorage(
        book: BrowseBook,
        onProgress: (BrowseTransferProgress) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val resolved = resolveDownloadInfo(book)
            ?: return@withContext Result.failure(Exception("Book has no download URL"))

        val downloadUrl = resolved.url
        val resolvedFormat = inferFormat(downloadUrl, resolved.type.orEmpty())
        val importsDir = File(context.filesDir, "imports")
        if (!importsDir.exists()) {
            importsDir.mkdirs()
        }

        val extension = when {
            downloadUrl.lowercase().contains(".pdf") || resolvedFormat.contains("pdf", ignoreCase = true) -> "pdf"
            downloadUrl.lowercase().contains(".cbz") || resolvedFormat.contains("cbz", ignoreCase = true) -> "cbz"
            downloadUrl.lowercase().contains(".cbr") || resolvedFormat.contains("cbr", ignoreCase = true) -> "cbr"
            downloadUrl.lowercase().contains(".azw3") || resolvedFormat.contains("azw3", ignoreCase = true) -> "azw3"
            downloadUrl.lowercase().contains(".mobi") || resolvedFormat.contains("mobi", ignoreCase = true) -> "mobi"
            else -> "epub"
        }
        val sanitizedTitle = sanitizeFileName(book.title).ifBlank { "downloaded_book" }
        val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.$extension"
        val destFile = File(importsDir, fileName)

        val effectiveBook = if (downloadUrl == book.downloadUrl) {
            book
        } else {
            book.copy(downloadUrl = downloadUrl, format = resolvedFormat)
        }

        downloadBook(effectiveBook, destFile, onProgress)
    }

    private fun sanitizeFileName(input: String): String {
        return input
            .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .trim('_')
    }

    fun cancelDownload() {
        cancelDownloadRequested = true
        _isDownloading.value = false
        _downloadProgress.value = 0f
    }

    fun addCatalog(catalog: BrowseCatalog) {
        if (isUnsupportedAnnaArchiveCatalog(catalog.url)) {
            _error.value = unsupportedAnnaArchiveOpdsMessage()
            return
        }
        if (_catalogs.value.any { it.url == catalog.url || it.id == catalog.id }) return
        _catalogs.value = _catalogs.value + catalog
        persistCustomCatalogs()
    }

    fun removeCatalog(catalogId: String) {
        if (defaultCatalogIds.contains(catalogId)) return
        _catalogs.value = _catalogs.value.filter { it.id != catalogId }
        persistCustomCatalogs()
    }

    private fun loadCustomCatalogs(): List<BrowseCatalog> {
        return try {
            val json = prefs.getString(KEY_CUSTOM_SOURCES, null) ?: return emptyList()
            val type = object : TypeToken<List<BrowseCatalog>>() {}.type
            gson.fromJson<List<BrowseCatalog>>(json, type)
                .orEmpty()
                .filterNot { isUnsupportedAnnaArchiveCatalog(it.url) }
                .map { it.copy(isCustom = true) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun persistCustomCatalogs() {
        val custom = _catalogs.value.filterNot { defaultCatalogIds.contains(it.id) }
        val json = gson.toJson(custom)
        prefs.edit().putString(KEY_CUSTOM_SOURCES, json).apply()
    }

    companion object {
        private const val KEY_CUSTOM_SOURCES = "custom_sources"
    }
}

private data class GutenbergCandidate(
    val label: String,
    val format: String,
    val url: String
)

private fun extractGutenbergIdFromBook(book: BrowseBook): String? {
    val candidates = listOfNotNull(
        book.id,
        book.detailUrl,
        book.downloadUrl
    )
    val regexes = listOf(
        Regex("gutenberg\\.org/ebooks/(\\d+)", RegexOption.IGNORE_CASE),
        Regex("gutenberg\\.org/etext/(\\d+)", RegexOption.IGNORE_CASE),
        Regex("pg(\\d+)", RegexOption.IGNORE_CASE),
        Regex("ebooks/(\\d+)\\.", RegexOption.IGNORE_CASE)
    )
    candidates.forEach { value ->
        regexes.forEach { regex ->
            val match = regex.find(value)
            if (match != null) return match.groupValues[1]
        }
    }
    return null
}
