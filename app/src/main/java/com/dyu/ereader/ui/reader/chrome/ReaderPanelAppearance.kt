package com.dyu.ereader.ui.reader.chrome

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class ReaderPanelAppearance(
    val ambientMode: Boolean = false,
    val readerBackground: Color = Color.Unspecified
)

val LocalReaderPanelAppearance = staticCompositionLocalOf { ReaderPanelAppearance() }

@Composable
fun ProvideReaderPanelAppearance(
    ambientMode: Boolean,
    readerBackground: Color,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalReaderPanelAppearance provides ReaderPanelAppearance(
            ambientMode = ambientMode,
            readerBackground = readerBackground
        ),
        content = content
    )
}

@Composable
fun readerPanelSectionColor(): Color {
    val appearance = LocalReaderPanelAppearance.current
    return if (appearance.ambientMode && appearance.readerBackground != Color.Unspecified) {
        appearance.readerBackground.copy(alpha = 0.86f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
}

@Composable
fun readerPanelAltSurfaceColor(): Color {
    val appearance = LocalReaderPanelAppearance.current
    return if (appearance.ambientMode && appearance.readerBackground != Color.Unspecified) {
        appearance.readerBackground.copy(alpha = 0.92f)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
}

@Composable
fun readerPanelHeaderChipColor(): Color {
    val appearance = LocalReaderPanelAppearance.current
    return if (appearance.ambientMode && appearance.readerBackground != Color.Unspecified) {
        appearance.readerBackground.copy(alpha = 0.74f)
    } else {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f)
    }
}
