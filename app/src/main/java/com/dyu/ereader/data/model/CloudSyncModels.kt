package com.dyu.ereader.data.model

enum class CloudProvider {
    GOOGLE_DRIVE, PROTON_DRIVE, ONE_DRIVE, DROPBOX, NONE
}

data class CloudSyncSettings(
    val provider: CloudProvider = CloudProvider.NONE,
    val isEnabled: Boolean = false,
    val autoSync: Boolean = false,
    val lastSyncTime: Long = 0L,
    val accessToken: String? = null,
    val syncHighlights: Boolean = true,
    val syncBookmarks: Boolean = true,
    val syncProgress: Boolean = true
)

data class SyncConflict(
    val localVersion: Long,
    val remoteVersion: Long,
    val fileName: String,
    val resolvedToLocal: Boolean = false
)
