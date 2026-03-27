package com.dyu.ereader.data.repository.tts

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.dyu.ereader.data.model.tts.TextToSpeechSettings
import com.dyu.ereader.data.model.tts.VoiceInfo
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.Locale

sealed class TTSEvent {
    data class Started(val utteranceId: String?) : TTSEvent()
    data class Range(val utteranceId: String?, val start: Int, val end: Int, val frame: Int) : TTSEvent()
    data class Done(val utteranceId: String?) : TTSEvent()
    data class Error(val utteranceId: String?) : TTSEvent()
    data class Paused(val utteranceId: String?) : TTSEvent()
    data class Stopped(val utteranceId: String?, val interrupted: Boolean) : TTSEvent()
}

class TextToSpeechRepository(
    private val context: Context
) : TextToSpeech.OnInitListener {
    
    private var tts: TextToSpeech? = null
    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    
    private val _settings = MutableStateFlow(
        TextToSpeechSettings(language = Locale.getDefault().toLanguageTag())
    )
    val settings: StateFlow<TextToSpeechSettings> = _settings.asStateFlow()
    
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _events = MutableSharedFlow<TTSEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<TTSEvent> = _events.asSharedFlow()

    private val _availableVoices = MutableStateFlow<List<VoiceInfo>>(emptyList())
    val availableVoices: StateFlow<List<VoiceInfo>> = _availableVoices.asStateFlow()

    private var pendingSpeakText: String? = null
    private var pauseRequested = false
    private var initRetryCount = 0
    private val MAX_INIT_RETRIES = 3

    init {
        initializeTTS()
    }

    private fun initializeTTS() {
        try {
            tts = TextToSpeech(context, this)
            Log.d("TTS", "TextToSpeech engine initialization started")
        } catch (e: Exception) {
            Log.e("TTS", "Failed to initialize TextToSpeech: ${e.message}", e)
            if (initRetryCount < MAX_INIT_RETRIES) {
                initRetryCount++
                repositoryScope.launch {
                    delay(500)
                    initializeTTS()
                }
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            Log.d("TTS", "TextToSpeech engine initialized successfully")
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isPlaying.value = true
                    Log.d("TTS", "Speech started: $utteranceId")
                    _events.tryEmit(TTSEvent.Started(utteranceId))
                }

                override fun onDone(utteranceId: String?) {
                    _isPlaying.value = false
                    Log.d("TTS", "Speech completed: $utteranceId")
                    releaseAudioFocus()
                    _events.tryEmit(TTSEvent.Done(utteranceId))
                }

                override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        _events.tryEmit(TTSEvent.Range(utteranceId, start, end, frame))
                    }
                }

                @Deprecated("Deprecated in TextToSpeech")
                override fun onError(utteranceId: String?) {
                    onError(utteranceId, TextToSpeech.ERROR)
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _isPlaying.value = false
                    Log.e("TTS", "Speech error: $utteranceId (code: $errorCode)")
                    releaseAudioFocus()
                    _events.tryEmit(TTSEvent.Error(utteranceId))
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    _isPlaying.value = false
                    Log.d("TTS", "Speech stopped: $utteranceId (interrupted: $interrupted)")
                    releaseAudioFocus()
                    if (pauseRequested) {
                        pauseRequested = false
                        _events.tryEmit(TTSEvent.Paused(utteranceId))
                    } else {
                        _events.tryEmit(TTSEvent.Stopped(utteranceId, interrupted))
                    }
                }
            })

            repositoryScope.launch {
                val voices = withContext(Dispatchers.IO) {
                    runCatching {
                        tts?.voices?.map { 
                            VoiceInfo(it.name, it.locale.toLanguageTag(), it == tts?.defaultVoice)
                        }
                    }.getOrNull() ?: emptyList()
                }
                _availableVoices.value = voices
                _isReady.value = true
                Log.d("TTS", "Available voices loaded: ${voices.size} voices")
                applyCurrentSettings()
                pendingSpeakText?.let { pending ->
                    pendingSpeakText = null
                    speakInternal(pending)
                }
            }
        } else {
            Log.e("TTS", "Initialization failed with status: $status")
            if (initRetryCount < MAX_INIT_RETRIES) {
                initRetryCount++
                repositoryScope.launch {
                    delay(500)
                    initializeTTS()
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(false)
                .build()
            val result = audioManager.requestAudioFocus(audioFocusRequest!!)
            Log.d("TTS", "Audio focus requested (API 26+): ${result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED}")
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            )
            Log.d("TTS", "Audio focus requested (legacy): ${result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED}")
            result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun releaseAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        Log.d("TTS", "Audio focus released")
    }

    fun speak(text: String) {
        Log.d("TTS", "Speak requested. Ready: ${_isReady.value}, Instance: ${tts != null}, Text length: ${text.length}")
        
        if (!_isReady.value || tts == null) {
            Log.w("TTS", "TTS not ready. Queuing request.")
            pendingSpeakText = text
            if (tts == null) {
                initializeTTS()
            }
            return
        }
        
        speakInternal(text)
    }

    private fun speakInternal(text: String) {
        if (text.isBlank()) {
            Log.w("TTS", "Empty text provided to speak")
            return
        }

        try {
            pauseRequested = false
            if (!requestAudioFocus()) {
                Log.w("TTS", "Failed to obtain audio focus")
            }

            tts?.stop() // Stop previous before starting new
            val utteranceId = "reader_utterance"
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val params = android.os.Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }
                Log.d("TTS", "Calling TTS.speak() (Bundle overload) with ${text.length} characters")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            } else {
                @Suppress("DEPRECATION")
                val params = hashMapOf(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID to utteranceId)
                Log.d("TTS", "Calling TTS.speak() (HashMap overload) with ${text.length} characters")
                @Suppress("DEPRECATION")
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
            }
            
            Log.d("TTS", "TTS.speak() returned: $result")
            if (result == TextToSpeech.SUCCESS) {
                _isPlaying.value = true
                Log.d("TTS", "Speech request successful")
            } else {
                _isPlaying.value = false
                Log.e("TTS", "Speak failed with code $result")
                releaseAudioFocus()
            }
        } catch (e: Exception) {
            Log.e("TTS", "Exception during speak: ${e.message}", e)
            _isPlaying.value = false
            releaseAudioFocus()
        }
    }

    fun pause() {
        pauseRequested = true
        tts?.stop()
        _isPlaying.value = false
    }

    fun resume() {
        // TTS doesn't have a true 'resume' for a specific block without re-sending text.
        // For now, _isPlaying is used as a state indicator.
    }

    fun stop() {
        pauseRequested = false
        tts?.stop()
        _isPlaying.value = false
    }

    fun setSpeed(speed: Float) {
        _settings.value = _settings.value.copy(speed = speed.coerceIn(0.5f, 2.0f))
        tts?.setSpeechRate(_settings.value.speed)
    }

    fun setPitch(pitch: Float) {
        _settings.value = _settings.value.copy(pitch = pitch.coerceIn(0.5f, 2.0f))
        tts?.setPitch(_settings.value.pitch)
    }

    fun setLanguage(language: String) {
        _settings.value = _settings.value.copy(language = language)
        val locale = Locale.forLanguageTag(language)
        tts?.language = locale
    }

    fun setVoice(voiceName: String) {
        val voice = tts?.voices?.find { it.name == voiceName }
        if (voice != null) {
            _settings.value = _settings.value.copy(
                voice = voice.name,
                language = voice.locale.toLanguageTag()
            )
            tts?.voice = voice
        }
    }

    private fun applyCurrentSettings() {
        val s = _settings.value
        tts?.setSpeechRate(s.speed)
        tts?.setPitch(s.pitch)
        
        if (s.voice != null) {
            val voice = tts?.voices?.find { it.name == s.voice }
            if (voice != null) {
                tts?.voice = voice
            }
        } else {
            val defaultVoice = tts?.defaultVoice
            if (defaultVoice != null) {
                tts?.voice = defaultVoice
            } else {
                val locale = Locale.forLanguageTag(s.language)
                tts?.language = locale
            }
        }
    }

    fun shutdown() {
        repositoryScope.cancel()
        tts?.shutdown()
        tts = null
    }
}
