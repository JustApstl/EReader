package com.dyu.ereader.data.repository.mobi

import android.content.Context
import android.net.Uri
import com.dyu.ereader.BuildConfig
import com.dyu.ereader.core.crypto.stableMd5
import com.dyu.ereader.data.format.mobi.MobiNative
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MobiConversionRepository(
    private val context: Context
) {
    private val client = OkHttpClient()

    suspend fun convertToEpub(bookUri: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val key = stableMd5(bookUri)
            val epubFile = File(context.cacheDir, "mobi_$key.epub")
            if (epubFile.exists() && epubFile.length() > 0) {
                return@runCatching epubFile.toUriString()
            }

            val sourceFile = copyToCacheIfNeeded(bookUri, key)
            val workDir = File(context.cacheDir, "mobi_$key")
            workDir.deleteRecursively()
            workDir.mkdirs()
            if (epubFile.exists()) {
                epubFile.delete()
            }

            try {
                MobiNative.extractToEpubDir(sourceFile.absolutePath, workDir.absolutePath)
                zipEpubFolder(workDir, epubFile)
                workDir.deleteRecursively()
                epubFile.toUriString()
            } catch (nativeError: Throwable) {
                val serverUrl = BuildConfig.MOBI_CONVERTER_URL
                if (serverUrl.isBlank()) {
                    workDir.deleteRecursively()
                    throw IllegalStateException(
                        nativeError.message ?: "Native MOBI conversion failed and no server configured."
                    )
                }
                convertViaServer(sourceFile, epubFile, serverUrl).getOrThrow()
                workDir.deleteRecursively()
                epubFile.toUriString()
            }
        }
    }

    private fun copyToCacheIfNeeded(uriString: String, key: String): File {
        val uri = Uri.parse(uriString)
        return if (uri.scheme == null || uri.scheme == "file") {
            File(requireNotNull(uri.path) { "Invalid MOBI file path" })
        } else {
            val dest = File(context.cacheDir, "mobi_source_$key.mobi")
            if (!dest.exists()) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(dest).use { output ->
                        input.copyTo(output)
                    }
                } ?: throw IllegalStateException("Unable to read MOBI file.")
            }
            dest
        }
    }

    private fun zipEpubFolder(sourceDir: File, outputFile: File) {
        val mimetypeFile = File(sourceDir, "mimetype")
        val metaInfDir = File(sourceDir, "META-INF")
        val oebpsDir = File(sourceDir, "OEBPS")

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            if (mimetypeFile.exists()) {
                val bytes = mimetypeFile.readBytes()
                val crcValue = CRC32().apply { update(bytes) }.value
                val entry = ZipEntry("mimetype").apply {
                    method = ZipEntry.STORED
                    size = bytes.size.toLong()
                    compressedSize = bytes.size.toLong()
                    crc = crcValue
                }
                zos.putNextEntry(entry)
                zos.write(bytes)
                zos.closeEntry()
            }
            addDirToZip(zos, metaInfDir, "META-INF")
            addDirToZip(zos, oebpsDir, "OEBPS")
        }
    }

    private fun addDirToZip(zos: ZipOutputStream, dir: File, basePath: String) {
        if (!dir.exists()) return
        dir.listFiles()?.forEach { file ->
            val entryPath = "$basePath/${file.name}"
            if (file.isDirectory) {
                addDirToZip(zos, file, entryPath)
            } else {
                FileInputStream(file).use { input ->
                    zos.putNextEntry(ZipEntry(entryPath))
                    input.copyTo(zos)
                    zos.closeEntry()
                }
            }
        }
    }

    private fun convertViaServer(
        sourceFile: File,
        outputFile: File,
        serverUrl: String
    ): Result<File> = runCatching {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                sourceFile.name,
                sourceFile.asRequestBody("application/x-mobipocket-ebook".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("Server conversion failed: ${response.code}")
            }
            val body = response.body
            val contentType = body.contentType()?.toString().orEmpty()
            if (!contentType.contains("epub") && !contentType.contains("zip") && !contentType.contains("octet-stream")) {
                throw IllegalStateException("Server response is not EPUB: $contentType")
            }
            outputFile.outputStream().use { output ->
                body.byteStream().copyTo(output)
            }
        }
        outputFile
    }

    private fun File.toUriString(): String = Uri.fromFile(this).toString()
}
