package com.dyu.ereader.data.repository.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReadingReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            ReadingReminderRepository(context).rescheduleFromPreferences()
        }
    }
}
