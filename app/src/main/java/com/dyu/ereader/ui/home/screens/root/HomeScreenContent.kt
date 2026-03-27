package com.dyu.ereader.ui.home.screens

import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.app.AppAccent
import com.dyu.ereader.data.model.app.AppFont
import com.dyu.ereader.data.model.app.AppTheme
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.data.model.library.BookItem
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.data.model.reader.ReaderControl
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.update.AppUpdateUiState
import com.dyu.ereader.ui.browse.screens.BrowseCatalogScreen
import com.dyu.ereader.ui.home.components.LibraryContent
import com.dyu.ereader.ui.home.components.LogsArea
import com.dyu.ereader.ui.home.settings.SettingsArea
import com.dyu.ereader.ui.home.settings.SettingsEvents
import com.dyu.ereader.ui.home.settings.cloud.CloudSyncScreen
import com.dyu.ereader.ui.home.state.HomeUiState
import com.dyu.ereader.ui.home.state.ReadingStatus
import com.dyu.ereader.ui.home.state.SortOrder
import com.dyu.ereader.core.logging.AppLogger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreenContent(
    currentTab: HomeRootTab,
    uiState: HomeUiState,
    appTheme: AppTheme,
    appFont: AppFont,
    appAccent: AppAccent,
    customAccentColor: Int?,
    navBarStyle: NavigationBarStyle,
    liquidGlassEnabled: Boolean,
    pendingCloudAuthUri: Uri?,
    librarySearchVisible: Boolean,
    hideBetaFeatures: Boolean,
    isLogsRefreshing: Boolean,
    onLogsRefreshingChange: (Boolean) -> Unit,
    treePickerLauncher: ActivityResultLauncher<Uri?>,
    focusSearchRequestKey: Int,
    onSearchChanged: (String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onOpenBook: (BookItem) -> Unit,
    onRefresh: () -> Unit,
    onToggleTypeFilter: (BookType) -> Unit,
    onToggleGenreFilter: (String) -> Unit,
    onToggleLanguageFilter: (String) -> Unit,
    onToggleYearFilter: (String) -> Unit,
    onToggleCountryFilter: (String) -> Unit,
    onToggleReadingStatus: (ReadingStatus) -> Unit,
    onClearAdvancedFilters: () -> Unit,
    onShowBookInfo: (BookItem) -> Unit,
    onDeleteBookRequest: (BookItem) -> Unit,
    onShowBookActions: (BookItem) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onToggleLayout: () -> Unit,
    onAppThemeChange: (AppTheme) -> Unit,
    onAppFontChange: (AppFont) -> Unit,
    onAppAccentChange: (AppAccent) -> Unit,
    onAppCustomAccentColorChange: (Int?) -> Unit,
    onAppTextScaleChange: (Float) -> Unit,
    onNavigationBarStyleChange: (NavigationBarStyle) -> Unit,
    onLiquidGlassToggle: (Boolean) -> Unit,
    onAnimationsToggle: (Boolean) -> Unit,
    onHapticsToggle: (Boolean) -> Unit,
    onTextScrollerToggle: (Boolean) -> Unit,
    onHideBetaFeaturesChanged: (Boolean) -> Unit,
    onDeveloperOptionsChanged: (Boolean) -> Unit,
    onShowBookTypeChanged: (Boolean) -> Unit,
    onShowRecentReadingChanged: (Boolean) -> Unit,
    onShowFavoritesChanged: (Boolean) -> Unit,
    onShowGenresChanged: (Boolean) -> Unit,
    onHideStatusBarChanged: (Boolean) -> Unit,
    onGridColumnsChanged: (Int) -> Unit,
    onRevokeLibraryAccess: () -> Unit,
    onNavigateLogs: () -> Unit,
    settingsSearchQuery: String,
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
    updateUiState: AppUpdateUiState,
    onCheckForUpdates: () -> Unit,
    onInstallLatestUpdate: () -> Unit,
    onToggleLatestChangelog: () -> Unit,
    onToggleReleaseHistory: () -> Unit,
    onConsumePendingCloudAuthUri: () -> Unit
) {
    when (currentTab) {
        HomeRootTab.BROWSE -> {
            if (!hideBetaFeatures) {
                BrowseCatalogScreen(
                    modifier = Modifier.fillMaxSize(),
                    liquidGlassEnabled = liquidGlassEnabled
                )
            } else {
                HomeLibraryPane(
                    uiState = uiState,
                    appTheme = appTheme,
                    liquidGlassEnabled = liquidGlassEnabled,
                    librarySearchVisible = librarySearchVisible,
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
                    onShowBookInfo = onShowBookInfo,
                    onDeleteBookRequest = onDeleteBookRequest,
                    onShowBookActions = onShowBookActions,
                    onDeleteCollection = onDeleteCollection,
                    onSortOrderChanged = onSortOrderChanged,
                    onToggleLayout = onToggleLayout
                )
            }
        }
        HomeRootTab.SETTINGS -> {
            SettingsArea(
                modifier = Modifier.fillMaxSize(),
                uiState = uiState,
                appTheme = appTheme,
                appFont = appFont,
                appAccent = appAccent,
                customAccentColor = customAccentColor,
                navBarStyle = navBarStyle,
                liquidGlassEnabled = liquidGlassEnabled,
                pendingCloudAuthUri = pendingCloudAuthUri,
                searchQuery = settingsSearchQuery,
                updateUiState = updateUiState,
                events = SettingsEvents(
                    onAppThemeChange = onAppThemeChange,
                    onAppFontChange = onAppFontChange,
                    onAppAccentChange = onAppAccentChange,
                    onAppCustomAccentColorChange = onAppCustomAccentColorChange,
                    onAppTextScaleChange = onAppTextScaleChange,
                    onLiquidGlassToggle = onLiquidGlassToggle,
                    onNavigationBarStyleChange = onNavigationBarStyleChange,
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
                    onSelectFolder = { treePickerLauncher.launch(null) },
                    onRevokeAccess = onRevokeLibraryAccess,
                    onShowLogs = onNavigateLogs,
                    onExportSettings = onExportSettings,
                    onImportSettings = onImportSettings,
                    onRecordLocalBackupExport = onRecordLocalBackupExport,
                    onRecordLocalBackupImport = onRecordLocalBackupImport,
                    onToggleReaderSearch = onToggleReaderSearch,
                    onToggleReaderListen = onToggleReaderListen,
                    onToggleReaderAccessibility = onToggleReaderAccessibility,
                    onToggleReaderAnalytics = onToggleReaderAnalytics,
                    onToggleReaderExport = onToggleReaderExport,
                    onReaderControlOrderChange = onReaderControlOrderChanged,
                    onReaderSettingsChanged = onReaderSettingsChanged,
                    onNotificationsEnabledChanged = onNotificationsEnabledChanged,
                    onUpdateNotificationsEnabledChanged = onUpdateNotificationsEnabledChanged,
                    onReadingReminderEnabledChanged = onReadingReminderEnabledChanged,
                    onReadingReminderTimeChanged = onReadingReminderTimeChanged,
                    onSendTestNotification = onSendTestNotification,
                    onCheckForUpdates = onCheckForUpdates,
                    onInstallLatestUpdate = onInstallLatestUpdate,
                    onToggleLatestChangelog = onToggleLatestChangelog,
                    onToggleReleaseHistory = onToggleReleaseHistory
                ),
                onConsumePendingCloudAuthUri = onConsumePendingCloudAuthUri
            )
        }
        HomeRootTab.CLOUD -> {
            CloudSyncScreen(
                modifier = Modifier.fillMaxSize(),
                liquidGlassEnabled = liquidGlassEnabled,
                pendingCloudAuthUri = pendingCloudAuthUri,
                onConsumePendingCloudAuthUri = onConsumePendingCloudAuthUri
            )
        }
        HomeRootTab.LOGS -> {
            HomeLogsPane(
                isRefreshing = isLogsRefreshing,
                onRefreshingChange = onLogsRefreshingChange,
                liquidGlassEnabled = liquidGlassEnabled
            )
        }
        HomeRootTab.LIBRARY -> {
            HomeLibraryPane(
                uiState = uiState,
                appTheme = appTheme,
                liquidGlassEnabled = liquidGlassEnabled,
                librarySearchVisible = librarySearchVisible,
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
                onShowBookInfo = onShowBookInfo,
                onDeleteBookRequest = onDeleteBookRequest,
                onShowBookActions = onShowBookActions,
                onDeleteCollection = onDeleteCollection,
                onSortOrderChanged = onSortOrderChanged,
                onToggleLayout = onToggleLayout
            )
        }
    }
}

@Composable
private fun HomeLibraryPane(
    uiState: HomeUiState,
    appTheme: AppTheme,
    liquidGlassEnabled: Boolean,
    librarySearchVisible: Boolean,
    treePickerLauncher: ActivityResultLauncher<Uri?>,
    focusSearchRequestKey: Int,
    onSearchChanged: (String) -> Unit,
    onToggleFavorite: (String, Boolean) -> Unit,
    onOpenBook: (BookItem) -> Unit,
    onRefresh: () -> Unit,
    onToggleTypeFilter: (BookType) -> Unit,
    onToggleGenreFilter: (String) -> Unit,
    onToggleLanguageFilter: (String) -> Unit,
    onToggleYearFilter: (String) -> Unit,
    onToggleCountryFilter: (String) -> Unit,
    onToggleReadingStatus: (ReadingStatus) -> Unit,
    onClearAdvancedFilters: () -> Unit,
    onShowBookInfo: (BookItem) -> Unit,
    onDeleteBookRequest: (BookItem) -> Unit,
    onShowBookActions: (BookItem) -> Unit,
    onDeleteCollection: (String) -> Unit,
    onSortOrderChanged: (SortOrder) -> Unit,
    onToggleLayout: () -> Unit
) {
    val libraryListState = androidx.compose.runtime.saveable.rememberSaveable(
        saver = androidx.compose.foundation.lazy.LazyListState.Saver
    ) {
        androidx.compose.foundation.lazy.LazyListState()
    }

    LibraryContent(
        uiState = uiState,
        appTheme = appTheme,
        liquidGlassEnabled = liquidGlassEnabled,
        librarySearchVisible = librarySearchVisible,
        listState = libraryListState,
        treePickerLauncher = treePickerLauncher,
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
        onShowBookInfo = onShowBookInfo,
        onDeleteBook = onDeleteBookRequest,
        onShowBookActions = onShowBookActions,
        onDeleteCollection = onDeleteCollection,
        onSortOrderChanged = onSortOrderChanged,
        onToggleLayout = onToggleLayout,
        focusSearchRequestKey = focusSearchRequestKey
    )
}

@Composable
private fun HomeLogsPane(
    isRefreshing: Boolean,
    onRefreshingChange: (Boolean) -> Unit,
    liquidGlassEnabled: Boolean
) {
    val refreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        state = refreshState,
        onRefresh = {
            scope.launch {
                onRefreshingChange(true)
                AppLogger.refresh()
                delay(600)
                onRefreshingChange(false)
            }
        },
        indicator = {
            PullToRefreshDefaults.Indicator(
                modifier = Modifier.align(Alignment.TopCenter),
                isRefreshing = isRefreshing,
                state = refreshState,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                color = MaterialTheme.colorScheme.primary
            )
        }
    ) {
        LogsArea(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp, start = 20.dp, end = 20.dp, top = 8.dp),
            liquidGlassEnabled = liquidGlassEnabled
        )
    }
}
