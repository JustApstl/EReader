package com.dyu.ereader.ui.components.surfaces

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dyu.ereader.ui.app.theme.UiTokens

@Composable
fun SectionSurface(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    shape: Shape = UiTokens.SectionShape,
    border: BorderStroke? = null,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    contentPadding: PaddingValues = PaddingValues(UiTokens.SectionPadding),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = color,
        border = border,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        Column(modifier = Modifier.padding(contentPadding), content = content)
    }
}
