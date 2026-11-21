package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DataObject
import androidx.compose.material.icons.rounded.DocumentScanner
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.PsychologyAlt
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Troubleshoot
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.domain.NeuralBrainMode
import com.example.domain.ProviderRoute
import com.example.domain.VisionMode
import com.example.domain.ZaytharEngineId
import com.example.ui.theme.LocalCyberColors
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ZaytharHexEmblem(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val cyber = LocalCyberColors.current
    val primary = cyber.accent
    val glow = cyber.accentSoft
    val panel = cyber.panelGlass

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f
        val cy = h / 2f
        val outerR = (minOf(w, h) * 0.45f).coerceAtLeast(1f)
        val innerR = outerR * 0.78f

        fun hexPath(radius: Float): Path {
            val path = Path()
            for (i in 0 until 6) {
                val angle = (PI / 3.0) * i - (PI / 2.0)
                val px = cx + radius * cos(angle).toFloat()
                val py = cy + radius * sin(angle).toFloat()
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            return path
        }

        val outerHex = hexPath(outerR)
        drawPath(
            path = outerHex,
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.42f), panel),
                center = Offset(cx, cy),
                radius = outerR
            )
        )
        drawPath(
            path = outerHex,
            color = primary,
            style = Stroke(width = (w * 0.055f).coerceAtLeast(1.5f), join = StrokeJoin.Round)
        )
        drawPath(
            path = hexPath(innerR),
            color = glow.copy(alpha = 0.55f),
            style = Stroke(width = (w * 0.025f).coerceAtLeast(1f), join = StrokeJoin.Round)
        )

        // Geometric "Z" neural bolt
        val zx = outerR * 0.40f
        val zy = outerR * 0.42f
        val zPath = Path().apply {
            moveTo(cx - zx, cy - zy)
            lineTo(cx + zx, cy - zy)
            lineTo(cx - zx * 0.85f, cy + zy)
            lineTo(cx + zx, cy + zy)
        }
        drawPath(
            path = zPath,
            color = Color.White,
            style = Stroke(
                width = (w * 0.11f).coerceAtLeast(2.2f),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        // Neural terminal nodes on Z ends
        val nodeRadius = (w * 0.055f).coerceAtLeast(1.5f)
        drawCircle(color = glow, radius = nodeRadius, center = Offset(cx - zx, cy - zy))
        drawCircle(color = glow, radius = nodeRadius, center = Offset(cx + zx, cy + zy))
    }
}

@Composable
fun CyberIconBadge(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    sizeDp: Dp = 56.dp,
    iconSizeDp: Dp = 28.dp,
    tint: Color? = null,
    isActive: Boolean = true
) {
    val cyber = LocalCyberColors.current
    val resolvedTint = tint ?: if (isActive) cyber.accentSoft else MaterialTheme.colorScheme.onSurfaceVariant
    val borderColor = if (isActive) cyber.accent else MaterialTheme.colorScheme.outline

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        resolvedTint.copy(alpha = if (isActive) 0.28f else 0.12f),
                        cyber.panelGlass
                    )
                )
            )
            .border(BorderStroke(1.2.dp, borderColor), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = resolvedTint,
            modifier = Modifier.size(iconSizeDp)
        )
    }
}

object ZaytharIcons {
    fun forProviderRoute(route: ProviderRoute): ImageVector = when (route) {
        ProviderRoute.AUTO -> Icons.Rounded.AutoAwesome
        ProviderRoute.HYBRID -> Icons.Rounded.Hub
        ProviderRoute.FLASH -> Icons.Rounded.Bolt
        ProviderRoute.ARGON -> Icons.Rounded.PsychologyAlt
        ProviderRoute.LITE -> Icons.Rounded.Speed
        ProviderRoute.HYDROGEN -> Icons.Rounded.TravelExplore
        ProviderRoute.CARBON -> Icons.Rounded.Memory
        ProviderRoute.NEON -> Icons.Rounded.PrecisionManufacturing
        ProviderRoute.FLASH_LITE_31 -> Icons.Rounded.FlashOn
    }

    fun forEngine(engine: ZaytharEngineId): ImageVector = when (engine) {
        ZaytharEngineId.SMART_ROUTER -> Icons.Rounded.PrecisionManufacturing
        ZaytharEngineId.NEURAL_CHAT -> Icons.Rounded.ChatBubble
        ZaytharEngineId.AI_BRAIN -> Icons.Rounded.PsychologyAlt
        ZaytharEngineId.IMAGE_STUDIO -> Icons.Rounded.AutoAwesome
        ZaytharEngineId.CAMERA_EYES -> Icons.Rounded.CameraAlt
        ZaytharEngineId.CODE_COPILOT -> Icons.Rounded.Code
        ZaytharEngineId.MULTI_AGENT -> Icons.Rounded.Hub
        ZaytharEngineId.RAG_AND_TOOLS -> Icons.Rounded.Memory
        ZaytharEngineId.VOICE_AUDIO -> Icons.Rounded.GraphicEq
    }

    fun forBrainMode(mode: NeuralBrainMode): ImageVector = when (mode) {
        NeuralBrainMode.REASONER -> Icons.Rounded.PsychologyAlt
        NeuralBrainMode.CONSOLIDATOR -> Icons.Rounded.AccountTree
        NeuralBrainMode.STRATEGIST -> Icons.Rounded.Route
        NeuralBrainMode.DIAGNOSTICS -> Icons.Rounded.Troubleshoot
    }

    fun forVisionMode(mode: VisionMode): ImageVector = when (mode) {
        VisionMode.SCENE -> Icons.Rounded.CenterFocusStrong
        VisionMode.OCR -> Icons.Rounded.DocumentScanner
        VisionMode.CODE_UI -> Icons.Rounded.Terminal
        VisionMode.OBJECT_ID -> Icons.Rounded.QrCodeScanner
    }

    fun forPlugin(pluginKey: String, category: String): ImageVector = when {
        pluginKey == "calculator" -> Icons.Rounded.Calculate
        pluginKey == "utc_clock" -> Icons.Rounded.Schedule
        pluginKey == "unit_converter" -> Icons.Rounded.Speed
        pluginKey == "json_inspector" -> Icons.Rounded.DataObject
        pluginKey == "web_grounding" -> Icons.Rounded.Public
        pluginKey == "kotlin_architect" -> Icons.Rounded.Code
        pluginKey == "zero_trust_auditor" -> Icons.Rounded.VerifiedUser
        pluginKey == "crypto_hash_uuid" -> Icons.Rounded.Security
        pluginKey == "color_text_metrics" -> Icons.Rounded.AutoAwesome
        category.equals("Security", ignoreCase = true) -> Icons.Rounded.Shield
        category.equals("Compute", ignoreCase = true) -> Icons.Rounded.Calculate
        category.equals("Developer", ignoreCase = true) -> Icons.Rounded.DataObject
        category.equals("Internet", ignoreCase = true) -> Icons.Rounded.Public
        category.equals("Engineering", ignoreCase = true) -> Icons.Rounded.Code
        else -> Icons.Rounded.Extension
    }

    val KnowledgeNode: ImageVector = Icons.Rounded.Hub
    val SecurityShield: ImageVector = Icons.Rounded.VerifiedUser
    val StudioSpark: ImageVector = Icons.Rounded.AutoAwesome
    val Dashboard: ImageVector = Icons.Rounded.Dashboard
    val PromptBook: ImageVector = Icons.AutoMirrored.Rounded.MenuBook
    val BrainCore: ImageVector = Icons.Rounded.Psychology
}
