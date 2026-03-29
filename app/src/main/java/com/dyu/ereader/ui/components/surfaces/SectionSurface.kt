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
    color: Color = MaterialTheme.colorScheme.surface,
    shape: Shape = UiTokens.SectionShape,
    border: BorderStroke? = null,
    tonalElevation: Dp = UiTokens.SectionTonalElevation,
    shadowElevation: Dp = UiTokens.SectionShadowElevation,
    contentPadding: PaddingValues = PaddingValues(UiTokens.SectionPadding),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val resolvedBorder = border ?: BorderStroke(
        1.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    )
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            color = color,
            border = resolvedBorder,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = shape,
            color = color,
            border = resolvedBorder,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}
