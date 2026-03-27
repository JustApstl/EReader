package com.dyu.ereader.ui.reader.controls.bars

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.ui.reader.state.ReaderUiState

@Composable
fun ReaderTopBarContent(
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
    hasBookmarkOnPage: Boolean
) {
    val titleText = uiState.title.trim()
    val subtitleText = uiState.author.trim().ifBlank { null }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 14.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ReaderTopActionButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "Back",
            highlighted = false,
            onClick = onBack
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            if (titleText.isNotBlank()) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!subtitleText.isNullOrBlank()) {
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showSearchAction) {
                ReaderTopActionButton(
                    icon = Icons.Rounded.Search,
                    contentDescription = "Search",
                    highlighted = false,
                    onClick = onShowSearch
                )
            }
            ReaderTopActionButton(
                icon = Icons.Rounded.Headset,
                contentDescription = if (autoReadEnabled) "Stop Listen" else "Listen",
                highlighted = autoReadEnabled || !isListenReady,
                onClick = onShowListen
            )
            if (isPageMode) {
                ReaderTopActionButton(
                    icon = Icons.Rounded.Bookmark,
                    contentDescription = if (hasBookmarkOnPage) "Remove Bookmark" else "Add Bookmark",
                    highlighted = hasBookmarkOnPage,
                    onClick = onAddBookmark
                )
            }
            ReaderTopActionButton(
                icon = Icons.Rounded.Menu,
                contentDescription = "Reader Menu",
                highlighted = false,
                onClick = onShowChapters
            )
        }
    }
}

@Composable
private fun ReaderTopActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    highlighted: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(28.dp),
        shape = CircleShape,
        color = if (highlighted) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = BorderStroke(
            1.dp,
            if (highlighted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(17.dp),
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    highlighted -> MaterialTheme.colorScheme.onSecondaryContainer
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
