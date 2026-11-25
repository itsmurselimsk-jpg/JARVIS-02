package com.blurr.voice.utilities

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class VisionMode(val label: String, val description: String) {
    XML("XML Mode", "Uses accessibility hierarchy (faster, lower battery & latency)"),
    SCREENSHOT("Screenshot Mode", "Uses screen visual capture + hierarchy (higher visual accuracy)")
}

object JarvisPrefs {
    const val PREFS_NAME = "BlurrSettings"
    const val KEY_SHOW_THOUGHTS = "show_thoughts"
    private const val KEY_VISION_MODE = "vision_mode"

    private val _visionMode = MutableStateFlow(VisionMode.XML)
    val visionMode: StateFlow<VisionMode> = _visionMode.asStateFlow()

    private val _showThoughts = MutableStateFlow(false)
    val showThoughts: StateFlow<Boolean> = _showThoughts.asStateFlow()

    fun refresh(context: Context) {
        getVisionMode(context)
        isShowThoughts(context)
    }

    fun getVisionMode(context: Context): VisionMode {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_VISION_MODE, VisionMode.XML.name)
        val mode = runCatching { VisionMode.valueOf(raw ?: VisionMode.XML.name) }
            .getOrDefault(VisionMode.XML)
        _visionMode.value = mode
        return mode
    }

    fun setVisionMode(context: Context, mode: VisionMode) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_VISION_MODE, mode.name).apply()
        _visionMode.value = mode
    }

    fun isShowThoughts(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val enabled = prefs.getBoolean(KEY_SHOW_THOUGHTS, false)
        _showThoughts.value = enabled
        return enabled
    }

    fun setShowThoughts(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_SHOW_THOUGHTS, enabled).apply()
        _showThoughts.value = enabled
    }
}
