package com.dyu.ereader.data.repository.update

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.dyu.ereader.R
import com.dyu.ereader.data.local.prefs.ReaderPreferencesStore
import com.dyu.ereader.data.model.update.AppReleaseInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateNotificationRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun notifyUpdateAvailable(release: AppReleaseInfo) {
        if (!canPostNotifications() || !areUpdateNotificationsEnabled()) {
            return
        }
        ensureChannel()
        NotificationManagerCompat.from(context).notify(
            UPDATE_NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("New update available")
                .setContentText("Version ${release.versionName} is ready to install.")
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        buildString {
                            append("Version ")
                            append(release.versionName)
                            append(" is available.")
                            release.title.takeIf { it.isNotBlank() }?.let { title ->
                                append("\n")
                                append(title)
                            }
                            release.assetLabel?.takeIf { it.isNotBlank() }?.let { label ->
                                append("\nBest match: ")
                                append(label)
                            }
                        }
                    )
                )
                .setContentIntent(releasePendingIntent(release))
                .addAction(
                    0,
                    "Download",
                    releasePendingIntent(release)
                )
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
                .build()
        )
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "App updates",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications about new app updates and releases."
        }
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun areUpdateNotificationsEnabled(): Boolean {
        val preferencesStore = ReaderPreferencesStore(context)
        return runBlocking {
            preferencesStore.notificationsEnabledFlow.first() &&
                preferencesStore.updateNotificationsEnabledFlow.first()
        }
    }

    private fun releasePendingIntent(release: AppReleaseInfo): PendingIntent {
        val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(release.downloadUrl ?: release.htmlUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            UPDATE_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val CHANNEL_ID = "app_updates"
        private const val UPDATE_NOTIFICATION_ID = 5101
    }
}
