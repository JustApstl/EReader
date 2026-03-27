package com.dyu.ereader.ui.reader.formats.epub

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.ActionMode
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.dyu.ereader.ui.app.theme.UiTokens
import com.dyu.ereader.ui.components.dialogs.appDialogContainerColor
import com.dyu.ereader.data.local.db.HighlightEntity
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.data.model.reader.ReaderElementStyle
import com.dyu.ereader.data.model.search.SearchResult
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.getBackgroundColor
import com.dyu.ereader.data.model.reader.getColor
import com.dyu.ereader.ui.reader.state.Chapter
import com.dyu.ereader.ui.reader.state.PageTurnDirection
import org.json.JSONArray
import org.json.JSONObject

private fun ReaderElementStyle.toJson(): JSONObject = JSONObject().apply {
    put("fontFamily", if (font == com.dyu.ereader.data.model.reader.ReaderFont.DEFAULT) "" else font.cssFamily)
    put("color", color?.let { String.format("#%06X", 0xFFFFFF and it) } ?: "")
}

private fun ReaderSettings.elementStylesJson(): JSONObject = JSONObject().apply {
    put("paragraph", elementStyles.paragraph.toJson())
    put("heading1", elementStyles.heading1.toJson())
    put("heading2", elementStyles.heading2.toJson())
    put("heading3", elementStyles.heading3.toJson())
    put("heading4", elementStyles.heading4.toJson())
    put("heading5", elementStyles.heading5.toJson())
    put("heading6", elementStyles.heading6.toJson())
    put("externalLink", elementStyles.externalLink.toJson())
    put("internalLink", elementStyles.internalLink.toJson())
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubJsReader(
    bookUri: String,
    initialProgress: Float,
    initialCfi: String?,
    settings: ReaderSettings,
    highlights: List<HighlightEntity>,
    marginNotes: List<MarginNoteEntity>,
    isDarkTheme: Boolean,
    onProgressChanged: (Float, String?) -> Unit,
    onToggleMenu: () -> Unit,
    onTextSelected: (String, String, String, Float, Float) -> Unit,
    onHighlightClicked: (Long, Float, Float) -> Unit,
    onMarginNoteClicked: (Long, Float, Float) -> Unit = { _, _, _ -> },
    onImageClick: (String) -> Unit = {},
    onChaptersLoaded: (List<Chapter>) -> Unit,
    onTextExtracted: (String) -> Unit = {},
    onPaginationChanged: (Int, Int) -> Unit = { _, _ -> },
    onStartListen: (String) -> Unit = {},
    onTapZone: (String) -> Unit = {},
    onSearchResults: (List<SearchResult>) -> Unit = {},
    onLoadingProgressChange: (Float) -> Unit = {},
    isListenSpeaking: Boolean = false,
    listenActive: Boolean = false,
    listenWordIndex: Int? = null,
    isSelectionMenuVisible: Boolean = false,
    isHighlightMenuVisible: Boolean = false,
    isMarginNoteMenuVisible: Boolean = false,
    pendingSearchQuery: String? = null,
    searchRequestId: Long = 0L,
    onSearchRequestConsumed: () -> Unit = {},
    pendingJumpHref: String? = null,
    pendingProgressJump: Float? = null,
    pendingPageTurn: PageTurnDirection? = null,
    requestTextExtraction: Boolean = false,
    onJumpConsumed: () -> Unit = {},
    onPageTurnConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var pendingUrl by remember { mutableStateOf<String?>(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    
    val currentSettingsJson = remember(settings, isDarkTheme) {
        val backgroundColorInt = settings.readerTheme.getBackgroundColor(isDarkTheme, settings.customBackgroundColor).toArgb()
        val bgR = (backgroundColorInt shr 16 and 0xFF) / 255.0
        val bgG = (backgroundColorInt shr 8 and 0xFF) / 255.0
        val bgB = (backgroundColorInt and 0xFF) / 255.0
        val maxChannel = maxOf(bgR, bgG, bgB)
        val minChannel = minOf(bgR, bgG, bgB)
        val bgLightness = (maxChannel + minChannel) / 2.0
        val textColorHex = if (settings.autoFontColor) {
            val adapt = if (bgLightness > 0.5) {
                // Light background -> keep hue family but make text significantly darker.
                Triple(bgR * 0.18, bgG * 0.18, bgB * 0.18)
            } else {
                // Dark background -> lighten toward white while preserving color tone.
                Triple(
                    bgR + (1.0 - bgR) * 0.82,
                    bgG + (1.0 - bgG) * 0.82,
                    bgB + (1.0 - bgB) * 0.82
                )
            }
            val r = (adapt.first.coerceIn(0.0, 1.0) * 255).toInt()
            val g = (adapt.second.coerceIn(0.0, 1.0) * 255).toInt()
            val b = (adapt.third.coerceIn(0.0, 1.0) * 255).toInt()
            String.format("#%02X%02X%02X", r, g, b)
        } else {
            settings.fontColorTheme.getColor(settings.customFontColor)?.let {
                String.format("#%06X", 0xFFFFFF and it.toArgb())
            } ?: "inherit"
        }
        val backgroundColorHex = String.format("#%06X", 0xFFFFFF and backgroundColorInt)

        JSONObject().apply {
            put("readingMode", settings.readingMode.name)
            put("fontSize", settings.fontSizeSp)
            put("fontFamily", settings.font.cssFamily)
            put("customFontUri", settings.customFontUri ?: "")
            put("lineHeight", settings.lineSpacing)
            put("textColor", textColorHex)
            put("autoFontColor", settings.autoFontColor)
            put("backgroundColor", backgroundColorHex)
            put("margin", settings.horizontalMarginDp)
            put("focusText", settings.focusText)
            put("focusTextBoldness", settings.focusTextBoldness)
            put("focusTextEmphasis", settings.focusTextEmphasis)
            put("focusTextColor", settings.focusTextColor?.let { String.format("#%06X", 0xFFFFFF and it) } ?: "")
            put("readerTheme", settings.readerTheme.name)
            put("backgroundImageUri", settings.backgroundImageUri)
            put("backgroundImageBlur", settings.backgroundImageBlur)
            put("backgroundImageOpacity", settings.backgroundImageOpacity)
            put("imageFilter", settings.imageFilter.name)
            put("usePublisherStyle", settings.usePublisherStyle)
            put("underlineLinks", settings.underlineLinks)
            put("textShadow", settings.textShadow)
            put("textShadowColor", settings.textShadowColor?.let { String.format("#%06X", 0xFFFFFF and it) } ?: "")
            put("pageTurn3d", settings.pageTurn3d)
            put("invertPageTurns", settings.invertPageTurns)
            put("pageTransitionStyle", settings.pageTransitionStyle.name)
            put("textAlignment", settings.textAlignment.cssValue)
            put("elementStyles", settings.elementStylesJson())
        }.toString()
    }

    val currentSettingsJsonProvider = rememberUpdatedState(currentSettingsJson)
    val onProgressChangedProvider = rememberUpdatedState(onProgressChanged)
    val onToggleMenuProvider = rememberUpdatedState(onToggleMenu)
    val onTextSelectedProvider = rememberUpdatedState(onTextSelected)
    val onHighlightClickedProvider = rememberUpdatedState(onHighlightClicked)
    val onMarginNoteClickedProvider = rememberUpdatedState(onMarginNoteClicked)
    val onImageClickProvider = rememberUpdatedState(onImageClick)
    val initialProgressProvider = rememberUpdatedState(initialProgress)
    val initialCfiProvider = rememberUpdatedState(initialCfi)
    val onChaptersLoadedProvider = rememberUpdatedState(onChaptersLoaded)
    val onTextExtractedProvider = rememberUpdatedState(onTextExtracted)
    val onPaginationChangedProvider = rememberUpdatedState(onPaginationChanged)
    val onStartListenProvider = rememberUpdatedState(onStartListen)
    val onTapZoneProvider = rememberUpdatedState(onTapZone)
    val onLoadingProgressChangeProvider = rememberUpdatedState(onLoadingProgressChange)
    val onSearchResultsProvider = rememberUpdatedState(onSearchResults)
    var lastToggleMenuAt by remember { mutableLongStateOf(0L) }
    var lastImageClickAt by remember { mutableLongStateOf(0L) }

    val dispatchToggleMenu = {
        val now = SystemClock.elapsedRealtime()
        if (now - lastToggleMenuAt >= 220L) {
            lastToggleMenuAt = now
            onToggleMenuProvider.value()
        }
    }
    val dispatchImageClick: (String) -> Unit = { url ->
        if (url.isNotBlank()) {
            val now = SystemClock.elapsedRealtime()
            if (now - lastImageClickAt >= 260L) {
                lastImageClickAt = now
                onImageClickProvider.value(url)
            }
        }
    }

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    val mapPointToWindow: (Float, Float) -> Pair<Float, Float> = { x, y ->
        val safeX = if (x.isNaN() || x.isInfinite()) 0f else x
        val safeY = if (y.isNaN() || y.isInfinite()) 0f else y
        val webView = webViewInstance
        if (webView == null) {
            safeX to safeY
        } else {
            val location = IntArray(2)
            webView.getLocationInWindow(location)
            (safeX + location[0]) to (safeY + location[1])
        }
    }

    val renderHighlights = {
        val json = JSONArray()
        highlights.forEach { hl ->
            json.put(JSONObject().apply {
                put("id", hl.id)
                put("selectionJson", hl.selectionJson)
                put("color", hl.color)
            })
        }
        val payload = JSONObject.quote(json.toString())
        webViewInstance?.evaluateJavascript(
            "if(window.renderHighlights) window.renderHighlights($payload);",
            null
        )
    }

    val renderMarginNotes = {
        val json = JSONArray()
        marginNotes.forEach { note ->
            if (note.cfi.isNotBlank()) {
                json.put(
                    JSONObject().apply {
                        put("id", note.id)
                        put("cfi", note.cfi)
                        put("color", note.color)
                    }
                )
            }
        }
        val payload = JSONObject.quote(json.toString())
        webViewInstance?.evaluateJavascript(
            "if(window.renderNoteIndicators) window.renderNoteIndicators($payload);",
            null
        )
    }

    val bridge = remember(bookUri) { 
        ReaderBridge(
            context = context, 
            bookUri = bookUri, 
            getSettingsProvider = { currentSettingsJsonProvider.value }, 
            getInitialProgressProvider = { initialProgressProvider.value },
            getInitialCfiProvider = { initialCfiProvider.value },
            onProgressUpdate = { p, c -> onProgressChangedProvider.value(p, c) },
            onToggleMenuAction = dispatchToggleMenu,
            onTextSelectedAction = { a, j, t, x, y ->
                val (adjustedX, adjustedY) = mapPointToWindow(x, y)
                onTextSelectedProvider.value(a, j, t, adjustedX, adjustedY)
            },
            onChaptersLoadedAction = { chapters -> onChaptersLoadedProvider.value(chapters) },
            onRequestHighlightsAction = { renderHighlights() },
            onRequestMarginNotesAction = { renderMarginNotes() },
            onHighlightClickedAction = { id, x, y ->
                val (adjustedX, adjustedY) = mapPointToWindow(x, y)
                onHighlightClickedProvider.value(id, adjustedX, adjustedY)
            },
            onMarginNoteClickedAction = { id, x, y ->
                val (adjustedX, adjustedY) = mapPointToWindow(x, y)
                onMarginNoteClickedProvider.value(id, adjustedX, adjustedY)
            },
            onImageClickedAction = dispatchImageClick,
            onLinkClickAction = { url -> pendingUrl = url },
            onTextExtractedAction = { text -> onTextExtractedProvider.value(text) },
            onPaginationChangedAction = { current, total -> onPaginationChangedProvider.value(current, total) },
            onStartListenAction = { text -> onStartListenProvider.value(text) },
            onTapZoneAction = { zone -> onTapZoneProvider.value(zone) },
            onSearchResultsAction = { results -> onSearchResultsProvider.value(results) }
        )
    }

    if (pendingUrl != null) {
        val targetUrl = pendingUrl.orEmpty()
        val isEmailLink = targetUrl.startsWith("mailto:", ignoreCase = true)
        AlertDialog(
            onDismissRequest = { pendingUrl = null },
            shape = UiTokens.SettingsCardShape,
            containerColor = appDialogContainerColor(),
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            title = { Text("Open Link") },
            text = {
                Text(
                    if (isEmailLink) {
                        "Do you want to open this email link?\n\n$targetUrl"
                    } else {
                        "Do you want to visit this website?\n\n$targetUrl"
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val urlToOpen = pendingUrl
                        pendingUrl = null
                        urlToOpen?.let {
                            try {
                                val uri = Uri.parse(it)
                                val intent = if (it.startsWith("mailto:", ignoreCase = true)) {
                                    Intent(Intent.ACTION_SENDTO, uri)
                                } else {
                                    Intent(Intent.ACTION_VIEW, uri)
                                }
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("EpubJsReader", "Error opening browser", e)
                            }
                        }
                    }
                ) {
                    Text(if (isEmailLink) "Open" else "Visit")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUrl = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    LaunchedEffect(currentSettingsJson, webViewInstance) {
        webViewInstance?.evaluateJavascript("if(window.applySettings) window.applySettings($currentSettingsJson);", null)
    }

    LaunchedEffect(highlights, webViewInstance) {
        renderHighlights()
    }

    LaunchedEffect(marginNotes, webViewInstance) {
        renderMarginNotes()
    }

    LaunchedEffect(
        isSelectionMenuVisible,
        isHighlightMenuVisible,
        isMarginNoteMenuVisible,
        webViewInstance
    ) {
        val isSelectionOpen = if (isSelectionMenuVisible) "true" else "false"
        val isHighlightOpen = if (isHighlightMenuVisible) "true" else "false"
        val isNoteOpen = if (isMarginNoteMenuVisible) "true" else "false"
        webViewInstance?.evaluateJavascript(
            "if(window.setNativeMenuState) window.setNativeMenuState($isSelectionOpen, $isHighlightOpen, $isNoteOpen);",
            null
        )
    }

    LaunchedEffect(isSelectionMenuVisible, webViewInstance) {
        if (isSelectionMenuVisible) {
            webViewInstance?.requestFocus()
        }
    }

    LaunchedEffect(listenActive, webViewInstance) {
        val activeFlag = if (listenActive) "true" else "false"
        webViewInstance?.evaluateJavascript(
            "if(window.setListenActive) window.setListenActive($activeFlag);",
            null
        )
    }

    LaunchedEffect(listenWordIndex, listenActive, webViewInstance) {
        if (!listenActive) {
            return@LaunchedEffect
        }
        val index = listenWordIndex ?: return@LaunchedEffect
        webViewInstance?.evaluateJavascript(
            "if(window.setTtsWordIndex) window.setTtsWordIndex($index);",
            null
        )
    }

    LaunchedEffect(pendingSearchQuery, searchRequestId, webViewInstance) {
        val query = pendingSearchQuery?.trim().orEmpty()
        if (pendingSearchQuery != null && webViewInstance != null) {
            val safeQuery = JSONObject.quote(query)
            webViewInstance?.evaluateJavascript(
                "if(window.searchInBook) window.searchInBook($safeQuery);",
                null
            )
            onSearchRequestConsumed()
        }
    }

    LaunchedEffect(pendingJumpHref, webViewInstance) {
        if (pendingJumpHref != null && webViewInstance != null) {
            val safeHref = JSONObject.quote(pendingJumpHref)
            webViewInstance?.evaluateJavascript("if(window.jumpTo) window.jumpTo($safeHref);", null)
            onJumpConsumed()
        }
    }

    LaunchedEffect(pendingProgressJump, webViewInstance) {
        if (pendingProgressJump != null && webViewInstance != null) {
            webViewInstance?.evaluateJavascript("if(window.jumpToPercentage) window.jumpToPercentage($pendingProgressJump);", null)
            onJumpConsumed()
        }
    }

    LaunchedEffect(pendingPageTurn, webViewInstance) {
        if (pendingPageTurn != null && webViewInstance != null) {
            val js = if (pendingPageTurn == PageTurnDirection.NEXT) {
                "if(window.nextPage) window.nextPage();"
            } else {
                "if(window.prevPage) window.prevPage();"
            }
            webViewInstance?.evaluateJavascript(js, null)
            onPageTurnConsumed()
        }
    }

    LaunchedEffect(requestTextExtraction, listenActive, webViewInstance) {
        if (requestTextExtraction && webViewInstance != null) {
            val js = "if(window.requestTextExtraction){ window.requestTextExtraction(); } else if(window.getCurrentText){ window.getCurrentText(); }"
            webViewInstance?.evaluateJavascript(js, null)
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { ctx ->
            object : WebView(ctx) {
                override fun startActionMode(callback: ActionMode.Callback?, type: Int): ActionMode? {
                    // Suppress native menu to let our BottomSheet show
                    return super.startActionMode(SelectionBlockerCallback(callback), type)
                }

                override fun startActionMode(callback: ActionMode.Callback?): ActionMode? {
                    return super.startActionMode(SelectionBlockerCallback(callback))
                }
            }.apply {
                @Suppress("DEPRECATION")
                this.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    // Prefer default cache behavior for file:// assets to avoid blank renders on some devices.
                    cacheMode = WebSettings.LOAD_DEFAULT
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    // Leave offscreen pre-rastering disabled to avoid blank WebView issues.
                    offscreenPreRaster = false
                }
                
                isLongClickable = true
                isFocusable = true
                isFocusableInTouchMode = true
                
                // Let WebView choose the appropriate layer type (hardware can render blank on some devices).
                setLayerType(android.view.View.LAYER_TYPE_NONE, null)
                
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                        val url = request?.url?.toString() ?: return false
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            pendingUrl = url
                            return true
                        }
                        return false
                    }
                }

                setBackgroundColor(android.graphics.Color.TRANSPARENT)

                webChromeClient = object : WebChromeClient() {
                    override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                        Log.d("READER_JS", cm?.message() ?: "")
                        return true
                    }

                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        // Report loading progress (0-100% converted to 0-1 scale)
                        val progress = newProgress / 100f
                        mainHandler.post { onLoadingProgressChangeProvider.value(progress) }
                    }
                }

                addJavascriptInterface(bridge, "AndroidReader")
                webViewInstance = this
                loadUrl("file:///android_asset/reader/index.html")
            }
        },
        update = { /* Avoid disruptive updates */ }
    )
}
