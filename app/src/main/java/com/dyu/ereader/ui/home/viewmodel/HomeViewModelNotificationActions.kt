package com.dyu.ereader.ui.home.viewmodel

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

internal fun HomeViewModel.handleNotificationsEnabledChanged(enabled: Boolean) = viewModelScope.launch {
    prefsStore.setNotificationsEnabled(enabled)
    notificationRepo.setNotificationsEnabled(enabled)
    val reminderEnabled = prefsStore.readingReminderEnabledFlow.first()
    val hour = prefsStore.readingReminderHourFlow.first()
    val minute = prefsStore.readingReminderMinuteFlow.first()
    notificationRepo.updateReminder(
        enabled = enabled && reminderEnabled,
        hour = hour,
        minute = minute
    )
}

internal fun HomeViewModel.handleReadingReminderEnabledChanged(enabled: Boolean) = viewModelScope.launch {
    prefsStore.setReadingReminderEnabled(enabled)
    val notificationsEnabled = prefsStore.notificationsEnabledFlow.first()
    val hour = prefsStore.readingReminderHourFlow.first()
    val minute = prefsStore.readingReminderMinuteFlow.first()
    notificationRepo.updateReminder(
        enabled = notificationsEnabled && enabled,
        hour = hour,
        minute = minute
    )
}

internal fun HomeViewModel.handleReadingReminderTimeChanged(hour: Int, minute: Int) = viewModelScope.launch {
    prefsStore.setReadingReminderTime(hour, minute)
    val notificationsEnabled = prefsStore.notificationsEnabledFlow.first()
    val reminderEnabled = prefsStore.readingReminderEnabledFlow.first()
    notificationRepo.updateReminder(
        enabled = notificationsEnabled && reminderEnabled,
        hour = hour,
        minute = minute
    )
}

internal fun HomeViewModel.handleSendTestNotification() {
    notificationRepo.sendTestNotification()
}

internal fun HomeViewModel.handleRecordLocalBackupExport() = viewModelScope.launch {
    prefsStore.setLastLocalBackupExportAt(System.currentTimeMillis())
}

internal fun HomeViewModel.handleRecordLocalBackupImport() = viewModelScope.launch {
    prefsStore.setLastLocalBackupImportAt(System.currentTimeMillis())
}
