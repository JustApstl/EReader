package com.dyu.ereader.ui.components.dialogs

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
import androidx.compose.foundation.BorderStroke
import androidx.core.graphics.ColorUtils
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.dialogs.appDialogBorderColor
import com.dyu.ereader.ui.components.dialogs.appDialogContainerColor
import com.dyu.ereader.ui.components.dialogs.appDialogContentColor

@Composable
fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
    initialColor: Int?,
    onColorPreview: (Int) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val hasInitialColor = initialColor != null
    val color = initialColor ?: 0xFFFDFCFB.toInt()
    var didApply by remember { mutableStateOf(false) }
    
    val initialHsl = remember(color) {
        val out = FloatArray(3)
        ColorUtils.colorToHSL(color, out)
        if (!hasInitialColor) {
            out[1] = 0.5f
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
    val contentColor = appDialogContentColor()

    val handleDismiss = {
        if (!didApply) {
            onCancel()
        }
        onDismiss()
    }

    // Live preview while sliding (skip initial to avoid auto-adjust flash)
    var didInitialPreview by remember { mutableStateOf(false) }
    LaunchedEffect(currentColorInt) {
        if (!didInitialPreview) {
            didInitialPreview = true
            return@LaunchedEffect
        }
        onColorPreview(currentColorInt)
    }

    Dialog(onDismissRequest = handleDismiss) {
        Surface(
            shape = UiTokens.SettingsCardShape,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, appDialogBorderColor()),
            color = appDialogContainerColor(),
            contentColor = contentColor,
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
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                
                Spacer(Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(currentColor)
                        .border(1.dp, appDialogBorderColor(), CircleShape)
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
                    TextButton(onClick = handleDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            didApply = true
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
    onValueChangeFinished: ((Float) -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float>,
    trackBrush: Brush
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isDragging by interactionSource.collectIsPressedAsState()
    var internalValue by remember { mutableFloatStateOf(value.coerceIn(valueRange.start, valueRange.endInclusive)) }

    LaunchedEffect(value, isDragging) {
        if (!isDragging) {
            internalValue = value.coerceIn(valueRange.start, valueRange.endInclusive)
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val displayValue = if (valueRange.endInclusive > 1f) internalValue.toInt().toString() else "${(internalValue * 100).toInt()}%"
            Text(
                displayValue,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Box(modifier = Modifier.fillMaxWidth().height(32.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)) {
                drawRect(brush = trackBrush)
            }
            Slider(
                value = internalValue,
                onValueChange = {
                    internalValue = it.coerceIn(valueRange.start, valueRange.endInclusive)
                    onValueChange(internalValue)
                },
                onValueChangeFinished = {
                    onValueChangeFinished?.invoke(internalValue)
                },
                valueRange = valueRange,
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.Transparent,
                    inactiveTrackColor = Color.Transparent,
                    thumbColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.fillMaxWidth(),
                interactionSource = interactionSource
            )
        }
    }
}
