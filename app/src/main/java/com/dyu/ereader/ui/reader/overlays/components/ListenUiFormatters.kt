package com.dyu.ereader.ui.reader.overlays.components

internal fun formatRemainingTime(remainingMs: Long): String {
    val totalSeconds = (remainingMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%02d:%02d left".format(minutes, seconds)
}
