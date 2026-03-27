package com.dyu.ereader.ui.reader.overlays.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dyu.ereader.data.model.export.ExportFormat
import com.dyu.ereader.ui.reader.overlays.components.ReaderPanelScaffold
import com.dyu.ereader.ui.reader.overlays.components.ReaderSheetSection
import com.dyu.ereader.ui.reader.chrome.readerPanelSectionColor
import com.dyu.ereader.ui.reader.settings.components.ToggleRow
import com.dyu.ereader.ui.reader.viewmodel.ReaderViewModel

@Composable
fun ExportDialog(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    var selectedFormat by remember { mutableStateOf(ExportFormat.PDF) }
    var includeAnnotations by remember { mutableStateOf(true) }
    var includeBookmarks by remember { mutableStateOf(true) }
    val isExporting by viewModel.isExporting.collectAsState()

    ReaderPanelScaffold(
        title = "Export Highlights",
        icon = Icons.Rounded.IosShare,
        onDismiss = onDismiss,
        closeContentDescription = "Close Export",
        modifier = modifier.verticalScroll(rememberScrollState())
    ) {
        ReaderSheetSection(
            title = "Format",
            icon = Icons.Rounded.TaskAlt
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExportFormat.entries.forEach { format ->
                    FormatTile(
                        format = format,
                        isSelected = selectedFormat == format,
                        onClick = { selectedFormat = format },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        ReaderSheetSection(
            title = "Include Content",
            icon = Icons.Rounded.TaskAlt
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ToggleRow(
                    title = "Annotations & Highlights",
                    desc = "Include saved highlights and annotation text.",
                    checked = includeAnnotations,
                    icon = Icons.Rounded.Draw,
                    onToggle = { includeAnnotations = it }
                )
                ToggleRow(
                    title = "Saved Bookmarks",
                    desc = "Include bookmarks saved for this book.",
                    checked = includeBookmarks,
                    icon = Icons.Rounded.Bookmark,
                    onToggle = { includeBookmarks = it }
                )
            }
        }

        if (isExporting) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Text("Preparing your export...", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick = onDismiss,
                enabled = !isExporting,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    viewModel.exportBook(
                        format = selectedFormat,
                        includeAnnotations = includeAnnotations,
                        includeBookmarks = includeBookmarks
                    )
                    onDismiss()
                },
                enabled = !isExporting,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Export", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FormatTile(
    format: ExportFormat,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = if (format == ExportFormat.PDF) Icons.Rounded.PictureAsPdf else Icons.Rounded.Description,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = format.displayLabel(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(min = 0.dp)
            )
        }
    }
}

private fun ExportFormat.displayLabel(): String = when (this) {
    ExportFormat.PDF -> "PDF"
    ExportFormat.MARKDOWN -> "Markdown"
    ExportFormat.JSON -> "JSON"
    ExportFormat.EMAIL -> "Email"
}
