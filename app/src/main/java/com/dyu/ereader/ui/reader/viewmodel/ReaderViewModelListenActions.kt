package com.dyu.ereader.ui.reader.viewmodel

import androidx.lifecycle.viewModelScope
import com.dyu.ereader.data.model.reader.ReadingMode
import com.dyu.ereader.data.model.tts.ListenSleepTimerMode
import com.dyu.ereader.data.repository.tts.TTSEvent
import com.dyu.ereader.ui.reader.state.PageTurnDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal fun ReaderViewModel.handleSetAutoReadEnabled(enabled: Boolean) {
    if (autoReadEnabledFlow.value == enabled) return
    autoReadEnabledFlow.value = enabled
    if (enabled) {
        handleStartTTSFromCurrentPage()
    } else {
        sleepTimerJobState?.cancel()
        sleepTimerModeFlow.value = ListenSleepTimerMode.OFF
        sleepTimerRemainingFlow.value = null
        handleStopTTS()
    }
}

internal fun ReaderViewModel.handleSetSleepTimerMode(mode: ListenSleepTimerMode) {
    sleepTimerJobState?.cancel()
    sleepTimerModeFlow.value = mode
    sleepTimerRemainingFlow.value = null

    val durationMs = when (mode) {
        ListenSleepTimerMode.MINUTES_10 -> 10 * 60 * 1000L
        ListenSleepTimerMode.MINUTES_20 -> 20 * 60 * 1000L
        ListenSleepTimerMode.END_OF_PAGE,
        ListenSleepTimerMode.OFF -> 0L
    }

    if (durationMs <= 0L) {
        return
    }

    sleepTimerJobState = viewModelScope.launch {
        val endsAt = System.currentTimeMillis() + durationMs
        while (isActive) {
            val remaining = (endsAt - System.currentTimeMillis()).coerceAtLeast(0L)
            sleepTimerRemainingFlow.value = remaining
            if (remaining == 0L) {
                disableAutoReadForSleepTimerInternal()
                break
            }
            delay(1000)
        }
    }
}

internal fun ReaderViewModel.handleStartTTSFromCurrentPage() {
    autoReadSessionActiveState = true
    textExtractionRetryCountState = 0
    uiStateFlow.update { it.copy(requestTextExtraction = false) }
    uiStateFlow.update { it.copy(requestTextExtraction = true) }
}

internal fun ReaderViewModel.scheduleTextExtractionRetryInternal() {
    if (textExtractionRetryCountState >= maxTextExtractionRetriesState) {
        textExtractionRetryCountState = 0
        return
    }
    textExtractionRetryCountState += 1
    textExtractionRetryJobState?.cancel()
    textExtractionRetryJobState = viewModelScope.launch {
        delay(180)
        uiStateFlow.update { it.copy(requestTextExtraction = true) }
    }
}

internal fun ReaderViewModel.handleStartTTS(text: String) {
    if (text.isBlank()) return
    autoReadSessionActiveState = false
    currentTTSTextFlow.value = text
    ttsWordOffsetsState = computeWordOffsets(text)
    currentTTSSentenceFlow.value = extractSentenceForWord(text, null, ttsWordOffsetsState)
    uiStateFlow.update { it.copy(lastExtractedText = text) }
    ttsWordIndexFlow.value = null
    pausedTtsCharOffsetState = null
    resumeBaseCharOffsetState = 0
    lastTtsCharRangeStartState = 0
    manualTtsJobState?.cancel()
    manualTtsJobState = null
    ttsRepo.speak(text)
}

internal fun ReaderViewModel.handleOnTextExtracted(text: String) {
    uiStateFlow.update { it.copy(requestTextExtraction = false, lastExtractedText = text) }
    if (text.isNotBlank()) {
        textExtractionRetryCountState = 0
        autoReadSessionActiveState = true
        currentTTSTextFlow.value = text
        ttsWordOffsetsState = computeWordOffsets(text)
        currentTTSSentenceFlow.value = extractSentenceForWord(text, null, ttsWordOffsetsState)
        ttsWordIndexFlow.value = null
        pausedTtsCharOffsetState = null
        resumeBaseCharOffsetState = 0
        lastTtsCharRangeStartState = 0
        manualTtsJobState?.cancel()
        manualTtsJobState = null
        ttsRepo.speak(text)
    } else {
        ttsWordOffsetsState = intArrayOf()
        ttsWordIndexFlow.value = null
        currentTTSSentenceFlow.value = ""
        manualTtsJobState?.cancel()
        manualTtsJobState = null
        if (autoReadSessionActiveState || autoReadEnabledFlow.value) {
            scheduleTextExtractionRetryInternal()
        } else {
            textExtractionRetryCountState = 0
        }
    }
}

internal fun ReaderViewModel.handlePauseTTS() {
    val activeWordIndex = ttsWordIndexFlow.value
    val fallbackOffset = activeWordIndex
        ?.takeIf { it in ttsWordOffsetsState.indices }
        ?.let { ttsWordOffsetsState[it] }
    pausedTtsCharOffsetState = (fallbackOffset ?: lastTtsCharRangeStartState)
        .coerceIn(0, currentTTSTextFlow.value.length.coerceAtLeast(0))
    ttsRepo.pause()
    manualTtsJobState?.cancel()
    manualTtsJobState = null
}

internal fun ReaderViewModel.handleResumeTTS() {
    if (ttsRepo.isPlaying.value) {
        ttsRepo.pause()
    } else {
        val text = currentTTSTextFlow.value.ifBlank { uiStateFlow.value.lastExtractedText }
        val pausedOffset = pausedTtsCharOffsetState?.coerceIn(0, text.length.coerceAtLeast(0))
        if (text.isNotBlank()) {
            val resumeOffset = pausedOffset
                ?.takeIf { it in 1 until text.length }
                ?: 0
            resumeBaseCharOffsetState = resumeOffset
            pausedTtsCharOffsetState = null
            lastTtsCharRangeStartState = resumeOffset
            ttsWordIndexFlow.value = wordIndexForChar(ttsWordOffsetsState, resumeOffset)
            currentTTSSentenceFlow.value = extractSentenceForWord(
                text = text,
                wordIndex = ttsWordIndexFlow.value,
                offsets = ttsWordOffsetsState
            )
            ttsRepo.speak(text.substring(resumeOffset))
        } else {
            handleStartTTSFromCurrentPage()
        }
    }
}

internal fun ReaderViewModel.handleResetTTS() {
    pausedTtsCharOffsetState = null
    resumeBaseCharOffsetState = 0
    lastTtsCharRangeStartState = 0
    manualTtsJobState?.cancel()
    manualTtsJobState = null
    ttsWordIndexFlow.value = null

    val text = currentTTSTextFlow.value.ifBlank { uiStateFlow.value.lastExtractedText }
    if (text.isNotBlank()) {
        currentTTSTextFlow.value = text
        ttsWordOffsetsState = computeWordOffsets(text)
        currentTTSSentenceFlow.value = extractSentenceForWord(text, null, ttsWordOffsetsState)
        ttsRepo.speak(text)
    } else {
        handleStartTTSFromCurrentPage()
    }
}

internal fun ReaderViewModel.handleStopTTS() {
    ttsRepo.stop()
    autoReadPendingState = false
    autoReadSessionActiveState = false
    ttsWordIndexFlow.value = null
    pausedTtsCharOffsetState = null
    resumeBaseCharOffsetState = 0
    lastTtsCharRangeStartState = 0
    manualTtsJobState?.cancel()
    manualTtsJobState = null
    textExtractionRetryCountState = 0
    textExtractionRetryJobState?.cancel()
    textExtractionRetryJobState = null
}

internal fun ReaderViewModel.observeTtsStateInternal() {
    viewModelScope.launch {
        ttsRepo.isPlaying.collectLatest { isSpeakingNow ->
            val now = System.currentTimeMillis()

            if (!wasSpeakingLastTickState && isSpeakingNow) {
                lastTtsSessionStartState = now
            } else if (wasSpeakingLastTickState && !isSpeakingNow && lastTtsSessionStartState > 0L) {
                val durationMs = now - lastTtsSessionStartState
                if (durationMs > 0L) {
                    analyticsRepo.recordTTSUsage(readerBookId, durationMs)
                }
                lastTtsSessionStartState = 0L
            }

            wasSpeakingLastTickState = isSpeakingNow
        }
    }
}

internal fun ReaderViewModel.observeTtsEventsInternal() {
    viewModelScope.launch {
        ttsRepo.events.collectLatest { event ->
            when (event) {
                is TTSEvent.Started -> {
                    lastTtsStartedAtState = System.currentTimeMillis()
                    lastTtsRangeAtState = 0L
                    if (currentTTSTextFlow.value.isNotBlank()) {
                        scheduleManualTtsHighlightFallbackInternal()
                    }
                }
                is TTSEvent.Range -> {
                    if (currentTTSTextFlow.value.isNotBlank()) {
                        lastTtsRangeAtState = System.currentTimeMillis()
                        manualTtsJobState?.cancel()
                        manualTtsJobState = null
                        val absoluteStart = event.start + resumeBaseCharOffsetState
                        lastTtsCharRangeStartState = absoluteStart
                        val index = wordIndexForChar(ttsWordOffsetsState, absoluteStart)
                        ttsWordIndexFlow.value = index
                        currentTTSSentenceFlow.value = extractSentenceForWord(
                            text = currentTTSTextFlow.value,
                            wordIndex = index,
                            offsets = ttsWordOffsetsState
                        )
                    }
                }
                is TTSEvent.Done -> {
                    manualTtsJobState?.cancel()
                    manualTtsJobState = null
                    if (!autoReadEnabledFlow.value || !autoReadSessionActiveState) {
                        return@collectLatest
                    }
                    val state = uiStateFlow.value
                    val isAtEnd = if (state.settings.readingMode == ReadingMode.PAGE && state.totalPages > 0) {
                        state.currentPage >= state.totalPages
                    } else {
                        state.progress >= 0.999f
                    }
                    if (isAtEnd) {
                        return@collectLatest
                    }
                    if (sleepTimerModeFlow.value == ListenSleepTimerMode.END_OF_PAGE) {
                        disableAutoReadForSleepTimerInternal()
                        return@collectLatest
                    }
                    autoReadPendingState = true
                    uiStateFlow.update { it.copy(pendingPageTurn = PageTurnDirection.NEXT) }
                    autoReadJobState?.cancel()
                    if (state.settings.readingMode != ReadingMode.PAGE) {
                        autoReadJobState = viewModelScope.launch {
                            delay(450)
                            if (autoReadPendingState && autoReadEnabledFlow.value) {
                                handleStartTTSFromCurrentPage()
                            }
                        }
                    }
                }
                is TTSEvent.Stopped -> {
                    if (event.interrupted) {
                        autoReadPendingState = false
                        autoReadSessionActiveState = false
                        ttsWordIndexFlow.value = null
                        manualTtsJobState?.cancel()
                        manualTtsJobState = null
                    }
                }
                is TTSEvent.Paused,
                is TTSEvent.Error -> {
                    autoReadPendingState = false
                    ttsWordIndexFlow.value = null
                    manualTtsJobState?.cancel()
                    manualTtsJobState = null
                }
            }
        }
    }
}

internal fun ReaderViewModel.scheduleManualTtsHighlightFallbackInternal() {
    manualTtsJobState?.cancel()
    val totalWords = ttsWordOffsetsState.size
    if (totalWords == 0) return
    manualTtsJobState = viewModelScope.launch {
        delay(900)
        if (lastTtsRangeAtState > 0L || !ttsRepo.isPlaying.value) return@launch
        val speed = ttsRepo.settings.value.speed.coerceIn(0.5f, 2.0f)
        val stepMs = (320f / speed).toLong().coerceIn(120L, 650L)
        var index = ttsWordIndexFlow.value?.coerceIn(0, totalWords - 1) ?: 0
        while (isActive && ttsRepo.isPlaying.value && index < totalWords) {
            ttsWordIndexFlow.value = index
            lastTtsCharRangeStartState = ttsWordOffsetsState[index]
            currentTTSSentenceFlow.value = extractSentenceForWord(
                text = currentTTSTextFlow.value,
                wordIndex = index,
                offsets = ttsWordOffsetsState
            )
            delay(stepMs)
            index += 1
        }
    }
}

internal fun ReaderViewModel.disableAutoReadForSleepTimerInternal() {
    sleepTimerJobState?.cancel()
    sleepTimerModeFlow.value = ListenSleepTimerMode.OFF
    sleepTimerRemainingFlow.value = null
    autoReadEnabledFlow.value = false
    handleStopTTS()
}
