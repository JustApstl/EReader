package com.dyu.ereader.data.format.epub

import android.content.Context
import android.net.Uri
import android.util.Log
import com.dyu.ereader.data.metadata.BookMetadataCleaner
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType
import org.jsoup.nodes.Document
import org.jsoup.Jsoup
import java.util.zip.ZipInputStream

object EpubMetadataReader {
    private const val TAG = "EpubMetadataReader"

    private fun firstNonBlank(vararg values: String?): String? {
        return values.firstOrNull { !it.isNullOrBlank() }?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun normalizeIsbn(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null

        val fromUrn = Regex("(?i)urn:isbn:([0-9Xx\\-]+)").find(trimmed)?.groupValues?.getOrNull(1)
        val fromPrefix = Regex("(?i)isbn(?:-13|-10)?\\s*[:\\s]?([0-9Xx\\-]{10,17})").find(trimmed)?.groupValues?.getOrNull(1)
        val candidate = (fromUrn ?: fromPrefix ?: trimmed).filter { it.isDigit() || it == 'X' || it == 'x' }
        val normalized = candidate.uppercase()
        return if (normalized.length == 10 || normalized.length == 13) normalized else null
    }

    private fun extractIsbn(opfDoc: Document): String? {
        val refinedMeta = opfDoc.select("meta[property=identifier-type]").firstOrNull { meta ->
            val text = meta.text().trim()
            text.contains("isbn", ignoreCase = true) || text == "15"
        }
        val refinedId = refinedMeta?.attr("refines")?.removePrefix("#")?.trim().orEmpty()
        val refinedValue = if (refinedId.isNotEmpty()) {
            opfDoc.select("dc|identifier[id=$refinedId], identifier[id=$refinedId]").text()
        } else {
            ""
        }

        val metaIsbn = firstNonBlank(
            opfDoc.select("meta[name=isbn]").attr("content"),
            opfDoc.select("meta[name=calibre:isbn]").attr("content")
        )

        val identifiers = opfDoc.select("dc|identifier, identifier")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }

        val candidates = buildList {
            if (refinedValue.isNotBlank()) add(refinedValue)
            if (!metaIsbn.isNullOrBlank()) add(metaIsbn)
            addAll(identifiers)
        }

        return candidates.firstNotNullOfOrNull { normalizeIsbn(it) }
    }

    private fun splitMetadataList(raw: String): List<String> {
        return raw.split(';', ',', '/', '|')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun extractCountries(opfDoc: Document): List<String> {
        val coverageValues = buildList {
            addAll(opfDoc.select("dc|coverage, coverage").map { it.text() })
            addAll(opfDoc.select("meta[property=dcterms:spatial]").map { it.text() })
            addAll(opfDoc.select("meta[name=calibre:country]").map { it.attr("content") })
            addAll(opfDoc.select("meta[name=calibre:region]").map { it.attr("content") })
            addAll(opfDoc.select("meta[name=country]").map { it.attr("content") })
        }

        return coverageValues
            .flatMap { splitMetadataList(it) }
            .distinct()
            .sorted()
    }

    private fun collectMetaValues(opfDoc: Document, selector: String): List<String> {
        return opfDoc.select(selector)
            .mapNotNull { element ->
                val content = element.attr("content").trim()
                val text = element.text().trim()
                when {
                    content.isNotEmpty() -> content
                    text.isNotEmpty() -> text
                    else -> null
                }
            }
    }

    private fun extractPageCount(opfDoc: Document): Int? {
        val values = buildList {
            addAll(collectMetaValues(opfDoc, "meta[name=calibre:page_count]"))
            addAll(collectMetaValues(opfDoc, "meta[name=page_count], meta[name=page-count], meta[name=pagecount], meta[name=pageCount]"))
            addAll(collectMetaValues(opfDoc, "meta[property=page-count], meta[property=pageCount]"))
            addAll(collectMetaValues(opfDoc, "meta[property=schema:numberOfPages], meta[property=numberOfPages]"))
            addAll(opfDoc.select("dc|extent, extent").map { it.text().trim() })
        }

        return values.firstNotNullOfOrNull { raw ->
            val match = Regex("(\\d{1,6})").find(raw)?.groupValues?.getOrNull(1)
            match?.toIntOrNull()?.takeIf { it > 0 }
        }
    }

    fun readPageCount(context: Context, uri: Uri): Int? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipInputStream = ZipInputStream(inputStream)
                var entry = zipInputStream.nextEntry

                val importantFiles = mutableMapOf<String, ByteArray>()
                while (entry != null) {
                    if (entry.name.endsWith(".opf") || entry.name == "META-INF/container.xml") {
                        importantFiles[entry.name] = zipInputStream.readBytes()
                    }
                    entry = zipInputStream.nextEntry
                }

                val containerXml = importantFiles["META-INF/container.xml"] ?: return null
                val containerDoc = Jsoup.parse(containerXml.decodeToString())
                val opfPath = containerDoc.select("rootfile").attr("full-path")

                val opfBytes = importantFiles[opfPath] ?: run {
                    var foundOpf: ByteArray? = null
                    context.contentResolver.openInputStream(uri)?.use { i2 ->
                        val z2 = ZipInputStream(i2)
                        var e2 = z2.nextEntry
                        while (e2 != null) {
                            if (e2.name == opfPath) {
                                foundOpf = z2.readBytes()
                                break
                            }
                            e2 = z2.nextEntry
                        }
                    }
                    foundOpf
                } ?: return null

                extractPageCount(Jsoup.parse(opfBytes.decodeToString()))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EPUB page count", e)
            null
        }
    }

    fun readMetadata(context: Context, uri: Uri, id: String, fileName: String, fileSize: Long): BookItem? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val zipInputStream = ZipInputStream(inputStream)
                var entry = zipInputStream.nextEntry

                val importantFiles = mutableMapOf<String, ByteArray>()
                
                // First pass: collect container.xml and potential OPF files
                // Metadata files are small, so caching them in memory is efficient.
                while (entry != null) {
                    if (entry.name.endsWith(".opf") || entry.name == "META-INF/container.xml") {
                        importantFiles[entry.name] = zipInputStream.readBytes()
                    }
                    entry = zipInputStream.nextEntry
                }
                
                val containerXml = importantFiles["META-INF/container.xml"] ?: return null
                val containerDoc = Jsoup.parse(containerXml.decodeToString())
                val opfPath = containerDoc.select("rootfile").attr("full-path")
                
                val opfBytes = importantFiles[opfPath] ?: run {
                    // Fallback: If OPF wasn't found by extension, search by path in a second pass
                    var foundOpf: ByteArray? = null
                    context.contentResolver.openInputStream(uri)?.use { i2 ->
                        val z2 = ZipInputStream(i2)
                        var e2 = z2.nextEntry
                        while (e2 != null) {
                            if (e2.name == opfPath) {
                                foundOpf = z2.readBytes()
                                break
                            }
                            e2 = z2.nextEntry
                        }
                    }
                    foundOpf
                } ?: return null

                val opfDoc = Jsoup.parse(opfBytes.decodeToString())
                val version = opfDoc.select("package").attr("version").trim()
                val epubType = when {
                    version.startsWith("3") -> BookType.EPUB3
                    version.startsWith("2") -> BookType.EPUB
                    version.startsWith("1") -> BookType.EPUB
                    else -> BookType.EPUB
                }
                
                // Use immutable variables and safe parsing
                val title = BookMetadataCleaner.cleanTitle(
                    opfDoc.select("dc|title").text()
                    .ifEmpty { opfDoc.select("title").text() }
                    .ifEmpty { fileName.removeSuffix(".epub") },
                    fileName
                )
                
                val author = BookMetadataCleaner.cleanAuthor(
                    opfDoc.select("dc|creator").text()
                    .ifEmpty { opfDoc.select("creator").text() }
                    .ifEmpty { "Unknown Author" },
                    fileName
                )
                
                val descriptionElement = opfDoc.select("dc|description").first()
                    ?: opfDoc.select("description").first()
                val description = BookMetadataCleaner.cleanDescription(firstNonBlank(
                    descriptionElement?.html(),
                    descriptionElement?.text(),
                    opfDoc.select("meta[property=dcterms:description]").text(),
                    opfDoc.select("meta[name=description]").attr("content"),
                    opfDoc.select("meta[name=calibre:description]").attr("content")
                ))

                val publisher = BookMetadataCleaner.cleanPublisher(firstNonBlank(
                    opfDoc.select("dc|publisher").text(),
                    opfDoc.select("publisher").text(),
                    opfDoc.select("meta[property=dcterms:publisher]").text(),
                    opfDoc.select("meta[name=publisher]").attr("content"),
                    opfDoc.select("meta[name=calibre:publisher]").attr("content")
                ))

                val publishedDate = BookMetadataCleaner.cleanPublishedDate(firstNonBlank(
                    opfDoc.select("dc|date").text(),
                    opfDoc.select("date").text(),
                    opfDoc.select("meta[property=dcterms:date]").text(),
                    opfDoc.select("meta[name=date]").attr("content")
                ))

                val language = BookMetadataCleaner.cleanLanguageTag(firstNonBlank(
                    opfDoc.select("dc|language").text(),
                    opfDoc.select("language").text(),
                    opfDoc.select("meta[property=dcterms:language]").text(),
                    opfDoc.select("meta[name=language]").attr("content"),
                    opfDoc.select("meta[name=calibre:language]").attr("content")
                ))

                val genres = BookMetadataCleaner.cleanValues(
                    opfDoc.select("dc|subject").map { it.text().trim() }
                )
                val isbn = extractIsbn(opfDoc)
                val countries = BookMetadataCleaner.cleanValues(extractCountries(opfDoc))

                // Resolve cover image path
                val coverId = opfDoc.select("meta[name=cover]").attr("content")
                val coverPath = if (coverId.isNotEmpty()) {
                    opfDoc.select("item[id=$coverId]").attr("href")
                } else {
                    opfDoc.select("item[properties*=cover-image]").attr("href")
                        .ifEmpty { opfDoc.select("item[id*=cover]").attr("href") }
                }
                
                var coverImage: ByteArray? = null
                if (coverPath.isNotEmpty()) {
                    val baseDir = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") else ""
                    val fullCoverPath = if (baseDir.isNotEmpty()) "$baseDir/$coverPath" else coverPath
                    val normalizedPath = fullCoverPath.replace("//", "/").replace("./", "")

                    // Final pass to extract the cover image
                    context.contentResolver.openInputStream(uri)?.use { i3 ->
                        val z3 = ZipInputStream(i3)
                        var e3 = z3.nextEntry
                        while (e3 != null) {
                            if (e3.name == normalizedPath || e3.name.endsWith("/" + coverPath.substringAfterLast("/"))) {
                                coverImage = z3.readBytes()
                                break
                            }
                            e3 = z3.nextEntry
                        }
                    }
                }

                BookItem(
                    id = id,
                    uri = uri.toString(),
                    fileName = fileName,
                    title = title,
                    author = author,
                    coverImage = coverImage,
                    description = description,
                    publisher = publisher,
                    publishedDate = publishedDate,
                    isbn = isbn,
                    language = language,
                    genres = genres,
                    fileSize = fileSize,
                    year = publishedDate?.take(4),
                    type = epubType,
                    countries = countries,
                    dateAdded = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EPUB metadata for $fileName", e)
            null
        }
    }
}
