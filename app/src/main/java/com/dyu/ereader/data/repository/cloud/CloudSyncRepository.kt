package com.dyu.ereader.data.repository.cloud

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import com.dyu.ereader.core.crypto.stableMd5
import com.dyu.ereader.data.local.db.AnnotationCollectionEntity
import com.dyu.ereader.data.local.db.BookDatabase
import com.dyu.ereader.data.local.db.BookEntity
import com.dyu.ereader.data.local.db.BookmarkEntity
import com.dyu.ereader.data.local.db.HighlightEntity
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.data.local.prefs.AppStartupPreferences
import com.dyu.ereader.data.local.prefs.ReaderPreferencesStore
import com.dyu.ereader.data.model.cloud.BackupSnapshotInfo
import com.dyu.ereader.data.model.cloud.CloudBackupProfile
import com.dyu.ereader.data.model.cloud.CloudBackupScope
import com.dyu.ereader.data.model.cloud.CloudLinkedAccount
import com.dyu.ereader.data.model.cloud.CloudProvider
import com.dyu.ereader.data.model.cloud.CloudStorageSummary
import com.dyu.ereader.data.model.cloud.CloudSyncSettings
import com.dyu.ereader.data.repository.cloud.dropbox.DropboxApi
import com.dyu.ereader.data.repository.analytics.AnalyticsBackupPayload
import com.dyu.ereader.data.repository.analytics.AnalyticsRepository
import com.dyu.ereader.data.repository.cloud.google.GoogleDriveApi
import com.dyu.ereader.data.repository.cloud.webdav.WebDavApi
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class CloudSyncRepository(
    private val context: Context,
    private val database: BookDatabase,
    private val readerPreferencesStore: ReaderPreferencesStore,
    private val analyticsRepository: AnalyticsRepository
) {
    private val gson = Gson()
    private val googleDriveApi = GoogleDriveApi(context)
    private val dropboxApi = DropboxApi(context)
    private val webDavApi = WebDavApi()
    private val sharedPreferences by lazy {
        context.getSharedPreferences("cloud_sync", Context.MODE_PRIVATE)
    }

    private val _syncSettings = MutableStateFlow(CloudSyncSettings())
    val syncSettings: StateFlow<CloudSyncSettings> = _syncSettings.asStateFlow()

    private val _linkedAccounts = MutableStateFlow<List<CloudLinkedAccount>>(emptyList())
    val linkedAccounts: StateFlow<List<CloudLinkedAccount>> = _linkedAccounts.asStateFlow()

    private val _storageSummary = MutableStateFlow(CloudStorageSummary())
    val storageSummary: StateFlow<CloudStorageSummary> = _storageSummary.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _backupSnapshots = MutableStateFlow<List<BackupSnapshotInfo>>(emptyList())
    val backupSnapshots: StateFlow<List<BackupSnapshotInfo>> = _backupSnapshots.asStateFlow()

    init {
        loadSettings()
        refreshBackupSnapshots()
    }

    private fun loadSettings() {
        try {
            val settingsJson = sharedPreferences.getString(KEY_SETTINGS, null)
            val loaded = settingsJson
                ?.let { gson.fromJson(it, CloudSyncSettings::class.java) }
                ?: CloudSyncSettings()
            val migrated = migrateLegacyAccounts(loaded)
            applyLoadedSettings(migrated)
        } catch (e: Exception) {
            Log.e("CloudSync", "Failed to load settings", e)
            applyLoadedSettings(CloudSyncSettings())
        }
    }

    private fun migrateLegacyAccounts(settings: CloudSyncSettings): CloudSyncSettings {
        if (settings.linkedAccounts.isNotEmpty()) {
            val hydrated = settings.linkedAccounts.mapIndexed { index, account ->
                account.hydrateDefaults(index)
            }
            return settings.copy(
                linkedAccounts = hydrated,
                provider = hydrated.firstOrNull()?.provider ?: CloudProvider.NONE,
                isEnabled = hydrated.isNotEmpty(),
                accessToken = null
            )
        }

        val migratedAccounts = mutableListOf<CloudLinkedAccount>()

        if (settings.provider != CloudProvider.NONE && !settings.accessToken.isNullOrBlank()) {
            migratedAccounts += createLinkedAccount(
                provider = settings.provider,
                accessToken = settings.accessToken,
                displayName = defaultAccountName(settings.provider, 1)
            )
        }

        val legacyTokensJson = sharedPreferences.getString(KEY_OAUTH_TOKENS, null)
        if (!legacyTokensJson.isNullOrBlank()) {
            val rawMap: Map<*, *> = gson.fromJson(legacyTokensJson, Map::class.java) ?: emptyMap<Any, Any>()
            rawMap.entries.mapNotNull { (key, value) ->
                val providerName = key as? String ?: return@mapNotNull null
                val token = value as? String ?: return@mapNotNull null
                val provider = runCatching { CloudProvider.valueOf(providerName) }.getOrNull()
                    ?: return@mapNotNull null
                provider to token
            }.forEachIndexed { index, (provider, token) ->
                migratedAccounts += createLinkedAccount(
                    provider = provider,
                    accessToken = token,
                    displayName = defaultAccountName(provider, index + 1)
                )
            }
        }

        val deduplicated = migratedAccounts.distinctBy { account ->
            account.provider to (account.email ?: account.displayName)
        }

        return settings.copy(
            provider = deduplicated.firstOrNull()?.provider ?: CloudProvider.NONE,
            isEnabled = deduplicated.isNotEmpty(),
            accessToken = null,
            linkedAccounts = deduplicated
        )
    }

    private fun applyLoadedSettings(settings: CloudSyncSettings) {
        _syncSettings.value = settings.sanitized()
        _linkedAccounts.value = _syncSettings.value.linkedAccounts
        _storageSummary.value = summarizeStorage(_linkedAccounts.value)
        _lastSyncTime.value = _syncSettings.value.lastSyncTime
    }

    private fun updateSettings(transform: (CloudSyncSettings) -> CloudSyncSettings) {
        val updated = transform(_syncSettings.value).sanitized()
        _syncSettings.value = updated
        _linkedAccounts.value = updated.linkedAccounts
        _storageSummary.value = summarizeStorage(updated.linkedAccounts)
        _lastSyncTime.value = updated.lastSyncTime
        saveSettings()
    }

    private fun saveSettings() {
        try {
            sharedPreferences.edit().apply {
                putString(KEY_SETTINGS, gson.toJson(_syncSettings.value))
                remove(KEY_OAUTH_TOKENS)
                apply()
            }
        } catch (e: Exception) {
            Log.e("CloudSync", "Failed to save settings", e)
        }
    }

    fun setProvider(provider: CloudProvider) {
        updateSettings {
            if (it.linkedAccounts.isEmpty()) it.copy(provider = provider) else it
        }
    }

    fun setAutoSync(enabled: Boolean) {
        updateSettings { it.copy(autoSync = enabled) }
    }

    fun addCloudAccount(
        provider: CloudProvider,
        accessToken: String,
        displayName: String? = null,
        email: String? = null,
        photoUrl: String? = null,
        refreshToken: String? = null,
        accessTokenExpiresAt: Long? = null,
        accountId: String? = null,
        serverUrl: String? = null,
        usedBytes: Long? = null,
        totalBytes: Long? = null
    ): CloudLinkedAccount {
        val currentAccounts = _linkedAccounts.value
        val existing = currentAccounts.firstOrNull { account ->
            account.provider == provider && when {
                !accountId.isNullOrBlank() && account.accountId == accountId -> true
                !email.isNullOrBlank() && account.email.equals(email, ignoreCase = true) -> true
                else -> false
            }
        }

        val resolvedAccount = if (existing != null) {
            existing.copy(
                displayName = displayName?.takeIf { it.isNotBlank() } ?: existing.displayName,
                email = email ?: existing.email,
                photoUrl = photoUrl ?: existing.photoUrl,
                accessToken = accessToken,
                refreshToken = refreshToken ?: existing.refreshToken,
                accessTokenExpiresAt = accessTokenExpiresAt ?: existing.accessTokenExpiresAt,
                accountId = accountId ?: existing.accountId,
                serverUrl = serverUrl ?: existing.serverUrl,
                usedBytes = usedBytes ?: existing.usedBytes,
                totalBytes = totalBytes ?: existing.totalBytes
            )
        } else {
            val providerCount = currentAccounts.count { it.provider == provider } + 1
            createLinkedAccount(
                provider = provider,
                accessToken = accessToken,
                displayName = displayName ?: defaultAccountName(provider, providerCount),
                email = email,
                photoUrl = photoUrl,
                refreshToken = refreshToken,
                accessTokenExpiresAt = accessTokenExpiresAt,
                accountId = accountId,
                serverUrl = serverUrl,
                usedBytes = usedBytes,
                totalBytes = totalBytes
            )
        }

        updateSettings { settings ->
            val updatedAccounts = settings.linkedAccounts
                .filterNot { it.id == resolvedAccount.id }
                .plus(resolvedAccount)
                .sortedWith(compareBy<CloudLinkedAccount> { it.provider.name }.thenBy { it.displayName.lowercase(Locale.ROOT) })
            settings.copy(
                provider = updatedAccounts.firstOrNull()?.provider ?: CloudProvider.NONE,
                linkedAccounts = updatedAccounts
            )
        }

        return resolvedAccount
    }

    fun setOAuthToken(provider: CloudProvider, token: String) {
        addCloudAccount(provider = provider, accessToken = token)
    }

    fun removeCloudAccount(accountId: String) {
        updateSettings { settings ->
            val updatedAccounts = settings.linkedAccounts.filterNot { it.id == accountId }
            settings.copy(
                provider = updatedAccounts.firstOrNull()?.provider ?: CloudProvider.NONE,
                linkedAccounts = updatedAccounts
            )
        }
    }

    fun removeOAuthToken(provider: CloudProvider) {
        updateSettings { settings ->
            val updatedAccounts = settings.linkedAccounts.filterNot { it.provider == provider }
            settings.copy(
                provider = updatedAccounts.firstOrNull()?.provider ?: CloudProvider.NONE,
                linkedAccounts = updatedAccounts
            )
        }
    }

    fun updateAccountBackupScopes(accountId: String, scopes: Set<CloudBackupScope>) {
        updateSettings { settings ->
            settings.copy(
                linkedAccounts = settings.linkedAccounts.map { account ->
                    if (account.id == accountId) {
                        account.copy(enabledBackupScopes = scopes)
                    } else {
                        account
                    }
                }
            )
        }
    }

    fun getCloudAccount(accountId: String): CloudLinkedAccount? {
        return _linkedAccounts.value.firstOrNull { it.id == accountId }
    }

    suspend fun performSync(accountId: String? = null) = withContext(Dispatchers.IO) {
        if (_isSyncing.value) return@withContext

        val targetAccounts = if (accountId == null) {
            _linkedAccounts.value
        } else {
            _linkedAccounts.value.filter { it.id == accountId }
        }
        if (targetAccounts.isEmpty()) return@withContext

        _isSyncing.value = true
        _syncError.value = null

        try {
            targetAccounts.forEach { account ->
                val backupFile = when (account.provider) {
                    CloudProvider.GOOGLE_DRIVE -> createBackupArchive(
                        scopes = account.enabledBackupScopes,
                        fileName = "${account.provider.name.lowercase(Locale.ROOT)}_${account.id}_backup.zip"
                    )
                    else -> createBackupFile(
                        scopes = account.enabledBackupScopes,
                        fileName = "${account.provider.name.lowercase(Locale.ROOT)}_${account.id}_backup.json"
                    )
                }
                when (account.provider) {
                    CloudProvider.GOOGLE_DRIVE -> syncWithGoogleDrive(account, backupFile)
                    CloudProvider.WEB_DAV -> syncWithWebDav(account, backupFile)
                    CloudProvider.PROTON_DRIVE -> syncWithProtonDrive(account, backupFile)
                    CloudProvider.ONE_DRIVE -> syncWithOneDrive(account, backupFile)
                    CloudProvider.DROPBOX -> syncWithDropbox(account, backupFile)
                    CloudProvider.NONE -> Unit
                }
                if (account.provider != CloudProvider.GOOGLE_DRIVE) {
                    val backupBytes = backupFile.length().coerceAtLeast(1024L)
                    updateSettings { settings ->
                        settings.copy(
                            linkedAccounts = settings.linkedAccounts.map { existing ->
                                if (existing.id == account.id) {
                                    existing.withBackupFootprint(backupBytes)
                                } else {
                                    existing
                                }
                            }
                        )
                    }
                }
                backupFile.delete()
            }

            val syncedAt = System.currentTimeMillis()
            updateSettings { it.copy(lastSyncTime = syncedAt) }
        } catch (e: Exception) {
            Log.e("CloudSync", "Sync failed", e)
            _syncError.value = e.message ?: "Unknown sync error"
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun syncWithGoogleDrive(account: CloudLinkedAccount, backupFile: File) {
        val accountEmail = account.email ?: error("Google Drive account email is missing")
        val existingRemoteFileId = account.remoteBackupFileId
            ?: googleDriveApi.getLatestBackup(accountEmail)?.id
        val uploaded = googleDriveApi.uploadBackup(
            accountEmail = accountEmail,
            backupFile = backupFile,
            existingFileId = existingRemoteFileId
        )
        val quota = googleDriveApi.getStorageQuota(accountEmail)
        updateSettings { settings ->
            settings.copy(
                linkedAccounts = settings.linkedAccounts.map { existing ->
                    if (existing.id == account.id) {
                        existing.copy(
                            remoteBackupFileId = uploaded.id,
                            backupBytes = uploaded.size.coerceAtLeast(backupFile.length()),
                            lastBackupTime = uploaded.modifiedTime.takeIf { it > 0L } ?: System.currentTimeMillis(),
                            usedBytes = quota.usedBytes.coerceAtLeast(uploaded.size),
                            totalBytes = quota.totalBytes.coerceAtLeast(quota.usedBytes)
                        )
                    } else {
                        existing
                    }
                }
            )
        }
        Log.d("CloudSync", "Uploaded ${backupFile.length()} bytes backup to Google Drive for ${account.displayName}")
    }

    private suspend fun syncWithDropbox(account: CloudLinkedAccount, backupFile: File) {
        val uploaded = dropboxApi.uploadBackup(account, backupFile)
        updateSettings { settings ->
            settings.copy(
                linkedAccounts = settings.linkedAccounts.map { existing ->
                    if (existing.id == account.id) {
                        existing.copy(
                            accessToken = uploaded.accessToken,
                            refreshToken = uploaded.refreshToken ?: existing.refreshToken,
                            accessTokenExpiresAt = uploaded.accessTokenExpiresAt ?: existing.accessTokenExpiresAt,
                            remoteBackupFileId = uploaded.value.path,
                            backupBytes = uploaded.value.size.coerceAtLeast(backupFile.length()),
                            lastBackupTime = uploaded.value.modifiedTime.takeIf { it > 0L } ?: System.currentTimeMillis(),
                            usedBytes = uploaded.usedBytes,
                            totalBytes = uploaded.totalBytes.coerceAtLeast(uploaded.usedBytes)
                        )
                    } else {
                        existing
                    }
                }
            )
        }
        Log.d("CloudSync", "Uploaded ${backupFile.length()} bytes backup to Dropbox for ${account.displayName}")
    }

    private suspend fun syncWithWebDav(account: CloudLinkedAccount, backupFile: File) {
        val serverUrl = account.serverUrl ?: error("WebDAV server URL is missing")
        val username = account.email ?: error("WebDAV username is missing")
        val password = account.accessToken
        val uploaded = webDavApi.uploadBackup(
            serverUrl = serverUrl,
            username = username,
            password = password,
            backupFile = backupFile
        )
        updateSettings { settings ->
            settings.copy(
                linkedAccounts = settings.linkedAccounts.map { existing ->
                    if (existing.id == account.id) {
                        existing.copy(
                            remoteBackupFileId = uploaded.value.path,
                            backupBytes = uploaded.value.size.coerceAtLeast(backupFile.length()),
                            lastBackupTime = uploaded.value.modifiedTime.takeIf { it > 0L } ?: System.currentTimeMillis(),
                            usedBytes = uploaded.usedBytes,
                            totalBytes = uploaded.totalBytes.coerceAtLeast(uploaded.usedBytes)
                        )
                    } else {
                        existing
                    }
                }
            )
        }
        Log.d("CloudSync", "Uploaded ${backupFile.length()} bytes backup to WebDAV for ${account.displayName}")
    }

    private suspend fun syncWithProtonDrive(account: CloudLinkedAccount, backupFile: File) {
        simulateNetworkDelay()
        Log.d("CloudSync", "Prepared ${backupFile.length()} bytes backup for Proton Drive account ${account.displayName}")
    }

    private suspend fun syncWithOneDrive(account: CloudLinkedAccount, backupFile: File) {
        simulateNetworkDelay()
        Log.d("CloudSync", "Prepared ${backupFile.length()} bytes backup for OneDrive account ${account.displayName}")
    }

    private suspend fun simulateNetworkDelay() {
        delay(1500)
    }

    suspend fun refreshGoogleDriveAccount(accountId: String) = withContext(Dispatchers.IO) {
        val account = _linkedAccounts.value.firstOrNull { it.id == accountId } ?: return@withContext
        if (account.provider != CloudProvider.GOOGLE_DRIVE) return@withContext
        val accountEmail = account.email ?: return@withContext

        val quota = googleDriveApi.getStorageQuota(accountEmail)
        val latestBackup = googleDriveApi.getLatestBackup(accountEmail)
        updateSettings { settings ->
            settings.copy(
                linkedAccounts = settings.linkedAccounts.map { existing ->
                    if (existing.id == account.id) {
                        existing.copy(
                            remoteBackupFileId = latestBackup?.id,
                            backupBytes = latestBackup?.size ?: existing.backupBytes,
                            lastBackupTime = latestBackup?.modifiedTime ?: existing.lastBackupTime,
                            usedBytes = quota.usedBytes,
                            totalBytes = quota.totalBytes.coerceAtLeast(quota.usedBytes)
                        )
                    } else {
                        existing
                    }
                }
            )
        }
    }

    suspend fun refreshDropboxAccount(accountId: String) = withContext(Dispatchers.IO) {
        val account = _linkedAccounts.value.firstOrNull { it.id == accountId } ?: return@withContext
        if (account.provider != CloudProvider.DROPBOX) return@withContext

        val currentAccount = dropboxApi.getCurrentAccount(account)
        val latestBackup = dropboxApi.getLatestBackup(account)
        val quota = dropboxApi.getStorageQuota(account)
        updateSettings { settings ->
            settings.copy(
                linkedAccounts = settings.linkedAccounts.map { existing ->
                    if (existing.id == account.id) {
                        existing.copy(
                            displayName = currentAccount.value.name.displayName,
                            email = currentAccount.value.email,
                            photoUrl = currentAccount.value.profilePhotoUrl ?: existing.photoUrl,
                            accessToken = quota.accessToken,
                            refreshToken = quota.refreshToken ?: existing.refreshToken,
                            accessTokenExpiresAt = quota.accessTokenExpiresAt ?: existing.accessTokenExpiresAt,
                            accountId = currentAccount.value.accountId,
                            remoteBackupFileId = latestBackup.value?.path,
                            backupBytes = latestBackup.value?.size ?: existing.backupBytes,
                            lastBackupTime = latestBackup.value?.modifiedTime ?: existing.lastBackupTime,
                            usedBytes = quota.value.usedBytes,
                            totalBytes = quota.value.totalBytes.coerceAtLeast(quota.value.usedBytes)
                        )
                    } else {
                        existing
                    }
                }
            )
        }
    }

    suspend fun refreshWebDavAccount(accountId: String) = withContext(Dispatchers.IO) {
        val account = _linkedAccounts.value.firstOrNull { it.id == accountId } ?: return@withContext
        if (account.provider != CloudProvider.WEB_DAV) return@withContext

        val serverUrl = account.serverUrl ?: return@withContext
        val username = account.email ?: return@withContext
        val password = account.accessToken
        val quota = webDavApi.getStorageQuota(serverUrl, username, password)
        val latestBackup = webDavApi.getLatestBackup(serverUrl, username, password)
        updateSettings { settings ->
            settings.copy(
                linkedAccounts = settings.linkedAccounts.map { existing ->
                    if (existing.id == account.id) {
                        existing.copy(
                            remoteBackupFileId = latestBackup?.path,
                            backupBytes = latestBackup?.size ?: existing.backupBytes,
                            lastBackupTime = latestBackup?.modifiedTime ?: existing.lastBackupTime,
                            usedBytes = quota.usedBytes,
                            totalBytes = quota.totalBytes.coerceAtLeast(quota.usedBytes)
                        )
                    } else {
                        existing
                    }
                }
            )
        }
    }

    suspend fun restoreLatestBackup(accountId: String) = withContext(Dispatchers.IO) {
        val account = _linkedAccounts.value.firstOrNull { it.id == accountId }
            ?: error("Cloud account not found")
        when (account.provider) {
            CloudProvider.GOOGLE_DRIVE -> {
                val accountEmail = account.email ?: error("Google Drive account email is missing")
                val latestBackup = googleDriveApi.getLatestBackup(accountEmail)
                    ?: error("No Google Drive backup found for ${account.displayName}")
                val downloadFile = File(context.cacheDir, "google_drive_restore_${account.id}.zip")
                googleDriveApi.downloadBackup(accountEmail, latestBackup.id, downloadFile)
                restoreBackupArchive(downloadFile)
                downloadFile.delete()
                refreshGoogleDriveAccount(accountId)
            }

            CloudProvider.DROPBOX -> {
                val downloadFile = File(context.cacheDir, "dropbox_restore_${account.id}.zip")
                val downloaded = dropboxApi.downloadBackup(account, downloadFile)
                restoreBackupArchive(downloadFile)
                downloadFile.delete()
                updateSettings { settings ->
                    settings.copy(
                        linkedAccounts = settings.linkedAccounts.map { existing ->
                            if (existing.id == account.id) {
                                existing.copy(
                                    accessToken = downloaded.accessToken,
                                    refreshToken = downloaded.refreshToken ?: existing.refreshToken,
                                    accessTokenExpiresAt = downloaded.accessTokenExpiresAt ?: existing.accessTokenExpiresAt,
                                    remoteBackupFileId = downloaded.value.path,
                                    backupBytes = downloaded.value.size,
                                    lastBackupTime = downloaded.value.modifiedTime.takeIf { it > 0L } ?: existing.lastBackupTime,
                                    usedBytes = downloaded.usedBytes,
                                    totalBytes = downloaded.totalBytes.coerceAtLeast(downloaded.usedBytes)
                                )
                            } else {
                                existing
                            }
                        }
                    )
                }
            }

            CloudProvider.WEB_DAV -> {
                val serverUrl = account.serverUrl ?: error("WebDAV server URL is missing")
                val username = account.email ?: error("WebDAV username is missing")
                val password = account.accessToken
                val downloadFile = File(context.cacheDir, "webdav_restore_${account.id}.zip")
                val downloaded = webDavApi.downloadBackup(
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                    targetFile = downloadFile
                )
                restoreBackupArchive(downloadFile)
                downloadFile.delete()
                updateSettings { settings ->
                    settings.copy(
                        linkedAccounts = settings.linkedAccounts.map { existing ->
                            if (existing.id == account.id) {
                                existing.copy(
                                    remoteBackupFileId = downloaded.value.path,
                                    backupBytes = downloaded.value.size,
                                    lastBackupTime = downloaded.value.modifiedTime.takeIf { it > 0L } ?: existing.lastBackupTime,
                                    usedBytes = downloaded.usedBytes,
                                    totalBytes = downloaded.totalBytes.coerceAtLeast(downloaded.usedBytes)
                                )
                            } else {
                                existing
                            }
                        }
                    )
                }
            }

            else -> error("Restore is currently available for Google Drive, Dropbox, and WebDAV only")
        }
    }

    fun clearSyncError() {
        _syncError.value = null
    }

    fun clearAuthentication() {
        updateSettings {
            it.copy(
                provider = CloudProvider.NONE,
                linkedAccounts = emptyList()
            )
        }
    }

    suspend fun createBackupFile(
        scopes: Set<CloudBackupScope> = CloudBackupScope.defaultSelection(),
        fileName: String = "ereader_backup.json"
    ): File = withContext(Dispatchers.IO) {
        val json = exportBackupJson(scopes)
        val file = File(context.cacheDir, fileName)
        file.writeText(json)
        file
    }

    suspend fun createBackupArchive(
        scopes: Set<CloudBackupScope> = CloudBackupScope.defaultSelection(),
        fileName: String = "ereader_cloud_backup.zip"
    ): File = withContext(Dispatchers.IO) {
        val archiveFile = File(context.cacheDir, fileName)
        val bookEntries = if (CloudBackupScope.BOOKS in scopes) {
            collectBookArchiveEntries()
        } else {
            emptyList()
        }
        val readerAssetEntries = if (CloudBackupScope.READER_SETTINGS in scopes) {
            collectReaderAssetEntries()
        } else {
            emptyList()
        }

        ZipOutputStream(archiveFile.outputStream().buffered()).use { zip ->
            val payload = buildBackupPayload(
                scopes = scopes,
                extras = mapOf(
                    "bookFiles" to bookEntries.map { it.manifest },
                    "readerAssets" to readerAssetEntries.map { it.manifest }
                )
            )

            zip.putNextEntry(ZipEntry(BACKUP_MANIFEST_ENTRY))
            zip.write(gson.toJson(payload).toByteArray())
            zip.closeEntry()

            bookEntries.forEach { entry ->
                zip.putNextEntry(ZipEntry(entry.manifest.archivePath))
                entry.copyTo(zip)
                zip.closeEntry()
            }

            readerAssetEntries.forEach { entry ->
                zip.putNextEntry(ZipEntry(entry.manifest.archivePath))
                entry.copyTo(zip)
                zip.closeEntry()
            }
        }

        archiveFile
    }

    suspend fun exportBackupJson(
        scopes: Set<CloudBackupScope> = CloudBackupScope.defaultSelection()
    ): String = withContext(Dispatchers.IO) {
        gson.toJson(buildBackupPayload(scopes = scopes))
    }

    suspend fun createSnapshot(label: String? = null): Result<BackupSnapshotInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val createdAt = System.currentTimeMillis()
            val resolvedLabel = label?.trim().takeUnless { it.isNullOrBlank() }
                ?: "Snapshot ${SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(createdAt))}"
            val fileName = buildSnapshotFileName(createdAt, resolvedLabel)
            val file = File(snapshotDirectory(), fileName)
            val payload = buildBackupPayload(
                scopes = CloudBackupScope.defaultSelection(),
                extras = mapOf(
                    "snapshotLabel" to resolvedLabel,
                    "snapshotCreatedAt" to createdAt
                )
            )
            file.writeText(gson.toJson(payload))
            val info = BackupSnapshotInfo(
                id = file.name,
                label = resolvedLabel,
                createdAt = createdAt,
                fileSize = file.length()
            )
            refreshBackupSnapshots()
            info
        }
    }

    suspend fun restoreSnapshot(snapshotId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(snapshotDirectory(), snapshotId)
            require(file.exists()) { "Snapshot not found" }
            val root = JsonParser.parseString(file.readText()).asJsonObject
            restoreBackupPayload(root)
            refreshBackupSnapshots()
        }
    }

    suspend fun importBackupJson(json: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val root = JsonParser.parseString(json).asJsonObject
            if (!root.has("readerPreferencesJson") && !root.has("books") && !root.has("settings") && !root.has("appStartupPreferences")) {
                readerPreferencesStore.importPreferencesJson(json)
                return@runCatching
            }
            restoreBackupPayload(root)
            refreshBackupSnapshots()
        }
    }

    suspend fun deleteSnapshot(snapshotId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(snapshotDirectory(), snapshotId)
            if (file.exists() && !file.delete()) {
                error("Could not delete snapshot")
            }
            refreshBackupSnapshots()
        }
    }

    fun refreshBackupSnapshots() {
        val snapshots = snapshotDirectory()
            .listFiles { file -> file.isFile && file.extension.equals("json", ignoreCase = true) }
            .orEmpty()
            .mapNotNull { file -> file.toSnapshotInfo() }
            .sortedByDescending { it.createdAt }
        _backupSnapshots.value = snapshots
    }

    private suspend fun buildBackupPayload(
        scopes: Set<CloudBackupScope>,
        extras: Map<String, Any?> = emptyMap()
    ): Map<String, Any?> {
        val sanitizedSettings = _syncSettings.value.copy(
            accessToken = null,
            linkedAccounts = emptyList()
        )
        return buildMap {
            put("version", 5)
            put("timestamp", System.currentTimeMillis())
            put("settings", sanitizedSettings)
            put("backupScopes", scopes.map { it.name })
            put("authenticatedProviders", _linkedAccounts.value.map { it.provider.name }.distinct())
            put(
                "backupProfiles",
                _linkedAccounts.value.map { account ->
                    CloudBackupProfile(
                        provider = account.provider,
                        displayName = account.displayName,
                        email = account.email,
                        photoUrl = account.photoUrl,
                        connectedAt = account.connectedAt,
                        lastBackupTime = account.lastBackupTime
                    )
                }
            )

            if (CloudBackupScope.APP_SETTINGS in scopes) {
                put("appStartupPreferences", readerPreferencesStore.readAppStartupPreferences())
            }
            if (CloudBackupScope.READER_SETTINGS in scopes) {
                put("readerPreferencesJson", readerPreferencesStore.exportPreferencesJson())
            }
            if (CloudBackupScope.BOOKS in scopes) {
                put("books", database.bookDao().getAllBooksOnce())
            }
            if (CloudBackupScope.ANNOTATIONS in scopes) {
                put("highlights", database.highlightDao().getAllHighlightsOnce())
                put("bookmarks", database.bookmarkDao().getAllBookmarksOnce())
                put("marginNotes", database.marginNoteDao().getAllMarginNotesOnce())
                put("annotationCollections", database.annotationCollectionDao().getAllCollectionsOnce())
            }
            put("analytics", analyticsRepository.exportSnapshot())
            extras.forEach { (key, value) ->
                if (value != null) put(key, value)
            }
        }
    }

    private suspend fun restoreBackupArchive(archiveFile: File) {
        val extractedDir = File(context.cacheDir, RESTORE_EXTRACT_DIRECTORY).apply {
            deleteRecursively()
            mkdirs()
        }
        unzipArchive(archiveFile, extractedDir)
        val manifestFile = File(extractedDir, BACKUP_MANIFEST_ENTRY)
        require(manifestFile.exists()) { "Backup manifest is missing from archive" }

        val root = JsonParser.parseString(manifestFile.readText()).asJsonObject
        val restoredBookFiles = restoreArchivedBooks(extractedDir, root)
        val restoredReaderAssets = restoreArchivedReaderAssets(extractedDir, root)
        restoreBackupPayload(
            root = root,
            restoredBookFiles = restoredBookFiles,
            restoredReaderAssets = restoredReaderAssets
        )
        extractedDir.deleteRecursively()
        refreshBackupSnapshots()
    }

    private suspend fun restoreBackupPayload(
        root: com.google.gson.JsonObject,
        restoredBookFiles: Map<String, File> = emptyMap(),
        restoredReaderAssets: Map<String, File> = emptyMap()
    ) {
        root.get("appStartupPreferences")?.let { appPreferencesElement ->
            runCatching { gson.fromJson(appPreferencesElement, AppStartupPreferences::class.java) }
                .onSuccess { applyAppStartupPreferences(it) }
        }

        root.get("readerPreferencesJson")
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            ?.let { rewriteReaderPreferencesJson(it, restoredReaderAssets) }
            ?.let { readerPreferencesStore.importPreferencesJson(it) }

        root.get("settings")?.let { settingsElement ->
            runCatching { gson.fromJson(settingsElement, CloudSyncSettings::class.java) }
                .onSuccess { restored ->
                    updateSettings {
                        it.copy(
                            autoSync = restored.autoSync,
                            lastSyncTime = restored.lastSyncTime,
                            syncHighlights = restored.syncHighlights,
                            syncBookmarks = restored.syncBookmarks,
                            syncProgress = restored.syncProgress
                        )
                    }
                }
        }

        restoreDatabaseEntities(root, restoredBookFiles)

        root.get("analytics")?.let { analyticsElement ->
            runCatching { gson.fromJson(analyticsElement, AnalyticsBackupPayload::class.java) }
                .onSuccess { analyticsRepository.importSnapshot(it) }
        }
    }

    private suspend fun applyAppStartupPreferences(preferences: AppStartupPreferences) {
        readerPreferencesStore.setAppTheme(preferences.theme)
        readerPreferencesStore.setAppFont(preferences.appFont)
        readerPreferencesStore.setAppAccent(preferences.accent)
        readerPreferencesStore.setAppAccentCustomColor(preferences.customAccentColor)
        readerPreferencesStore.setLiquidGlassEnabled(preferences.liquidGlassEnabled)
        readerPreferencesStore.setNavigationBarStyle(preferences.navBarStyle)
        readerPreferencesStore.setHideStatusBar(preferences.hideStatusBar)
        readerPreferencesStore.setAppTextScale(preferences.appTextScale)
        readerPreferencesStore.setHideBetaFeatures(preferences.hideBetaFeatures)
    }

    private suspend fun restoreDatabaseEntities(
        root: com.google.gson.JsonObject,
        restoredBookFiles: Map<String, File> = emptyMap()
    ) {
        val books = parseList(root, "books", Array<BookEntity>::class.java)
            .map { book ->
                val restoredFile = restoredBookFiles[book.id] ?: return@map book
                book.copy(
                    uri = restoredFile.toUri().toString(),
                    fileName = restoredFile.name,
                    fileSize = restoredFile.length()
                )
            }
        val highlights = parseList(root, "highlights", Array<HighlightEntity>::class.java)
        val bookmarks = parseList(root, "bookmarks", Array<BookmarkEntity>::class.java)
        val marginNotes = parseList(root, "marginNotes", Array<MarginNoteEntity>::class.java)
        val annotationCollections = parseList(
            root,
            "annotationCollections",
            Array<AnnotationCollectionEntity>::class.java
        )

        if (
            books.isEmpty() &&
            highlights.isEmpty() &&
            bookmarks.isEmpty() &&
            marginNotes.isEmpty() &&
            annotationCollections.isEmpty()
        ) {
            return
        }

        database.annotationCollectionDao().clearAllCollections()
        database.highlightDao().clearAllHighlights()
        database.marginNoteDao().clearAllMarginNotes()
        database.bookmarkDao().clearAllBookmarks()
        database.bookDao().clearAllBooks()

        if (books.isNotEmpty()) database.bookDao().insertBooks(books)
        if (highlights.isNotEmpty()) database.highlightDao().insertHighlights(highlights)
        if (bookmarks.isNotEmpty()) database.bookmarkDao().insertBookmarks(bookmarks)
        if (marginNotes.isNotEmpty()) database.marginNoteDao().insertMarginNotes(marginNotes)
        if (annotationCollections.isNotEmpty()) database.annotationCollectionDao().insertCollections(annotationCollections)
    }

    private fun <T> parseList(
        root: com.google.gson.JsonObject,
        key: String,
        arrayType: Class<Array<T>>
    ): List<T> {
        val element = root.get(key) ?: return emptyList()
        return runCatching {
            gson.fromJson(element, arrayType)?.toList().orEmpty()
        }.getOrDefault(emptyList())
    }

    private suspend fun collectBookArchiveEntries(): List<StreamArchiveEntry<ArchivedBookFileManifest>> {
        val books = database.bookDao().getAllBooksOnce()
        return books.mapIndexed { index, book ->
            val uri = Uri.parse(book.uri)
            ensureUriReadable(uri, "book \"${book.title}\"")
            StreamArchiveEntry(
                sourceUri = uri,
                manifest = ArchivedBookFileManifest(
                    bookId = book.id,
                    fileName = book.fileName,
                    archivePath = "books/${index + 1}_${sanitizeArchiveName(book.fileName)}"
                )
            )
        }
    }

    private suspend fun collectReaderAssetEntries(): List<StreamArchiveEntry<ArchivedReaderAssetManifest>> {
        val settings = readerPreferencesStore.readResolvedReaderSettings()
        val entries = mutableListOf<StreamArchiveEntry<ArchivedReaderAssetManifest>>()

        settings.backgroundImageUri
            ?.takeIf { it.isNotBlank() }
            ?.let { uriString ->
                val uri = Uri.parse(uriString)
                ensureUriReadable(uri, "reader background")
                entries += StreamArchiveEntry(
                    sourceUri = uri,
                    manifest = ArchivedReaderAssetManifest(
                        preferenceKey = PREF_KEY_BACKGROUND_IMAGE_URI,
                        fileName = resolveArchiveFileName(uri, "background_image"),
                        archivePath = "reader_assets/background_${sanitizeArchiveName(resolveArchiveFileName(uri, "background_image"))}"
                    )
                )
            }

        settings.customFontUri
            ?.takeIf { it.isNotBlank() }
            ?.let { uriString ->
                val uri = Uri.parse(uriString)
                ensureUriReadable(uri, "reader font")
                entries += StreamArchiveEntry(
                    sourceUri = uri,
                    manifest = ArchivedReaderAssetManifest(
                        preferenceKey = PREF_KEY_CUSTOM_FONT_URI,
                        fileName = resolveArchiveFileName(uri, "custom_font"),
                        archivePath = "reader_assets/font_${sanitizeArchiveName(resolveArchiveFileName(uri, "custom_font"))}"
                    )
                )
            }

        return entries
    }

    private fun restoreArchivedBooks(
        extractedDir: File,
        root: com.google.gson.JsonObject
    ): Map<String, File> {
        val manifests = parseList(root, "bookFiles", Array<ArchivedBookFileManifest>::class.java)
        if (manifests.isEmpty()) return emptyMap()

        val targetDir = File(context.filesDir, RESTORED_BOOKS_DIRECTORY).apply {
            deleteRecursively()
            mkdirs()
        }

        return manifests.associate { manifest ->
            val source = File(extractedDir, manifest.archivePath)
            require(source.exists()) { "Missing archived book file ${manifest.fileName}" }
            val target = File(targetDir, "${manifest.bookId}_${sanitizeArchiveName(manifest.fileName)}")
            source.copyTo(target, overwrite = true)
            manifest.bookId to target
        }
    }

    private fun restoreArchivedReaderAssets(
        extractedDir: File,
        root: com.google.gson.JsonObject
    ): Map<String, File> {
        val manifests = parseList(root, "readerAssets", Array<ArchivedReaderAssetManifest>::class.java)
        if (manifests.isEmpty()) return emptyMap()

        val targetDir = File(context.filesDir, RESTORED_READER_ASSETS_DIRECTORY).apply {
            deleteRecursively()
            mkdirs()
        }

        return manifests.associate { manifest ->
            val source = File(extractedDir, manifest.archivePath)
            require(source.exists()) { "Missing archived reader asset ${manifest.fileName}" }
            val target = File(targetDir, sanitizeArchiveName(manifest.fileName))
            source.copyTo(target, overwrite = true)
            manifest.preferenceKey to target
        }
    }

    private fun rewriteReaderPreferencesJson(
        json: String,
        restoredReaderAssets: Map<String, File>
    ): String {
        if (restoredReaderAssets.isEmpty()) return json
        val rootType = object : com.google.gson.reflect.TypeToken<MutableMap<String, Any?>>() {}.type
        val values = runCatching {
            gson.fromJson<MutableMap<String, Any?>>(json, rootType)
        }.getOrElse { mutableMapOf() }

        restoredReaderAssets[PREF_KEY_BACKGROUND_IMAGE_URI]
            ?.let { values[PREF_KEY_BACKGROUND_IMAGE_URI] = it.toUri().toString() }
        restoredReaderAssets[PREF_KEY_CUSTOM_FONT_URI]
            ?.let { values[PREF_KEY_CUSTOM_FONT_URI] = it.toUri().toString() }

        return gson.toJson(values)
    }

    private fun unzipArchive(archiveFile: File, extractedDir: File) {
        val rootPath = extractedDir.canonicalPath
        ZipInputStream(archiveFile.inputStream().buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = File(extractedDir, entry.name).canonicalFile
                require(target.path == rootPath || target.path.startsWith("$rootPath${File.separator}")) {
                    "Invalid backup archive entry"
                }
                if (entry.isDirectory) {
                    target.mkdirs()
                } else {
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output ->
                        zip.copyTo(output)
                    }
                }
                zip.closeEntry()
            }
        }
    }

    private fun StreamArchiveEntry<*>.copyTo(output: java.io.OutputStream) {
        openUriInputStream(sourceUri).use { input ->
            input.copyTo(output)
        }
    }

    private fun openUriInputStream(uri: Uri): java.io.InputStream {
        return when (uri.scheme) {
            "file" -> {
                val path = uri.path ?: error("Invalid file URI")
                File(path).inputStream()
            }
            "content" -> context.contentResolver.openInputStream(uri)
                ?: error("Unable to read content URI $uri")
            else -> error("Unsupported URI scheme: ${uri.scheme}")
        }
    }

    private fun ensureUriReadable(uri: Uri, label: String) {
        runCatching { openUriInputStream(uri).close() }
            .getOrElse { throw IOException("Unable to read $label for backup", it) }
    }

    private fun resolveArchiveFileName(uri: Uri, fallback: String): String {
        return uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.takeIf { it.isNotBlank() }
            ?: fallback
    }

    private fun sanitizeArchiveName(name: String): String {
        return name.replace("[^A-Za-z0-9._-]".toRegex(), "_").ifBlank { "file" }
    }

    private fun snapshotDirectory(): File {
        return File(context.filesDir, SNAPSHOT_DIRECTORY).apply { mkdirs() }
    }

    private fun buildSnapshotFileName(createdAt: Long, label: String): String {
        val slug = label
            .lowercase(Locale.ROOT)
            .replace("[^a-z0-9]+".toRegex(), "-")
            .trim('-')
            .take(40)
            .ifBlank { "snapshot" }
        return "${createdAt}_${slug}.json"
    }

    private fun File.toSnapshotInfo(): BackupSnapshotInfo? {
        return runCatching {
            val root = JsonParser.parseString(readText()).asJsonObject
            val label = root.get("snapshotLabel")?.takeIf { it.isJsonPrimitive }?.asString
                ?: nameWithoutExtension.substringAfter('_').replace('-', ' ').replaceFirstChar { it.uppercase() }
            val createdAt = root.get("snapshotCreatedAt")?.takeIf { it.isJsonPrimitive }?.asLong
                ?: lastModified()
            BackupSnapshotInfo(
                id = name,
                label = label,
                createdAt = createdAt,
                fileSize = length()
            )
        }.getOrNull()
    }

    private fun summarizeStorage(accounts: List<CloudLinkedAccount>): CloudStorageSummary {
        return CloudStorageSummary(
            totalUsedBytes = accounts.sumOf { it.usedBytes },
            totalCapacityBytes = accounts.sumOf { it.totalBytes },
            linkedAccountCount = accounts.size,
            linkedProviderCount = accounts.map { it.provider }.distinct().size
        )
    }

    private fun createLinkedAccount(
        provider: CloudProvider,
        accessToken: String,
        displayName: String,
        email: String? = null,
        photoUrl: String? = null,
        refreshToken: String? = null,
        accessTokenExpiresAt: Long? = null,
        accountId: String? = null,
        serverUrl: String? = null,
        usedBytes: Long? = null,
        totalBytes: Long? = null
    ): CloudLinkedAccount {
        val connectedAt = System.currentTimeMillis()
        val resolvedTotalBytes = totalBytes ?: provider.defaultTotalBytes()
        val resolvedUsedBytes = usedBytes ?: provider.simulatedUsedBytes(
            seed = email ?: "$displayName$accessToken",
            totalBytes = resolvedTotalBytes
        )
        return CloudLinkedAccount(
            id = stableMd5("${provider.name}:${email ?: displayName}:$connectedAt"),
            provider = provider,
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresAt = accessTokenExpiresAt,
            accountId = accountId,
            serverUrl = serverUrl,
            connectedAt = connectedAt,
            usedBytes = resolvedUsedBytes,
            totalBytes = resolvedTotalBytes,
            enabledBackupScopes = CloudBackupScope.defaultSelection()
        )
    }

    private fun defaultAccountName(provider: CloudProvider, index: Int): String {
        val providerName = when (provider) {
            CloudProvider.GOOGLE_DRIVE -> "Google Drive"
            CloudProvider.PROTON_DRIVE -> "Proton Drive"
            CloudProvider.ONE_DRIVE -> "OneDrive"
            CloudProvider.DROPBOX -> "Dropbox"
            CloudProvider.WEB_DAV -> "WebDAV"
            CloudProvider.NONE -> "Cloud"
        }
        return "$providerName Account $index"
    }

    private fun CloudSyncSettings.sanitized(): CloudSyncSettings {
        val normalizedAccounts = linkedAccounts
            .mapIndexed { index, account -> account.hydrateDefaults(index) }
            .distinctBy { it.id }
        return copy(
            provider = normalizedAccounts.firstOrNull()?.provider ?: CloudProvider.NONE,
            isEnabled = normalizedAccounts.isNotEmpty(),
            accessToken = null,
            linkedAccounts = normalizedAccounts
        )
    }

    private fun CloudLinkedAccount.hydrateDefaults(index: Int): CloudLinkedAccount {
        val resolvedTotal = totalBytes.takeIf { it > 0 } ?: provider.defaultTotalBytes()
        val resolvedName = displayName.ifBlank { defaultAccountName(provider, index + 1) }
        val resolvedUsed = usedBytes.takeIf { it > 0 } ?: provider.simulatedUsedBytes(
            seed = email ?: resolvedName,
            totalBytes = resolvedTotal
        )
        return copy(
            displayName = resolvedName,
            totalBytes = resolvedTotal,
            usedBytes = resolvedUsed.coerceAtMost(resolvedTotal),
            enabledBackupScopes = enabledBackupScopes
        )
    }

    private fun CloudLinkedAccount.withBackupFootprint(backupBytes: Long): CloudLinkedAccount {
        val nonBackupUsedBytes = (usedBytes - backupBytes).coerceAtLeast(0L)
        val nextUsedBytes = (nonBackupUsedBytes + backupBytes).coerceAtMost(totalBytes)
        return copy(
            backupBytes = backupBytes,
            usedBytes = nextUsedBytes,
            lastBackupTime = System.currentTimeMillis()
        )
    }

    private fun CloudProvider.defaultTotalBytes(): Long {
        return when (this) {
            CloudProvider.GOOGLE_DRIVE -> 15L * GIGABYTE
            CloudProvider.PROTON_DRIVE -> 5L * GIGABYTE
            CloudProvider.ONE_DRIVE -> 5L * GIGABYTE
            CloudProvider.DROPBOX -> 2L * GIGABYTE
            CloudProvider.WEB_DAV -> 0L
            CloudProvider.NONE -> 0L
        }
    }

    private fun CloudProvider.simulatedUsedBytes(seed: String, totalBytes: Long): Long {
        if (totalBytes <= 0L) return 0L
        val ratio = (stableMd5("${name}_$seed").takeLast(2).toInt(16) / 255f)
        val usagePercent = 0.18f + (ratio * 0.54f)
        return (totalBytes * usagePercent).toLong()
    }

    companion object {
        private const val KEY_SETTINGS = "cloud_sync_settings"
        private const val KEY_OAUTH_TOKENS = "oauth_tokens"
        private const val SNAPSHOT_DIRECTORY = "backup_snapshots"
        private const val BACKUP_MANIFEST_ENTRY = "backup_manifest.json"
        private const val RESTORE_EXTRACT_DIRECTORY = "cloud_restore_extract"
        private const val RESTORED_BOOKS_DIRECTORY = "cloud_restore/books"
        private const val RESTORED_READER_ASSETS_DIRECTORY = "cloud_restore/reader_assets"
        private const val PREF_KEY_BACKGROUND_IMAGE_URI = "background_image_uri"
        private const val PREF_KEY_CUSTOM_FONT_URI = "custom_font_uri"
        private const val GIGABYTE = 1024L * 1024L * 1024L
    }
}

private data class StreamArchiveEntry<T>(
    val sourceUri: Uri,
    val manifest: T
)

private data class ArchivedBookFileManifest(
    val bookId: String,
    val fileName: String,
    val archivePath: String
)

private data class ArchivedReaderAssetManifest(
    val preferenceKey: String,
    val fileName: String,
    val archivePath: String
)
