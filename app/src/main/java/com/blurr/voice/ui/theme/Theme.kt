package com.blurr.voice.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val JarvisDarkColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = JarvisBlack,
    primaryContainer = JarvisDeepBlue,
    onPrimaryContainer = JarvisCyanGlow,
    secondary = JarvisElectricBlue,
    onSecondary = JarvisBlack,
    secondaryContainer = JarvisGlassElevated,
    onSecondaryContainer = JarvisTextPrimary,
    tertiary = JarvisEmerald,
    onTertiary = JarvisBlack,
    background = JarvisBlack,
    onBackground = JarvisTextPrimary,
    surface = JarvisObsidian,
    onSurface = JarvisTextPrimary,
    surfaceVariant = JarvisGlassSurface,
    onSurfaceVariant = JarvisTextSecondary,
    surfaceContainerLowest = JarvisBlack,
    surfaceContainerLow = JarvisObsidian,
    surfaceContainer = JarvisGlassSurface,
    surfaceContainerHigh = JarvisGlassElevated,
    surfaceContainerHighest = JarvisGlassHighlight,
    outline = JarvisOutline,
    outlineVariant = JarvisOutline.copy(alpha = 0.6f),
    error = JarvisCrimson,
    onError = JarvisTextPrimary
)

val JarvisShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun JarvisTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisDarkColorScheme,
        typography = JarvisTypography,
        shapes = JarvisShapes,
        content = content
    )
}

@Composable
fun BlurrTheme(
    content: @Composable () -> Unit
) {
    JarvisTheme(content = content)
}