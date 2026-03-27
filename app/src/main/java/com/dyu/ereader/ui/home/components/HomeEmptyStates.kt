package com.dyu.ereader.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dyu.ereader.ui.components.surfaces.SectionSurface
import com.dyu.ereader.core.logging.AppLogger

@Composable
fun EmptyPermissionState(onGrantAccess: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(40.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            androidx.compose.material3.Icon(Icons.Rounded.FolderOpen, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Text("Access Required", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Button(onClick = onGrantAccess) { Text("Select Library Folder") }
        }
    }
}

@Composable
fun EmptyLibraryState(onRefresh: () -> Unit, onChangeFolder: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("No books found", color = MaterialTheme.colorScheme.onSurface)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRefresh) { Text("Scan Again") }
                OutlinedButton(onClick = onChangeFolder) { Text("Change Folder") }
            }
        }
    }
}

@Composable
fun LibraryLoadingState() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Loading library...",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Scanning your selected folder for books.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun LogsArea(
    modifier: Modifier = Modifier,
    liquidGlassEnabled: Boolean = false
) {
    val logs by AppLogger.logs.collectAsState()
    val hasErrorLogs = logs.any { it.isErrorLikeLog() }
    val sectionColor = if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
    val sectionBorder = BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (liquidGlassEnabled) 0.2f else 0.3f)
    )
    SectionSurface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(28.dp),
        color = sectionColor,
        border = sectionBorder,
        contentPadding = PaddingValues(8.dp)
    ) {
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                LogDescriptionCard(
                    title = "System log is clear",
                    description = "There are no errors or events yet. When something needs attention, system messages will appear here."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!hasErrorLogs) {
                    item {
                        LogDescriptionCard(
                            title = "No system errors detected",
                            description = "Recent activity is shown below. If the app runs into a problem, error details will appear here."
                        )
                    }
                }
                items(logs) { log ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                item { Spacer(Modifier.padding(2.dp)) }
            }
        }
    }
}

@Composable
private fun LogDescriptionCard(
    title: String,
    description: String
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun String.isErrorLikeLog(): Boolean {
    val text = lowercase()
    return listOf(
        "error",
        "exception",
        "failed",
        "fatal",
        "crash",
        "stack trace",
        "stacktrace"
    ).any(text::contains)
}
