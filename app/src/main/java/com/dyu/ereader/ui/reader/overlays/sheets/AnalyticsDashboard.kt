package com.dyu.ereader.ui.reader.overlays.sheets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.QueryStats
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dyu.ereader.data.model.analytics.ReadingSession
import com.dyu.ereader.data.model.analytics.ReadingStatistics
import com.dyu.ereader.ui.reader.overlays.components.ReaderPanelScaffold
import com.dyu.ereader.ui.reader.overlays.components.ReaderSheetMetricCard
import com.dyu.ereader.ui.reader.overlays.components.ReaderSheetSection
import com.dyu.ereader.ui.reader.viewmodel.ReaderViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AnalyticsDashboard(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val readingStats by viewModel.readingStats.collectAsState(initial = ReadingStatistics(""))
    val readingSessions by viewModel.readingSessions.collectAsState(initial = emptyList())
    val libraryStats by viewModel.libraryStats.collectAsState()
    val isLoading by viewModel.isLoadingAnalytics.collectAsState()

    ReaderPanelScaffold(
        title = "Analytics",
        icon = Icons.Rounded.QueryStats,
        onDismiss = onDismiss,
        closeContentDescription = "Close Analytics",
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
        } else {
            ReaderSheetSection(
                title = "Reading Overview",
                icon = Icons.Rounded.Timer
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReaderSheetMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Timer,
                            label = "Total Time",
                            value = formatDuration(readingStats.totalMinutesRead * 60 * 1000)
                        )
                        ReaderSheetMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.LocalFireDepartment,
                            label = "Streak",
                            value = "${libraryStats.currentStreakDays}d"
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReaderSheetMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Rounded.MenuBook,
                            label = "Sessions",
                            value = readingStats.sessionsCount.toString()
                        )
                        ReaderSheetMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.DoneAll,
                            label = "Finished",
                            value = libraryStats.booksFinished.toString()
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ReaderSheetMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.History,
                            label = "Avg Session",
                            value = formatDuration(libraryStats.averageSessionDurationMs)
                        )
                        ReaderSheetMetricCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Rounded.Category,
                            label = "Top Format",
                            value = libraryStats.mostUsedFormat ?: "N/A"
                        )
                    }
                }
            }

            ReaderSheetSection(
                title = "Recent Activity",
                icon = Icons.Rounded.History
            ) {
                if (readingSessions.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.84f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Text(
                            text = "No recent sessions recorded yet.",
                            modifier = Modifier.padding(18.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 280.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(readingSessions) { session ->
                            ReadingSessionItem(session = session)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingSessionItem(session: ReadingSession) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    modifier = Modifier.size(38.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = session.bookTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = dateFormat.format(Date(session.startTime)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = formatDuration(session.duration),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> "${hours}h ${minutes % 60}m"
        minutes > 0 -> "${minutes}m ${seconds % 60}s"
        else -> "${seconds}s"
    }
}
