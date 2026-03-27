package com.dyu.ereader.ui.reader.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.ui.components.insets.stableStatusBarsPadding
import kotlin.math.roundToInt

@Composable
internal fun BoxScope.ReaderBookmarkIndicator(
    show: Boolean,
    showChrome: Boolean
) {
    if (!show) return

    val topOffset = if (showChrome) 72.dp else 12.dp
    Surface(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .stableStatusBarsPadding()
            .padding(top = topOffset, end = 16.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.Bookmark,
                contentDescription = "Bookmarked Page",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Saved",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
internal fun ReaderHiddenChromeProgress(
    visible: Boolean,
    navBarStyle: NavigationBarStyle,
    hideStatusBar: Boolean,
    isPageMode: Boolean,
    progress: Float,
    currentPage: Int,
    totalPages: Int
) {
    if (!visible) return

    val bottomPadding = if (navBarStyle == NavigationBarStyle.DEFAULT) 52.dp else 12.dp
    val topPadding = if (navBarStyle == NavigationBarStyle.DEFAULT && !hideStatusBar) 48.dp else 12.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding, top = topPadding)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            val progressText = if (isPageMode && totalPages > 0) {
                "PAGE ${currentPage.coerceAtLeast(1)} OF $totalPages  •  ${(progress * 100).roundToInt()}% COMPLETE"
            } else {
                "${(progress * 100).roundToInt()}% COMPLETE"
            }
            Text(
                text = progressText,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
internal fun BoxScope.ReaderFocusHandle(
    visible: Boolean,
    showChrome: Boolean,
    onToggleChrome: () -> Unit
) {
    if (!visible) return

    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val gestureBarBottomPadding by animateDpAsState(
        targetValue = if (showChrome) 140.dp + navBarPadding else 24.dp + navBarPadding,
        label = "gestureBarPadding"
    )

    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = gestureBarBottomPadding)
            .width(100.dp)
            .height(32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggleChrome
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(4.dp)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    shape = CircleShape
                )
        )
    }
}
