package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ZaytharPreferences
import com.example.domain.ProviderRoute
import com.example.domain.SupportedLanguage
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.components.TelemetryBar
import com.example.ui.components.ZaytharSpiralEmblem
import com.example.ui.theme.AccentSwatches
import com.example.ui.theme.LocalCyberColors

@Composable
fun PersonaBuilderScreen(
    prefs: ZaytharPreferences,
    onUpdatePrefs: ((ZaytharPreferences) -> ZaytharPreferences) -> Unit,
    onToast: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    val styles = listOf(
        "Balanced & helpful",
        "Warm Companion & Empathetic",
        "Socratic Mentor & Coach",
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
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ZaytharSpiralEmblem(size = 120.dp)
                    Text(text = "ZAYTHAR NEURAL", style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Adaptive personal intelligence · ${prefs.personaStyle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "● SYSTEM ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(title = "PERSONALITY PARAMETERS")

                CyberSliderRow("Empathy ratio", prefs.empathyRatio) { v ->
                    onUpdatePrefs { it.copy(empathyRatio = v) }
                }
                CyberSliderRow("Cognitive capacity", prefs.cognitiveCapacity) { v ->
                    onUpdatePrefs { it.copy(cognitiveCapacity = v) }
                }
                CyberSliderRow("Adaptability", prefs.adaptability) { v ->
                    onUpdatePrefs { it.copy(adaptability = v) }
                }
                CyberSliderRow("Creativity", prefs.personaCreativity) { v ->
                    onUpdatePrefs { it.copy(personaCreativity = v) }
                }

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
                    text = "INITIALIZE PERSONA",
                    onClick = { onToast("Persona preferences saved and applied to future chats.") },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "initialize_persona_button"
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: ZaytharPreferences,
    onUpdatePrefs: ((ZaytharPreferences) -> ZaytharPreferences) -> Unit,
    onSelectAccent: (Long) -> Unit,
    onToast: (String) -> Unit,
    onNavigate: (String) -> Unit = {}
) {
    val cyber = LocalCyberColors.current
    var selectedCategory by remember { mutableStateOf(SettingsCategory.ALL) }
    var langExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    val currentLanguage = SupportedLanguage.fromCode(prefs.languageCode)
    val currentRoute = ProviderRoute.fromId(prefs.selectedProviderRoute)

    val showGeneral = selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.GENERAL
    val showAppearance = selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.APPEARANCE
    val showPersonalization = selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.PERSONALIZATION
    val showVoice = selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.VOICE
    val showSecurity = selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.SECURITY
    val showAbout = selectedCategory == SettingsCategory.ALL || selectedCategory == SettingsCategory.ABOUT

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "ZAYTHAR SETTINGS & APPEARANCE",
                titlePrefix = "Settings",
                titleAccent = "Center",
                trailing = {
                    CyberActionButton(
                        text = "SAVE",
                        onClick = { onToast("Preferences saved on this device.") },
                        testTag = "save_settings_button"
                    )
                }
            )
        }

        item {
            SettingsCategoryPills(
                selected = selectedCategory,
                onSelect = { selectedCategory = it }
            )
        }

        if (showGeneral) {
            item {
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    CyberCardHeader(title = "GENERAL & COGNITIVE ENGINE", badge = currentRoute.title)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable { langExpanded = true }
                                    .testTag("settings_language_dropdown"),
                                shape = MaterialTheme.shapes.small,
                                color = cyber.panelElevated,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("LANGUAGE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(currentLanguage.displayName, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            DropdownMenu(expanded = langExpanded, onDismissRequest = { langExpanded = false }) {
                                SupportedLanguage.entries.forEach { lang ->
                                    DropdownMenuItem(
                                        text = { Text(lang.displayName) },
                                        onClick = {
                                            onUpdatePrefs { it.copy(languageCode = lang.code) }
                                            langExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .minimumInteractiveComponentSize()
                                    .clickable { modelExpanded = true }
                                    .testTag("settings_model_dropdown"),
                                shape = MaterialTheme.shapes.small,
                                color = cyber.panelElevated,
                                border = BorderStroke(1.dp, cyber.accent.copy(alpha = 0.5f))
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("DEFAULT MODEL", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(currentRoute.title, style = MaterialTheme.typography.bodyMedium, color = cyber.accentSoft)
                                }
                            }
                            DropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                                ProviderRoute.entries.forEach { route ->
                                    DropdownMenuItem(
                                        text = { Text(route.title) },
                                        onClick = {
                                            onUpdatePrefs { it.copy(selectedProviderRoute = route.id) }
                                            modelExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    CyberToggleRow("Deep thinking mode (show reasoning)", prefs.thinkingEnabled, { c ->
                        onUpdatePrefs { it.copy(thinkingEnabled = c) }
                    }, testTag = "thinking_mode_settings_switch")
                    CyberToggleRow("Auto-mirror language (Urdu / Roman Urdu / Multi)", prefs.autoMirrorLanguage, { c ->
                        onUpdatePrefs { it.copy(autoMirrorLanguage = c) }
                    }, testTag = "auto_mirror_language_switch")
                    CyberToggleRow("Neural overdrive", prefs.neuralOverdrive, { c ->
                        onUpdatePrefs { it.copy(neuralOverdrive = c) }
                    })
                    CyberToggleRow("Auto cognition", prefs.autoCognition, { c ->
                        onUpdatePrefs { it.copy(autoCognition = c) }
                    })
                    CyberToggleRow("3D holographic motion effects", prefs.motion3dEnabled, { c ->
                        onUpdatePrefs { it.copy(motion3dEnabled = c) }
                    })

                    Spacer(modifier = Modifier.height(8.dp))
                    CyberSliderRow("Processing velocity", prefs.processingVelocity) { v ->
                        onUpdatePrefs { it.copy(processingVelocity = v) }
                    }
                    CyberSliderRow("Synaptic depth", prefs.synapticDepth) { v ->
                        onUpdatePrefs { it.copy(synapticDepth = v) }
                    }
                    CyberSliderRow("Response precision", prefs.responsePrecision) { v ->
                        onUpdatePrefs { it.copy(responsePrecision = v) }
                    }
                    CyberSliderRow("Creativity bias", prefs.creativityBias) { v ->
                        onUpdatePrefs { it.copy(creativityBias = v) }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    CyberSecondaryButton(
                        text = "RESET DEFAULTS",
                        onClick = {
                            onUpdatePrefs { ZaytharPreferences() }
                            onToast("Settings reset to defaults.")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "reset_settings_button"
                    )
                }
            }
        }

        if (showAppearance) {
            item {
                CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                    CyberCardHeader(title = "APPEARANCE & THEME", badge = prefs.themeMode.label.uppercase())

                    Text(
                        text = "CHOOSE HOW ZAYTHAR AI LOOKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    ThemeVisualPreviewCards(
                        selectedMode = prefs.themeMode,
                        onSelectMode = { mode ->
                            onUpdatePrefs { it.copy(themeMode = mode) }
                            onToast("Chat theme set to ${mode.label}.")
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "COLOR THEME",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AccentSwatches.forEach { (hex, name) ->
                            CircularAccentSwatch(
                                color = Color(hex),
                                selected = prefs.accentHex == hex,
                                name = name,
                                onClick = { onSelectAccent(hex) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    CyberToggleRow("Holographic interface", prefs.holographicUi, { c ->
                        onUpdatePrefs { it.copy(holographicUi = c) }
                    })
                    CyberSliderRow("Secondary glow", prefs.glowStrength) { v ->
                        onUpdatePrefs { it.copy(glowStrength = v) }
                    }
                    CyberSliderRow("Metallic specularity", prefs.metalSpecularity) { v ->
                        onUpdatePrefs { it.copy(metalSpecularity = v) }
                    }
                    CyberSliderRow("Contrast", prefs.contrastStrength) { v ->
                        onUpdatePrefs { it.copy(contrastStrength = v) }
                    }
                    TelemetryBar("Interface glow", "${prefs.glowStrength.toInt()}%", prefs.glowStrength / 100f)
                }
            }
        }

        if (showPersonalization) {
            item {
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    CyberCardHeader(title = "PERSONALIZATION & EMPATHY", badge = prefs.personaStyle.uppercase())
                    Text(
                        text = "Empathy: ${prefs.empathyRatio.toInt()}% · Cognitive: ${prefs.cognitiveCapacity.toInt()}% · Adaptability: ${prefs.adaptability.toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CyberSecondaryButton(
                        text = "OPEN AI PERSONA BUILDER",
                        onClick = { onNavigate("persona") },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_open_persona_button"
                    )
                }
            }
        }

        if (showVoice) {
            item {
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    CyberCardHeader(title = "VOICE & SPEECH", badge = "GEMINI 3.8 LIVE")
                    CyberToggleRow("Read chat replies aloud (TTS)", prefs.speakReplies, { c ->
                        onUpdatePrefs { it.copy(speakReplies = c) }
                    })
                    Spacer(modifier = Modifier.height(8.dp))
                    CyberSecondaryButton(
                        text = "OPEN LIVE VOICE & AUDIO NEXUS",
                        onClick = { onNavigate("voice_audio") },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_open_voice_button"
                    )
                }
            }
        }

        if (showSecurity || showAbout) {
            item {
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    CyberCardHeader(title = "DATA, SECURITY & ABOUT", badge = "v2.0.0")
                    Text(
                        text = "Operator: ${prefs.operatorName} · Cloud Firestore UID-scoped storage + Firebase App Check.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CyberSecondaryButton(
                            text = "SECURITY",
                            onClick = { onNavigate("security") },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_open_security_button"
                        )
                        CyberSecondaryButton(
                            text = "LIBRARY & RAG",
                            onClick = { onNavigate("library") },
                            modifier = Modifier.weight(1f),
                            testTag = "settings_open_library_button"
                        )
                    }
                }
            }
        }
    }
}
