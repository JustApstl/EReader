package com.dyu.ereader.data.repository.update

import android.content.Context
import android.os.Build
import com.dyu.ereader.BuildConfig
import com.dyu.ereader.data.local.prefs.ReaderPreferencesStore
import com.dyu.ereader.data.model.update.AppReleaseInfo
import com.google.gson.JsonArray
import com.google.gson.Gson
import com.google.gson.JsonObject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesStore: ReaderPreferencesStore
) {
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun getCachedLatestRelease(): AppReleaseInfo? = preferencesStore.readCachedLatestRelease()

    suspend fun getLastCheckedAt(): Long? = preferencesStore.getUpdateLastCheckedAt()

    suspend fun dismissRelease(versionName: String) {
        preferencesStore.setDismissedUpdateVersion(versionName)
    }

    suspend fun getLastNotifiedVersion(): String? = preferencesStore.getLastNotifiedUpdateVersion()

    suspend fun markUpdateNotified(versionName: String) {
        preferencesStore.setLastNotifiedUpdateVersion(versionName)
    }

    suspend fun clearError() {
        preferencesStore.setUpdateLastCheckedAt(System.currentTimeMillis())
    }

    suspend fun shouldCheckForUpdates(now: Long = System.currentTimeMillis()): Boolean {
        val lastCheckedAt = preferencesStore.getUpdateLastCheckedAt() ?: return true
        val elapsed = now - lastCheckedAt
        return elapsed >= UPDATE_CHECK_INTERVAL_MS
    }

    suspend fun checkForUpdates(
        currentVersionName: String,
        force: Boolean
    ): Result<UpdateSnapshot> = withContext(Dispatchers.IO) {
        if (BuildConfig.GITHUB_RELEASE_OWNER.isBlank() || BuildConfig.GITHUB_RELEASE_REPO.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GitHub release repository is not configured"))
        }

        if (!force && !shouldCheckForUpdates()) {
            return@withContext Result.success(
                UpdateSnapshot(
                    latestRelease = preferencesStore.readCachedLatestRelease(),
                    updateAvailable = preferencesStore.readCachedLatestRelease()?.let {
                        compareVersionNames(it.versionName, currentVersionName) > 0
                    } ?: false,
                    dismissedVersion = preferencesStore.getDismissedUpdateVersion(),
                    lastCheckedAt = preferencesStore.getUpdateLastCheckedAt()
                )
            )
        }

        runCatching {
            val latest = fetchRelease(
                url = "https://api.github.com/repos/${BuildConfig.GITHUB_RELEASE_OWNER}/${BuildConfig.GITHUB_RELEASE_REPO}/releases/latest"
            ) ?: throw IllegalStateException("No release data returned from GitHub")

            val checkedAt = System.currentTimeMillis()
            preferencesStore.cacheLatestRelease(latest)
            preferencesStore.setUpdateLastCheckedAt(checkedAt)

            UpdateSnapshot(
                latestRelease = latest,
                updateAvailable = compareVersionNames(latest.versionName, currentVersionName) > 0,
                dismissedVersion = preferencesStore.getDismissedUpdateVersion(),
                lastCheckedAt = checkedAt
            )
        }
    }

    suspend fun fetchReleaseHistory(limit: Int = 8): Result<List<AppReleaseInfo>> = withContext(Dispatchers.IO) {
        if (BuildConfig.GITHUB_RELEASE_OWNER.isBlank() || BuildConfig.GITHUB_RELEASE_REPO.isBlank()) {
            return@withContext Result.failure(IllegalStateException("GitHub release repository is not configured"))
        }

        runCatching {
            fetchReleaseList(
                url = "https://api.github.com/repos/${BuildConfig.GITHUB_RELEASE_OWNER}/${BuildConfig.GITHUB_RELEASE_REPO}/releases?per_page=${limit.coerceIn(1, 20)}"
            )
        }
    }

    suspend fun prepareChangelogForInstalledVersion(currentVersionName: String): AppReleaseInfo? {
        val previousInstalledVersion = preferencesStore.getLastInstalledVersion()
        val alreadyShownVersion = preferencesStore.getLastShownChangelogVersion()
        preferencesStore.setLastInstalledVersion(currentVersionName)

        if (previousInstalledVersion.isNullOrBlank() || previousInstalledVersion == currentVersionName) {
            return null
        }
        if (alreadyShownVersion == currentVersionName) {
            return null
        }

        val cached = preferencesStore.readCachedLatestRelease()
        val release = when {
            cached?.versionName == currentVersionName -> cached
            else -> fetchReleaseForVersion(currentVersionName)
        }

        return release ?: AppReleaseInfo(
            versionName = currentVersionName,
            tagName = currentVersionName,
            title = "Updated to $currentVersionName",
            notes = "This version was installed successfully.",
            htmlUrl = buildReleasePageUrl(),
            downloadUrl = null,
            assetName = null,
            publishedAt = null
        )
    }

    suspend fun markChangelogShown(versionName: String) {
        preferencesStore.setLastShownChangelogVersion(versionName)
    }

    private suspend fun fetchReleaseForVersion(versionName: String): AppReleaseInfo? = withContext(Dispatchers.IO) {
        val normalized = normalizeVersionName(versionName)
        val candidateTags = listOf("v$normalized", normalized).distinct()
        candidateTags.firstNotNullOfOrNull { tag ->
            fetchRelease(
                url = "https://api.github.com/repos/${BuildConfig.GITHUB_RELEASE_OWNER}/${BuildConfig.GITHUB_RELEASE_REPO}/releases/tags/$tag"
            )
        }
    }

    private fun fetchRelease(url: String): AppReleaseInfo? {
        val json = fetchJson(url) ?: return null
        return json.toAppReleaseInfo()
    }

    private fun fetchReleaseList(url: String): List<AppReleaseInfo> {
        val json = fetchJsonArray(url) ?: return emptyList()
        return json.mapNotNull { element ->
            runCatching { element.asJsonObject.toAppReleaseInfo() }.getOrNull()
        }
    }

    private fun fetchJson(url: String): JsonObject? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "${context.packageName}/${BuildConfig.VERSION_NAME}")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return null
            }
            val body = response.body.string()
            if (body.isBlank()) {
                return null
            }
            return gson.fromJson(body, JsonObject::class.java)
        }
    }

    private fun fetchJsonArray(url: String): JsonArray? {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github+json")
            .header("X-GitHub-Api-Version", "2022-11-28")
            .header("User-Agent", "${context.packageName}/${BuildConfig.VERSION_NAME}")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return null
            }
            val body = response.body.string()
            if (body.isBlank()) {
                return null
            }
            return gson.fromJson(body, JsonArray::class.java)
        }
    }

    private fun JsonObject.toAppReleaseInfo(): AppReleaseInfo {
        val tagName = get("tag_name")?.asString.orEmpty()
        val versionName = normalizeVersionName(tagName.ifBlank { get("name")?.asString.orEmpty() })
        val notes = get("body")?.asString.orEmpty().trim()
        val title = get("name")?.asString?.takeIf { it.isNotBlank() }
            ?: "Version $versionName"
        val htmlUrl = get("html_url")?.asString.orEmpty().ifBlank { buildReleasePageUrl() }
        val assets = getAsJsonArray("assets")
        val apkAsset = pickBestApkAsset(assets)
        val assetName = apkAsset?.get("name")?.asString

        return AppReleaseInfo(
            versionName = versionName,
            tagName = tagName.ifBlank { versionName },
            title = title,
            notes = notes.ifBlank { "Release notes are not available for this version yet." },
            htmlUrl = htmlUrl,
            downloadUrl = apkAsset?.get("browser_download_url")?.asString,
            assetName = assetName,
            assetLabel = assetName?.toReadableAssetLabel(),
            publishedAt = get("published_at")?.asString?.toEpochMillisOrNull()
        )
    }

    private fun pickBestApkAsset(assets: JsonArray?): JsonObject? {
        if (assets == null || assets.size() == 0) return null
        val candidates = assets.mapNotNull { element ->
            val asset = element.asJsonObject
            val name = asset.get("name")?.asString.orEmpty()
            if (!name.lowercase().endsWith(".apk")) {
                return@mapNotNull null
            }
            asset to scoreApkAsset(name)
        }
        return candidates.maxByOrNull { it.second }?.first
    }

    private fun scoreApkAsset(name: String): Int {
        val normalized = name.lowercase()
        var score = 0

        if ("universal" in normalized) score += 90
        if ("release" in normalized) score += 8
        if ("debug" in normalized) score -= 20
        if ("x86_64" in normalized || "x86-64" in normalized) score += if (supportsAbi("x86_64")) 80 else -8
        if ("x86" in normalized && "x86_64" !in normalized) score += if (supportsAbi("x86")) 70 else -8
        if ("arm64-v8a" in normalized || "arm64" in normalized || "aarch64" in normalized) {
            score += if (supportsAbi("arm64-v8a")) 100 else -10
        }
        if ("armeabi-v7a" in normalized || "armeabi" in normalized || "armv7" in normalized) {
            score += if (supportsAbi("armeabi-v7a")) 85 else -10
        }
        if ("apk" in normalized) score += 2

        if (normalized.none { it.isLetterOrDigit() }) {
            score -= 4
        }

        return score
    }

    private fun supportsAbi(abi: String): Boolean =
        Build.SUPPORTED_ABIS.any { supported -> supported.equals(abi, ignoreCase = true) }

    private fun String.toReadableAssetLabel(): String =
        removeSuffix(".apk")
            .replace('-', ' ')
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()
            .replaceFirstChar { char -> char.uppercase() }

    private fun buildReleasePageUrl(): String =
        "https://github.com/${BuildConfig.GITHUB_RELEASE_OWNER}/${BuildConfig.GITHUB_RELEASE_REPO}/releases"

    private fun normalizeVersionName(value: String): String =
        value.trim()
            .removePrefix("refs/tags/")
            .removePrefix("release/")
            .removePrefix("v")
            .ifBlank { BuildConfig.VERSION_NAME }

    private fun String.toEpochMillisOrNull(): Long? =
        runCatching { Instant.parse(this).toEpochMilli() }.getOrNull()

    private fun compareVersionNames(left: String, right: String): Int {
        val leftParts = tokenizeVersion(left)
        val rightParts = tokenizeVersion(right)
        val max = maxOf(leftParts.size, rightParts.size)
        for (index in 0 until max) {
            val leftValue = leftParts.getOrElse(index) { 0 }
            val rightValue = rightParts.getOrElse(index) { 0 }
            if (leftValue != rightValue) {
                return leftValue.compareTo(rightValue)
            }
        }
        return 0
    }

    private fun tokenizeVersion(value: String): List<Int> =
        normalizeVersionName(value)
            .split(Regex("[^0-9]+"))
            .filter { it.isNotBlank() }
            .mapNotNull { it.toIntOrNull() }
            .ifEmpty { listOf(0) }

    data class UpdateSnapshot(
        val latestRelease: AppReleaseInfo?,
        val updateAvailable: Boolean,
        val dismissedVersion: String?,
        val lastCheckedAt: Long?
    )

    companion object {
        private const val UPDATE_CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L
    }
}
