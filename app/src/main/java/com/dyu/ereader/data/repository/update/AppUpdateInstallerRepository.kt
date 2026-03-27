package com.dyu.ereader.data.repository.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.dyu.ereader.data.model.update.AppReleaseInfo
import com.dyu.ereader.data.model.update.PendingAppInstall
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateInstallerRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val client = OkHttpClient()

    suspend fun prepareUpdateInstall(release: AppReleaseInfo): Result<PendingAppInstall> = withContext(Dispatchers.IO) {
        runCatching {
            val downloadUrl = release.downloadUrl
                ?: throw IllegalStateException("This release does not include a downloadable APK.")
            val fileName = sanitizeApkFileName(release)
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            updatesDir.listFiles()?.forEach { existing ->
                if (existing.name != fileName && existing.extension.equals("apk", ignoreCase = true)) {
                    existing.delete()
                }
            }
            val targetFile = File(updatesDir, fileName)
            val request = Request.Builder()
                .url(downloadUrl)
                .header("Accept", "application/octet-stream")
                .header("User-Agent", "${context.packageName}/updater")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IllegalStateException("Unable to download the update package right now.")
                }
                val body = response.body
                targetFile.outputStream().use { output ->
                    body.byteStream().copyTo(output)
                }
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                targetFile
            )

            PendingAppInstall(
                versionName = release.versionName,
                fileName = targetFile.name,
                uriString = apkUri.toString()
            )
        }
    }

    fun canInstallPackages(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    fun createUnknownAppsSettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun createInstallIntent(install: PendingAppInstall): Intent =
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(install.uriString), APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

    private fun sanitizeApkFileName(release: AppReleaseInfo): String {
        val rawName = release.assetName?.takeIf { it.endsWith(".apk", ignoreCase = true) }
            ?: "ereader-${release.versionName}.apk"
        return rawName.replace(Regex("[^A-Za-z0-9._-]"), "_")
    }

    private companion object {
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
