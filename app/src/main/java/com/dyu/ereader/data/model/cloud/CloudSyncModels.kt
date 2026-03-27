package com.dyu.ereader.data.model.cloud

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class CloudProvider {
    GOOGLE_DRIVE, PROTON_DRIVE, ONE_DRIVE, DROPBOX, WEB_DAV, NONE
}

enum class CloudBackupScope(val label: String) {
    BOOKS("Books"),
    APP_SETTINGS("App Settings"),
    READER_SETTINGS("Reader Settings"),
    ANNOTATIONS("Annotations");

    companion object {
        fun defaultSelection(): Set<CloudBackupScope> = entries.toSet()
    }
}

data class CloudLinkedAccount(
    val id: String,
    val provider: CloudProvider,
    val displayName: String,
    val email: String? = null,
    val photoUrl: String? = null,
    val accessToken: String,
    val refreshToken: String? = null,
    val accessTokenExpiresAt: Long? = null,
    val accountId: String? = null,
    val serverUrl: String? = null,
    val connectedAt: Long = System.currentTimeMillis(),
    val lastBackupTime: Long = 0L,
    val usedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val backupBytes: Long = 0L,
    val remoteBackupFileId: String? = null,
    val enabledBackupScopes: Set<CloudBackupScope> = CloudBackupScope.defaultSelection()
)

data class CloudStorageSummary(
    val totalUsedBytes: Long = 0L,
    val totalCapacityBytes: Long = 0L,
    val linkedAccountCount: Int = 0,
    val linkedProviderCount: Int = 0
)

data class CloudSyncSettings(
    val provider: CloudProvider = CloudProvider.NONE,
    val isEnabled: Boolean = false,
    val autoSync: Boolean = false,
    val lastSyncTime: Long = 0L,
    val accessToken: String? = null,
    val syncHighlights: Boolean = true,
    val syncBookmarks: Boolean = true,
    val syncProgress: Boolean = true,
    val linkedAccounts: List<CloudLinkedAccount> = emptyList()
)

data class CloudBackupProfile(
    val provider: CloudProvider,
    val displayName: String,
    val email: String? = null,
    val photoUrl: String? = null,
    val connectedAt: Long = 0L,
    val lastBackupTime: Long = 0L
)

data class SyncConflict(
    val localVersion: Long,
    val remoteVersion: Long,
    val fileName: String,
    val resolvedToLocal: Boolean = false
)

data class BackupSnapshotInfo(
    val id: String,
    val label: String,
    val createdAt: Long,
    val fileSize: Long
)

data class CloudProviderInfo(
    val provider: CloudProvider,
    val name: String,
    val iconUrl: String?,
    val fallbackIcon: ImageVector,
    val color: Color,
    val description: String
)
