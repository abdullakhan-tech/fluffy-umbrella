package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreSessionMessage
import com.example.domain.AgentRole
import com.example.domain.SupportedLanguage
import com.example.domain.UiState
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.theme.LocalCyberColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AssistantChatCard(
    modifier: Modifier = Modifier,
    sessionTitle: String,
    activePluginCount: Int,
    messagesState: UiState<List<FirestoreSessionMessage>>,
    chatUiState: UiState<String>,
    listState: LazyListState,
    composerText: String,
    activeAgent: AgentRole,
    prefs: ZaytharPreferences,
    isBusy: Boolean,
    isListening: Boolean,
    langMenuExpanded: Boolean,
    agentMenuExpanded: Boolean,
    onLangMenuToggle: (Boolean) -> Unit,
    onAgentMenuToggle: (Boolean) -> Unit,
    onComposerChange: (String) -> Unit,
    onSend: () -> Unit,
    onClearChat: () -> Unit,
    onOpenHistory: () -> Unit,
    onNewSession: () -> Unit,
    onOpenPlugins: () -> Unit,
    onOpenCameraEyes: () -> Unit,
    onToggleWebConnect: (Boolean) -> Unit,
    onSelectAgent: (AgentRole) -> Unit,
    onSelectLanguage: (SupportedLanguage) -> Unit,
    onToggleSpeakReplies: (Boolean) -> Unit,
    onMicClick: () -> Unit
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current
    val currentLanguage = SupportedLanguage.fromCode(prefs.languageCode)
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()

    CyberCard(modifier = modifier) {
        CyberCardHeader(
            title = sessionTitle.uppercase(),
            badge = if (prefs.webConnectEnabled) "WEB+CLOUD SYNC" else "CLOUD SYNC",
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_chat_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Open chat history threads",
                            tint = cyber.cyberGreen
                        )
                    }
                    IconButton(
                        onClick = onNewSession,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("new_chat_thread_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New chat thread",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = onClearChat,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear thread messages",
                            tint = cyber.accentSoft
                        )
                    }
                }
            }
        )

        // Status & Capability Toggle Strip: Web Connect, Plugins, Camera Eyes
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable { onToggleWebConnect(!prefs.webConnectEnabled) }
                    .testTag("web_connect_toggle"),
                shape = MaterialTheme.shapes.small,
                color = if (prefs.webConnectEnabled) cyber.cyberGreen.copy(alpha = 0.16f) else cyber.panelElevated,
                border = BorderStroke(
                    1.dp,
                    if (prefs.webConnectEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = if (prefs.webConnectEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (prefs.webConnectEnabled) "WEB: ON" else "WEB: OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (prefs.webConnectEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onOpenPlugins)
                    .testTag("assistant_plugins_chip"),
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
                        imageVector = Icons.Default.Extension,
                        contentDescription = null,
                        tint = cyber.accentSoft,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "PLUGINS ($activePluginCount)",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable(onClick = onOpenCameraEyes)
                    .testTag("assistant_camera_eyes_chip"),
                shape = MaterialTheme.shapes.small,
                color = cyber.panelElevated,
                border = BorderStroke(1.dp, cyber.accent.copy(alpha = 0.45f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = cyber.accentSoft,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "CAMERA EYES",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.accentSoft
                    )
                }
            }
        }

        if (messagesState is UiState.Error) {
            Text(
                text = "Sync warning: ${messagesState.message}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Message list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messagesState is UiState.Loading && messages.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Syncing cloud thread…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (messages.isEmpty() && !isBusy) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Thread ready.\nAsk anything, toggle WEB for live internet search, or try \"120 km to mi\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id.ifBlank { "${it.role}_${it.text.hashCode()}" } }) { msg ->
                        val isUser = msg.role == "user"
                        val stripeColor = if (isUser) cyber.accent else cyber.cyberGreen
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(0.92f),
                                shape = MaterialTheme.shapes.small,
                                color = cyber.panelElevated,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .heightIn(min = 44.dp)
                                            .background(stripeColor)
                                    )
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = buildString {
                                                append(if (isUser) prefs.operatorName else "ZAYTHAR")
                                                if (!isUser && msg.modelLabel.isNotBlank()) {
                                                    append(" · ${msg.modelLabel}")
                                                }
                                                if (msg.hasImage) {
                                                    append(" · VISION")
                                                }
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = stripeColor
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (msg.webSources.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                msg.webSources.forEach { url ->
                                                    Surface(
                                                        modifier = Modifier.clickable {
                                                            if (url.startsWith("https://") || url.startsWith("http://")) {
                                                                runCatching {
                                                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                                                }
                                                            }
                                                        },
                                                        shape = MaterialTheme.shapes.extraSmall,
                                                        color = cyber.panelGlass,
                                                        border = BorderStroke(1.dp, cyber.cyberGreen.copy(alpha = 0.45f))
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.OpenInNew,
                                                                contentDescription = null,
                                                                tint = cyber.cyberGreen,
                                                                modifier = Modifier.size(12.dp)
                                                            )
                                                            Text(
                                                                text = Uri.parse(url).host ?: url.take(28),
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = cyber.cyberGreen,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (chatUiState is UiState.Loading || chatUiState is UiState.Streaming) {
                        item(key = "streaming_assistant_reply") {
                            val liveText = if (chatUiState is UiState.Streaming) {
                                chatUiState.partialText
                            } else {
                                if (prefs.webConnectEnabled) "GROUNDING WEB & STREAMING…" else "PROCESSING REQUEST…"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
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
                                                text = "ZAYTHAR · STREAMING",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = cyber.cyberGreen
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = liveText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Composer Box
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = composerText,
                onValueChange = onComposerChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_prompt_input"),
                placeholder = {
                    Text(
                        if (prefs.webConnectEnabled) "Ask ${activeAgent.title} or paste an https:// URL…"
                        else "Message ${activeAgent.title}…",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = cyber.accent,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
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
                            tint = if (isListening) cyber.cyberGreen else Color.White
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    CyberActionButton(
                        text = if (isBusy) "…" else "SEND",
                        onClick = onSend,
                        enabled = !isBusy && composerText.isNotBlank(),
                        testTag = "send_chat_button"
                    )
                }
            }
        }
    }
}
