package com.dyu.ereader.data.storage

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dyu.ereader.data.model.AppTheme
import com.dyu.ereader.data.model.FontColorTheme
import com.dyu.ereader.data.model.ImageFilter
import com.dyu.ereader.data.model.PageTransitionStyle
import com.dyu.ereader.data.model.ReaderControl
import com.dyu.ereader.data.model.ReaderFont
import com.dyu.ereader.data.model.ReaderSettings
import com.dyu.ereader.data.model.ReaderTheme
import com.dyu.ereader.data.model.ReadingMode
import com.dyu.ereader.data.model.NavigationBarStyle
import com.dyu.ereader.util.stableMd5
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ebook_reader_prefs")

class ReaderPreferencesStore(private val context: Context) {

    private object Keys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val LIQUID_GLASS_ENABLED = booleanPreferencesKey("liquid_glass_enabled")
        val LIBRARY_TREE_URI = stringPreferencesKey("library_tree_uri")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val READER_THEME = stringPreferencesKey("reader_theme")
        val FOCUS_TEXT_ENABLED = booleanPreferencesKey("focus_text_enabled")
        val FOCUS_TEXT_BOLDNESS = intPreferencesKey("focus_text_boldness")
        val FOCUS_TEXT_COLOR = intPreferencesKey("focus_text_color")
        val FONT_SIZE_SP = floatPreferencesKey("font_size_sp")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val HORIZONTAL_MARGIN = floatPreferencesKey("horizontal_margin")
        val FONT_NAME = stringPreferencesKey("font_name")
        val FOCUS_MODE = booleanPreferencesKey("focus_mode")
        val SHOW_BOOK_TYPE = booleanPreferencesKey("show_book_type")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val HIDE_STATUS_BAR = booleanPreferencesKey("hide_status_bar")
        val SHOW_RECENT_READING = booleanPreferencesKey("show_recent_reading")
        val SHOW_FAVORITES = booleanPreferencesKey("show_favorites")
        val SHOW_GENRES = booleanPreferencesKey("show_genres")
        val GRID_COLUMNS = intPreferencesKey("grid_columns")
        val CUSTOM_BG_COLOR = intPreferencesKey("custom_bg_color")
        val ANIMATIONS_ENABLED = booleanPreferencesKey("animations_enabled")
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val BACKGROUND_IMAGE_BLUR = floatPreferencesKey("background_image_blur")
        val BACKGROUND_IMAGE_OPACITY = floatPreferencesKey("background_image_opacity")
        val FONT_COLOR_THEME = stringPreferencesKey("font_color_theme")
        val AUTO_FONT_COLOR = booleanPreferencesKey("auto_font_color")
        val CUSTOM_FONT_COLOR = intPreferencesKey("custom_font_color")
        val CUSTOM_FONT_URI = stringPreferencesKey("custom_font_uri")
        val IMAGE_FILTER = stringPreferencesKey("image_filter")
        val USE_PUBLISHER_STYLE = booleanPreferencesKey("use_publisher_style")
        val UNDERLINE_LINKS = booleanPreferencesKey("underline_links")
        val TEXT_SHADOW = booleanPreferencesKey("text_shadow")
        val TEXT_SHADOW_COLOR = intPreferencesKey("text_shadow_color")
        val NAV_BAR_STYLE = stringPreferencesKey("nav_bar_style")
        val PAGE_TURN_3D = booleanPreferencesKey("page_turn_3d")
        val PAGE_TRANSITION_STYLE = stringPreferencesKey("page_transition_style")
        
        // Reader Control Toggles
        val SHOW_READER_SEARCH = booleanPreferencesKey("show_reader_search")
        val SHOW_READER_TTS = booleanPreferencesKey("show_reader_tts")
        val SHOW_READER_ACCESSIBILITY = booleanPreferencesKey("show_reader_accessibility")
        val SHOW_READER_ANALYTICS = booleanPreferencesKey("show_reader_analytics")
        val SHOW_READER_EXPORT = booleanPreferencesKey("show_reader_export")
        val READER_CONTROL_ORDER = stringPreferencesKey("reader_control_order")
    }

    val appThemeFlow: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        when (preferences[Keys.APP_THEME]) {
            AppTheme.SYSTEM.name -> AppTheme.SYSTEM
            AppTheme.LIGHT.name -> AppTheme.LIGHT
            AppTheme.BLACK.name -> AppTheme.BLACK
            AppTheme.DARK.name -> AppTheme.DARK
            else -> AppTheme.SYSTEM
        }
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[Keys.APP_THEME] = theme.name
        }
    }

    val liquidGlassEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.LIQUID_GLASS_ENABLED] ?: false
    }

    suspend fun setLiquidGlassEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LIQUID_GLASS_ENABLED] = enabled
        }
    }

    val navBarStyleFlow: Flow<NavigationBarStyle> = context.dataStore.data.map { preferences ->
        NavigationBarStyle.entries.find { it.name == preferences[Keys.NAV_BAR_STYLE] } ?: NavigationBarStyle.DEFAULT
    }

    suspend fun setNavigationBarStyle(style: NavigationBarStyle) {
        context.dataStore.edit { preferences ->
            preferences[Keys.NAV_BAR_STYLE] = style.name
        }
    }

    val animationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.ANIMATIONS_ENABLED] ?: true
    }

    suspend fun setAnimationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.ANIMATIONS_ENABLED] = enabled
        }
    }

    val libraryTreeUriFlow: Flow<String?> = context.dataStore.data.map { preferences ->
        preferences[Keys.LIBRARY_TREE_URI]
    }

    suspend fun setLibraryTreeUri(uri: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LIBRARY_TREE_URI] = uri
        }
    }

    val showBookTypeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.SHOW_BOOK_TYPE] ?: true
    }

    suspend fun setShowBookType(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SHOW_BOOK_TYPE] = show
        }
    }

    val showRecentReadingFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.SHOW_RECENT_READING] ?: true
    }

    suspend fun setShowRecentReading(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SHOW_RECENT_READING] = show
        }
    }

    val showFavoritesFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.SHOW_FAVORITES] ?: true
    }

    suspend fun setShowFavorites(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SHOW_FAVORITES] = show
        }
    }

    val showGenresFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.SHOW_GENRES] ?: true
    }

    suspend fun setShowGenres(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SHOW_GENRES] = show
        }
    }

    val hideStatusBarFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.HIDE_STATUS_BAR] ?: false
    }

    suspend fun setHideStatusBar(hide: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.HIDE_STATUS_BAR] = hide
        }
    }

    val sortOrderFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[Keys.SORT_ORDER] ?: "TITLE"
    }

    suspend fun setSortOrder(order: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SORT_ORDER] = order
        }
    }

    val gridColumnsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.GRID_COLUMNS] ?: 3
    }

    suspend fun setGridColumns(columns: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.GRID_COLUMNS] = columns.coerceIn(2, 4)
        }
    }

    // Reader Control Toggles
    val showReaderSearchFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.SHOW_READER_SEARCH] ?: true
    }
    suspend fun setShowReaderSearch(show: Boolean) = context.dataStore.edit { it[Keys.SHOW_READER_SEARCH] = show }

    val showReaderTTSFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.SHOW_READER_TTS] ?: true
    }
    suspend fun setShowReaderTTS(show: Boolean) = context.dataStore.edit { it[Keys.SHOW_READER_TTS] = show }

    val showReaderAccessibilityFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.SHOW_READER_ACCESSIBILITY] ?: true
    }
    suspend fun setShowReaderAccessibility(show: Boolean) = context.dataStore.edit { it[Keys.SHOW_READER_ACCESSIBILITY] = show }

    val showReaderAnalyticsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.SHOW_READER_ANALYTICS] ?: true
    }
    suspend fun setShowReaderAnalytics(show: Boolean) = context.dataStore.edit { it[Keys.SHOW_READER_ANALYTICS] = show }

    val showReaderExportFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.SHOW_READER_EXPORT] ?: true
    }
    suspend fun setShowReaderExport(show: Boolean) = context.dataStore.edit { it[Keys.SHOW_READER_EXPORT] = show }

    val readerControlOrderFlow: Flow<List<ReaderControl>> = context.dataStore.data.map { preferences ->
        parseReaderControlOrder(preferences[Keys.READER_CONTROL_ORDER])
    }
    suspend fun setReaderControlOrder(order: List<ReaderControl>) {
        val sanitized = sanitizeReaderControlOrder(order)
        context.dataStore.edit { preferences ->
            preferences[Keys.READER_CONTROL_ORDER] = sanitized.joinToString(",") { it.name }
        }
    }

    val readerSettingsFlow: Flow<ReaderSettings> = context.dataStore.data.map { preferences ->
        preferences.toReaderSettings()
    }

    suspend fun setReaderSettings(settings: ReaderSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.READING_MODE] = settings.readingMode.name
            preferences[Keys.READER_THEME] = settings.readerTheme.name
            preferences[Keys.FOCUS_TEXT_ENABLED] = settings.focusText
            preferences[Keys.FOCUS_TEXT_BOLDNESS] = settings.focusTextBoldness
            preferences[Keys.FONT_SIZE_SP] = settings.fontSizeSp
            preferences[Keys.LINE_SPACING] = settings.lineSpacing
            preferences[Keys.HORIZONTAL_MARGIN] = settings.horizontalMarginDp
            preferences[Keys.FONT_NAME] = settings.font.name
            preferences[Keys.FOCUS_MODE] = settings.focusMode
            preferences[Keys.HIDE_STATUS_BAR] = settings.hideStatusBar
            preferences[Keys.BACKGROUND_IMAGE_URI] = settings.backgroundImageUri ?: ""
            preferences[Keys.BACKGROUND_IMAGE_BLUR] = settings.backgroundImageBlur
            preferences[Keys.BACKGROUND_IMAGE_OPACITY] = settings.backgroundImageOpacity
            preferences[Keys.FONT_COLOR_THEME] = settings.fontColorTheme.name
            preferences[Keys.AUTO_FONT_COLOR] = settings.autoFontColor
            preferences[Keys.IMAGE_FILTER] = settings.imageFilter.name
            preferences[Keys.USE_PUBLISHER_STYLE] = settings.usePublisherStyle
            preferences[Keys.UNDERLINE_LINKS] = settings.underlineLinks
            preferences[Keys.TEXT_SHADOW] = settings.textShadow
            if (settings.textShadowColor != null) {
                preferences[Keys.TEXT_SHADOW_COLOR] = settings.textShadowColor
            } else {
                preferences.remove(Keys.TEXT_SHADOW_COLOR)
            }
            preferences[Keys.NAV_BAR_STYLE] = settings.navBarStyle.name
            preferences[Keys.PAGE_TURN_3D] = settings.pageTurn3d
            preferences[Keys.PAGE_TRANSITION_STYLE] = settings.pageTransitionStyle.name
            
            if (settings.customBackgroundColor != null) {
                preferences[Keys.CUSTOM_BG_COLOR] = settings.customBackgroundColor
            } else {
                preferences.remove(Keys.CUSTOM_BG_COLOR)
            }
            
            if (settings.customFontColor != null) {
                preferences[Keys.CUSTOM_FONT_COLOR] = settings.customFontColor
            } else {
                preferences.remove(Keys.CUSTOM_FONT_COLOR)
            }

            preferences[Keys.CUSTOM_FONT_URI] = settings.customFontUri ?: ""

            if (settings.focusTextColor != null) {
                preferences[Keys.FOCUS_TEXT_COLOR] = settings.focusTextColor
            } else {
                preferences.remove(Keys.FOCUS_TEXT_COLOR)
            }
        }
    }

    suspend fun setBookProgress(bookUri: String, progress: Float, cfi: String? = null) {
        context.dataStore.edit { preferences ->
            preferences[progressKey(bookUri)] = progress.coerceIn(0f, 1f)
            cfi?.let { preferences[cfiKey(bookUri)] = it }
        }
    }

    fun bookProgressFlow(bookUri: String): Flow<Float> {
        val key = progressKey(bookUri)
        return context.dataStore.data.map { preferences ->
            (preferences[key] ?: 0f).coerceIn(0f, 1f)
        }
    }

    fun bookCfiFlow(bookUri: String): Flow<String?> {
        val key = cfiKey(bookUri)
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    suspend fun exportPreferencesJson(): String {
        val prefs = context.dataStore.data.first().asMap()
        val exportMap = mutableMapOf<String, Any>()
        
        prefs.forEach { (key, value) ->
            exportMap[key.name] = value
        }
        
        return Gson().toJson(exportMap)
    }

    suspend fun importPreferencesJson(json: String) {
        val importMap: Map<String, Any> = Gson().fromJson(json, object : TypeToken<Map<String, Any>>() {}.type)
        
        context.dataStore.edit { preferences ->
            importMap.forEach { (keyName, value) ->
                when (value) {
                    is String -> preferences[stringPreferencesKey(keyName)] = value
                    is Boolean -> preferences[booleanPreferencesKey(keyName)] = value
                    is Double -> {
                        // Gson parses numbers as Doubles. We need to figure out if it was Int or Float.
                        // We check the standard keys first.
                        val intKeys = listOf(
                            "grid_columns",
                            "focus_text_boldness",
                            "custom_bg_color",
                            "custom_font_color",
                            "focus_text_color",
                            "text_shadow_color"
                        )
                        if (intKeys.contains(keyName)) {
                            preferences[intPreferencesKey(keyName)] = value.toInt()
                        } else {
                            preferences[floatPreferencesKey(keyName)] = value.toFloat()
                        }
                    }
                    is Long -> preferences[intPreferencesKey(keyName)] = value.toInt()
                    is Int -> preferences[intPreferencesKey(keyName)] = value
                }
            }
        }
    }

    private fun progressKey(bookUri: String): Preferences.Key<Float> {
        return floatPreferencesKey("progress_${stableMd5(bookUri)}")
    }

    private fun cfiKey(bookUri: String): Preferences.Key<String> {
        return stringPreferencesKey("cfi_${stableMd5(bookUri)}")
    }

    private fun Preferences.toReaderSettings(): ReaderSettings {
        val readingMode = when (this[Keys.READING_MODE]) {
            ReadingMode.PAGE.name -> ReadingMode.PAGE
            else -> ReadingMode.SCROLL
        }

        val readerTheme = ReaderTheme.entries.firstOrNull { it.name == this[Keys.READER_THEME] } ?: ReaderTheme.SYSTEM
        val fontColorTheme = FontColorTheme.entries.firstOrNull { it.name == this[Keys.FONT_COLOR_THEME] } ?: FontColorTheme.DEFAULT
        val autoFontColor = this[Keys.AUTO_FONT_COLOR] ?: true
        val imageFilter = ImageFilter.entries.firstOrNull { it.name == this[Keys.IMAGE_FILTER] } ?: ImageFilter.NONE
        val usePublisherStyle = this[Keys.USE_PUBLISHER_STYLE] ?: false
        val underlineLinks = this[Keys.UNDERLINE_LINKS] ?: false
        val textShadow = this[Keys.TEXT_SHADOW] ?: false
        val navBarStyle = NavigationBarStyle.entries.find { it.name == this[Keys.NAV_BAR_STYLE] } ?: NavigationBarStyle.DEFAULT
        val pageTurn3d = this[Keys.PAGE_TURN_3D] ?: ReaderSettings().pageTurn3d
        val pageTransitionStyle = PageTransitionStyle.entries.find { it.name == this[Keys.PAGE_TRANSITION_STYLE] }
            ?: PageTransitionStyle.DEFAULT

        val focusTextEnabled = this[Keys.FOCUS_TEXT_ENABLED] ?: false
        val focusTextBoldness = this[Keys.FOCUS_TEXT_BOLDNESS] ?: 700
        val font = ReaderFont.entries.firstOrNull { it.name == this[Keys.FONT_NAME] } ?: ReaderFont.SERIF
        val focusMode = this[Keys.FOCUS_MODE] ?: false
        val hideStatusBar = this[Keys.HIDE_STATUS_BAR] ?: false
        val backgroundImageUri = this[Keys.BACKGROUND_IMAGE_URI].let { if (it.isNullOrEmpty()) null else it }
        val customFontUri = this[Keys.CUSTOM_FONT_URI].let { if (it.isNullOrEmpty()) null else it }

        return ReaderSettings(
            readingMode = readingMode,
            readerTheme = readerTheme,
            focusText = focusTextEnabled,
            focusTextBoldness = focusTextBoldness,
            focusTextColor = this[Keys.FOCUS_TEXT_COLOR],
            fontSizeSp = this[Keys.FONT_SIZE_SP] ?: ReaderSettings().fontSizeSp,
            lineSpacing = this[Keys.LINE_SPACING] ?: ReaderSettings().lineSpacing,
            horizontalMarginDp = this[Keys.HORIZONTAL_MARGIN] ?: ReaderSettings().horizontalMarginDp,
            font = font,
            focusMode = focusMode,
            hideStatusBar = hideStatusBar,
            customBackgroundColor = this[Keys.CUSTOM_BG_COLOR],
            backgroundImageUri = backgroundImageUri,
            backgroundImageBlur = this[Keys.BACKGROUND_IMAGE_BLUR] ?: 0f,
            backgroundImageOpacity = this[Keys.BACKGROUND_IMAGE_OPACITY] ?: 1f,
            fontColorTheme = fontColorTheme,
            autoFontColor = autoFontColor,
            customFontColor = this[Keys.CUSTOM_FONT_COLOR],
            customFontUri = customFontUri,
            imageFilter = imageFilter,
            usePublisherStyle = usePublisherStyle,
            underlineLinks = underlineLinks,
            textShadow = textShadow,
            textShadowColor = this[Keys.TEXT_SHADOW_COLOR],
            navBarStyle = navBarStyle,
            pageTurn3d = pageTurn3d,
            pageTransitionStyle = pageTransitionStyle
        )
    }

    private fun parseReaderControlOrder(raw: String?): List<ReaderControl> {
        val parsed = raw
            ?.split(",")
            ?.mapNotNull { token -> ReaderControl.entries.find { it.name == token.trim() } }
            ?.distinct()
            .orEmpty()
        return sanitizeReaderControlOrder(parsed)
    }

    private fun sanitizeReaderControlOrder(order: List<ReaderControl>): List<ReaderControl> {
        val defaults = ReaderControl.defaultOrder()
        val cleaned = order.distinct().filter { defaults.contains(it) }
        return (cleaned + defaults).distinct()
    }
}
