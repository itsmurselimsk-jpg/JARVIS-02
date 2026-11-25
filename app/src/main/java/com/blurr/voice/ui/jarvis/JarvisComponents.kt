package com.blurr.voice.ui.jarvis

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.blurr.voice.ui.theme.JarvisAmber
import com.blurr.voice.ui.theme.JarvisBlack
import com.blurr.voice.ui.theme.JarvisCrimson
import com.blurr.voice.ui.theme.JarvisCyan
import com.blurr.voice.ui.theme.JarvisCyanGlow
import com.blurr.voice.ui.theme.JarvisElectricBlue
import com.blurr.voice.ui.theme.JarvisEmerald
import com.blurr.voice.ui.theme.JarvisGlassElevated
import com.blurr.voice.ui.theme.JarvisGlassSurface
import com.blurr.voice.ui.theme.JarvisObsidian
import com.blurr.voice.ui.theme.JarvisOutline
import com.blurr.voice.ui.theme.JarvisTextPrimary
import com.blurr.voice.ui.theme.JarvisTextSecondary
import com.blurr.voice.utilities.PandaState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun jarvisStateColor(state: PandaState): Color {
    return when (state) {
        PandaState.IDLE -> JarvisCyan
        PandaState.LISTENING -> JarvisAmber
        PandaState.PROCESSING -> JarvisElectricBlue
        PandaState.SPEAKING -> JarvisEmerald
        PandaState.ERROR -> JarvisCrimson
    }
}

fun jarvisStateLabel(state: PandaState): String {
    return when (state) {
        PandaState.IDLE -> "JARVIS ONLINE"
        PandaState.LISTENING -> "LISTENING"
        PandaState.PROCESSING -> "THINKING"
        PandaState.SPEAKING -> "SPEAKING"
        PandaState.ERROR -> "ATTENTION"
    }
}

@Composable
fun JarvisArcReactor(
    state: PandaState,
    rmsLevel: Float = 0f,
    isSpeaking: Boolean = false,
    size: Dp = 220.dp,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val targetColor = jarvisStateColor(state)
    val accentColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(450),
        label = "reactorColor"
    )
    val transition = rememberInfiniteTransition(label = "reactorTransition")

    val outerRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    PandaState.PROCESSING -> 3600
                    PandaState.LISTENING, PandaState.SPEAKING -> 6000
                    else -> 14000
                },
                easing = LinearEasing
            )
        ),
        label = "outerRotation"
    )

    val innerRotation by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    PandaState.PROCESSING -> 2400
                    PandaState.LISTENING, PandaState.SPEAKING -> 4500
                    else -> 10000
                },
                easing = LinearEasing
            )
        ),
        label = "innerRotation"
    )

    val breathe by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val reactiveBoost by animateFloatAsState(
        targetValue = when {
            state == PandaState.LISTENING -> (rmsLevel * 0.22f).coerceIn(0f, 0.24f)
            isSpeaking || state == PandaState.SPEAKING -> 0.14f
            else -> 0f
        },
        animationSpec = tween(140),
        label = "reactiveBoost"
    )

    val boxModifier = modifier
        .size(size)
        .clip(CircleShape)
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    role = Role.Button,
                    onClickLabel = "Activate JARVIS voice",
                    onClick = onClick
                )
            } else Modifier
        )
        .semantics {
            contentDescription = "JARVIS AI Core, status ${jarvisStateLabel(state)}"
        }

    Box(modifier = boxModifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val maxR = this.size.minDimension / 2f

            // Ambient radial holographic glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accentColor.copy(alpha = (0.34f + reactiveBoost).coerceIn(0f, 0.7f)),
                        JarvisElectricBlue.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxR * (0.98f + reactiveBoost * 0.2f)
                ),
                radius = maxR,
                center = center
            )

            // Outer telemetry tick ring
            val tickRadius = maxR * 0.88f
            val tickCount = 36
            for (i in 0 until tickCount) {
                val angleRad = (i * (360f / tickCount) + outerRotation * 0.35f) * (PI / 180f)
                val isMajor = i % 3 == 0
                val inner = tickRadius - if (isMajor) maxR * 0.065f else maxR * 0.032f
                val start = Offset(
                    center.x + (inner * cos(angleRad)).toFloat(),
                    center.y + (inner * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    center.x + (tickRadius * cos(angleRad)).toFloat(),
                    center.y + (tickRadius * sin(angleRad)).toFloat()
                )
                drawLine(
                    color = if (isMajor) accentColor.copy(alpha = 0.85f) else JarvisElectricBlue.copy(alpha = 0.45f),
                    start = start,
                    end = end,
                    strokeWidth = if (isMajor) 3f else 1.5f,
                    cap = StrokeCap.Round
                )
            }

            // Segmented outer rotating arcs
            rotate(degrees = outerRotation, pivot = center) {
                val outerArcRadius = maxR * 0.77f
                val topLeft = Offset(center.x - outerArcRadius, center.y - outerArcRadius)
                val arcSize = Size(outerArcRadius * 2f, outerArcRadius * 2f)
                for (seg in 0 until 4) {
                    drawArc(
                        color = accentColor.copy(alpha = 0.9f),
                        startAngle = seg * 90f + 10f,
                        sweepAngle = 56f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = maxR * 0.035f, cap = StrokeCap.Round)
                    )
                }
            }

            // Counter-rotating mid electric blue ring
            rotate(degrees = innerRotation, pivot = center) {
                val midRadius = maxR * 0.61f
                val topLeft = Offset(center.x - midRadius, center.y - midRadius)
                val arcSize = Size(midRadius * 2f, midRadius * 2f)
                for (seg in 0 until 3) {
                    drawArc(
                        color = JarvisElectricBlue.copy(alpha = 0.88f),
                        startAngle = seg * 120f + 16f,
                        sweepAngle = 82f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = maxR * 0.048f, cap = StrokeCap.Round)
                    )
                }
            }

            // Inner reactor ring
            drawCircle(
                color = accentColor.copy(alpha = 0.55f),
                radius = maxR * 0.46f,
                center = center,
                style = Stroke(width = 2.5f)
            )

            // Pulsing central core
            val coreRadius = maxR * 0.34f * (breathe * 0.95f + reactiveBoost)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        JarvisCyanGlow,
                        accentColor,
                        JarvisElectricBlue.copy(alpha = 0.25f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = coreRadius * 1.45f
                ),
                radius = coreRadius * 1.45f,
                center = center
            )

            // Crisp dark ring & geometric inner core
            drawCircle(
                color = JarvisObsidian.copy(alpha = 0.88f),
                radius = maxR * 0.21f,
                center = center
            )
            drawCircle(
                color = accentColor,
                radius = maxR * 0.21f,
                center = center,
                style = Stroke(width = 3f)
            )
            drawCircle(
                color = accentColor.copy(alpha = 0.95f),
                radius = maxR * (0.085f + reactiveBoost * 0.14f),
                center = center
            )
        }
    }
}

@Composable
fun JarvisWaveform(
    isActive: Boolean,
    isSpeaking: Boolean = false,
    rmsLevel: Float = 0f,
    barCount: Int = 28,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing)
        ),
        label = "wavePhase"
    )
    val activityScale by animateFloatAsState(
        targetValue = when {
            isSpeaking -> 0.85f
            isActive -> (0.35f + rmsLevel * 0.85f).coerceIn(0.28f, 1f)
            else -> 0.16f
        },
        animationSpec = tween(120),
        label = "activityScale"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .semantics {
                contentDescription = if (isActive || isSpeaking) "Live audio waveform active" else "Audio waveform standby"
            }
    ) {
        val barSpacing = size.width / barCount
        val barWidth = (barSpacing * 0.52f).coerceAtLeast(3f)
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val normPos = (i.toFloat() / (barCount - 1)) * 2f - 1f
            val envelope = 1f - (normPos * normPos * 0.72f)
            val wave = ((sin(phase + i * 0.45f) + 1f) * 0.5f)
            val barH = (size.height * (0.14f + 0.78f * wave * activityScale) * envelope)
                .coerceIn(6f, size.height * 0.92f)

            val x = i * barSpacing + (barSpacing - barWidth) / 2f
            val color = when {
                isSpeaking -> JarvisEmerald
                isActive -> if (i % 2 == 0) JarvisCyan else JarvisElectricBlue
                else -> JarvisElectricBlue.copy(alpha = 0.48f)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, centerY - barH / 2f),
                size = Size(barWidth, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}

@Composable
fun JarvisGlassCard(
    modifier: Modifier = Modifier,
    borderColor: Color = JarvisOutline,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val baseModifier = modifier
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    JarvisGlassElevated.copy(alpha = 0.88f),
                    JarvisGlassSurface.copy(alpha = 0.92f)
                )
            )
        )
        .border(width = 1.dp, color = borderColor.copy(alpha = 0.75f), shape = shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Box(modifier = baseModifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            content = content
        )
    }
}

@Composable
fun JarvisStatusPill(
    text: String,
    color: Color = JarvisCyan,
    pulsing: Boolean = true,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "pillPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dotAlpha"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = if (pulsing) alpha else 1f))
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun JarvisGlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    primaryColor: Color = JarvisElectricBlue,
    secondaryColor: Color = JarvisCyan,
    outlined: Boolean = false
) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.2.dp, secondaryColor.copy(alpha = 0.7f)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = JarvisGlassSurface.copy(alpha = 0.75f),
                contentColor = JarvisTextPrimary
            ),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = secondaryColor)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(text = text, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier
                .heightIn(min = 52.dp)
                .background(
                    brush = Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
                    shape = RoundedCornerShape(16.dp)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = JarvisBlack
            ),
            contentPadding = PaddingValues(horizontal = 22.dp, vertical = 14.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = JarvisBlack)
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = JarvisBlack
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = JarvisCyan
        )
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = JarvisTextSecondary
            )
        }
    }
}
