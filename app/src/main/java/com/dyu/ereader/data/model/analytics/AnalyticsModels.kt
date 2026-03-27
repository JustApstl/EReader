package com.dyu.ereader.data.model.analytics

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
    val completed: Boolean = false,
    val preferredFormat: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class LibraryStatistics(
    val totalBooksRead: Int = 0,
    val totalMinutesRead: Long = 0L,
    val averagePerBook: Long = 0L,
    val averageSessionDurationMs: Long = 0L,
    val currentStreakDays: Int = 0,
    val booksFinished: Int = 0,
    val mostUsedFormat: String? = null,
    val favoriteGenre: String? = null,
    val totalHighlights: Int = 0,
    val totalBookmarks: Int = 0,
    val lastUpdated: Long = System.currentTimeMillis()
)

data class AnalyticsSettings(
    val analyticsEnabled: Boolean = true,
    val shareStatistics: Boolean = false,
    val privacyMode: Boolean = true // Don't track sensitive data
)

data class ReadingSession(
    val bookId: String,
    val bookTitle: String,
    val format: String? = null,
    val startTime: Long,
    val duration: Long, // in milliseconds
    val pagesRead: Int = 0,
    val wordsRead: Int = 0
)
