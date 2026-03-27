package com.dyu.ereader.data.local.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dyu.ereader.data.model.app.AppAccent
import com.dyu.ereader.data.model.app.AppFont
import com.dyu.ereader.data.model.app.AppTheme
import com.dyu.ereader.data.model.library.BookCollection
import com.dyu.ereader.data.model.update.AppReleaseInfo
import com.dyu.ereader.data.model.reader.FontColorTheme
import com.dyu.ereader.data.model.reader.ImageFilter
import com.dyu.ereader.data.model.reader.PageTransitionStyle
import com.dyu.ereader.data.model.reader.ReaderElementStyles
import com.dyu.ereader.data.model.reader.ReaderTapZoneAction
import com.dyu.ereader.data.model.reader.ReaderControl
import com.dyu.ereader.data.model.reader.ReaderFont
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.ReaderTheme
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.data.model.reader.TextAlignment
import com.dyu.ereader.core.crypto.stableMd5
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ebook_reader_prefs")

data class AppStartupPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val appFont: AppFont = AppFont.SYSTEM,
    val accent: AppAccent = AppAccent.SYSTEM,
    val customAccentColor: Int? = null,
    val liquidGlassEnabled: Boolean = false,
    val navBarStyle: NavigationBarStyle = NavigationBarStyle.DEFAULT,
    val hideStatusBar: Boolean = false,
    val appTextScale: Float = 1f,
    val hideBetaFeatures: Boolean = false
)

class ReaderPreferencesStore(private val context: Context) {

    private object Keys {
        val APP_THEME = stringPreferencesKey("app_theme")
        val APP_FONT = stringPreferencesKey("app_font")
        val APP_ACCENT = stringPreferencesKey("app_accent")
        val APP_ACCENT_CUSTOM_COLOR = intPreferencesKey("app_accent_custom_color")
        val LIQUID_GLASS_ENABLED = booleanPreferencesKey("liquid_glass_enabled")
        val APP_TEXT_SCALE = floatPreferencesKey("app_text_scale")
        val LIBRARY_TREE_URI = stringPreferencesKey("library_tree_uri")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val READER_THEME = stringPreferencesKey("reader_theme")
        val FOCUS_TEXT_ENABLED = booleanPreferencesKey("focus_text_enabled")
        val FOCUS_TEXT_BOLDNESS = intPreferencesKey("focus_text_boldness")
        val FOCUS_TEXT_EMPHASIS = floatPreferencesKey("focus_text_emphasis")
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
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val TEXT_SCROLLER_ENABLED = booleanPreferencesKey("text_scroller_enabled")
        val HIDE_BETA_FEATURES = booleanPreferencesKey("hide_beta_features")
        val DEVELOPER_OPTIONS_ENABLED = booleanPreferencesKey("developer_options_enabled")
        val BACKGROUND_IMAGE_URI = stringPreferencesKey("background_image_uri")
        val BACKGROUND_IMAGE_BLUR = floatPreferencesKey("background_image_blur")
        val BACKGROUND_IMAGE_OPACITY = floatPreferencesKey("background_image_opacity")
        val BACKGROUND_IMAGE_ZOOM = floatPreferencesKey("background_image_zoom")
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
        val INVERT_PAGE_TURNS = booleanPreferencesKey("invert_page_turns")
        val TEXT_ALIGNMENT = stringPreferencesKey("text_alignment")
        val ELEMENT_TEXT_STYLES = stringPreferencesKey("element_text_styles")
        val AMBIENT_MODE = booleanPreferencesKey("ambient_mode")
        val TAP_ACTION_LEFT = stringPreferencesKey("tap_action_left")
        val TAP_ACTION_RIGHT = stringPreferencesKey("tap_action_right")
        val TAP_ACTION_TOP = stringPreferencesKey("tap_action_top")
        val TAP_ACTION_BOTTOM = stringPreferencesKey("tap_action_bottom")
        val PER_BOOK_READER_SETTINGS = stringPreferencesKey("per_book_reader_settings")
        val BROWSE_FORMAT_PREFS = stringPreferencesKey("browse_format_prefs")
        val BROWSE_SAVED_SEARCHES = stringPreferencesKey("browse_saved_searches")
        val BROWSE_LAST_VISIT = stringPreferencesKey("browse_last_visit")
        
        // Reader Control Toggles
        val SHOW_READER_SEARCH = booleanPreferencesKey("show_reader_search")
        // Keep legacy key for backward compatibility.
        val SHOW_READER_LISTEN = booleanPreferencesKey("show_reader_tts")
        val SHOW_READER_ACCESSIBILITY = booleanPreferencesKey("show_reader_accessibility")
        val SHOW_READER_ANALYTICS = booleanPreferencesKey("show_reader_analytics")
        val SHOW_READER_EXPORT = booleanPreferencesKey("show_reader_export")
        val READER_CONTROL_ORDER = stringPreferencesKey("reader_control_order")
        val NEW_DOWNLOADS = stringPreferencesKey("new_downloads")
        val LIBRARY_COLLECTIONS = stringPreferencesKey("library_collections")
        val READER_ONBOARDING_SEEN = booleanPreferencesKey("reader_onboarding_seen")
        val LAST_LOCAL_BACKUP_EXPORT_AT = stringPreferencesKey("last_local_backup_export_at")
        val LAST_LOCAL_BACKUP_IMPORT_AT = stringPreferencesKey("last_local_backup_import_at")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val READING_REMINDER_ENABLED = booleanPreferencesKey("reading_reminder_enabled")
        val READING_REMINDER_HOUR = intPreferencesKey("reading_reminder_hour")
        val READING_REMINDER_MINUTE = intPreferencesKey("reading_reminder_minute")
        val UPDATE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("update_notifications_enabled")
        val NOTIFICATION_PERMISSION_PROMPTED = booleanPreferencesKey("notification_permission_prompted")
        val UPDATE_LAST_CHECKED_AT = stringPreferencesKey("update_last_checked_at")
        val UPDATE_CACHED_LATEST_RELEASE = stringPreferencesKey("update_cached_latest_release")
        val UPDATE_DISMISSED_VERSION = stringPreferencesKey("update_dismissed_version")
        val UPDATE_LAST_INSTALLED_VERSION = stringPreferencesKey("update_last_installed_version")
        val UPDATE_LAST_SHOWN_CHANGELOG_VERSION = stringPreferencesKey("update_last_shown_changelog_version")
        val UPDATE_LAST_NOTIFIED_VERSION = stringPreferencesKey("update_last_notified_version")
    }

    private val gson = Gson()

    val appThemeFlow: Flow<AppTheme> = context.dataStore.data.map { preferences ->
        preferences.toAppTheme()
    }

    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[Keys.APP_THEME] = theme.name
        }
    }

    val appFontFlow: Flow<AppFont> = context.dataStore.data.map { preferences ->
        AppFont.entries.find { it.name == preferences[Keys.APP_FONT] } ?: AppFont.SYSTEM
    }

    suspend fun setAppFont(font: AppFont) {
        context.dataStore.edit { preferences ->
            preferences[Keys.APP_FONT] = font.name
        }
    }

    suspend fun readAppStartupPreferences(): AppStartupPreferences {
        val preferences = context.dataStore.data.first()
        return AppStartupPreferences(
            theme = preferences.toAppTheme(),
            appFont = AppFont.entries.find { it.name == preferences[Keys.APP_FONT] } ?: AppFont.SYSTEM,
            accent = AppAccent.fromName(preferences[Keys.APP_ACCENT]),
            customAccentColor = preferences[Keys.APP_ACCENT_CUSTOM_COLOR],
            liquidGlassEnabled = preferences[Keys.LIQUID_GLASS_ENABLED] ?: false,
            navBarStyle = NavigationBarStyle.entries.find { it.name == preferences[Keys.NAV_BAR_STYLE] } ?: NavigationBarStyle.DEFAULT,
            hideStatusBar = preferences[Keys.HIDE_STATUS_BAR] ?: false,
            appTextScale = preferences[Keys.APP_TEXT_SCALE] ?: 1f,
            hideBetaFeatures = preferences[Keys.HIDE_BETA_FEATURES] ?: false
        )
    }

    val appAccentFlow: Flow<AppAccent> = context.dataStore.data.map { preferences ->
        AppAccent.fromName(preferences[Keys.APP_ACCENT])
    }

    suspend fun setAppAccent(accent: AppAccent) {
        context.dataStore.edit { preferences ->
            preferences[Keys.APP_ACCENT] = accent.name
        }
    }

    val appAccentCustomColorFlow: Flow<Int?> = context.dataStore.data.map { preferences ->
        preferences[Keys.APP_ACCENT_CUSTOM_COLOR]
    }

    suspend fun setAppAccentCustomColor(color: Int?) {
        context.dataStore.edit { preferences ->
            if (color == null) {
                preferences.remove(Keys.APP_ACCENT_CUSTOM_COLOR)
            } else {
                preferences[Keys.APP_ACCENT_CUSTOM_COLOR] = color
            }
        }
    }

    val liquidGlassEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.LIQUID_GLASS_ENABLED] ?: false
    }

    val readerOnboardingSeenFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.READER_ONBOARDING_SEEN] ?: false
    }

    suspend fun setLiquidGlassEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LIQUID_GLASS_ENABLED] = enabled
        }
    }

    suspend fun setReaderOnboardingSeen(seen: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.READER_ONBOARDING_SEEN] = seen
        }
    }

    val lastLocalBackupExportAtFlow: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_LOCAL_BACKUP_EXPORT_AT]?.toLongOrNull()
    }

    suspend fun setLastLocalBackupExportAt(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_LOCAL_BACKUP_EXPORT_AT] = timestamp.toString()
        }
    }

    val lastLocalBackupImportAtFlow: Flow<Long?> = context.dataStore.data.map { preferences ->
        preferences[Keys.LAST_LOCAL_BACKUP_IMPORT_AT]?.toLongOrNull()
    }

    suspend fun setLastLocalBackupImportAt(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.LAST_LOCAL_BACKUP_IMPORT_AT] = timestamp.toString()
        }
    }

    val appTextScaleFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[Keys.APP_TEXT_SCALE] ?: 1f
    }

    suspend fun setAppTextScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[Keys.APP_TEXT_SCALE] = scale.coerceIn(0.85f, 1.25f)
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

    val hapticsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.HAPTICS_ENABLED] ?: true
    }

    suspend fun setHapticsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.HAPTICS_ENABLED] = enabled
        }
    }

    val textScrollerEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.TEXT_SCROLLER_ENABLED] ?: true
    }

    suspend fun setTextScrollerEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.TEXT_SCROLLER_ENABLED] = enabled
        }
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.NOTIFICATIONS_ENABLED] ?: false
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val readingReminderEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.READING_REMINDER_ENABLED] ?: false
    }

    suspend fun setReadingReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.READING_REMINDER_ENABLED] = enabled
        }
    }

    val readingReminderHourFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.READING_REMINDER_HOUR] ?: 20
    }

    val readingReminderMinuteFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[Keys.READING_REMINDER_MINUTE] ?: 0
    }

    suspend fun setReadingReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[Keys.READING_REMINDER_HOUR] = hour.coerceIn(0, 23)
            preferences[Keys.READING_REMINDER_MINUTE] = minute.coerceIn(0, 59)
        }
    }

    val updateNotificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.UPDATE_NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setUpdateNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.UPDATE_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    val notificationPermissionPromptedFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.NOTIFICATION_PERMISSION_PROMPTED] ?: false
    }

    suspend fun setNotificationPermissionPrompted(prompted: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.NOTIFICATION_PERMISSION_PROMPTED] = prompted
        }
    }

    suspend fun getUpdateLastCheckedAt(): Long? =
        context.dataStore.data.first()[Keys.UPDATE_LAST_CHECKED_AT]?.toLongOrNull()

    suspend fun setUpdateLastCheckedAt(timestamp: Long) {
        context.dataStore.edit { preferences ->
            preferences[Keys.UPDATE_LAST_CHECKED_AT] = timestamp.toString()
        }
    }

    suspend fun readCachedLatestRelease(): AppReleaseInfo? {
        val raw = context.dataStore.data.first()[Keys.UPDATE_CACHED_LATEST_RELEASE] ?: return null
        return runCatching { gson.fromJson(raw, AppReleaseInfo::class.java) }.getOrNull()
    }

    suspend fun cacheLatestRelease(release: AppReleaseInfo?) {
        context.dataStore.edit { preferences ->
            if (release == null) {
                preferences.remove(Keys.UPDATE_CACHED_LATEST_RELEASE)
            } else {
                preferences[Keys.UPDATE_CACHED_LATEST_RELEASE] = gson.toJson(release)
            }
        }
    }

    suspend fun getDismissedUpdateVersion(): String? =
        context.dataStore.data.first()[Keys.UPDATE_DISMISSED_VERSION]

    suspend fun setDismissedUpdateVersion(versionName: String?) {
        context.dataStore.edit { preferences ->
            if (versionName.isNullOrBlank()) {
                preferences.remove(Keys.UPDATE_DISMISSED_VERSION)
            } else {
                preferences[Keys.UPDATE_DISMISSED_VERSION] = versionName
            }
        }
    }

    suspend fun getLastInstalledVersion(): String? =
        context.dataStore.data.first()[Keys.UPDATE_LAST_INSTALLED_VERSION]

    suspend fun setLastInstalledVersion(versionName: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.UPDATE_LAST_INSTALLED_VERSION] = versionName
        }
    }

    suspend fun getLastShownChangelogVersion(): String? =
        context.dataStore.data.first()[Keys.UPDATE_LAST_SHOWN_CHANGELOG_VERSION]

    suspend fun setLastShownChangelogVersion(versionName: String) {
        context.dataStore.edit { preferences ->
            preferences[Keys.UPDATE_LAST_SHOWN_CHANGELOG_VERSION] = versionName
        }
    }

    suspend fun getLastNotifiedUpdateVersion(): String? =
        context.dataStore.data.first()[Keys.UPDATE_LAST_NOTIFIED_VERSION]

    suspend fun setLastNotifiedUpdateVersion(versionName: String?) {
        context.dataStore.edit { preferences ->
            if (versionName.isNullOrBlank()) {
                preferences.remove(Keys.UPDATE_LAST_NOTIFIED_VERSION)
            } else {
                preferences[Keys.UPDATE_LAST_NOTIFIED_VERSION] = versionName
            }
        }
    }

    val hideBetaFeaturesFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.HIDE_BETA_FEATURES] ?: false
    }

    suspend fun setHideBetaFeatures(hidden: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.HIDE_BETA_FEATURES] = hidden
        }
    }

    val developerOptionsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.DEVELOPER_OPTIONS_ENABLED] ?: false
    }

    suspend fun setDeveloperOptionsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.DEVELOPER_OPTIONS_ENABLED] = enabled
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

    val browseSavedSearchesFlow: Flow<List<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[Keys.BROWSE_SAVED_SEARCHES] ?: return@map emptyList()
        runCatching {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(raw, type)
        }.getOrDefault(emptyList())
    }

    val newDownloadIdsFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        val raw = preferences[Keys.NEW_DOWNLOADS] ?: return@map emptySet()
        runCatching {
            val type = object : TypeToken<Map<String, Long>>() {}.type
            gson.fromJson<Map<String, Long>>(raw, type).keys
        }.getOrDefault(emptySet())
    }

    val libraryCollectionsFlow: Flow<List<BookCollection>> = context.dataStore.data.map { preferences ->
        val raw = preferences[Keys.LIBRARY_COLLECTIONS] ?: return@map emptyList()
        runCatching {
            val type = object : TypeToken<List<BookCollection>>() {}.type
            gson.fromJson<List<BookCollection>>(raw, type)
        }.getOrDefault(emptyList())
            .map { it.copy(bookIds = it.bookIds.toSet()) }
            .sortedBy { it.createdAt }
    }

    suspend fun setShowBookType(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[Keys.SHOW_BOOK_TYPE] = show
        }
    }

    suspend fun addBrowseSavedSearch(query: String) {
        val normalized = query.trim()
        if (normalized.isBlank()) return
        context.dataStore.edit { preferences ->
            val raw = preferences[Keys.BROWSE_SAVED_SEARCHES]
            val type = object : TypeToken<List<String>>() {}.type
            val current = runCatching { gson.fromJson<List<String>>(raw ?: "[]", type) }.getOrElse { emptyList() }
            val updated = mutableListOf<String>()
            updated.add(normalized)
            updated.addAll(current.filterNot { it.equals(normalized, ignoreCase = true) })
            preferences[Keys.BROWSE_SAVED_SEARCHES] = gson.toJson(updated.take(6))
        }
    }

    suspend fun getBrowseFormatPreference(url: String): String? {
        val raw = context.dataStore.data.first()[Keys.BROWSE_FORMAT_PREFS] ?: return null
        val type = object : TypeToken<Map<String, String>>() {}.type
        val map = runCatching { gson.fromJson<Map<String, String>>(raw, type) }.getOrElse { emptyMap() }
        return map[url]
    }

    suspend fun setBrowseFormatPreference(url: String, format: String) {
        context.dataStore.edit { preferences ->
            val raw = preferences[Keys.BROWSE_FORMAT_PREFS]
            val type = object : TypeToken<Map<String, String>>() {}.type
            val current = runCatching { gson.fromJson<Map<String, String>>(raw ?: "{}", type) }.getOrElse { emptyMap() }
            val updated = current.toMutableMap()
            updated[url] = format
            preferences[Keys.BROWSE_FORMAT_PREFS] = gson.toJson(updated)
        }
    }

    suspend fun getBrowseLastVisit(url: String): Long? {
        val raw = context.dataStore.data.first()[Keys.BROWSE_LAST_VISIT] ?: return null
        val type = object : TypeToken<Map<String, Long>>() {}.type
        val map = runCatching { gson.fromJson<Map<String, Long>>(raw, type) }.getOrElse { emptyMap() }
        return map[url]
    }

    suspend fun setBrowseLastVisit(url: String, timestamp: Long) {
        context.dataStore.edit { preferences ->
            val raw = preferences[Keys.BROWSE_LAST_VISIT]
            val type = object : TypeToken<Map<String, Long>>() {}.type
            val current = runCatching { gson.fromJson<Map<String, Long>>(raw ?: "{}", type) }.getOrElse { emptyMap() }
            val updated = current.toMutableMap()
            updated[url] = timestamp
            preferences[Keys.BROWSE_LAST_VISIT] = gson.toJson(updated)
        }
    }

    suspend fun addNewDownload(bookId: String) {
        context.dataStore.edit { preferences ->
            val raw = preferences[Keys.NEW_DOWNLOADS]
            val type = object : TypeToken<Map<String, Long>>() {}.type
            val current = runCatching { gson.fromJson<Map<String, Long>>(raw ?: "{}", type) }.getOrElse { emptyMap() }
            val updated = current.toMutableMap()
            updated[bookId] = System.currentTimeMillis()
            preferences[Keys.NEW_DOWNLOADS] = gson.toJson(updated)
        }
    }

    suspend fun clearNewDownload(bookId: String) {
        context.dataStore.edit { preferences ->
            val raw = preferences[Keys.NEW_DOWNLOADS] ?: return@edit
            val type = object : TypeToken<Map<String, Long>>() {}.type
            val current = runCatching { gson.fromJson<Map<String, Long>>(raw, type) }.getOrElse { emptyMap() }
            if (!current.containsKey(bookId)) return@edit
            val updated = current.toMutableMap()
            updated.remove(bookId)
            if (updated.isEmpty()) {
                preferences.remove(Keys.NEW_DOWNLOADS)
            } else {
                preferences[Keys.NEW_DOWNLOADS] = gson.toJson(updated)
            }
        }
    }

    suspend fun createLibraryCollection(name: String): Boolean {
        val normalized = name.trim()
        if (normalized.isBlank()) return false
        var created = false
        context.dataStore.edit { preferences ->
            val collections = preferences.readCollections().toMutableList()
            if (collections.any { it.name.equals(normalized, ignoreCase = true) }) return@edit
            collections += BookCollection(name = normalized)
            preferences[Keys.LIBRARY_COLLECTIONS] = gson.toJson(collections.sortedBy { it.createdAt })
            created = true
        }
        return created
    }

    suspend fun toggleBookInCollection(collectionName: String, bookId: String): Boolean {
        val normalized = collectionName.trim()
        if (normalized.isBlank() || bookId.isBlank()) return false
        var added = false
        context.dataStore.edit { preferences ->
            val updated = preferences.readCollections().map { collection ->
                if (!collection.name.equals(normalized, ignoreCase = true)) {
                    collection
                } else {
                    val nextIds = collection.bookIds.toMutableSet()
                    added = if (nextIds.contains(bookId)) {
                        nextIds.remove(bookId)
                        false
                    } else {
                        nextIds.add(bookId)
                        true
                    }
                    collection.copy(bookIds = nextIds)
                }
            }
            preferences[Keys.LIBRARY_COLLECTIONS] = gson.toJson(updated)
        }
        return added
    }

    suspend fun removeBookFromAllCollections(bookId: String) {
        context.dataStore.edit { preferences ->
            val updated = preferences.readCollections().map { collection ->
                collection.copy(bookIds = collection.bookIds - bookId)
            }
            preferences[Keys.LIBRARY_COLLECTIONS] = gson.toJson(updated)
        }
    }

    suspend fun deleteLibraryCollection(collectionName: String) {
        val normalized = collectionName.trim()
        if (normalized.isBlank()) return
        context.dataStore.edit { preferences ->
            val updated = preferences.readCollections()
                .filterNot { it.name.equals(normalized, ignoreCase = true) }
            if (updated.isEmpty()) {
                preferences.remove(Keys.LIBRARY_COLLECTIONS)
            } else {
                preferences[Keys.LIBRARY_COLLECTIONS] = gson.toJson(updated)
            }
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
        preferences[Keys.GRID_COLUMNS] ?: 2
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

    val showReaderListenFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[Keys.SHOW_READER_LISTEN] ?: true
    }
    suspend fun setShowReaderListen(show: Boolean) = context.dataStore.edit { it[Keys.SHOW_READER_LISTEN] = show }

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

    suspend fun readResolvedReaderSettings(bookId: String? = null): ReaderSettings {
        val preferences = context.dataStore.data.first()
        val global = preferences.toReaderSettings()
        if (bookId.isNullOrBlank()) return global

        val raw = preferences[Keys.PER_BOOK_READER_SETTINGS] ?: return global
        val type = object : TypeToken<Map<String, ReaderSettings>>() {}.type
        val perBookSettings = runCatching {
            gson.fromJson<Map<String, ReaderSettings>>(raw, type)[bookId]
        }.getOrNull()

        return perBookSettings ?: global
    }

    fun perBookReaderSettingsFlow(bookId: String): Flow<ReaderSettings?> {
        return context.dataStore.data.map { preferences ->
            val raw = preferences[Keys.PER_BOOK_READER_SETTINGS] ?: return@map null
            val type = object : TypeToken<Map<String, ReaderSettings>>() {}.type
            runCatching {
                gson.fromJson<Map<String, ReaderSettings>>(raw, type)[bookId]
            }.getOrNull()
        }
    }

    suspend fun setReaderSettings(settings: ReaderSettings) {
        context.dataStore.edit { preferences ->
            preferences[Keys.READING_MODE] = settings.readingMode.name
            preferences[Keys.READER_THEME] = settings.readerTheme.name
            preferences[Keys.FOCUS_TEXT_ENABLED] = settings.focusText
            preferences[Keys.FOCUS_TEXT_BOLDNESS] = settings.focusTextBoldness
            preferences[Keys.FOCUS_TEXT_EMPHASIS] = settings.focusTextEmphasis
            preferences[Keys.FONT_SIZE_SP] = settings.fontSizeSp
            preferences[Keys.LINE_SPACING] = settings.lineSpacing
            preferences[Keys.HORIZONTAL_MARGIN] = settings.horizontalMarginDp
            preferences[Keys.FONT_NAME] = settings.font.name
            preferences[Keys.FOCUS_MODE] = settings.focusMode
            preferences[Keys.HIDE_STATUS_BAR] = settings.hideStatusBar
            preferences[Keys.BACKGROUND_IMAGE_URI] = settings.backgroundImageUri ?: ""
            preferences[Keys.BACKGROUND_IMAGE_BLUR] = settings.backgroundImageBlur
            preferences[Keys.BACKGROUND_IMAGE_OPACITY] = settings.backgroundImageOpacity
            preferences[Keys.BACKGROUND_IMAGE_ZOOM] = settings.backgroundImageZoom
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
            preferences[Keys.INVERT_PAGE_TURNS] = settings.invertPageTurns
            preferences[Keys.PAGE_TRANSITION_STYLE] = settings.pageTransitionStyle.name
            preferences[Keys.TEXT_ALIGNMENT] = settings.textAlignment.name
            if (settings.elementStyles == ReaderElementStyles()) {
                preferences.remove(Keys.ELEMENT_TEXT_STYLES)
            } else {
                preferences[Keys.ELEMENT_TEXT_STYLES] = gson.toJson(settings.elementStyles)
            }
            preferences[Keys.AMBIENT_MODE] = settings.ambientMode
            preferences[Keys.TAP_ACTION_LEFT] = settings.leftTapAction.name
            preferences[Keys.TAP_ACTION_RIGHT] = settings.rightTapAction.name
            preferences[Keys.TAP_ACTION_TOP] = settings.topTapAction.name
            preferences[Keys.TAP_ACTION_BOTTOM] = settings.bottomTapAction.name
            
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

    suspend fun setPerBookReaderSettings(bookId: String, settings: ReaderSettings?) {
        context.dataStore.edit { preferences ->
            val raw = preferences[Keys.PER_BOOK_READER_SETTINGS]
            val type = object : TypeToken<Map<String, ReaderSettings>>() {}.type
            val current = runCatching {
                gson.fromJson<Map<String, ReaderSettings>>(raw ?: "{}", type)
            }.getOrElse { emptyMap() }
            val updated = current.toMutableMap()
            if (settings == null) {
                updated.remove(bookId)
            } else {
                updated[bookId] = settings
            }
            if (updated.isEmpty()) {
                preferences.remove(Keys.PER_BOOK_READER_SETTINGS)
            } else {
                preferences[Keys.PER_BOOK_READER_SETTINGS] = gson.toJson(updated)
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

        val readerTheme = ReaderTheme.entries.firstOrNull { it.name == this[Keys.READER_THEME] } ?: ReaderSettings().readerTheme
        val fontColorTheme = FontColorTheme.entries.firstOrNull { it.name == this[Keys.FONT_COLOR_THEME] } ?: FontColorTheme.DEFAULT
        val autoFontColor = this[Keys.AUTO_FONT_COLOR] ?: ReaderSettings().autoFontColor
        val imageFilter = ImageFilter.entries.firstOrNull { it.name == this[Keys.IMAGE_FILTER] } ?: ImageFilter.NONE
        val usePublisherStyle = this[Keys.USE_PUBLISHER_STYLE] ?: ReaderSettings().usePublisherStyle
        val underlineLinks = this[Keys.UNDERLINE_LINKS] ?: false
        val textShadow = this[Keys.TEXT_SHADOW] ?: false
        val navBarStyle = NavigationBarStyle.entries.find { it.name == this[Keys.NAV_BAR_STYLE] } ?: NavigationBarStyle.DEFAULT
        val pageTurn3d = this[Keys.PAGE_TURN_3D] ?: ReaderSettings().pageTurn3d
        val invertPageTurns = this[Keys.INVERT_PAGE_TURNS] ?: ReaderSettings().invertPageTurns
        val pageTransitionStyle = PageTransitionStyle.entries.find { it.name == this[Keys.PAGE_TRANSITION_STYLE] }
            ?: PageTransitionStyle.DEFAULT
        val textAlignment = TextAlignment.entries.find { it.name == this[Keys.TEXT_ALIGNMENT] }
            ?: TextAlignment.DEFAULT
        val elementStyles = runCatching {
            this[Keys.ELEMENT_TEXT_STYLES]
                ?.takeIf { it.isNotBlank() }
                ?.let { gson.fromJson(it, ReaderElementStyles::class.java) }
        }.getOrNull() ?: ReaderElementStyles()
        val ambientMode = this[Keys.AMBIENT_MODE] ?: false
        val leftTapAction = ReaderTapZoneAction.entries.find { it.name == this[Keys.TAP_ACTION_LEFT] }
            ?: ReaderSettings().leftTapAction
        val rightTapAction = ReaderTapZoneAction.entries.find { it.name == this[Keys.TAP_ACTION_RIGHT] }
            ?: ReaderSettings().rightTapAction
        val topTapAction = ReaderTapZoneAction.entries.find { it.name == this[Keys.TAP_ACTION_TOP] }
            ?: ReaderSettings().topTapAction
        val bottomTapAction = ReaderTapZoneAction.entries.find { it.name == this[Keys.TAP_ACTION_BOTTOM] }
            ?: ReaderSettings().bottomTapAction

        val focusTextEnabled = this[Keys.FOCUS_TEXT_ENABLED] ?: false
        val focusTextBoldness = this[Keys.FOCUS_TEXT_BOLDNESS] ?: 700
        val focusTextEmphasis = this[Keys.FOCUS_TEXT_EMPHASIS] ?: ReaderSettings().focusTextEmphasis
        val font = ReaderFont.entries.firstOrNull { it.name == this[Keys.FONT_NAME] } ?: ReaderFont.DEFAULT
        val focusMode = this[Keys.FOCUS_MODE] ?: false
        val hideStatusBar = this[Keys.HIDE_STATUS_BAR] ?: false
        val backgroundImageUri = this[Keys.BACKGROUND_IMAGE_URI].let { if (it.isNullOrEmpty()) null else it }
        val backgroundImageBlur = this[Keys.BACKGROUND_IMAGE_BLUR] ?: 0f
        val backgroundImageOpacity = this[Keys.BACKGROUND_IMAGE_OPACITY] ?: 1f
        val backgroundImageZoom = this[Keys.BACKGROUND_IMAGE_ZOOM] ?: 1f
        val customFontUri = this[Keys.CUSTOM_FONT_URI].let { if (it.isNullOrEmpty()) null else it }

        return ReaderSettings(
            readingMode = readingMode,
            readerTheme = readerTheme,
            focusText = focusTextEnabled,
            focusTextBoldness = focusTextBoldness,
            focusTextEmphasis = focusTextEmphasis,
            focusTextColor = this[Keys.FOCUS_TEXT_COLOR],
            fontSizeSp = this[Keys.FONT_SIZE_SP] ?: ReaderSettings().fontSizeSp,
            lineSpacing = this[Keys.LINE_SPACING] ?: ReaderSettings().lineSpacing,
            horizontalMarginDp = this[Keys.HORIZONTAL_MARGIN] ?: ReaderSettings().horizontalMarginDp,
            font = font,
            focusMode = focusMode,
            hideStatusBar = hideStatusBar,
            customBackgroundColor = this[Keys.CUSTOM_BG_COLOR],
            backgroundImageUri = backgroundImageUri,
            backgroundImageBlur = backgroundImageBlur,
            backgroundImageOpacity = backgroundImageOpacity,
            backgroundImageZoom = backgroundImageZoom,
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
            invertPageTurns = invertPageTurns,
            pageTransitionStyle = pageTransitionStyle,
            textAlignment = textAlignment,
            elementStyles = elementStyles,
            ambientMode = ambientMode,
            leftTapAction = leftTapAction,
            rightTapAction = rightTapAction,
            topTapAction = topTapAction,
            bottomTapAction = bottomTapAction
        )
    }

    private fun Preferences.toAppTheme(): AppTheme {
        return when (this[Keys.APP_THEME]) {
            AppTheme.SYSTEM.name -> AppTheme.SYSTEM
            AppTheme.LIGHT.name -> AppTheme.LIGHT
            AppTheme.BLACK.name -> AppTheme.BLACK
            AppTheme.DARK.name -> AppTheme.DARK
            else -> AppTheme.SYSTEM
        }
    }

    private fun parseReaderControlOrder(raw: String?): List<ReaderControl> {
        val parsed = raw
            ?.split(",")
            ?.mapNotNull { token ->
                val normalized = token.trim()
                when (normalized) {
                    "TTS" -> ReaderControl.LISTEN
                    else -> ReaderControl.entries.find { it.name == normalized }
                }
            }
            ?.distinct()
            .orEmpty()
        return sanitizeReaderControlOrder(parsed)
    }

    private fun sanitizeReaderControlOrder(order: List<ReaderControl>): List<ReaderControl> {
        val defaults = ReaderControl.defaultOrder()
        val cleaned = order.distinct().filter { defaults.contains(it) }
        return (cleaned + defaults).distinct()
    }

    private fun Preferences.readCollections(): List<BookCollection> {
        val raw = this[Keys.LIBRARY_COLLECTIONS] ?: return emptyList()
        val type = object : TypeToken<List<BookCollection>>() {}.type
        return runCatching { gson.fromJson<List<BookCollection>>(raw, type) }
            .getOrDefault(emptyList())
            .map { it.copy(bookIds = it.bookIds.toSet()) }
            .sortedBy { it.createdAt }
    }

}
