package com.dyu.ereader.ui.browse.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.browse.BrowseDownloadState
import com.dyu.ereader.data.model.browse.BrowseDownloadTask
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.surfaces.SectionSurface

@Composable
internal fun DownloadQueuePanel(
    tasks: List<BrowseDownloadTask>,
    onCancel: (BrowseDownloadTask) -> Unit,
    onRetry: (BrowseDownloadTask) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (tasks.isEmpty()) return

    SectionSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "Downloads",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${tasks.size}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    AppChromeIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = "Close downloads",
                        onClick = onDismiss,
                        size = 28.dp,
                        iconSize = 15.dp
                    )
                }
            }

            tasks.take(3).forEach { task ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                        Surface(
                            modifier = Modifier.size(40.dp, 56.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow
                        ) {
                            BrowseCoverImage(
                                coverUrl = task.coverUrl,
                                title = task.title,
                                author = task.author,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = when (task.state) {
                                    BrowseDownloadState.QUEUED -> "Queued"
                                    BrowseDownloadState.DOWNLOADING -> "Downloading ${task.progress.toInt()}%"
                                    BrowseDownloadState.PAUSED -> "Paused ${task.progress.toInt()}%"
                                    BrowseDownloadState.COMPLETED -> "Completed"
                                    BrowseDownloadState.FAILED -> "Failed"
                                    BrowseDownloadState.CANCELED -> "Canceled"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        when (task.state) {
                            BrowseDownloadState.QUEUED,
                            BrowseDownloadState.DOWNLOADING -> {
                                TextButton(onClick = { onCancel(task) }) { Text("Cancel") }
                            }
                            BrowseDownloadState.PAUSED,
                            BrowseDownloadState.FAILED,
                            BrowseDownloadState.CANCELED -> {
                                TextButton(onClick = { onRetry(task) }) { Text("Retry") }
                            }
                            BrowseDownloadState.COMPLETED -> Unit
                        }
                        }
                    }
                    if (task.state == BrowseDownloadState.DOWNLOADING) {
                        LinearProgressIndicator(
                            progress = { (task.progress / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
        }
    }
}
