package com.dyu.ereader.data.repository.browse

internal const val BROWSE_USER_AGENT = "EReader/Android (OPDS client)"

internal data class OpdsProfile(
    val id: String,
    val hostHint: String,
    val preferHttps: Boolean = true,
    val userAgent: String = BROWSE_USER_AGENT,
    val acceptHeader: String = "application/atom+xml,application/xml;q=0.9,application/opds+json,application/json;q=0.8,*/*;q=0.7",
    val coverRelPriority: List<String> = listOf(
        "http://opds-spec.org/image",
        "http://opds-spec.org/image/thumbnail",
        "image",
        "image/thumbnail",
        "thumbnail",
        "cover"
    ),
    val downloadRelPriority: List<String> = listOf(
        "http://opds-spec.org/acquisition/open-access",
        "http://opds-spec.org/acquisition",
        "acquisition",
        "alternate",
        "self"
    ),
    val searchTemplateOverride: String? = null,
    val formatScores: Map<String, Int> = emptyMap(),
    val downloadTokenBoosts: Map<String, Int> = emptyMap()
)

internal data class DownloadInfo(
    val url: String,
    val type: String? = null
)

internal data class EntryDetails(
    val author: String? = null,
    val coverUrl: String? = null,
    val summary: String? = null,
    val rights: String? = null,
    val publisher: String? = null,
    val published: String? = null,
    val subjects: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val downloads: Int? = null
)
