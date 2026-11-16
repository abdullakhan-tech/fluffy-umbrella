package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreChatMessage
import com.example.domain.AgentRole
import com.example.domain.SupportedLanguage
import com.example.domain.UiState
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.NeuralCoreActivity
import com.example.ui.components.NeuralCoreVisualizer
import com.example.ui.theme.LocalCyberColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(
    prefs: ZaytharPreferences,
    activeAgent: AgentRole,
    composerText: String,
    messagesState: UiState<List<FirestoreChatMessage>>,
    chatUiState: UiState<String>,
    onComposerChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onClearChat: () -> Unit,
    onSelectAgent: (AgentRole) -> Unit,
    onSelectLanguage: (SupportedLanguage) -> Unit,
    onToggleSpeakReplies: (Boolean) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current
    val listState = rememberLazyListState()
    var isListening by remember { mutableStateOf(false) }
    var langMenuExpanded by remember { mutableStateOf(false) }
    var agentMenuExpanded by remember { mutableStateOf(false) }

    val isBusy = chatUiState is UiState.Loading || chatUiState is UiState.Streaming
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            null
        }
    }

    DisposableEffect(speechRecognizer) {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { isListening = false }
            override fun onError(error: Int) {
                isListening = false
                onToast("Voice capture unavailable or cancelled.")
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                val spoken = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!spoken.isNullOrBlank()) {
                    onSend(spoken)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        onDispose {
            speechRecognizer?.destroy()
        }
    }

    fun startVoiceListening() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            onToast("Voice recognition is not supported on this device.")
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, prefs.languageCode)
        }
        isListening = true
        runCatching { recognizer.startListening(intent) }.onFailure {
            isListening = false
            onToast("Could not start voice capture.")
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startVoiceListening()
        } else {
            onToast("Microphone permission is needed for voice input.")
        }
    }

    val streamingPreview = (chatUiState as? UiState.Streaming)?.partialText
    LaunchedEffect(messages.size, streamingPreview?.length, isBusy) {
        val total = messages.size + if (isBusy) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val wide = maxWidth >= 780.dp
        if (wide) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AssistantOrbCard(
                    modifier = Modifier.weight(0.75f),
                    motionEnabled = prefs.motion3dEnabled,
                    activeAgent = activeAgent,
                    isBusy = isBusy,
                    isListening = isListening,
                    onSuggestionClick = { onSend(it) }
                )
                AssistantChatCard(
                    modifier = Modifier.weight(1.35f),
                    messagesState = messagesState,
                    chatUiState = chatUiState,
                    listState = listState,
                    composerText = composerText,
                    activeAgent = activeAgent,
                    prefs = prefs,
                    isBusy = isBusy,
                    isListening = isListening,
                    langMenuExpanded = langMenuExpanded,
                    agentMenuExpanded = agentMenuExpanded,
                    onLangMenuToggle = { langMenuExpanded = it },
                    onAgentMenuToggle = { agentMenuExpanded = it },
                    onComposerChange = onComposerChange,
                    onSend = { onSend(composerText) },
                    onClearChat = onClearChat,
                    onSelectAgent = onSelectAgent,
                    onSelectLanguage = onSelectLanguage,
                    onToggleSpeakReplies = onToggleSpeakReplies,
                    onMicClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startVoiceListening()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Compact top strip with mini orb and quick suggestions
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = cyber.panelGlass,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            NeuralCoreVisualizer(
                                size = 64.dp,
                                motionEnabled = prefs.motion3dEnabled,
                                coreLabel = activeAgent.shortCode,
                                activity = when {
                                    isListening -> NeuralCoreActivity.LISTENING
                                    isBusy -> NeuralCoreActivity.THINKING
                                    else -> NeuralCoreActivity.READY
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PERSONAL INTELLIGENCE / READY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cyber.cyberGreen
                                )
                                Text(
                                    text = buildAnnotatedString {
                                        append("Your AI. ")
                                        withStyle(SpanStyle(color = cyber.accentSoft)) {
                                            append("At your command.")
                                        }
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Explain a complex topic",
                                "Write a code snippet",
                                "Plan my day"
                            ).forEach { suggestion ->
                                Surface(
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clickable(enabled = !isBusy) { onSend(suggestion) },
                                    shape = MaterialTheme.shapes.small,
                                    color = cyber.panelElevated,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = suggestion,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                AssistantChatCard(
                    modifier = Modifier.weight(1f),
                    messagesState = messagesState,
                    chatUiState = chatUiState,
                    listState = listState,
                    composerText = composerText,
                    activeAgent = activeAgent,
                    prefs = prefs,
                    isBusy = isBusy,
                    isListening = isListening,
                    langMenuExpanded = langMenuExpanded,
                    agentMenuExpanded = agentMenuExpanded,
                    onLangMenuToggle = { langMenuExpanded = it },
                    onAgentMenuToggle = { agentMenuExpanded = it },
                    onComposerChange = onComposerChange,
                    onSend = { onSend(composerText) },
                    onClearChat = onClearChat,
                    onSelectAgent = onSelectAgent,
                    onSelectLanguage = onSelectLanguage,
                    onToggleSpeakReplies = onToggleSpeakReplies,
                    onMicClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startVoiceListening()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AssistantOrbCard(
    modifier: Modifier = Modifier,
    motionEnabled: Boolean,
    activeAgent: AgentRole,
    isBusy: Boolean,
    isListening: Boolean,
    onSuggestionClick: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    CyberCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "PERSONAL INTELLIGENCE / READY",
                style = MaterialTheme.typography.labelSmall,
                color = cyber.cyberGreen
            )
            NeuralCoreVisualizer(
                size = 170.dp,
                motionEnabled = motionEnabled,
                coreLabel = activeAgent.shortCode,
                activity = when {
                    isListening -> NeuralCoreActivity.LISTENING
                    isBusy -> NeuralCoreActivity.THINKING
                    else -> NeuralCoreActivity.READY
                }
            )
            Text(
                text = buildAnnotatedString {
                    append("Your AI. ")
                    withStyle(SpanStyle(color = cyber.accentSoft)) {
                        append("At your command.")
                    }
                },
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Talk naturally, ask questions, or give Zaythar a task.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            listOf(
                "Explain a complex topic",
                "Write a code snippet",
                "Plan my day"
            ).forEach { prompt ->
                CyberSecondaryButton(
                    text = prompt,
                    onClick = { onSuggestionClick(prompt) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AssistantChatCard(
    modifier: Modifier = Modifier,
    messagesState: UiState<List<FirestoreChatMessage>>,
    chatUiState: UiState<String>,
    listState: androidx.compose.foundation.lazy.LazyListState,
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
    onSelectAgent: (AgentRole) -> Unit,
    onSelectLanguage: (SupportedLanguage) -> Unit,
    onToggleSpeakReplies: (Boolean) -> Unit,
    onMicClick: () -> Unit
) {
    val cyber = LocalCyberColors.current
    val currentLanguage = SupportedLanguage.fromCode(prefs.languageCode)
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()

    CyberCard(modifier = modifier) {
        CyberCardHeader(
            title = "CONVERSATION LOG",
            badge = "CLOUD FIRESTORE SYNC",
            trailing = {
                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear conversation",
                        tint = cyber.accentSoft
                    )
                }
            }
        )

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
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Syncing cloud conversation log…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else if (messages.isEmpty() && !isBusy) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your conversation will appear here.\nTry asking a question or using \"calculate 24 * 7\".",
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
                                modifier = Modifier.fillMaxWidth(0.90f),
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
                                "PROCESSING REQUEST…"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(0.90f),
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
                    Text("Message ${activeAgent.title}…", style = MaterialTheme.typography.bodyMedium)
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
                    // Language selector
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

                    // Agent selector
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

                    // Voice Mic button
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
