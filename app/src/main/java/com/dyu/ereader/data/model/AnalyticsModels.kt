package com.dyu.ereader.data.model

data class ReadingStatistics(
    val bookId: String,
    val totalMinutesRead: Long = 0L,
    val sessionsCount: Int = 0,
    val averageSessionDuration: Long = 0L,
    val wordsPerMinute: Int = 0,
    val highlightCount: Int = 0,
    val bookmarkCount: Int = 0,
    val pagesTurned: Int = 0,
    val searchCount: Int = 0,
    val ttsUsageMs: Long = 0L,
    val noteCount: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class LibraryStatistics(
    val totalBooksRead: Int = 0,
    val totalMinutesRead: Long = 0L,
    val averagePerBook: Long = 0L,
    val favoriteGenre: String? = null,
    val totalHighlights: Int = 0,
    val totalBookmarks: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class AnalyticsSettings(
    val analyticsEnabled: Boolean = false,
    val shareStatistics: Boolean = false,
    val privacyMode: Boolean = true // Don't track sensitive data
)

data class ReadingSession(
    val bookId: String,
    val bookTitle: String,
    val startTime: Long,
    val duration: Long, // in milliseconds
    val pagesRead: Int = 0,
    val wordsRead: Int = 0
)
