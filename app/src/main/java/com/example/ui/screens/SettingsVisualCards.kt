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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.ThemeMode

internal enum class SettingsCategory(val label: String, val icon: ImageVector) {
    ALL("All Settings", Icons.Rounded.Settings),
    GENERAL("General", Icons.Rounded.Settings),
    APPEARANCE("Appearance", Icons.Rounded.Palette),
    PERSONALIZATION("Personalization", Icons.Rounded.Person),
    VOICE("Voice & Speech", Icons.Rounded.GraphicEq),
    SECURITY("Security & Data", Icons.Rounded.VerifiedUser),
    ABOUT("About", Icons.Rounded.Info)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SettingsCategoryPills(
    selected: SettingsCategory,
    onSelect: (SettingsCategory) -> Unit
) {
    val cyber = LocalCyberColors.current
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SettingsCategory.entries.forEach { cat ->
            val active = cat == selected
            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable { onSelect(cat) }
                    .testTag("settings_tab_${cat.name.lowercase()}"),
                shape = RoundedCornerShape(10.dp),
                color = if (active) cyber.accent.copy(alpha = 0.2f) else cyber.panelElevated,
                border = BorderStroke(1.dp, if (active) cyber.accent else MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = cat.icon,
                        contentDescription = null,
                        tint = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = cat.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
internal fun ThemeVisualPreviewCards(
    selectedMode: ThemeMode,
    onSelectMode: (ThemeMode) -> Unit
) {
    val cyber = LocalCyberColors.current
    val cards = listOf(
        Triple(ThemeMode.LIGHT, "Light", "Clean & Bright"),
        Triple(ThemeMode.DARK, "Dark", "Smooth & Focused"),
        Triple(ThemeMode.SYSTEM, "System", "Follow OS")
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        cards.forEach { (mode, title, subtitle) ->
            val selected = selectedMode == mode
            val previewBg = when (mode) {
                ThemeMode.LIGHT -> Color(0xFFF3F5FA)
                ThemeMode.DARK -> Color(0xFF0A0D13)
                ThemeMode.SYSTEM -> Color(0xFF1A1E28)
            }
            val previewSurface = when (mode) {
                ThemeMode.LIGHT -> Color(0xFFFFFFFF)
                ThemeMode.DARK -> Color(0xFF151A24)
                ThemeMode.SYSTEM -> Color(0xFF242B38)
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .clickable { onSelectMode(mode) }
                    .testTag("theme_mode_${mode.id}"),
                shape = RoundedCornerShape(12.dp),
                color = if (selected) cyber.accent.copy(alpha = 0.14f) else cyber.panelElevated,
                border = BorderStroke(
                    width = if (selected) 2.dp else 1.dp,
                    color = if (selected) cyber.accent else MaterialTheme.colorScheme.outlineVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(previewBg)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                            .padding(6.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(24.dp)
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(cyber.accent)
                                )
                                Icon(
                                    imageVector = when (mode) {
                                        ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                        ThemeMode.DARK -> Icons.Rounded.DarkMode
                                        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                    },
                                    contentDescription = null,
                                    tint = if (mode == ThemeMode.LIGHT) Color(0xFF202530) else Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(14.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(previewSurface)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.7f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(previewSurface)
                            )
                        }
                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "$title theme active",
                                tint = cyber.accent,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(16.dp)
                            )
                        }
                    }

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) cyber.accentSoft else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
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
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            )
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = cyber.accent
                )
            )
        }
    }
}

@Composable
internal fun CircularAccentSwatch(
    color: Color,
    selected: Boolean,
    name: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 3.dp else 1.dp,
                color = if (selected) Color.White else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .testTag("accent_swatch_${name.lowercase()}")
    )
}
