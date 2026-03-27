package com.dyu.ereader.ui.home.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.dyu.ereader.data.model.app.AppAccent
import com.dyu.ereader.data.model.app.AppFont
import com.dyu.ereader.data.model.app.AppTheme
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.data.model.reader.ReaderControl
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.update.AppUpdateUiState
import com.dyu.ereader.ui.home.overlays.HomeDeleteDialog
import com.dyu.ereader.ui.home.overlays.HomeDetailsOverlay
import com.dyu.ereader.ui.home.overlays.HomeFilterSheet
import com.dyu.ereader.ui.home.overlays.sheets.HomeBookActionsSheet
import com.dyu.ereader.ui.home.state.HomeUiState
import com.dyu.ereader.ui.home.state.ReadingStatus
import com.dyu.ereader.ui.home.state.SortOrder
import com.dyu.ereader.ui.components.insets.stableStatusBarsPadding
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    appTheme: AppTheme,
    appFont: AppFont,
    appAccent: AppAccent,
    customAccentColor: Int?,
    navBarStyle: NavigationBarStyle,
    liquidGlassEnabled: Boolean = false,
    libraryMessage: String? = null,
    pendingExportUri: Uri? = null,
    pendingCloudAuthUri: Uri? = null,
    onSearchChanged: (String) -> Unit,
    onBrowseSearch: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onRefresh: () -> Unit,
    onLibraryAccessGranted: (Uri) -> Unit,
    onRevokeLibraryAccess: () -> Unit,
    onOpenBook: (BookItem) -> Unit,
    onAppThemeChange: (AppTheme) -> Unit,
    onAppFontChange: (AppFont) -> Unit,
    onAppAccentChange: (AppAccent) -> Unit,
    onAppCustomAccentColorChange: (Int?) -> Unit,
    onAppTextScaleChange: (Float) -> Unit,
    onNavigationBarStyleChange: (NavigationBarStyle) -> Unit,
    onLiquidGlassToggle: (Boolean) -> Unit = {},
    onAnimationsToggle: (Boolean) -> Unit = {},
    onHapticsToggle: (Boolean) -> Unit = {},
    onTextScrollerToggle: (Boolean) -> Unit = {},
    onHideBetaFeaturesChanged: (Boolean) -> Unit = {},
    onDeveloperOptionsChanged: (Boolean) -> Unit = {},
    onToggleFavorite: (String, Boolean) -> Unit,
    onToggleLayout: () -> Unit,
    onShowBookTypeChanged: (Boolean) -> Unit,
    onShowRecentReadingChanged: (Boolean) -> Unit,
    onShowFavoritesChanged: (Boolean) -> Unit,
    onShowGenresChanged: (Boolean) -> Unit,
    onHideStatusBarChanged: (Boolean) -> Unit,
    onGridColumnsChanged: (Int) -> Unit = {},
    onToggleTypeFilter: (BookType) -> Unit = {},
    onToggleGenreFilter: (String) -> Unit = {},
    onToggleLanguageFilter: (String) -> Unit = {},
    onToggleYearFilter: (String) -> Unit = {},
    onToggleCountryFilter: (String) -> Unit = {},
    onToggleReadingStatus: (ReadingStatus) -> Unit = {},
    onClearAdvancedFilters: () -> Unit = {},
    onExportSettings: suspend () -> String,
    onImportSettings: (String) -> Unit,
    onRecordLocalBackupExport: () -> Unit,
    onRecordLocalBackupImport: () -> Unit,
    onToggleReaderSearch: (Boolean) -> Unit,
    onToggleReaderListen: (Boolean) -> Unit,
    onToggleReaderAccessibility: (Boolean) -> Unit,
    onToggleReaderAnalytics: (Boolean) -> Unit,
    onToggleReaderExport: (Boolean) -> Unit,
    onReaderControlOrderChanged: (List<ReaderControl>) -> Unit,
    onReaderSettingsChanged: (ReaderSettings) -> Unit,
    onNotificationsEnabledChanged: (Boolean) -> Unit,
    onUpdateNotificationsEnabledChanged: (Boolean) -> Unit,
    onReadingReminderEnabledChanged: (Boolean) -> Unit,
    onReadingReminderTimeChanged: (Int, Int) -> Unit,
    onSendTestNotification: () -> Unit,
    updateUiState: AppUpdateUiState = AppUpdateUiState(),
    onCheckForUpdates: () -> Unit = {},
    onInstallLatestUpdate: () -> Unit = {},
    onToggleLatestChangelog: () -> Unit = {},
    onToggleReleaseHistory: () -> Unit = {},
    onExportAnnotations: (BookItem) -> Unit,
    onCreateCollection: (String, BookItem) -> Unit,
    onToggleBookInCollection: (String, BookItem) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onDeleteBook: (BookItem) -> Unit,
    onConsumeLibraryMessage: () -> Unit,
    onConsumePendingExportUri: () -> Unit,
    onConsumePendingCloudAuthUri: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showFilterSheet by remember { mutableStateOf(false) }
    var focusSearchRequestKey by remember { mutableIntStateOf(0) }
    var selectedBookForInfo by remember { mutableStateOf<BookItem?>(null) }
    var selectedBookForActions by remember { mutableStateOf<BookItem?>(null) }
    var pendingDeleteBook by remember { mutableStateOf<BookItem?>(null) }
    var isLogsRefreshing by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(HomeRootTab.LIBRARY) }
    var librarySearchVisible by remember { mutableStateOf(false) }
    var browseSearchVisible by remember { mutableStateOf(false) }
    var browseSearchQuery by remember { mutableStateOf("") }
    var settingsSearchVisible by remember { mutableStateOf(false) }
    var settingsSearchQuery by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val hideBetaFeatures = uiState.display.hideBetaFeatures
    val scope = rememberCoroutineScope()
    val hasActiveFilters = currentTab == HomeRootTab.LIBRARY && uiState.hasActiveLibraryFilters()

    BackHandler(enabled = currentTab != HomeRootTab.LIBRARY) {
        currentTab = currentTab.backDestination()
    }

    LaunchedEffect(currentTab) {
        if (currentTab != HomeRootTab.LIBRARY) {
            librarySearchVisible = false
        }
        if (currentTab != HomeRootTab.BROWSE) {
            browseSearchVisible = false
            browseSearchQuery = ""
        }
        if (currentTab != HomeRootTab.SETTINGS) {
            settingsSearchVisible = false
            settingsSearchQuery = ""
        }
    }

    BackHandler(enabled = hasActiveFilters) {
        if (uiState.searchQuery.isNotEmpty()) {
            onSearchChanged("")
        }
        if (uiState.selectedGenres.isNotEmpty() ||
            uiState.selectedTypes.isNotEmpty() ||
            uiState.selectedLanguages.isNotEmpty() ||
            uiState.selectedYears.isNotEmpty() ||
            uiState.selectedCountries.isNotEmpty() ||
            uiState.selectedStatuses.isNotEmpty()
        ) {
            onClearAdvancedFilters()
        }
    }

    BackHandler(
        enabled = currentTab == HomeRootTab.LIBRARY && librarySearchVisible && uiState.searchQuery.isEmpty()
    ) {
        librarySearchVisible = false
    }

    LaunchedEffect(libraryMessage) {
        val message = libraryMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        onConsumeLibraryMessage()
    }

    LaunchedEffect(pendingExportUri) {
        val exportUri = pendingExportUri ?: return@LaunchedEffect
        runCatching {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = context.contentResolver.getType(exportUri) ?: "text/markdown"
                putExtra(Intent.EXTRA_STREAM, exportUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share export"))
        }.onFailure {
            snackbarHostState.showSnackbar("Unable to share export")
        }
        onConsumePendingExportUri()
    }

    HomeDeleteDialog(
        pendingDeleteBook = pendingDeleteBook,
        onDeleteBook = onDeleteBook,
        onDismiss = { pendingDeleteBook = null }
    )

    fun shareBook(book: BookItem) {
        val mimeType = when (book.type) {
            BookType.EPUB, BookType.EPUB3 -> "application/epub+zip"
            BookType.PDF -> "application/pdf"
            BookType.MOBI -> "application/x-mobipocket-ebook"
            BookType.AZW3 -> "application/vnd.amazon.ebook"
            BookType.CBZ -> "application/vnd.comicbook+zip"
            BookType.CBR -> "application/vnd.comicbook-rar"
        }
        scope.launch {
            runCatching {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, Uri.parse(book.uri))
                    putExtra(Intent.EXTRA_SUBJECT, book.title)
                    putExtra(Intent.EXTRA_TEXT, "${book.title} by ${book.author}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share book"))
            }.onFailure {
                snackbarHostState.showSnackbar("Unable to share this book")
            }
        }
    }

    fun openBookFile(book: BookItem) {
        val mimeType = when (book.type) {
            BookType.EPUB, BookType.EPUB3 -> "application/epub+zip"
            BookType.PDF -> "application/pdf"
            BookType.MOBI -> "application/x-mobipocket-ebook"
            BookType.AZW3 -> "application/vnd.amazon.ebook"
            BookType.CBZ -> "application/vnd.comicbook+zip"
            BookType.CBR -> "application/vnd.comicbook-rar"
        }
        scope.launch {
            runCatching {
                val openIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(book.uri), mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(openIntent)
            }.recoverCatching {
                val chooserIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.parse(book.uri), "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(chooserIntent, "Open file"))
            }.onFailure {
                snackbarHostState.showSnackbar("No app available to open this file")
            }
        }
    }
    
    // Improved Status Bar handling to prevent layout jumps
    val statusBarColor = MaterialTheme.colorScheme.background
    val isLightStatusBar = statusBarColor.luminance() > 0.5f

    DisposableEffect(uiState.display.hideStatusBar, statusBarColor, isLightStatusBar) {
        val window = (context as? ComponentActivity)?.window ?: return@DisposableEffect onDispose {}
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        @Suppress("DEPRECATION")
        window.statusBarColor = statusBarColor.toArgb()
        insetsController.isAppearanceLightStatusBars = isLightStatusBar
        
        if (uiState.display.hideStatusBar) {
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        } else {
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
        
        onDispose { 
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars()) 
        }
    }

    val treePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            onLibraryAccessGranted(uri)
        }
    }

    HomeFilterSheet(
        show = showFilterSheet,
        uiState = uiState,
        onDismiss = { showFilterSheet = false },
        onToggleType = onToggleTypeFilter,
        onToggleGenre = onToggleGenreFilter,
        onToggleLanguage = onToggleLanguageFilter,
        onToggleYear = onToggleYearFilter,
        onToggleCountry = onToggleCountryFilter,
        onSortOrderChanged = onSortOrderChanged,
        onToggleReadingStatus = onToggleReadingStatus,
        onClearAdvancedFilters = onClearAdvancedFilters
    )

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (liquidGlassEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.background,
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                Color.Transparent
                            ),
                            radius = 900f
                        )
                    )
            )
        }

        LaunchedEffect(hideBetaFeatures) {
        if (hideBetaFeatures && currentTab == HomeRootTab.BROWSE) {
            currentTab = HomeRootTab.LIBRARY
        }
    }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets(0.dp),
            containerColor = Color.Transparent,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (navBarStyle == NavigationBarStyle.DEFAULT) {
                    HomeDefaultBottomBar(
                        currentTab = currentTab.ordinal,
                        onTabSelected = { currentTab = HomeRootTab.entries[it] },
                        liquidGlassEnabled = liquidGlassEnabled,
                        hideBetaFeatures = hideBetaFeatures
                    )
                }
            }
        ) { paddingValues ->
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                // Header - Always preserve status bar space to prevent "jumping" content
                val headerModifier = Modifier
                    .fillMaxWidth()
                    .stableStatusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)

                HomeScreenHeader(
                    currentTab = currentTab.ordinal,
                    uiState = uiState,
                    onFocusSearch = {
                        if (currentTab == HomeRootTab.LIBRARY) {
                            librarySearchVisible = true
                            focusSearchRequestKey += 1
                        }
                    },
                    browseSearchQuery = browseSearchQuery,
                    browseSearchVisible = browseSearchVisible,
                    onBrowseSearchVisibilityChange = { browseSearchVisible = it },
                    onBrowseSearchChanged = { browseSearchQuery = it },
                    onBrowseSearch = { query ->
                        val trimmed = query.trim()
                        if (trimmed.isNotEmpty()) {
                            onBrowseSearch(trimmed)
                        }
                    },
                    settingsSearchQuery = settingsSearchQuery,
                    settingsSearchVisible = settingsSearchVisible,
                    onSettingsSearchVisibilityChange = { settingsSearchVisible = it },
                    onSettingsSearchChanged = { settingsSearchQuery = it },
                    onShowFilter = { showFilterSheet = true },
                    hasActiveFilters = hasActiveFilters,
                    onClearLogs = { com.dyu.ereader.core.logging.AppLogger.clear() },
                    librarySearchVisible = librarySearchVisible,
                    onLibrarySearchVisibilityChange = { visible ->
                        librarySearchVisible = visible
                    },
                    onLibrarySearchClear = {
                        onSearchChanged("")
                    },
                    liquidGlassEnabled = liquidGlassEnabled,
                    modifier = headerModifier
                )

                Box(modifier = Modifier.weight(1f)) {
                    HomeScreenContent(
                        currentTab = currentTab,
                        uiState = uiState,
                        appTheme = appTheme,
                        appFont = appFont,
                        appAccent = appAccent,
                        customAccentColor = customAccentColor,
                        navBarStyle = navBarStyle,
                        liquidGlassEnabled = liquidGlassEnabled,
                        pendingCloudAuthUri = pendingCloudAuthUri,
                        librarySearchVisible = librarySearchVisible || uiState.searchQuery.isNotBlank(),
                        hideBetaFeatures = hideBetaFeatures,
                        isLogsRefreshing = isLogsRefreshing,
                        onLogsRefreshingChange = { isLogsRefreshing = it },
                        treePickerLauncher = treePickerLauncher,
                        focusSearchRequestKey = focusSearchRequestKey,
                        onSearchChanged = onSearchChanged,
                        onToggleFavorite = onToggleFavorite,
                        onOpenBook = onOpenBook,
                        onRefresh = onRefresh,
                        onToggleTypeFilter = onToggleTypeFilter,
                        onToggleGenreFilter = onToggleGenreFilter,
                        onToggleLanguageFilter = onToggleLanguageFilter,
                        onToggleYearFilter = onToggleYearFilter,
                        onToggleCountryFilter = onToggleCountryFilter,
                        onToggleReadingStatus = onToggleReadingStatus,
                        onClearAdvancedFilters = onClearAdvancedFilters,
                        onShowBookInfo = { selectedBookForInfo = it },
                        onDeleteBookRequest = { pendingDeleteBook = it },
                        onShowBookActions = { selectedBookForActions = it },
                        onDeleteCollection = onDeleteCollection,
                        onSortOrderChanged = onSortOrderChanged,
                        onToggleLayout = onToggleLayout,
                        onAppThemeChange = onAppThemeChange,
                        onAppFontChange = onAppFontChange,
                        onAppAccentChange = onAppAccentChange,
                        onAppCustomAccentColorChange = onAppCustomAccentColorChange,
                        onAppTextScaleChange = onAppTextScaleChange,
                        onNavigationBarStyleChange = onNavigationBarStyleChange,
                        onLiquidGlassToggle = onLiquidGlassToggle,
                        onAnimationsToggle = onAnimationsToggle,
                        onHapticsToggle = onHapticsToggle,
                        onTextScrollerToggle = onTextScrollerToggle,
                        onHideBetaFeaturesChanged = onHideBetaFeaturesChanged,
                        onDeveloperOptionsChanged = onDeveloperOptionsChanged,
                        onShowBookTypeChanged = onShowBookTypeChanged,
                        onShowRecentReadingChanged = onShowRecentReadingChanged,
                        onShowFavoritesChanged = onShowFavoritesChanged,
                        onShowGenresChanged = onShowGenresChanged,
                        onHideStatusBarChanged = onHideStatusBarChanged,
                        onGridColumnsChanged = onGridColumnsChanged,
                        onRevokeLibraryAccess = onRevokeLibraryAccess,
                        onNavigateLogs = { currentTab = HomeRootTab.LOGS },
                        settingsSearchQuery = settingsSearchQuery,
                        onExportSettings = onExportSettings,
                        onImportSettings = onImportSettings,
                        onRecordLocalBackupExport = onRecordLocalBackupExport,
                        onRecordLocalBackupImport = onRecordLocalBackupImport,
                        onToggleReaderSearch = onToggleReaderSearch,
                        onToggleReaderListen = onToggleReaderListen,
                        onToggleReaderAccessibility = onToggleReaderAccessibility,
                        onToggleReaderAnalytics = onToggleReaderAnalytics,
                        onToggleReaderExport = onToggleReaderExport,
                        onReaderControlOrderChanged = onReaderControlOrderChanged,
                        onReaderSettingsChanged = onReaderSettingsChanged,
                        onNotificationsEnabledChanged = onNotificationsEnabledChanged,
                        onUpdateNotificationsEnabledChanged = onUpdateNotificationsEnabledChanged,
                        onReadingReminderEnabledChanged = onReadingReminderEnabledChanged,
                        onReadingReminderTimeChanged = onReadingReminderTimeChanged,
                        onSendTestNotification = onSendTestNotification,
                        updateUiState = updateUiState,
                        onCheckForUpdates = onCheckForUpdates,
                        onInstallLatestUpdate = onInstallLatestUpdate,
                        onToggleLatestChangelog = onToggleLatestChangelog,
                        onToggleReleaseHistory = onToggleReleaseHistory,
                        onConsumePendingCloudAuthUri = onConsumePendingCloudAuthUri
                    )
                }
            }
        }

        // Custom Floating Navigation Bar - Liquid Glass Improved
        if (navBarStyle == NavigationBarStyle.FLOATING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                HomeFloatingBottomBar(
                    currentTab = currentTab.ordinal,
                    onTabSelected = { currentTab = HomeRootTab.entries[it] },
                    animationsEnabled = uiState.display.animationsEnabled,
                    liquidGlassEnabled = liquidGlassEnabled,
                    hideBetaFeatures = hideBetaFeatures,
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }
        }
        HomeDetailsOverlay(
            book = selectedBookForInfo,
            liquidGlassEnabled = liquidGlassEnabled,
            onClose = { selectedBookForInfo = null },
            onRead = {
                selectedBookForInfo?.let { onOpenBook(it) }
                selectedBookForInfo = null
            }
        )
        HomeBookActionsSheet(
            book = selectedBookForActions,
            collections = uiState.collections,
            liquidGlassEnabled = liquidGlassEnabled,
            onDismiss = { selectedBookForActions = null },
            onRead = { book ->
                onOpenBook(book)
                selectedBookForActions = null
            },
            onShowInfo = { book ->
                selectedBookForInfo = book
                selectedBookForActions = null
            },
            onToggleFavorite = { book -> onToggleFavorite(book.id, !book.isFavorite) },
            onShare = ::shareBook,
            onExportHighlights = onExportAnnotations,
            onOpenFile = ::openBookFile,
            onDelete = { book ->
                pendingDeleteBook = book
                selectedBookForActions = null
            },
            onCreateCollection = onCreateCollection,
            onToggleCollection = onToggleBookInCollection,
            onDeleteCollection = onDeleteCollection
        )
    }
}
