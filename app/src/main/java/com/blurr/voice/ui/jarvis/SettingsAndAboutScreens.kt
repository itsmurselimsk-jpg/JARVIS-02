package com.blurr.voice.ui.jarvis

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blurr.voice.BuildConfig
import com.blurr.voice.MyApplication
import com.blurr.voice.PermissionsActivity
import com.blurr.voice.PrivacyActivity
import com.blurr.voice.api.GoogleTts
import com.blurr.voice.ui.theme.JarvisBlack
import com.blurr.voice.ui.theme.JarvisCyan
import com.blurr.voice.ui.theme.JarvisElectricBlue
import com.blurr.voice.ui.theme.JarvisEmerald
import com.blurr.voice.ui.theme.JarvisGlassElevated
import com.blurr.voice.ui.theme.JarvisGlassSurface
import com.blurr.voice.ui.theme.JarvisOutline
import com.blurr.voice.ui.theme.JarvisTextMuted
import com.blurr.voice.ui.theme.JarvisTextPrimary
import com.blurr.voice.ui.theme.JarvisTextSecondary
import com.blurr.voice.utilities.PandaState
import com.blurr.voice.utilities.VoiceLanguage

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    viewModel: JarvisViewModel
) {
    val context = LocalContext.current
    val voiceLanguage by viewModel.voiceLanguage.collectAsStateWithLifecycle()
    val selectedVoice by viewModel.selectedVoice.collectAsStateWithLifecycle()
    val showThoughts by viewModel.showThoughts.collectAsStateWithLifecycle()
    val storedKey by viewModel.picovoiceKey.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()

    val voices = remember { GoogleTts.getAvailableVoices() }
    var wakeKeyInput by rememberSaveable(storedKey) { mutableStateOf(storedKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Bilingual Voice Language
        JarvisGlassCard {
            SectionHeader(
                title = "Voice & Speech Language",
                subtitle = "Applies to Speech-to-Text and Text-to-Speech"
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VoiceLanguage.entries.forEach { lang ->
                    val selected = voiceLanguage == lang
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setVoiceLanguage(lang) },
                        label = { Text(lang.displayLabel) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JarvisElectricBlue.copy(alpha = 0.25f),
                            selectedLabelColor = JarvisCyan,
                            containerColor = JarvisGlassElevated,
                            labelColor = JarvisTextSecondary
                        ),
                        border = BorderStroke(1.dp, if (selected) JarvisCyan else JarvisOutline)
                    )
                }
            }
        }

        // TTS Voice Profile
        JarvisGlassCard {
            SectionHeader(
                title = "TTS Voice Profile",
                subtitle = if (GoogleTts.apiKey.isBlank()) {
                    "Using on-device bilingual Android TTS engine (no cloud TTS key needed)"
                } else "Google Cloud Neural TTS active"
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                voices.forEach { voice ->
                    val selected = selectedVoice == voice
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.selectVoice(voice) },
                        label = { Text(voice.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JarvisElectricBlue.copy(alpha = 0.25f),
                            selectedLabelColor = JarvisCyan,
                            containerColor = JarvisGlassElevated,
                            labelColor = JarvisTextSecondary
                        ),
                        border = BorderStroke(1.dp, if (selected) JarvisCyan else JarvisOutline)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            JarvisGlowButton(
                text = "Test Selected Voice",
                icon = Icons.Filled.RecordVoiceOver,
                outlined = true,
                onClick = { viewModel.testSelectedVoice() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_test_voice")
            )
        }

        // Agent Thoughts Overlay
        JarvisGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Show Agent Thoughts Overlay", style = MaterialTheme.typography.titleMedium, color = JarvisTextPrimary)
                    Text(
                        text = "Display live step reasoning HUD while JARVIS automates tasks",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )
                }
                Switch(
                    checked = showThoughts,
                    onCheckedChange = { viewModel.setShowThoughts(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisBlack,
                        checkedTrackColor = JarvisCyan
                    ),
                    modifier = Modifier.testTag("settings_show_thoughts_switch")
                )
            }
        }

        // Wake Word Settings
        JarvisGlassCard {
            SectionHeader(
                title = "Porcupine Wake Word (Optional)",
                subtitle = "Hands-free background listener using your personal Picovoice key"
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = wakeKeyInput,
                onValueChange = { wakeKeyInput = it },
                placeholder = { Text("Paste Picovoice AccessKey...", color = JarvisTextMuted) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = JarvisBlack,
                    unfocusedContainerColor = JarvisBlack,
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisOutline,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                JarvisGlowButton(
                    text = "Save Key",
                    icon = Icons.Filled.Key,
                    outlined = true,
                    onClick = { viewModel.savePicovoiceKey(wakeKeyInput) },
                    modifier = Modifier.weight(1f)
                )
                JarvisGlowButton(
                    text = if (telemetry.isWakeWordRunning) "Disable Wake" else "Enable Wake",
                    icon = Icons.Filled.Hearing,
                    onClick = {
                        if (wakeKeyInput.isNotBlank()) viewModel.savePicovoiceKey(wakeKeyInput)
                        viewModel.toggleWakeWordService()
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Permissions & Battery Links
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            JarvisGlowButton(
                text = "Permissions Hub",
                icon = Icons.Filled.Security,
                outlined = true,
                onClick = { context.startActivity(Intent(context, PermissionsActivity::class.java)) },
                modifier = Modifier.weight(1f)
            )
            JarvisGlowButton(
                text = "Battery Guide",
                icon = Icons.Filled.BatteryAlert,
                outlined = true,
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://dontkillmyapp.com/"))
                    runCatching { context.startActivity(intent) }
                        .onFailure { Toast.makeText(context, "No browser available", Toast.LENGTH_SHORT).show() }
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun AboutJarvisScreen(
    viewModel: JarvisViewModel
) {
    val context = LocalContext.current
    val billingStatus by viewModel.billingStatus.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        JarvisGlassCard(borderColor = JarvisCyan.copy(alpha = 0.5f)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                JarvisArcReactor(state = PandaState.IDLE, size = 120.dp)
                Text("JARVIS AI", style = MaterialTheme.typography.headlineMedium, color = JarvisCyan)
                Text(
                    text = "Version ${BuildConfig.VERSION_NAME} • Holographic Android Assistant",
                    style = MaterialTheme.typography.labelMedium,
                    color = JarvisTextSecondary
                )
                JarvisStatusPill(
                    text = if (billingStatus == MyApplication.BillingStatus.AVAILABLE) {
                        "PLAY BILLING READY"
                    } else {
                        "UNLIMITED LOCAL MODE ACTIVE"
                    },
                    color = JarvisEmerald,
                    pulsing = false
                )
            }
        }

        JarvisGlassCard {
            SectionHeader(title = "Neural Engine & Security")
            Spacer(modifier = Modifier.height(8.dp))
            AboutRow("Primary Reasoning Model", "gemini-3.8-flash (Firebase AI Logic)")
            AboutRow("Fast Perception Model", "gemini-3.5-flash-lite")
            AboutRow("API Key Security", "Firebase App Check active • Zero embedded API keys")
            AboutRow("Voice Support", "Bilingual English (en-IN) & Hindi (hi-IN)")
            AboutRow("Persistence", "Local Room database (Memories & Chat Transcripts)")
        }

        JarvisGlassCard {
            SectionHeader(title = "Accessibility & Privacy Promise")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "JARVIS AI accesses screen UI trees or screenshots strictly when you initiate an automation task or Screen Assistant scan. No secrets or Gemini API keys are packaged inside the APK.",
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisTextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            JarvisGlowButton(
                text = "View Memory & Privacy Architecture",
                icon = Icons.Filled.VerifiedUser,
                outlined = true,
                onClick = { context.startActivity(Intent(context, PrivacyActivity::class.java)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("about_privacy_button")
            )
        }
    }
}

@Composable
private fun AboutRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = JarvisCyan)
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = JarvisTextPrimary)
    }
}
