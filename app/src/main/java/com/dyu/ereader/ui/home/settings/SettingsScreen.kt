package com.dyu.ereader.ui.home.settings

import android.os.Build
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dyu.ereader.data.model.app.AppAccent
import com.dyu.ereader.data.model.app.AppFont
import com.dyu.ereader.data.model.app.AppTheme
import com.dyu.ereader.ui.components.dialogs.ColorPickerDialog
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.data.model.reader.ReaderControl
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.update.AppUpdateUiState
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.surfaces.SectionSurface
import com.dyu.ereader.ui.home.state.HomeUiState
import kotlinx.coroutines.launch

data class SettingsEvents(
    val onAppThemeChange: (AppTheme) -> Unit,
    val onAppFontChange: (AppFont) -> Unit,
    val onAppAccentChange: (AppAccent) -> Unit,
    val onAppCustomAccentColorChange: (Int?) -> Unit,
    val onAppTextScaleChange: (Float) -> Unit,
    val onLiquidGlassToggle: (Boolean) -> Unit,
    val onNavigationBarStyleChange: (NavigationBarStyle) -> Unit,
    val onAnimationsToggle: (Boolean) -> Unit,
    val onHapticsToggle: (Boolean) -> Unit,
    val onTextScrollerToggle: (Boolean) -> Unit,
    val onHideBetaFeaturesChanged: (Boolean) -> Unit,
    val onDeveloperOptionsChanged: (Boolean) -> Unit,
    val onShowBookTypeChanged: (Boolean) -> Unit,
    val onShowRecentReadingChanged: (Boolean) -> Unit,
    val onShowFavoritesChanged: (Boolean) -> Unit,
    val onShowGenresChanged: (Boolean) -> Unit,
    val onHideStatusBarChanged: (Boolean) -> Unit,
    val onGridColumnsChanged: (Int) -> Unit,
    val onSelectFolder: () -> Unit,
    val onRevokeAccess: () -> Unit,
    val onShowLogs: () -> Unit,
    val onExportSettings: suspend () -> String,
    val onImportSettings: (String) -> Unit,
    val onRecordLocalBackupExport: () -> Unit,
    val onRecordLocalBackupImport: () -> Unit,
    // New feature toggle events
    val onToggleReaderSearch: (Boolean) -> Unit,
    val onToggleReaderListen: (Boolean) -> Unit,
    val onToggleReaderAccessibility: (Boolean) -> Unit,
    val onToggleReaderAnalytics: (Boolean) -> Unit,
    val onToggleReaderExport: (Boolean) -> Unit,
    val onReaderControlOrderChange: (List<ReaderControl>) -> Unit,
    val onReaderSettingsChanged: (ReaderSettings) -> Unit,
    val onNotificationsEnabledChanged: (Boolean) -> Unit,
    val onUpdateNotificationsEnabledChanged: (Boolean) -> Unit,
    val onReadingReminderEnabledChanged: (Boolean) -> Unit,
    val onReadingReminderTimeChanged: (Int, Int) -> Unit,
    val onSendTestNotification: () -> Unit,
    val onCheckForUpdates: () -> Unit,
    val onInstallLatestUpdate: () -> Unit,
    val onToggleLatestChangelog: () -> Unit,
    val onToggleReleaseHistory: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsArea(
    modifier: Modifier = Modifier,
    uiState: HomeUiState,
    appTheme: AppTheme,
    appFont: AppFont,
    appAccent: AppAccent,
    customAccentColor: Int?,
    navBarStyle: NavigationBarStyle,
    liquidGlassEnabled: Boolean = false,
    pendingCloudAuthUri: Uri? = null,
    searchQuery: String = "",
    updateUiState: AppUpdateUiState = AppUpdateUiState(),
    events: SettingsEvents,
    onConsumePendingCloudAuthUri: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var developerMessageJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
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
    var showAccentColorPicker by remember { mutableStateOf(false) }
    val defaultReaderControls = remember { ReaderControl.defaultOrder() }
    val hasReaderControlChanges = orderedReaderControls != defaultReaderControls
    var readerControlDragActive by remember { mutableStateOf(false) }
    val mainScrollState = rememberScrollState()
    var currentDestination by rememberSaveable { mutableStateOf(SettingsDestination.ROOT) }
    val normalizedSearchQuery = remember(searchQuery) { searchQuery.trim().lowercase() }
    val isSearching = normalizedSearchQuery.isNotBlank()
    val canNavigateBack = !isSearching && currentDestination != SettingsDestination.ROOT
    val showAppearanceSection = if (isSearching) normalizedSearchQuery.matchesSettingsSearch(
        "appearance", "theme", "font", "accent", "color", "oled", "dark", "light", "system",
        "text size", "text scale", "navigation", "navigation bar", "animations", "haptics", "beta",
        "app settings", "app font", "accent color", "oled mode", "system theme"
    ) else currentDestination == SettingsDestination.APP_APPEARANCE
    val showReaderDefaultsSection = if (isSearching) normalizedSearchQuery.matchesSettingsSearch(
        "reader", "reading", "page", "theme", "font size", "line spacing", "margin",
        "alignment", "publisher", "ambient", "status bar", "background", "typeface", "spacing",
        "preview", "reader theme", "reading preview"
    ) else currentDestination == SettingsDestination.READER_APPEARANCE
    val showReaderControlsSection = if (isSearching) normalizedSearchQuery.matchesSettingsSearch(
        "reader", "controls", "search", "listen", "accessibility", "analytics", "export", "highlight",
        "reader buttons", "toolbar", "reorder", "drag"
    ) else currentDestination == SettingsDestination.READER_APPEARANCE
    val showLibraryDisplaySection = if (isSearching) normalizedSearchQuery.matchesSettingsSearch(
        "library", "display", "recent", "genres", "favorites", "file type", "status bar", "immersive",
        "grid", "columns", "layout", "badges"
    ) else currentDestination == SettingsDestination.LIBRARY
    val showBackupSection = if (isSearching) normalizedSearchQuery.matchesSettingsSearch(
        "backup", "data", "export", "import", "local backup", "restore", "storage bar",
        "last export", "last import", "backup file"
    ) else currentDestination == SettingsDestination.STORAGE_BACKUP
    val showNotificationsSection = if (isSearching) normalizedSearchQuery.matchesSettingsSearch(
        "notifications", "reminder", "reading reminder", "alerts", "test notification",
        "permission", "push", "daily reminder", "update notifications", "app updates"
    ) else currentDestination == SettingsDestination.NOTIFICATIONS
    val showStorageSection = if (isSearching) normalizedSearchQuery.matchesSettingsSearch(
        "storage", "folder", "library access", "revoke", "manage", "location", "relocate",
        "disconnect", "permission"
    ) else currentDestination == SettingsDestination.STORAGE_BACKUP
    val showAboutSection = if (isSearching) normalizedSearchQuery.matchesSettingsSearch(
        "about", "version", "logs", "developer", "haptics", "beta", "debug", "troubleshooting",
        "update", "updates", "release", "changelog", "github"
    ) else currentDestination == SettingsDestination.ABOUT
    val hasSearchResults = showAppearanceSection ||
        showReaderDefaultsSection ||
        showReaderControlsSection ||
        showLibraryDisplaySection ||
        showBackupSection ||
        showNotificationsSection ||
        showStorageSection ||
        showAboutSection
    val matchedSearchSections = remember(
        showAppearanceSection,
        showReaderDefaultsSection,
        showReaderControlsSection,
        showLibraryDisplaySection,
        showBackupSection,
        showNotificationsSection,
        showStorageSection,
        showAboutSection
    ) {
        buildList {
            if (showAppearanceSection) {
                add(
                    SearchResultGroup(
                        title = "App Settings",
                        description = "Theme, font, accent color, and navigation style.",
                        icon = Icons.Rounded.Palette
                    )
                )
            }
            if (showReaderDefaultsSection) {
                add(
                    SearchResultGroup(
                        title = "Reader Appearance",
                        description = "Background, typeface, layout, spacing, and preview.",
                        icon = Icons.AutoMirrored.Rounded.MenuBook
                    )
                )
            }
            if (showReaderControlsSection) {
                add(
                    SearchResultGroup(
                        title = "Reader Controls",
                        description = "Search, listen, accessibility, analytics, and control order.",
                        icon = Icons.Rounded.Tune
                    )
                )
            }
            if (showLibraryDisplaySection) {
                add(
                    SearchResultGroup(
                        title = "Library",
                        description = "Grid columns, sections, badges, and shelf display.",
                        icon = Icons.Rounded.AutoStories
                    )
                )
            }
            if (showBackupSection || showStorageSection) {
                add(
                    SearchResultGroup(
                        title = "Storage & Backup",
                        description = "Library folder location, local backup, restore, and device storage.",
                        icon = Icons.Rounded.Storage
                    )
                )
            }
            if (showNotificationsSection) {
                add(
                    SearchResultGroup(
                        title = "Notifications",
                        description = "Reminders, permissions, and test alerts.",
                        icon = Icons.Rounded.Notifications
                    )
                )
            }
            if (showAboutSection) {
                add(
                    SearchResultGroup(
                        title = "About",
                        description = "Version, update checks, logs, and advanced tools.",
                        icon = Icons.Rounded.Info
                    )
                )
            }
        }
    }

    BackHandler(enabled = canNavigateBack) {
        currentDestination = currentDestination.backDestination()
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
                    events.onRecordLocalBackupExport()
                    snackbarHostState.showSnackbar("Backup exported successfully")
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
                    events.onRecordLocalBackupImport()
                    snackbarHostState.showSnackbar("Backup imported successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Import failed: ${e.message}")
                }
            }
        }
    }

    if (showAccentColorPicker) {
        ColorPickerDialog(
            onDismiss = { showAccentColorPicker = false },
            onColorSelected = {
                events.onAppCustomAccentColorChange(it)
                events.onAppAccentChange(AppAccent.CUSTOM)
                showAccentColorPicker = false
            },
            initialColor = customAccentColor,
            onCancel = {}
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(mainScrollState, enabled = !readerControlDragActive)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (isSearching) {
                SettingsOverviewCard(
                    title = "Search Settings",
                    description = "Find app, reader, library, backup, notification, and about settings.",
                    icon = Icons.Rounded.Search,
                    isSearching = true,
                    query = searchQuery,
                    resultCount = matchedSearchSections.size
                )
                if (matchedSearchSections.isNotEmpty()) {
                    SearchResultsSummary(groups = matchedSearchSections)
                }
            } else {
                when (currentDestination) {
                    SettingsDestination.ROOT -> {
                        SettingsMainButtons(
                            onSelected = { section ->
                                currentDestination = when (section) {
                                    SettingsMainSection.APP_SETTINGS -> SettingsDestination.APP_APPEARANCE
                                    SettingsMainSection.READER -> SettingsDestination.READER_APPEARANCE
                                    SettingsMainSection.LIBRARY -> SettingsDestination.LIBRARY
                                    SettingsMainSection.STORAGE_BACKUP -> SettingsDestination.STORAGE_BACKUP
                                    SettingsMainSection.NOTIFICATIONS -> SettingsDestination.NOTIFICATIONS
                                    SettingsMainSection.ABOUT -> SettingsDestination.ABOUT
                                }
                            }
                        )
                    }

                    SettingsDestination.APP_APPEARANCE -> {
                        SettingsNavigationHeader(
                            backLabel = "Settings",
                            title = "App Settings",
                            description = "Theme, font, color, and navigation.",
                            onBack = { currentDestination = SettingsDestination.ROOT }
                        )
                    }

                    SettingsDestination.READER_APPEARANCE -> {
                        SettingsNavigationHeader(
                            backLabel = "Settings",
                            title = "Reader",
                            description = "Background, layout, type, and controls.",
                            onBack = { currentDestination = SettingsDestination.ROOT }
                        )
                    }

                    SettingsDestination.LIBRARY -> {
                        SettingsNavigationHeader(
                            backLabel = "Settings",
                            title = SettingsMainSection.LIBRARY.label,
                            description = SettingsMainSection.LIBRARY.description,
                            onBack = { currentDestination = SettingsDestination.ROOT }
                        )
                    }

                    SettingsDestination.STORAGE_BACKUP -> {
                        SettingsNavigationHeader(
                            backLabel = "Settings",
                            title = SettingsMainSection.STORAGE_BACKUP.label,
                            description = SettingsMainSection.STORAGE_BACKUP.description,
                            onBack = { currentDestination = SettingsDestination.ROOT }
                        )
                    }

                    SettingsDestination.NOTIFICATIONS -> {
                        SettingsNavigationHeader(
                            backLabel = "Settings",
                            title = SettingsMainSection.NOTIFICATIONS.label,
                            description = SettingsMainSection.NOTIFICATIONS.description,
                            onBack = { currentDestination = SettingsDestination.ROOT }
                        )
                    }

                    SettingsDestination.ABOUT -> {
                        SettingsNavigationHeader(
                            backLabel = "Settings",
                            title = SettingsMainSection.ABOUT.label,
                            description = SettingsMainSection.ABOUT.description,
                            onBack = { currentDestination = SettingsDestination.ROOT }
                        )
                    }
                }
            }

            if (showAppearanceSection) {
                AppearanceSection(
                    uiState = uiState,
                    appTheme = appTheme,
                    appFont = appFont,
                    appAccent = appAccent,
                    customAccentColor = customAccentColor,
                    navBarStyle = navBarStyle,
                    liquidGlassEnabled = liquidGlassEnabled,
                    onShowAccentColorPicker = { showAccentColorPicker = true },
                    events = events
                )
            }

            if (showReaderDefaultsSection) {
                ReaderDefaultsSection(
                    uiState = uiState,
                    liquidGlassEnabled = liquidGlassEnabled,
                    onReaderSettingsChanged = events.onReaderSettingsChanged
                )
            }

            if (showReaderControlsSection) {
                ReaderControlsSection(
                    uiState = uiState,
                    liquidGlassEnabled = liquidGlassEnabled,
                    orderedReaderControls = orderedReaderControls,
                    hasReaderControlChanges = hasReaderControlChanges,
                    onResetOrder = {
                        orderedReaderControls = defaultReaderControls
                        events.onReaderControlOrderChange(defaultReaderControls)
                    },
                    onOrderChanged = {
                        orderedReaderControls = it
                        events.onReaderControlOrderChange(it)
                    },
                    onDragActiveChange = { readerControlDragActive = it },
                    events = events
                )
            }

            if (showLibraryDisplaySection) {
                LibraryDisplaySection(
                    uiState = uiState,
                    liquidGlassEnabled = liquidGlassEnabled,
                    events = events
                )
            }

            if (showBackupSection) {
                BackupDataSection(
                    uiState = uiState,
                    liquidGlassEnabled = liquidGlassEnabled,
                    onExport = { exportLauncher.launch("ereader_backup.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json")) }
                )
            }

            if (showNotificationsSection) {
                NotificationsSection(
                    uiState = uiState,
                    liquidGlassEnabled = liquidGlassEnabled,
                    events = events
                )
            }

            if (showStorageSection) {
                StorageSection(
                    uiState = uiState,
                    liquidGlassEnabled = liquidGlassEnabled,
                    onSelectFolder = events.onSelectFolder,
                    onRevokeAccess = events.onRevokeAccess
                )
            }

            if (showAboutSection) {
                AboutSection(
                    liquidGlassEnabled = liquidGlassEnabled,
                    appVersionLabel = appVersionLabel,
                    updateUiState = updateUiState,
                    hideBetaFeatures = uiState.display.hideBetaFeatures,
                    onHideBetaFeaturesChanged = events.onHideBetaFeaturesChanged,
                    developerOptionsEnabled = uiState.display.developerOptionsEnabled,
                    onDeveloperOptionsChanged = events.onDeveloperOptionsChanged,
                    hapticsEnabled = uiState.display.hapticsEnabled,
                    onCheckForUpdates = events.onCheckForUpdates,
                    onInstallLatestUpdate = events.onInstallLatestUpdate,
                    onToggleLatestChangelog = events.onToggleLatestChangelog,
                    onToggleReleaseHistory = events.onToggleReleaseHistory,
                    onDeveloperMessage = { message ->
                        developerMessageJob?.cancel()
                        developerMessageJob = scope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                message = message,
                                duration = SnackbarDuration.Short
                            )
                        }
                    },
                    onShowLogs = events.onShowLogs
                )
            }

            if (isSearching && !hasSearchResults) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "No settings matched \"$searchQuery\"",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
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

private fun String.matchesSettingsSearch(vararg keywords: String): Boolean {
    if (isBlank()) return true
    val normalizedQuery = lowercase()
    val haystack = keywords.joinToString(separator = " ") { it.lowercase() }
    if (haystack.contains(normalizedQuery)) return true
    val tokens = normalizedQuery
        .split(Regex("\\s+"))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return true
    return tokens.all { token -> haystack.contains(token) }
}

private data class SearchResultGroup(
    val title: String,
    val description: String,
    val icon: ImageVector
)

private enum class SettingsDestination {
    ROOT,
    APP_APPEARANCE,
    READER_APPEARANCE,
    LIBRARY,
    STORAGE_BACKUP,
    NOTIFICATIONS,
    ABOUT
}

private fun SettingsDestination.backDestination(): SettingsDestination = when (this) {
    SettingsDestination.ROOT -> SettingsDestination.ROOT
    SettingsDestination.APP_APPEARANCE -> SettingsDestination.ROOT
    SettingsDestination.READER_APPEARANCE -> SettingsDestination.ROOT
    SettingsDestination.LIBRARY -> SettingsDestination.ROOT
    SettingsDestination.STORAGE_BACKUP -> SettingsDestination.ROOT
    SettingsDestination.NOTIFICATIONS -> SettingsDestination.ROOT
    SettingsDestination.ABOUT -> SettingsDestination.ROOT
}

private enum class SettingsMainSection(
    val label: String,
    val description: String,
    val icon: ImageVector
) {
    APP_SETTINGS(
        "App Settings",
        "Theme, color, font, and reading style.",
        Icons.Rounded.Palette
    ),
    READER(
        "Reader",
        "Background, layout, type, and controls.",
        Icons.AutoMirrored.Rounded.MenuBook
    ),
    LIBRARY(
        "Library",
        "Shelves, badges, and layout.",
        Icons.Rounded.AutoStories
    ),
    STORAGE_BACKUP(
        "Storage & Backup",
        "Library folder, restore, and device storage.",
        Icons.Rounded.Storage
    ),
    NOTIFICATIONS(
        "Notifications",
        "Reminders and alerts.",
        Icons.Rounded.Notifications
    ),
    ABOUT(
        "About",
        "Version, updates, logs, and advanced tools.",
        Icons.Rounded.Info
    )
}

@Composable
private fun SettingsMainButtons(
    onSelected: (SettingsMainSection) -> Unit
) {
    BoxWithConstraints {
        val columns = if (maxWidth < 620.dp) 1 else 2
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsMainSection.entries.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { section ->
                        SettingsEntryButton(
                            title = section.label,
                            description = section.description,
                            icon = section.icon,
                            onClick = { onSelected(section) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsOverviewCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSearching: Boolean,
    query: String,
    resultCount: Int = 0
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SectionSurface(
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
            shape = RoundedCornerShape(28.dp),
            contentPadding = PaddingValues(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (isSearching && query.isNotBlank()) {
                        buildString {
                            append("Showing ")
                            append(resultCount)
                            append(if (resultCount == 1) " result" else " results")
                            append(" for \"$query\". ")
                            append(description)
                        }
                    } else {
                        description
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SearchResultsSummary(
    groups: List<SearchResultGroup>
) {
    SectionSurface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Top Matches",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            groups.forEachIndexed { index, group ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    ) {
                        Icon(
                            imageVector = group.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(10.dp).size(16.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = group.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = group.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (index != groups.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
                }
            }
        }
    }
}

@Composable
private fun SettingsNavigationHeader(
    backLabel: String,
    title: String,
    description: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AppChromeIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back to $backLabel",
            onClick = onBack
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = backLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingsEntryButton(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    selectedColor: Color = MaterialTheme.colorScheme.primaryContainer,
    selectedBorderColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    SectionSurface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(26.dp),
        contentPadding = PaddingValues(16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 124.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = selectedColor.copy(alpha = 0.22f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = selectedBorderColor,
                        modifier = Modifier.padding(12.dp).size(18.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        modifier = Modifier.padding(8.dp).size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
