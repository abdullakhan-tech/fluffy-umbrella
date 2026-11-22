package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.SpaceGroteskFontFamily
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

enum class OrbState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

private data class NeuralNode3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val phase: Float,
    val layer: Int // 0 = outer sensory shell, 1 = hidden synaptic layer
)

private data class ProjectedNode(
    val x: Float,
    val y: Float,
    val z: Float,
    val alpha: Float,
    val radius: Float,
    val layer: Int
)

private data class SynapticEdge(
    val fromIdx: Int,
    val toIdx: Int,
    val speed: Float,
    val offset: Float
)

private data class OrbVisualConfig(
    val primaryColor: Color,
    val secondaryColor: Color,
    val coreGlow: Color,
    val pulseDurationMs: Int,
    val spinMultiplier: Float
)

@Composable
fun NeuralCoreVisualizer(
    modifier: Modifier = Modifier,
    size: Dp = 250.dp,
    motionEnabled: Boolean = true,
    coreLabel: String = "Z",
    activityLevel: Float = 0.65f,
    orbState: OrbState = OrbState.IDLE,
    audioLevel: Float = 0f,
    customGlowColor: Color? = null
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current

    val orbConfig = remember(orbState, customGlowColor, cyber) {
        val baseConfig = when (orbState) {
            OrbState.IDLE -> OrbVisualConfig(
                primaryColor = cyber.accentSoft,
                secondaryColor = cyber.cyberGreen,
                coreGlow = Color(0xFF0D2538),
                pulseDurationMs = 2200,
                spinMultiplier = 1.0f
            )
            OrbState.LISTENING -> OrbVisualConfig(
                primaryColor = cyber.cyberGreen,
                secondaryColor = Color(0xFF00E5FF),
                coreGlow = Color(0xFF082C18),
                pulseDurationMs = 1200,
                spinMultiplier = 1.4f
            )
            OrbState.PROCESSING -> OrbVisualConfig(
                primaryColor = Color(0xFF9D00FF),
                secondaryColor = Color(0xFFFF007F),
                coreGlow = Color(0xFF2C0A42),
                pulseDurationMs = 750,
                spinMultiplier = 2.4f
            )
            OrbState.SPEAKING -> OrbVisualConfig(
                primaryColor = Color(0xFF00E5FF),
                secondaryColor = Color(0xFFFFB700),
                coreGlow = Color(0xFF332008),
                pulseDurationMs = 950,
                spinMultiplier = 1.6f
            )
            OrbState.ERROR -> OrbVisualConfig(
                primaryColor = Color(0xFFFF3366),
                secondaryColor = Color(0xFFFF7A90),
                coreGlow = Color(0xFF360812),
                pulseDurationMs = 500,
                spinMultiplier = 1.8f
            )
        }
        if (customGlowColor != null) {
            baseConfig.copy(primaryColor = customGlowColor)
        } else {
            baseConfig
        }
    }

    val primary = orbConfig.primaryColor
    val secondary = orbConfig.secondaryColor

    val systemReduceMotion = remember {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        scale == 0f
    }
    val animate = motionEnabled && !systemReduceMotion

    val nodes = remember {
        val outer = List(64) { i ->
            val y = 1f - (i / 63f) * 2f
            val radius = sqrt(max(0f, 1f - y * y))
            val angle = i * 2.3999632f
            NeuralNode3D(
                x = cos(angle) * radius,
                y = y,
                z = sin(angle) * radius,
                phase = i * 0.61f,
                layer = 0
            )
        }
        val hidden = List(28) { i ->
            val y = 1f - (i / 27f) * 2f
            val rShell = 0.66f * sqrt(max(0f, 1f - y * y))
            val angle = i * 2.3999632f + 1.15f
            NeuralNode3D(
                x = cos(angle) * rShell,
                y = y * 0.66f,
                z = sin(angle) * rShell,
                phase = i * 0.91f,
                layer = 1
            )
        }
        outer + hidden
    }

    val activeEdges = remember(nodes) {
        buildList {
            for (h in 64 until nodes.size) {
                val hn = nodes[h]
                val nearestOuter = (0 until 64).sortedBy { o ->
                    val on = nodes[o]
                    val dx = hn.x - on.x
                    val dy = hn.y - on.y
                    val dz = hn.z - on.z
                    dx * dx + dy * dy + dz * dz
                }.take(2)
                nearestOuter.forEachIndexed { idx, oIdx ->
                    add(
                        SynapticEdge(
                            fromIdx = oIdx,
                            toIdx = h,
                            speed = 0.45f + ((h + idx) % 5) * 0.14f,
                            offset = ((h * 0.37f + idx * 0.51f) % 1f)
                        )
                    )
                }
            }
        }
    }

    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var pulseBoost by remember { mutableFloatStateOf(0f) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val startNanos = System.nanoTime()
        while (true) {
            withFrameNanos { frameNanos ->
                elapsedMillis = (frameNanos - startNanos) / 1_000_000L
                if (pulseBoost > 0.01f) {
                    pulseBoost *= 0.95f
                } else {
                    pulseBoost = 0f
                }
            }
        }
    }

    val pulseScale = if (animate) {
        val pulseTransition = rememberInfiniteTransition(label = "corePulse")
        val anim by pulseTransition.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.06f,
            animationSpec = infiniteRepeatable(
                animation = tween(orbConfig.pulseDurationMs, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseScale"
        )
        anim
    } else {
        1.0f
    }

    val clampedActivity = (activityLevel + pulseBoost + audioLevel * 0.5f).coerceIn(0.2f, 1.8f)
    val sphereScale = if (animate) pulseScale + pulseBoost * 0.08f + audioLevel * 0.12f else 1f
    val coreSphereSize = size * 0.40f

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.30f * cyber.glowStrength),
                        cyber.panelGlass.copy(alpha = 0.94f),
                        Color.Transparent
                    )
                )
            )
            .border(1.2.dp, primary.copy(alpha = 0.55f), CircleShape)
            .pointerInput(Unit) {
                detectTapGestures {
                    pulseBoost = 0.75f
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        tiltX = 0f
                        tiltY = 0f
                    },
                    onDragCancel = {
                        tiltX = 0f
                        tiltY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    tiltY = (tiltY + dragAmount.x * 0.0045f).coerceIn(-0.85f, 0.85f)
                    tiltX = (tiltX + dragAmount.y * 0.0045f).coerceIn(-0.85f, 0.85f)
                }
            }
            .testTag("neural_core_visualizer")
            .semantics {
                contentDescription = "Interactive 3D Neural AI Orb: ${orbState.name.lowercase(Locale.ROOT)}"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            if (w <= 0f || h <= 0f) return@Canvas
            val cx = w / 2f
            val cy = h / 2f
            val radiusScale = (minOf(w, h) * 0.41f).coerceAtLeast(1f)
            val nowSec = elapsedMillis * 0.001f
            val spin = elapsedMillis * (0.00018f * clampedActivity * orbConfig.spinMultiplier) + tiltY
            val cosSpin = cos(spin)
            val sinSpin = sin(spin)
            val cosTilt = cos(tiltX)
            val sinTilt = sin(tiltX)

            // Outward acoustic wave ripples during speech, listening, or audio activity
            val effectiveAudio = (audioLevel + if (orbState == OrbState.LISTENING || orbState == OrbState.SPEAKING) 0.35f else 0f).coerceIn(0f, 1f)
            if (effectiveAudio > 0.05f && animate) {
                for (wave in 0 until 3) {
                    val wavePhase = (nowSec * 1.8f + wave * 0.33f) % 1f
                    val waveRadius = minOf(w, h) * (0.28f + wavePhase * 0.22f * (1f + effectiveAudio * 0.35f))
                    val waveAlpha = ((1f - wavePhase) * 0.42f * effectiveAudio).coerceIn(0f, 1f)
                    drawCircle(
                        color = secondary.copy(alpha = waveAlpha),
                        radius = waveRadius,
                        style = Stroke(
                            width = 1.5f + effectiveAudio * 1.2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), wavePhase * 16f)
                        )
                    )
                }
            }

            val projected = nodes.map { n ->
                val layerSpin = if (n.layer == 1) -0.55f else 1f
                val rx = n.x * cosSpin - n.z * sinSpin * layerSpin
                val rz = n.x * sinSpin * layerSpin + n.z * cosSpin
                val ry = n.y
                val yy = ry * cosTilt - rz * sinTilt
                val zz = ry * sinTilt + rz * cosTilt
                val depth = 2.65f + zz * 0.55f
                val perspective = 2.65f / depth
                ProjectedNode(
                    x = cx + rx * radiusScale * perspective,
                    y = cy + yy * radiusScale * perspective,
                    z = zz,
                    alpha = (0.25f + 0.75f * (zz + 1f) / 2f).coerceIn(0.15f, 1f),
                    radius = (if (n.layer == 1) 2.4f else 1.8f) + ((zz + 1f) / 2f) * 2.4f,
                    layer = n.layer
                )
            }

            // Outer lattice mesh connections
            val linkDist = w * 0.145f
            for (i in 0 until 64) {
                val a = projected[i]
                for (j in i + 1 until 64) {
                    val b = projected[j]
                    val d = hypot(a.x - b.x, a.y - b.y)
                    if (d < linkDist) {
                        val lineAlpha = (1f - d / linkDist) * 0.32f * minOf(a.alpha, b.alpha)
                        drawLine(
                            color = primary.copy(alpha = lineAlpha.coerceIn(0f, 1f)),
                            start = Offset(a.x, a.y),
                            end = Offset(b.x, b.y),
                            strokeWidth = 1.15f
                        )
                    }
                }
            }

            // Cross-layer neural synapses & forward-pass firing pulses
            activeEdges.forEach { edge ->
                val a = projected[edge.fromIdx]
                val b = projected[edge.toIdx]
                val baseAlpha = 0.25f * minOf(a.alpha, b.alpha)
                drawLine(
                    color = secondary.copy(alpha = baseAlpha.coerceIn(0f, 1f)),
                    start = Offset(a.x, a.y),
                    end = Offset(b.x, b.y),
                    strokeWidth = 1.25f
                )
                if (animate) {
                    val t = (nowSec * edge.speed * clampedActivity * orbConfig.spinMultiplier + edge.offset) % 1f
                    val px = a.x + (b.x - a.x) * t
                    val py = a.y + (b.y - a.y) * t
                    val packetAlpha = (sin(t * PI.toFloat()) * minOf(a.alpha, b.alpha)).coerceIn(0f, 1f)
                    drawCircle(
                        color = secondary.copy(alpha = packetAlpha),
                        radius = 2.8f + audioLevel * 1.5f,
                        center = Offset(px, py)
                    )
                }
            }

            // Draw neural nodes sorted by layer/depth glow
            projected.forEachIndexed { idx, p ->
                val pulse = if (animate) 0.65f + 0.35f * sin(nowSec * 2.8f + nodes[idx].phase) else 0.85f
                val nodeColor = if (p.layer == 1) secondary else primary
                drawCircle(
                    color = nodeColor.copy(alpha = (p.alpha * pulse).coerceIn(0f, 1f)),
                    radius = p.radius * pulse * (1.35f + audioLevel * 0.3f),
                    center = Offset(p.x, p.y)
                )
            }

            // Outer calibrated telemetry ring
            rotate(degrees = if (animate) (elapsedMillis * 0.015f * orbConfig.spinMultiplier) % 360f else 0f) {
                drawCircle(
                    color = primary.copy(alpha = 0.48f),
                    radius = minOf(w, h) * 0.465f,
                    style = Stroke(
                        width = 1.6f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    )
                )
            }

            // Counter-rotating synaptic attention ring
            rotate(degrees = if (animate) -(elapsedMillis * 0.022f * orbConfig.spinMultiplier) % 360f else 0f) {
                drawCircle(
                    color = secondary.copy(alpha = 0.40f),
                    radius = minOf(w, h) * 0.33f,
                    style = Stroke(
                        width = 1.4f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 12f), 0f)
                    )
                )
            }

            // Tilted orbital ellipses
            val orbitRatios = listOf(0.84f, 1.03f, 1.19f)
            orbitRatios.forEachIndexed { k, r ->
                val deg = ((spin * 180f / PI.toFloat()) * 0.35f) + k * 52f - 28f
                rotate(degrees = deg) {
                    val ow = radiusScale * r * 2f
                    val oh = radiusScale * (0.24f + k * 0.03f) * 2f
                    drawOval(
                        color = (if (k == 1) secondary else primary).copy(alpha = 0.35f),
                        topLeft = Offset(cx - ow / 2f, cy - oh / 2f),
                        size = Size(ow, oh),
                        style = Stroke(width = 1.8f)
                    )
                }
            }
        }

        // Central glowing holographic core nucleus
        val nucleusGradient = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f),
                primary,
                orbConfig.coreGlow,
                Color(0xFF07090E)
            ),
            center = Offset(0.35f, 0.25f)
        )

        Box(
            modifier = Modifier
                .size(coreSphereSize * sphereScale)
                .clip(CircleShape)
                .background(nucleusGradient)
                .border(1.5.dp, primary.copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = coreLabel,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.20f).sp,
                color = Color.White
            )
        }
    }
}
