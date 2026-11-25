package com.blurr.voice.ui.jarvis

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.blurr.voice.ui.theme.JarvisAmber
import com.blurr.voice.ui.theme.JarvisBlack
import com.blurr.voice.ui.theme.JarvisCyan
import com.blurr.voice.ui.theme.JarvisElectricBlue
import com.blurr.voice.ui.theme.JarvisGlassElevated
import com.blurr.voice.ui.theme.JarvisGlassSurface
import com.blurr.voice.ui.theme.JarvisOutline
import com.blurr.voice.ui.theme.JarvisTextMuted
import com.blurr.voice.ui.theme.JarvisTextPrimary
import com.blurr.voice.ui.theme.JarvisTextSecondary

private fun drawableToImageBitmap(drawable: Drawable?): ImageBitmap? {
    if (drawable == null) return null
    return runCatching {
        val w = drawable.intrinsicWidth.takeIf { it > 0 } ?: 96
        val h = drawable.intrinsicHeight.takeIf { it > 0 } ?: 96
        val bmp = Bitmap.createBitmap(w.coerceAtMost(144), h.coerceAtMost(144), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        bmp.asImageBitmap()
    }.getOrNull()
}

@Composable
fun AppLauncherScreen(
    viewModel: JarvisViewModel
) {
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    var search by rememberSaveable { mutableStateOf("") }

    val filtered = remember(apps, search) {
        if (search.isBlank()) apps
        else apps.filter {
            it.label.contains(search, ignoreCase = true) ||
                it.packageName.contains(search, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                placeholder = { Text("Search installed apps (${apps.size})...", color = JarvisTextMuted) },
                leadingIcon = {
                    Icon(imageVector = Icons.Filled.Search, contentDescription = "Search Apps", tint = JarvisCyan)
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = JarvisGlassSurface,
                    unfocusedContainerColor = JarvisGlassSurface,
                    focusedBorderColor = JarvisCyan,
                    unfocusedBorderColor = JarvisOutline,
                    focusedTextColor = JarvisTextPrimary,
                    unfocusedTextColor = JarvisTextPrimary
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("app_launcher_search")
            )

            IconButton(
                onClick = { viewModel.loadInstalledApps() },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(JarvisGlassElevated)
                    .border(1.dp, JarvisOutline, RoundedCornerShape(14.dp))
            ) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = "Refresh Apps", tint = JarvisCyan)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            JarvisGlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (apps.isEmpty()) "Scanning launchable applications..." else "No applications match \"$search\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JarvisTextSecondary
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 104.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    val bitmap = remember(app.packageName) { drawableToImageBitmap(app.icon) }
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(JarvisGlassSurface)
                            .border(1.dp, JarvisOutline, RoundedCornerShape(18.dp))
                            .clickable { viewModel.launchApp(app.packageName) }
                            .padding(horizontal = 8.dp, vertical = 14.dp)
                            .testTag("launch_app_${app.packageName}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = "${app.label} icon",
                                modifier = Modifier.size(44.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Apps,
                                contentDescription = "${app.label} icon",
                                tint = JarvisCyan,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceControlScreen(
    viewModel: JarvisViewModel
) {
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Audio Volume Controls
        JarvisGlassCard {
            SectionHeader(
                title = "Audio Telemetry",
                subtitle = "Live hardware volume controls"
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Filled.VolumeUp, contentDescription = null, tint = JarvisCyan)
                    Text("Media Volume", style = MaterialTheme.typography.bodyMedium, color = JarvisTextPrimary)
                }
                Text(
                    text = "${telemetry.mediaVolume} / ${telemetry.mediaMaxVolume}",
                    style = MaterialTheme.typography.labelMedium,
                    color = JarvisCyan
                )
            }
            Slider(
                value = telemetry.mediaVolume.toFloat(),
                onValueChange = { viewModel.setMediaVolume(it.toInt()) },
                valueRange = 0f..telemetry.mediaMaxVolume.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = JarvisCyan,
                    activeTrackColor = JarvisElectricBlue,
                    inactiveTrackColor = JarvisOutline
                ),
                modifier = Modifier.testTag("media_volume_slider")
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Ring Volume", style = MaterialTheme.typography.bodyMedium, color = JarvisTextPrimary)
                Text(
                    text = "${telemetry.ringVolume} / ${telemetry.ringMaxVolume}",
                    style = MaterialTheme.typography.labelMedium,
                    color = JarvisElectricBlue
                )
            }
            Slider(
                value = telemetry.ringVolume.toFloat(),
                onValueChange = { viewModel.setRingVolume(it.toInt()) },
                valueRange = 0f..telemetry.ringMaxVolume.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = JarvisElectricBlue,
                    activeTrackColor = JarvisCyan,
                    inactiveTrackColor = JarvisOutline
                ),
                modifier = Modifier.testTag("ring_volume_slider")
            )
        }

        // Flashlight Hardware Toggle
        JarvisGlassCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (telemetry.isTorchOn) Icons.Filled.FlashlightOn else Icons.Filled.FlashlightOff,
                        contentDescription = "Flashlight",
                        tint = if (telemetry.isTorchOn) JarvisAmber else JarvisCyan,
                        modifier = Modifier.size(28.dp)
                    )
                    Column {
                        Text("Flashlight Torch", style = MaterialTheme.typography.titleMedium, color = JarvisTextPrimary)
                        Text(
                            text = if (telemetry.hasFlashlight) {
                                if (telemetry.isTorchOn) "Active" else "Standby"
                            } else "Hardware flash unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = JarvisTextSecondary
                        )
                    }
                }
                Switch(
                    checked = telemetry.isTorchOn,
                    enabled = telemetry.hasFlashlight,
                    onCheckedChange = { viewModel.toggleFlashlight() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisBlack,
                        checkedTrackColor = JarvisAmber
                    ),
                    modifier = Modifier.testTag("flashlight_switch")
                )
            }
        }

        // Safe Android System Panels & Settings Shortcuts
        SectionHeader(
            title = "System Panels & Hardware Shortcuts",
            subtitle = "Opens native Android panels safely"
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DevicePanelTile(
                title = "Wi-Fi Panel",
                subtitle = "Network switch",
                icon = Icons.Filled.Wifi,
                onClick = { viewModel.openWifiPanel() },
                modifier = Modifier.weight(1f)
            )
            DevicePanelTile(
                title = "Bluetooth",
                subtitle = "Paired devices",
                icon = Icons.Filled.Bluetooth,
                onClick = { viewModel.openBluetoothSettings() },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DevicePanelTile(
                title = "Brightness",
                subtitle = "Display settings",
                icon = Icons.Filled.Brightness6,
                onClick = { viewModel.openDisplaySettings() },
                modifier = Modifier.weight(1f)
            )
            DevicePanelTile(
                title = "Battery",
                subtitle = "Power telemetry",
                icon = Icons.Filled.BatteryChargingFull,
                onClick = { viewModel.openBatterySettings() },
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            DevicePanelTile(
                title = "Alarms",
                subtitle = "Clock & timers",
                icon = Icons.Filled.AccessAlarm,
                onClick = { viewModel.openAlarmsApp() },
                modifier = Modifier.weight(1f)
            )
            DevicePanelTile(
                title = "Accessibility",
                subtitle = "JARVIS service",
                icon = Icons.Filled.AccessibilityNew,
                onClick = { viewModel.openAccessibilitySettings() },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun DevicePanelTile(
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
        Icon(imageVector = icon, contentDescription = title, tint = JarvisCyan, modifier = Modifier.size(26.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = JarvisTextPrimary)
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = JarvisTextSecondary)
    }
}
