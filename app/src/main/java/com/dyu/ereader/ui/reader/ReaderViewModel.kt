package com.dyu.ereader.ui.reader

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dyu.ereader.data.database.HighlightEntity
import com.dyu.ereader.data.database.BookmarkEntity
import com.dyu.ereader.data.database.MarginNoteEntity
import com.dyu.ereader.data.model.FontColorTheme
import com.dyu.ereader.data.model.ImageFilter
import com.dyu.ereader.data.model.PageTransitionStyle
import com.dyu.ereader.data.model.ReaderFont
import com.dyu.ereader.data.model.ReaderSettings
import com.dyu.ereader.data.model.ReaderTheme
import com.dyu.ereader.data.model.ReadingMode
import com.dyu.ereader.data.model.NavigationBarStyle
import com.dyu.ereader.data.repository.ReaderRepository
import com.dyu.ereader.data.repository.TextToSpeechRepository
import com.dyu.ereader.data.repository.AnalyticsRepository
import com.dyu.ereader.data.repository.ExportRepository
import com.dyu.ereader.data.repository.AccessibilityRepository
import com.dyu.ereader.data.repository.SearchRepository
import com.dyu.ereader.data.model.SearchResult
import com.dyu.ereader.data.model.SearchQuery
import com.dyu.ereader.data.model.ExportFormat
import com.dyu.ereader.data.model.ExportData
import com.dyu.ereader.data.model.ExportOptions
import com.dyu.ereader.data.model.ExportedHighlight
import com.dyu.ereader.data.model.ExportedBookmark
import com.dyu.ereader.data.model.ExportedNote
import com.dyu.ereader.data.model.ReadingStatistics
import com.dyu.ereader.data.model.ReadingSession
import com.dyu.ereader.data.model.DyslexiaFont
import com.dyu.ereader.data.model.VoiceInfo
import com.dyu.ereader.util.stableMd5
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

data class Chapter(val label: String, val href: String)

data class SelectionMenuState(
    val chapterAnchor: String,
    val selectionJson: String,
    val text: String,
    val x: Float,
    val y: Float
)

data class HighlightMenuState(
    val highlightId: Long,
    val x: Float,
    val y: Float
)

data class MarginNoteMenuState(
    val noteId: Long,
    val x: Float,
    val y: Float
)

data class ReaderUiState(
    val isLoading: Boolean = true,
    val loadingProgress: Float = 0f,
    val title: String = "",
    val author: String = "",
    val settings: ReaderSettings = ReaderSettings(),
    val progress: Float = 0f,
    val savedCfi: String? = null,
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val currentChapterIndex: Int = 0,
    val chapters: List<Chapter> = emptyList(),
    val highlights: List<HighlightEntity> = emptyList(),
    val bookmarks: List<BookmarkEntity> = emptyList(),
    val marginNotes: List<MarginNoteEntity> = emptyList(),
    val errorMessage: String? = null,
    val selectionMenu: SelectionMenuState? = null,
    val highlightMenu: HighlightMenuState? = null,
    val marginNoteMenu: MarginNoteMenuState? = null,
    val zoomImageUrl: String? = null,
    val pendingAnchorJump: String? = null,
    val pendingProgressJump: Float? = null,
    val requestTextExtraction: Boolean = false,
    val lastExtractedText: String = ""
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: ReaderRepository,
    private val ttsRepository: TextToSpeechRepository,
    private val analyticsRepository: AnalyticsRepository,
    private val exportRepository: ExportRepository,
    private val accessibilityRepository: AccessibilityRepository,
    private val searchRepository: SearchRepository
) : ViewModel() {

    private val bookArg: String = savedStateHandle.get<String>("bookUriArg") ?: ""
    private val bookUri: String = com.dyu.ereader.util.decodeNavArg(bookArg)
    private var isDarkTheme: Boolean = savedStateHandle.get<Boolean>("isDarkTheme") ?: false

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    // Search functionality
    val searchResults: StateFlow<List<SearchResult>> = searchRepository.searchResults
    val isSearching: StateFlow<Boolean> = searchRepository.isSearching

    // TTS functionality
    val isSpeaking = ttsRepository.isPlaying
    val isTTSReady = ttsRepository.isReady
    val ttsSpeed = ttsRepository.settings.map { it.speed }
    val currentVoice = ttsRepository.settings.map { it.voice }
    val availableVoices: StateFlow<List<VoiceInfo>> = ttsRepository.availableVoices

    private val _currentTTSText = MutableStateFlow("")
    val currentTTSText: StateFlow<String> = _currentTTSText.asStateFlow()

    // Accessibility functionality
    val fontSize = accessibilityRepository.settings.map { it.largerTextSize }
    val lineHeight = accessibilityRepository.settings.map { it.enhancedLineSpacing }
    val letterSpacing = accessibilityRepository.settings.map { it.letterSpacing }
    val highContrastEnabled = accessibilityRepository.settings.map { it.highContrast }
    val dyslexicFontEnabled = accessibilityRepository.settings.map { it.dyslexiaFont != DyslexiaFont.ROBOTO }
    val screenReaderEnabled = accessibilityRepository.settings.map { it.screenReaderEnabled }

    // Analytics functionality
    val readingStats = analyticsRepository.bookStatistics.map { it[bookId] ?: ReadingStatistics(bookId) }
    val readingSessions = analyticsRepository.readingSessions
    private val _isLoadingAnalytics = MutableStateFlow(false)
    val isLoadingAnalytics: StateFlow<Boolean> = _isLoadingAnalytics.asStateFlow()

    // Export functionality
    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private var saveProgressJob: Job? = null
    private val bookId = stableMd5(bookUri)
    private var isInitialized = false

    init {
        observeSettings()
        observeAnnotations()
        loadBook()
    }

    fun setReadingMode(mode: ReadingMode) {
        // Early return if already in this mode to avoid unnecessary recomputations
        if (_uiState.value.settings.readingMode == mode) return
        // Update settings efficiently without triggering full recomposition
        updateSettings { copy(readingMode = mode) }
    }
    
    fun setReaderTheme(theme: ReaderTheme) {
        // Early return if theme already set
        if (_uiState.value.settings.readerTheme == theme) return
        updateSettings { copy(readerTheme = theme) }
    }

    fun setFontColorTheme(theme: FontColorTheme) {
        // Early return if already using this theme
        if (_uiState.value.settings.fontColorTheme == theme && !_uiState.value.settings.autoFontColor) return
        updateSettings { copy(fontColorTheme = theme, autoFontColor = false) }
    }

    fun setAutoFontColor(enabled: Boolean) {
        // Early return if already in desired state
        if (_uiState.value.settings.autoFontColor == enabled) return
        updateSettings {
            if (enabled) {
                copy(autoFontColor = true, fontColorTheme = FontColorTheme.DEFAULT)
            } else {
                copy(autoFontColor = false)
            }
        }
    }

    fun setLoading(loading: Boolean) {
        // Only update if state actually changes
        if (_uiState.value.isLoading == loading) return
        _uiState.update { it.copy(isLoading = loading) }
    }

    fun setLoadingProgress(progress: Float) {
        val clampedProgress = progress.coerceIn(0f, 1f)
        // Only update if progress meaningfully changed (reduces update frequency)
        if ((_uiState.value.loadingProgress * 100).toInt() == (clampedProgress * 100).toInt()) return
        _uiState.update { it.copy(loadingProgress = clampedProgress) }
    }

    fun setChapters(chapters: List<Chapter>) {
        _uiState.update { it.copy(chapters = chapters, isLoading = false) }
    }

    fun jumpToChapter(href: String) {
        _uiState.update { it.copy(pendingAnchorJump = href) }
    }

    fun onProgressChanged(progress: Float, cfi: String? = null) {
        val clamped = progress.coerceIn(0f, 1f)
        
        if (!isInitialized && clamped == 0f && _uiState.value.progress > 0.01f) {
            return
        }

        _uiState.update { it.copy(
            progress = clamped, 
            savedCfi = cfi ?: it.savedCfi 
        ) }

        saveProgressJob?.cancel()
        saveProgressJob = viewModelScope.launch {
            delay(1000)
            repository.saveProgress(bookUri, clamped, cfi ?: _uiState.value.savedCfi)
        }
        
        isInitialized = true
    }

    fun requestProgressJump(progress: Float) {
        val clamped = progress.coerceIn(0f, 1f)
        _uiState.update { it.copy(pendingProgressJump = clamped, progress = clamped) }
    }

    fun consumeJumps() {
        _uiState.update { it.copy(pendingAnchorJump = null, pendingProgressJump = null) }
    }

    fun onPaginationChanged(current: Int, total: Int) {
        _uiState.update { state ->
            state.copy(
                currentPage = current, 
                totalPages = total
            )
        }
    }

    fun zoomImage(url: String?) {
        _uiState.update { it.copy(zoomImageUrl = url) }
    }

    fun dismissMenus() {
        _uiState.update { it.copy(selectionMenu = null, highlightMenu = null, marginNoteMenu = null) }
    }

    private fun loadBook() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            
            val savedProgress = repository.getBookProgress(bookUri).first()
            val savedCfi = repository.getBookCfi(bookUri).first()
            
            _uiState.update { it.copy(
                progress = savedProgress, 
                savedCfi = savedCfi
            ) }
            
            repository.updateLastOpened(bookUri)
            
            // Safety fallback: if JS bridge never calls setChapters, hide loading after 5s
            delay(5000)
            if (_uiState.value.isLoading) {
                _uiState.update { it.copy(isLoading = false) }
            }
            isInitialized = true
        }
    }

    private fun observeSettings() {
        viewModelScope.launch {
            repository.readerSettingsFlow.collectLatest { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
    }

    private fun observeAnnotations() {
        viewModelScope.launch {
            repository.getHighlights(bookId).collectLatest { highlights ->
                _uiState.update { it.copy(highlights = highlights) }
            }
        }
        viewModelScope.launch {
            repository.getBookmarks(bookId).collectLatest { bookmarks ->
                _uiState.update { it.copy(bookmarks = bookmarks) }
            }
        }
        viewModelScope.launch {
            repository.getMarginNotes(bookId).collectLatest { notes ->
                _uiState.update { it.copy(marginNotes = notes) }
            }
        }
    }

    private fun updateSettings(transform: ReaderSettings.() -> ReaderSettings) {
        val old = _uiState.value.settings
        val updated = old.transform()
        
        if (old == updated) return

        _uiState.update { it.copy(settings = updated) }
        
        viewModelScope.launch {
            repository.saveReaderSettings(updated)
        }
    }

    fun resetSettings() {
        updateSettings { ReaderSettings() }
    }

    fun retry() {
        loadBook()
    }

    fun onThemeChanged(isDark: Boolean) {
        isDarkTheme = isDark
    }

    fun setFocusTextEnabled(enabled: Boolean) {
        updateSettings { copy(focusText = enabled) }
    }

    fun setFocusTextBoldness(boldness: Int) {
        updateSettings { copy(focusTextBoldness = boldness) }
    }

    fun setFocusTextColor(color: Int?) {
        updateSettings { copy(focusTextColor = color) }
    }

    fun setFocusMode(enabled: Boolean) {
        updateSettings { copy(focusMode = enabled) }
    }

    fun setHideStatusBar(hide: Boolean) {
        updateSettings { copy(hideStatusBar = hide) }
    }

    fun setCustomBackgroundColor(color: Int?) {
        updateSettings { copy(customBackgroundColor = color, readerTheme = ReaderTheme.CUSTOM) }
    }

    fun setCustomFontColor(color: Int?) {
        updateSettings {
            copy(
                customFontColor = color,
                fontColorTheme = FontColorTheme.CUSTOM,
                autoFontColor = false
            )
        }
    }

    fun setBackgroundImageUri(uri: String?) {
        updateSettings { copy(backgroundImageUri = uri, readerTheme = ReaderTheme.IMAGE) }
    }

    fun setBackgroundImageBlur(blur: Float) {
        updateSettings { copy(backgroundImageBlur = blur) }
    }

    fun setBackgroundImageOpacity(opacity: Float) {
        updateSettings { copy(backgroundImageOpacity = opacity) }
    }

    fun setReaderFontSize(size: Float) {
        updateSettings { copy(fontSizeSp = size) }
    }

    fun setLineSpacing(spacing: Float) {
        updateSettings { copy(lineSpacing = spacing) }
    }

    fun setMargins(margins: Float) {
        updateSettings { copy(horizontalMarginDp = margins) }
    }

    fun setFont(readerFont: ReaderFont) {
        updateSettings { copy(font = readerFont) }
    }

    fun setCustomFontUri(uri: String?) {
        updateSettings {
            copy(
                customFontUri = uri,
                font = if (uri != null) ReaderFont.CUSTOM else if (font == ReaderFont.CUSTOM) ReaderFont.SERIF else font
            )
        }
    }

    fun clearCustomFont() {
        // Explicitly clear custom font and reset to SERIF
        updateSettings {
            copy(
                customFontUri = null,
                font = ReaderFont.SERIF
            )
        }
    }

    fun setImageFilter(filter: ImageFilter) {
        updateSettings { copy(imageFilter = filter) }
    }

    fun setUsePublisherStyle(use: Boolean) {
        updateSettings { copy(usePublisherStyle = use) }
    }

    fun setUnderlineLinks(enabled: Boolean) {
        updateSettings { copy(underlineLinks = enabled) }
    }

    fun setTextShadow(enabled: Boolean) {
        updateSettings { copy(textShadow = enabled) }
    }

    fun setTextShadowColor(color: Int?) {
        updateSettings { copy(textShadowColor = color) }
    }

    fun setNavigationBarStyle(style: NavigationBarStyle) {
        updateSettings { copy(navBarStyle = style) }
    }

    fun setPageTurn3dEnabled(enabled: Boolean) {
        updateSettings { copy(pageTurn3d = enabled) }
    }

    fun setPageTransitionStyle(style: PageTransitionStyle) {
        updateSettings { copy(pageTransitionStyle = style, pageTurn3d = true) }
    }

    fun consumeAnchorJump() {
        _uiState.update { it.copy(pendingAnchorJump = null) }
    }

    fun addHighlight(chapterAnchor: String, selectionJson: String, text: String, color: String) {
        viewModelScope.launch {
            val existingHighlight = _uiState.value.highlights.firstOrNull { it.selectionJson == selectionJson }
            val existingColor = existingHighlight?.color?.trim()?.uppercase()
            val targetColor = color.trim().uppercase()
            if (existingColor == targetColor) {
                return@launch
            }

            repository.removeHighlightsBySelection(bookId, selectionJson)
            repository.addHighlight(
                HighlightEntity(
                    bookId = bookId,
                    chapterAnchor = chapterAnchor,
                    selectionJson = selectionJson,
                    selectedText = text,
                    color = color,
                    createdAt = System.currentTimeMillis()
                )
            )
            if (existingHighlight == null) {
                analyticsRepository.recordHighlight(bookId)
            }
        }
    }

    fun removeHighlight(id: Long) {
        viewModelScope.launch {
            repository.removeHighlight(id)
            dismissMenus()
        }
    }

    fun addBookmark(chapterAnchor: String, cfi: String, title: String? = null) {
        viewModelScope.launch {
            repository.addBookmark(
                BookmarkEntity(
                    bookId = bookId,
                    chapterAnchor = chapterAnchor,
                    cfi = cfi,
                    title = title
                )
            )
            analyticsRepository.recordBookmark(bookId)
        }
    }

    fun addBookmarkAtCurrentLocation() {
        val progressPercent = (_uiState.value.progress * 100f).toInt().coerceIn(0, 100)
        val currentCfi = _uiState.value.savedCfi?.takeIf { it.isNotBlank() }
            ?: "progress:$progressPercent"
        val bookmarkTitle = "Bookmark ${progressPercent}%"
        addBookmark(
            chapterAnchor = currentCfi,
            cfi = currentCfi,
            title = bookmarkTitle
        )
    }

    fun removeBookmark(bookmark: BookmarkEntity) {
        viewModelScope.launch {
            repository.removeBookmark(bookmark)
        }
    }

    fun addMarginNote(chapterAnchor: String, cfi: String, content: String, color: String = "#FFF59D") {
        if (content.isBlank()) return
        viewModelScope.launch {
            repository.removeMarginNotesByCfi(bookId, cfi)
            repository.addMarginNote(
                MarginNoteEntity(
                    bookId = bookId,
                    chapterAnchor = chapterAnchor,
                    cfi = cfi,
                    position = "RIGHT",
                    content = content.trim(),
                    color = color
                )
            )
            dismissMenus()
        }
    }

    fun removeMarginNote(note: MarginNoteEntity) {
        viewModelScope.launch {
            repository.deleteMarginNote(note)
            dismissMenus()
        }
    }

    fun onTextSelected(anchor: String, json: String, text: String, x: Float, y: Float) {
        Log.d("ReaderViewModel", "onTextSelected: $text at ($x, $y)")
        _uiState.update { 
            it.copy(
                selectionMenu = SelectionMenuState(anchor, json, text, x, y),
                highlightMenu = null,
                marginNoteMenu = null
            )
        }
    }

    fun onHighlightClicked(id: Long, x: Float, y: Float) {
        Log.d("ReaderViewModel", "onHighlightClicked: $id at ($x, $y)")
        _uiState.update {
            it.copy(highlightMenu = HighlightMenuState(id, x, y), selectionMenu = null, marginNoteMenu = null)
        }
    }

    fun onMarginNoteClicked(id: Long, x: Float, y: Float) {
        Log.d("ReaderViewModel", "onMarginNoteClicked: $id at ($x, $y)")
        _uiState.update {
            it.copy(marginNoteMenu = MarginNoteMenuState(id, x, y), selectionMenu = null, highlightMenu = null)
        }
    }

    // Text-to-Speech functionality
    fun startTTSFromCurrentPage() {
        _uiState.update { it.copy(requestTextExtraction = true) }
    }

    fun startTTS(text: String) {
        if (text.isNotBlank()) {
            _currentTTSText.value = text
            _uiState.update { it.copy(lastExtractedText = text) }
            ttsRepository.speak(text)
        }
    }

    fun onTextExtracted(text: String) {
        _uiState.update { it.copy(requestTextExtraction = false, lastExtractedText = text) }
        if (text.isNotBlank()) {
            _currentTTSText.value = text
            ttsRepository.speak(text)
        }
    }

    fun pauseTTS() {
        ttsRepository.pause()
    }

    fun resumeTTS() {
        if (ttsRepository.isPlaying.value) {
            ttsRepository.pause()
        } else {
            val text = _uiState.value.lastExtractedText
            if (text.isNotBlank()) {
                ttsRepository.speak(text)
            } else {
                startTTSFromCurrentPage()
            }
        }
    }

    fun stopTTS() {
        ttsRepository.stop()
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
        analyticsRepository.startReadingSession(bookId)
    }

    fun endReadingSession() {
        analyticsRepository.endReadingSession(bookId)
    }

    fun recordHighlight() {
        analyticsRepository.recordHighlight(bookId)
    }

    fun recordBookmark() {
        analyticsRepository.recordBookmark(bookId)
    }

    // Search functionality
    fun searchInBook(query: String) {
        if (query.isBlank()) {
            searchRepository.clearResults()
            return
        }

        viewModelScope.launch {
            searchRepository.search(SearchQuery(bookUri, query))
        }
    }

    fun onSearchResultsReceived(results: List<SearchResult>) {
        searchRepository.onSearchResultsReceived(results)
    }

    fun navigateToSearchResult(result: SearchResult) {
        // Jump to the chapter and highlight the text
        _uiState.update { 
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
                val data = ExportData(
                    bookTitle = _uiState.value.title,
                    bookAuthor = _uiState.value.author,
                    exportDate = System.currentTimeMillis(),
                    format = format,
                    highlights = if (includeAnnotations) _uiState.value.highlights.map { 
                        ExportedHighlight(
                            text = it.selectedText,
                            color = it.color,
                            chapter = it.chapterAnchor,
                            context = "", // Context not stored in HighlightEntity
                            timestamp = it.createdAt
                        )
                    } else emptyList(),
                    bookmarks = if (includeBookmarks) _uiState.value.bookmarks.map {
                        ExportedBookmark(
                            title = it.title ?: "Bookmark",
                            chapter = it.chapterAnchor,
                            note = it.note,
                            timestamp = it.createdAt
                        )
                    } else emptyList(),
                    notes = _uiState.value.marginNotes.map {
                        ExportedNote(
                            content = it.content,
                            chapter = it.chapterAnchor,
                            position = it.position,
                            timestamp = it.createdAt
                        )
                    }
                )
                val options = ExportOptions(format = format)
                exportRepository.exportData(data, options)
            } finally {
                _isExporting.value = false
            }
        }
    }
}
