package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ZaytharPreferences
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberSectionTitle
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.SpaceGroteskFontFamily

private data class PersonaOption(
    val id: String,
    val name: String,
    val role: String,
    val tone: String,
    val defaultHonorific: String,
    val initial: String,
    val accentColor: Color
)

private val AVAILABLE_PERSONAS = listOf(
    PersonaOption(
        id = "jarvis",
        name = "JARVIS",
        role = "Chief of Staff",
        tone = "Crisp, factual, dry wit",
        defaultHonorific = "Sir",
        initial = "J",
        accentColor = Color(0xFFFFC107)
    ),
    PersonaOption(
        id = "friday",
        name = "FRIDAY",
        role = "Tactical Co-Pilot",
        tone = "Warm, direct, supportive",
        defaultHonorific = "Boss",
        initial = "F",
        accentColor = Color(0xFF00E5FF)
    ),
    PersonaOption(
        id = "zaythar",
        name = "ZAYTHAR CORE",
        role = "Neural Architect",
        tone = "Analytical, precise, balanced",
        defaultHonorific = "Operator",
        initial = "Z",
        accentColor = Color(0xFFFF3155)
    )
)

private val HONORIFIC_OPTIONS = listOf("Sir", "Boss", "Operator", "Chief")

@Composable
fun PersonaBuilderScreen(
    prefs: ZaytharPreferences,
    onUpdatePrefs: ((ZaytharPreferences) -> ZaytharPreferences) -> Unit,
    onToast: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    val activePersona = AVAILABLE_PERSONAS.find { it.id == prefs.selectedPersona } ?: AVAILABLE_PERSONAS[0]

    val styles = listOf(
        "Balanced & helpful",
        "Professional & concise",
        "Friendly & expressive",
        "Technical & analytical"
    )
    var styleMenuExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "IDENTITY MATRIX",
                titlePrefix = "AI Persona",
                titleAccent = "Builder"
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(activePersona.accentColor.copy(alpha = 0.4f), cyber.panelGlass)
                                )
                            )
                            .border(2.5.dp, activePersona.accentColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activePersona.initial,
                            fontFamily = SpaceGroteskFontFamily,
                            style = MaterialTheme.typography.displaySmall,
                            color = Color.White
                        )
                    }
                    Text(
                        text = activePersona.name,
                        fontFamily = SpaceGroteskFontFamily,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${activePersona.role} · Addressing you as '${prefs.honorific}'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "● ACTIVE PROTOCOL: ${activePersona.id.uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = activePersona.accentColor
                    )
                }
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(title = "AI PERSONA SELECTOR")
                Text(
                    text = "Switch active intelligence archetype to adapt conversational tone and protocol.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                AVAILABLE_PERSONAS.forEach { persona ->
                    val isSelected = prefs.selectedPersona == persona.id
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onUpdatePrefs {
                                    it.copy(
                                        selectedPersona = persona.id,
                                        honorific = persona.defaultHonorific
                                    )
                                }
                                onToast("Persona set to ${persona.name} (${persona.defaultHonorific})")
                            }
                            .testTag("persona_select_${persona.id}"),
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) persona.accentColor.copy(alpha = 0.15f) else cyber.panelElevated,
                        border = BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) persona.accentColor else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(persona.accentColor.copy(alpha = 0.25f))
                                    .border(1.5.dp, persona.accentColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = persona.initial,
                                    fontFamily = SpaceGroteskFontFamily,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = persona.accentColor
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = persona.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isSelected) persona.accentColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "• ${persona.role}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = persona.tone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Text(
                                    text = "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = persona.accentColor
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "HONORIFIC PROTOCOL",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    HONORIFIC_OPTIONS.forEachIndexed { index, option ->
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = HONORIFIC_OPTIONS.size),
                            onClick = {
                                onUpdatePrefs { it.copy(honorific = option) }
                                onToast("Honorific address updated to '$option'")
                            },
                            selected = prefs.honorific == option,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = activePersona.accentColor.copy(alpha = 0.25f),
                                activeContentColor = activePersona.accentColor,
                                inactiveContentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.testTag("honorific_option_${option.lowercase()}")
                        ) {
                            Text(option, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(title = "PERSONALITY PARAMETERS")

                CyberSliderRow(
                    label = "Empathy ratio",
                    value = prefs.empathyRatio,
                    onValueChange = { v -> onUpdatePrefs { it.copy(empathyRatio = v) } }
                )
                CyberSliderRow(
                    label = "Cognitive capacity",
                    value = prefs.cognitiveCapacity,
                    onValueChange = { v -> onUpdatePrefs { it.copy(cognitiveCapacity = v) } }
                )
                CyberSliderRow(
                    label = "Adaptability",
                    value = prefs.adaptability,
                    onValueChange = { v -> onUpdatePrefs { it.copy(adaptability = v) } }
                )
                CyberSliderRow(
                    label = "Creativity",
                    value = prefs.personaCreativity,
                    onValueChange = { v -> onUpdatePrefs { it.copy(personaCreativity = v) } }
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "ASSISTANT PERSONALITY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Box {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { styleMenuExpanded = true }
                            .testTag("persona_style_dropdown"),
                        shape = MaterialTheme.shapes.small,
                        color = cyber.panelElevated,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = prefs.personaStyle,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = styleMenuExpanded,
                        onDismissRequest = { styleMenuExpanded = false }
                    ) {
                        styles.forEach { style ->
                            DropdownMenuItem(
                                text = { Text(style) },
                                onClick = {
                                    onUpdatePrefs { it.copy(personaStyle = style) }
                                    styleMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                CyberActionButton(
                    text = "APPLY PERSONA CONFIG",
                    onClick = { onToast("Persona ${activePersona.name} (${prefs.honorific}) active.") },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "initialize_persona_button"
                )
            }
        }
    }
}
