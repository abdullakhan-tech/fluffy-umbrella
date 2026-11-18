package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalCyberColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated agent topology panel for the Zaythar command dashboard.
 * The diagram is intentionally a visualization of configured app modules,
 * not a claim that every specialist is currently executing.
 */
@Composable
fun AgentNexusVisualization(
    motionEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val cyber = LocalCyberColors.current
    val transition = rememberInfiniteTransition(label = "agentNexus")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "agentNexusRotation"
    )
    val accent = Color(0xFFFF355D)
    val glow = Color(0xFFFF8A9E)
    val agents = listOf("VOICE", "VISION", "RESEARCH", "CODE", "MEMORY", "PLANNER")

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF101117), Color(0xFF07090E))
                ),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "COLLABORATION NEXUS",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                "AGENT TOPOLOGY",
                style = MaterialTheme.typography.labelSmall,
                color = glow
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radiusX = size.width * 0.36f
            val radiusY = size.height * 0.37f
            val nodes = agents.indices.map { index ->
                val angle = Math.toRadians((index * 60.0) + (if (motionEnabled) phase.toDouble() else 0.0) * 0.12)
                Offset(
                    center.x + cos(angle).toFloat() * radiusX,
                    center.y + sin(angle).toFloat() * radiusY
                )
            }

            // Fine radial guides and a subtle command-grid frame.
            for (ring in 1..3) {
                drawCircle(
                    color = accent.copy(alpha = 0.10f / ring),
                    radius = minOf(size.width, size.height) * (0.12f + ring * 0.12f),
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            nodes.forEachIndexed { index, point ->
                drawLine(
                    color = accent.copy(alpha = 0.50f),
                    start = center,
                    end = point,
                    strokeWidth = 1.2.dp.toPx()
                )
                drawLine(
                    color = glow.copy(alpha = 0.20f),
                    start = point,
                    end = nodes[(index + 1) % nodes.size],
                    strokeWidth = 0.8.dp.toPx()
                )
                if (motionEnabled) {
                    val pulse = (phase / 360f + index / 6f) % 1f
                    val moving = Offset(
                        center.x + (point.x - center.x) * pulse,
                        center.y + (point.y - center.y) * pulse
                    )
                    drawCircle(accent.copy(alpha = 0.85f), radius = 3.2.dp.toPx(), center = moving)
                    drawCircle(glow.copy(alpha = 0.15f), radius = 8.dp.toPx(), center = moving)
                }
                drawCircle(
                    color = accent.copy(alpha = 0.14f),
                    radius = 21.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = glow.copy(alpha = 0.9f),
                    radius = 13.dp.toPx(),
                    center = point,
                    style = Stroke(width = 1.4.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFF20232C),
                    radius = 9.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = if (motionEnabled) accent else glow.copy(alpha = 0.65f),
                    radius = 3.dp.toPx(),
                    center = point
                )
            }

            drawCircle(
                color = accent.copy(alpha = 0.20f),
                radius = 34.dp.toPx(),
                center = center
            )
            drawCircle(
                color = accent.copy(alpha = 0.95f),
                radius = 25.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF171923),
                radius = 20.dp.toPx(),
                center = center
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            agents.take(3).forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = glow)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            agents.drop(3).forEach { label ->
                Text(label, style = MaterialTheme.typography.labelSmall, color = glow)
            }
        }
        Text(
            text = if (motionEnabled) "NEURAL LINKS · ANIMATED" else "NEURAL LINKS · MOTION PAUSED",
            style = MaterialTheme.typography.labelSmall,
            color = cyber.accentSoft
        )
    }
}
