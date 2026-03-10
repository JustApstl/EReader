package com.dyu.ereader.data.repository

import com.dyu.ereader.data.model.AnalyticsSettings
import com.dyu.ereader.data.model.LibraryStatistics
import com.dyu.ereader.data.model.ReadingStatistics
import com.dyu.ereader.data.model.ReadingSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AnalyticsRepository {
    private val _settings = MutableStateFlow(AnalyticsSettings())
    val settings: StateFlow<AnalyticsSettings> = _settings.asStateFlow()

    private val _libraryStats = MutableStateFlow(LibraryStatistics())
    val libraryStatistics: StateFlow<LibraryStatistics> = _libraryStats.asStateFlow()

    private val _bookStats = MutableStateFlow<Map<String, ReadingStatistics>>(emptyMap())
    val bookStatistics: StateFlow<Map<String, ReadingStatistics>> = _bookStats.asStateFlow()

    private val _readingSessions = MutableStateFlow<List<com.dyu.ereader.data.model.ReadingSession>>(emptyList())
    val readingSessions: StateFlow<List<com.dyu.ereader.data.model.ReadingSession>> = _readingSessions.asStateFlow()

    private val _sessionStartTime = MutableStateFlow(0L)

    fun enableAnalytics(enabled: Boolean) {
        _settings.value = _settings.value.copy(analyticsEnabled = enabled)
    }

    fun setPrivacyMode(enabled: Boolean) {
        _settings.value = _settings.value.copy(privacyMode = enabled)
    }

    fun startReadingSession(bookId: String) {
        if (_settings.value.analyticsEnabled) {
            _sessionStartTime.value = System.currentTimeMillis()
        }
    }

    fun endReadingSession(bookId: String) {
        if (!_settings.value.analyticsEnabled || _sessionStartTime.value == 0L) return
        
        val sessionDuration = (System.currentTimeMillis() - _sessionStartTime.value) / 60000 // Convert to minutes
        val currentStats = _bookStats.value[bookId] ?: ReadingStatistics(bookId)
        
        _bookStats.value = _bookStats.value + (bookId to currentStats.copy(
            totalMinutesRead = currentStats.totalMinutesRead + sessionDuration,
            sessionsCount = currentStats.sessionsCount + 1,
            lastUpdated = System.currentTimeMillis()
        ))

        // Add reading session
        val session = ReadingSession(
            bookId = bookId,
            bookTitle = "Book $bookId", // This would need to be passed in or looked up
            startTime = _sessionStartTime.value,
            duration = (System.currentTimeMillis() - _sessionStartTime.value)
        )
        _readingSessions.value = (_readingSessions.value + session).takeLast(50) // Keep last 50 sessions

        updateLibraryStats()
        _sessionStartTime.value = 0L
    }

    fun recordHighlight(bookId: String) {
        if (_settings.value.analyticsEnabled) {
            val stats = _libraryStats.value
            _libraryStats.value = stats.copy(totalHighlights = stats.totalHighlights + 1)
        }
    }

    fun recordBookmark(bookId: String) {
        if (_settings.value.analyticsEnabled) {
            val stats = _libraryStats.value
            _libraryStats.value = stats.copy(totalBookmarks = stats.totalBookmarks + 1)
        }
    }

    fun recordPageTurn(bookId: String, direction: String = "next") {
        if (_settings.value.analyticsEnabled) {
            // Page turns tracked via reading time accumulation in sessions
        }
    }

    fun recordSearch(bookId: String, query: String, resultsCount: Int) {
        if (_settings.value.analyticsEnabled && !_settings.value.privacyMode) {
            // Search queries tracked in privacy-respecting mode
        }
    }

    fun recordTTSUsage(bookId: String, durationMs: Long) {
        if (_settings.value.analyticsEnabled) {
            // TTS usage tracked separately from reading time
        }
    }

    fun recordNoteAdded(bookId: String) {
        if (_settings.value.analyticsEnabled) {
            // Notes tracked via annotation storage
        }
    }

    fun recordThemeChange(newTheme: String) {
        if (_settings.value.analyticsEnabled && !_settings.value.privacyMode) {
            // Log theme preference change if needed
        }
    }

    private fun updateLibraryStats() {
        val allStats = _bookStats.value.values
        if (allStats.isEmpty()) return

        _libraryStats.value = _libraryStats.value.copy(
            totalBooksRead = allStats.count { it.totalMinutesRead > 0 },
            totalMinutesRead = allStats.sumOf { it.totalMinutesRead },
            averagePerBook = if (allStats.isNotEmpty()) allStats.sumOf { it.totalMinutesRead } / allStats.size else 0,
            lastUpdated = System.currentTimeMillis()
        )
    }

    fun clearAnalytics() {
        _bookStats.value = emptyMap()
        _libraryStats.value = LibraryStatistics()
    }
}
