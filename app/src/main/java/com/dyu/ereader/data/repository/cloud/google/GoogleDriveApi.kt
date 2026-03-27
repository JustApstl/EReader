package com.dyu.ereader.data.repository.cloud.google

import android.accounts.Account
import android.content.Context
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException
import java.time.Instant

internal class GoogleDriveApi(
    private val context: Context
) {
    private val client = OkHttpClient()

    suspend fun uploadBackup(
        accountEmail: String,
        backupFile: File,
        existingFileId: String?
    ): GoogleDriveBackupMetadata = withContext(Dispatchers.IO) {
        val metadataJson = JsonObject().apply {
            addProperty("name", BACKUP_FILE_NAME)
            if (existingFileId.isNullOrBlank()) {
                add("parents", JsonParser.parseString("[\"appDataFolder\"]").asJsonArray)
            }
        }.toString()

        val body = MultipartBody.Builder()
            .setType(MULTIPART_RELATED_MEDIA_TYPE)
            .addPart(
                headers = okhttp3.Headers.headersOf("Content-Type", JSON_MEDIA_TYPE.toString()),
                body = metadataJson.toRequestBody(JSON_MEDIA_TYPE)
            )
            .addPart(
                headers = okhttp3.Headers.headersOf("Content-Type", ZIP_MEDIA_TYPE.toString()),
                body = backupFile.asRequestBody(ZIP_MEDIA_TYPE)
            )
            .build()

        val request = Request.Builder()
            .url(
                if (existingFileId.isNullOrBlank()) {
                    "$UPLOAD_BASE_URL/files?uploadType=multipart&fields=id,name,size,modifiedTime"
                } else {
                    "$UPLOAD_BASE_URL/files/$existingFileId?uploadType=multipart&fields=id,name,size,modifiedTime"
                }
            )
            .let { builder ->
                if (existingFileId.isNullOrBlank()) {
                    builder.post(body)
                } else {
                    builder.patch(body)
                }
            }
            .build()

        executeAuthorized(accountEmail, request).use { response ->
            val root = parseResponse(response)
            root.toBackupMetadata()
        }
    }

    suspend fun getLatestBackup(accountEmail: String): GoogleDriveBackupMetadata? = withContext(Dispatchers.IO) {
        val url = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("drive/v3/files")
            .addQueryParameter("spaces", "appDataFolder")
            .addQueryParameter("pageSize", "1")
            .addQueryParameter("orderBy", "modifiedTime desc")
            .addQueryParameter("fields", "files(id,name,size,modifiedTime)")
            .addQueryParameter("q", "name = '$BACKUP_FILE_NAME' and trashed = false")
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        executeAuthorized(accountEmail, request).use { response ->
            val root = parseResponse(response)
            val files = root.getAsJsonArray("files") ?: return@use null
            val first = files.firstOrNull()?.asJsonObject ?: return@use null
            first.toBackupMetadata()
        }
    }

    suspend fun downloadBackup(accountEmail: String, fileId: String, targetFile: File) = withContext(Dispatchers.IO) {
        val url = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("drive/v3/files/$fileId")
            .addQueryParameter("alt", "media")
            .build()
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        executeAuthorized(accountEmail, request).use { response ->
            val body = response.body
            targetFile.outputStream().use { output ->
                body.byteStream().use { input ->
                    input.copyTo(output)
                }
            }
        }
    }

    suspend fun getStorageQuota(accountEmail: String): GoogleDriveStorageQuota = withContext(Dispatchers.IO) {
        val url = okhttp3.HttpUrl.Builder()
            .scheme("https")
            .host("www.googleapis.com")
            .addPathSegments("drive/v3/about")
            .addQueryParameter("fields", "storageQuota(limit,usage)")
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        executeAuthorized(accountEmail, request).use { response ->
            val root = parseResponse(response)
            val quota = root.getAsJsonObject("storageQuota")
            GoogleDriveStorageQuota(
                totalBytes = quota?.get("limit")?.asLongOrNull() ?: 0L,
                usedBytes = quota?.get("usage")?.asLongOrNull() ?: 0L
            )
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun executeAuthorized(accountEmail: String, request: Request): okhttp3.Response {
        val token = getAccessToken(accountEmail)
        val authenticated = request.newBuilder()
            .header("Authorization", "Bearer $token")
            .build()
        val response = client.newCall(authenticated).execute()
        if (response.code != HTTP_UNAUTHORIZED) {
            return response
        }

        response.close()
        GoogleAuthUtil.invalidateToken(context, token)
        val refreshedToken = getAccessToken(accountEmail)
        val retried = request.newBuilder()
            .header("Authorization", "Bearer $refreshedToken")
            .build()
        return client.newCall(retried).execute()
    }

    private suspend fun getAccessToken(accountEmail: String): String = withContext(Dispatchers.IO) {
        val account = Account(accountEmail, GOOGLE_ACCOUNT_TYPE)
        try {
            GoogleAuthUtil.getToken(context, account, "oauth2:$DRIVE_APPDATA_SCOPE")
        } catch (error: GoogleAuthException) {
            throw IOException("Google Drive authorization failed. Reconnect the account and try again.", error)
        }
    }

    private fun parseResponse(response: okhttp3.Response): JsonObject {
        if (!response.isSuccessful) {
            val message = response.body.string().ifBlank { response.message }
            error("Google Drive request failed (${response.code}): $message")
        }
        val payload = response.body.string()
        if (payload.isBlank()) {
            return JsonObject()
        }
        return JsonParser.parseString(payload).asJsonObject
    }

    private fun JsonObject.toBackupMetadata(): GoogleDriveBackupMetadata {
        return GoogleDriveBackupMetadata(
            id = get("id")?.asString.orEmpty(),
            name = get("name")?.asString ?: BACKUP_FILE_NAME,
            size = get("size")?.asLongOrNull() ?: 0L,
            modifiedTime = get("modifiedTime")?.asString
                ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                ?: 0L
        )
    }

    private fun com.google.gson.JsonElement.asLongOrNull(): Long? {
        return runCatching { asLong }.getOrNull()
    }

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        const val BACKUP_FILE_NAME = "ereader_cloud_backup.zip"

        private const val GOOGLE_ACCOUNT_TYPE = "com.google"
        private const val UPLOAD_BASE_URL = "https://www.googleapis.com/upload/drive/v3"
        private const val HTTP_UNAUTHORIZED = 401

        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        private val ZIP_MEDIA_TYPE = "application/zip".toMediaType()
        private val MULTIPART_RELATED_MEDIA_TYPE = "multipart/related".toMediaType()
    }
}

internal data class GoogleDriveBackupMetadata(
    val id: String,
    val name: String,
    val size: Long,
    val modifiedTime: Long
)

internal data class GoogleDriveStorageQuota(
    val totalBytes: Long,
    val usedBytes: Long
)
