package com.dyu.ereader.ui.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Downloading
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dyu.ereader.data.model.browse.BrowseDownloadState
import com.dyu.ereader.data.model.browse.BrowseDownloadTask
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.menus.AppDropdownMenu
import com.dyu.ereader.ui.components.menus.AppDropdownMenuItem
import com.dyu.ereader.ui.components.surfaces.SectionSurface

@Composable
internal fun BrowseCatalogHeader(
    title: String,
    iconUrl: String?,
    downloadQueue: List<BrowseDownloadTask>,
    showDownloads: Boolean,
    liquidGlassEnabled: Boolean,
    onBack: () -> Unit,
    onToggleDownloads: () -> Unit,
    onDismissDownloads: () -> Unit,
    onDownloadTaskAction: (BrowseDownloadTask) -> Unit
) {
    SectionSurface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AppChromeIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
                liquidGlassEnabled = liquidGlassEnabled
            )
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!iconUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = iconUrl,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp).clip(androidx.compose.foundation.shape.CircleShape),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        Icons.Rounded.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (downloadQueue.isEmpty()) {
                            "Browse feed and discover your next read."
                        } else {
                            "${downloadQueue.size} download${if (downloadQueue.size == 1) "" else "s"} in progress or queued."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            BrowseDownloadsButton(
                downloadQueue = downloadQueue,
                expanded = showDownloads,
                liquidGlassEnabled = liquidGlassEnabled,
                onToggle = onToggleDownloads,
                onDismiss = onDismissDownloads,
                onTaskAction = onDownloadTaskAction
            )
        }
    }
}

@Composable
private fun BrowseDownloadsButton(
    downloadQueue: List<BrowseDownloadTask>,
    expanded: Boolean,
    liquidGlassEnabled: Boolean,
    onToggle: () -> Unit,
    onDismiss: () -> Unit,
    onTaskAction: (BrowseDownloadTask) -> Unit
) {
    Box {
        BadgedBox(
            badge = {
                if (downloadQueue.isNotEmpty()) {
                    Badge {
                        Text(downloadQueue.size.toString())
                    }
                }
            }
        ) {
            AppChromeIconButton(
                icon = Icons.Rounded.Download,
                contentDescription = "Downloads",
                onClick = onToggle,
                selected = downloadQueue.isNotEmpty() || expanded,
                liquidGlassEnabled = liquidGlassEnabled,
                size = 44.dp
            )
        }
        AppDropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            title = "Downloads",
            liquidGlassEnabled = liquidGlassEnabled
        ) {
            if (downloadQueue.isEmpty()) {
                Text(
                    text = "No downloads yet.",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                downloadQueue.take(6).forEach { task ->
                    val (statusLabel, statusIcon) = when (task.state) {
                        BrowseDownloadState.QUEUED -> "Queued" to Icons.Rounded.Schedule
                        BrowseDownloadState.DOWNLOADING -> "Downloading" to Icons.Rounded.Downloading
                        BrowseDownloadState.PAUSED -> "Paused" to Icons.Rounded.PauseCircle
                        BrowseDownloadState.COMPLETED -> "Completed" to Icons.Rounded.CheckCircle
                        BrowseDownloadState.FAILED -> "Failed" to Icons.Rounded.Error
                        BrowseDownloadState.CANCELED -> "Canceled" to Icons.Rounded.Cancel
                    }
                    val trailingIcon = when (task.state) {
                        BrowseDownloadState.QUEUED -> Icons.Rounded.Close
                        BrowseDownloadState.DOWNLOADING -> Icons.Rounded.Pause
                        BrowseDownloadState.PAUSED,
                        BrowseDownloadState.FAILED,
                        BrowseDownloadState.CANCELED -> Icons.Rounded.PlayArrow
                        BrowseDownloadState.COMPLETED -> null
                    }
                    val supportingText = buildString {
                        val progressText = formatDownloadProgress(task)
                        if (progressText.isNotBlank()) append(progressText)
                        task.speedBytesPerSecond?.takeIf { it > 0L }?.let { speed ->
                            if (isNotBlank()) append(" • ")
                            append("${formatBytes(speed)}/s")
                        }
                        task.error?.takeIf { it.isNotBlank() }?.let { errorMessage ->
                            if (isNotBlank()) append(" • ")
                            append(errorMessage)
                        }
                    }.ifBlank { null }
                    AppDropdownMenuItem(
                        label = task.title,
                        icon = statusIcon,
                        supportingText = supportingText,
                        badgeText = statusLabel,
                        trailingIcon = trailingIcon,
                        enableMarquee = true,
                        onClick = { onTaskAction(task) }
                    )
                }
                if (downloadQueue.size > 6) {
                    Text(
                        text = "+${downloadQueue.size - 6} more",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
