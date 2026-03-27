package com.dyu.ereader.ui.browse.components

import com.dyu.ereader.data.model.browse.BrowseDownloadState
import com.dyu.ereader.data.model.browse.BrowseDownloadTask
import java.util.Locale

internal fun formatDownloadProgress(task: BrowseDownloadTask): String {
    return when (task.state) {
        BrowseDownloadState.DOWNLOADING,
        BrowseDownloadState.PAUSED -> {
            val downloaded = task.downloadedBytes.takeIf { it > 0L }?.let(::formatBytes)
            val total = task.totalBytes?.let(::formatBytes)
            listOfNotNull(
                "${task.progress.toInt()}%",
                downloaded,
                total?.let { "/ $it" }
            ).joinToString(" ").ifBlank { "${task.progress.toInt()}%" }
        }
        else -> task.author.takeIf {
            it.isNotBlank() && !it.equals("Unknown Author", ignoreCase = true)
        }.orEmpty()
    }
}

internal fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${value.toInt()} ${units[unitIndex]}"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[unitIndex])
    }
}
