package com.dyu.ereader.ui.reader

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.ReaderSettings
import com.dyu.ereader.data.model.ReadingMode
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import java.io.FileOutputStream

@Composable
fun PdfReaderScreen(
    uri: String,
    settings: ReaderSettings,
    onProgressChanged: (Float) -> Unit,
    onPaginationChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var renderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    DisposableEffect(uri) {
        val file = File(context.cacheDir, "temp_pdf_${System.currentTimeMillis()}.pdf")
        try {
            context.contentResolver.openInputStream(android.net.Uri.parse(uri))?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            val fd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val pdfRenderer = PdfRenderer(fd)
            renderer = pdfRenderer
            pageCount = pdfRenderer.pageCount
        } catch (e: Exception) {
            error = e.message ?: "Failed to load PDF"
        }

        onDispose {
            renderer?.close()
            file.delete()
        }
    }

    if (error != null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = error!!, modifier = Modifier.padding(16.dp))
        }
        return
    }

    if (renderer == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (settings.readingMode == ReadingMode.SCROLL) {
        val listState = rememberLazyListState()

        LaunchedEffect(listState, pageCount) {
            snapshotFlow { listState.firstVisibleItemIndex }
                .distinctUntilChanged()
                .collect { index ->
                    if (pageCount > 0) {
                        onPaginationChanged(index + 1, pageCount)
                        onProgressChanged(index.toFloat() / (pageCount - 1).coerceAtLeast(1))
                    }
                }
        }

        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize()
        ) {
            items(pageCount) { index ->
                PdfPage(renderer!!, index, isContinuous = true)
            }
        }
    } else {
        val pagerState = rememberPagerState(pageCount = { pageCount })

        LaunchedEffect(pagerState, pageCount) {
            snapshotFlow { pagerState.currentPage }
                .distinctUntilChanged()
                .collect { page ->
                    if (pageCount > 0) {
                        onPaginationChanged(page + 1, pageCount)
                        onProgressChanged(page.toFloat() / (pageCount - 1).coerceAtLeast(1))
                    }
                }
        }

        HorizontalPager(
            state = pagerState,
            modifier = modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { pageIndex ->
            PdfPage(renderer!!, pageIndex, isContinuous = false)
        }
    }
}

@Composable
private fun PdfPage(renderer: PdfRenderer, index: Int, isContinuous: Boolean) {
    val bitmap = remember(index) {
        renderer.openPage(index).use { page ->
            val width = page.width * 2 // Increase resolution
            val height = page.height * 2
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp
        }
    }

    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = "Page ${index + 1}",
        modifier = if (isContinuous) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
        contentScale = if (isContinuous) ContentScale.FillWidth else ContentScale.Fit
    )
}
