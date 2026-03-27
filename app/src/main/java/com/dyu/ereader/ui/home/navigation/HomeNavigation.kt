package com.dyu.ereader.ui.home.navigation

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.dyu.ereader.ui.app.MainViewModel
import com.dyu.ereader.ui.home.screens.HomeScreen
import com.dyu.ereader.ui.home.viewmodel.HomeViewModel

const val HOME_ROUTE = "home"

fun NavGraphBuilder.homeScreen(
    mainViewModel: MainViewModel,
    onNavigateToReader: (uri: String, typeName: String) -> Unit
) {
    composable(HOME_ROUTE) {
        val homeViewModel: HomeViewModel = hiltViewModel()

        val state by homeViewModel.uiState.collectAsState()
        val appearance by mainViewModel.appearance.collectAsState()
        val updateState by mainViewModel.appUpdateState.collectAsState()
        val pendingCloudAuthUri by mainViewModel.pendingCloudAuthUri.collectAsState()
        val libraryMessage by homeViewModel.libraryMessage.collectAsState()
        val pendingExportUri by homeViewModel.pendingExportUri.collectAsState()
        HomeScreen(
            uiState = state,
            appTheme = appearance.theme,
            appFont = appearance.appFont,
            appAccent = appearance.accent,
            customAccentColor = appearance.customAccentColor,
            navBarStyle = appearance.navBarStyle,
            liquidGlassEnabled = appearance.liquidGlassEnabled,
            libraryMessage = libraryMessage,
            pendingExportUri = pendingExportUri,
            pendingCloudAuthUri = pendingCloudAuthUri,
            onSearchChanged = homeViewModel::onSearchChanged,
            onBrowseSearch = { query ->
                homeViewModel.searchBrowse(query)
                homeViewModel.addBrowseSavedSearch(query)
            },
            onSortOrderChanged = homeViewModel::onSortOrderChanged,
            onRefresh = homeViewModel::refreshLibrary,
            onLibraryAccessGranted = homeViewModel::onLibraryAccessGranted,
            onRevokeLibraryAccess = homeViewModel::revokeLibraryAccess,
            onOpenBook = { book ->
                onNavigateToReader(book.uri, book.type.name)
            },
            onAppThemeChange = mainViewModel::setTheme,
            onAppFontChange = mainViewModel::setAppFont,
            onAppAccentChange = mainViewModel::setAccent,
            onAppCustomAccentColorChange = mainViewModel::setCustomAccentColor,
            onAppTextScaleChange = homeViewModel::onAppTextScaleChange,
            onLiquidGlassToggle = mainViewModel::setLiquidGlassEnabled,
            onNavigationBarStyleChange = mainViewModel::setNavigationBarStyle,
            onAnimationsToggle = homeViewModel::onAnimationsToggle,
            onHapticsToggle = homeViewModel::onHapticsToggle,
            onTextScrollerToggle = homeViewModel::onTextScrollerToggle,
            onHideBetaFeaturesChanged = homeViewModel::onHideBetaFeaturesChanged,
            onDeveloperOptionsChanged = homeViewModel::onDeveloperOptionsChanged,
            onToggleFavorite = homeViewModel::toggleFavorite,
            onToggleLayout = homeViewModel::toggleLayout,
            onShowBookTypeChanged = homeViewModel::onShowBookTypeChanged,
            onShowRecentReadingChanged = homeViewModel::onShowRecentReadingChanged,
            onShowFavoritesChanged = homeViewModel::onShowFavoritesChanged,
            onShowGenresChanged = homeViewModel::onShowGenresChanged,
            onHideStatusBarChanged = homeViewModel::onHideStatusBarChanged,
            onGridColumnsChanged = homeViewModel::onGridColumnsChanged,
            onToggleTypeFilter = homeViewModel::onToggleTypeFilter,
            onToggleGenreFilter = homeViewModel::onToggleGenreFilter,
            onToggleLanguageFilter = homeViewModel::onToggleLanguageFilter,
            onToggleYearFilter = homeViewModel::onToggleYearFilter,
            onToggleCountryFilter = homeViewModel::onToggleCountryFilter,
            onToggleReadingStatus = homeViewModel::onToggleReadingStatus,
            onClearAdvancedFilters = homeViewModel::clearAdvancedFilters,
            onExportSettings = homeViewModel::exportSettings,
            onImportSettings = homeViewModel::importSettings,
            onRecordLocalBackupExport = homeViewModel::recordLocalBackupExport,
            onRecordLocalBackupImport = homeViewModel::recordLocalBackupImport,
            onToggleReaderSearch = homeViewModel::onToggleReaderSearch,
            onToggleReaderListen = homeViewModel::onToggleReaderListen,
            onToggleReaderAccessibility = homeViewModel::onToggleReaderAccessibility,
            onToggleReaderAnalytics = homeViewModel::onToggleReaderAnalytics,
            onToggleReaderExport = homeViewModel::onToggleReaderExport,
            onReaderControlOrderChanged = homeViewModel::onReaderControlOrderChanged,
            onReaderSettingsChanged = homeViewModel::onReaderSettingsChanged,
            onNotificationsEnabledChanged = homeViewModel::onNotificationsEnabledChanged,
            onUpdateNotificationsEnabledChanged = homeViewModel::onUpdateNotificationsEnabledChanged,
            onReadingReminderEnabledChanged = homeViewModel::onReadingReminderEnabledChanged,
            onReadingReminderTimeChanged = homeViewModel::onReadingReminderTimeChanged,
            onSendTestNotification = homeViewModel::sendTestNotification,
            updateUiState = updateState,
            onCheckForUpdates = { mainViewModel.checkForUpdates(force = true) },
            onInstallLatestUpdate = mainViewModel::installLatestUpdate,
            onToggleLatestChangelog = mainViewModel::toggleLatestReleaseDetails,
            onToggleReleaseHistory = mainViewModel::toggleReleaseHistory,
            onPreviewUpdateState = mainViewModel::previewUpdateState,
            onExportAnnotations = homeViewModel::exportBookAnnotations,
            onCreateCollection = homeViewModel::createLibraryCollection,
            onToggleBookInCollection = homeViewModel::toggleBookInCollection,
            onDeleteCollection = homeViewModel::deleteLibraryCollection,
            onDeleteBook = homeViewModel::deleteBook,
            onConsumeLibraryMessage = homeViewModel::consumeLibraryMessage,
            onConsumePendingExportUri = homeViewModel::consumePendingExportUri,
            onConsumePendingCloudAuthUri = mainViewModel::consumePendingCloudAuthUri
        )
    }
}
