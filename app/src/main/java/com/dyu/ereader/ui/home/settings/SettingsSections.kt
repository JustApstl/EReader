package com.dyu.ereader.ui.home.settings

import android.Manifest
import android.os.Build
import android.os.StatFs
import android.text.format.DateFormat
import android.text.format.Formatter
import android.net.Uri
import android.os.SystemClock
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.core.content.ContextCompat
import com.dyu.ereader.data.model.app.AppTheme
import com.dyu.ereader.data.model.reader.ReaderControl
import com.dyu.ereader.data.model.update.AppUpdateUiState
import com.dyu.ereader.ui.components.inputs.appSegmentedButtonColors
import com.dyu.ereader.ui.home.state.HomeUiState
import java.util.Date

@Composable
internal fun ReaderControlsSection(
    uiState: HomeUiState,
    liquidGlassEnabled: Boolean,
    orderedReaderControls: List<ReaderControl>,
    hasReaderControlChanges: Boolean,
    onResetOrder: () -> Unit,
    onOrderChanged: (List<ReaderControl>) -> Unit,
    onDragActiveChange: (Boolean) -> Unit,
    events: SettingsEvents
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val itemHeightPx = with(density) { 72.dp.toPx() }
    val itemSpacingPx = with(density) { 6.dp.toPx() }
    val itemFullHeightPx = itemHeightPx + itemSpacingPx
    var previewControls by remember { mutableStateOf(mergeReaderControlOrder(orderedReaderControls)) }
    var draggedControl by rememberSaveable { mutableStateOf<ReaderControl?>(null) }
    var dragOffsetPx by remember { mutableStateOf(0f) }

    LaunchedEffect(orderedReaderControls) {
        if (draggedControl == null) {
            previewControls = mergeReaderControlOrder(orderedReaderControls)
        }
    }

    SettingsCard(
        title = "Reader Controls",
        icon = Icons.Rounded.Tune,
        liquidGlassEnabled = liquidGlassEnabled,
        beta = true,
        onReset = if (hasReaderControlChanges) onResetOrder else null
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "Check to show. Long press the handle and drag to reorder.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            previewControls.forEachIndexed { index, control ->
                key(control) {
                    val (title, icon, checked, onCheckedChange) = when (control) {
                        ReaderControl.SEARCH -> ReaderControlRowData(
                            title = "Search",
                            icon = Icons.Rounded.Search,
                            checked = uiState.display.showReaderSearch,
                            onCheckedChange = events.onToggleReaderSearch
                        )
                        ReaderControl.LISTEN -> ReaderControlRowData(
                            title = "Listen",
                            icon = Icons.Rounded.Headset,
                            checked = uiState.display.showReaderListen,
                            onCheckedChange = events.onToggleReaderListen
                        )
                        ReaderControl.ACCESSIBILITY -> ReaderControlRowData(
                            title = "Accessibility",
                            icon = Icons.Rounded.Accessibility,
                            checked = uiState.display.showReaderAccessibility,
                            onCheckedChange = events.onToggleReaderAccessibility
                        )
                        ReaderControl.ANALYTICS -> ReaderControlRowData(
                            title = "Reading Analytics",
                            icon = Icons.Rounded.Analytics,
                            checked = uiState.display.showReaderAnalytics,
                            onCheckedChange = events.onToggleReaderAnalytics
                        )
                        ReaderControl.EXPORT_HIGHLIGHT -> ReaderControlRowData(
                            title = "Export Highlight",
                            icon = Icons.Rounded.Share,
                            checked = uiState.display.showReaderExport,
                            onCheckedChange = events.onToggleReaderExport
                        )
                    }

                    val isDragged = draggedControl == control

                    ReaderControlRow(
                        title = title,
                        icon = icon,
                        checked = checked,
                        onCheckedChange = onCheckedChange,
                        liquidGlassEnabled = liquidGlassEnabled,
                        isDragging = isDragged,
                        dragHandleModifier = Modifier.pointerInput(control) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedControl = control
                                    dragOffsetPx = 0f
                                    onDragActiveChange(true)
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, dragAmount ->
                                    val activeControl = draggedControl ?: return@detectDragGesturesAfterLongPress
                                    change.consume()
                                    dragOffsetPx += dragAmount.y

                                    var currentIndex = previewControls.indexOf(activeControl)
                                    var updatedControls = previewControls

                                    while (dragOffsetPx > itemFullHeightPx * 0.5f && currentIndex < updatedControls.lastIndex) {
                                        updatedControls = updatedControls.toMutableList().apply {
                                            removeAt(currentIndex)
                                            add(currentIndex + 1, activeControl)
                                        }
                                        currentIndex += 1
                                        dragOffsetPx -= itemFullHeightPx
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }

                                    while (dragOffsetPx < -itemFullHeightPx * 0.5f && currentIndex > 0) {
                                        updatedControls = updatedControls.toMutableList().apply {
                                            removeAt(currentIndex)
                                            add(currentIndex - 1, activeControl)
                                        }
                                        currentIndex -= 1
                                        dragOffsetPx += itemFullHeightPx
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }

                                    if (updatedControls !== previewControls) {
                                        previewControls = updatedControls
                                    }
                                },
                                onDragEnd = {
                                    if (previewControls != orderedReaderControls) {
                                        onOrderChanged(previewControls)
                                    }
                                    draggedControl = null
                                    dragOffsetPx = 0f
                                    onDragActiveChange(false)
                                },
                                onDragCancel = {
                                    previewControls = mergeReaderControlOrder(orderedReaderControls)
                                    draggedControl = null
                                    dragOffsetPx = 0f
                                    onDragActiveChange(false)
                                }
                            )
                        },
                        modifier = Modifier
                            .offset(
                                y = if (isDragged) {
                                    with(density) { dragOffsetPx.toDp() }
                                } else {
                                    0.dp
                                }
                            )
                    )
                }
            }
        }
    }
}

@Composable
internal fun LibraryDisplaySection(
    uiState: HomeUiState,
    liquidGlassEnabled: Boolean,
    events: SettingsEvents
) {
    SettingsCard(title = "Library Display", icon = Icons.Rounded.AutoStories, liquidGlassEnabled = liquidGlassEnabled) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Grid Columns",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.ExtraBold
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val columnOptions = listOf(2, 3, 4)
                    columnOptions.forEachIndexed { index, cols ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = columnOptions.size),
                            onClick = { events.onGridColumnsChanged(cols) },
                            selected = uiState.display.gridColumns == cols,
                            colors = appSegmentedButtonColors(),
                            label = { Text(cols.toString()) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            SettingSwitch("Hide Recent Reading", "Hide recently opened books section", !uiState.display.showRecentReading, { events.onShowRecentReadingChanged(!it) })
            SettingSwitch("Hide Genres", "Hide the book categories row", !uiState.display.showGenres, { events.onShowGenresChanged(!it) })
            SettingSwitch("Hide Favorites", "Hide favorited books row and heart icons", !uiState.display.showFavorites, { events.onShowFavoritesChanged(!it) })
            SettingSwitch("Hide File Type", "Hide file type badges on book covers", !uiState.display.showBookType, { events.onShowBookTypeChanged(!it) })
            SettingSwitch("Hide Status Bar", "Enable immersive mode while browsing", uiState.display.hideStatusBar, events.onHideStatusBarChanged)
            }
        }
    }
}

@Composable
internal fun BackupDataSection(
    uiState: HomeUiState,
    liquidGlassEnabled: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val context = LocalContext.current
    val libraryBytes = remember(uiState.allBooks) {
        uiState.allBooks.sumOf { it.fileSize.coerceAtLeast(0L) }
    }
    val storageStats = remember {
        runCatching {
            StatFs(context.filesDir.absolutePath).let { stat ->
                StorageStats(
                    totalBytes = stat.totalBytes,
                    availableBytes = stat.availableBytes
                )
            }
        }.getOrElse { StorageStats() }
    }
    val timestampFormat = remember {
        java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.getDefault())
    }
    val libraryUsageProgress = if (storageStats.totalBytes > 0L) {
        (libraryBytes.toFloat() / storageStats.totalBytes.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    SettingsCard(title = "Backup", icon = Icons.Rounded.CloudSync, liquidGlassEnabled = liquidGlassEnabled) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Backup & Data",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Keep your reading setup safe with local backup and restore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Library Storage",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LinearProgressIndicator(
                            progress = { libraryUsageProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .clip(RoundedCornerShape(999.dp)),
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                        Text(
                            text = "${Formatter.formatShortFileSize(context, libraryBytes)} used by your library folder",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${Formatter.formatShortFileSize(context, storageStats.availableBytes)} free of ${Formatter.formatShortFileSize(context, storageStats.totalBytes)} on this device",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Included in local backup",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "App settings, reader settings, bookmarks, highlights, notes, and library metadata.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Local Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Export a backup file or import one from this device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onExport,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.FileUpload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export Backup", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onImport,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(Icons.Rounded.FileDownload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Import Backup", fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Recent activity",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = uiState.lastLocalBackupExportAt?.let {
                                "Last export: ${timestampFormat.format(Date(it))}"
                            } ?: "Last export: Not yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = uiState.lastLocalBackupImportAt?.let {
                                "Last import: ${timestampFormat.format(Date(it))}"
                            } ?: "Last import: Not yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Cloud Backup",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "Paused for now while local backup stays as the main backup path.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun NotificationsSection(
    uiState: HomeUiState,
    liquidGlassEnabled: Boolean,
    events: SettingsEvents
) {
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(checkNotificationPermissionGranted(context)) }
    var pendingAction by rememberSaveable { mutableStateOf<NotificationPermissionAction?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        when (pendingAction) {
            NotificationPermissionAction.ENABLE_NOTIFICATIONS -> {
                events.onNotificationsEnabledChanged(granted)
            }
            NotificationPermissionAction.ENABLE_UPDATE_NOTIFICATIONS -> {
                events.onNotificationsEnabledChanged(granted)
                events.onUpdateNotificationsEnabledChanged(granted)
            }
            NotificationPermissionAction.ENABLE_REMINDER -> {
                events.onNotificationsEnabledChanged(granted)
                if (granted) {
                    events.onReadingReminderEnabledChanged(true)
                }
            }
            NotificationPermissionAction.SEND_TEST -> {
                if (granted) {
                    events.onNotificationsEnabledChanged(true)
                    events.onSendTestNotification()
                }
            }
            null -> Unit
        }
        pendingAction = null
    }

    fun requestNotificationPermission(action: NotificationPermissionAction) {
        pendingAction = action
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val reminderPresets = listOf(
        ReminderPreset("Morning", 8, 0),
        ReminderPreset("Evening", 20, 0),
        ReminderPreset("Night", 21, 30)
    )
    val selectedPresetIndex = reminderPresets.indexOfFirst {
        it.hour == uiState.display.readingReminderHour && it.minute == uiState.display.readingReminderMinute
    }

    SettingsCard(title = "Notifications", icon = Icons.Rounded.NotificationsActive, liquidGlassEnabled = liquidGlassEnabled) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (permissionGranted) {
                            "Send gentle reminders so it is easier to come back and continue reading."
                        } else {
                            "Allow notifications first so reminders can appear."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (!permissionGranted) {
                        OutlinedButton(
                            onClick = { requestNotificationPermission(NotificationPermissionAction.ENABLE_NOTIFICATIONS) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.Notifications, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Allow Notifications", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            SettingSwitch(
                title = "App Notifications",
                desc = "Allow alerts from the app.",
                checked = uiState.display.notificationsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !permissionGranted) {
                        requestNotificationPermission(NotificationPermissionAction.ENABLE_NOTIFICATIONS)
                    } else {
                        events.onNotificationsEnabledChanged(enabled)
                    }
                }
            )

            SettingSwitch(
                title = "Update Notifications",
                desc = "Notify when a new app release is available.",
                checked = uiState.display.updateNotificationsEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !permissionGranted) {
                        requestNotificationPermission(NotificationPermissionAction.ENABLE_UPDATE_NOTIFICATIONS)
                    } else {
                        if (enabled && !uiState.display.notificationsEnabled) {
                            events.onNotificationsEnabledChanged(true)
                        }
                        events.onUpdateNotificationsEnabledChanged(enabled)
                    }
                }
            )

            SettingSwitch(
                title = "Reminders",
                desc = "Daily reminder to come back and continue reading.",
                checked = uiState.display.notificationsEnabled && uiState.display.readingReminderEnabled,
                onCheckedChange = { enabled ->
                    if (enabled && !permissionGranted) {
                        requestNotificationPermission(NotificationPermissionAction.ENABLE_REMINDER)
                    } else {
                        if (enabled && !uiState.display.notificationsEnabled) {
                            events.onNotificationsEnabledChanged(true)
                        }
                        events.onReadingReminderEnabledChanged(enabled)
                    }
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Reminder Time",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    reminderPresets.forEachIndexed { index, preset ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = reminderPresets.size),
                            onClick = { events.onReadingReminderTimeChanged(preset.hour, preset.minute) },
                            selected = selectedPresetIndex == index,
                            colors = appSegmentedButtonColors(),
                            enabled = uiState.display.notificationsEnabled && uiState.display.readingReminderEnabled,
                            label = { Text(preset.label) },
                            icon = {
                                Icon(
                                    imageVector = Icons.Rounded.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
                Text(
                    text = "Current reminder: ${formatReminderTime(context, uiState.display.readingReminderHour, uiState.display.readingReminderMinute)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedButton(
                onClick = {
                    if (!permissionGranted) {
                        requestNotificationPermission(NotificationPermissionAction.SEND_TEST)
                    } else {
                        if (!uiState.display.notificationsEnabled) {
                            events.onNotificationsEnabledChanged(true)
                        }
                        events.onSendTestNotification()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.NotificationsActive, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Send Test Notification", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
internal fun StorageSection(
    uiState: HomeUiState,
    liquidGlassEnabled: Boolean,
    onSelectFolder: () -> Unit,
    onRevokeAccess: () -> Unit
) {
    SettingsCard(title = "Storage Management", icon = Icons.Rounded.Storage, liquidGlassEnabled = liquidGlassEnabled) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                    .padding(16.dp)
            ) {
                Text(
                    "Current Library Location",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (uiState.libraryUri.isNullOrBlank()) "No folder selected" else uiState.libraryUri,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onSelectFolder,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Relocate", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onRevokeAccess,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Text("Disconnect", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private data class StorageStats(
    val totalBytes: Long = 0L,
    val availableBytes: Long = 0L
)

private data class ReminderPreset(
    val label: String,
    val hour: Int,
    val minute: Int
)

private enum class NotificationPermissionAction {
    ENABLE_NOTIFICATIONS,
    ENABLE_UPDATE_NOTIFICATIONS,
    ENABLE_REMINDER,
    SEND_TEST
}

private fun checkNotificationPermissionGranted(context: android.content.Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
}

private fun formatReminderTime(context: android.content.Context, hour: Int, minute: Int): String {
    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.HOUR_OF_DAY, hour)
        set(java.util.Calendar.MINUTE, minute)
    }
    return DateFormat.getTimeFormat(context).format(calendar.time)
}

@Composable
internal fun AboutSection(
    liquidGlassEnabled: Boolean,
    appVersionLabel: String,
    updateUiState: AppUpdateUiState,
    hideBetaFeatures: Boolean,
    onHideBetaFeaturesChanged: (Boolean) -> Unit,
    developerOptionsEnabled: Boolean,
    onDeveloperOptionsChanged: (Boolean) -> Unit,
    hapticsEnabled: Boolean,
    onCheckForUpdates: () -> Unit,
    onInstallLatestUpdate: () -> Unit,
    onToggleLatestChangelog: () -> Unit,
    onToggleReleaseHistory: () -> Unit,
    onDeveloperMessage: (String) -> Unit,
    onShowLogs: () -> Unit
) {
    val context = LocalContext.current

    fun openReleaseUrl(url: String) {
        runCatching {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    Uri.parse(url)
                ).apply {
                    addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }

    SettingsCard(title = "About", icon = Icons.Rounded.Info, liquidGlassEnabled = liquidGlassEnabled) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                onClick = onShowLogs,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.BugReport, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text("View Debug Logs", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            var debugTapCount by rememberSaveable { mutableStateOf(0) }
            var lastDebugTapAt by rememberSaveable { mutableStateOf(0L) }
            val tapsToUnlock = 7
            val tapWindowMs = 900L
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp)
                    .clickable {
                        if (!developerOptionsEnabled) {
                            val now = SystemClock.elapsedRealtime()
                            if (now - lastDebugTapAt > tapWindowMs) {
                                debugTapCount = 0
                            }
                            lastDebugTapAt = now
                            if (hapticsEnabled) {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            }
                            debugTapCount = (debugTapCount + 1).coerceAtMost(tapsToUnlock)
                            val remaining = tapsToUnlock - debugTapCount
                            if (remaining <= 0) {
                                onDeveloperOptionsChanged(true)
                                onDeveloperMessage("Developer Options enabled")
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                }
                                debugTapCount = 0
                            } else {
                                onDeveloperMessage("$remaining taps away from Developer Options")
                            }
                        }
                    },
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("App Version", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    appVersionLabel,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Updates", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    PremiumUpdateHero(
                        status = when {
                            updateUiState.isChecking -> "Checking now"
                            updateUiState.updateAvailable && updateUiState.latestRelease != null -> "Update available"
                            else -> "Latest version installed"
                        },
                        message = when {
                            updateUiState.updateAvailable && updateUiState.latestRelease != null ->
                                "Version ${updateUiState.latestRelease.versionName} is ready to install."
                            updateUiState.latestRelease != null ->
                                "No updates available. You're currently on the latest version."
                            updateUiState.lastCheckedAt != null && updateUiState.errorMessage == null ->
                                "No updates available. You're currently on the latest version."
                            else -> "Check GitHub for the latest release."
                        },
                        updateAvailable = updateUiState.updateAvailable
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        PremiumUpdateMetric(
                            label = "Current build",
                            value = appVersionLabel
                        )
                        PremiumUpdateMetric(
                            label = "Latest release",
                            value = updateUiState.latestRelease?.versionName ?: "No release found yet"
                        )
                        PremiumUpdateMetric(
                            label = "Last checked",
                            value = updateUiState.lastCheckedAt?.let { checkedAt ->
                                "${DateFormat.getMediumDateFormat(LocalContext.current).format(Date(checkedAt))} ${DateFormat.getTimeFormat(LocalContext.current).format(Date(checkedAt))}"
                            } ?: "Not checked yet"
                        )
                    }
                    updateUiState.errorMessage?.let { error ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.18f))
                        ) {
                            Text(
                                text = error,
                                modifier = Modifier.padding(14.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    if (updateUiState.updateAvailable && updateUiState.latestRelease != null) {
                        Button(
                            onClick = {
                                if (updateUiState.latestRelease.downloadUrl != null) {
                                    onInstallLatestUpdate()
                                } else {
                                    openReleaseUrl(updateUiState.latestRelease.htmlUrl)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            enabled = !updateUiState.isPreparingInstall
                        ) {
                            Icon(Icons.Rounded.FileDownload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                when {
                                    updateUiState.isPreparingInstall -> "Preparing Update..."
                                    updateUiState.latestRelease.downloadUrl != null -> "Update Now"
                                    else -> "Open Release Page"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    PremiumUpdateAction(
                        icon = Icons.Rounded.CloudSync,
                        title = if (updateUiState.isChecking) "Checking..." else "Check for updates",
                        subtitle = "Refresh the latest release information from GitHub.",
                        onClick = onCheckForUpdates
                    )
                    PremiumUpdateAction(
                        icon = Icons.Rounded.Description,
                        title = if (updateUiState.showLatestReleaseDetails) "Hide latest changelog" else "View latest changelog",
                        subtitle = "See the newest release notes inside the app.",
                        enabled = updateUiState.latestRelease != null,
                        onClick = onToggleLatestChangelog
                    )
                    PremiumUpdateAction(
                        icon = Icons.Rounded.History,
                        title = when {
                            updateUiState.isLoadingReleaseHistory -> "Loading release history..."
                            updateUiState.showReleaseHistory -> "Hide release history"
                            else -> "View release history"
                        },
                        subtitle = "Browse earlier versions and open their release pages.",
                        onClick = onToggleReleaseHistory
                    )
                    if (updateUiState.showLatestReleaseDetails && updateUiState.latestRelease != null) {
                        val release = updateUiState.latestRelease
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Latest Changelog",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = release.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = buildString {
                                        append(release.versionName)
                                        release.publishedAt?.let {
                                            append(" • ")
                                            append(DateFormat.getMediumDateFormat(context).format(Date(it)))
                                        }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = release.notes.ifBlank { "No changelog notes were provided for this release." },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                OutlinedButton(
                                    onClick = { openReleaseUrl(release.htmlUrl) },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Open Release Page", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                    if (updateUiState.showReleaseHistory) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f))
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Release History",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (updateUiState.releaseHistory.isEmpty()) {
                                    Text(
                                        text = "No release history is available yet.",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    updateUiState.releaseHistory.forEachIndexed { index, release ->
                                        Surface(
                                            onClick = { openReleaseUrl(release.htmlUrl) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerLow
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = release.title,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = buildString {
                                                        append(release.versionName)
                                                        release.publishedAt?.let {
                                                            append(" • ")
                                                            append(DateFormat.getMediumDateFormat(context).format(Date(it)))
                                                        }
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = release.notes.ifBlank {
                                                        "No changelog notes were provided for this release."
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                        if (index != updateUiState.releaseHistory.lastIndex) {
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 8.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (developerOptionsEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                SettingSwitch(
                    title = "Developer Options",
                    desc = "Show advanced testing controls",
                    checked = developerOptionsEnabled,
                    onCheckedChange = onDeveloperOptionsChanged
                )
                SettingSwitch(
                    title = "Hide Beta Features",
                    desc = "Hide beta features and options across the app",
                    checked = hideBetaFeatures,
                    onCheckedChange = onHideBetaFeaturesChanged
                )
            }
        }
    }
}

@Composable
private fun PremiumUpdateHero(
    status: String,
    message: String,
    updateAvailable: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = if (updateAvailable) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.88f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = BorderStroke(
            1.dp,
            if (updateAvailable) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (updateAvailable) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = if (updateAvailable) {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.82f)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun PremiumUpdateMetric(
    label: String,
    value: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PremiumUpdateAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (enabled) 1f else 0.5f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(10.dp).size(18.dp),
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
