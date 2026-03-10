package com.dyu.ereader.util

import android.content.Context
import android.net.Uri
import android.util.Log
import com.dyu.ereader.data.model.BookItem
import com.dyu.ereader.data.model.BookType
import org.jsoup.Jsoup
import java.util.zip.ZipInputStream

object EpubMetadataReader {
    private const val TAG = "EpubMetadataReader"

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
                
                // Use immutable variables and safe parsing
                val title = opfDoc.select("dc|title").text()
                    .ifEmpty { opfDoc.select("title").text() }
                    .ifEmpty { fileName.removeSuffix(".epub") }
                
                val author = opfDoc.select("dc|creator").text()
                    .ifEmpty { opfDoc.select("creator").text() }
                    .ifEmpty { "Unknown Author" }
                
                val description = opfDoc.select("dc|description").text()
                    .ifEmpty { opfDoc.select("description").text() }
                    .takeIf { it.isNotEmpty() }
                
                val publisher = opfDoc.select("dc|publisher").text()
                    .ifEmpty { opfDoc.select("publisher").text() }
                    .takeIf { it.isNotEmpty() }
                
                val publishedDate = opfDoc.select("dc|date").text()
                    .ifEmpty { opfDoc.select("date").text() }
                    .takeIf { it.isNotEmpty() }
                
                val language = opfDoc.select("dc|language").text()
                    .ifEmpty { opfDoc.select("language").text() }
                    .takeIf { it.isNotEmpty() }
                
                val genres = opfDoc.select("dc|subject").map { it.text() }

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
                    language = language,
                    genres = genres,
                    fileSize = fileSize,
                    year = publishedDate?.take(4),
                    type = BookType.EPUB,
                    dateAdded = System.currentTimeMillis()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading EPUB metadata for $fileName", e)
            null
        }
    }
}
