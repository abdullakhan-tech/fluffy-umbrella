package com.example.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PsychologyAlt
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalCyberColors

@Composable
internal fun ThinkingTracePanel(
    thoughtText: String,
    isLive: Boolean,
    messageId: String = "live",
    modifier: Modifier = Modifier
) {
    if (thoughtText.isBlank()) return
    val cyber = LocalCyberColors.current
    var expanded by remember(messageId, isLive) { mutableStateOf(isLive) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("thinking_trace_panel_$messageId"),
        shape = MaterialTheme.shapes.small,
        color = cyber.panelGlass,
        border = BorderStroke(
            1.dp,
            if (isLive) cyber.cyberGreen.copy(alpha = 0.7f) else cyber.accent.copy(alpha = 0.35f)
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable { expanded = !expanded }
                    .testTag("thinking_trace_toggle_$messageId"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PsychologyAlt,
                        contentDescription = "AI thinking process",
                        tint = if (isLive) cyber.cyberGreen else cyber.accentSoft,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isLive) "THINKING PROCESS · LIVE" else "ZAYTHAR THINKING PROCESS",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLive) cyber.cyberGreen else cyber.accentSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = if (expanded) "Hide thinking steps" else "Show thinking steps",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (expanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = thoughtText.trim(),
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .testTag("thinking_trace_body_$messageId")
                )
            }
        }
    }
}

@Composable
internal fun AttachedChatImagePreview(
    bitmap: Bitmap,
    onClear: () -> Unit
) {
    val cyber = LocalCyberColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("attached_chat_image_preview"),
        shape = MaterialTheme.shapes.small,
        color = cyber.panelElevated,
        border = BorderStroke(1.dp, cyber.cyberGreen.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Attached photo preview",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PHOTO ATTACHED",
                    style = MaterialTheme.typography.labelSmall,
                    color = cyber.cyberGreen
                )
                Text(
                    text = "Ask a question about this image or tap SEND",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = onClear,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("clear_attached_image_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Remove attached photo",
                    tint = cyber.accentSoft
                )
            }
        }
    }
}

internal fun decodeSampledChatBitmap(context: Context, uri: Uri, maxDim: Int = 1024): Bitmap? =
    runCatching {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOpts)
        }
        val maxSide = maxOf(boundsOpts.outWidth, boundsOpts.outHeight)
        var sampleSize = 1
        if (maxSide > maxDim) {
            while (maxSide / sampleSize > maxDim) {
                sampleSize *= 2
            }
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOpts)
        }
    }.getOrNull()

@Composable
internal fun AssistantThemeDropdownChip(
    themeMode: com.example.ui.theme.ThemeMode,
    onSelectThemeMode: (com.example.ui.theme.ThemeMode) -> Unit
) {
    val cyber = LocalCyberColors.current
    var themeMenuExpanded by remember { mutableStateOf(false) }
    Box {
        Surface(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .clickable { themeMenuExpanded = true }
                .testTag("chat_theme_chip"),
            shape = MaterialTheme.shapes.small,
            color = cyber.panelElevated,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = when (themeMode) {
                        com.example.ui.theme.ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                        com.example.ui.theme.ThemeMode.LIGHT -> Icons.Rounded.LightMode
                        com.example.ui.theme.ThemeMode.DARK -> Icons.Rounded.DarkMode
                    },
                    contentDescription = "Select chat theme",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = when (themeMode) {
                        com.example.ui.theme.ThemeMode.SYSTEM -> "THEME: AUTO"
                        com.example.ui.theme.ThemeMode.LIGHT -> "THEME: LIGHT"
                        com.example.ui.theme.ThemeMode.DARK -> "THEME: DARK"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        DropdownMenu(
            expanded = themeMenuExpanded,
            onDismissRequest = { themeMenuExpanded = false }
        ) {
            com.example.ui.theme.ThemeMode.entries.forEach { mode ->
                DropdownMenuItem(
                    text = { Text(mode.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (mode) {
                                com.example.ui.theme.ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                com.example.ui.theme.ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                com.example.ui.theme.ThemeMode.DARK -> Icons.Rounded.DarkMode
                            },
                            contentDescription = null
                        )
                    },
                    trailingIcon = if (themeMode == mode) {
                        {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Selected",
                                tint = cyber.cyberGreen
                            )
                        }
                    } else null,
                    modifier = Modifier.testTag("chat_theme_option_${mode.id}"),
                    onClick = {
                        onSelectThemeMode(mode)
                        themeMenuExpanded = false
                    }
                )
            }
        }
    }
}
