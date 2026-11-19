package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreSessionMessage
import com.example.domain.AgentRole
import com.example.domain.SupportedLanguage
import com.example.domain.UiState
import com.example.domain.toChatMessage
import com.example.ui.components.AnimationStyle
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.ThemeMode

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
    styleId: String,
    onStyleSelected: (String) -> Unit,
    isBusy: Boolean,
    isListening: Boolean,
    langMenuExpanded: Boolean,
    agentMenuExpanded: Boolean,
    onLangMenuToggle: (Boolean) -> Unit,
    onAgentMenuToggle: (Boolean) -> Unit,
    onComposerChange: (String) -> Unit,
    onSend: () -> Unit,
    onClearChat: () -> Unit,
    onClearAndNewSession: () -> Unit = onClearChat,
    onOpenHistory: () -> Unit,
    onNewSession: () -> Unit,
    onOpenPlugins: () -> Unit,
    onOpenCameraEyes: () -> Unit,
    onToggleWebConnect: (Boolean) -> Unit,
    onSelectAgent: (AgentRole) -> Unit,
    onSelectLanguage: (SupportedLanguage) -> Unit,
    onToggleSpeakReplies: (Boolean) -> Unit,
    onMicClick: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    onToast: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    val currentLanguage = SupportedLanguage.fromCode(prefs.languageCode)
    val rawMessages = (messagesState as? UiState.Success)?.data ?: emptyList()
    val chatMessages = remember(rawMessages) { rawMessages.map { it.toChatMessage() } }
    val streamingState = chatUiState as? UiState.Streaming
    val streamingReply = streamingState?.partialText
    var themeMenuExpanded by remember { mutableStateOf(false) }
    var showClearSessionDialog by remember { mutableStateOf(false) }

    CyberCard(modifier = modifier) {
        CyberCardHeader(
            title = sessionTitle.uppercase(),
            badge = if (prefs.webConnectEnabled) "GOOGLE SEARCH+CLOUD" else "CLOUD SYNC",
            trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onOpenHistory,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_chat_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.History,
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
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "New chat thread",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { showClearSessionDialog = true },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear chat history and start new session",
                            tint = cyber.accentSoft
                        )
                    }
                }
            }
        )

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
                    .clickable(onClick = onOpenHistory)
                    .testTag("switch_thread_chip"),
                shape = MaterialTheme.shapes.small,
                color = cyber.panelElevated,
                border = BorderStroke(1.dp, cyber.cyberGreen.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapHoriz,
                        contentDescription = "Switch chat thread",
                        tint = cyber.cyberGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "THREADS",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .clickable { showClearSessionDialog = true }
                    .testTag("clear_new_session_button"),
                shape = MaterialTheme.shapes.small,
                color = cyber.panelElevated,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RestartAlt,
                        contentDescription = "Clear current chat history and start new session",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "CLEAR & NEW",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

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
                        imageVector = Icons.Rounded.Language,
                        contentDescription = "Toggle Google Search Grounding",
                        tint = if (prefs.webConnectEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (prefs.webConnectEnabled) "SEARCH: ON" else "SEARCH: OFF",
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
                        imageVector = Icons.Rounded.Extension,
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
                        imageVector = Icons.Rounded.CameraAlt,
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
                            imageVector = when (prefs.themeMode) {
                                ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                ThemeMode.DARK -> Icons.Rounded.DarkMode
                            },
                            contentDescription = "Select chat theme",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = when (prefs.themeMode) {
                                ThemeMode.SYSTEM -> "THEME: AUTO"
                                ThemeMode.LIGHT -> "THEME: LIGHT"
                                ThemeMode.DARK -> "THEME: DARK"
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
                    ThemeMode.entries.forEach { mode ->
                        DropdownMenuItem(
                            text = { Text(mode.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (mode) {
                                        ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                                        ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                        ThemeMode.DARK -> Icons.Rounded.DarkMode
                                    },
                                    contentDescription = null
                                )
                            },
                            trailingIcon = if (prefs.themeMode == mode) {
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

        Text(
            text = "VISUAL MODE · ${AnimationStyle.fromId(styleId).title.uppercase()}",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFFF4655),
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
            AnimationStyle.entries.forEach { style ->
                FilterChip(
                    selected = styleId == style.id,
                    onClick = { onStyleSelected(style.id) },
                    label = { Text(style.title, maxLines = 1) }
                )
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

        GeminiChatView(
            messages = chatMessages,
            inputText = composerText,
            onInputChange = onComposerChange,
            onSendMessage = { onSend() },
            isGenerating = isBusy,
            streamingReply = streamingReply,
            streamingSearchQueries = streamingState?.searchQueries.orEmpty(),
            streamingWebSources = streamingState?.webSources.orEmpty(),
            isInitialLoading = messagesState is UiState.Loading,
            operatorName = prefs.operatorName,
            placeholderText = if (prefs.webConnectEnabled) {
                "Ask ${activeAgent.title} for live Google Search answers or paste an https:// URL…"
            } else {
                "Message ${activeAgent.title}…"
            },
            emptyStateText = "Thread ready.\nAsk up-to-date questions with Google Search Grounding, or try \"120 km to mi\".",
            loadingPromptText = if (prefs.webConnectEnabled) {
                "SEARCHING GOOGLE & STREAMING…"
            } else {
                "PROCESSING REQUEST…"
            },
            listState = listState,
            onToast = onToast,
            composerControls = {
                ChatComposerControls(
                    activeAgent = activeAgent,
                    currentLanguage = currentLanguage,
                    prefs = prefs,
                    isListening = isListening,
                    langMenuExpanded = langMenuExpanded,
                    agentMenuExpanded = agentMenuExpanded,
                    onLangMenuToggle = onLangMenuToggle,
                    onAgentMenuToggle = onAgentMenuToggle,
                    onSelectAgent = onSelectAgent,
                    onSelectLanguage = onSelectLanguage,
                    onToggleSpeakReplies = onToggleSpeakReplies,
                    onMicClick = onMicClick
                )
            }
        )
    }

    if (showClearSessionDialog) {
        ClearSessionConfirmationDialog(
            sessionTitle = sessionTitle,
            messageCount = chatMessages.size,
            onConfirm = {
                showClearSessionDialog = false
                onClearAndNewSession()
            },
            onDismiss = { showClearSessionDialog = false }
        )
    }
}
