package com.blurr.voice.utilities

import com.blurr.voice.BuildConfig
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages optional API keys without referencing deprecated client-side GEMINI_API_KEY fields.
 * Primary Gemini inference uses Firebase AI Logic + App Check with zero embedded keys.
 */
object ApiKeyManager {

    private val apiKeys: List<String> = BuildConfig.GEMINI_API_KEYS
        .split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }

    private val currentIndex = AtomicInteger(0)

    fun getNextKeyOrNull(): String? {
        if (apiKeys.isEmpty()) return null
        val index = Math.floorMod(currentIndex.getAndIncrement(), apiKeys.size)
        return apiKeys[index]
    }

    fun getNextKey(): String {
        return getNextKeyOrNull()
            ?: throw IllegalStateException("Optional key list is empty; Gemini uses Firebase AI Logic.")
    }
}