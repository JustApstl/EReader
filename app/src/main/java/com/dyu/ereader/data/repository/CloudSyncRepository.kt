package com.dyu.ereader.data.repository

import android.content.Context
import android.util.Log
import com.dyu.ereader.data.database.BookDatabase
import com.dyu.ereader.data.model.CloudProvider
import com.dyu.ereader.data.model.CloudSyncSettings
import com.dyu.ereader.data.storage.ReaderPreferencesStore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class CloudSyncRepository(
    private val context: Context,
    private val database: BookDatabase,
    private val readerPreferencesStore: ReaderPreferencesStore
) {
    private val gson = Gson()
    private val _syncSettings = MutableStateFlow(CloudSyncSettings())
    val syncSettings: StateFlow<CloudSyncSettings> = _syncSettings.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long>(0L)
    val lastSyncTime: StateFlow<Long> = _lastSyncTime.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _oauthTokens = MutableStateFlow<Map<CloudProvider, String>>(emptyMap())
    val oauthTokens: StateFlow<Map<CloudProvider, String>> = _oauthTokens.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        try {
            val settingsJson = context.getSharedPreferences("cloud_sync", Context.MODE_PRIVATE)
                .getString(KEY_SETTINGS, null)
            if (settingsJson != null) {
                val loaded = gson.fromJson(settingsJson, CloudSyncSettings::class.java)
                _syncSettings.value = loaded
            }
            loadTokens()
        } catch (e: Exception) {
            Log.e("CloudSync", "Failed to load settings", e)
        }
    }

    private fun loadTokens() {
        try {
            val tokensJson = context.getSharedPreferences("cloud_sync", Context.MODE_PRIVATE)
                .getString(KEY_OAUTH_TOKENS, null)
            if (tokensJson != null) {
                val loaded = gson.fromJson(tokensJson, Map::class.java) as? Map<String, String> ?: emptyMap()
                val tokenMap = loaded.mapKeys { (key, _) ->
                    try {
                        CloudProvider.valueOf(key)
                    } catch (e: Exception) {
                        CloudProvider.NONE
                    }
                }.filterKeys { it != CloudProvider.NONE }
                _oauthTokens.value = tokenMap
                _isAuthenticated.value = tokenMap.isNotEmpty()
            }
        } catch (e: Exception) {
            Log.e("CloudSync", "Failed to load tokens", e)
        }
    }

    fun setProvider(provider: CloudProvider) {
        _syncSettings.value = _syncSettings.value.copy(provider = provider)
        saveSettings()
    }

    fun setAutoSync(enabled: Boolean) {
        _syncSettings.value = _syncSettings.value.copy(autoSync = enabled)
        saveSettings()
    }

    fun setOAuthToken(provider: CloudProvider, token: String) {
        val current = _oauthTokens.value.toMutableMap()
        current[provider] = token
        _oauthTokens.value = current
        _isAuthenticated.value = true
        saveTokens()
        
        if (_syncSettings.value.provider == CloudProvider.NONE) {
            setProvider(provider)
        }
    }

    fun removeOAuthToken(provider: CloudProvider) {
        val current = _oauthTokens.value.toMutableMap()
        current.remove(provider)
        _oauthTokens.value = current
        _isAuthenticated.value = current.isNotEmpty()
        saveTokens()
        
        if (_syncSettings.value.provider == provider) {
            setProvider(CloudProvider.NONE)
        }
    }

    fun getOAuthToken(provider: CloudProvider): String? {
        return _oauthTokens.value[provider]
    }

    private fun saveSettings() {
        try {
            val settingsJson = gson.toJson(_syncSettings.value)
            context.getSharedPreferences("cloud_sync", Context.MODE_PRIVATE).edit().apply {
                putString(KEY_SETTINGS, settingsJson)
                apply()
            }
        } catch (e: Exception) {
            Log.e("CloudSync", "Failed to save settings", e)
        }
    }

    private fun saveTokens() {
        try {
            val tokenMap = _oauthTokens.value.mapKeys { (key, _) -> key.name }
            val tokensJson = gson.toJson(tokenMap)
            context.getSharedPreferences("cloud_sync", Context.MODE_PRIVATE).edit().apply {
                putString(KEY_OAUTH_TOKENS, tokensJson)
                apply()
            }
        } catch (e: Exception) {
            Log.e("CloudSync", "Failed to save tokens", e)
        }
    }

    suspend fun performSync() = withContext(Dispatchers.IO) {
        if (_isSyncing.value) return@withContext
        if (_syncSettings.value.provider == CloudProvider.NONE) return@withContext
        
        _isSyncing.value = true
        _syncError.value = null

        try {
            val provider = _syncSettings.value.provider
            val token = getOAuthToken(provider)
            if (token == null) {
                _syncError.value = "${provider.name}: Not authenticated"
                _isSyncing.value = false
                return@withContext
            }

            val backupFile = createBackupFile()

            when (provider) {
                CloudProvider.GOOGLE_DRIVE -> syncWithGoogleDrive(token, backupFile)
                CloudProvider.PROTON_DRIVE -> syncWithProtonDrive(token, backupFile)
                CloudProvider.ONE_DRIVE -> syncWithOneDrive(token, backupFile)
                CloudProvider.DROPBOX -> syncWithDropbox(token, backupFile)
                CloudProvider.NONE -> {
                    _isSyncing.value = false
                    return@withContext
                }
            }
            
            val currentTime = System.currentTimeMillis()
            _lastSyncTime.value = currentTime
            _syncSettings.value = _syncSettings.value.copy(lastSyncTime = currentTime)
            saveSettings()
            
        } catch (e: Exception) {
            Log.e("CloudSync", "Sync failed", e)
            _syncError.value = e.message ?: "Unknown sync error"
        } finally {
            _isSyncing.value = false
        }
    }

    private suspend fun syncWithGoogleDrive(token: String, backupFile: File) {
        simulateNetworkDelay()
        Log.d("CloudSync", "Prepared ${backupFile.length()} bytes backup for Google Drive")
    }

    private suspend fun syncWithDropbox(token: String, backupFile: File) {
        simulateNetworkDelay()
        Log.d("CloudSync", "Prepared ${backupFile.length()} bytes backup for Dropbox")
    }

    private suspend fun syncWithProtonDrive(token: String, backupFile: File) {
        simulateNetworkDelay()
        Log.d("CloudSync", "Prepared ${backupFile.length()} bytes backup for Proton Drive")
    }

    private suspend fun syncWithOneDrive(token: String, backupFile: File) {
        simulateNetworkDelay()
        Log.d("CloudSync", "Prepared ${backupFile.length()} bytes backup for OneDrive")
    }

    private suspend fun simulateNetworkDelay() {
        delay(2000)
    }

    fun clearSyncError() {
        _syncError.value = null
    }

    fun clearAuthentication() {
        _oauthTokens.value = emptyMap()
        _isAuthenticated.value = false
        _syncSettings.value = _syncSettings.value.copy(provider = CloudProvider.NONE)
        saveTokens()
        saveSettings()
    }

    suspend fun createBackupFile(): File = withContext(Dispatchers.IO) {
        val backupData = mutableMapOf<String, Any>()
        
        backupData["version"] = 2
        backupData["timestamp"] = System.currentTimeMillis()
        backupData["settings"] = _syncSettings.value
        backupData["authenticatedProviders"] = _oauthTokens.value.keys.map { it.name }
        backupData["readerPreferencesJson"] = readerPreferencesStore.exportPreferencesJson()
        backupData["books"] = database.bookDao().getAllBooksOnce()
        backupData["highlights"] = database.highlightDao().getAllHighlightsOnce()
        backupData["bookmarks"] = database.bookmarkDao().getAllBookmarksOnce()
        backupData["marginNotes"] = database.marginNoteDao().getAllMarginNotesOnce()

        val json = gson.toJson(backupData)
        val file = File(context.cacheDir, "ereader_backup.json")
        file.writeText(json)
        file
    }

    companion object {
        private const val KEY_SETTINGS = "cloud_sync_settings"
        private const val KEY_OAUTH_TOKENS = "oauth_tokens"
    }
}
