package com.dyu.ereader.ui.reader.chrome

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalConfiguration
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.ui.home.state.HomeDisplayPreferences
import com.dyu.ereader.ui.components.insets.stableStatusBarsPadding
import com.dyu.ereader.ui.reader.controls.bars.ReaderBottomBarContent
import com.dyu.ereader.ui.reader.controls.bars.ReaderTopBarContent
import com.dyu.ereader.ui.reader.state.ReaderUiState

@Composable
internal fun ReaderTopChrome(
    visible: Boolean,
    navBarStyle: NavigationBarStyle,
    uiState: ReaderUiState,
    onBack: () -> Unit,
    showSearchAction: Boolean,
    onShowSearch: () -> Unit,
    onShowChapters: () -> Unit,
    onAddBookmark: () -> Unit,
    autoReadEnabled: Boolean,
    isListenReady: Boolean,
    onToggleAutoRead: () -> Unit,
    onShowListen: () -> Unit,
    isPageMode: Boolean,
    hasBookmarkOnPage: Boolean,
    ambientMode: Boolean,
    readerBackground: Color
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        if (navBarStyle == NavigationBarStyle.FLOATING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .stableStatusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = if (ambientMode) readerBackground.copy(alpha = 0.76f) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
                ) {
                    ReaderTopBarContent(
                        uiState = uiState,
                        onBack = onBack,
                        showSearchAction = showSearchAction,
                        onShowSearch = onShowSearch,
                        onShowChapters = onShowChapters,
                        onAddBookmark = onAddBookmark,
                        autoReadEnabled = autoReadEnabled,
                        isListenReady = isListenReady,
                        onToggleAutoRead = onToggleAutoRead,
                        onShowListen = onShowListen,
                        isPageMode = isPageMode,
                        hasBookmarkOnPage = hasBookmarkOnPage
                    )
                }
            }
        } else {
            val topShape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .stableStatusBarsPadding()
                    .shadow(4.dp, topShape),
                color = if (ambientMode) readerBackground.copy(alpha = 0.88f) else MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
                shape = topShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
            ) {
                ReaderTopBarContent(
                    uiState = uiState,
                    onBack = onBack,
                    showSearchAction = showSearchAction,
                    onShowSearch = onShowSearch,
                    onShowChapters = onShowChapters,
                    onAddBookmark = onAddBookmark,
                    autoReadEnabled = autoReadEnabled,
                    isListenReady = isListenReady,
                    onToggleAutoRead = onToggleAutoRead,
                    onShowListen = onShowListen,
                    isPageMode = isPageMode,
                    hasBookmarkOnPage = hasBookmarkOnPage
                )
            }
        }
    }
}

@Composable
internal fun ReaderBottomChrome(
    visible: Boolean,
    navBarStyle: NavigationBarStyle,
    liquidGlassEnabled: Boolean,
    uiState: ReaderUiState,
    displayPrefs: HomeDisplayPreferences,
    activeActionId: String? = null,
    onHome: () -> Unit,
    onShowSettings: () -> Unit,
    onShowSearch: () -> Unit,
    onShowListen: () -> Unit,
    onShowAccessibility: () -> Unit,
    onShowAnalytics: () -> Unit,
    onShowExport: () -> Unit,
    onProgressChange: (Float) -> Unit,
    dockedPanelContent: (@Composable () -> Unit)?,
    ambientMode: Boolean,
    readerBackground: Color,
    modifier: Modifier = Modifier
) {
    val dockedPanelMaxHeight = (LocalConfiguration.current.screenHeightDp * 0.62f).dp

    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        if (navBarStyle == NavigationBarStyle.FLOATING) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .navigationBarsPadding()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    color = if (liquidGlassEnabled) {
                        if (ambientMode) readerBackground.copy(alpha = 0.72f) else MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f)
                    } else {
                        if (ambientMode) readerBackground.copy(alpha = 0.88f) else MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
                ) {
                    Column {
                        if (dockedPanelContent != null) {
                            ProvideReaderPanelAppearance(
                                ambientMode = ambientMode,
                                readerBackground = readerBackground
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = dockedPanelMaxHeight)
                                        .padding(top = 8.dp, bottom = 4.dp)
                                ) {
                                    dockedPanelContent()
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                        }
                        ReaderBottomBarContent(
                            uiState = uiState,
                            onHome = onHome,
                            displayPrefs = displayPrefs,
                            activeActionId = activeActionId,
                            onShowSettings = onShowSettings,
                            onShowSearch = onShowSearch,
                            onShowListen = onShowListen,
                            onShowAccessibility = onShowAccessibility,
                            onShowAnalytics = onShowAnalytics,
                            onShowExport = onShowExport,
                            onProgressChange = onProgressChange
                        )
                    }
                }
            }
        } else {
            val bottomShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .shadow(4.dp, bottomShape),
                color = if (ambientMode) readerBackground.copy(alpha = 0.9f) else MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 0.dp,
                shape = bottomShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f))
            ) {
                Column {
                    if (dockedPanelContent != null) {
                        ProvideReaderPanelAppearance(
                            ambientMode = ambientMode,
                            readerBackground = readerBackground
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = dockedPanelMaxHeight)
                                    .padding(top = 8.dp, bottom = 4.dp)
                            ) {
                                dockedPanelContent()
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
                    }
                    ReaderBottomBarContent(
                        uiState = uiState,
                        onHome = onHome,
                        displayPrefs = displayPrefs,
                        activeActionId = activeActionId,
                        onShowSettings = onShowSettings,
                        onShowSearch = onShowSearch,
                        onShowListen = onShowListen,
                        onShowAccessibility = onShowAccessibility,
                        onShowAnalytics = onShowAnalytics,
                        onShowExport = onShowExport,
                        onProgressChange = onProgressChange
                    )
                }
            }
        }
    }
}
