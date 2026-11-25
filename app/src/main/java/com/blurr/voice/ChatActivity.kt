package com.blurr.voice

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.blurr.voice.ui.jarvis.ChatScreen
import com.blurr.voice.ui.jarvis.JarvisViewModel
import com.blurr.voice.ui.theme.JarvisBlack
import com.blurr.voice.ui.theme.JarvisCyan
import com.blurr.voice.ui.theme.JarvisTextPrimary
import com.blurr.voice.ui.theme.JarvisTheme
import com.blurr.voice.utilities.configureAppCheck

class ChatActivity : AppCompatActivity() {

    private val jarvisViewModel: JarvisViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureAppCheck(this, intent)
        enableEdgeToEdge()

        val initialPrompt = intent.getStringExtra("custom_message")
            ?.takeIf { it.isNotBlank() && it != "Hello! How can I helpy?" }

        setContent {
            JarvisTheme {
                val micLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    jarvisViewModel.refreshTelemetry()
                    if (!granted) {
                        Toast.makeText(this, "Microphone permission denied", Toast.LENGTH_SHORT).show()
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(JarvisBlack)
                        .statusBarsPadding()
                        .navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { finish() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = JarvisCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "JARVIS AI CHAT",
                            style = MaterialTheme.typography.titleLarge,
                            color = JarvisTextPrimary
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ChatScreen(
                            viewModel = jarvisViewModel,
                            onRequestMicPermission = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                            initialPrompt = initialPrompt
                        )
                    }
                }
            }
        }
    }
}
