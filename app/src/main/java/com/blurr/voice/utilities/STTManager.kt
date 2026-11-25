package com.blurr.voice.utilities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class STTManager(private val context: Context) {

    companion object {
        private val _rmsLevelFlow = MutableStateFlow(0f)
        val rmsLevelFlow: StateFlow<Float> = _rmsLevelFlow.asStateFlow()

        private val _isListeningFlow = MutableStateFlow(false)
        val isListeningFlow: StateFlow<Boolean> = _isListeningFlow.asStateFlow()
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var onResultCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onListeningStateChange: ((Boolean) -> Unit)? = null
    private var onPartialResultCallback: ((String) -> Unit)? = null
    private var isInitialized = false
    private val visualizerManager = STTVisualizer(context)

    private fun initializeSpeechRecognizer() {
        if (isInitialized) return

        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer?.setRecognitionListener(createRecognitionListener())
                isInitialized = true
                Log.d("STTManager", "Speech recognizer initialized successfully")
            } catch (e: Exception) {
                Log.e("STTManager", "Failed to initialize speech recognizer", e)
            }
        } else {
            Log.e("STTManager", "Speech recognition not available on this device")
        }
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d("STTManager", "Ready for speech")
                isListening = true
                _isListeningFlow.value = true
                onListeningStateChange?.invoke(true)
            }

            override fun onBeginningOfSpeech() {
                Log.d("STTManager", "Beginning of speech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                _rmsLevelFlow.value = normalized
                visualizerManager.onRmsChanged(rmsdB)
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d("STTManager", "End of speech")
                isListening = false
                _isListeningFlow.value = false
                _rmsLevelFlow.value = 0f
                onListeningStateChange?.invoke(false)
                onPartialResultCallback = null
            }

            override fun onError(error: Int) {
                isListening = false
                _isListeningFlow.value = false
                _rmsLevelFlow.value = 0f
                onListeningStateChange?.invoke(false)
                visualizerManager.hide()

                val errorMessage = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                    SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                    SpeechRecognizer.ERROR_NETWORK -> "Network error"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    SpeechRecognizer.ERROR_SERVER -> "Server error"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
                    else -> "Unknown error: $error"
                }

                Log.e("STTManager", "Speech recognition error: $errorMessage")
                onErrorCallback?.invoke(errorMessage)
                onPartialResultCallback = null
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                _isListeningFlow.value = false
                _rmsLevelFlow.value = 0f
                onListeningStateChange?.invoke(false)
                visualizerManager.hide()

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0]
                    Log.d("STTManager", "Recognized text: $recognizedText")
                    onResultCallback?.invoke(recognizedText)
                } else {
                    Log.w("STTManager", "No results from speech recognition")
                    onErrorCallback?.invoke("No speech detected")
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val partialText = matches[0]
                    onPartialResultCallback?.invoke(partialText)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onListeningStateChange: (Boolean) -> Unit,
        onPartialResult: (String) -> Unit,
        languageOverride: VoiceLanguage? = null
    ) {
        if (isListening) {
            Log.w("STTManager", "Already listening")
            return
        }

        this.onResultCallback = onResult
        this.onErrorCallback = onError
        this.onListeningStateChange = onListeningStateChange
        this.onPartialResultCallback = onPartialResult

        val activeLanguage = languageOverride ?: VoicePreferenceManager.getSelectedLanguage(context)
        val languageTag = when (activeLanguage) {
            VoiceLanguage.HINDI -> "hi-IN"
            VoiceLanguage.ENGLISH -> "en-IN"
            VoiceLanguage.AUTO -> Locale.getDefault().toLanguageTag().ifBlank { "en-IN" }
        }

        CoroutineScope(Dispatchers.Main).launch {
            initializeSpeechRecognizer()

            if (speechRecognizer == null) {
                onError("Speech recognition not available")
                return@launch
            }
            visualizerManager.show()

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            try {
                speechRecognizer?.startListening(intent)
                Log.d("STTManager", "Started listening in language: $languageTag")
            } catch (e: Exception) {
                Log.e("STTManager", "Error starting speech recognition", e)
                isListening = false
                _isListeningFlow.value = false
                onError("Failed to start speech recognition: ${e.message}")
            }
        }
    }

    fun stopListening() {
        CoroutineScope(Dispatchers.Main).launch {
            if (isListening && speechRecognizer != null) {
                try {
                    speechRecognizer?.stopListening()
                    isListening = false
                    _isListeningFlow.value = false
                    _rmsLevelFlow.value = 0f
                    Log.d("STTManager", "Stopped listening")
                } catch (e: Exception) {
                    Log.e("STTManager", "Error stopping speech recognition", e)
                }
            }
        }
    }

    fun isCurrentlyListening(): Boolean = isListening

    fun shutdown() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("STTManager", "Error destroying speech recognizer", e)
        }
        visualizerManager.hide()
        speechRecognizer = null
        isListening = false
        _isListeningFlow.value = false
        _rmsLevelFlow.value = 0f
        isInitialized = false
        Log.d("STTManager", "STT Manager shutdown")
    }
}