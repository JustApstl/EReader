package com.dyu.ereader.ui.tts

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.dyu.ereader.ui.reader.ReaderViewModel
import com.dyu.ereader.data.model.VoiceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TTSReader(
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel,
    onDismiss: () -> Unit
) {
    val isSpeaking by viewModel.isSpeaking.collectAsState(initial = false)
    val currentText by viewModel.currentTTSText.collectAsState(initial = "")
    val playbackSpeed by viewModel.ttsSpeed.collectAsState(initial = 1.0f)
    val currentVoice by viewModel.currentVoice.collectAsState(initial = null)
    val availableVoices by viewModel.availableVoices.collectAsState(initial = emptyList())

    // Pulsing animation for "reading" indicator
    val pulsing = rememberInfiniteTransition()
    val pulse by pulsing.animateFloat(
        initialValue = 1f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            )
    ) {
        // Header
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
            tonalElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = pulse),
                        tonalElevation = 4.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = "Reading",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    Text(
                        if (isSpeaking) "Now Reading..." else "Ready",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, "Close")
                }
            }
        }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(
                            width = 1.dp,
                            color = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                Text(
                    text = currentText.ifEmpty { "No text being read" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(24.dp),
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        // Controls
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Speed Control
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Rounded.Speed, null, modifier = Modifier.size(20.dp))
                Slider(
                    value = playbackSpeed,
                    onValueChange = { viewModel.setTTSSpeed(it) },
                    valueRange = 0.5f..2.0f,
                    steps = 7,
                    modifier = Modifier.weight(1f)
                )
                Text("${String.format("%.1f", playbackSpeed)}x", style = MaterialTheme.typography.bodyMedium)
            }

            // Voice Selection
            if (availableVoices.isNotEmpty()) {
                var voiceExpanded by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Rounded.RecordVoiceOver, null, modifier = Modifier.size(20.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        ExposedDropdownMenuBox(
                            expanded = voiceExpanded,
                            onExpandedChange = { voiceExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = currentVoice ?: "Default",
                                onValueChange = {},
                                readOnly = true,
                                modifier = Modifier.menuAnchor(),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(voiceExpanded) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = voiceExpanded,
                                onDismissRequest = { voiceExpanded = false }
                            ) {
                                availableVoices.forEach { voice: VoiceInfo ->
                                    DropdownMenuItem(
                                        text = { Text(voice.name) },
                                        onClick = {
                                            viewModel.setTTSVoice(voice.name)
                                            voiceExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { viewModel.pauseTTS() },
                    enabled = isSpeaking,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Pause, "Pause")
                }

                FilledTonalIconButton(
                    onClick = { if (isSpeaking) viewModel.pauseTTS() else viewModel.resumeTTS() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(if (isSpeaking) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, if (isSpeaking) "Pause" else "Play")
                }

                FilledTonalIconButton(
                    onClick = { viewModel.stopTTS() },
                    enabled = isSpeaking,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Stop, "Stop")
                }
            }

            // Start Reading Button
            Button(
                onClick = {
                    // This would trigger reading the current visible page
                    viewModel.startTTS("Replace this with actual selected text from epub.js")
                },
                enabled = !isSpeaking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.PlayArrow, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Start Reading")
            }
        }
    }
}