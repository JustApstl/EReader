package com.dyu.ereader.ui.reader

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.webkit.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.dyu.ereader.data.database.HighlightEntity
import com.dyu.ereader.data.database.MarginNoteEntity
import com.dyu.ereader.data.model.ReaderSettings
import com.dyu.ereader.data.model.getBackgroundColor
import com.dyu.ereader.data.model.getColor
import com.dyu.ereader.util.Logger
import org.json.JSONArray
import org.json.JSONObject

class ReaderBridge(
    private val context: android.content.Context,
    private val bookUri: String,
    private val getSettingsProvider: () -> String,
    private val getInitialProgressProvider: () -> Float,
    private val getInitialCfiProvider: () -> String?,
    private val onProgressUpdate: (Float, String?) -> Unit,
    private val onToggleMenuAction: () -> Unit,
    private val onTextSelectedAction: (String, String, String, Float, Float) -> Unit,
    private val onChaptersLoadedAction: (List<Chapter>) -> Unit,
    private val onRequestHighlightsAction: () -> Unit,
    private val onRequestMarginNotesAction: () -> Unit,
    private val onHighlightClickedAction: (Long, Float, Float) -> Unit,
    private val onMarginNoteClickedAction: (Long, Float, Float) -> Unit,
    private val onLinkClickAction: (String) -> Unit,
    private val onTextExtractedAction: (String) -> Unit,
    private val onPaginationChangedAction: (Int, Int) -> Unit,
    private val onStartTTSAction: (String) -> Unit = {}
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface
    fun getBookBase64(): String {
        return try {
            val uri = Uri.parse(bookUri)
            context.contentResolver.openInputStream(uri)?.use { stream ->
                Base64.encodeToString(stream.readBytes(), Base64.NO_WRAP)
            } ?: ""
        } catch (e: Exception) { "" }
    }

    @JavascriptInterface
    fun getSettings(): String = getSettingsProvider()

    @JavascriptInterface
    fun getInitialProgress(): Float = getInitialProgressProvider()

    @JavascriptInterface
    fun getInitialCfi(): String = getInitialCfiProvider() ?: ""

    @JavascriptInterface
    fun toggleMenu() {
        mainHandler.post { onToggleMenuAction() }
    }

    @JavascriptInterface
    fun log(message: String) {
        Logger.log("READER_JS: $message")
        Log.d("READER_JS", message)
    }

    @JavascriptInterface
    fun onChaptersLoaded(chaptersJson: String) {
        try {
            val jsonArray = JSONArray(chaptersJson)
            val chapters = mutableListOf<Chapter>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                chapters.add(Chapter(
                    label = obj.getString("label"),
                    href = obj.getString("href")
                ))
            }
            mainHandler.post { onChaptersLoadedAction(chapters) }
        } catch (e: Exception) {
            Log.e("ReaderBridge", "Error parsing chapters", e)
        }
    }

    @JavascriptInterface
    fun getBackgroundImageDataUrl(uriString: String?): String {
        if (uriString.isNullOrBlank()) return ""
        return try {
            val uri = Uri.parse(uriString)
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:$mimeType;base64,$b64"
            } ?: ""
        } catch (e: Exception) {
            Log.e("ReaderBridge", "Error loading background image: $uriString", e)
            ""
        }
    }

    @JavascriptInterface
    fun getFontDataUrl(uriString: String?): String {
        if (uriString.isNullOrBlank()) return ""
        return try {
            val uri = Uri.parse(uriString)
            val mimeType = context.contentResolver.getType(uri) ?: "font/ttf"
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val bytes = stream.readBytes()
                val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                "data:$mimeType;base64,$b64"
            } ?: ""
        } catch (e: Exception) {
            Log.e("ReaderBridge", "Error loading custom font: $uriString", e)
            ""
        }
    }

    @JavascriptInterface
    fun onLocationChanged(locationJson: String) {
        try {
            val json = JSONObject(locationJson)
            var percentage = json.optDouble("explicitPercentage", -1.0)
            if (percentage < 0) {
                percentage = json.optJSONObject("start")?.optDouble("percentage", 0.0) ?: 0.0
            }
            val cfi = json.optJSONObject("start")?.optString("cfi")
            mainHandler.post { onProgressUpdate(percentage.toFloat(), cfi) }
        } catch (e: Exception) { }
    }

    @JavascriptInterface
    fun onTextSelected(anchor: String, json: String, text: String, x: Float, y: Float) {
        mainHandler.post { onTextSelectedAction(anchor, json, text, x, y) }
    }

    @JavascriptInterface
    fun requestHighlights() {
        mainHandler.post { onRequestHighlightsAction() }
    }

    @JavascriptInterface
    fun requestMarginNotes() {
        mainHandler.post { onRequestMarginNotesAction() }
    }

    @JavascriptInterface
    fun onHighlightClicked(id: Long, x: Float, y: Float) {
        mainHandler.post { onHighlightClickedAction(id, x, y) }
    }

    @JavascriptInterface
    fun onMarginNoteClicked(id: Long, x: Float, y: Float) {
        mainHandler.post { onMarginNoteClickedAction(id, x, y) }
    }

    @JavascriptInterface
    fun openBrowser(url: String) {
        mainHandler.post { onLinkClickAction(url) }
    }

    @JavascriptInterface
    fun onTextExtracted(text: String) {
        mainHandler.post { onTextExtractedAction(text) }
    }

    @JavascriptInterface
    fun onPaginationChanged(current: Int, total: Int) {
        mainHandler.post { onPaginationChangedAction(current, total) }
    }

    @JavascriptInterface
    fun startTTSWithSelectedText(text: String) {
        if (text.isNotBlank()) {
            mainHandler.post { onStartTTSAction(text) }
        }
    }
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
    onChaptersLoaded: (List<Chapter>) -> Unit,
    onTextExtracted: (String) -> Unit = {},
    onPaginationChanged: (Int, Int) -> Unit = { _, _ -> },
    onStartTTS: (String) -> Unit = {},
    onLoadingProgressChange: (Float) -> Unit = {},
    isSelectionMenuVisible: Boolean = false,
    isHighlightMenuVisible: Boolean = false,
    isMarginNoteMenuVisible: Boolean = false,
    pendingJumpHref: String? = null,
    pendingProgressJump: Float? = null,
    requestTextExtraction: Boolean = false,
    onJumpConsumed: () -> Unit = {},
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
            } ?: "#1A1A1A"
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
            put("pageTransitionStyle", settings.pageTransitionStyle.name)
        }.toString()
    }

    val currentSettingsJsonProvider = rememberUpdatedState(currentSettingsJson)
    val onProgressChangedProvider = rememberUpdatedState(onProgressChanged)
    val onToggleMenuProvider = rememberUpdatedState(onToggleMenu)
    val onTextSelectedProvider = rememberUpdatedState(onTextSelected)
    val onHighlightClickedProvider = rememberUpdatedState(onHighlightClicked)
    val onMarginNoteClickedProvider = rememberUpdatedState(onMarginNoteClicked)
    val initialProgressProvider = rememberUpdatedState(initialProgress)
    val initialCfiProvider = rememberUpdatedState(initialCfi)
    val onChaptersLoadedProvider = rememberUpdatedState(onChaptersLoaded)
    val onTextExtractedProvider = rememberUpdatedState(onTextExtracted)
    val onPaginationChangedProvider = rememberUpdatedState(onPaginationChanged)
    val onStartTTSProvider = rememberUpdatedState(onStartTTS)
    val onLoadingProgressChangeProvider = rememberUpdatedState(onLoadingProgressChange)

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
            onToggleMenuAction = { onToggleMenuProvider.value() },
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
            onLinkClickAction = { url -> pendingUrl = url },
            onTextExtractedAction = { text -> onTextExtractedProvider.value(text) },
            onPaginationChangedAction = { current, total -> onPaginationChangedProvider.value(current, total) },
            onStartTTSAction = { text -> onStartTTSProvider.value(text) }
        )
    }

    if (pendingUrl != null) {
        AlertDialog(
            onDismissRequest = { pendingUrl = null },
            title = { Text("Open Link") },
            text = { Text("Do you want to visit this website?\n\n$pendingUrl") },
            confirmButton = {
                Button(
                    onClick = {
                        val urlToOpen = pendingUrl
                        pendingUrl = null
                        urlToOpen?.let {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(it))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Log.e("EpubJsReader", "Error opening browser", e)
                            }
                        }
                    }
                ) {
                    Text("Visit")
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

    LaunchedEffect(requestTextExtraction, webViewInstance) {
        if (requestTextExtraction && webViewInstance != null) {
            webViewInstance?.evaluateJavascript("if(window.getCurrentText) window.getCurrentText();", null)
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
                    // Performance optimizations for faster loading
                    cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    // Enable optimized rendering
                    offscreenPreRaster = true
                }
                
                isLongClickable = true
                isFocusable = true
                isFocusableInTouchMode = true
                
                // Hardware acceleration for better performance
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
                
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

private class SelectionBlockerCallback(private val originalCallback: ActionMode.Callback?) : ActionMode.Callback {
    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        originalCallback?.onCreateActionMode(mode, menu)
        // Clear native menu items so the floating toolbar doesn't appear and block our BottomSheet
        menu?.clear()
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        originalCallback?.onPrepareActionMode(mode, menu)
        menu?.clear()
        return false
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean = false

    override fun onDestroyActionMode(mode: ActionMode?) {
        originalCallback?.onDestroyActionMode(mode)
    }
}
