package com.dyu.ereader.ui.reader.overlays.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.tts.ListenSleepTimerMode
import com.dyu.ereader.ui.components.buttons.AppChromeIconButton
import com.dyu.ereader.ui.components.inputs.appSliderColors
import com.dyu.ereader.ui.reader.settings.components.FilterChip as ReaderSettingsFilterChip

@Composable
fun ListenDockedPanel(
    isSpeaking: Boolean,
    playbackSpeed: Float,
    isReady: Boolean,
    autoReadEnabled: Boolean,
    currentSentence: String,
    sleepTimerMode: ListenSleepTimerMode,
    sleepTimerRemainingMs: Long?,
    onSpeedChange: (Float) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStartAutoRead: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onSleepTimerModeChange: (ListenSleepTimerMode) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ReaderPanelScaffold(
        title = "Listen",
        icon = Icons.Rounded.Headset,
        onDismiss = onDismiss,
        closeContentDescription = "Close Listen",
        modifier = modifier
            .padding(vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        headerActions = {
            if (sleepTimerMode != ListenSleepTimerMode.OFF) {
                AssistChip(
                    onClick = {},
                    enabled = false,
                    label = {
                        Text(
                            when (sleepTimerMode) {
                                ListenSleepTimerMode.MINUTES_10,
                                ListenSleepTimerMode.MINUTES_20 -> sleepTimerRemainingMs?.let(::formatRemainingTime)
                                    ?: sleepTimerMode.label
                                ListenSleepTimerMode.END_OF_PAGE -> "After page"
                                ListenSleepTimerMode.OFF -> ""
                            }
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        disabledContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f),
                        disabledLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        disabledLeadingIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }
    ) {
        ReaderSheetSection(
            title = "Playback",
            icon = Icons.Rounded.PlayArrow
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currentSentence.ifBlank { if (isReady) "Playback starts from the current page." else "Initializing narration..." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isReady) Icons.Rounded.GraphicEq else Icons.Rounded.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isReady) "System voice ready" else "Loading voice engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilledIconButton(
                            onClick = {
                                if (isSpeaking) {
                                    onPause()
                                } else if (autoReadEnabled || currentSentence.isNotBlank()) {
                                    onResume()
                                } else {
                                    onStartAutoRead()
                                }
                            },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = if (isSpeaking) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isSpeaking) "Pause Listen" else "Play Listen",
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        AppChromeIconButton(
                            icon = Icons.Rounded.Replay,
                            contentDescription = "Reset Listen",
                            onClick = onReset,
                            size = 38.dp,
                            iconSize = 18.dp
                        )

                        AppChromeIconButton(
                            icon = Icons.Rounded.Stop,
                            contentDescription = "Stop Listen",
                            onClick = onStop,
                            destructive = true,
                            size = 38.dp,
                            iconSize = 18.dp
                        )
                    }
                }
            }
        }

        ReaderSheetSection(
            title = "Playback Speed",
            icon = Icons.Rounded.Speed
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Playback speed",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "${"%.2f".format(playbackSpeed)}x",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Slider(
                value = playbackSpeed,
                onValueChange = onSpeedChange,
                valueRange = 0.5f..2.5f,
                steps = 14,
                colors = appSliderColors()
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                    ReaderSettingsFilterChip(
                        selected = kotlin.math.abs(playbackSpeed - speed) < 0.01f,
                        onClick = { onSpeedChange(speed) },
                        label = "${"%.2f".format(speed)}x"
                    )
                }
            }
        }

        ReaderSheetSection(
            title = "Sleep Timer",
            icon = Icons.Rounded.Schedule
        ) {
            Text(
                text = "Sleep timer",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ListenSleepTimerMode.entries.forEach { mode ->
                    ReaderSettingsFilterChip(
                        selected = sleepTimerMode == mode,
                        onClick = { onSleepTimerModeChange(mode) },
                        label = mode.label,
                        icon = if (sleepTimerMode == mode && mode != ListenSleepTimerMode.OFF) {
                            Icons.Rounded.Schedule
                        } else {
                            null
                        }
                    )
                }
            }
        }
    }
}
