package com.example.ui.screens

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.data.local.ZaytharPreferences
import com.example.domain.AgentRole
import com.example.domain.ChatMessage
import com.example.domain.GroundedWebSource
import com.example.domain.SupportedLanguage
import com.example.ui.theme.LocalCyberColors

internal fun copyMessageToClipboard(
    context: Context,
    text: String,
    onToast: (String) -> Unit
) {
    if (text.isBlank()) return
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    if (clipboard != null) {
        clipboard.setPrimaryClip(ClipData.newPlainText("Zaythar Chat Message", text))
        onToast("Message copied to clipboard.")
    } else {
        onToast("Clipboard unavailable on this device.")
    }
}

internal fun createShareMessageIntent(
    text: String,
    chooserTitle: String = "Share chat message"
): Intent {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    return Intent.createChooser(sendIntent, chooserTitle)
}

internal fun shareMessageViaIntent(
    context: Context,
    text: String,
    onToast: (String) -> Unit
) {
    if (text.isBlank()) return
    val chooser = createShareMessageIntent(text).apply {
        if (context !is Activity) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
    runCatching {
        context.startActivity(chooser)
    }.onFailure {
        onToast("No app available to share this message.")
    }
}

@Composable
internal fun ChatMessageBubble(
    msg: ChatMessage,
    operatorName: String,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current
    val isUser = msg.isFromUser
    val stripeColor = if (isUser) Color(0xFFFF2638) else Color(0xFFB8C0CC)
    val bubbleColor = if (isUser) Color(0xFF350B12) else Color(0xFF191C22)
    val timeLabel = msg.formattedTime()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_message_${msg.id}"),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = RoundedCornerShape(
                topStart = 22.dp,
                topEnd = 22.dp,
                bottomEnd = if (isUser) 6.dp else 22.dp,
                bottomStart = if (isUser) 22.dp else 6.dp
            ),
            color = bubbleColor,
            border = BorderStroke(
                1.dp,
                if (isUser) Color(0xFF9E1D2B).copy(alpha = 0.8f) else Color(0xFF69717D).copy(alpha = 0.55f)
            )
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .heightIn(min = 44.dp)
                        .background(stripeColor)
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, top = 6.dp, end = 6.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = buildString {
                                append(if (isUser) operatorName else "ZAYTHAR")
                                if (!isUser && msg.modelLabel.isNotBlank()) {
                                    append(" · ${msg.modelLabel}")
                                }
                                if (msg.hasImage) {
                                    append(" · VISION")
                                }
                                if (timeLabel.isNotBlank()) {
                                    append(" · $timeLabel")
                                }
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = stripeColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { copyMessageToClipboard(context, msg.text, onToast) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_message_${msg.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy message to clipboard",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = { shareMessageViaIntent(context, msg.text, onToast) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("share_message_${msg.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share message",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    SelectionContainer {
                        if (isUser) {
                            Text(
                                text = msg.text,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        } else {
                            AiMarkdownContent(
                                markdown = msg.text,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 6.dp)
                                    .testTag("markdown_content_${msg.id}"),
                                onToast = onToast
                            )
                        }
                    }
                    GoogleSearchGroundingSection(
                        searchQueries = msg.searchQueries,
                        sources = msg.resolvedSources(),
                        searchEntryPointHtml = msg.searchEntryPointHtml,
                        onOpenUrl = { url -> openGroundedExternalUrl(context, url, onToast) }
                    )
                }
            }
        }
    }
}

@Composable
internal fun StreamingChatBubble(
    liveText: String,
    searchQueries: List<String> = emptyList(),
    webSources: List<GroundedWebSource> = emptyList()
) {
    val cyber = LocalCyberColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("streaming_chat_bubble"),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.92f),
            shape = MaterialTheme.shapes.small,
            color = cyber.panelElevated,
            border = BorderStroke(1.dp, cyber.cyberGreen.copy(alpha = 0.6f))
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .heightIn(min = 44.dp)
                        .background(cyber.cyberGreen)
                )
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (searchQueries.isNotEmpty() || webSources.isNotEmpty()) {
                            "ZAYTHAR · GOOGLE SEARCH GROUNDING"
                        } else {
                            "ZAYTHAR · STREAMING"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    AiMarkdownContent(
                        markdown = liveText,
                        modifier = Modifier.fillMaxWidth()
                    )
                    StreamingGroundingIndicator(
                        searchQueries = searchQueries,
                        sources = webSources
                    )
                }
            }
        }
    }
}

@Composable
internal fun ChatComposerControls(
    activeAgent: AgentRole,
    currentLanguage: SupportedLanguage,
    prefs: ZaytharPreferences,
    isListening: Boolean,
    langMenuExpanded: Boolean,
    agentMenuExpanded: Boolean,
    onLangMenuToggle: (Boolean) -> Unit,
    onAgentMenuToggle: (Boolean) -> Unit,
    onSelectAgent: (AgentRole) -> Unit,
    onSelectLanguage: (SupportedLanguage) -> Unit,
    onToggleSpeakReplies: (Boolean) -> Unit,
    onMicClick: () -> Unit
) {
    val cyber = LocalCyberColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable { onLangMenuToggle(true) }
                    .testTag("language_selector"),
                shape = MaterialTheme.shapes.small,
                color = cyber.panelElevated,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Text(
                    text = currentLanguage.displayName.substringBefore(" "),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
            DropdownMenu(
                expanded = langMenuExpanded,
                onDismissRequest = { onLangMenuToggle(false) }
            ) {
                SupportedLanguage.entries.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang.displayName) },
                        onClick = {
                            onSelectLanguage(lang)
                            onLangMenuToggle(false)
                        }
                    )
                }
            }
        }

        Box {
            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable { onAgentMenuToggle(true) }
                    .testTag("agent_selector"),
                shape = MaterialTheme.shapes.small,
                color = cyber.panelElevated,
                border = BorderStroke(1.dp, cyber.accent.copy(alpha = 0.5f))
            ) {
                Text(
                    text = activeAgent.title.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = cyber.accentSoft,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
                )
            }
            DropdownMenu(
                expanded = agentMenuExpanded,
                onDismissRequest = { onAgentMenuToggle(false) }
            ) {
                AgentRole.entries.forEach { role ->
                    DropdownMenuItem(
                        text = { Text("${role.title} — ${role.subtitle}") },
                        onClick = {
                            onSelectAgent(role)
                            onAgentMenuToggle(false)
                        }
                    )
                }
            }
        }

        IconButton(
            onClick = onMicClick,
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .border(
                    1.dp,
                    if (isListening) cyber.cyberGreen else cyber.accent,
                    MaterialTheme.shapes.small
                )
                .testTag("voice_mic_button")
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = if (isListening) "Listening" else "Voice input",
                tint = if (isListening) cyber.cyberGreen else MaterialTheme.colorScheme.onSurface
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
            contentDescription = "Speak replies toggle",
            tint = if (prefs.speakReplies) cyber.cyberGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Switch(
            checked = prefs.speakReplies,
            onCheckedChange = onToggleSpeakReplies,
            modifier = Modifier.testTag("speak_replies_switch"),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = cyber.accent
            )
        )
    }
}
