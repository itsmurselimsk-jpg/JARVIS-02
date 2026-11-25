package com.blurr.voice.api

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import com.blurr.voice.MyApplication
import com.blurr.voice.utilities.NetworkConnectivityManager
import com.blurr.voice.utilities.NetworkNotifier
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.ImagePart
import com.google.firebase.ai.type.TextPart
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeout
import java.util.concurrent.ConcurrentHashMap

/**
 * Unified Gemini client powered by Firebase AI Logic (`Firebase.ai`) and App Check.
 * Uses zero embedded API keys and supports multimodal (text + Bitmap) and streaming inference.
 */
object GeminiApi {

    private const val TAG = "GeminiApi"
    const val DEFAULT_MODEL = "gemini-3.8-flash"
    const val LITE_MODEL = "gemini-3.5-flash-lite"

    private val modelCache = ConcurrentHashMap<String, GenerativeModel>()

    fun resolveSupportedModel(requested: String?): String {
        val raw = requested?.trim().orEmpty()
        return when {
            raw.contains("lite", ignoreCase = true) -> LITE_MODEL
            raw.startsWith("gemini-3") || raw == "gemini-4-argon" -> raw
            else -> DEFAULT_MODEL
        }
    }

    private fun getModel(modelName: String, jsonMode: Boolean = false): GenerativeModel {
        val resolved = resolveSupportedModel(modelName)
        val cacheKey = "$resolved|$jsonMode"
        return modelCache.getOrPut(cacheKey) {
            if (jsonMode) {
                Firebase.ai.generativeModel(
                    modelName = resolved,
                    generationConfig = generationConfig {
                        responseMimeType = "application/json"
                    }
                )
            } else {
                Firebase.ai.generativeModel(modelName = resolved)
            }
        }
    }

    suspend fun generateContent(
        chat: List<Pair<String, List<Any>>>,
        images: List<Bitmap> = emptyList(),
        modelName: String = DEFAULT_MODEL,
        maxRetry: Int = 3,
        context: Context? = null,
        jsonMode: Boolean = false
    ): String? {
        try {
            val appCtx = context ?: MyApplication.appContext
            val isOnline = NetworkConnectivityManager(appCtx).isNetworkAvailable()
            if (!isOnline) {
                Log.e(TAG, "No internet connection. Skipping generateContent call.")
                NetworkNotifier.notifyOffline()
                return null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network check failed: ${e.message}")
        }

        val resolvedModel = resolveSupportedModel(modelName)
        val sdkContents = toSdkContents(chat, images)
        if (sdkContents.isEmpty()) {
            Log.w(TAG, "Skipping empty prompt.")
            return null
        }

        var attempt = 0
        while (attempt < maxRetry) {
            try {
                val model = getModel(resolvedModel, jsonMode = jsonMode)
                val response = withTimeout(60_000L) {
                    model.generateContent(sdkContents)
                }
                val text = response.text?.trim()
                if (!text.isNullOrEmpty()) {
                    Log.d(TAG, "Model $resolvedModel succeeded on attempt ${attempt + 1}.")
                    return text
                }
                Log.w(TAG, "Empty response returned from $resolvedModel on attempt ${attempt + 1}.")
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Model $resolvedModel timed out on attempt ${attempt + 1}", e)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase AI inference failed on attempt ${attempt + 1}", e)
            }

            attempt++
            if (attempt < maxRetry) {
                delay(800L * attempt)
            }
        }
        return null
    }

    fun generateStream(
        prompt: String,
        history: List<Pair<Boolean, String>> = emptyList(),
        modelName: String = DEFAULT_MODEL
    ): Flow<String> {
        val model = getModel(modelName, jsonMode = false)
        val contents = mutableListOf<Content>()
        history.forEach { (isUser, text) ->
            if (text.isNotBlank()) {
                contents += content(if (isUser) "user" else "model") {
                    text(text)
                }
            }
        }
        contents += content("user") { text(prompt) }
        return model.generateContentStream(contents).mapNotNull { chunk -> chunk.text }
    }

    private fun toSdkContents(
        chat: List<Pair<String, List<Any>>>,
        extraImages: List<Bitmap>
    ): List<Content> {
        val result = mutableListOf<Content>()
        chat.forEachIndexed { index, (rawRole, parts) ->
            val role = when (rawRole.lowercase()) {
                "model", "assistant" -> "model"
                else -> "user"
            }
            val built = content(role) {
                parts.forEach { part ->
                    when (part) {
                        is TextPart -> if (part.text.isNotBlank()) text(part.text)
                        is ImagePart -> image(part.image)
                        is String -> if (part.isNotBlank()) text(part)
                        is Bitmap -> image(part)
                    }
                }
                if (index == chat.lastIndex && extraImages.isNotEmpty()) {
                    extraImages.forEach { bmp -> image(bmp) }
                }
            }
            if (built.parts.isNotEmpty()) {
                result.add(built)
            }
        }
        return result
    }
}