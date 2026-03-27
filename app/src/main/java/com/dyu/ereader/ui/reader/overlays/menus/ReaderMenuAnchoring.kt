package com.dyu.ereader.ui.reader.overlays.menus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import com.dyu.ereader.ui.reader.controls.menus.anchoredDropdownPositionProvider

@Composable
internal fun rememberReaderMenuPositionProvider(
    x: Float,
    y: Float,
    screenWidthDp: Dp,
    screenHeightDp: Dp
): PopupPositionProvider {
    val density = LocalDensity.current
    val marginPx = with(density) { 10.dp.roundToPx() }
    val verticalGapPx = with(density) { 10.dp.roundToPx() }
    val screenWidthPx = with(density) { screenWidthDp.roundToPx() }
    val screenHeightPx = with(density) { screenHeightDp.roundToPx() }
    val anchorX = if (!x.isNaN() && !x.isInfinite()) x.toInt() else screenWidthPx / 2
    val anchorY = if (!y.isNaN() && !y.isInfinite()) y.toInt() else screenHeightPx / 3

    return remember(anchorX, anchorY, marginPx, verticalGapPx) {
        anchoredDropdownPositionProvider(
            anchorX = anchorX,
            anchorY = anchorY,
            marginPx = marginPx,
            verticalGapPx = verticalGapPx
        )
    }
}
