package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Molecular-scale holographic orb: nested orbital shells, bonded atoms, and travelling
 * synaptic packets. This is a visual system monitor, not a representation of literal biology.
 */
@Composable
fun MolecularNeuralOrb(
    modifier: Modifier = Modifier,
    size: Dp = 164.dp,
    activity: Float = 0.72f
) {
    val cyber = com.example.ui.theme.LocalCyberColors.current
    val transition = rememberInfiniteTransition(label = "molecularOrb")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitalPhase"
    )
    val level = activity.coerceIn(0.1f, 1f)

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(cyber.accent.copy(alpha = 0.18f), cyber.panelGlass, Color.Transparent)
                )
            )
            .semantics { contentDescription = "Molecular neural orb with animated atom bonds and synaptic signals" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val radius = this.size.minDimension * 0.39f
            val atomCount = 18
            val points = List(atomCount) { index ->
                val band = index % 3
                val angle = phase * (if (band == 1) -0.72f else 1f) + index * (2f * PI.toFloat() / atomCount)
                val r = radius * (0.45f + band * 0.22f)
                androidx.compose.ui.geometry.Offset(
                    cx + cos(angle) * r,
                    cy + sin(angle) * r * (0.68f + band * 0.08f)
                )
            }

            // Molecular bonds connect each atom to its nearest neighbors.
            points.forEachIndexed { index, point ->
                val next = points[(index + 1) % atomCount]
                val cross = points[(index + 5) % atomCount]
                drawLine(cyber.accentSoft.copy(alpha = 0.52f), point, next, strokeWidth = 1.35f)
                drawLine(cyber.cyberGreen.copy(alpha = 0.24f), point, cross, strokeWidth = 0.8f)
            }

            // Three counter-rotating orbital shells.
            repeat(3) { ring ->
                rotate(degrees = phase * (if (ring == 1) -24f else 18f) + ring * 60f) {
                    drawOval(
                        color = (if (ring == 1) cyber.cyberGreen else cyber.accentSoft).copy(alpha = 0.42f),
                        topLeft = androidx.compose.ui.geometry.Offset(cx - radius * (0.72f + ring * 0.13f), cy - radius * (0.25f + ring * 0.05f)),
                        size = androidx.compose.ui.geometry.Size(radius * (1.44f + ring * 0.26f), radius * (0.5f + ring * 0.1f)),
                        style = Stroke(width = 1.1f)
                    )
                }
            }

            points.forEachIndexed { index, point ->
                val packet = ((phase / (2f * PI.toFloat()) * 2.4f + index * 0.173f) % 1f)
                drawCircle(
                    color = cyber.accent.copy(alpha = (0.52f + 0.42f * level)),
                    radius = 2.1f + 1.2f * level,
                    center = point
                )
                if (packet < 0.12f) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.92f),
                        radius = 3.1f,
                        center = point
                    )
                }
            }

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color.White.copy(alpha = 0.95f), cyber.accent, Color(0xFF4A1020), Color(0xFF08090D)),
                    center = androidx.compose.ui.geometry.Offset(cx * 0.88f, cy * 0.82f),
                    radius = radius * 0.72f
                ),
                radius = radius * 0.27f,
                center = androidx.compose.ui.geometry.Offset(cx, cy)
            )
            drawCircle(
                color = cyber.accentSoft.copy(alpha = 0.65f),
                radius = radius * 0.3f,
                center = androidx.compose.ui.geometry.Offset(cx, cy),
                style = Stroke(width = 1.3f)
            )
        }
    }
}
