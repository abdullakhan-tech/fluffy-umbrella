package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PrecisionManufacturing
import androidx.compose.material.icons.rounded.PsychologyAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.ZaytharPreferences
import com.example.domain.ProviderRoute
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSectionTitle
import com.example.ui.components.NeuralCoreVisualizer
import com.example.ui.components.TelemetryBar
import com.example.ui.theme.LocalCyberColors

private data class CommandDeckModule(
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTag: String
)

@Composable
fun OverviewScreen(
    prefs: ZaytharPreferences,
    firebaseReady: Boolean,
    uptimeSeconds: Long,
    activePluginCount: Int,
    onNavigate: (String) -> Unit,
    ragCorpusCount: Int = 0
) {
    val activeRoute = ProviderRoute.fromId(prefs.selectedProviderRoute)
    val uptimeLabel = if (uptimeSeconds < 60) "${uptimeSeconds}s" else "${uptimeSeconds / 60}m ${uptimeSeconds % 60}s"
    val uptimeProgress = (uptimeSeconds / 3600f).coerceIn(0.05f, 1f)
    val coreActivity = (0.45f + (activePluginCount * 0.06f) + if (firebaseReady) 0.15f else 0f).coerceIn(0.35f, 1.0f)

    val deckModules = listOf(
        CommandDeckModule("engine_hub", "AI Engine Nexus", "Smart Router & live suite runner", Icons.Rounded.PrecisionManufacturing, "deck_engines"),
        CommandDeckModule("assistant", "AI Assistant", "Multi-thread chat, Search & Maps", Icons.Rounded.ChatBubbleOutline, "deck_assistant"),
        CommandDeckModule("voice_audio", "Voice & Audio", "Gemini 3.8 Live & Transcribe", Icons.Rounded.GraphicEq, "deck_voice_audio"),
        CommandDeckModule("brain", "AI Brain", "RAG-grounded neural reasoning", Icons.Rounded.PsychologyAlt, "deck_brain"),
        CommandDeckModule("camera_eyes", "Camera Eyes", "Live multimodal photo vision", Icons.Rounded.CameraAlt, "deck_camera_eyes"),
        CommandDeckModule("plugins", "Plugin Plug Hub", "Extensible tools & directives", Icons.Rounded.Extension, "deck_plugins"),
        CommandDeckModule("code", "Code Copilot", "Principal engineering & audits", Icons.Rounded.Code, "deck_code"),
        CommandDeckModule("collaboration", "Collaboration Nexus", "Multi-agent specialist swarm", Icons.Rounded.Hub, "deck_collaboration"),
        CommandDeckModule("studio", "Media Studio", "Nano Banana 2.1 AI image gen", Icons.Rounded.AutoAwesome, "deck_studio"),
        CommandDeckModule("knowledge", "Knowledge Base", "Hybrid BM25 + Graph RAG", Icons.Rounded.Memory, "deck_knowledge"),
        CommandDeckModule("settingsPanel", "AI Core Settings", "Processing & visual tuning", Icons.Rounded.Tune, "deck_settings")
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 720.dp
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        NeuralCoreHeroCard(
                            modifier = Modifier.weight(1.6f),
                            motionEnabled = prefs.motion3dEnabled,
                            activityLevel = coreActivity,
                            onOpenEngineHub = { onNavigate("engine_hub") }
                        )
                        TelemetryStatusCard(
                            modifier = Modifier.weight(1f),
                            firebaseReady = firebaseReady,
                            activeRoute = activeRoute,
                            webConnected = prefs.webConnectEnabled,
                            activePluginCount = activePluginCount,
                            uptimeLabel = uptimeLabel,
                            uptimeProgress = uptimeProgress,
                            motionEnabled = prefs.motion3dEnabled,
                            ragCorpusCount = ragCorpusCount
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        NeuralCoreHeroCard(
                            modifier = Modifier.fillMaxWidth(),
                            motionEnabled = prefs.motion3dEnabled,
                            activityLevel = coreActivity,
                            onOpenEngineHub = { onNavigate("engine_hub") }
                        )
                        TelemetryStatusCard(
                            modifier = Modifier.fillMaxWidth(),
                            firebaseReady = firebaseReady,
                            activeRoute = activeRoute,
                            webConnected = prefs.webConnectEnabled,
                            activePluginCount = activePluginCount,
                            uptimeLabel = uptimeLabel,
                            uptimeProgress = uptimeProgress,
                            motionEnabled = prefs.motion3dEnabled,
                            ragCorpusCount = ragCorpusCount
                        )
                    }
                }
            }
        }

        item {
            CyberSectionTitle(
                eyebrow = "INTELLIGENCE MODULES",
                titlePrefix = "Command",
                titleAccent = "Deck"
            )
        }

        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columns = if (maxWidth >= 720.dp) 3 else 2
                val rows = deckModules.chunked(columns)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { mod ->
                                DeckModuleCard(
                                    module = mod,
                                    onClick = { onNavigate(mod.route) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(columns - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "ZAYTHAR AI · NEURAL COMMAND NEXUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "AI CAN MAKE MISTAKES · VERIFY OUTPUTS",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun NeuralCoreHeroCard(
    modifier: Modifier = Modifier,
    motionEnabled: Boolean,
    activityLevel: Float,
    onOpenEngineHub: () -> Unit
) {
    val cyber = LocalCyberColors.current
    CyberCard(modifier = modifier) {
        CyberCardHeader(
            title = "NEURAL CORE VISUALIZER",
            badge = "● 2-LAYER SYNAPSE CORE",
            badgeColor = cyber.cyberGreen
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_neural_banner),
                contentDescription = "Neural command center holographic backdrop",
                contentScale = ContentScale.Crop,
                alpha = if (cyber.isDark) 0.25f else 0.12f,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                cyber.panelGlass.copy(alpha = 0.65f)
                            )
                        )
                    )
            )
            NeuralCoreVisualizer(
                size = 225.dp,
                motionEnabled = motionEnabled,
                activityLevel = activityLevel
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MULTI-LAYER SYNAPTIC CORE · LIVE RENDER",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (motionEnabled) "SYNAPTIC FIELD ONLINE" else "STATIC RENDER",
                style = MaterialTheme.typography.labelSmall,
                color = cyber.cyberGreen
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        CyberActionButton(
            text = "OPEN AI ENGINE HUB & RUN SUITE",
            onClick = onOpenEngineHub,
            modifier = Modifier.fillMaxWidth(),
            testTag = "open_engine_hub_hero_button"
        )
    }
}

@Composable
private fun TelemetryStatusCard(
    modifier: Modifier = Modifier,
    firebaseReady: Boolean,
    activeRoute: ProviderRoute,
    webConnected: Boolean,
    activePluginCount: Int,
    uptimeLabel: String,
    uptimeProgress: Float,
    motionEnabled: Boolean,
    ragCorpusCount: Int
) {
    val cyber = LocalCyberColors.current
    CyberCard(modifier = modifier) {
        CyberCardHeader(
            title = "SYSTEM TELEMETRY",
            badge = "STATUS INDICATORS",
            badgeColor = cyber.cyberGreen
        )

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            TelemetryBar(
                label = "Firebase AI Logic",
                statusText = if (firebaseReady) "READY" else "UNCONFIGURED",
                progress = if (firebaseReady) 1f else 0f,
                useGreen = firebaseReady
            )
            TelemetryBar(
                label = "Hybrid RAG Core",
                statusText = if (ragCorpusCount > 0) "$ragCorpusCount DOCS INDEXED" else "BM25 + GRAPH READY",
                progress = if (ragCorpusCount > 0) (ragCorpusCount / 20f).coerceIn(0.25f, 1f) else 0.5f,
                useGreen = true
            )
            TelemetryBar(
                label = "LLM Web Grounding",
                statusText = if (webConnected) "INTERNET LIVE" else "OFFLINE",
                progress = if (webConnected) 1f else 0.25f,
                useGreen = webConnected
            )
            TelemetryBar(
                label = "Plugin Plugs",
                statusText = "$activePluginCount PLUGGED IN",
                progress = (activePluginCount / 6f).coerceIn(0.1f, 1f),
                useGreen = activePluginCount > 0
            )
            TelemetryBar(
                label = "Active Route",
                statusText = activeRoute.title,
                progress = 1f
            )
            TelemetryBar(
                label = "Session uptime",
                statusText = uptimeLabel,
                progress = uptimeProgress,
                useGreen = true
            )
            TelemetryBar(
                label = "Neural renderer",
                statusText = if (motionEnabled) "ACTIVE" else "PAUSED",
                progress = if (motionEnabled) 1f else 0.35f,
                useGreen = motionEnabled
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "APP CHECK / AI CORE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = if (firebaseReady) "AI READY" else "SETUP REQUIRED",
                style = MaterialTheme.typography.labelSmall,
                color = if (firebaseReady) cyber.cyberGreen else cyber.accentSoft
            )
        }
    }
}

@Composable
private fun DeckModuleCard(
    module: CommandDeckModule,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyber = LocalCyberColors.current
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(module.testTag)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = cyber.panelElevated,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            CyberIconBadge(
                icon = module.icon,
                contentDescription = module.title,
                sizeDp = 42.dp,
                iconSizeDp = 22.dp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = module.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = module.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "OPEN MODULE",
                    style = MaterialTheme.typography.labelSmall,
                    color = cyber.accentSoft
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = cyber.accentSoft,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
