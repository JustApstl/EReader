package com.dyu.ereader.ui.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.dyu.ereader.data.model.cloud.CloudBackupScope
import com.dyu.ereader.data.model.cloud.CloudProvider
import kotlinx.coroutines.launch

internal fun HomeViewModel.handleSignInToCloud(provider: CloudProvider) {
    viewModelScope.launch {
        val account = cloudRepo.addCloudAccount(
            provider = provider,
            accessToken = "local_token_${System.currentTimeMillis()}"
        )
        cloudMessageFlow.value = "Linked ${account.displayName}"
    }
}

internal fun HomeViewModel.handleOnCloudAuthComplete(
    provider: CloudProvider,
    accessToken: String,
    displayName: String?,
    email: String?,
    photoUrl: String?,
    refreshToken: String?,
    accessTokenExpiresAt: Long?,
    accountId: String?,
    serverUrl: String?,
    usedBytes: Long?,
    totalBytes: Long?
) {
    viewModelScope.launch {
        val account = cloudRepo.addCloudAccount(
            provider = provider,
            accessToken = accessToken,
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            refreshToken = refreshToken,
            accessTokenExpiresAt = accessTokenExpiresAt,
            accountId = accountId,
            serverUrl = serverUrl,
            usedBytes = usedBytes,
            totalBytes = totalBytes
        )
        when (provider) {
            CloudProvider.GOOGLE_DRIVE -> runCatching { cloudRepo.refreshGoogleDriveAccount(account.id) }
            CloudProvider.DROPBOX -> runCatching { cloudRepo.refreshDropboxAccount(account.id) }
            CloudProvider.WEB_DAV -> runCatching { cloudRepo.refreshWebDavAccount(account.id) }
            else -> Unit
        }
        cloudMessageFlow.value = "Linked ${account.displayName}"
    }
}

internal fun HomeViewModel.handleSignOutFromCloud() {
    viewModelScope.launch {
        cloudRepo.clearAuthentication()
        cloudMessageFlow.value = "Disconnected all cloud accounts"
    }
}

internal fun HomeViewModel.handleRemoveCloudAccount(accountId: String) {
    viewModelScope.launch {
        val accountName = cloudRepo.getCloudAccount(accountId)?.displayName
        cloudRepo.removeCloudAccount(accountId)
        cloudMessageFlow.value = if (accountName != null) {
            "Removed $accountName"
        } else {
            "Removed cloud account"
        }
    }
}

internal fun HomeViewModel.handleSyncNow() {
    viewModelScope.launch {
        cloudRepo.performSync()
        if (cloudRepo.syncError.value == null) {
            cloudMessageFlow.value = "Cloud backup completed"
        }
    }
}

internal fun HomeViewModel.handleSyncCloudAccount(accountId: String) {
    viewModelScope.launch {
        val accountName = cloudRepo.getCloudAccount(accountId)?.displayName
        cloudRepo.performSync(accountId)
        if (cloudRepo.syncError.value == null) {
            cloudMessageFlow.value = if (accountName != null) {
                "Backed up $accountName"
            } else {
                "Cloud backup completed"
            }
        }
    }
}

internal fun HomeViewModel.handleRestoreCloudAccount(accountId: String) {
    viewModelScope.launch {
        val accountName = cloudRepo.getCloudAccount(accountId)?.displayName
        runCatching { cloudRepo.restoreLatestBackup(accountId) }
            .onSuccess {
                cloudMessageFlow.value = if (accountName != null) {
                    "Restored latest backup from $accountName"
                } else {
                    "Cloud restore completed"
                }
            }
            .onFailure { error ->
                cloudMessageFlow.value = "Restore failed: ${error.localizedMessage ?: "Unknown error"}"
            }
    }
}

internal fun HomeViewModel.handleClearSyncError() {
    cloudRepo.clearSyncError()
}

internal fun HomeViewModel.handleSetCloudAccountBackupScope(
    accountId: String,
    scope: CloudBackupScope,
    enabled: Boolean
) {
    val currentAccount = cloudRepo.getCloudAccount(accountId) ?: return
    val updatedScopes = currentAccount.enabledBackupScopes.toMutableSet().apply {
        if (enabled) add(scope) else remove(scope)
    }
    cloudRepo.updateAccountBackupScopes(accountId, updatedScopes)
}

internal fun HomeViewModel.handleCreateBackupSnapshot(label: String) {
    viewModelScope.launch {
        cloudRepo.createSnapshot(label)
            .onSuccess { snapshot ->
                cloudMessageFlow.value = "Saved snapshot \"${snapshot.label}\""
            }
            .onFailure { error ->
                cloudMessageFlow.value = "Snapshot failed: ${error.localizedMessage ?: "Unknown error"}"
            }
    }
}

internal fun HomeViewModel.handleRestoreBackupSnapshot(snapshotId: String, label: String) {
    viewModelScope.launch {
        cloudRepo.restoreSnapshot(snapshotId)
            .onSuccess {
                cloudMessageFlow.value = "Restored \"$label\""
            }
            .onFailure { error ->
                cloudMessageFlow.value = "Restore failed: ${error.localizedMessage ?: "Unknown error"}"
            }
    }
}

internal fun HomeViewModel.handleDeleteBackupSnapshot(snapshotId: String, label: String) {
    viewModelScope.launch {
        cloudRepo.deleteSnapshot(snapshotId)
            .onSuccess {
                cloudMessageFlow.value = "Deleted \"$label\""
            }
            .onFailure { error ->
                cloudMessageFlow.value = "Delete failed: ${error.localizedMessage ?: "Unknown error"}"
            }
    }
}

internal fun HomeViewModel.consumeCloudMessageInternal() {
    cloudMessageFlow.value = null
}

internal fun HomeViewModel.handleClearAnalytics() {
    analyticsRepo.clearAnalytics()
}
