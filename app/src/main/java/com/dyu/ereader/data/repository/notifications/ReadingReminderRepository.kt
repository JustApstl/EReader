package com.dyu.ereader.data.repository.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dyu.ereader.R
import com.dyu.ereader.data.local.prefs.ReaderPreferencesStore
import com.dyu.ereader.ui.app.MainActivity
import java.util.Calendar
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

@Singleton
class ReadingReminderRepository @Inject constructor(
    @param:ApplicationContext
    private val context: Context
) {
    fun setNotificationsEnabled(enabled: Boolean) {
        if (!enabled) {
            cancelReminder()
        }
        ensureChannel()
    }

    fun updateReminder(enabled: Boolean, hour: Int, minute: Int) {
        ensureChannel()
        if (!enabled) {
            cancelReminder()
            return
        }

        val triggerAtMillis = nextTriggerAtMillis(hour, minute)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            AlarmManager.INTERVAL_DAY,
            reminderPendingIntent()
        )
    }

    fun cancelReminder() {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        alarmManager.cancel(reminderPendingIntent())
    }

    fun sendTestNotification() {
        ensureChannel()
        NotificationManagerCompat.from(context).notify(
            TEST_NOTIFICATION_ID,
            buildNotification(
                title = "Reading reminder",
                message = "Pick up where you left off and continue your current book."
            )
        )
    }

    fun showScheduledReminder() {
        ensureChannel()
        NotificationManagerCompat.from(context).notify(
            REMINDER_NOTIFICATION_ID,
            buildNotification(
                title = "Come back to reading",
                message = "Your library is waiting. Resume where you left off."
            )
        )
    }

    fun rescheduleFromPreferences() {
        val preferencesStore = ReaderPreferencesStore(context)
        runBlocking {
            val notificationsEnabled = preferencesStore.notificationsEnabledFlow.first()
            val reminderEnabled = preferencesStore.readingReminderEnabledFlow.first()
            val hour = preferencesStore.readingReminderHourFlow.first()
            val minute = preferencesStore.readingReminderMinuteFlow.first()
            setNotificationsEnabled(notificationsEnabled)
            updateReminder(
                enabled = notificationsEnabled && reminderEnabled,
                hour = hour,
                minute = minute
            )
        }
    }

    private fun buildNotification(title: String, message: String) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setContentIntent(contentPendingIntent())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

    private fun contentPendingIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            2001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun reminderPendingIntent(): PendingIntent {
        val intent = Intent(context, ReadingReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reading reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders to return and continue reading."
        }
        manager.createNotificationChannel(channel)
    }

    private fun nextTriggerAtMillis(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    companion object {
        const val CHANNEL_ID = "reading_reminders"
        private const val REMINDER_NOTIFICATION_ID = 4101
        private const val TEST_NOTIFICATION_ID = 4102
    }
}
