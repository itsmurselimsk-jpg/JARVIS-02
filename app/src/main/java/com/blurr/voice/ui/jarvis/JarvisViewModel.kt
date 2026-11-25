package com.blurr.voice.ui.jarvis

import android.Manifest
import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.Settings
import android.text.TextUtils
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.blurr.voice.ConversationalAgentService
import com.blurr.voice.MyApplication
import com.blurr.voice.ScreenInteractionService
import com.blurr.voice.api.Eyes
import com.blurr.voice.api.Finger
import com.blurr.voice.api.GeminiApi
import com.blurr.voice.api.GoogleTts
import com.blurr.voice.api.PicovoiceKeyManager
import com.blurr.voice.api.TTSVoice
import com.blurr.voice.data.AppDatabase
import com.blurr.voice.data.ChatMessageEntity
import com.blurr.voice.data.Memory
import com.blurr.voice.services.EnhancedWakeWordService
import com.blurr.voice.services.FloatingPandaButtonService
import com.blurr.voice.utilities.JarvisPrefs
import com.blurr.voice.utilities.PandaState
import com.blurr.voice.utilities.PandaStateManager
import com.blurr.voice.utilities.STTManager
import com.blurr.voice.utilities.TTSManager
import com.blurr.voice.utilities.VisionMode
import com.blurr.voice.utilities.VoiceLanguage
import com.blurr.voice.utilities.VoicePreferenceManager
import com.blurr.voice.v2.AgentService
import com.blurr.voice.v2.logging.TaskLog
import com.blurr.voice.v2.logging.TaskLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class JarvisScreen(val title: String, val subtitle: String) {
    HOME("JARVIS AI", "Neural Core & Command Hub"),
    CHAT("AI Chat", "Bilingual Gemini Stream"),
    VOICE("Voice Assistant", "English & Hindi Speech Core"),
    AUTOMATION("Automation Center", "Device Task Execution"),
    SCREEN_ASSISTANT("Screen Assistant", "Live Screen Intelligence"),
    APP_LAUNCHER("App Launcher", "Instant Application Matrix"),
    DEVICE_CONTROL("Device Control", "Hardware & System Panels"),
    HISTORY("Conversation History", "Chats, Task Logs & Memory"),
    SETTINGS("Settings", "Voice, Vision & Security"),
    ABOUT("About JARVIS", "Architecture & Privacy")
}

data class InstalledAppInfo(
    val label: String,
    val packageName: String,
    val icon: Drawable?
)

data class TelemetryState(
    val hasMicPermission: Boolean = false,
    val hasAccessibility: Boolean = false,
    val hasOverlay: Boolean = false,
    val hasNotifications: Boolean = true,
    val isDefaultAssistant: Boolean = false,
    val hasFlashlight: Boolean = false,
    val isTorchOn: Boolean = false,
    val isFloatingHudRunning: Boolean = false,
    val isWakeWordRunning: Boolean = false,
    val isAgentRunning: Boolean = false,
    val activeAgentTask: String? = null,
    val mediaVolume: Int = 5,
    val mediaMaxVolume: Int = 15,
    val ringVolume: Int = 5,
    val ringMaxVolume: Int = 7
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val db = AppDatabase.getDatabase(appContext)
    private val chatDao = db.chatMessageDao()
    private val memoryDao = db.memoryDao()
    private val stateManager = PandaStateManager.getInstance(appContext)
    private val sttManager = STTManager(appContext)
    private val ttsManager = TTSManager.getInstance(appContext)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val cameraManager = appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager

    private val _currentScreen = MutableStateFlow(JarvisScreen.HOME)
    val currentScreen: StateFlow<JarvisScreen> = _currentScreen.asStateFlow()
    private val backStack = ArrayDeque<JarvisScreen>()

    private val _telemetry = MutableStateFlow(TelemetryState())
    val telemetry: StateFlow<TelemetryState> = _telemetry.asStateFlow()

    private val _voiceState = MutableStateFlow(stateManager.getCurrentState())
    val voiceState: StateFlow<PandaState> = _voiceState.asStateFlow()

    val rmsLevel: StateFlow<Float> = STTManager.rmsLevelFlow
    val isListening: StateFlow<Boolean> = STTManager.isListeningFlow
    val isSpeaking: StateFlow<Boolean> = TTSManager.isSpeakingFlow
    val voiceLanguage: StateFlow<VoiceLanguage> = VoicePreferenceManager.selectedLanguage
    val visionMode: StateFlow<VisionMode> = JarvisPrefs.visionMode
    val showThoughts: StateFlow<Boolean> = JarvisPrefs.showThoughts
    val billingStatus = MyApplication.billingStatus

    val serviceTranscript: StateFlow<String> = ConversationalAgentService.latestTranscript
    val serviceReply: StateFlow<String> = ConversationalAgentService.latestReply

    val chatMessages: StateFlow<List<ChatMessageEntity>> =
        chatDao.observeMessagesForSession(CHAT_SESSION_ID)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMemories: StateFlow<List<Memory>> =
        memoryDao.getAllMemories()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _streamingReply = MutableStateFlow("")
    val streamingReply: StateFlow<String> = _streamingReply.asStateFlow()

    private val _isChatGenerating = MutableStateFlow(false)
    val isChatGenerating: StateFlow<Boolean> = _isChatGenerating.asStateFlow()

    private val _voicePartialText = MutableStateFlow("")
    val voicePartialText: StateFlow<String> = _voicePartialText.asStateFlow()

    private val _voiceReplyText = MutableStateFlow("Tap the core or speak in English or Hindi.")
    val voiceReplyText: StateFlow<String> = _voiceReplyText.asStateFlow()

    private val _voiceErrorText = MutableStateFlow<String?>(null)
    val voiceErrorText: StateFlow<String?> = _voiceErrorText.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _taskLogs = MutableStateFlow<List<TaskLog>>(emptyList())
    val taskLogs: StateFlow<List<TaskLog>> = _taskLogs.asStateFlow()

    private val _screenSummary = MutableStateFlow("")
    val screenSummary: StateFlow<String> = _screenSummary.asStateFlow()

    private val _screenActivityName = MutableStateFlow("")
    val screenActivityName: StateFlow<String> = _screenActivityName.asStateFlow()

    private val _isAnalyzingScreen = MutableStateFlow(false)
    val isAnalyzingScreen: StateFlow<Boolean> = _isAnalyzingScreen.asStateFlow()

    private val _selectedVoice = MutableStateFlow(VoicePreferenceManager.getSelectedVoice(appContext))
    val selectedVoice: StateFlow<TTSVoice> = _selectedVoice.asStateFlow()

    private val _picovoiceKey = MutableStateFlow(PicovoiceKeyManager(appContext).getUserProvidedKey().orEmpty())
    val picovoiceKey: StateFlow<String> = _picovoiceKey.asStateFlow()

    private var chatJob: Job? = null
    private var torchCameraId: String? = null

    private val stateListener: (PandaState) -> Unit = { newState ->
        _voiceState.value = newState
    }

    init {
        JarvisPrefs.refresh(appContext)
        VoicePreferenceManager.getSelectedLanguage(appContext)
        stateManager.addStateChangeListener(stateListener)
        findTorchCamera()
        refreshTelemetry()
        loadInstalledApps()
        refreshTaskLogs()
    }

    fun navigateTo(screen: JarvisScreen) {
        if (_currentScreen.value == screen) return
        if (screen == JarvisScreen.HOME) {
            backStack.clear()
        } else {
            backStack.addLast(_currentScreen.value)
        }
        _currentScreen.value = screen
        refreshTelemetry()
        if (screen == JarvisScreen.HISTORY || screen == JarvisScreen.AUTOMATION) {
            refreshTaskLogs()
        }
        if (screen == JarvisScreen.APP_LAUNCHER && _installedApps.value.isEmpty()) {
            loadInstalledApps()
        }
    }

    fun navigateBack(): Boolean {
        if (backStack.isNotEmpty()) {
            _currentScreen.value = backStack.removeLast()
            return true
        }
        if (_currentScreen.value != JarvisScreen.HOME) {
            _currentScreen.value = JarvisScreen.HOME
            return true
        }
        return false
    }

    fun refreshTelemetry() {
        val micGranted = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val notifGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true
        val overlayGranted = Settings.canDrawOverlays(appContext)
        val a11yEnabled = isAccessibilityEnabled()
        val assistantHeld = isDefaultAssistant()

        val mediaMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val mediaCur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val ringMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING).coerceAtLeast(1)
        val ringCur = audioManager.getStreamVolume(AudioManager.STREAM_RING)

        _telemetry.value = _telemetry.value.copy(
            hasMicPermission = micGranted,
            hasAccessibility = a11yEnabled,
            hasOverlay = overlayGranted,
            hasNotifications = notifGranted,
            isDefaultAssistant = assistantHeld,
            hasFlashlight = torchCameraId != null,
            isFloatingHudRunning = FloatingPandaButtonService.isRunning,
            isWakeWordRunning = EnhancedWakeWordService.isRunning,
            isAgentRunning = AgentService.isRunning,
            activeAgentTask = AgentService.currentTask,
            mediaVolume = mediaCur,
            mediaMaxVolume = mediaMax,
            ringVolume = ringCur,
            ringMaxVolume = ringMax
        )
    }

    private fun isAccessibilityEnabled(): Boolean {
        if (ScreenInteractionService.instance != null) return true
        val expected = "${appContext.packageName}/${ScreenInteractionService::class.java.canonicalName}"
        val enabled = Settings.Secure.getInt(appContext.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        if (enabled != 1) return false
        val settingValue = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(settingValue)
        while (splitter.hasNext()) {
            if (splitter.next().equals(expected, ignoreCase = true)) return true
        }
        return false
    }

    private fun isDefaultAssistant(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val rm = appContext.getSystemService(RoleManager::class.java)
            rm?.isRoleHeld(RoleManager.ROLE_ASSISTANT) == true
        } else {
            val flat = Settings.Secure.getString(appContext.contentResolver, "voice_interaction_service")
            flat?.substringBefore('/') == appContext.packageName
        }
    }

    // --- Voice Assistant & Bilingual Speech Core ---

    fun setVoiceLanguage(language: VoiceLanguage) {
        VoicePreferenceManager.saveSelectedLanguage(appContext, language)
    }

    fun startInAppVoiceTurn() {
        if (!_telemetry.value.hasMicPermission) {
            _voiceErrorText.value = "Microphone permission is required for voice commands."
            return
        }
        _voiceErrorText.value = null
        _voicePartialText.value = ""
        ttsManager.stop()
        stateManager.setState(PandaState.LISTENING)
        _voiceState.value = PandaState.LISTENING

        sttManager.startListening(
            onResult = { finalText ->
                _voicePartialText.value = finalText
                handleVoiceTranscript(finalText)
            },
            onError = { err ->
                _voiceErrorText.value = err
                stateManager.setState(PandaState.IDLE)
                _voiceState.value = PandaState.IDLE
            },
            onListeningStateChange = { listening ->
                if (!listening && _voiceState.value == PandaState.LISTENING) {
                    _voiceState.value = PandaState.PROCESSING
                }
            },
            onPartialResult = { partial ->
                _voicePartialText.value = partial
            }
        )
    }

    fun stopInAppVoice() {
        sttManager.stopListening()
        ttsManager.stop()
        stateManager.setState(PandaState.IDLE)
        _voiceState.value = PandaState.IDLE
    }

    private fun handleVoiceTranscript(transcript: String) {
        val clean = transcript.trim()
        if (clean.isEmpty()) {
            stateManager.setState(PandaState.IDLE)
            _voiceState.value = PandaState.IDLE
            return
        }
        viewModelScope.launch {
            stateManager.setState(PandaState.PROCESSING)
            _voiceState.value = PandaState.PROCESSING
            val langTag = if (clean.any { it in '\u0900'..'\u097F' }) "hi" else "en"
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = CHAT_SESSION_ID,
                    role = "user",
                    content = clean,
                    languageTag = langTag
                )
            )
            val systemContext = buildString {
                append("You are JARVIS AI, a concise, intelligent holographic smartphone assistant. ")
                append("Respond naturally in the user's language (English, Hindi/हिन्दी, or Hinglish). ")
                append("Keep voice replies under 3 crisp sentences unless asked for detail.\n")
                append("User said: ").append(clean)
            }
            val reply = withContext(Dispatchers.IO) {
                GeminiApi.generateContent(
                    chat = listOf("user" to listOf(systemContext)),
                    modelName = GeminiApi.DEFAULT_MODEL,
                    context = appContext
                )
            } ?: "I could not reach the Gemini neural core right now. Please check your connection."

            _voiceReplyText.value = reply
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = CHAT_SESSION_ID,
                    role = "model",
                    content = reply,
                    languageTag = if (reply.any { it in '\u0900'..'\u097F' }) "hi" else "en"
                )
            )
            stateManager.setState(PandaState.SPEAKING)
            _voiceState.value = PandaState.SPEAKING
            ttsManager.speakToUser(reply)
            stateManager.setState(PandaState.IDLE)
            _voiceState.value = PandaState.IDLE
        }
    }

    fun toggleSystemVoiceOverlay() {
        if (!ConversationalAgentService.isRunning) {
            val intent = Intent(appContext, ConversationalAgentService::class.java)
            ContextCompat.startForegroundService(appContext, intent)
            Toast.makeText(appContext, "JARVIS Voice Core activated", Toast.LENGTH_SHORT).show()
        } else {
            val stopIntent = Intent(appContext, ConversationalAgentService::class.java).apply {
                action = ConversationalAgentService.ACTION_STOP_SERVICE
            }
            appContext.startService(stopIntent)
        }
        refreshTelemetry()
    }

    // --- Streaming AI Chat ---

    fun sendChatMessage(rawPrompt: String) {
        val prompt = rawPrompt.trim()
        if (prompt.isEmpty() || _isChatGenerating.value) return

        chatJob?.cancel()
        chatJob = viewModelScope.launch {
            _isChatGenerating.value = true
            _streamingReply.value = ""
            val langTag = if (prompt.any { it in '\u0900'..'\u097F' }) "hi" else "en"
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = CHAT_SESSION_ID,
                    role = "user",
                    content = prompt,
                    languageTag = langTag
                )
            )

            val historyPairs = chatMessages.value.takeLast(12).map { msg ->
                (msg.role == "user") to msg.content
            }
            val systemHeader = "You are JARVIS AI, an advanced smartphone assistant fluent in English, Hindi (हिन्दी), and Hinglish. Match the user's language and keep responses helpful and accurate."
            val combinedPrompt = "$systemHeader\n\nUser: $prompt"

            val buffer = StringBuilder()
            try {
                withContext(Dispatchers.IO) {
                    GeminiApi.generateStream(
                        prompt = combinedPrompt,
                        history = historyPairs,
                        modelName = GeminiApi.DEFAULT_MODEL
                    ).collect { chunk ->
                        buffer.append(chunk)
                        _streamingReply.value = buffer.toString()
                    }
                }
            } catch (_: Exception) {
                if (buffer.isEmpty()) {
                    val fallback = withContext(Dispatchers.IO) {
                        GeminiApi.generateContent(
                            chat = listOf("user" to listOf(combinedPrompt)),
                            context = appContext
                        )
                    }
                    if (!fallback.isNullOrBlank()) buffer.append(fallback)
                }
            }

            val finalText = buffer.toString().trim().ifBlank {
                "Unable to reach Gemini right now. Please verify your network connection and try again."
            }
            val isActionable = looksActionableCommand(prompt)
            chatDao.insertMessage(
                ChatMessageEntity(
                    sessionId = CHAT_SESSION_ID,
                    role = "model",
                    content = finalText,
                    languageTag = if (finalText.any { it in '\u0900'..'\u097F' }) "hi" else "en",
                    isActionable = isActionable
                )
            )
            _streamingReply.value = ""
            _isChatGenerating.value = false
        }
    }

    private fun looksActionableCommand(text: String): Boolean {
        val lower = text.lowercase()
        val keywords = listOf("open ", "launch ", "send ", "call ", "set alarm", "search ", "scroll", "tap", "खोलो", "चलाओ", "भेजो")
        return keywords.any { lower.contains(it) }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatDao.clearSession(CHAT_SESSION_ID)
        }
    }

    // --- Automation & Screen Assistant ---

    fun runAutomationTask(task: String): Boolean {
        val trimmed = task.trim()
        if (trimmed.isEmpty()) return false
        refreshTelemetry()
        if (!_telemetry.value.hasAccessibility) {
            Toast.makeText(appContext, "Enable JARVIS Accessibility Service first", Toast.LENGTH_LONG).show()
            openAccessibilitySettings()
            return false
        }
        AgentService.start(appContext, trimmed)
        Toast.makeText(appContext, "JARVIS executing: $trimmed", Toast.LENGTH_SHORT).show()
        refreshTelemetry()
        return true
    }

    fun stopAutomationTask() {
        AgentService.stop(appContext)
        refreshTelemetry()
    }

    fun setVisionMode(mode: VisionMode) {
        JarvisPrefs.setVisionMode(appContext, mode)
    }

    fun toggleFloatingHud() {
        refreshTelemetry()
        if (!_telemetry.value.hasOverlay) {
            openOverlaySettings()
            return
        }
        val intent = Intent(appContext, FloatingPandaButtonService::class.java)
        if (FloatingPandaButtonService.isRunning) {
            appContext.stopService(intent)
        } else {
            appContext.startService(intent)
        }
        refreshTelemetry()
    }

    fun inspectCurrentScreen() {
        refreshTelemetry()
        if (!_telemetry.value.hasAccessibility) {
            _screenSummary.value = "Accessibility Service is currently disabled. Grant Accessibility permission to inspect live screen UI context."
            return
        }
        viewModelScope.launch {
            _isAnalyzingScreen.value = true
            try {
                val eyes = Eyes(appContext)
                val activity = withContext(Dispatchers.IO) { eyes.getCurrentActivityName() }
                val xml = withContext(Dispatchers.IO) { eyes.openXMLEyes() }
                _screenActivityName.value = activity
                val prompt = "Summarize this Android screen state clearly in 3 bullet points (what app/screen is open and key available actions):\nActivity: $activity\nUI Elements:\n${xml.take(4500)}"
                val analysis = withContext(Dispatchers.IO) {
                    GeminiApi.generateContent(
                        chat = listOf("user" to listOf(prompt)),
                        modelName = GeminiApi.LITE_MODEL,
                        context = appContext
                    )
                }
                _screenSummary.value = analysis ?: "Active Window: $activity\nCaptured ${xml.length} characters of UI context."
            } catch (e: Exception) {
                _screenSummary.value = "Screen inspection error: ${e.message ?: "Unknown"}"
            } finally {
                _isAnalyzingScreen.value = false
            }
        }
    }

    // --- App Launcher ---

    fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = appContext.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val apps = pm.queryIntentActivities(mainIntent, 0)
                .mapNotNull { resolveInfo ->
                    val pkg = resolveInfo.activityInfo?.packageName ?: return@mapNotNull null
                    if (pkg == appContext.packageName) return@mapNotNull null
                    val label = resolveInfo.loadLabel(pm)?.toString().orEmpty().ifBlank { pkg }
                    val icon = runCatching { resolveInfo.loadIcon(pm) }.getOrNull()
                    InstalledAppInfo(label = label, packageName = pkg, icon = icon)
                }
                .distinctBy { it.packageName }
                .sortedBy { it.label.lowercase() }
            _installedApps.value = apps
        }
    }

    fun launchApp(packageName: String) {
        val ok = Finger(appContext).openApp(packageName)
        if (!ok) {
            Toast.makeText(appContext, "Unable to launch $packageName", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Device Control (Safe APIs & System Panels) ---

    fun setMediaVolume(value: Int) {
        val clamped = value.coerceIn(0, _telemetry.value.mediaMaxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, clamped, 0)
        refreshTelemetry()
    }

    fun setRingVolume(value: Int) {
        val clamped = value.coerceIn(0, _telemetry.value.ringMaxVolume)
        runCatching { audioManager.setStreamVolume(AudioManager.STREAM_RING, clamped, 0) }
        refreshTelemetry()
    }

    private fun findTorchCamera() {
        runCatching {
            val cm = cameraManager ?: return
            for (id in cm.cameraIdList) {
                val chars = cm.getCameraCharacteristics(id)
                val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (hasFlash) {
                    torchCameraId = id
                    break
                }
            }
        }
    }

    fun toggleFlashlight() {
        val id = torchCameraId ?: run {
            Toast.makeText(appContext, "No camera flash detected on this device", Toast.LENGTH_SHORT).show()
            return
        }
        val next = !_telemetry.value.isTorchOn
        runCatching {
            cameraManager?.setTorchMode(id, next)
            _telemetry.value = _telemetry.value.copy(isTorchOn = next)
        }.onFailure {
            Toast.makeText(appContext, "Could not toggle flashlight: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWifiPanel() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Intent(Settings.Panel.ACTION_WIFI)
        } else {
            Intent(Settings.ACTION_WIFI_SETTINGS)
        }
        launchSystemIntent(intent, Intent(Settings.ACTION_WIFI_SETTINGS))
    }

    fun openBluetoothSettings() {
        launchSystemIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    }

    fun openDisplaySettings() {
        launchSystemIntent(Intent(Settings.ACTION_DISPLAY_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    }

    fun openBatterySettings() {
        launchSystemIntent(Intent(Intent.ACTION_POWER_USAGE_SUMMARY), Intent(Settings.ACTION_SETTINGS))
    }

    fun openAlarmsApp() {
        launchSystemIntent(Intent(AlarmClock.ACTION_SHOW_ALARMS), Intent(Settings.ACTION_DATE_SETTINGS))
    }

    fun openAccessibilitySettings() {
        launchSystemIntent(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS), Intent(Settings.ACTION_SETTINGS))
    }

    fun openOverlaySettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${appContext.packageName}"))
        launchSystemIntent(intent, Intent(Settings.ACTION_SETTINGS))
    }

    private fun launchSystemIntent(primary: Intent, fallback: Intent) {
        val pm = appContext.packageManager
        val chosen = if (primary.resolveActivity(pm) != null) primary else fallback
        chosen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { appContext.startActivity(chosen) }
            .onFailure { Toast.makeText(appContext, "Settings screen unavailable", Toast.LENGTH_SHORT).show() }
    }

    // --- History, Memories & Settings ---

    fun refreshTaskLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            _taskLogs.value = TaskLogger.getLogs(appContext)
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryDao.deleteMemoryById(id)
        }
    }

    fun selectVoice(voice: TTSVoice) {
        VoicePreferenceManager.saveSelectedVoice(appContext, voice)
        _selectedVoice.value = voice
    }

    fun testSelectedVoice() {
        viewModelScope.launch {
            val lang = VoicePreferenceManager.getSelectedLanguage(appContext)
            val sample = if (lang == VoiceLanguage.HINDI) {
                "नमस्ते, मैं जार्विस हूँ। सभी सिस्टम ऑनलाइन हैं।"
            } else {
                "JARVIS online. Bilingual voice core is calibrated and ready."
            }
            ttsManager.speakToUser(sample)
        }
    }

    fun setShowThoughts(enabled: Boolean) {
        JarvisPrefs.setShowThoughts(appContext, enabled)
    }

    fun savePicovoiceKey(key: String) {
        val trimmed = key.trim()
        PicovoiceKeyManager(appContext).saveUserProvidedKey(trimmed)
        _picovoiceKey.value = trimmed
        Toast.makeText(appContext, "Wake word AccessKey saved", Toast.LENGTH_SHORT).show()
    }

    fun toggleWakeWordService() {
        if (EnhancedWakeWordService.isRunning) {
            appContext.stopService(Intent(appContext, EnhancedWakeWordService::class.java))
        } else {
            if (!_telemetry.value.hasMicPermission) {
                Toast.makeText(appContext, "Grant Microphone permission first", Toast.LENGTH_SHORT).show()
                return
            }
            val key = PicovoiceKeyManager(appContext).getUserProvidedKey()
            if (key.isNullOrBlank()) {
                Toast.makeText(appContext, "Enter a Picovoice AccessKey first", Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(appContext, EnhancedWakeWordService::class.java).apply {
                putExtra(EnhancedWakeWordService.EXTRA_USE_PORCUPINE, true)
            }
            ContextCompat.startForegroundService(appContext, intent)
        }
        refreshTelemetry()
    }

    override fun onCleared() {
        super.onCleared()
        stateManager.removeStateChangeListener(stateListener)
        sttManager.shutdown()
    }

    companion object {
        const val CHAT_SESSION_ID = "jarvis_main"
    }
}
