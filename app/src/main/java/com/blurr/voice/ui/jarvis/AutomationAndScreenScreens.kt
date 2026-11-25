package com.blurr.voice.ui.jarvis

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AddAlarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blurr.voice.triggers.ui.CreateTriggerActivity
import com.blurr.voice.triggers.ui.TriggersActivity
import com.blurr.voice.ui.theme.JarvisAmber
import com.blurr.voice.ui.theme.JarvisBlack
import com.blurr.voice.ui.theme.JarvisCrimson
import com.blurr.voice.ui.theme.JarvisCyan
import com.blurr.voice.ui.theme.JarvisElectricBlue
import com.blurr.voice.ui.theme.JarvisEmerald
import com.blurr.voice.ui.theme.JarvisGlassElevated
import com.blurr.voice.ui.theme.JarvisGlassSurface
import com.blurr.voice.ui.theme.JarvisOutline
import com.blurr.voice.ui.theme.JarvisTextMuted
import com.blurr.voice.ui.theme.JarvisTextPrimary
import com.blurr.voice.ui.theme.JarvisTextSecondary
import com.blurr.voice.utilities.VisionMode

@Composable
fun AutomationCenterScreen(
    viewModel: JarvisViewModel
) {
    val context = LocalContext.current
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val taskLogs by viewModel.taskLogs.collectAsStateWithLifecycle()
    var taskCommand by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Accessibility Guard Banner
        JarvisGlassCard(
            borderColor = if (telemetry.hasAccessibility) JarvisEmerald else JarvisAmber
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (telemetry.hasAccessibility) "Automation Core Ready" else "Accessibility Required",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (telemetry.hasAccessibility) JarvisEmerald else JarvisAmber
                    )
                    Text(
                        text = if (telemetry.hasAccessibility) {
                            "Screen reading, taps, swipes, and text entry are active."
                        } else {
                            "Enable JARVIS AI Automation Service to control apps hands-free."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )
                }
                if (!telemetry.hasAccessibility) {
                    JarvisGlowButton(
                        text = "Enable",
                        icon = Icons.Filled.AccessibilityNew,
                        onClick = { viewModel.openAccessibilitySettings() },
                        modifier = Modifier.testTag("automation_enable_a11y")
                    )
                }
            }
        }

        // Running Agent Indicator
        if (telemetry.isAgentRunning) {
            JarvisGlassCard(borderColor = JarvisElectricBlue) {
                SectionHeader(title = "Active Agent Task")
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = telemetry.activeAgentTask ?: "Executing device workflow...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisTextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                JarvisGlowButton(
                    text = "Stop Active Task",
                    icon = Icons.Filled.Stop,
                    primaryColor = JarvisCrimson,
                    secondaryColor = JarvisAmber,
                    onClick = { viewModel.stopAutomationTask() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("automation_stop_task")
                )
            }
        }

        // Command Dispatcher Card
        JarvisGlassCard {
            SectionHeader(
                title = "Dispatch Phone Task",
                subtitle = "Describe any multi-step task in English or Hindi"
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = taskCommand,
                onValueChange = { taskCommand = it },
                placeholder = {
                    Text("e.g., Open Settings and check Wi-Fi, or YouTube खोलो", color = JarvisTextMuted)
                },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = JarvisBlack,
                    unfocusedContainerColor = JarvisBlack,
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisOutline,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("automation_command_input")
            )
            Spacer(modifier = Modifier.height(12.dp))
            JarvisGlowButton(
                text = "Execute Task",
                icon = Icons.Filled.PlayArrow,
                onClick = {
                    if (viewModel.runAutomationTask(taskCommand)) {
                        taskCommand = ""
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("automation_execute_button")
            )
        }

        // Quick Presets
        JarvisGlassCard {
            SectionHeader(
                title = "Quick Bilingual Presets",
                subtitle = "Tap to dispatch immediately"
            )
            Spacer(modifier = Modifier.height(8.dp))
            val presets = listOf(
                "Open Calculator app",
                "Open YouTube and search tech news",
                "Open Device Settings",
                "सेटिंग्स खोलो और बैटरी चेक करो"
            )
            presets.forEach { preset ->
                AssistChip(
                    onClick = { viewModel.runAutomationTask(preset) },
                    label = { Text(preset) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Bolt, contentDescription = null, tint = JarvisCyan)
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = JarvisGlassElevated,
                        labelColor = JarvisTextPrimary
                    ),
                    border = BorderStroke(1.dp, JarvisOutline)
                )
            }
        }

        // Triggers Hub
        JarvisGlassCard {
            SectionHeader(
                title = "Scheduled & Event Triggers",
                subtitle = "Automate JARVIS routines by time or notification"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                JarvisGlowButton(
                    text = "Manage Triggers",
                    outlined = true,
                    onClick = { context.startActivity(Intent(context, TriggersActivity::class.java)) },
                    modifier = Modifier.weight(1f)
                )
                JarvisGlowButton(
                    text = "New Trigger",
                    icon = Icons.Filled.AddAlarm,
                    onClick = { context.startActivity(Intent(context, CreateTriggerActivity::class.java)) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Recent Log Snapshot
        if (taskLogs.isNotEmpty()) {
            JarvisGlassCard(onClick = { viewModel.navigateTo(JarvisScreen.HISTORY) }) {
                SectionHeader(
                    title = "Last Execution",
                    subtitle = "Tap to view full logs"
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = taskLogs.first().input,
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisTextPrimary
                )
                Text(
                    text = taskLogs.first().output,
                    style = MaterialTheme.typography.bodySmall,
                    color = JarvisTextSecondary
                )
            }
        }
    }
}

@Composable
fun ScreenAssistantScreen(
    viewModel: JarvisViewModel
) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val visionMode by viewModel.visionMode.collectAsStateWithLifecycle()
    val screenSummary by viewModel.screenSummary.collectAsStateWithLifecycle()
    val screenActivity by viewModel.screenActivityName.collectAsStateWithLifecycle()
    val isAnalyzing by viewModel.isAnalyzingScreen.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Vision Permissions Status
        JarvisGlassCard {
            SectionHeader(
                title = "Screen Perception Health",
                subtitle = "Required for reading and acting on other apps"
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Accessibility Service", style = MaterialTheme.typography.titleMedium, color = JarvisTextPrimary)
                    Text(
                        text = if (telemetry.hasAccessibility) "Active • UI tree accessible" else "Disabled • Tap to enable",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (telemetry.hasAccessibility) JarvisEmerald else JarvisAmber
                    )
                }
                JarvisGlowButton(
                    text = if (telemetry.hasAccessibility) "Open" else "Grant",
                    outlined = telemetry.hasAccessibility,
                    onClick = { viewModel.openAccessibilitySettings() }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Display Over Other Apps", style = MaterialTheme.typography.titleMedium, color = JarvisTextPrimary)
                    Text(
                        text = if (telemetry.hasOverlay) "Granted • Floating HUD ready" else "Required for floating trigger",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (telemetry.hasOverlay) JarvisEmerald else JarvisAmber
                    )
                }
                JarvisGlowButton(
                    text = if (telemetry.hasOverlay) "Manage" else "Grant",
                    outlined = telemetry.hasOverlay,
                    onClick = { viewModel.openOverlaySettings() }
                )
            }
        }

        // Vision Mode Toggle (XML vs Screenshot)
        JarvisGlassCard {
            SectionHeader(
                title = "Vision Pipeline Mode",
                subtitle = visionMode.description
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                VisionMode.entries.forEach { mode ->
                    val selected = visionMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { viewModel.setVisionMode(mode) },
                        label = { Text(mode.label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = JarvisElectricBlue.copy(alpha = 0.25f),
                            selectedLabelColor = JarvisCyan,
                            containerColor = JarvisGlassElevated,
                            labelColor = JarvisTextSecondary
                        ),
                        border = BorderStroke(1.dp, if (selected) JarvisCyan else JarvisOutline),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("vision_mode_${mode.name.lowercase()}")
                    )
                }
            }
        }

        // Floating Overlay Button Toggle
        JarvisGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Floating JARVIS Trigger",
                        style = MaterialTheme.typography.titleMedium,
                        color = JarvisTextPrimary
                    )
                    Text(
                        text = "Show a compact floating JARVIS button over other apps",
                        style = MaterialTheme.typography.bodySmall,
                        color = JarvisTextSecondary
                    )
                }
                Switch(
                    checked = telemetry.isFloatingHudRunning,
                    onCheckedChange = { viewModel.toggleFloatingHud() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisBlack,
                        checkedTrackColor = JarvisCyan
                    ),
                    modifier = Modifier.testTag("floating_hud_switch")
                )
            }
        }

        // Live Screen Context Inspector
        JarvisGlassCard {
            SectionHeader(
                title = "Live Screen Context",
                subtitle = if (screenActivity.isNotBlank()) "Window: $screenActivity" else "Inspect active window with Gemini"
            )
            Spacer(modifier = Modifier.height(10.dp))
            JarvisGlowButton(
                text = if (isAnalyzing) "Scanning Screen..." else "Analyze Current Screen",
                icon = Icons.Filled.ScreenshotMonitor,
                onClick = { viewModel.inspectCurrentScreen() },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("analyze_screen_button")
            )
            if (isAnalyzing) {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator(color = JarvisCyan, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
            if (screenSummary.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = screenSummary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisTextPrimary
                )
            }
        }
    }
}
