package com.dyu.ereader.ui.reader.formats.azw3

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.dyu.ereader.ui.reader.formats.unsupported.UnsupportedFormatScreen

@Composable
fun Azw3ReaderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    UnsupportedFormatScreen(
        onBack = onBack,
        title = "AZW3 isn’t supported yet.",
        message = "Try converting this book to EPUB or PDF to read it here.",
        modifier = modifier
    )
}
