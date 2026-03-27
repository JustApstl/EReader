package com.dyu.ereader.data.model.update

data class AppReleaseInfo(
    val versionName: String,
    val tagName: String,
    val title: String,
    val notes: String,
    val htmlUrl: String,
    val downloadUrl: String?,
    val assetName: String?,
    val assetLabel: String? = null,
    val publishedAt: Long? = null
)

data class PendingAppInstall(
    val versionName: String,
    val fileName: String,
    val uriString: String
)

data class AppUpdateUiState(
    val isChecking: Boolean = false,
    val isPreparingInstall: Boolean = false,
    val latestRelease: AppReleaseInfo? = null,
    val releaseHistory: List<AppReleaseInfo> = emptyList(),
    val updateAvailable: Boolean = false,
    val lastCheckedAt: Long? = null,
    val errorMessage: String? = null,
    val showUpdatePrompt: Boolean = false,
    val changelogRelease: AppReleaseInfo? = null,
    val showChangelogPrompt: Boolean = false,
    val showLatestReleaseDetails: Boolean = false,
    val isLoadingReleaseHistory: Boolean = false,
    val showReleaseHistory: Boolean = false
)
