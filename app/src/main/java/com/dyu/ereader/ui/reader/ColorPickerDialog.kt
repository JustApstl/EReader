package com.dyu.ereader.ui.reader

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.ColorUtils

@Composable
fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
    initialColor: Int?
) {
    val color = initialColor ?: 0xFFFDFCFB.toInt()
    
    val initialHsl = remember(color) {
        val out = FloatArray(3)
        ColorUtils.colorToHSL(color, out)
        if (initialColor == null) {
            out[2] = 0.5f
        }
        out
    }

    var hue by remember { mutableFloatStateOf(initialHsl[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsl[1]) }
    var lightness by remember { mutableFloatStateOf(initialHsl[2]) }

    val currentColorInt = remember(hue, saturation, lightness) {
        ColorUtils.HSLToColor(floatArrayOf(hue, saturation, lightness))
    }
    val currentColor = Color(currentColorInt)

    // Automatically update the theme color while sliding
    LaunchedEffect(currentColorInt) {
        onColorSelected(currentColorInt)
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Pick Color",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                )
                
                Spacer(Modifier.height(20.dp))
                
                HslSlider(
                    label = "Hue",
                    value = hue,
                    onValueChange = { hue = it },
                    valueRange = 0f..360f,
                    trackBrush = Brush.horizontalGradient(
                        colors = listOf(Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red)
                    )
                )
                
                HslSlider(
                    label = "Saturation",
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f,
                    trackBrush = Brush.horizontalGradient(
                        colors = listOf(Color.Gray, currentColor.copy(alpha = 1f))
                    )
                )
                
                HslSlider(
                    label = "Lightness",
                    value = lightness,
                    onValueChange = { lightness = it },
                    valueRange = 0f..1f,
                    trackBrush = Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color.Gray, Color.White)
                    )
                )
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onColorSelected(currentColorInt)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}

@Composable
private fun HslSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    trackBrush: Brush
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val displayValue = if (valueRange.endInclusive > 1f) value.toInt().toString() else "${(value * 100).toInt()}%"
            Text(displayValue, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
        
        Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)) {
                drawRect(brush = trackBrush)
            }
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
