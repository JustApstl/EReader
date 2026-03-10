package com.dyu.ereader.ui.tts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dyu.ereader.ui.reader.ReaderViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TTSControls(
    modifier: Modifier = Modifier,
    viewModel: ReaderViewModel = hiltViewModel()
) {
    val isSpeaking by viewModel.isSpeaking.collectAsState(initial = false)
    val playbackSpeed by viewModel.ttsSpeed.collectAsState(initial = 1.0f)
    val availableVoices by viewModel.availableVoices.collectAsState(initial = emptyList())
    val currentVoice by viewModel.currentVoice.collectAsState(initial = null)
    val isReady by viewModel.isTTSReady.collectAsState(initial = false)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                "Text-to-Speech",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!isReady) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(4.dp), color = MaterialTheme.colorScheme.primary)
                    Text("Initializing voice engine...", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Voice selection - Refined Dropdown
            if (availableVoices.isNotEmpty()) {
                var expanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = currentVoice ?: "System Default",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Playback Voice") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(
                            type = ExposedDropdownMenuAnchorType.PrimaryNotEditable,
                            enabled = true
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        availableVoices.forEach { voice ->
                            DropdownMenuItem(
                                text = { Text("${voice.name} (${voice.locale})", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    viewModel.setTTSVoice(voice.name)
                                    expanded = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = MaterialTheme.colorScheme.onSurface,
                                    leadingIconColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // Speed control
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("Reading Speed", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("${"%.1f".format(playbackSpeed)}x", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold)
                }
                Slider(
                    value = playbackSpeed,
                    onValueChange = { viewModel.setTTSSpeed(it) },
                    valueRange = 0.5f..2.5f,
                    steps = 14,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                )
            }

            // Playback controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                IconButton(onClick = { /* Rewind */ }, enabled = isReady) {
                    Icon(Icons.Rounded.Replay10, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                
                FilledIconButton(
                    onClick = {
                        if (isSpeaking) viewModel.pauseTTS() else viewModel.startTTSFromCurrentPage()
                    },
                    enabled = isReady,
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Icon(
                        imageVector = if (isSpeaking) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Playback",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                IconButton(onClick = { viewModel.stopTTS() }, enabled = isReady) {
                    Icon(Icons.Rounded.Stop, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.error)
                }
                
                IconButton(onClick = { /* Forward */ }, enabled = isReady) {
                    Icon(Icons.Rounded.Forward10, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
