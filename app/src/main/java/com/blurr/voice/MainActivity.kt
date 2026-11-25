package com.blurr.voice

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.blurr.voice.ui.jarvis.JarvisHudShell
import com.blurr.voice.ui.jarvis.JarvisScreen
import com.blurr.voice.ui.jarvis.JarvisViewModel
import com.blurr.voice.ui.theme.JarvisTheme
import com.blurr.voice.utilities.Logger
import com.blurr.voice.utilities.UserProfileManager
import com.blurr.voice.utilities.configureAppCheck

class MainActivity : AppCompatActivity() {

    private val jarvisViewModel: JarvisViewModel by viewModels()

    companion object {
        const val ACTION_WAKE_WORD_FAILED = "com.blurr.voice.WAKE_WORD_FAILED"
        const val ACTION_PURCHASE_UPDATED = "com.blurr.voice.PURCHASE_UPDATED"
        const val EXTRA_OPEN_SCREEN = "extra_open_screen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val profileManager = UserProfileManager(this)
        if (!profileManager.isProfileComplete()) {
            profileManager.saveProfile("JARVIS User", "user@jarvis.local")
        }

        handleIntent(intent)

        setContent {
            JarvisTheme {
                JarvisHudShell(viewModel = jarvisViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            "com.blurr.voice.WAKE_UP_PANDA" -> {
                Logger.d("MainActivity", "Wake up JARVIS shortcut activated")
                jarvisViewModel.navigateTo(JarvisScreen.VOICE)
                if (!ConversationalAgentService.isRunning) {
                    val serviceIntent = Intent(this, ConversationalAgentService::class.java)
                    runCatching { ContextCompat.startForegroundService(this, serviceIntent) }
                }
            }
        }
        intent.getStringExtra(EXTRA_OPEN_SCREEN)?.let { screenName ->
            runCatching { JarvisScreen.valueOf(screenName) }.getOrNull()?.let { target ->
                jarvisViewModel.navigateTo(target)
            }
        }
    }
}