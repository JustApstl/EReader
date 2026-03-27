package com.dyu.ereader.data.repository.cloud.webdav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

internal class WebDavApi(
    private val client: OkHttpClient = OkHttpClient()
) {
    suspend fun connect(
        serverUrl: String,
        username: String,
        password: String
    ): WebDavConnectionResult = withContext(Dispatchers.IO) {
        val normalizedServerUrl = normalizeCollectionUrl(serverUrl)
        ensureCollection(normalizedServerUrl, username, password)
        val quota = getStorageQuotaInternal(normalizedServerUrl, username, password)
        val host = URL(normalizedServerUrl).host.removePrefix("www.")
        WebDavConnectionResult(
            serverUrl = normalizedServerUrl,
            accountId = "$username@$host",
            displayName = "$username @ $host",
            usedBytes = quota.usedBytes,
            totalBytes = quota.totalBytes
        )
    }

    suspend fun uploadBackup(
        serverUrl: String,
        username: String,
        password: String,
        backupFile: File
    ): WebDavOperationResult<WebDavBackupMetadata> = withContext(Dispatchers.IO) {
        val normalizedServerUrl = normalizeCollectionUrl(serverUrl)
        ensureCollection(normalizedServerUrl, username, password)
        val backupUrl = buildBackupFileUrl(normalizedServerUrl)

        val request = Request.Builder()
            .url(backupUrl)
            .header("Authorization", Credentials.basic(username, password))
            .put(backupFile.readBytes().toRequestBody(ZIP_MEDIA_TYPE))
            .build()

        execute(request).use { response ->
            if (!response.isSuccessful) {
                throw IOException("WebDAV upload failed (${response.code}): ${response.message}")
            }
        }

        val metadata = getBackupMetadataInternal(normalizedServerUrl, username, password)
            ?: WebDavBackupMetadata(
                path = backupUrl,
                size = backupFile.length(),
                modifiedTime = System.currentTimeMillis()
            )
        val quota = getStorageQuotaInternal(normalizedServerUrl, username, password)

        WebDavOperationResult(
            value = metadata,
            usedBytes = quota.usedBytes,
            totalBytes = quota.totalBytes
        )
    }

    suspend fun getLatestBackup(
        serverUrl: String,
        username: String,
        password: String
    ): WebDavBackupMetadata? = withContext(Dispatchers.IO) {
        getBackupMetadataInternal(normalizeCollectionUrl(serverUrl), username, password)
    }

    suspend fun downloadBackup(
        serverUrl: String,
        username: String,
        password: String,
        targetFile: File
    ): WebDavOperationResult<WebDavBackupMetadata> = withContext(Dispatchers.IO) {
        val normalizedServerUrl = normalizeCollectionUrl(serverUrl)
        val backupUrl = buildBackupFileUrl(normalizedServerUrl)
        val request = Request.Builder()
            .url(backupUrl)
            .header("Authorization", Credentials.basic(username, password))
            .get()
            .build()

        execute(request).use { response ->
            if (!response.isSuccessful) {
                throw IOException("WebDAV download failed (${response.code}): ${response.message}")
            }
            targetFile.outputStream().use { output ->
                response.body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }
        }

        val metadata = getBackupMetadataInternal(normalizedServerUrl, username, password)
            ?: WebDavBackupMetadata(
                path = backupUrl,
                size = targetFile.length(),
                modifiedTime = System.currentTimeMillis()
            )
        val quota = getStorageQuotaInternal(normalizedServerUrl, username, password)

        WebDavOperationResult(
            value = metadata,
            usedBytes = quota.usedBytes,
            totalBytes = quota.totalBytes
        )
    }

    suspend fun getStorageQuota(
        serverUrl: String,
        username: String,
        password: String
    ): WebDavStorageQuota = withContext(Dispatchers.IO) {
        getStorageQuotaInternal(normalizeCollectionUrl(serverUrl), username, password)
    }

    private fun getBackupMetadataInternal(
        serverUrl: String,
        username: String,
        password: String
    ): WebDavBackupMetadata? {
        val backupUrl = buildBackupFileUrl(serverUrl)
        val headRequest = Request.Builder()
            .url(backupUrl)
            .header("Authorization", Credentials.basic(username, password))
            .head()
            .build()

        execute(headRequest).use { response ->
            return when (response.code) {
                HttpURLConnection.HTTP_NOT_FOUND -> null
                in 200..299 -> WebDavBackupMetadata(
                    path = backupUrl,
                    size = response.header("Content-Length")?.toLongOrNull() ?: 0L,
                    modifiedTime = response.header("Last-Modified")
                        ?.let(::parseHttpDateMillis)
                        ?: System.currentTimeMillis()
                )
                else -> throw IOException("WebDAV metadata lookup failed (${response.code}): ${response.message}")
            }
        }
    }

    private fun getStorageQuotaInternal(
        serverUrl: String,
        username: String,
        password: String
    ): WebDavStorageQuota {
        val request = Request.Builder()
            .url(serverUrl)
            .header("Authorization", Credentials.basic(username, password))
            .header("Depth", "0")
            .method("PROPFIND", QUOTA_PROP_FIND_BODY.toRequestBody(XML_MEDIA_TYPE))
            .build()

        execute(request).use { response ->
            if (!response.isSuccessful && response.code != MULTI_STATUS_CODE) {
                if (response.code == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw IOException("WebDAV folder was not found. Check the server URL.")
                }
                throw IOException("WebDAV quota request failed (${response.code}): ${response.message}")
            }
            val body = response.body.string()
            val usedBytes = body.extractXmlLong("quota-used-bytes") ?: 0L
            val availableBytes = body.extractXmlLong("quota-available-bytes") ?: 0L
            val totalBytes = if (availableBytes > 0L || usedBytes > 0L) {
                usedBytes + availableBytes
            } else {
                0L
            }
            return WebDavStorageQuota(
                usedBytes = usedBytes,
                totalBytes = totalBytes
            )
        }
    }

    private fun ensureCollection(
        serverUrl: String,
        username: String,
        password: String
    ) {
        val checkRequest = Request.Builder()
            .url(serverUrl)
            .header("Authorization", Credentials.basic(username, password))
            .header("Depth", "0")
            .method("PROPFIND", EMPTY_PROP_FIND_BODY.toRequestBody(XML_MEDIA_TYPE))
            .build()

        execute(checkRequest).use { response ->
            when (response.code) {
                HttpURLConnection.HTTP_OK, MULTI_STATUS_CODE -> return
                HttpURLConnection.HTTP_NOT_FOUND -> {
                    response.close()
                    val url = URL(serverUrl)
                    val path = url.path.trimEnd('/')
                    val parentPath = path.substringBeforeLast('/', "")
                    if (parentPath.isBlank()) {
                        createCollection(serverUrl, username, password)
                    } else {
                        val parentUrl = URL(url.protocol, url.host, url.port, "$parentPath/").toString()
                        ensureCollection(parentUrl, username, password)
                        createCollection(serverUrl, username, password)
                    }
                }
                else -> throw IOException("WebDAV folder check failed (${response.code}): ${response.message}")
            }
        }
    }

    private fun createCollection(
        serverUrl: String,
        username: String,
        password: String
    ) {
        val request = Request.Builder()
            .url(serverUrl)
            .header("Authorization", Credentials.basic(username, password))
            .method("MKCOL", EMPTY_BODY)
            .build()

        execute(request).use { response ->
            if (!(response.isSuccessful || response.code == METHOD_NOT_ALLOWED_CODE || response.code == CONFLICT_CODE)) {
                throw IOException("WebDAV folder could not be created (${response.code}): ${response.message}")
            }
        }
    }

    private fun buildBackupFileUrl(serverUrl: String): String {
        return if (serverUrl.endsWith("/")) {
            "$serverUrl$BACKUP_FILE_NAME"
        } else {
            "$serverUrl/$BACKUP_FILE_NAME"
        }
    }

    private fun normalizeCollectionUrl(serverUrl: String): String {
        val trimmed = serverUrl.trim()
        require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            "WebDAV server URL must start with http:// or https://"
        }
        return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
    }

    private fun execute(request: Request) = client.newCall(request).execute()

    private fun String.extractXmlLong(tagName: String): Long? {
        val pattern = Regex("""<[^>]*$tagName[^>]*>\s*([^<]+)\s*</[^>]*$tagName[^>]*>""", RegexOption.IGNORE_CASE)
        return pattern.find(this)?.groupValues?.getOrNull(1)?.trim()?.toLongOrNull()
    }

    private fun parseHttpDateMillis(value: String): Long? {
        return runCatching {
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("GMT")
            }.parse(value)?.time
        }.getOrNull()
    }

    companion object {
        private const val BACKUP_FILE_NAME = "ereader_cloud_backup.zip"
        private const val MULTI_STATUS_CODE = 207
        private const val METHOD_NOT_ALLOWED_CODE = 405
        private const val CONFLICT_CODE = 409

        private val XML_MEDIA_TYPE = "application/xml; charset=utf-8".toMediaType()
        private val ZIP_MEDIA_TYPE = "application/zip".toMediaType()
        private val EMPTY_BODY = ByteArray(0).toRequestBody(null)
        private const val EMPTY_PROP_FIND_BODY = """<?xml version="1.0" encoding="utf-8"?><d:propfind xmlns:d="DAV:"><d:prop><d:resourcetype/></d:prop></d:propfind>"""
        private const val QUOTA_PROP_FIND_BODY = """<?xml version="1.0" encoding="utf-8"?><d:propfind xmlns:d="DAV:"><d:prop><d:quota-used-bytes/><d:quota-available-bytes/></d:prop></d:propfind>"""
    }
}

internal data class WebDavConnectionResult(
    val serverUrl: String,
    val accountId: String,
    val displayName: String,
    val usedBytes: Long,
    val totalBytes: Long
)

internal data class WebDavBackupMetadata(
    val path: String,
    val size: Long,
    val modifiedTime: Long
)

internal data class WebDavStorageQuota(
    val usedBytes: Long,
    val totalBytes: Long
)

internal data class WebDavOperationResult<T>(
    val value: T,
    val usedBytes: Long,
    val totalBytes: Long
)
