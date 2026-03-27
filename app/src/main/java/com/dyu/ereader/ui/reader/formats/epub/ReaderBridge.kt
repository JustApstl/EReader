package com.dyu.ereader.ui.reader.formats.epub

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.webkit.JavascriptInterface
import com.dyu.ereader.data.model.search.SearchResult
import com.dyu.ereader.ui.reader.state.Chapter
import com.dyu.ereader.core.logging.AppLogger
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
    private val onImageClickedAction: (String) -> Unit,
    private val onLinkClickAction: (String) -> Unit,
    private val onTextExtractedAction: (String) -> Unit,
    private val onPaginationChangedAction: (Int, Int) -> Unit,
    private val onStartListenAction: (String) -> Unit = {},
    private val onTapZoneAction: (String) -> Unit = {},
    private val onSearchResultsAction: (List<SearchResult>) -> Unit = {}
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
    fun getBookCacheKey(): String {
        return bookUri.hashCode().toString()
    }

    @JavascriptInterface
    fun toggleMenu() {
        mainHandler.post { onToggleMenuAction() }
    }

    @JavascriptInterface
    fun log(message: String) {
        AppLogger.log("READER_JS: $message")
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
                    label = obj.optString("label"),
                    href = obj.optString("href"),
                    depth = obj.optInt("depth", 0),
                    hasChildren = obj.optBoolean("hasChildren", false)
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
    fun onImageClicked(url: String) {
        if (url.isBlank()) return
        mainHandler.post { onImageClickedAction(url) }
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
    fun onTapZone(zone: String) {
        mainHandler.post { onTapZoneAction(zone) }
    }

    @JavascriptInterface
    fun onSearchResults(resultsJson: String) {
        try {
            val jsonArray = JSONArray(resultsJson)
            val results = mutableListOf<SearchResult>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                results.add(
                    SearchResult(
                        chapterHref = obj.optString("chapterHref"),
                        chapterTitle = obj.optString("chapterTitle"),
                        textContext = obj.optString("textContext"),
                        matchStart = obj.optInt("matchStart", 0),
                        matchEnd = obj.optInt("matchEnd", 0),
                        percentage = obj.optDouble("percentage", 0.0).toFloat()
                    )
                )
            }
            mainHandler.post { onSearchResultsAction(results) }
        } catch (e: Exception) {
            Log.e("ReaderBridge", "Error parsing search results", e)
            mainHandler.post { onSearchResultsAction(emptyList()) }
        }
    }
}
