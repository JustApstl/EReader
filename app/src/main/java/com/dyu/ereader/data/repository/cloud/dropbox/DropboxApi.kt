package com.dyu.ereader.data.repository.cloud.dropbox

import android.content.Context
import android.net.Uri
import com.dropbox.core.DbxAppInfo
import com.dropbox.core.DbxAuthFinish
import com.dropbox.core.DbxPKCEWebAuth
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.DbxSessionStore
import com.dropbox.core.DbxWebAuth
import com.dropbox.core.TokenAccessType
import com.dropbox.core.oauth.DbxCredential
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FileMetadata
import com.dropbox.core.v2.files.WriteMode
import com.dropbox.core.v2.users.FullAccount
import com.dropbox.core.v2.users.SpaceAllocation
import com.dyu.ereader.BuildConfig
import com.dyu.ereader.data.model.cloud.CloudLinkedAccount
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class DropboxApi(
    context: Context
) {
    private val appContext = context.applicationContext
    private val requestConfig = DbxRequestConfig("EReader/${BuildConfig.VERSION_NAME}")
    private val sessionPreferences by lazy {
        appContext.getSharedPreferences(SESSION_PREFERENCES_NAME, Context.MODE_PRIVATE)
    }
    private val sessionStore = object : DbxSessionStore {
        override fun get(): String = sessionPreferences.getString(KEY_AUTH_SESSION, null).orEmpty()

        override fun set(value: String) {
            sessionPreferences.edit().putString(KEY_AUTH_SESSION, value).apply()
        }

        override fun clear() {
            sessionPreferences.edit().remove(KEY_AUTH_SESSION).apply()
        }
    }

    fun isConfigured(): Boolean = BuildConfig.DROPBOX_APP_KEY.isNotBlank()

    fun createAuthorizationUrl(): String {
        require(isConfigured()) { missingAppKeyMessage() }
        return webAuth().authorize(
            DbxWebAuth.newRequestBuilder()
                .withRedirectUri(REDIRECT_URI, sessionStore)
                .withTokenAccessType(TokenAccessType.OFFLINE)
                .build()
        )
    }

    suspend fun finishAuthentication(callbackUri: Uri): DropboxAuthenticationResult = withContext(Dispatchers.IO) {
        require(isConfigured()) { missingAppKeyMessage() }

        val finish = webAuth().finishFromRedirect(
            REDIRECT_URI,
            sessionStore,
            callbackUri.toDropboxQueryMap()
        )
        val session = createClientSession(
            accessToken = finish.accessToken,
            refreshToken = finish.refreshToken,
            accessTokenExpiresAt = finish.expiresAt
        )
        val currentAccount = session.client.users().currentAccount
        val spaceUsage = session.client.users().spaceUsage

        DropboxAuthenticationResult(
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            accessTokenExpiresAt = session.accessTokenExpiresAt,
            accountId = currentAccount.accountId,
            displayName = currentAccount.name.displayName,
            email = currentAccount.email,
            photoUrl = currentAccount.profilePhotoUrl,
            usedBytes = spaceUsage.used,
            totalBytes = resolveTotalBytes(spaceUsage.allocation)
        )
    }

    suspend fun uploadBackup(
        account: CloudLinkedAccount,
        backupFile: File
    ): DropboxOperationResult<DropboxBackupMetadata> = withContext(Dispatchers.IO) {
        val session = authorizedSession(account)
        val metadata = backupFile.inputStream().use { input ->
            session.client.files()
                .uploadBuilder(BACKUP_PATH)
                .withMode(WriteMode.OVERWRITE)
                .uploadAndFinish(input, backupFile.length())
        }
        val spaceUsage = session.client.users().spaceUsage
        DropboxOperationResult(
            value = DropboxBackupMetadata(
                path = metadata.pathLower ?: BACKUP_PATH,
                size = metadata.size,
                modifiedTime = metadata.serverModified.time
            ),
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            accessTokenExpiresAt = session.accessTokenExpiresAt,
            usedBytes = spaceUsage.used,
            totalBytes = resolveTotalBytes(spaceUsage.allocation)
        )
    }

    suspend fun getLatestBackup(account: CloudLinkedAccount): DropboxOperationResult<DropboxBackupMetadata?> =
        withContext(Dispatchers.IO) {
            val session = authorizedSession(account)
            val metadata = runCatching {
                session.client.files().getMetadata(BACKUP_PATH) as? FileMetadata
            }.getOrNull()

            DropboxOperationResult(
                value = metadata?.let {
                    DropboxBackupMetadata(
                        path = it.pathLower ?: BACKUP_PATH,
                        size = it.size,
                        modifiedTime = it.serverModified.time
                    )
                },
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                accessTokenExpiresAt = session.accessTokenExpiresAt,
                usedBytes = account.usedBytes,
                totalBytes = account.totalBytes
            )
        }

    suspend fun downloadBackup(
        account: CloudLinkedAccount,
        targetFile: File
    ): DropboxOperationResult<DropboxBackupMetadata> = withContext(Dispatchers.IO) {
        val session = authorizedSession(account)
        val metadata = targetFile.outputStream().use { output ->
            session.client.files()
                .downloadBuilder(BACKUP_PATH)
                .start()
                .use { downloader -> downloader.download(output) }
        }
        val spaceUsage = session.client.users().spaceUsage

        DropboxOperationResult(
            value = DropboxBackupMetadata(
                path = metadata.pathLower ?: BACKUP_PATH,
                size = metadata.size,
                modifiedTime = metadata.serverModified.time
            ),
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            accessTokenExpiresAt = session.accessTokenExpiresAt,
            usedBytes = spaceUsage.used,
            totalBytes = resolveTotalBytes(spaceUsage.allocation)
        )
    }

    suspend fun getStorageQuota(account: CloudLinkedAccount): DropboxOperationResult<DropboxStorageQuota> =
        withContext(Dispatchers.IO) {
            val session = authorizedSession(account)
            val spaceUsage = session.client.users().spaceUsage
            DropboxOperationResult(
                value = DropboxStorageQuota(
                    usedBytes = spaceUsage.used,
                    totalBytes = resolveTotalBytes(spaceUsage.allocation)
                ),
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                accessTokenExpiresAt = session.accessTokenExpiresAt,
                usedBytes = spaceUsage.used,
                totalBytes = resolveTotalBytes(spaceUsage.allocation)
            )
        }

    suspend fun getCurrentAccount(account: CloudLinkedAccount): DropboxOperationResult<FullAccount> =
        withContext(Dispatchers.IO) {
            val session = authorizedSession(account)
            val currentAccount = session.client.users().currentAccount
            val spaceUsage = session.client.users().spaceUsage
            DropboxOperationResult(
                value = currentAccount,
                accessToken = session.accessToken,
                refreshToken = session.refreshToken,
                accessTokenExpiresAt = session.accessTokenExpiresAt,
                usedBytes = spaceUsage.used,
                totalBytes = resolveTotalBytes(spaceUsage.allocation)
            )
        }

    private fun webAuth(): DbxPKCEWebAuth {
        return DbxPKCEWebAuth(requestConfig, DbxAppInfo(BuildConfig.DROPBOX_APP_KEY))
    }

    private suspend fun authorizedSession(account: CloudLinkedAccount): DropboxClientSession =
        createClientSession(
            accessToken = account.accessToken,
            refreshToken = account.refreshToken,
            accessTokenExpiresAt = account.accessTokenExpiresAt
        )

    private suspend fun createClientSession(
        accessToken: String,
        refreshToken: String?,
        accessTokenExpiresAt: Long?
    ): DropboxClientSession = withContext(Dispatchers.IO) {
        require(isConfigured()) { "Dropbox app key is not configured." }

        val credential = if (!refreshToken.isNullOrBlank()) {
            DbxCredential(accessToken, accessTokenExpiresAt, refreshToken, BuildConfig.DROPBOX_APP_KEY)
        } else {
            DbxCredential(accessToken)
        }
        val refreshed = if (!refreshToken.isNullOrBlank() && credential.aboutToExpire()) {
            credential.refresh(requestConfig)
        } else {
            null
        }
        val resolvedCredential = if (refreshed != null) {
            DbxCredential(
                refreshed.accessToken,
                refreshed.expiresAt,
                refreshToken,
                BuildConfig.DROPBOX_APP_KEY
            )
        } else {
            credential
        }

        DropboxClientSession(
            client = DbxClientV2(requestConfig, resolvedCredential),
            accessToken = resolvedCredential.accessToken,
            refreshToken = resolvedCredential.refreshToken,
            accessTokenExpiresAt = resolvedCredential.expiresAt
        )
    }

    private fun resolveTotalBytes(allocation: SpaceAllocation): Long {
        return when {
            allocation.isIndividual -> allocation.individualValue.allocated
            allocation.isTeam -> allocation.teamValue.allocated
            else -> 0L
        }
    }

    private fun Uri.toDropboxQueryMap(): Map<String, Array<String>> {
        val names = queryParameterNames
        return names.associateWith { name ->
            getQueryParameters(name).toTypedArray()
        }
    }

    companion object {
        const val REDIRECT_URI = "ereader://dropbox-auth"

        private const val BACKUP_PATH = "/ereader_cloud_backup.zip"
        private const val KEY_AUTH_SESSION = "dropbox_auth_session"
        private const val SESSION_PREFERENCES_NAME = "dropbox_auth"
    }

    private fun missingAppKeyMessage(): String {
        return "Dropbox app key is not configured. Set DROPBOX_APP_KEY in local.properties, gradle.properties, or your environment."
    }
}

internal data class DropboxAuthenticationResult(
    val accessToken: String,
    val refreshToken: String?,
    val accessTokenExpiresAt: Long?,
    val accountId: String,
    val displayName: String,
    val email: String?,
    val photoUrl: String?,
    val usedBytes: Long,
    val totalBytes: Long
)

internal data class DropboxBackupMetadata(
    val path: String,
    val size: Long,
    val modifiedTime: Long
)

internal data class DropboxStorageQuota(
    val usedBytes: Long,
    val totalBytes: Long
)

internal data class DropboxOperationResult<T>(
    val value: T,
    val accessToken: String,
    val refreshToken: String?,
    val accessTokenExpiresAt: Long?,
    val usedBytes: Long,
    val totalBytes: Long
)

private data class DropboxClientSession(
    val client: DbxClientV2,
    val accessToken: String,
    val refreshToken: String?,
    val accessTokenExpiresAt: Long?
)
