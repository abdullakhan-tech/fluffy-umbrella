package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ZaytharPreferences
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberSectionTitle
import com.example.ui.components.TelemetryBar
import com.example.ui.theme.AccentSwatches
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: ZaytharPreferences,
    onUpdatePrefs: ((ZaytharPreferences) -> ZaytharPreferences) -> Unit,
    onSelectAccent: (Long) -> Unit,
    onToast: (String) -> Unit
) {
    val cyber = LocalCyberColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "NEURAL CONFIGURATION",
                titlePrefix = "AI Cognitive",
                titleAccent = "Settings",
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
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(title = "PROCESSING PARAMETERS")

                CyberSliderRow(
                    label = "Processing velocity",
                    value = prefs.processingVelocity,
                    onValueChange = { v -> onUpdatePrefs { it.copy(processingVelocity = v) } }
                )
                CyberSliderRow(
                    label = "Synaptic depth",
                    value = prefs.synapticDepth,
                    onValueChange = { v -> onUpdatePrefs { it.copy(synapticDepth = v) } }
                )
                CyberSliderRow(
                    label = "Response precision",
                    value = prefs.responsePrecision,
                    onValueChange = { v -> onUpdatePrefs { it.copy(responsePrecision = v) } }
                )
                CyberSliderRow(
                    label = "Creativity bias",
                    value = prefs.creativityBias,
                    onValueChange = { v -> onUpdatePrefs { it.copy(creativityBias = v) } }
                )

                CyberToggleRow(
                    label = "Neural overdrive",
                    checked = prefs.neuralOverdrive,
                    onCheckedChange = { c -> onUpdatePrefs { it.copy(neuralOverdrive = c) } }
                )
                CyberToggleRow(
                    label = "Holographic interface",
                    checked = prefs.holographicUi,
                    onCheckedChange = { c -> onUpdatePrefs { it.copy(holographicUi = c) } }
                )
                CyberToggleRow(
                    label = "Auto cognition",
                    checked = prefs.autoCognition,
                    onCheckedChange = { c -> onUpdatePrefs { it.copy(autoCognition = c) } }
                )
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(
                    title = "THEME & VISUALIZER",
                    badge = prefs.themeMode.label.uppercase()
                )

                Text(
                    text = "CHAT INTERFACE APPEARANCE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val modes = listOf(
                        ThemeMode.SYSTEM to Icons.Default.BrightnessAuto,
                        ThemeMode.LIGHT to Icons.Default.LightMode,
                        ThemeMode.DARK to Icons.Default.DarkMode
                    )
                    modes.forEachIndexed { index, (mode, icon) ->
                        val selected = prefs.themeMode == mode
                        SegmentedButton(
                            selected = selected,
                            onClick = {
                                onUpdatePrefs { it.copy(themeMode = mode) }
                                onToast("Chat theme set to ${mode.label}.")
                            },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = cyber.accent.copy(alpha = 0.2f),
                                activeContentColor = cyber.accentSoft,
                                activeBorderColor = cyber.accent
                            ),
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("theme_mode_${mode.id}")
                        ) {
                            Text(
                                text = when (mode) {
                                    ThemeMode.SYSTEM -> "System"
                                    ThemeMode.LIGHT -> "Light"
                                    ThemeMode.DARK -> "Dark"
                                },
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Follow your device dark/light mode automatically or lock the chat interface to Light or Dark.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))

                CyberSliderRow(
                    label = "Secondary glow",
                    value = prefs.glowStrength,
                    onValueChange = { v -> onUpdatePrefs { it.copy(glowStrength = v) } }
                )
                CyberSliderRow(
                    label = "Metallic specularity",
                    value = prefs.metalSpecularity,
                    onValueChange = { v -> onUpdatePrefs { it.copy(metalSpecularity = v) } }
                )
                CyberSliderRow(
                    label = "Contrast",
                    value = prefs.contrastStrength,
                    onValueChange = { v -> onUpdatePrefs { it.copy(contrastStrength = v) } }
                )

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "ACCENT COLOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AccentSwatches.forEach { (hex, name) ->
                        val selected = prefs.accentHex == hex
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color(hex), MaterialTheme.shapes.small)
                                .border(
                                    width = if (selected) 3.dp else 1.dp,
                                    color = if (selected) Color.White else MaterialTheme.colorScheme.outline,
                                    shape = MaterialTheme.shapes.small
                                )
                                .clickable { onSelectAccent(hex) }
                                .testTag("accent_swatch_${name.lowercase()}")
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Selected: " + (AccentSwatches.firstOrNull { it.first == prefs.accentHex }?.second ?: "Custom"),
                    style = MaterialTheme.typography.labelSmall,
                    color = cyber.accentSoft
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select an accent color to personalize the command interface.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                TelemetryBar(
                    label = "Interface glow",
                    statusText = "${prefs.glowStrength.toInt()}%",
                    progress = prefs.glowStrength / 100f
                )
                Spacer(modifier = Modifier.height(10.dp))
                TelemetryBar(
                    label = "Metallic contrast",
                    statusText = "${prefs.contrastStrength.toInt()}%",
                    progress = prefs.contrastStrength / 100f
                )
            }
        }
    }
}

@Composable
internal fun CyberSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    val cyber = LocalCyberColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${value.toInt()}%",
                style = MaterialTheme.typography.titleSmall,
                color = cyber.accentSoft
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..100f,
            colors = SliderDefaults.colors(
                thumbColor = cyber.accentSoft,
                activeTrackColor = cyber.accent
            )
        )
    }
}

@Composable
internal fun CyberToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val cyber = LocalCyberColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = cyber.accent
                )
            )
        }
    }
}