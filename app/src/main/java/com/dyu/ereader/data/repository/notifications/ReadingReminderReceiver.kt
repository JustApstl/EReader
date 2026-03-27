package com.dyu.ereader.data.repository.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReadingReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        ReadingReminderRepository(context).showScheduledReminder()
    }
}
