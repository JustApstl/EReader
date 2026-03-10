package com.dyu.ereader.data.repository

import android.content.Context
import com.dyu.ereader.data.model.BrowseBook
import com.dyu.ereader.data.model.BrowseCatalog
import com.dyu.ereader.data.model.BrowseFeed
import com.dyu.ereader.data.model.BrowseLink
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.net.URL
import java.io.File

class BrowseRepository(
    private val context: Context
) {
    private val gson = Gson()
    private val defaultCatalogs = listOf(
            BrowseCatalog(
                id = "standard_ebooks",
                title = "Standard Ebooks",
                url = "https://standardebooks.org/feeds/opds/all",
                description = "High quality, carefully formatted, completely free ebooks",
                icon = "https://standardebooks.org/favicon.ico"
            ),
            BrowseCatalog(
                id = "project_gutenberg",
                title = "Project Gutenberg",
                url = "https://m.gutenberg.org/ebooks.opds/",
                description = "Over 70,000 free ebooks online",
                icon = "https://www.gutenberg.org/favicon.ico"
            ),
            BrowseCatalog(
                id = "feedbooks",
                title = "Feedbooks",
                url = "https://catalog.feedbooks.com/publicdomain.atom",
                description = "Free public domain books",
                icon = "https://www.feedbooks.com/favicon.ico"
            )
        )
    private val defaultCatalogIds = defaultCatalogs.map { it.id }.toSet()
    private val prefs by lazy { context.getSharedPreferences("browse_sources", Context.MODE_PRIVATE) }

    private val _catalogs = MutableStateFlow(defaultCatalogs + loadCustomCatalogs())
    val catalogs: StateFlow<List<BrowseCatalog>> = _catalogs.asStateFlow()

    private val _currentFeed = MutableStateFlow<BrowseFeed?>(null)
    val currentFeed: StateFlow<BrowseFeed?> = _currentFeed.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Descriptive User-Agent as requested by Standard Ebooks
    private val USER_AGENT = "EReader/2.1.0 (Android E-Book Reader; +https://github.com/dyu/ereader)"

    suspend fun loadCatalog(catalog: BrowseCatalog) = withContext(Dispatchers.IO) {
        _isLoading.value = true
        _error.value = null
        try {
            val response = Jsoup.connect(catalog.url)
                .userAgent(USER_AGENT)
                .header("Accept", "application/atom+xml,application/xml;q=0.9,*/*;q=0.8")
                .ignoreContentType(true)
                .followRedirects(true)
                .timeout(15000)
                .execute()
            
            val doc = response.parse()
            
            val entryElements = doc.select("entry")
            val entries = entryElements.map { entry ->
                parseEntry(entry, catalog.url)
            }

            val linkElements = doc.select("link")
            val links = linkElements.map { link ->
                BrowseLink(
                    rel = link.attr("rel"),
                    href = resolveUrl(catalog.url, link.attr("href")),
                    type = link.attr("type")
                )
            }

            _currentFeed.value = BrowseFeed(
                id = doc.select("id").first()?.text() ?: catalog.id,
                title = doc.select("title").first()?.text() ?: catalog.title,
                entries = entries,
                links = links
            )
        } catch (e: Exception) {
            _error.value = "Failed to load catalog: ${e.localizedMessage ?: e.javaClass.simpleName}"
            e.printStackTrace()
        } finally {
            _isLoading.value = false
        }
    }

    fun clearFeed() {
        _currentFeed.value = null
        _error.value = null
    }

    private fun parseEntry(entry: Element, baseUrl: String): BrowseBook {
        val id = entry.select("id").text().ifBlank {
            entry.select("guid").text().ifBlank {
                entry.select("link").first()?.attr("href") ?: "unknown"
            }
        }
        val title = entry.select("title").text().ifBlank { "Untitled" }
        val author = entry.select("author name").text().ifBlank {
            entry.select("author").text().ifBlank {
                entry.select("dc\\:creator").text().ifBlank {
                    "Unknown Author"
                }
            }
        }
        val summary = entry.select("summary, description, content").text().ifBlank {
            entry.select("content").text()
        }

        val links = entry.select("link")

        val coverUrl = links.firstOrNull {
            val rel = it.attr("rel").lowercase()
            val type = it.attr("type").lowercase()
            rel.contains("image") || rel.contains("thumbnail") || type.contains("image")
        }?.attr("href")?.let { resolveUrl(baseUrl, it) }
            ?: entry.select("media\\:thumbnail").first()?.attr("url")?.let { resolveUrl(baseUrl, it) }

        val downloadLink = links
            .mapNotNull { link ->
                val rel = link.attr("rel").lowercase()
                val type = link.attr("type").lowercase()
                val href = resolveUrl(baseUrl, link.attr("href"))
                if (href.isBlank()) return@mapNotNull null

                val score = when {
                    type.contains("application/epub+zip") -> 100
                    href.lowercase().contains(".epub") -> 95
                    type.contains("application/pdf") -> 80
                    href.lowercase().contains(".pdf") -> 75
                    rel.contains("acquisition") -> 60
                    rel.contains("alternate") -> 30
                    else -> 0
                }

                Triple(link, href, score)
            }
            .filter { (_, href, score) ->
                score > 0 && (href.startsWith("http://") || href.startsWith("https://"))
            }
            .maxByOrNull { (_, _, score) -> score }
            ?.first
            ?: entry.select("enclosure[type*=epub], enclosure[type*=pdf]").first()

        val downloadUrl = resolveUrl(baseUrl, downloadLink?.attr("href") ?: "")
        val downloadType = downloadLink?.attr("type").orEmpty()

        return BrowseBook(
            id = id,
            title = title,
            author = author,
            summary = summary.take(200) + if (summary.length > 200) "..." else "",
            coverUrl = coverUrl,
            downloadUrl = downloadUrl,
            format = inferFormat(downloadUrl, downloadType)
        )
    }

    private fun inferFormat(downloadUrl: String, type: String): String {
        val mime = type.lowercase()
        val href = downloadUrl.lowercase()
        return when {
            mime.contains("epub") || href.contains(".epub") -> "epub"
            mime.contains("pdf") || href.contains(".pdf") -> "pdf"
            else -> "epub"
        }
    }

    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        if (relativeUrl.isBlank()) return ""
        return try {
            URL(URL(baseUrl), relativeUrl).toString()
        } catch (e: Exception) {
            relativeUrl
        }
    }

    suspend fun searchCatalog(query: String) {
        _error.value = "Search not implemented yet for this catalog"
    }

    private val _downloadProgress = MutableStateFlow<Float>(0f)
    val downloadProgress: StateFlow<Float> = _downloadProgress.asStateFlow()

    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    suspend fun downloadBook(book: BrowseBook, destFile: File, onProgress: (Float) -> Unit = {}): Result<File> = 
        withContext(Dispatchers.IO) {
            _isDownloading.value = true
            _downloadProgress.value = 0f
            try {
                if (book.downloadUrl.isBlank()) {
                    _isDownloading.value = false
                    return@withContext Result.failure(Exception("Book has no download URL"))
                }

                val url = URL(book.downloadUrl)
                val connection = url.openConnection().apply {
                    setRequestProperty("User-Agent", USER_AGENT)
                    setRequestProperty("Accept", "application/epub+zip,application/pdf,application/octet-stream,*/*")
                    connectTimeout = 30000
                    readTimeout = 30000
                }

                val totalSize = connection.contentLength.toLong()

                val inputStream = connection.getInputStream()
                var downloadedSize: Long = 0

                destFile.outputStream().use { fileOutput ->
                    inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            fileOutput.write(buffer, 0, bytesRead)
                            downloadedSize += bytesRead
                            if (totalSize > 0L) {
                                val progress = (downloadedSize.toFloat() / totalSize) * 100f
                                _downloadProgress.value = progress
                                onProgress(progress)
                            } else {
                                _downloadProgress.value = 0f
                                onProgress(0f)
                            }
                        }
                    }
                }

                _isDownloading.value = false
                _downloadProgress.value = 100f
                Result.success(destFile)
            } catch (e: Exception) {
                _isDownloading.value = false
                _downloadProgress.value = 0f
                destFile.delete()
                Result.failure(e)
            }
        }

    suspend fun downloadBookToAppStorage(
        book: BrowseBook,
        onProgress: (Float) -> Unit = {}
    ): Result<File> = withContext(Dispatchers.IO) {
        val importsDir = File(context.filesDir, "imports")
        if (!importsDir.exists()) {
            importsDir.mkdirs()
        }

        val extension = when {
            book.downloadUrl.lowercase().contains(".pdf") || book.format.contains("pdf", ignoreCase = true) -> "pdf"
            else -> "epub"
        }
        val sanitizedTitle = sanitizeFileName(book.title).ifBlank { "downloaded_book" }
        val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.$extension"
        val destFile = File(importsDir, fileName)

        downloadBook(book, destFile, onProgress)
    }

    private fun sanitizeFileName(input: String): String {
        return input
            .replace("[^a-zA-Z0-9._-]".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .trim('_')
    }

    fun cancelDownload() {
        _isDownloading.value = false
        _downloadProgress.value = 0f
    }

    fun addCatalog(catalog: BrowseCatalog) {
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
            gson.fromJson<List<BrowseCatalog>>(json, type).orEmpty()
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
