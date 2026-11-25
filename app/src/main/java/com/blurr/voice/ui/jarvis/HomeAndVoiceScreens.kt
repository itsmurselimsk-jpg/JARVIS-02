package com.blurr.voice.ui.jarvis

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blurr.voice.MyApplication
import com.blurr.voice.ui.theme.JarvisAmber
import com.blurr.voice.ui.theme.JarvisCrimson
import com.blurr.voice.ui.theme.JarvisCyan
import com.blurr.voice.ui.theme.JarvisElectricBlue
import com.blurr.voice.ui.theme.JarvisEmerald
import com.blurr.voice.ui.theme.JarvisGlassElevated
import com.blurr.voice.ui.theme.JarvisOutline
import com.blurr.voice.ui.theme.JarvisTextPrimary
import com.blurr.voice.ui.theme.JarvisTextSecondary
import com.blurr.voice.utilities.PandaState
import com.blurr.voice.utilities.VoiceLanguage

@Composable
fun HomeDashboardScreen(
    viewModel: JarvisViewModel,
    onTapToSpeak: () -> Unit
) {
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val billingStatus by viewModel.billingStatus.collectAsStateWithLifecycle()
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val serviceReply by viewModel.serviceReply.collectAsStateWithLifecycle()
    val serviceTranscript by viewModel.serviceTranscript.collectAsStateWithLifecycle()

    val latestPreview = serviceReply.ifBlank {
        messages.lastOrNull()?.content ?: "Systems nominal. Ready for English or Hindi voice commands and device automation."
    }
    val latestPrompt = serviceTranscript.ifBlank {
        messages.lastOrNull { it.role == "user" }?.content
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Brand & Online Status Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            JarvisStatusPill(
                text = jarvisStateLabel(voiceState),
                color = jarvisStateColor(voiceState)
            )
            JarvisStatusPill(
                text = if (billingStatus == MyApplication.BillingStatus.AVAILABLE) "CORE PRO" else "LOCAL UNLIMITED",
                color = JarvisElectricBlue,
                pulsing = false
            )
        }

        // Arc Reactor Centerpiece
        JarvisGlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = jarvisStateColor(voiceState).copy(alpha = 0.5f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "JARVIS AI",
                    style = MaterialTheme.typography.displaySmall,
                    color = JarvisCyan
                )
                Text(
                    text = "BILINGUAL NEURAL INTERFACE",
                    style = MaterialTheme.typography.labelMedium,
                    color = JarvisTextSecondary
                )

                JarvisArcReactor(
                    state = voiceState,
                    rmsLevel = rmsLevel,
                    isSpeaking = isSpeaking,
                    size = 210.dp,
                    onClick = onTapToSpeak,
                    modifier = Modifier.testTag("home_arc_reactor")
                )

                JarvisWaveform(
                    isActive = isListening || voiceState == PandaState.LISTENING,
                    isSpeaking = isSpeaking || voiceState == PandaState.SPEAKING,
                    rmsLevel = rmsLevel
                )

                JarvisGlowButton(
                    text = if (isListening) "LISTENING..." else "TAP TO SPEAK",
                    icon = Icons.Filled.Mic,
                    onClick = onTapToSpeak,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("tap_to_speak_button")
                )
            }
        }

        // AI Greeting & Conversation Preview Card
        JarvisGlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_preview_card"),
            onClick = { viewModel.navigateTo(JarvisScreen.CHAT) }
        ) {
            SectionHeader(
                title = "Latest Neural Feed",
                subtitle = "Tap to continue in AI Chat"
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (!latestPrompt.isNullOrBlank()) {
                Text(
                    text = "YOU: $latestPrompt",
                    style = MaterialTheme.typography.labelMedium,
                    color = JarvisElectricBlue,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = latestPreview,
                style = MaterialTheme.typography.bodyMedium,
                color = JarvisTextPrimary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }

        // Quick Action Grid
        SectionHeader(
            title = "Quick Modules",
            subtitle = "Direct neural & device subroutines"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "AI Chat",
                subtitle = "Gemini Stream",
                icon = Icons.AutoMirrored.Filled.Chat,
                onClick = { viewModel.navigateTo(JarvisScreen.CHAT) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_chat_card")
            )
            QuickActionCard(
                title = "Screen HUD",
                subtitle = "Live Analysis",
                icon = Icons.Filled.ScreenshotMonitor,
                onClick = { viewModel.navigateTo(JarvisScreen.SCREEN_ASSISTANT) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_screen_card")
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionCard(
                title = "Apps",
                subtitle = "Quick Launch",
                icon = Icons.Filled.Apps,
                onClick = { viewModel.navigateTo(JarvisScreen.APP_LAUNCHER) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_apps_card")
            )
            QuickActionCard(
                title = "Device",
                subtitle = "Controls & Panels",
                icon = Icons.Filled.Tune,
                onClick = { viewModel.navigateTo(JarvisScreen.DEVICE_CONTROL) },
                modifier = Modifier
                    .weight(1f)
                    .testTag("quick_device_card")
            )
        }

        JarvisGlowButton(
            text = "Open Automation Center",
            icon = Icons.Filled.Bolt,
            outlined = true,
            onClick = { viewModel.navigateTo(JarvisScreen.AUTOMATION) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("quick_automation_button")
        )
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    JarvisGlassCard(
        modifier = modifier,
        onClick = onClick,
        contentPadding = PaddingValues(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = JarvisCyan,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = JarvisTextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = JarvisTextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun VoiceAssistantScreen(
    viewModel: JarvisViewModel,
    onRequestMicPermission: () -> Unit
) {
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val rmsLevel by viewModel.rmsLevel.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val voiceLanguage by viewModel.voiceLanguage.collectAsStateWithLifecycle()
    val partialText by viewModel.voicePartialText.collectAsStateWithLifecycle()
    val replyText by viewModel.voiceReplyText.collectAsStateWithLifecycle()
    val errorText by viewModel.voiceErrorText.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Microphone status & permission card
        JarvisGlassCard(
            borderColor = if (telemetry.hasMicPermission) JarvisEmerald else JarvisCrimson,
            contentPadding = PaddingValues(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (telemetry.hasMicPermission) Icons.Filled.Mic else Icons.Filled.MicOff,
                        contentDescription = "Microphone status",
                        tint = if (telemetry.hasMicPermission) JarvisEmerald else JarvisCrimson
                    )
                    Column {
                        Text(
                            text = if (telemetry.hasMicPermission) "Microphone Armed" else "Microphone Permission Needed",
                            style = MaterialTheme.typography.titleMedium,
                            color = JarvisTextPrimary
                        )
                        Text(
                            text = "Language: ${voiceLanguage.displayLabel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisTextSecondary
                        )
                    }
                }
                if (!telemetry.hasMicPermission) {
                    JarvisGlowButton(
                        text = "Grant",
                        onClick = onRequestMicPermission,
                        modifier = Modifier.testTag("grant_mic_button")
                    )
                }
            }
        }

        // Bilingual Hindi & English Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VoiceLanguage.entries.forEach { lang ->
                val selected = voiceLanguage == lang
                FilterChip(
                    selected = selected,
                    onClick = { viewModel.setVoiceLanguage(lang) },
                    label = {
                        Text(
                            text = when (lang) {
                                VoiceLanguage.AUTO -> "Auto (EN/HI)"
                                VoiceLanguage.ENGLISH -> "English"
                                VoiceLanguage.HINDI -> "हिन्दी (Hindi)"
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = JarvisElectricBlue.copy(alpha = 0.25f),
                        selectedLabelColor = JarvisCyan,
                        containerColor = JarvisGlassElevated,
                        labelColor = JarvisTextSecondary
                    ),
                    border = BorderStroke(1.dp, if (selected) JarvisCyan else JarvisOutline),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("lang_chip_${lang.name.lowercase()}")
                )
            }
        }

        // Orbital Reactor + State Telemetry
        JarvisStatusPill(
            text = when {
                isListening -> "LISTENING • SPEAK NOW"
                isSpeaking -> "SPEAKING RESPONSE"
                voiceState == PandaState.PROCESSING -> "THINKING • GEMINI CORE"
                else -> "STANDBY • TAP CORE TO SPEAK"
            },
            color = when {
                isListening -> JarvisAmber
                isSpeaking -> JarvisEmerald
                else -> JarvisCyan
            }
        )

        JarvisArcReactor(
            state = voiceState,
            rmsLevel = rmsLevel,
            isSpeaking = isSpeaking,
            size = 220.dp,
            onClick = {
                if (isListening || isSpeaking) viewModel.stopInAppVoice()
                else if (telemetry.hasMicPermission) viewModel.startInAppVoiceTurn()
                else onRequestMicPermission()
            },
            modifier = Modifier.testTag("voice_arc_reactor")
        )

        JarvisWaveform(
            isActive = isListening,
            isSpeaking = isSpeaking,
            rmsLevel = rmsLevel
        )

        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText!!,
                style = MaterialTheme.typography.bodySmall,
                color = JarvisCrimson,
                textAlign = TextAlign.Center
            )
        }

        // Live Transcript & Reply Cards
        JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(title = "Live Transcript")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = partialText.ifBlank { "Say anything in English or Hindi (e.g. \"Aaj ka mausam kaisa hai?\" or \"Explain black holes\")." },
                style = MaterialTheme.typography.bodyMedium,
                color = if (partialText.isBlank()) JarvisTextSecondary else JarvisTextPrimary
            )
        }

        JarvisGlassCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = JarvisElectricBlue.copy(alpha = 0.7f)
        ) {
            SectionHeader(title = "JARVIS Response")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = replyText,
                style = MaterialTheme.typography.bodyLarge,
                color = JarvisTextPrimary
            )
        }

        // Action Controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            JarvisGlowButton(
                text = if (isListening || isSpeaking) "Stop" else "Tap to Speak",
                icon = if (isListening || isSpeaking) Icons.Filled.Stop else Icons.Filled.Mic,
                onClick = {
                    if (isListening || isSpeaking) viewModel.stopInAppVoice()
                    else if (telemetry.hasMicPermission) viewModel.startInAppVoiceTurn()
                    else onRequestMicPermission()
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("voice_primary_action")
            )
            JarvisGlowButton(
                text = "Overlay Core",
                icon = Icons.Filled.GraphicEq,
                outlined = true,
                onClick = { viewModel.toggleSystemVoiceOverlay() },
                modifier = Modifier
                    .weight(1f)
                    .testTag("voice_overlay_button")
            )
        }
    }
}
