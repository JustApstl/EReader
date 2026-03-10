package com.dyu.ereader.ui.home

import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyu.ereader.data.model.AppTheme
import com.dyu.ereader.data.model.NavigationBarStyle
import com.dyu.ereader.data.model.ReaderControl
import kotlinx.coroutines.launch

data class SettingsEvents(
    val onAppThemeChange: (AppTheme) -> Unit,
    val onLiquidGlassToggle: (Boolean) -> Unit,
    val onNavigationBarStyleChange: (NavigationBarStyle) -> Unit,
    val onAnimationsToggle: (Boolean) -> Unit,
    val onShowBookTypeChanged: (Boolean) -> Unit,
    val onShowRecentReadingChanged: (Boolean) -> Unit,
    val onShowFavoritesChanged: (Boolean) -> Unit,
    val onShowGenresChanged: (Boolean) -> Unit,
    val onHideStatusBarChanged: (Boolean) -> Unit,
    val onGridColumnsChanged: (Int) -> Unit,
    val onSelectFolder: () -> Unit,
    val onRevokeAccess: () -> Unit,
    val onShowLogs: () -> Unit,
    val onShowCloudBackup: () -> Unit,
    val onExportSettings: suspend () -> String,
    val onImportSettings: (String) -> Unit,
    // New feature toggle events
    val onToggleReaderSearch: (Boolean) -> Unit,
    val onToggleReaderTTS: (Boolean) -> Unit,
    val onToggleReaderAccessibility: (Boolean) -> Unit,
    val onToggleReaderAnalytics: (Boolean) -> Unit,
    val onToggleReaderExport: (Boolean) -> Unit,
    val onReaderControlOrderChange: (List<ReaderControl>) -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsArea(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    appTheme: AppTheme,
    navBarStyle: NavigationBarStyle,
    liquidGlassEnabled: Boolean = false,
    events: SettingsEvents
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val appVersionLabel = remember(context) {
        runCatching {
            @Suppress("DEPRECATION")
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionName = packageInfo.versionName ?: "Unknown"
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            "v$versionName ($versionCode)"
        }.getOrElse { "vUnknown" }
    }
    var orderedReaderControls by remember(uiState.display.readerControlOrder) {
        mutableStateOf(
            mergeReaderControlOrder(uiState.display.readerControlOrder)
        )
    }

    LaunchedEffect(uiState.display.readerControlOrder) {
        orderedReaderControls = mergeReaderControlOrder(uiState.display.readerControlOrder)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = events.onExportSettings()
                    context.contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(json.toByteArray())
                    }
                    snackbarHostState.showSnackbar("Settings exported successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Export failed: ${e.message}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val json = stream.bufferedReader().use { it.readText() }
                        events.onImportSettings(json)
                    }
                    snackbarHostState.showSnackbar("Settings imported successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Import failed: ${e.message}")
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            SettingsCard(title = "Appearance", icon = Icons.Rounded.Palette, liquidGlassEnabled = liquidGlassEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Column {
                        Text(
                            "App Theme", 
                            style = MaterialTheme.typography.labelLarge, 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            AppTheme.entries.forEach { theme ->
                                ThemeItem(
                                    theme = theme,
                                    isSelected = appTheme == theme,
                                    onClick = { events.onAppThemeChange(theme) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    Column {
                        Text(
                            "Navigation Bar Style", 
                            style = MaterialTheme.typography.labelLarge, 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(12.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            NavigationBarStyle.entries.forEachIndexed { index, style ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = NavigationBarStyle.entries.size),
                                    onClick = { events.onNavigationBarStyleChange(style) },
                                    selected = navBarStyle == style,
                                    label = { 
                                        Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) 
                                    }
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    
                    SettingSwitch(
                        title = "Liquid Glass", 
                        beta = true,
                        desc = "Enable frosted glass effect", 
                        checked = liquidGlassEnabled, 
                        onCheckedChange = events.onLiquidGlassToggle
                    )

                    SettingSwitch(
                        title = "Reduced Motion", 
                        desc = "Disable custom UI animations", 
                        checked = !uiState.display.animationsEnabled, 
                        onCheckedChange = { events.onAnimationsToggle(!it) }
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    
                    Column {
                        Text(
                            "Library Grid Columns", 
                            style = MaterialTheme.typography.labelLarge, 
                            color = MaterialTheme.colorScheme.primary, 
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(Modifier.height(12.dp))
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            val columnOptions = listOf(2, 3, 4)
                            columnOptions.forEachIndexed { index, cols ->
                                SegmentedButton(
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = columnOptions.size),
                                    onClick = { events.onGridColumnsChanged(cols) },
                                    selected = uiState.display.gridColumns == cols,
                                    label = { Text(cols.toString()) }
                                )
                            }
                        }
                    }
                }
            }

            SettingsCard(
                title = "Reader Controls", 
                icon = Icons.Rounded.Tune, 
                liquidGlassEnabled = liquidGlassEnabled, 
                beta = true,
                onReset = {
                    val default = ReaderControl.defaultOrder()
                    orderedReaderControls = default
                    events.onReaderControlOrderChange(default)
                }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Check to show. Long press and drag to reorder.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    var draggedIndex by remember { mutableStateOf<Int?>(null) }
                    var dragOffset by remember { mutableStateOf(0f) }

                    orderedReaderControls.forEachIndexed { index, control ->
                        val (title, desc, icon, checked, onCheckedChange) = when (control) {
                            ReaderControl.SEARCH -> ReaderControlRowData(
                                title = "Search on Reading",
                                desc = "Show search tool in reader bar",
                                icon = Icons.Rounded.Search,
                                checked = uiState.display.showReaderSearch,
                                onCheckedChange = events.onToggleReaderSearch
                            )
                            ReaderControl.TTS -> ReaderControlRowData(
                                title = "Text to Speech",
                                desc = "Enable voice playback tool",
                                icon = Icons.Rounded.VolumeUp,
                                checked = uiState.display.showReaderTTS,
                                onCheckedChange = events.onToggleReaderTTS
                            )
                            ReaderControl.ACCESSIBILITY -> ReaderControlRowData(
                                title = "Accessibility",
                                desc = "Enable accessibility settings tool",
                                icon = Icons.Rounded.Accessibility,
                                checked = uiState.display.showReaderAccessibility,
                                onCheckedChange = events.onToggleReaderAccessibility
                            )
                            ReaderControl.ANALYTICS -> ReaderControlRowData(
                                title = "Reading Analytics",
                                desc = "Enable reading stats tool",
                                icon = Icons.Rounded.Analytics,
                                checked = uiState.display.showReaderAnalytics,
                                onCheckedChange = events.onToggleReaderAnalytics
                            )
                            ReaderControl.EXPORT_HIGHLIGHT -> ReaderControlRowData(
                                title = "Export Highlight",
                                desc = "Enable export/highlight share tool",
                                icon = Icons.Rounded.Share,
                                checked = uiState.display.showReaderExport,
                                onCheckedChange = events.onToggleReaderExport
                            )
                        }

                        val isDragged = draggedIndex == index
                        val itemHeight = 72f

                        ReaderControlRow(
                            title = title,
                            desc = desc,
                            icon = icon,
                            checked = checked,
                            onCheckedChange = onCheckedChange,
                            isDragging = isDragged,
                            modifier = Modifier
                                .offset(y = if (isDragged) dragOffset.dp else 0.dp)
                                .pointerInput(control) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            draggedIndex = index
                                            dragOffset = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffset += dragAmount.y

                                            val threshold = itemHeight / 2

                                            // Swap down
                                            if (dragOffset > threshold && index < orderedReaderControls.lastIndex) {
                                                val updated = orderedReaderControls.toMutableList()
                                                val temp = updated[index]
                                                updated[index] = updated[index + 1]
                                                updated[index + 1] = temp
                                                orderedReaderControls = updated
                                                events.onReaderControlOrderChange(updated)
                                                draggedIndex = index + 1
                                                dragOffset -= itemHeight
                                            }
                                            // Swap up
                                            else if (dragOffset < -threshold && index > 0) {
                                                val updated = orderedReaderControls.toMutableList()
                                                val temp = updated[index]
                                                updated[index] = updated[index - 1]
                                                updated[index - 1] = temp
                                                orderedReaderControls = updated
                                                events.onReaderControlOrderChange(updated)
                                                draggedIndex = index - 1
                                                dragOffset += itemHeight
                                            }
                                        },
                                        onDragEnd = {
                                            draggedIndex = null
                                            dragOffset = 0f
                                        },
                                        onDragCancel = {
                                            draggedIndex = null
                                            dragOffset = 0f
                                        }
                                    )
                                }
                        )
                    }
                }
            }

            SettingsCard(title = "Library Display", icon = Icons.Rounded.AutoStories, liquidGlassEnabled = liquidGlassEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    SettingSwitch("Hide Recent Reading", "Hide recently opened books section", !uiState.display.showRecentReading, { events.onShowRecentReadingChanged(!it) })
                    SettingSwitch("Hide Genres", "Hide the book categories row", !uiState.display.showGenres, { events.onShowGenresChanged(!it) })
                    SettingSwitch("Hide Favorites", "Hide favorited books row and heart icons", !uiState.display.showFavorites, { events.onShowFavoritesChanged(!it) })
                    SettingSwitch("Hide File Type", "Hide PDF/EPUB badges on book covers", !uiState.display.showBookType, { events.onShowBookTypeChanged(!it) })
                    SettingSwitch("Hide Status Bar", "Enable immersive mode while browsing", uiState.display.hideStatusBar, events.onHideStatusBarChanged, beta = true)
                }
            }

            SettingsCard(title = "Backup & Data", icon = Icons.Rounded.CloudSync, liquidGlassEnabled = liquidGlassEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Cloud Backup Access Button - Fixed theme sync
                    Surface(
                        onClick = events.onShowCloudBackup,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Cloud Backup", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                Text("Sync library across providers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.outline)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                    Text(
                        "Synchronize your reading progress and application settings locally.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { exportLauncher.launch("ereader_backup.json") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Rounded.FileUpload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Export", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { importLauncher.launch(arrayOf("application/json")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Icon(Icons.Rounded.FileDownload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Import", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            SettingsCard(title = "Storage Management", icon = Icons.Rounded.Storage, liquidGlassEnabled = liquidGlassEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
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
                            onClick = events.onSelectFolder, 
                            modifier = Modifier.weight(1f), 
                            shape = RoundedCornerShape(16.dp)
                        ) { 
                            Text("Relocate", fontWeight = FontWeight.Bold) 
                        }
                        OutlinedButton(
                            onClick = events.onRevokeAccess, 
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

            SettingsCard(title = "Information", icon = Icons.Rounded.Info, liquidGlassEnabled = liquidGlassEnabled) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Refined Debug Logs Button
                    Surface(
                        onClick = events.onShowLogs,
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
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("EReader Pro", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Text(
                            appVersionLabel,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(100.dp))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 90.dp)
        )
    }
}

private data class ReaderControlRowData(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit
)

private fun mergeReaderControlOrder(current: List<ReaderControl>): List<ReaderControl> {
    val defaults = ReaderControl.defaultOrder()
    val merged = (current + defaults).distinct()
    return merged.filter { defaults.contains(it) }
}

@Composable
private fun ReaderControlRow(
    title: String,
    desc: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isDragging: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(if (isDragging) 1.05f else 1f)
    val elevation by animateFloatAsState(if (isDragging) 8f else 0f)
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(elevation = elevation.dp, shape = RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f + if (isDragging) 0.3f else 0f))
            .clickable(enabled = !isDragging) { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = !isDragging
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            Icons.Rounded.DragHandle,
            contentDescription = "Hold and drag to reorder",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDragging) 1f else 0.7f),
            modifier = Modifier
                .size(20.dp)
                .padding(end = 4.dp)
                .alpha(if (isDragging) 1f else 0.7f)
        )
    }
}

@Composable
private fun ThemeItem(
    theme: AppTheme,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor by animateColorAsState(if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
    
    Column(
        modifier = modifier
            .clip(CircleShape)
            .background(containerColor)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                    when (theme) {
                        AppTheme.LIGHT -> Color(0xFFF5F5F5)
                        AppTheme.DARK -> Color(0xFF1A1C1E)
                        AppTheme.BLACK -> Color(0xFF000000)
                        AppTheme.SYSTEM -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .then(
                    if (theme == AppTheme.SYSTEM) Modifier.border(1.dp, Color.Gray.copy(alpha = 0.3f), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    Icons.Rounded.CheckCircle, 
                    null, 
                    tint = if (theme == AppTheme.LIGHT) MaterialTheme.colorScheme.primary else Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = when(theme) {
                AppTheme.LIGHT -> "Light"
                AppTheme.DARK -> "Dark"
                AppTheme.BLACK -> "OLED"
                AppTheme.SYSTEM -> "System"
            },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun BetaBadge(modifier: Modifier = Modifier) {
    Badge(
        containerColor = Color(0xFF2196F3), // Blue color
        contentColor = Color.White,
        modifier = modifier
    ) {
        Text(
            "BETA", 
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontWeight = FontWeight.Black),
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
private fun SettingsCard(
    title: String, 
    icon: ImageVector, 
    liquidGlassEnabled: Boolean = false, 
    beta: Boolean = false, 
    onReset: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically, 
            horizontalArrangement = Arrangement.SpaceBetween, 
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
        ) {
            Box {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(icon, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                }
                if (beta) {
                    BetaBadge(Modifier.align(Alignment.TopEnd).offset(x = 32.dp, y = (-6).dp))
                }
            }
            if (onReset != null) {
                IconButton(onClick = onReset, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = if (liquidGlassEnabled) MaterialTheme.colorScheme.surface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, desc: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, beta: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween, 
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Box {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
                if (beta) {
                    BetaBadge(Modifier.align(Alignment.TopEnd).offset(x = 24.dp, y = (-4).dp))
                }
            }
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
