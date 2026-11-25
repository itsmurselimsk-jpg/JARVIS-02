package com.blurr.voice.v2.llm

import android.content.Context
import android.util.Log
import com.blurr.voice.utilities.ApiKeyManager
import com.blurr.voice.v2.AgentOutput
import com.blurr.voice.v2.logging.TaskLogger
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * Gemini v2 client powered by Firebase AI Logic (`Firebase.ai`) for structured JSON agent outputs.
 */
class GeminiApi(
    modelName: String,
    @Suppress("unused") private val apiKeyManager: ApiKeyManager,
    private val context: Context,
    private val maxRetry: Int = 3
) {

    companion object {
        private const val TAG = "GeminiV2Api"
        private const val DEFAULT_MODEL = "gemini-3.8-flash"
    }

    private val resolvedModelName: String =
        if (modelName.startsWith("gemini-3") || modelName == "gemini-4-argon") modelName else DEFAULT_MODEL

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val modelCache = ConcurrentHashMap<String, GenerativeModel>()

    private fun getJsonModel(): GenerativeModel = modelCache.getOrPut("$resolvedModelName|json") {
        Firebase.ai.generativeModel(
            modelName = resolvedModelName,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )
    }

    private fun getPlainModel(): GenerativeModel = modelCache.getOrPut("$resolvedModelName|plain") {
        Firebase.ai.generativeModel(modelName = resolvedModelName)
    }

    suspend fun generateAgentOutput(messages: List<GeminiMessage>): AgentOutput? {
        val jsonString = retryWithBackoff(times = maxRetry) {
            performFirebaseAiCall(messages)
        } ?: return null

        try {
            val input = jsonParser.encodeToString(messages)
            TaskLogger.log(context, input, jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to log task: ${e.message}")
        }

        return try {
            val cleaned = jsonString.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            jsonParser.decodeFromString<AgentOutput>(cleaned)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse JSON into AgentOutput: ${e.message}", e)
            null
        }
    }

    private suspend fun performFirebaseAiCall(messages: List<GeminiMessage>): String {
        val history = convertToSdkHistory(messages)
        val response = withTimeout(60_000L) {
            getJsonModel().generateContent(history)
        }
        response.text?.let {
            Log.d(TAG, "Received structured JSON response from $resolvedModelName.")
            return it
        }
        throw ContentBlockedException("Blocked or empty response from Firebase AI Logic.")
    }

    private fun convertToSdkHistory(messages: List<GeminiMessage>): List<Content> {
        return messages.mapNotNull { message ->
            val role = when (message.role) {
                MessageRole.USER -> "user"
                MessageRole.MODEL -> "model"
                MessageRole.TOOL -> "user"
            }
            val built = content(role) {
                message.parts.forEach { part ->
                    when (part) {
                        is TextPart -> if (part.text.isNotBlank()) text(part.text)
                    }
                }
            }
            built.takeIf { it.parts.isNotEmpty() }
        }
    }


    suspend fun generateGroundedContent(prompt: String): String? {
        return try {
            val response = withTimeout(60_000L) {
                getPlainModel().generateContent(prompt)
            }
            response.text
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Grounded query timed out", e)
            null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Firebase AI content call", e)
            null
        }
    }
}

class ContentBlockedException(message: String) : Exception(message)

private suspend fun <T> retryWithBackoff(
    times: Int,
    initialDelay: Long = 1000L,
    maxDelay: Long = 16000L,
    factor: Double = 2.0,
    block: suspend () -> T
): T? {
    var currentDelay = initialDelay
    repeat(times) { attempt ->
        try {
            return block()
        } catch (e: CancellationException) {
            if (e is TimeoutCancellationException) {
                Log.e("RetryUtil", "Attempt ${attempt + 1}/$times timed out", e)
            } else {
                throw e
            }
        } catch (e: Exception) {
            Log.e("RetryUtil", "Attempt ${attempt + 1}/$times failed: ${e.message}", e)
        }
        if (attempt == times - 1) {
            Log.e("RetryUtil", "All $times retry attempts failed.")
            return null
        }
        delay(currentDelay)
        currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
    }
    return null
}