package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ZaytharPreferences
import com.example.domain.AgentRole
import com.example.ui.theme.ThemeMode
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberSectionTitle
import com.example.ui.theme.LocalCyberColors

private data class CommandTheme(
    val id: String,
    val title: String,
    val description: String,
    val accent: Long,
    val dark: Boolean
)

private val commandThemes = listOf(
    CommandTheme("reactor-red", "REACTOR RED", "Signature red / black command deck", 0xFFFF3155L, true),
    CommandTheme("titanium", "TITANIUM", "Silver metal with graphite surfaces", 0xFFB7C4D0L, true),
    CommandTheme("stealth", "STEALTH", "Minimal black with muted steel", 0xFF82909AL, true),
    CommandTheme("arc-blue", "ARC BLUE", "Deep navy with electric blue accents", 0xFF168BFFL, true),
    CommandTheme("light-command", "LIGHT COMMAND", "Clean silver and white interface", 0xFFB3261EL, false)
)

private val personalityProfiles = listOf(
    "friday" to "FRIDAY · Calm, proactive, capable",
    "professional" to "PROFESSIONAL · Structured and precise",
    "friendly" to "FRIENDLY · Warm and conversational",
    "developer" to "DEVELOPER · Engineering-focused",
    "researcher" to "RESEARCHER · Evidence-led analysis"
)

@Composable
fun CommandStudioScreen(
    prefs: ZaytharPreferences,
    selectedAgent: AgentRole,
    onUpdatePrefs: ((ZaytharPreferences) -> ZaytharPreferences) -> Unit,
    onSelectAgent: (AgentRole) -> Unit,
    onOpenAgentWorkspace: () -> Unit
) {
    val cyber = LocalCyberColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "PERSONALIZATION & CONTROL",
                titlePrefix = "COMMAND",
                titleAccent = "STUDIO"
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                CyberCardHeader(title = "VISUAL INTERFACE", badge = "5 PRESETS")
                Text(
                    text = "Choose a persistent command-deck palette. Your selection is saved on this device.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                commandThemes.forEach { theme ->
                    val selected = prefs.themePresetId == theme.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("theme_preset_${theme.id}"),
                        onClick = {
                            onUpdatePrefs {
                                it.copy(
                                    themePresetId = theme.id,
                                    accentHex = theme.accent,
                                    themeMode = if (theme.dark) ThemeMode.DARK else ThemeMode.LIGHT
                                )
                            }
                        },
                        color = if (selected) cyber.accent.copy(alpha = 0.12f) else cyber.panelElevated,
                        border = BorderStroke(
                            if (selected) 2.dp else 1.dp,
                            if (selected) cyber.accent else MaterialTheme.colorScheme.outline
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                color = Color(theme.accent),
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier.height(34.dp).weight(0.12f)
                            ) {}
                            Column(modifier = Modifier.weight(1f)) {
                                Text(theme.title, style = MaterialTheme.typography.titleSmall)
                                Text(theme.description, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = if (selected) "ACTIVE" else "APPLY",
                                color = if (selected) cyber.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(title = "AI PERSONALITY CORE", badge = "5 PROFILES")
                Text(
                    text = "This changes the native AI system instruction for future conversations; it does not change the underlying model.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(10.dp))
                personalityProfiles.forEach { (id, label) ->
                    FilterChip(
                        selected = prefs.aiPersonality == id,
                        onClick = { onUpdatePrefs { it.copy(aiPersonality = id) } },
                        label = { Text(label) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    )
                }
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(title = "ACTIVE SPECIALIST", badge = "AGENT ROUTING")
                Text(
                    text = selectedAgent.title.uppercase(),
                    style = MaterialTheme.typography.titleLarge,
                    color = cyber.accent
                )
                Text(
                    text = "Select a specialist for the next chat, or open Collaboration Nexus to run a coordinated team of agents.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                (listOf(AgentRole.GENERAL) + AgentRole.specialists).forEach { role ->
                    FilterChip(
                        selected = selectedAgent == role,
                        onClick = { onSelectAgent(role) },
                        label = { Text(role.title) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onOpenAgentWorkspace, modifier = Modifier.fillMaxWidth()) {
                    Text("OPEN COLLABORATION NEXUS")
                }
            }
        }
    }
}
