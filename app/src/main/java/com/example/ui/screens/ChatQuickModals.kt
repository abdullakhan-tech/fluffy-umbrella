package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PsychologyAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.ProviderRoute
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.ZaytharIcons
import com.example.ui.theme.LocalCyberColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelSelectorSheet(
    currentRoute: ProviderRoute,
    onSelectRoute: (ProviderRoute) -> Unit,
    onOpenModelMatrix: () -> Unit,
    onDismiss: () -> Unit
) {
    val cyber = LocalCyberColors.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cyber.panelGlass,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("model_selector_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "SELECT AI MODEL",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = "All routes run live via Firebase AI Logic Gemini engines with resilient fallback.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ProviderRoute.entries, key = { it.id }) { route ->
                    val selected = route == currentRoute
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable {
                                onSelectRoute(route)
                                onDismiss()
                            }
                            .testTag("sheet_model_${route.id}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) cyber.accent.copy(alpha = 0.16f) else cyber.panelElevated,
                        border = BorderStroke(
                            1.dp,
                            if (selected) cyber.accent else MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CyberIconBadge(
                                icon = ZaytharIcons.forProviderRoute(route),
                                contentDescription = route.title,
                                sizeDp = 40.dp,
                                iconSizeDp = 20.dp,
                                isActive = selected
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = route.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (selected) cyber.accentSoft else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = route.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (selected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = "Selected model",
                                    tint = cyber.accentSoft
                                )
                            }
                        }
                    }
                }
            }
            CyberSecondaryButton(
                text = "COMPARE ALL MODELS IN MATRIX",
                onClick = {
                    onDismiss()
                    onOpenModelMatrix()
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "compare_models_sheet_button"
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

private data class QuickToolGridItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String,
    val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsHubSheet(
    webConnectEnabled: Boolean,
    onToggleWebConnect: () -> Unit,
    onNavigate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cyber = LocalCyberColors.current
    val items = listOf(
        QuickToolGridItem("Image Studio", "Nano Banana 2.1", Icons.Rounded.AutoAwesome, "studio", "tool_sheet_studio"),
        QuickToolGridItem("Code Copilot", "Multi-lang & audits", Icons.Rounded.Code, "code", "tool_sheet_code"),
        QuickToolGridItem("Camera Eyes", "Photo & OCR vision", Icons.Rounded.CameraAlt, "camera_eyes", "tool_sheet_vision"),
        QuickToolGridItem("AI Brain", "Reasoning & JSON", Icons.Rounded.PsychologyAlt, "brain", "tool_sheet_brain"),
        QuickToolGridItem("Plugin Plug", "Math, UTC & tools", Icons.Rounded.Extension, "plugins", "tool_sheet_plugins"),
        QuickToolGridItem("Prompt Nexus", "Expert templates", Icons.Rounded.Language, "prompts", "tool_sheet_prompts")
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cyber.panelGlass,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("tools_hub_sheet"),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Zaythar AI Tools",
                style = MaterialTheme.typography.titleLarge
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable {
                        onToggleWebConnect()
                        onDismiss()
                    }
                    .testTag("tool_sheet_web_toggle"),
                shape = RoundedCornerShape(12.dp),
                color = if (webConnectEnabled) cyber.cyberGreen.copy(alpha = 0.14f) else cyber.panelElevated,
                border = BorderStroke(1.dp, if (webConnectEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = if (webConnectEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Web Search Grounding", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (webConnectEnabled) "Live Google Search enabled for chat" else "Tap to enable real-time web grounding",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = if (webConnectEnabled) "ON" else "OFF",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (webConnectEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items.chunked(2).forEach { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pair.forEach { tool ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .clickable {
                                    onDismiss()
                                    onNavigate(tool.route)
                                }
                                .testTag(tool.tag),
                            shape = RoundedCornerShape(12.dp),
                            color = cyber.panelElevated,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tool.icon,
                                    contentDescription = tool.title,
                                    tint = cyber.accentSoft,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(tool.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    text = tool.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable {
                        onDismiss()
                        onNavigate("engine_hub")
                    }
                    .testTag("tool_sheet_engine_hub"),
                shape = RoundedCornerShape(12.dp),
                color = cyber.accent.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, cyber.accent.copy(alpha = 0.6f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Open AI Engine Nexus", style = MaterialTheme.typography.titleSmall, color = cyber.accentSoft)
                        Text("Smart Router & all-engine verification suite", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = cyber.accentSoft)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun VoiceChatBottomBar(
    onOpenVoiceChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyber = LocalCyberColors.current
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onOpenVoiceChat)
            .testTag("voice_chat_bottom_pill"),
        shape = RoundedCornerShape(16.dp),
        color = cyber.panelGlass,
        border = BorderStroke(1.dp, cyber.accent.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(cyber.accent.copy(alpha = 0.18f), Color.Transparent)
                    )
                )
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(cyber.accentSoft, cyber.accent, Color(0xFF1A0810))
                        )
                    )
                    .border(1.dp, cyber.accentSoft.copy(alpha = 0.7f), CircleShape)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Tap to start voice chat",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Talk naturally with Zaythar AI",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                listOf(10, 18, 12, 22, 14, 18, 10).forEach { h ->
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(h.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(cyber.accentSoft.copy(alpha = 0.65f))
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cyber.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Mic,
                    contentDescription = "Start voice chat",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
