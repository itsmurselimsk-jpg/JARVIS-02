package com.blurr.voice.utilities

import android.content.Context
import com.blurr.voice.api.TTSVoice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VoiceLanguage(
    val displayLabel: String,
    val sttTag: String?,
    val ttsLanguageTag: String
) {
    AUTO("Auto (EN / HI)", null, "en-IN"),
    ENGLISH("English (India / US)", "en-IN", "en-IN"),
    HINDI("Hindi (हिन्दी)", "hi-IN", "hi-IN")
}

object VoicePreferenceManager {
    private const val PREFS_NAME = "BlurrSettings"
    private const val KEY_SELECTED_VOICE = "selected_voice"
    private const val KEY_VOICE_LANGUAGE = "voice_language"

    private val _selectedLanguage = MutableStateFlow(VoiceLanguage.AUTO)
    val selectedLanguage: StateFlow<VoiceLanguage> = _selectedLanguage.asStateFlow()

    fun getSelectedVoice(context: Context): TTSVoice {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val selectedVoiceName = sharedPreferences.getString(KEY_SELECTED_VOICE, TTSVoice.CHIRP_PUCK.name)
        return runCatching {
            TTSVoice.valueOf(selectedVoiceName ?: TTSVoice.CHIRP_PUCK.name)
        }.getOrDefault(TTSVoice.CHIRP_PUCK)
    }

    fun saveSelectedVoice(context: Context, voice: TTSVoice) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString(KEY_SELECTED_VOICE, voice.name)
            .apply()
    }

    fun getSelectedLanguage(context: Context): VoiceLanguage {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = sharedPreferences.getString(KEY_VOICE_LANGUAGE, VoiceLanguage.AUTO.name)
        val lang = runCatching {
            VoiceLanguage.valueOf(raw ?: VoiceLanguage.AUTO.name)
        }.getOrDefault(VoiceLanguage.AUTO)
        _selectedLanguage.value = lang
        return lang
    }

    fun saveSelectedLanguage(context: Context, language: VoiceLanguage) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString(KEY_VOICE_LANGUAGE, language.name)
            .apply()
        _selectedLanguage.value = language
    }
}