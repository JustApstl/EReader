package com.dyu.ereader.core.logging

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object AppLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs = _logs.asStateFlow()

    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun log(message: String, throwable: Throwable? = null) {
        val timestamp = LocalDateTime.now().format(formatter)
        val logEntry = "[$timestamp] $message" + (throwable?.let { "\n${it.stackTraceToString()}" } ?: "")
        
        val currentLogs = _logs.value.toMutableList()
        currentLogs.add(0, logEntry)
        if (currentLogs.size > 100) {
            currentLogs.removeAt(currentLogs.size - 1)
        }
        _logs.value = currentLogs
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun refresh() {
        _logs.value = _logs.value.toList()
    }
}
