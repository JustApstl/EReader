package com.dyu.ereader.ui.components.dialogs

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun appDialogContainerColor(liquidGlassEnabled: Boolean = false): Color {
    return when {
        liquidGlassEnabled -> MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
}

@Composable
fun appDialogBorderColor(liquidGlassEnabled: Boolean = false): Color {
    return if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.26f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f)
    }
}

@Composable
fun appDialogCardColor(liquidGlassEnabled: Boolean = false): Color {
    return if (liquidGlassEnabled) {
        MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.88f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow
    }
}

@Composable
fun appDialogContentColor(): Color = MaterialTheme.colorScheme.onSurface

@Composable
fun appDialogSecondaryContentColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant

@Composable
fun appDialogTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    focusedTextColor = appDialogContentColor(),
    unfocusedTextColor = appDialogContentColor(),
    focusedLabelColor = appDialogSecondaryContentColor(),
    unfocusedLabelColor = appDialogSecondaryContentColor(),
    cursorColor = MaterialTheme.colorScheme.primary
)

fun appTextFieldShape(multiline: Boolean = false): Shape {
    return if (multiline) RoundedCornerShape(12.dp) else RoundedCornerShape(10.dp)
}
