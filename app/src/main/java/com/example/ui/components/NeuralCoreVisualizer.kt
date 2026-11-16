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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.SpaceGroteskFontFamily
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

enum class NeuralCoreActivity { READY, LISTENING, THINKING, SPEAKING }

private data class NeuralNode3D(
    val x: Float,
    val y: Float,
    val z: Float,
    val phase: Float
)

private data class ProjectedNode(
    val x: Float,
    val y: Float,
    val z: Float,
    val alpha: Float,
    val radius: Float
)

@Composable
fun NeuralCoreVisualizer(
    modifier: Modifier = Modifier,
    size: Dp = 250.dp,
    motionEnabled: Boolean = true,
    coreLabel: String = "Z",
    activity: NeuralCoreActivity = NeuralCoreActivity.READY
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current
    val baseAccent = cyber.accent
    val accent = when (activity) {
        NeuralCoreActivity.READY -> baseAccent
        NeuralCoreActivity.LISTENING -> Color(0xFF35D8FF)
        NeuralCoreActivity.THINKING -> Color(0xFFB18CFF)
        NeuralCoreActivity.SPEAKING -> Color(0xFF75F2C2)
    }
    val accentSoft = when (activity) {
        NeuralCoreActivity.READY -> cyber.accentSoft
        NeuralCoreActivity.LISTENING -> Color(0xFF9AEFFF)
        NeuralCoreActivity.THINKING -> Color(0xFFD7C5FF)
        NeuralCoreActivity.SPEAKING -> Color(0xFFB8FFE2)
    }
    val activityEnergy = when (activity) {
        NeuralCoreActivity.READY -> 1f
        NeuralCoreActivity.LISTENING -> 1.28f
        NeuralCoreActivity.THINKING -> 1.16f
        NeuralCoreActivity.SPEAKING -> 1.38f
    }

    val systemReduceMotion = remember {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        scale == 0f
    }
    val animate = motionEnabled && !systemReduceMotion

    val nodes = remember {
        List(76) { i ->
            val y = 1f - (i / 75f) * 2f
            val radius = sqrt(max(0f, 1f - y * y))
            val angle = i * 2.3999632f
            NeuralNode3D(
                x = cos(angle) * radius,
                y = y,
                z = sin(angle) * radius,
                phase = i * 0.73f
            )
        }
    }

    var tiltX by remember { mutableFloatStateOf(0f) }
    var tiltY by remember { mutableFloatStateOf(0f) }
    var elapsedMillis by remember { mutableLongStateOf(0L) }

    LaunchedEffect(animate) {
        if (!animate) return@LaunchedEffect
        val startNanos = System.nanoTime()
        while (true) {
            withFrameNanos { frameNanos ->
                elapsedMillis = (frameNanos - startNanos) / 1_000_000L
            }
        }
    }

    val pulseTransition = rememberInfiniteTransition(label = "corePulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val sphereScale = if (animate) pulseScale else 1f
    val coreSphereSize = size * 0.44f

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.24f * cyber.glowStrength),
                        cyber.panelGlass.copy(alpha = 0.92f),
                        Color.Transparent
                    )
                )
            )
            .border(1.dp, accent.copy(alpha = 0.42f), CircleShape)
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
                    tiltY = (tiltY + dragAmount.x * 0.004f).coerceIn(-0.6f, 0.6f)
                    tiltX = (tiltX + dragAmount.y * 0.004f).coerceIn(-0.6f, 0.6f)
                }
            }
            .semantics {
                contentDescription = "Interactive 3D neural core, ${activity.name.lowercase()} state"
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val radiusScale = minOf(w, h) * 0.39f
            val nowSec = elapsedMillis * 0.001f
            val spin = elapsedMillis * 0.00018f * activityEnergy + tiltY
            val cosSpin = cos(spin)
            val sinSpin = sin(spin)
            val cosTilt = cos(tiltX)
            val sinTilt = sin(tiltX)

            val projected = nodes.map { n ->
                val rx = n.x * cosSpin - n.z * sinSpin
                val rz = n.x * sinSpin + n.z * cosSpin
                val ry = n.y
                val yy = ry * cosTilt - rz * sinTilt
                val zz = ry * sinTilt + rz * cosTilt
                val depth = 2.7f + zz * 0.55f
                val perspective = 2.7f / depth
                ProjectedNode(
                    x = cx + rx * radiusScale * perspective,
                    y = cy + yy * radiusScale * perspective,
                    z = zz,
                    alpha = (0.28f + 0.72f * (zz + 1f) / 2f).coerceIn(0.15f, 1f),
                    radius = (1.8f + ((zz + 1f) / 2f) * 2.6f)
                )
            }

            // Deep-space halo and soft atmospheric bloom behind the neural lattice.
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.22f * cyber.glowStrength),
                        accent.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = radiusScale * 1.62f
                ),
                radius = radiusScale * 1.62f,
                center = Offset(cx, cy)
            )

            // Fine radial energy filaments add depth without a heavyweight 3D dependency.
            for (ray in 0 until 36) {
                val angle = ray * (2f * PI.toFloat() / 36f) + spin * 0.22f
                val inner = radiusScale * 0.72f
                val outer = radiusScale * (1.12f + 0.05f * sin(nowSec * 1.4f + ray))
                drawLine(
                    color = accentSoft.copy(alpha = 0.08f * activityEnergy),
                    start = Offset(cx + cos(angle) * inner, cy + sin(angle) * inner),
                    end = Offset(cx + cos(angle) * outer, cy + sin(angle) * outer),
                    strokeWidth = if (ray % 3 == 0) 1.5f else 0.7f
                )
            }

            val linkDist = w * 0.145f
            for (i in projected.indices) {
                val a = projected[i]
                for (j in i + 1 until projected.size) {
                    val b = projected[j]
                    val d = hypot(a.x - b.x, a.y - b.y)
                    if (d < linkDist) {
                        val lineAlpha = (1f - d / linkDist) * 0.42f * activityEnergy * minOf(a.alpha, b.alpha)
                        drawLine(
                            color = accent.copy(alpha = lineAlpha.coerceIn(0f, 1f)),
                            start = Offset(a.x, a.y),
                            end = Offset(b.x, b.y),
                            strokeWidth = 1.2f
                        )
                    }
                }
            }

            projected.forEachIndexed { idx, p ->
                val pulse = if (animate) 0.65f + 0.35f * sin(nowSec * 2.2f + nodes[idx].phase) else 0.85f
                drawCircle(
                    color = accentSoft.copy(alpha = (p.alpha * pulse).coerceIn(0f, 1f)),
                    radius = p.radius * pulse * 1.65f * activityEnergy,
                    center = Offset(p.x, p.y)
                )
            }

            // A second, slower counter-rotating orbital track creates a layered 3D silhouette.
            rotate(degrees = if (animate) (elapsedMillis * -0.0065f) % 360f else -18f) {
                drawOval(
                    color = Color.White.copy(alpha = 0.24f),
                    topLeft = Offset(cx - radiusScale * 1.23f, cy - radiusScale * 0.31f),
                    size = Size(radiusScale * 2.46f, radiusScale * 0.62f),
                    style = Stroke(
                        width = 1.1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(3f, 9f), 0f)
                    )
                )
            }

            // Outer dashed ring
            rotate(degrees = if (animate) (elapsedMillis * 0.01f) % 360f else 0f) {
                drawCircle(
                    color = accentSoft.copy(alpha = 0.45f),
                    radius = minOf(w, h) * 0.46f,
                    style = Stroke(
                        width = 1.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                )
            }

            // Fine luminous latitude bands imply a volumetric, glass-like sphere.
            for (band in -2..2) {
                val bandY = cy + band * radiusScale * 0.19f
                val bandWidth = radiusScale * 2f * sqrt(max(0.08f, 1f - (band * 0.19f) * (band * 0.19f)))
                drawOval(
                    color = accentSoft.copy(alpha = (0.10f + 0.025f * (2 - kotlin.math.abs(band))) * activityEnergy),
                    topLeft = Offset(cx - bandWidth / 2f, bandY - radiusScale * 0.055f),
                    size = Size(bandWidth, radiusScale * 0.11f),
                    style = Stroke(width = 0.85f)
                )
            }

            // Tilted orbital ellipses
            val orbitRatios = listOf(0.82f, 1.02f, 1.18f)
            orbitRatios.forEachIndexed { k, r ->
                val deg = ((spin * 180f / PI.toFloat()) * 0.35f) + k * 52f - 28f
                rotate(degrees = deg) {
                    val ow = radiusScale * r * 2f
                    val oh = radiusScale * (0.24f + k * 0.03f) * 2f
                    drawOval(
                        color = (if (k == 1) Color.White else accentSoft).copy(alpha = 0.38f),
                        topLeft = Offset(cx - ow / 2f, cy - oh / 2f),
                        size = Size(ow, oh),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        // Central glowing holographic core sphere
        Box(
            modifier = Modifier
                .size(coreSphereSize * sphereScale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.85f),
                            accent,
                            Color(0xFF380D1C),
                            Color(0xFF090B10)
                        ),
                        center = Offset(0.35f, 0.25f)
                    )
                )
                .border(1.5.dp, accentSoft.copy(alpha = 0.9f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = coreLabel,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.22f).sp,
                color = Color.White
            )
        }
    }
}
