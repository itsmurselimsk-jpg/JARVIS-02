package com.blurr.voice.ui.jarvis

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ScreenshotMonitor
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blurr.voice.ui.theme.JarvisBlack
import com.blurr.voice.ui.theme.JarvisCyan
import com.blurr.voice.ui.theme.JarvisElectricBlue
import com.blurr.voice.ui.theme.JarvisGlassElevated
import com.blurr.voice.ui.theme.JarvisGlassSurface
import com.blurr.voice.ui.theme.JarvisObsidian
import com.blurr.voice.ui.theme.JarvisOutline
import com.blurr.voice.ui.theme.JarvisTextMuted
import com.blurr.voice.ui.theme.JarvisTextPrimary
import com.blurr.voice.ui.theme.JarvisTextSecondary
import kotlinx.coroutines.launch

private data class DrawerEntry(
    val screen: JarvisScreen,
    val icon: ImageVector,
    val badge: String? = null
)

@Composable
fun JarvisHudShell(
    viewModel: JarvisViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshTelemetry()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.refreshTelemetry()
        if (granted) {
            viewModel.navigateTo(JarvisScreen.VOICE)
            viewModel.startInAppVoiceTurn()
        } else {
            Toast.makeText(context, "Microphone permission is required for voice commands.", Toast.LENGTH_SHORT).show()
        }
    }

    val requestVoiceAction: () -> Unit = {
        viewModel.refreshTelemetry()
        if (viewModel.telemetry.value.hasMicPermission) {
            if (currentScreen != JarvisScreen.VOICE) {
                viewModel.navigateTo(JarvisScreen.VOICE)
            }
            viewModel.startInAppVoiceTurn()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    BackHandler(enabled = drawerState.isOpen || currentScreen != JarvisScreen.HOME) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.navigateBack()
        }
    }

    val drawerItems = listOf(
        DrawerEntry(JarvisScreen.HOME, Icons.Filled.Home),
        DrawerEntry(JarvisScreen.CHAT, Icons.AutoMirrored.Filled.Chat),
        DrawerEntry(JarvisScreen.VOICE, Icons.Filled.Mic),
        DrawerEntry(JarvisScreen.AUTOMATION, Icons.Filled.Bolt),
        DrawerEntry(JarvisScreen.SCREEN_ASSISTANT, Icons.Filled.ScreenshotMonitor),
        DrawerEntry(JarvisScreen.APP_LAUNCHER, Icons.Filled.Apps),
        DrawerEntry(JarvisScreen.DEVICE_CONTROL, Icons.Filled.Tune),
        DrawerEntry(JarvisScreen.HISTORY, Icons.Filled.History),
        DrawerEntry(JarvisScreen.SETTINGS, Icons.Filled.Settings),
        DrawerEntry(JarvisScreen.ABOUT, Icons.Filled.Info)
    )

    val bottomItems = listOf(
        JarvisScreen.HOME to Icons.Filled.Home,
        JarvisScreen.CHAT to Icons.AutoMirrored.Filled.Chat,
        JarvisScreen.VOICE to Icons.Filled.GraphicEq,
        JarvisScreen.AUTOMATION to Icons.Filled.Bolt
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .fillMaxHeight(),
                drawerContainerColor = JarvisObsidian,
                drawerContentColor = JarvisTextPrimary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        JarvisArcReactor(
                            state = voiceState,
                            size = 46.dp
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "JARVIS AI",
                                style = MaterialTheme.typography.headlineSmall,
                                color = JarvisCyan
                            )
                            Text(
                                text = jarvisStateLabel(voiceState),
                                style = MaterialTheme.typography.labelSmall,
                                color = jarvisStateColor(voiceState)
                            )
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = JarvisOutline
                    )

                    drawerItems.forEach { item ->
                        val selected = currentScreen == item.screen
                        NavigationDrawerItem(
                            selected = selected,
                            onClick = {
                                viewModel.navigateTo(item.screen)
                                scope.launch { drawerState.close() }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.screen.title,
                                    tint = if (selected) JarvisCyan else JarvisTextSecondary
                                )
                            },
                            label = {
                                Text(
                                    text = item.screen.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = JarvisElectricBlue.copy(alpha = 0.22f),
                                selectedTextColor = JarvisTextPrimary,
                                unselectedContainerColor = Color.Transparent,
                                unselectedTextColor = JarvisTextSecondary
                            ),
                            modifier = Modifier
                                .padding(vertical = 2.dp)
                                .testTag("drawer_${item.screen.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(JarvisBlack),
            containerColor = JarvisBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                JarvisTopHudBar(
                    screen = currentScreen,
                    statusColor = jarvisStateColor(voiceState),
                    isAccessibilityReady = telemetry.hasAccessibility,
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onSettingsClick = { viewModel.navigateTo(JarvisScreen.SETTINGS) }
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier
                        .border(width = 0.5.dp, color = JarvisOutline.copy(alpha = 0.8f)),
                    containerColor = JarvisObsidian.copy(alpha = 0.96f),
                    contentColor = JarvisTextPrimary,
                    tonalElevation = 0.dp
                ) {
                    bottomItems.forEach { (screen, icon) ->
                        val selected = currentScreen == screen
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.navigateTo(screen) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = screen.title
                                )
                            },
                            label = {
                                Text(
                                    text = when (screen) {
                                        JarvisScreen.HOME -> "Home"
                                        JarvisScreen.CHAT -> "Chat"
                                        JarvisScreen.VOICE -> "Voice"
                                        JarvisScreen.AUTOMATION -> "Auto"
                                        else -> screen.title
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = JarvisCyan,
                                selectedTextColor = JarvisCyan,
                                indicatorColor = JarvisElectricBlue.copy(alpha = 0.22f),
                                unselectedIconColor = JarvisTextMuted,
                                unselectedTextColor = JarvisTextMuted
                            ),
                            modifier = Modifier.testTag("nav_${screen.name.lowercase()}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                JarvisBlack,
                                JarvisObsidian.copy(alpha = 0.55f),
                                JarvisBlack
                            )
                        )
                    )
                    .padding(innerPadding)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .align(Alignment.TopCenter)
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            (fadeIn() + scaleIn(initialScale = 0.98f)) togetherWith fadeOut()
                        },
                        label = "jarvisScreenTransition"
                    ) { screen ->
                        when (screen) {
                            JarvisScreen.HOME -> HomeDashboardScreen(
                                viewModel = viewModel,
                                onTapToSpeak = requestVoiceAction
                            )
                            JarvisScreen.CHAT -> ChatScreen(
                                viewModel = viewModel,
                                onRequestMicPermission = {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            )
                            JarvisScreen.VOICE -> VoiceAssistantScreen(
                                viewModel = viewModel,
                                onRequestMicPermission = {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            )
                            JarvisScreen.AUTOMATION -> AutomationCenterScreen(viewModel = viewModel)
                            JarvisScreen.SCREEN_ASSISTANT -> ScreenAssistantScreen(viewModel = viewModel)
                            JarvisScreen.APP_LAUNCHER -> AppLauncherScreen(viewModel = viewModel)
                            JarvisScreen.DEVICE_CONTROL -> DeviceControlScreen(viewModel = viewModel)
                            JarvisScreen.HISTORY -> ConversationHistoryScreen(viewModel = viewModel)
                            JarvisScreen.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            JarvisScreen.ABOUT -> AboutJarvisScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JarvisTopHudBar(
    screen: JarvisScreen,
    statusColor: Color,
    isAccessibilityReady: Boolean,
    onMenuClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(JarvisBlack.copy(alpha = 0.94f))
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onMenuClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(JarvisGlassSurface)
                .border(1.dp, JarvisOutline, CircleShape)
                .testTag("hud_menu_button")
        ) {
            Icon(
                imageVector = Icons.Filled.Menu,
                contentDescription = "Open HUD Drawer",
                tint = JarvisCyan
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = screen.title.uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    color = JarvisTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }
            Text(
                text = if (isAccessibilityReady) "${screen.subtitle} • A11Y READY" else screen.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = JarvisTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(JarvisGlassElevated.copy(alpha = 0.7f))
                .border(1.dp, JarvisOutline, CircleShape)
                .testTag("hud_settings_button")
        ) {
            Icon(
                imageVector = if (screen == JarvisScreen.SETTINGS) Icons.Filled.AutoAwesome else Icons.Filled.Settings,
                contentDescription = "Open Settings",
                tint = JarvisElectricBlue
            )
        }
    }
}
