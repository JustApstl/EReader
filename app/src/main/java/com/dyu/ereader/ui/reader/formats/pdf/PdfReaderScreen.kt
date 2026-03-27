package com.dyu.ereader.ui.reader.formats.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.ReadingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun PdfReaderScreen(
    uri: String,
    settings: ReaderSettings,
    initialProgress: Float = 0f,
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

    val initialPageIndex = remember(pageCount, initialProgress) {
        if (pageCount <= 1) 0 else {
            val raw = (initialProgress.coerceIn(0f, 1f) * (pageCount - 1)).toInt()
            raw.coerceIn(0, pageCount - 1)
        }
    }
    val cache = rememberPdfBitmapCache()

    if (settings.readingMode == ReadingMode.SCROLL) {
        val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPageIndex)

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
                PdfPage(
                    renderer = renderer!!,
                    index = index,
                    isContinuous = true,
                    cache = cache
                )
            }
        }
    } else {
        val zoomedPages = remember { mutableStateMapOf<Int, Boolean>() }
        val pagerState = rememberPagerState(
            initialPage = initialPageIndex,
            pageCount = { pageCount }
        )

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
            beyondViewportPageCount = 1,
            userScrollEnabled = zoomedPages[pagerState.currentPage] != true
        ) { pageIndex ->
            PdfPage(
                renderer = renderer!!,
                index = pageIndex,
                isContinuous = false,
                cache = cache,
                onZoomActiveChanged = { isZoomed ->
                    zoomedPages[pageIndex] = isZoomed
                }
            )
        }
    }
}

@Composable
private fun PdfPage(
    renderer: PdfRenderer,
    index: Int,
    isContinuous: Boolean,
    cache: LruCache<Long, Bitmap>,
    onZoomActiveChanged: (Boolean) -> Unit = {}
) {
    BoxWithConstraints(
        modifier = if (isContinuous) Modifier.fillMaxWidth() else Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val targetWidthPx = max(
            1,
            with(density) { maxWidth.toPx() }.roundToInt()
        )
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val cacheKey = (index.toLong() shl 32) or (targetWidthPx.toLong() and 0xFFFFFFFF)
        val cached = remember(cacheKey) { cache.get(cacheKey) }
        val bitmapState by produceState<Bitmap?>(initialValue = cached, renderer, index, targetWidthPx) {
            if (cached != null) return@produceState
            value = withContext(Dispatchers.IO) {
                renderPage(renderer, index, targetWidthPx)
            }
            value?.let { cache.put(cacheKey, it) }
        }
        var scale by remember(index) { mutableFloatStateOf(1f) }
        var offset by remember(index) { mutableStateOf(Offset.Zero) }

        LaunchedEffect(scale) {
            onZoomActiveChanged(scale > 1.01f)
        }

        DisposableEffect(Unit) {
            onDispose { onZoomActiveChanged(false) }
        }

        val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            val newScale = (scale * zoomChange).coerceIn(1f, 4f)
            val proposedOffset = if (newScale <= 1.01f) {
                Offset.Zero
            } else {
                Offset(
                    x = offset.x + panChange.x,
                    y = offset.y + panChange.y
                )
            }
            scale = newScale
            offset = clampPdfZoomOffset(
                offset = proposedOffset,
                scale = newScale,
                containerWidthPx = containerWidthPx,
                containerHeightPx = containerHeightPx,
                bitmap = bitmapState,
                isContinuous = isContinuous
            )
        }

        if (bitmapState == null) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        } else {
            Box(
                modifier = Modifier
                    .then(if (isContinuous) Modifier.fillMaxWidth() else Modifier.fillMaxSize())
                    .transformable(state = transformableState),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = bitmapState!!.asImageBitmap(),
                    contentDescription = "Page ${index + 1}",
                    modifier = (if (isContinuous) Modifier.fillMaxWidth() else Modifier.fillMaxSize())
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                    contentScale = if (isContinuous) ContentScale.FillWidth else ContentScale.Fit
                )
            }
        }
    }
}

@Composable
private fun rememberPdfBitmapCache(): LruCache<Long, Bitmap> {
    return remember {
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = max(1024, maxKb / 8)
        object : LruCache<Long, Bitmap>(cacheSize) {
            override fun sizeOf(key: Long, value: Bitmap): Int = value.byteCount / 1024
        }
    }
}

private fun renderPage(
    renderer: PdfRenderer,
    index: Int,
    targetWidthPx: Int
): Bitmap? {
    return runCatching {
        renderer.openPage(index).use { page ->
            val scale = targetWidthPx / page.width.toFloat()
            val width = (page.width * scale).roundToInt().coerceAtLeast(1)
            val height = (page.height * scale).roundToInt().coerceAtLeast(1)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bmp
        }
    }.getOrNull()
}

private fun clampPdfZoomOffset(
    offset: Offset,
    scale: Float,
    containerWidthPx: Float,
    containerHeightPx: Float,
    bitmap: Bitmap?,
    isContinuous: Boolean
): Offset {
    if (bitmap == null || scale <= 1.01f) return Offset.Zero

    val baseSize = if (isContinuous) {
        val width = containerWidthPx
        val height = width * (bitmap.height.toFloat() / bitmap.width.toFloat())
        width to height
    } else {
        val fitScale = min(
            containerWidthPx / bitmap.width.toFloat(),
            containerHeightPx / bitmap.height.toFloat()
        )
        val width = bitmap.width * fitScale
        val height = bitmap.height * fitScale
        width to height
    }

    val extraWidth = ((baseSize.first * scale) - containerWidthPx).coerceAtLeast(0f) / 2f
    val extraHeight = ((baseSize.second * scale) - containerHeightPx).coerceAtLeast(0f) / 2f

    return Offset(
        x = offset.x.coerceIn(-extraWidth, extraWidth),
        y = offset.y.coerceIn(-extraHeight, extraHeight)
    )
}
