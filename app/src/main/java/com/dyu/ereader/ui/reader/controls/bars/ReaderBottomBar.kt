package com.dyu.ereader.ui.reader.controls.bars

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Accessibility
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.dyu.ereader.data.model.reader.ReaderControl
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.ui.components.inputs.appSliderColors
import com.dyu.ereader.ui.home.state.HomeDisplayPreferences
import com.dyu.ereader.ui.reader.state.ReaderUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun ReaderBottomBarContent(
    uiState: ReaderUiState,
    onHome: () -> Unit,
    displayPrefs: HomeDisplayPreferences,
    activeActionId: String? = null,
    onShowSettings: () -> Unit,
    onShowSearch: () -> Unit,
    onShowListen: () -> Unit,
    onShowAccessibility: () -> Unit,
    onShowAnalytics: () -> Unit,
    onShowExport: () -> Unit,
    onProgressChange: (Float) -> Unit
) {
    @Suppress("NAME_SHADOWING")
    val onProgressChange = rememberUpdatedState(onProgressChange)
    val scope = rememberCoroutineScope()
    var dragJob by remember { mutableStateOf<Job?>(null) }
    val totalPages = uiState.totalPages
    val isPageMode = uiState.settings.readingMode == ReadingMode.PAGE
    val targetProgress = if (isPageMode) {
        if (totalPages > 1) pageProgressFor(uiState.currentPage, totalPages) else uiState.progress
    } else {
        uiState.progress
    }
    var sliderValue by remember { mutableFloatStateOf(targetProgress.coerceIn(0f, 1f)) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(targetProgress, isDragging) {
        if (!isDragging) {
            sliderValue = targetProgress.coerceIn(0f, 1f)
        }
    }

    val displayProgress = if (isDragging) sliderValue else targetProgress
    val percentCompleteLabel = "${(displayProgress * 100).roundToInt()}% COMPLETE"
    val pageProgressLabel = if (isPageMode) {
        val total = totalPages.coerceAtLeast(1)
        val pageIndex = if (totalPages > 1) {
            uiState.currentPage.coerceIn(1, total)
        } else {
            1
        }
        "PAGE ${pageIndex.coerceIn(1, total)} OF $total"
    } else {
        ""
    }

    Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pageProgressLabel,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = percentCompleteLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (uiState.chapters.size > 1) {
                    val chapterMarkers = uiState.chapters.size.coerceAtMost(12)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(chapterMarkers) { index ->
                            val ratio = if (chapterMarkers <= 1) {
                                0f
                            } else {
                                index.toFloat() / (chapterMarkers - 1).toFloat()
                            }
                            val passed = displayProgress + 0.02f >= ratio
                            Spacer(
                                modifier = Modifier
                                    .width(if (passed) 12.dp else 6.dp)
                                    .height(3.dp)
                                    .background(
                                        if (passed) {
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
                                        } else {
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                        },
                                        RoundedCornerShape(50)
                                    )
                            )
                        }
                    }
                }

                Slider(
                    value = sliderValue,
                    onValueChange = {
                        isDragging = true
                        sliderValue = it
                        dragJob?.cancel()
                        dragJob = scope.launch {
                            delay(90)
                            onProgressChange.value(sliderValue.coerceIn(0f, 1f))
                        }
                    },
                    onValueChangeFinished = {
                        dragJob?.cancel()
                        isDragging = false
                        onProgressChange.value(sliderValue.coerceIn(0f, 1f))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(22.dp),
                    colors = appSliderColors()
                )
            }
        }

        val readerControlActions = if (displayPrefs.hideBetaFeatures) {
            emptyList()
        } else {
            buildList<ReaderBottomAction> {
                val orderedReaderControls = if (displayPrefs.readerControlOrder.isEmpty()) {
                    ReaderControl.defaultOrder()
                } else {
                    displayPrefs.readerControlOrder
                }
                orderedReaderControls.forEach { control ->
                    when (control) {
                        ReaderControl.SEARCH -> Unit
                        ReaderControl.LISTEN -> if (displayPrefs.showReaderListen) {
                            add(ReaderBottomAction("listen", Icons.Rounded.Headset, "Listen", activeActionId == "listen", onShowListen))
                        }
                        ReaderControl.ACCESSIBILITY -> if (displayPrefs.showReaderAccessibility) {
                            add(ReaderBottomAction("accessibility", Icons.Rounded.Accessibility, "Access", activeActionId == "accessibility", onShowAccessibility))
                        }
                        ReaderControl.ANALYTICS -> if (displayPrefs.showReaderAnalytics) {
                            add(ReaderBottomAction("analytics", Icons.Rounded.Analytics, "Stats", activeActionId == "analytics", onShowAnalytics))
                        }
                        ReaderControl.EXPORT_HIGHLIGHT -> if (displayPrefs.showReaderExport) {
                            add(ReaderBottomAction("export", Icons.Rounded.Share, "Export", activeActionId == "export", onShowExport))
                        }
                    }
                }
            }
        }

        val actions = buildList<ReaderBottomAction> {
            add(ReaderBottomAction("home", Icons.Rounded.Home, "Home", false, onHome))
            add(ReaderBottomAction("settings", Icons.Rounded.Settings, "Settings", activeActionId == "settings", onShowSettings))
            addAll(readerControlActions)
        }
        val actionScroll = rememberScrollState()

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.horizontalScroll(actionScroll),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions.forEach { action ->
                    ReaderBottomActionTile(action = action)
                }
            }
        }
    }
}

@Composable
private fun ReaderBottomActionTile(action: ReaderBottomAction) {
    Surface(
        onClick = action.onClick,
        shape = CircleShape,
        color = if (action.highlighted) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        border = BorderStroke(
            1.dp,
            if (action.highlighted) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
            }
        )
    ) {
        Box(
            modifier = Modifier.size(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                action.icon,
                action.label,
                modifier = Modifier.size(18.dp),
                tint = if (action.highlighted) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

internal data class ReaderBottomAction(
    val id: String,
    val icon: ImageVector,
    val label: String,
    val highlighted: Boolean,
    val onClick: () -> Unit
)

private fun pageProgressFor(currentPage: Int, totalPages: Int): Float {
    if (totalPages <= 1) return 0f
    val clamped = currentPage.coerceIn(1, totalPages)
    return (clamped - 1).toFloat() / (totalPages - 1).toFloat()
}
