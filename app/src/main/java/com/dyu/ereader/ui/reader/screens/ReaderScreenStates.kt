package com.dyu.ereader.ui.reader.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.dyu.ereader.data.model.reader.ReaderTheme
import com.dyu.ereader.ui.reader.state.ReaderUiState
import kotlin.math.roundToInt

@Composable
internal fun ReaderBackgroundImage(uiState: ReaderUiState) {
    if (uiState.settings.readerTheme == ReaderTheme.IMAGE && uiState.settings.backgroundImageUri != null) {
        AsyncImage(
            model = uiState.settings.backgroundImageUri,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    alpha = uiState.settings.backgroundImageOpacity,
                    scaleX = uiState.settings.backgroundImageZoom,
                    scaleY = uiState.settings.backgroundImageZoom
                )
                .blur(uiState.settings.backgroundImageBlur.dp),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    }
}

@Composable
internal fun ReaderLoadingState(
    progress: Float,
    readerBackground: Color,
    contrastingContentColor: Color
) {
    val resolvedBackground = if (readerBackground.alpha == 0f) {
        MaterialTheme.colorScheme.background
    } else {
        readerBackground
    }
    val cardColor = if (resolvedBackground.luminance() > 0.5f) {
        MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.94f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f)
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = cardColor,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)
            ) {
                CircularProgressIndicator(
                    progress = { if (progress > 0) progress else 0f },
                    strokeWidth = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = contrastingContentColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(44.dp)
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Opening book...",
                    style = MaterialTheme.typography.titleSmall,
                    color = contrastingContentColor
                )
                if (progress > 0f) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "${(progress * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = contrastingContentColor.copy(alpha = 0.68f)
                    )
                }
            }
        }
    }
}

@Composable
internal fun ReaderErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Button(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
            Text("Retry")
        }
    }
}
