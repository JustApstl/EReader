package com.dyu.ereader.data.repository.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dyu.ereader.BuildConfig
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface AppUpdateWorkerEntryPoint {
    fun appUpdateRepository(): AppUpdateRepository
    fun appUpdateNotificationRepository(): AppUpdateNotificationRepository
}

class AppUpdateWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            AppUpdateWorkerEntryPoint::class.java
        )
        val appUpdateRepository = entryPoint.appUpdateRepository()
        val appUpdateNotificationRepository = entryPoint.appUpdateNotificationRepository()
        return appUpdateRepository.checkForUpdates(
            currentVersionName = BuildConfig.VERSION_NAME,
            force = true
        ).fold(
            onSuccess = { snapshot ->
                val latestRelease = snapshot.latestRelease
                if (snapshot.updateAvailable && latestRelease != null) {
                    val lastNotified = appUpdateRepository.getLastNotifiedVersion()
                    if (lastNotified != latestRelease.versionName) {
                        appUpdateNotificationRepository.notifyUpdateAvailable(latestRelease)
                        appUpdateRepository.markUpdateNotified(latestRelease.versionName)
                    }
                }
                Result.success()
            },
            onFailure = { error ->
                val message = error.message.orEmpty()
                if (
                    message.contains("not configured", ignoreCase = true) ||
                    message.contains("No release data returned from GitHub", ignoreCase = true)
                ) {
                    Result.success()
                } else {
                    Result.retry()
                }
            }
        )
    }

    companion object {
        private const val WORK_NAME = "app_update_background_check"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<AppUpdateWorker>(12, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
