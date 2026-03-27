package com.dyu.ereader.ui.reader.overlays.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.tts.ListenSleepTimerMode

@Composable
fun ListenMiniPlayer(
    currentSentence: String,
    speed: Float,
    isSpeaking: Boolean,
    autoReadEnabled: Boolean,
    sleepTimerMode: ListenSleepTimerMode,
    sleepTimerRemainingMs: Long?,
    readerBackground: Color,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!autoReadEnabled && !isSpeaking) return

    val backgroundInfluence = if (readerBackground.alpha > 0f) readerBackground.copy(alpha = 0.12f) else Color.Transparent
    val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f).compositeOver(backgroundInfluence)
    val contentColor = if (containerColor.luminance() > 0.56f) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface
    val secondaryContentColor = contentColor.copy(alpha = 0.72f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isSpeaking) "Listen" else "Paused",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (sleepTimerMode != ListenSleepTimerMode.OFF) {
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            Text(
                                when (sleepTimerMode) {
                                    ListenSleepTimerMode.MINUTES_10,
                                    ListenSleepTimerMode.MINUTES_20 -> sleepTimerRemainingMs?.let(::formatRemainingTime) ?: sleepTimerMode.label
                                    ListenSleepTimerMode.END_OF_PAGE -> "Stops after page"
                                    ListenSleepTimerMode.OFF -> ""
                                }
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            disabledLabelColor = contentColor,
                            disabledLeadingIconContentColor = contentColor
                        )
                    )
                }
            }

            Text(
                text = currentSentence.ifBlank { "Preparing narration…" },
                style = MaterialTheme.typography.bodyMedium,
                color = secondaryContentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {
                        val next = when {
                            speed < 1.0f -> 1.0f
                            speed < 1.25f -> 1.25f
                            speed < 1.5f -> 1.5f
                            speed < 1.75f -> 1.75f
                            speed < 2.0f -> 2.0f
                            else -> 0.75f
                        }
                        onSpeedChange(next)
                    },
                    label = { Text("${"%.2f".format(speed)}x") },
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Speed,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        labelColor = contentColor,
                        leadingIconContentColor = contentColor
                    )
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    FilledIconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isSpeaking) "Pause Listen" else "Resume Listen",
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Stop,
                            contentDescription = "Stop Listen",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}
