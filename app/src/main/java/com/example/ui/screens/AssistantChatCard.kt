package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PsychologyAlt
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.SwapHoriz
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreSessionMessage
import com.example.domain.AgentRole
import com.example.domain.SupportedLanguage
import com.example.domain.UiState
import com.example.domain.toChatMessage
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    onClearAndNewSession: () -> Unit = onClearChat,
    onOpenHistory: () -> Unit,
    onNewSession: () -> Unit,
    onOpenPlugins: () -> Unit,
    onOpenCameraEyes: () -> Unit,
    onOpenVoiceAudio: () -> Unit = {},
    onToggleWebConnect: (Boolean) -> Unit,
    onToggleMapsGrounding: (Boolean) -> Unit = {},
    isSpeaking: Boolean = false,
    onStopSpeaking: () -> Unit = {},
    onSelectAgent: (AgentRole) -> Unit,
    onSelectLanguage: (SupportedLanguage) -> Unit,
    onToggleSpeakReplies: (Boolean) -> Unit,
    onMicClick: () -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    thoughtTraces: Map<String, String> = emptyMap(),
    attachedImage: Bitmap? = null,
    onAttachImage: (Bitmap?) -> Unit = {},
    onToggleThinking: (Boolean) -> Unit = {},
    onSpeakMessage: ((String) -> Unit)? = null,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cyber = LocalCyberColors.current
    val currentLanguage = SupportedLanguage.fromCode(prefs.languageCode)
    val rawMessages = (messagesState as? UiState.Success)?.data ?: emptyList()
    val chatMessages = remember(rawMessages, thoughtTraces) {
        rawMessages.map { raw ->
            val mapped = raw.toChatMessage()
            val trace = thoughtTraces[mapped.id].orEmpty()
            if (trace.isNotBlank()) mapped.copy(thoughtSummary = trace) else mapped
        }
    }
    val streamingState = chatUiState as? UiState.Streaming
    val streamingReply = streamingState?.partialText
    val streamingThoughts = streamingState?.partialThoughts.orEmpty()
    var showClearSessionDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = withContext(Dispatchers.IO) { decodeSampledChatBitmap(context, uri) }
                if (bmp != null) {
                    onAttachImage(bmp)
                } else {
                    onToast("Could not load selected image.")
                }
            }
        }
    }

    CyberCard(modifier = modifier) {
        CyberCardHeader(
            title = sessionTitle.uppercase(),
            badge = when {
                prefs.webConnectEnabled && prefs.mapsGroundingEnabled -> "SEARCH+MAPS"
                prefs.mapsGroundingEnabled -> "MAPS+CLOUD"
                prefs.webConnectEnabled -> "GOOGLE SEARCH+CLOUD"
                else -> "CLOUD SYNC"
            },
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
                    .clickable { onToggleThinking(!prefs.thinkingEnabled) }
                    .testTag("thinking_toggle_chip"),
                shape = MaterialTheme.shapes.small,
                color = if (prefs.thinkingEnabled) cyber.cyberGreen.copy(alpha = 0.16f) else cyber.panelElevated,
                border = BorderStroke(
                    1.dp,
                    if (prefs.thinkingEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PsychologyAlt,
                        contentDescription = "Toggle AI Thinking Mode",
                        tint = if (prefs.thinkingEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (prefs.thinkingEnabled) "THINK: ON" else "THINK: OFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (prefs.thinkingEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier.minimumInteractiveComponentSize().clickable { onToggleMapsGrounding(!prefs.mapsGroundingEnabled) }.testTag("maps_grounding_toggle"),
                shape = MaterialTheme.shapes.small,
                color = if (prefs.mapsGroundingEnabled) cyber.cyberGreen.copy(alpha = 0.16f) else cyber.panelElevated,
                border = BorderStroke(1.dp, if (prefs.mapsGroundingEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.outline)
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.LocationOn, "Toggle Google Maps Grounding", tint = if (prefs.mapsGroundingEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                    Text(if (prefs.mapsGroundingEnabled) "MAPS: ON" else "MAPS: OFF", style = MaterialTheme.typography.labelSmall, color = if (prefs.mapsGroundingEnabled) cyber.cyberGreen else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Surface(
                modifier = Modifier.minimumInteractiveComponentSize().clickable(onClick = onOpenVoiceAudio).testTag("assistant_voice_live_chip"),
                shape = MaterialTheme.shapes.small,
                color = cyber.panelElevated,
                border = BorderStroke(1.dp, cyber.cyberGreen.copy(alpha = 0.45f))
            ) {
                Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.GraphicEq, null, tint = cyber.cyberGreen, modifier = Modifier.size(16.dp))
                    Text("VOICE LIVE", style = MaterialTheme.typography.labelSmall, color = cyber.cyberGreen)
                }
            }

            if (isSpeaking) {
                Surface(
                    modifier = Modifier.minimumInteractiveComponentSize().clickable(onClick = onStopSpeaking).testTag("stop_tts_chip"),
                    shape = MaterialTheme.shapes.small,
                    color = cyber.accent.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, cyber.accent)
                ) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Rounded.StopCircle, "Stop speaking", tint = cyber.accentSoft, modifier = Modifier.size(16.dp))
                        Text("STOP TTS", style = MaterialTheme.typography.labelSmall, color = cyber.accentSoft)
                    }
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

            AssistantThemeDropdownChip(
                themeMode = prefs.themeMode,
                onSelectThemeMode = onSelectThemeMode
            )
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
            streamingThoughts = streamingThoughts,
            attachedImage = attachedImage,
            onClearAttachedImage = { onAttachImage(null) },
            onSpeakMessage = onSpeakMessage,
            isInitialLoading = messagesState is UiState.Loading,
            operatorName = prefs.operatorName,
            placeholderText = if (prefs.webConnectEnabled) {
                "Ask ${activeAgent.title} anything (Urdu, Roman Urdu, English) or attach a photo…"
            } else {
                "Talk or ask ${activeAgent.title} anything…"
            },
            emptyStateText = "Zaythar AI is ready.\nAsk any question, share what's on your mind, attach a photo, or say \"Mera mood off hai, guide karo\".",
            loadingPromptText = when {
                prefs.thinkingEnabled && prefs.webConnectEnabled -> "THINKING, SEARCHING & STREAMING…"
                prefs.thinkingEnabled -> "THINKING DEEPLY…"
                prefs.webConnectEnabled -> "SEARCHING GOOGLE & STREAMING…"
                else -> "PROCESSING REQUEST…"
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
                    onMicClick = onMicClick,
                    hasAttachedImage = attachedImage != null,
                    onPickImageClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
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
