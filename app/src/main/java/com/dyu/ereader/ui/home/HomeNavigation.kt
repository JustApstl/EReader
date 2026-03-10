package com.dyu.ereader.ui.home

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.dyu.ereader.ui.MainViewModel

const val HOME_ROUTE = "home"

fun NavGraphBuilder.homeScreen(
    mainViewModel: MainViewModel,
    onNavigateToReader: (uri: String, typeName: String) -> Unit
) {
    composable(HOME_ROUTE) {
        val homeViewModel: HomeViewModel = hiltViewModel()

        val state by homeViewModel.uiState.collectAsState()
        val appTheme by mainViewModel.theme.collectAsState()
        val navBarStyle by mainViewModel.navBarStyle.collectAsState()
        val liquidGlassEnabled by mainViewModel.liquidGlassEnabled.collectAsState()

        HomeScreen(
            uiState = state,
            appTheme = appTheme,
            navBarStyle = navBarStyle,
            liquidGlassEnabled = liquidGlassEnabled,
            onSearchChanged = homeViewModel::onSearchChanged,
            onSortOrderChanged = homeViewModel::onSortOrderChanged,
            onRefresh = homeViewModel::refreshLibrary,
            onLibraryAccessGranted = homeViewModel::onLibraryAccessGranted,
            onRevokeLibraryAccess = homeViewModel::revokeLibraryAccess,
            onOpenBook = { book ->
                onNavigateToReader(book.uri, book.type.name)
            },
            onAppThemeChange = mainViewModel::setTheme,
            onLiquidGlassToggle = mainViewModel::setLiquidGlassEnabled,
            onNavigationBarStyleChange = mainViewModel::setNavigationBarStyle,
            onAnimationsToggle = homeViewModel::onAnimationsToggle,
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
            onClearAdvancedFilters = homeViewModel::clearAdvancedFilters,
            onExportSettings = homeViewModel::exportSettings,
            onImportSettings = homeViewModel::importSettings,
            onToggleReaderSearch = homeViewModel::onToggleReaderSearch,
            onToggleReaderTTS = homeViewModel::onToggleReaderTTS,
            onToggleReaderAccessibility = homeViewModel::onToggleReaderAccessibility,
            onToggleReaderAnalytics = homeViewModel::onToggleReaderAnalytics,
            onToggleReaderExport = homeViewModel::onToggleReaderExport,
            onReaderControlOrderChanged = homeViewModel::onReaderControlOrderChanged
        )
    }
}
