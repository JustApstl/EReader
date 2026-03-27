package com.dyu.ereader.data.repository.analytics

import com.dyu.ereader.data.model.analytics.AnalyticsSettings
import com.dyu.ereader.data.model.analytics.LibraryStatistics
import com.dyu.ereader.data.model.analytics.ReadingStatistics
import com.dyu.ereader.data.model.analytics.ReadingSession
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

    private val _readingSessions = MutableStateFlow<List<com.dyu.ereader.data.model.analytics.ReadingSession>>(emptyList())
    val readingSessions: StateFlow<List<com.dyu.ereader.data.model.analytics.ReadingSession>> = _readingSessions.asStateFlow()

    private val _sessionStartTime = MutableStateFlow(0L)
    private var currentSessionTitle: String = ""
    private var currentSessionFormat: String? = null

    fun enableAnalytics(enabled: Boolean) {
        _settings.value = _settings.value.copy(analyticsEnabled = enabled)
    }

    fun setPrivacyMode(enabled: Boolean) {
        _settings.value = _settings.value.copy(privacyMode = enabled)
    }

    fun startReadingSession(bookId: String, bookTitle: String = "", format: String? = null) {
        if (_settings.value.analyticsEnabled) {
            _sessionStartTime.value = System.currentTimeMillis()
            currentSessionTitle = bookTitle
            currentSessionFormat = format
        }
    }

    fun endReadingSession(bookId: String) {
        if (!_settings.value.analyticsEnabled || _sessionStartTime.value == 0L) return
        
        val sessionDuration = (System.currentTimeMillis() - _sessionStartTime.value) / 60000 // Convert to minutes
        val currentStats = _bookStats.value[bookId] ?: ReadingStatistics(bookId)
        val updatedSessions = currentStats.sessionsCount + 1
        val updatedTotalMinutes = currentStats.totalMinutesRead + sessionDuration
        val avgSession = if (updatedSessions > 0) updatedTotalMinutes / updatedSessions else 0L
        
        _bookStats.value = _bookStats.value + (bookId to currentStats.copy(
            totalMinutesRead = updatedTotalMinutes,
            sessionsCount = updatedSessions,
            averageSessionDuration = avgSession,
            lastUpdated = System.currentTimeMillis()
        ))

        // Add reading session
        val session = ReadingSession(
            bookId = bookId,
            bookTitle = if (currentSessionTitle.isNotBlank()) currentSessionTitle else "Book $bookId",
            format = currentSessionFormat,
            startTime = _sessionStartTime.value,
            duration = (System.currentTimeMillis() - _sessionStartTime.value)
        )
        _readingSessions.value = (_readingSessions.value + session).takeLast(50) // Keep last 50 sessions

        updateLibraryStats()
        _sessionStartTime.value = 0L
        currentSessionTitle = ""
        currentSessionFormat = null
    }

    fun recordHighlight(bookId: String) {
        if (_settings.value.analyticsEnabled) {
            val stats = _libraryStats.value
            _libraryStats.value = stats.copy(totalHighlights = stats.totalHighlights + 1)

            val current = _bookStats.value[bookId] ?: ReadingStatistics(bookId)
            _bookStats.value = _bookStats.value + (bookId to current.copy(
                highlightCount = current.highlightCount + 1,
                lastUpdated = System.currentTimeMillis()
            ))
        }
    }

    fun recordBookmark(bookId: String) {
        if (_settings.value.analyticsEnabled) {
            val stats = _libraryStats.value
            _libraryStats.value = stats.copy(totalBookmarks = stats.totalBookmarks + 1)

            val current = _bookStats.value[bookId] ?: ReadingStatistics(bookId)
            _bookStats.value = _bookStats.value + (bookId to current.copy(
                bookmarkCount = current.bookmarkCount + 1,
                lastUpdated = System.currentTimeMillis()
            ))
        }
    }

    fun recordPageTurn(bookId: String, direction: String = "next") {
        if (_settings.value.analyticsEnabled) {
            val current = _bookStats.value[bookId] ?: ReadingStatistics(bookId)
            _bookStats.value = _bookStats.value + (bookId to current.copy(
                pagesTurned = current.pagesTurned + 1,
                lastUpdated = System.currentTimeMillis()
            ))
        }
    }

    fun recordSearch(bookId: String, query: String, resultsCount: Int) {
        if (_settings.value.analyticsEnabled) {
            val current = _bookStats.value[bookId] ?: ReadingStatistics(bookId)
            _bookStats.value = _bookStats.value + (bookId to current.copy(
                searchCount = current.searchCount + 1,
                lastUpdated = System.currentTimeMillis()
            ))
        }
    }

    fun recordTTSUsage(bookId: String, durationMs: Long) {
        if (_settings.value.analyticsEnabled) {
            val current = _bookStats.value[bookId] ?: ReadingStatistics(bookId)
            _bookStats.value = _bookStats.value + (bookId to current.copy(
                ttsUsageMs = current.ttsUsageMs + durationMs,
                lastUpdated = System.currentTimeMillis()
            ))
        }
    }

    fun recordBookFinished(bookId: String, format: String? = null) {
        if (_settings.value.analyticsEnabled) {
            val current = _bookStats.value[bookId] ?: ReadingStatistics(bookId)
            _bookStats.value = _bookStats.value + (bookId to current.copy(
                completed = true,
                preferredFormat = format ?: current.preferredFormat,
                lastUpdated = System.currentTimeMillis()
            ))
            updateLibraryStats()
        }
    }

    fun recordNoteAdded(bookId: String) {
        if (_settings.value.analyticsEnabled) {
            val current = _bookStats.value[bookId] ?: ReadingStatistics(bookId)
            _bookStats.value = _bookStats.value + (bookId to current.copy(
                noteCount = current.noteCount + 1,
                lastUpdated = System.currentTimeMillis()
            ))
        }
    }

    fun recordThemeChange(newTheme: String) {
        if (_settings.value.analyticsEnabled && !_settings.value.privacyMode) {
            // Log theme preference change if needed
        }
    }

    private fun updateLibraryStats() {
        val allStats = _bookStats.value.values
        val sessions = _readingSessions.value
        if (allStats.isEmpty() && sessions.isEmpty()) return

        val totalMinutes = allStats.sumOf { it.totalMinutesRead }
        val averagePerBook = if (allStats.isNotEmpty()) totalMinutes / allStats.size else 0L
        val averageSessionDurationMs = if (sessions.isNotEmpty()) {
            sessions.sumOf { it.duration } / sessions.size
        } else {
            0L
        }
        val currentStreakDays = calculateCurrentStreakDays(sessions)
        val mostUsedFormat = sessions
            .mapNotNull { it.format?.trim()?.takeIf(String::isNotBlank) }
            .groupingBy { it.uppercase() }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key

        _libraryStats.value = _libraryStats.value.copy(
            totalBooksRead = allStats.count { it.totalMinutesRead > 0 },
            totalMinutesRead = totalMinutes,
            averagePerBook = averagePerBook,
            averageSessionDurationMs = averageSessionDurationMs,
            currentStreakDays = currentStreakDays,
            booksFinished = allStats.count { it.completed },
            mostUsedFormat = mostUsedFormat,
            lastUpdated = System.currentTimeMillis()
        )
    }

    fun clearAnalytics() {
        _bookStats.value = emptyMap()
        _libraryStats.value = LibraryStatistics()
        _readingSessions.value = emptyList()
    }

    fun exportSnapshot(): AnalyticsBackupPayload {
        return AnalyticsBackupPayload(
            settings = _settings.value,
            libraryStatistics = _libraryStats.value,
            bookStatistics = _bookStats.value,
            readingSessions = _readingSessions.value
        )
    }

    fun importSnapshot(snapshot: AnalyticsBackupPayload) {
        _settings.value = snapshot.settings
        _libraryStats.value = snapshot.libraryStatistics
        _bookStats.value = snapshot.bookStatistics
        _readingSessions.value = snapshot.readingSessions
    }

    private fun calculateCurrentStreakDays(sessions: List<ReadingSession>): Int {
        if (sessions.isEmpty()) return 0
        val days = sessions
            .map { it.startTime / DAY_MS }
            .distinct()
            .sortedDescending()
        if (days.isEmpty()) return 0
        var streak = 1
        var expected = days.first()
        for (index in 1 until days.size) {
            expected -= 1
            if (days[index] == expected) {
                streak += 1
            } else if (days[index] < expected) {
                break
            }
        }
        return streak
    }

    private companion object {
        const val DAY_MS = 24L * 60L * 60L * 1000L
    }
}

data class AnalyticsBackupPayload(
    val settings: AnalyticsSettings = AnalyticsSettings(),
    val libraryStatistics: LibraryStatistics = LibraryStatistics(),
    val bookStatistics: Map<String, ReadingStatistics> = emptyMap(),
    val readingSessions: List<ReadingSession> = emptyList()
)
