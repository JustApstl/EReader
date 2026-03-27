package com.dyu.ereader.ui.reader.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dyu.ereader.data.local.db.HighlightEntity
import com.dyu.ereader.data.local.db.BookmarkEntity
import com.dyu.ereader.data.local.db.MarginNoteEntity
import com.dyu.ereader.data.model.reader.FontColorTheme
import com.dyu.ereader.data.model.reader.ImageFilter
import com.dyu.ereader.data.model.reader.PageTransitionStyle
import com.dyu.ereader.data.model.reader.ReadingPreset
import com.dyu.ereader.data.model.reader.ReaderFont
import com.dyu.ereader.data.model.reader.ReaderSettings
import com.dyu.ereader.data.model.reader.ReaderTextElement
import com.dyu.ereader.data.model.reader.ReaderTapZoneAction
import com.dyu.ereader.data.model.reader.ReaderTheme
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.data.model.app.NavigationBarStyle
import com.dyu.ereader.data.model.reader.TextAlignment
import com.dyu.ereader.data.model.reader.withPreset
import com.dyu.ereader.data.repository.reader.ReaderRepository
import com.dyu.ereader.data.repository.tts.TTSEvent
import com.dyu.ereader.data.repository.tts.TextToSpeechRepository
import com.dyu.ereader.data.repository.analytics.AnalyticsRepository
import com.dyu.ereader.data.repository.export.ExportRepository
import com.dyu.ereader.data.repository.accessibility.AccessibilityRepository
import com.dyu.ereader.data.repository.search.SearchRepository
import com.dyu.ereader.data.repository.mobi.MobiConversionRepository
import com.dyu.ereader.data.model.search.SearchResult
import com.dyu.ereader.data.model.search.SearchQuery
import com.dyu.ereader.data.model.export.ExportFormat
import com.dyu.ereader.data.model.export.ExportOptions
import com.dyu.ereader.data.model.library.BookType
import com.dyu.ereader.data.model.analytics.ReadingStatistics
import com.dyu.ereader.data.model.analytics.ReadingSession
import com.dyu.ereader.data.model.accessibility.DyslexiaFont
import com.dyu.ereader.data.model.tts.ListenSleepTimerMode
import com.dyu.ereader.data.model.tts.VoiceInfo
import com.dyu.ereader.ui.reader.state.Chapter
import com.dyu.ereader.ui.reader.state.HighlightMenuState
import com.dyu.ereader.ui.reader.state.MarginNoteMenuState
import com.dyu.ereader.ui.reader.state.PageTurnDirection
import com.dyu.ereader.ui.reader.state.ReaderUiState
import com.dyu.ereader.ui.reader.state.SelectionMenuState
import com.dyu.ereader.core.crypto.stableMd5
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReaderRepository,
    private val ttsRepository: TextToSpeechRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val exportRepository: ExportRepository,
    private val accessibilityRepository: AccessibilityRepository,
    private val searchRepository: SearchRepository,
    private val mobiConversionRepository: MobiConversionRepository
) : ViewModel() {
    internal val readerRepository get() = repository
    internal val analyticsRepo get() = analyticsRepository
    internal val ttsRepo get() = ttsRepository

    private val bookArg: String = savedStateHandle.get<String>("bookUriArg") ?: ""
    private val bookTypeArg: String = savedStateHandle.get<String>("bookTypeArg") ?: BookType.EPUB.name
    private val bookUri: String = com.dyu.ereader.core.codec.decodeNavArg(bookArg)
    private var isDarkTheme: Boolean = savedStateHandle.get<Boolean>("isDarkTheme") ?: false
    internal val bookType: BookType = runCatching { BookType.valueOf(bookTypeArg) }.getOrDefault(BookType.EPUB)
    private val isReadableType: Boolean = bookType.isEpub || bookType == BookType.PDF || bookType == BookType.MOBI
    internal val readerBookId = stableMd5(bookUri)
    private val initialReaderSettings = runBlocking {
        repository.readResolvedReaderSettings(readerBookId)
    }

    internal val uiStateFlow = MutableStateFlow(ReaderUiState(settings = initialReaderSettings))
    val uiState: StateFlow<ReaderUiState> = uiStateFlow.asStateFlow()

    // Search functionality
    val searchResults: StateFlow<List<SearchResult>> = searchRepository.searchResults
    val isSearching: StateFlow<Boolean> = searchRepository.isSearching

    // TTS functionality
    val isSpeaking = ttsRepository.isPlaying
    val isTTSReady = ttsRepository.isReady
    val ttsSpeed = ttsRepository.settings.map { it.speed }
    val currentVoice = ttsRepository.settings.map { it.voice }
    val availableVoices: StateFlow<List<VoiceInfo>> = ttsRepository.availableVoices

    internal val currentTTSTextFlow = MutableStateFlow("")
    val currentTTSText: StateFlow<String> = currentTTSTextFlow.asStateFlow()

    internal val currentTTSSentenceFlow = MutableStateFlow("")
    val currentTTSSentence: StateFlow<String> = currentTTSSentenceFlow.asStateFlow()

    internal val ttsWordIndexFlow = MutableStateFlow<Int?>(null)
    val ttsWordIndex: StateFlow<Int?> = ttsWordIndexFlow.asStateFlow()
    internal var ttsWordOffsetsState: IntArray = intArrayOf()
    internal var manualTtsJobState: Job? = null
    internal var lastTtsRangeAtState: Long = 0L
    internal var lastTtsStartedAtState: Long = 0L
    internal var lastTtsCharRangeStartState: Int = 0
    internal var pausedTtsCharOffsetState: Int? = null
    internal var resumeBaseCharOffsetState: Int = 0
    internal val autoReadEnabledFlow = MutableStateFlow(false)
    val autoReadEnabled: StateFlow<Boolean> = autoReadEnabledFlow.asStateFlow()
    internal val sleepTimerModeFlow = MutableStateFlow(ListenSleepTimerMode.OFF)
    val sleepTimerMode: StateFlow<ListenSleepTimerMode> = sleepTimerModeFlow.asStateFlow()
    internal val sleepTimerRemainingFlow = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = sleepTimerRemainingFlow.asStateFlow()
    private val _showOnboarding = MutableStateFlow(false)
    val showOnboarding: StateFlow<Boolean> = _showOnboarding.asStateFlow()

    // Accessibility functionality
    val fontSize = accessibilityRepository.settings.map { it.largerTextSize }
    val lineHeight = accessibilityRepository.settings.map { it.enhancedLineSpacing }
    val letterSpacing = accessibilityRepository.settings.map { it.letterSpacing }
    val highContrastEnabled = accessibilityRepository.settings.map { it.highContrast }
    val dyslexicFontEnabled = accessibilityRepository.settings.map { it.dyslexiaFont != DyslexiaFont.ROBOTO }
    val screenReaderEnabled = accessibilityRepository.settings.map { it.screenReaderEnabled }

    // Analytics functionality
    val readingStats = analyticsRepository.bookStatistics.map { it[readerBookId] ?: ReadingStatistics(readerBookId) }
    val readingSessions = analyticsRepository.readingSessions
    val libraryStats = analyticsRepository.libraryStatistics
    private val _isLoadingAnalytics = MutableStateFlow(false)
    val isLoadingAnalytics: StateFlow<Boolean> = _isLoadingAnalytics.asStateFlow()

    // Export functionality
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private var saveProgressJob: Job? = null
    private var focusTextPreviewJob: Job? = null
    private var previewSettingsOverride: ReaderSettings? = null
    private var isInitialized = false
    internal var lastTtsSessionStartState: Long = 0L
    internal var wasSpeakingLastTickState: Boolean = false
    internal var autoReadPendingState: Boolean = false
    internal var autoReadSessionActiveState: Boolean = false
    internal var autoReadJobState: Job? = null
    internal var textExtractionRetryJobState: Job? = null
    internal var textExtractionRetryCountState: Int = 0
    internal val maxTextExtractionRetriesState = 3
    internal var sleepTimerJobState: Job? = null
    private var lastSearchQuery: String = ""
    private val removedFonts = emptySet<ReaderFont>()

    init {
        observeSettings()
        observeAnnotations()
        observeOnboarding()
        observeTtsState()
        observeTtsEvents()
        loadBook()
    }

    fun setReadingMode(mode: ReadingMode) {
        if (uiStateFlow.value.settings.readingMode == mode) return
        applyReaderSettings { it.copy(readingMode = mode) }
    }
    
    fun setReaderTheme(theme: ReaderTheme) {
        if (uiStateFlow.value.settings.readerTheme == theme) return
        applyReaderSettings { it.copy(readerTheme = theme) }
    }

    fun setAmbientMode(enabled: Boolean) {
        if (uiStateFlow.value.settings.ambientMode == enabled) return
        applyReaderSettings { it.copy(ambientMode = enabled) }
    }

    fun setFontColorTheme(theme: FontColorTheme) {
        val auto = theme == FontColorTheme.DEFAULT
        if (uiStateFlow.value.settings.fontColorTheme == theme && uiStateFlow.value.settings.autoFontColor == auto) return
        applyReaderSettings { it.copy(fontColorTheme = theme, autoFontColor = auto, usePublisherStyle = false) }
    }

    fun setAutoFontColor(enabled: Boolean) {
        if (uiStateFlow.value.settings.autoFontColor == enabled) return
        applyReaderSettings {
            if (enabled) {
                it.copy(autoFontColor = true, fontColorTheme = FontColorTheme.DEFAULT, usePublisherStyle = false)
            } else {
                it.copy(autoFontColor = false, usePublisherStyle = false)
            }
        }
    }

    fun setLoading(loading: Boolean) {
        if (uiStateFlow.value.isLoading == loading) return
        uiStateFlow.update { it.copy(isLoading = loading) }
    }

    fun setLoadingProgress(progress: Float) {
        val clampedProgress = progress.coerceIn(0f, 1f)
        if ((uiStateFlow.value.loadingProgress * 100).toInt() == (clampedProgress * 100).toInt()) return
        uiStateFlow.update { it.copy(loadingProgress = clampedProgress) }
    }

    fun setChapters(chapters: List<Chapter>) {
        uiStateFlow.update { it.copy(chapters = chapters, isLoading = false) }
    }

    fun jumpToChapter(href: String) {
        uiStateFlow.update { it.copy(pendingAnchorJump = href) }
    }

    fun onProgressChanged(progress: Float, cfi: String? = null) {
        val clamped = progress.coerceIn(0f, 1f)
        
        if (!isInitialized && clamped == 0f && uiStateFlow.value.progress > 0.01f) {
            return
        }

        uiStateFlow.update { it.copy(
            progress = clamped, 
            savedCfi = cfi ?: it.savedCfi 
        ) }

        if (clamped >= 0.98f) {
            analyticsRepository.recordBookFinished(readerBookId, bookType.label)
        }

        saveProgressJob?.cancel()
        saveProgressJob = viewModelScope.launch {
            delay(1000)
            repository.saveProgress(bookUri, clamped, cfi ?: uiStateFlow.value.savedCfi)
        }
        
        isInitialized = true

        if (autoReadPendingState) {
            autoReadPendingState = false
            autoReadJobState?.cancel()
            autoReadJobState = viewModelScope.launch {
                delay(140)
                if (autoReadEnabledFlow.value) {
                    handleStartTTSFromCurrentPage()
                }
            }
        }
    }

    fun requestProgressJump(progress: Float) {
        val clamped = progress.coerceIn(0f, 1f)
        uiStateFlow.update { it.copy(pendingProgressJump = clamped, progress = clamped) }
    }

    fun consumeJumps() {
        uiStateFlow.update { it.copy(pendingAnchorJump = null, pendingProgressJump = null) }
    }

    fun consumePageTurn() {
        uiStateFlow.update { it.copy(pendingPageTurn = null) }
    }

    fun onPaginationChanged(current: Int, total: Int) {
        uiStateFlow.update { state ->
            state.copy(
                currentPage = current, 
                totalPages = total
            )
        }
        analyticsRepository.recordPageTurn(readerBookId)
    }

    fun zoomImage(url: String?) {
        uiStateFlow.update { it.copy(zoomImageUrl = url) }
    }

    fun dismissMenus() {
        uiStateFlow.update { it.copy(selectionMenu = null, highlightMenu = null, marginNoteMenu = null) }
    }

    private fun loadBook() {
        viewModelScope.launch {
            uiStateFlow.update { it.copy(isLoading = true, errorMessage = null) }

            if (!isReadableType) {
                uiStateFlow.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Unsupported format"
                    )
                }
                return@launch
            }

            if (bookType == BookType.MOBI) {
                val conversionResult = mobiConversionRepository.convertToEpub(bookUri)
                if (conversionResult.isSuccess) {
                    val epubUri = conversionResult.getOrNull()
                    uiStateFlow.update {
                        it.copy(
                            resolvedBookUri = epubUri,
                            resolvedBookType = BookType.EPUB
                        )
                    }
                } else {
                    uiStateFlow.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = conversionResult.exceptionOrNull()?.message ?: "MOBI conversion failed"
                        )
                    }
                    return@launch
                }
            }
            
            val metadata = repository.getBookMetadata(readerBookId, bookUri)
            val fallbackTitle = metadata?.title?.takeIf { it.isNotBlank() }
                ?: Uri.parse(bookUri).lastPathSegment
                    ?.substringBeforeLast('.')
                    ?.replace('_', ' ')
                    ?.replace('-', ' ')
                    ?.trim()
                    .orEmpty()
            val fallbackAuthor = metadata?.author?.takeIf { it.isNotBlank() }.orEmpty()
            uiStateFlow.update {
                it.copy(
                    title = fallbackTitle,
                    author = fallbackAuthor
                )
            }

            val savedProgress = repository.getBookProgress(bookUri).first()
            val savedCfi = repository.getBookCfi(bookUri).first()
            
            uiStateFlow.update { it.copy(
                progress = savedProgress, 
                savedCfi = savedCfi
            ) }
            
            repository.updateLastOpened(bookUri)
            repository.clearNewDownloadById(readerBookId)
            
            delay(1000)
            if (uiStateFlow.value.isLoading) {
                uiStateFlow.update { it.copy(isLoading = false) }
            }
            isInitialized = true
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            repository.readerSettingsFlow(readerBookId).collectLatest { settings ->
                val sanitized = if (removedFonts.contains(settings.font)) {
                    settings.copy(font = ReaderFont.SYSTEM, usePublisherStyle = false)
                } else {
                    settings
                }
                if (sanitized != settings) {
                    repository.saveReaderSettings(readerBookId, sanitized)
                }
                val previewOverride = previewSettingsOverride
                when {
                    previewOverride == null -> {
                        uiStateFlow.update { it.copy(settings = sanitized) }
                    }
                    sanitized == previewOverride -> {
                        previewSettingsOverride = null
                        uiStateFlow.update { it.copy(settings = sanitized) }
                    }
                    else -> {
                        uiStateFlow.update { it.copy(settings = previewOverride) }
                    }
                }
            }
        }
    }

    private fun observeAnnotations() {
        viewModelScope.launch {
            repository.getHighlights(readerBookId).collectLatest { highlights ->
                uiStateFlow.update { it.copy(highlights = highlights) }
            }
        }
        viewModelScope.launch {
            repository.getBookmarks(readerBookId).collectLatest { bookmarks ->
                uiStateFlow.update { it.copy(bookmarks = bookmarks) }
            }
        }
        viewModelScope.launch {
            repository.getMarginNotes(readerBookId).collectLatest { notes ->
                uiStateFlow.update { it.copy(marginNotes = notes) }
            }
        }
    }

    private fun observeOnboarding() {
        viewModelScope.launch {
            repository.readerOnboardingSeenFlow.collectLatest { seen ->
                _showOnboarding.value = !seen
            }
        }
    }

    private fun applyReaderSettings(transform: (ReaderSettings) -> ReaderSettings) {
        val old = previewSettingsOverride ?: uiStateFlow.value.settings
        val updated = transform(old)
        
        if (old == updated) return

        previewSettingsOverride = updated
        uiStateFlow.update { it.copy(settings = updated) }
        
        viewModelScope.launch {
            repository.saveReaderSettings(readerBookId, updated)
        }
    }

    private fun previewReaderSettings(transform: (ReaderSettings) -> ReaderSettings) {
        val old = previewSettingsOverride ?: uiStateFlow.value.settings
        val updated = transform(old)
        if (old == updated) return
        previewSettingsOverride = updated
        uiStateFlow.update { it.copy(settings = updated) }
    }

    fun applySettings(settings: ReaderSettings) {
        applyReaderSettings { settings }
    }

    fun resetSettings() {
        previewSettingsOverride = null
        viewModelScope.launch {
            repository.resetReaderSettings(readerBookId)
        }
    }

    fun applyPreset(preset: ReadingPreset) {
        applyReaderSettings { current ->
            current.withPreset(preset, bookType)
        }
    }

    fun retry() {
        loadBook()
    }

    fun onThemeChanged(isDark: Boolean) {
        isDarkTheme = isDark
    }

    fun setFocusTextEnabled(enabled: Boolean) {
        focusTextPreviewJob?.cancel()
        applyReaderSettings { it.copy(focusText = enabled) }
    }

    fun previewFocusTextBoldness(boldness: Int) {
        scheduleFocusTextPreview { it.copy(focusTextBoldness = boldness) }
    }

    fun setFocusTextBoldness(boldness: Int) {
        focusTextPreviewJob?.cancel()
        applyReaderSettings { it.copy(focusTextBoldness = boldness) }
    }

    fun previewFocusTextEmphasis(emphasis: Float) {
        scheduleFocusTextPreview { it.copy(focusTextEmphasis = emphasis.coerceIn(0.15f, 0.8f)) }
    }

    fun setFocusTextEmphasis(emphasis: Float) {
        focusTextPreviewJob?.cancel()
        applyReaderSettings { it.copy(focusTextEmphasis = emphasis.coerceIn(0.15f, 0.8f)) }
    }

    fun previewFocusTextColor(color: Int?) {
        previewReaderSettings { it.copy(focusTextColor = color) }
    }

    fun setFocusTextColor(color: Int?) {
        focusTextPreviewJob?.cancel()
        applyReaderSettings { it.copy(focusTextColor = color) }
    }

    private fun scheduleFocusTextPreview(transform: (ReaderSettings) -> ReaderSettings) {
        focusTextPreviewJob?.cancel()
        focusTextPreviewJob = viewModelScope.launch {
            delay(32)
            previewReaderSettings(transform)
        }
    }

    fun setFocusMode(enabled: Boolean) {
        applyReaderSettings { it.copy(focusMode = enabled) }
    }

    fun setHideStatusBar(hide: Boolean) {
        applyReaderSettings { it.copy(hideStatusBar = hide) }
    }

    fun previewCustomBackgroundColor(color: Int?) {
        previewReaderSettings { it.copy(customBackgroundColor = color, readerTheme = ReaderTheme.CUSTOM, usePublisherStyle = false) }
    }

    fun setCustomBackgroundColor(color: Int?) {
        applyReaderSettings { it.copy(customBackgroundColor = color, readerTheme = ReaderTheme.CUSTOM, usePublisherStyle = false) }
    }

    fun previewCustomFontColor(color: Int?) {
        previewReaderSettings {
            it.copy(
                customFontColor = color,
                fontColorTheme = FontColorTheme.CUSTOM,
                autoFontColor = false,
                usePublisherStyle = false
            )
        }
    }

    fun setCustomFontColor(color: Int?) {
        applyReaderSettings {
            it.copy(
                customFontColor = color,
                fontColorTheme = FontColorTheme.CUSTOM,
                autoFontColor = false,
                usePublisherStyle = false
            )
        }
    }

    fun previewElementStyleColor(element: ReaderTextElement, color: Int?) {
        previewReaderSettings {
            it.copy(
                elementStyles = it.elementStyles.update(element) { style -> style.copy(color = color) },
                usePublisherStyle = false
            )
        }
    }

    fun setElementStyleColor(element: ReaderTextElement, color: Int?) {
        applyReaderSettings {
            it.copy(
                elementStyles = it.elementStyles.update(element) { style -> style.copy(color = color) },
                usePublisherStyle = false
            )
        }
    }

    fun setElementStyleFont(element: ReaderTextElement, font: ReaderFont) {
        applyReaderSettings {
            it.copy(
                elementStyles = it.elementStyles.update(element) { style -> style.copy(font = font) },
                usePublisherStyle = false
            )
        }
    }

    fun setBackgroundImageUri(uri: String?) {
        applyReaderSettings { it.copy(backgroundImageUri = uri, readerTheme = ReaderTheme.IMAGE, usePublisherStyle = false) }
    }

    fun previewBackgroundImageBlur(blur: Float) {
        previewReaderSettings { it.copy(backgroundImageBlur = blur) }
    }

    fun setBackgroundImageBlur(blur: Float) {
        applyReaderSettings { it.copy(backgroundImageBlur = blur) }
    }

    fun previewBackgroundImageOpacity(opacity: Float) {
        previewReaderSettings { it.copy(backgroundImageOpacity = opacity) }
    }

    fun setBackgroundImageOpacity(opacity: Float) {
        applyReaderSettings { it.copy(backgroundImageOpacity = opacity) }
    }

    fun previewBackgroundImageZoom(zoom: Float) {
        previewReaderSettings { it.copy(backgroundImageZoom = zoom) }
    }

    fun setBackgroundImageZoom(zoom: Float) {
        applyReaderSettings { it.copy(backgroundImageZoom = zoom) }
    }

    fun previewReaderFontSize(size: Float) {
        previewReaderSettings { it.copy(fontSizeSp = size, usePublisherStyle = false) }
    }

    fun setReaderFontSize(size: Float) {
        applyReaderSettings { it.copy(fontSizeSp = size, usePublisherStyle = false) }
    }

    fun previewLineSpacing(spacing: Float) {
        previewReaderSettings { it.copy(lineSpacing = spacing, usePublisherStyle = false) }
    }

    fun setLineSpacing(spacing: Float) {
        applyReaderSettings { it.copy(lineSpacing = spacing, usePublisherStyle = false) }
    }

    fun previewMargins(margins: Float) {
        previewReaderSettings { it.copy(horizontalMarginDp = margins, usePublisherStyle = false) }
    }

    fun setMargins(margins: Float) {
        applyReaderSettings { it.copy(horizontalMarginDp = margins, usePublisherStyle = false) }
    }

    fun setFont(readerFont: ReaderFont) {
        applyReaderSettings { it.copy(font = readerFont, usePublisherStyle = false) }
    }

    fun setCustomFontUri(uri: String?) {
        applyReaderSettings {
            it.copy(
                customFontUri = uri,
                font = if (uri != null) ReaderFont.CUSTOM else if (it.font == ReaderFont.CUSTOM) ReaderFont.SYSTEM else it.font,
                usePublisherStyle = false
            )
        }
    }

    fun clearCustomFont() {
        applyReaderSettings {
            it.copy(
                customFontUri = null,
                font = ReaderFont.SYSTEM,
                usePublisherStyle = false
            )
        }
    }

    fun setImageFilter(filter: ImageFilter) {
        applyReaderSettings { it.copy(imageFilter = filter) }
    }

    fun setUsePublisherStyle(use: Boolean) {
        applyReaderSettings { 
            if (use) {
                it.copy(usePublisherStyle = true, readerTheme = ReaderTheme.DEFAULT, font = ReaderFont.DEFAULT, fontColorTheme = FontColorTheme.DEFAULT)
            } else {
                it.copy(usePublisherStyle = false)
            }
        }
    }

    fun setUnderlineLinks(enabled: Boolean) {
        applyReaderSettings { it.copy(underlineLinks = enabled) }
    }

    fun setTextShadow(enabled: Boolean) {
        applyReaderSettings { it.copy(textShadow = enabled) }
    }

    fun previewTextShadowColor(color: Int?) {
        previewReaderSettings { it.copy(textShadowColor = color) }
    }

    fun setTextShadowColor(color: Int?) {
        applyReaderSettings { it.copy(textShadowColor = color) }
    }

    fun setNavigationBarStyle(style: NavigationBarStyle) {
        applyReaderSettings { it.copy(navBarStyle = style) }
    }

    fun setPageTurn3dEnabled(enabled: Boolean) {
        applyReaderSettings { it.copy(pageTurn3d = enabled) }
    }

    fun setInvertPageTurns(enabled: Boolean) {
        applyReaderSettings { it.copy(invertPageTurns = enabled) }
    }

    fun setPageTransitionStyle(style: PageTransitionStyle) {
        applyReaderSettings { it.copy(pageTransitionStyle = style, pageTurn3d = true) }
    }

    fun setTextAlignment(alignment: TextAlignment) {
        applyReaderSettings { it.copy(textAlignment = alignment, usePublisherStyle = false) }
    }

    fun setTapZoneAction(zone: String, action: ReaderTapZoneAction) {
        applyReaderSettings {
            when (zone.uppercase()) {
                "LEFT" -> it.copy(leftTapAction = action)
                "RIGHT" -> it.copy(rightTapAction = action)
                "TOP" -> it.copy(topTapAction = action)
                "BOTTOM" -> it.copy(bottomTapAction = action)
                else -> it
            }
        }
    }

    fun requestPageTurn(direction: PageTurnDirection) {
        uiStateFlow.update { it.copy(pendingPageTurn = direction) }
    }

    fun dismissOnboarding() {
        viewModelScope.launch {
            repository.setReaderOnboardingSeen(true)
        }
    }

    fun consumeAnchorJump() {
        uiStateFlow.update { it.copy(pendingAnchorJump = null) }
    }

    fun addHighlight(chapterAnchor: String, selectionJson: String, text: String, color: String) {
        handleAddHighlight(chapterAnchor, selectionJson, text, color)
    }

    fun removeHighlight(id: Long) {
        handleRemoveHighlight(id)
    }

    fun addBookmark(chapterAnchor: String, cfi: String, title: String? = null) {
        handleAddBookmark(chapterAnchor, cfi, title)
    }

    fun addBookmarkAtCurrentLocation() {
        handleAddBookmarkAtCurrentLocation()
    }

    fun removeBookmark(bookmark: BookmarkEntity) {
        handleRemoveBookmark(bookmark)
    }

    fun addMarginNote(chapterAnchor: String, cfi: String, content: String, color: String = "#FFF59D") {
        handleAddMarginNote(chapterAnchor, cfi, content, color)
    }

    fun removeMarginNote(note: MarginNoteEntity) {
        handleRemoveMarginNote(note)
    }

    fun onTextSelected(anchor: String, json: String, text: String, x: Float, y: Float) {
        handleTextSelected(anchor, json, text, x, y)
    }

    fun onHighlightClicked(id: Long, x: Float, y: Float) {
        handleHighlightClicked(id, x, y)
    }

    fun onMarginNoteClicked(id: Long, x: Float, y: Float) {
        handleMarginNoteClicked(id, x, y)
    }

    // Text-to-Speech functionality
    fun setAutoReadEnabled(enabled: Boolean) {
        handleSetAutoReadEnabled(enabled)
    }

    fun setSleepTimerMode(mode: ListenSleepTimerMode) {
        handleSetSleepTimerMode(mode)
    }

    fun startTTSFromCurrentPage() {
        handleStartTTSFromCurrentPage()
    }

    fun startTTS(text: String) {
        handleStartTTS(text)
    }

    fun onTextExtracted(text: String) {
        handleOnTextExtracted(text)
    }

    fun pauseTTS() {
        handlePauseTTS()
    }

    fun resumeTTS() {
        handleResumeTTS()
    }

    fun resetTTS() {
        handleResetTTS()
    }

    fun stopTTS() {
        handleStopTTS()
    }

    fun setTTSSpeed(speed: Float) {
        ttsRepository.setSpeed(speed)
    }

    fun setTTSPitch(pitch: Float) {
        ttsRepository.setPitch(pitch)
    }

    fun setTTSLanguage(language: String) {
        ttsRepository.setLanguage(language)
    }

    fun setTTSVoice(voiceName: String) {
        ttsRepository.setVoice(voiceName)
    }

    // Analytics functionality
    fun startReadingSession() {
        val title = uiStateFlow.value.title.ifBlank { "Unknown Title" }
        analyticsRepository.startReadingSession(readerBookId, title, bookType.label)
    }

    fun endReadingSession() {
        analyticsRepository.endReadingSession(readerBookId)
    }

    fun recordHighlight() {
        analyticsRepository.recordHighlight(readerBookId)
    }

    fun recordBookmark() {
        analyticsRepository.recordBookmark(readerBookId)
    }

    private fun observeTtsState() {
        observeTtsStateInternal()
    }

    private fun observeTtsEvents() {
        observeTtsEventsInternal()
    }

    // Search functionality
    fun searchInBook(query: String) {
        if (query.isBlank()) {
            lastSearchQuery = ""
            searchRepository.clearResults()
            uiStateFlow.update { state ->
                state.copy(
                    pendingSearchQuery = "",
                    searchRequestId = state.searchRequestId + 1
                )
            }
            return
        }

        lastSearchQuery = query
        searchRepository.startSearch(SearchQuery(bookUri, query))
        uiStateFlow.update { state ->
            state.copy(
                pendingSearchQuery = query,
                searchRequestId = state.searchRequestId + 1
            )
        }
    }

    fun onSearchResultsReceived(results: List<SearchResult>) {
        searchRepository.onSearchResultsReceived(results)
        if (lastSearchQuery.isNotBlank()) {
            analyticsRepository.recordSearch(readerBookId, query = lastSearchQuery, resultsCount = results.size)
        }
    }

    fun consumeSearchRequest() {
        uiStateFlow.update { it.copy(pendingSearchQuery = null) }
    }

    fun navigateToSearchResult(result: SearchResult) {
        uiStateFlow.update { 
            it.copy(
                pendingAnchorJump = result.chapterHref,
                pendingProgressJump = result.percentage
            )
        }
    }

    // Accessibility functionality
    fun setFontSize(size: Float) {
        accessibilityRepository.setLargerTextSize(size > 18f)
    }

    fun setLineHeight(height: Float) {
        accessibilityRepository.setEnhancedLineSpacing(height > 1.2f)
    }

    fun setLetterSpacing(spacing: Float) {
        accessibilityRepository.setLetterSpacing(spacing)
    }

    fun setHighContrastEnabled(enabled: Boolean) {
        accessibilityRepository.setHighContrast(enabled)
    }

    fun setDyslexicFontEnabled(enabled: Boolean) {
        accessibilityRepository.setDyslexiaFont(
            if (enabled) DyslexiaFont.OPENDYSLEXIC 
            else DyslexiaFont.ROBOTO
        )
    }

    fun setScreenReaderEnabled(enabled: Boolean) {
        accessibilityRepository.setScreenReaderEnabled(enabled)
    }

    // Export functionality
    fun exportBook(format: ExportFormat, includeAnnotations: Boolean, includeBookmarks: Boolean) {
        _isExporting.value = true
        viewModelScope.launch {
            try {
                val data = uiStateFlow.value.toExportData(
                    format = format,
                    includeAnnotations = includeAnnotations,
                    includeBookmarks = includeBookmarks
                )
                val options = ExportOptions(format = format)
                exportRepository.exportData(data, options)
            } finally {
                _isExporting.value = false
            }
        }
    }
}
