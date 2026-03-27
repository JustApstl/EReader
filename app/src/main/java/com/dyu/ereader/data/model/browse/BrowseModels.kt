package com.dyu.ereader.data.model.browse

data class BrowseCatalog(
    val id: String,
    val title: String,
    val url: String,
    val description: String? = null,
    val icon: String? = null,
    val languages: List<String> = emptyList(),
    val formats: List<String> = emptyList(),
    val isCustom: Boolean = false,
    val username: String? = null,
    val password: String? = null,
    val apiKey: String? = null
)

data class BrowseBook(
    val id: String,
    val title: String,
    val author: String,
    val summary: String? = null,
    val coverUrl: String? = null,
    val downloadUrl: String,
    val detailUrl: String? = null,
    val format: String, // EPUB, PDF, etc.
    val rights: String? = null,
    val publisher: String? = null,
    val published: String? = null,
    val downloadOptions: List<BrowseDownloadOption> = emptyList(),
    val subjects: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val downloads: Int? = null
)

data class BrowseDownloadOption(
    val url: String,
    val format: String,
    val mimeType: String? = null,
    val sizeBytes: Long? = null,
    val label: String? = null
)

data class BrowseFeed(
    val id: String,
    val title: String,
    val entries: List<BrowseBook>,
    val links: List<BrowseLink>,
    val navigationGroups: List<BrowseNavigationGroup> = emptyList()
)

data class BrowseLink(
    val rel: String, // "next", "prev", etc.
    val href: String,
    val type: String
)

data class BrowseNavigationGroup(
    val title: String,
    val href: String,
    val rel: String = "",
    val type: String = ""
)

enum class CatalogHealthStatus {
    UNKNOWN,
    CHECKING,
    ONLINE,
    ERROR
}

enum class BrowseDownloadState {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELED
}

data class BrowseTransferProgress(
    val progress: Float,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val speedBytesPerSecond: Long? = null
)

data class BrowseDownloadTask(
    val id: String,
    val title: String,
    val author: String,
    val format: String,
    val coverUrl: String?,
    val book: BrowseBook? = null,
    val directUrl: String? = null,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null,
    val speedBytesPerSecond: Long? = null,
    val state: BrowseDownloadState = BrowseDownloadState.QUEUED,
    val error: String? = null,
    val alreadyInLibrary: Boolean = false,
    val duplicateInQueue: Boolean = false
)
