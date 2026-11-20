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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreChatSession
import com.example.data.remote.FirestoreSessionMessage
import com.example.domain.AgentRole
import com.example.domain.SupportedLanguage
import com.example.domain.UiState
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.NeuralCoreVisualizer
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.ThemeMode

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AssistantScreen(
    prefs: ZaytharPreferences,
    activeAgent: AgentRole,
    composerText: String,
    sessionsState: UiState<List<FirestoreChatSession>>,
    activeSessionId: String,
    activePluginCount: Int,
    messagesState: UiState<List<FirestoreSessionMessage>>,
    chatUiState: UiState<String>,
    isSpeaking: Boolean = false,
    onComposerChange: (String) -> Unit,
    onSend: (String) -> Unit,
    onClearChat: () -> Unit,
    onClearAndNewSession: () -> Unit = onClearChat,
    onSelectSession: (String) -> Unit,
    onCreateSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onOpenPlugins: () -> Unit,
    onOpenCameraEyes: () -> Unit,
    onToggleWebConnect: (Boolean) -> Unit,
    onSelectAgent: (AgentRole) -> Unit,
    onSelectLanguage: (SupportedLanguage) -> Unit,
    onToggleSpeakReplies: (Boolean) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit = {},
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current
    val listState = rememberLazyListState()
    var isListening by remember { mutableStateOf(false) }
    var langMenuExpanded by remember { mutableStateOf(false) }
    var agentMenuExpanded by remember { mutableStateOf(false) }
    var showHistorySheet by remember { mutableStateOf(false) }

    val isBusy = chatUiState is UiState.Loading || chatUiState is UiState.Streaming
    val messages = (messagesState as? UiState.Success)?.data ?: emptyList()
    val sessions = (sessionsState as? UiState.Success)?.data ?: emptyList()
    val activeSessionTitle = sessions.find { it.id == activeSessionId }?.title ?: "Primary Nexus"

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
        onDispose { speechRecognizer?.destroy() }
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
        if (granted) startVoiceListening() else onToast("Microphone permission is needed for voice input.")
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
                    isSpeaking = isSpeaking,
                    onSuggestionClick = { onSend(it) }
                )
                AssistantChatCard(
                    modifier = Modifier.weight(1.35f),
                    sessionTitle = activeSessionTitle,
                    activePluginCount = activePluginCount,
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
                    onClearAndNewSession = onClearAndNewSession,
                    onOpenHistory = { showHistorySheet = true },
                    onNewSession = { onCreateSession("New Nexus Thread") },
                    onOpenPlugins = onOpenPlugins,
                    onOpenCameraEyes = onOpenCameraEyes,
                    onToggleWebConnect = onToggleWebConnect,
                    onSelectAgent = onSelectAgent,
                    onSelectLanguage = onSelectLanguage,
                    onToggleSpeakReplies = onToggleSpeakReplies,
                    onMicClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startVoiceListening()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onSelectThemeMode = onSelectThemeMode,
                    onToast = onToast
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                                size = 58.dp,
                                motionEnabled = prefs.motion3dEnabled,
                                coreLabel = activeAgent.shortCode,
                                activityLevel = when {
                                    isListening -> 1.15f
                                    isBusy -> 0.95f
                                    isSpeaking -> 0.8f
                                    else -> 0.45f
                                }
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = when {
                    isListening -> "PERSONAL INTELLIGENCE / LISTENING"
                    isBusy -> "PERSONAL INTELLIGENCE / THINKING"
                     isSpeaking -> "PERSONAL INTELLIGENCE / SPEAKING"
                    else -> "PERSONAL INTELLIGENCE / READY"
                },
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
                                "Write a Kotlin coroutine",
                                "100 km to mi"
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
                    sessionTitle = activeSessionTitle,
                    activePluginCount = activePluginCount,
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
                    onClearAndNewSession = onClearAndNewSession,
                    onOpenHistory = { showHistorySheet = true },
                    onNewSession = { onCreateSession("New Nexus Thread") },
                    onOpenPlugins = onOpenPlugins,
                    onOpenCameraEyes = onOpenCameraEyes,
                    onToggleWebConnect = onToggleWebConnect,
                    onSelectAgent = onSelectAgent,
                    onSelectLanguage = onSelectLanguage,
                    onToggleSpeakReplies = onToggleSpeakReplies,
                    onMicClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startVoiceListening()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onSelectThemeMode = onSelectThemeMode,
                    onToast = onToast
                )
            }
        }
    }

    if (showHistorySheet) {
        ChatHistorySheet(
            sessions = sessions,
            activeSessionId = activeSessionId,
            onSelectSession = onSelectSession,
            onCreateSession = onCreateSession,
            onRenameSession = onRenameSession,
            onDeleteSession = onDeleteSession,
            onDismiss = { showHistorySheet = false }
        )
    }
}

@Composable
private fun AssistantOrbCard(
    modifier: Modifier = Modifier,
    motionEnabled: Boolean,
    activeAgent: AgentRole,
    isBusy: Boolean,
    isListening: Boolean,
    isSpeaking: Boolean,
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
                text = when {
                    isListening -> "PERSONAL INTELLIGENCE / LISTENING"
                    isBusy -> "PERSONAL INTELLIGENCE / THINKING"
                     isSpeaking -> "PERSONAL INTELLIGENCE / SPEAKING"
                    else -> "PERSONAL INTELLIGENCE / READY"
                },
                style = MaterialTheme.typography.labelSmall,
                color = cyber.cyberGreen
            )
            NeuralCoreVisualizer(
                size = 170.dp,
                motionEnabled = motionEnabled,
                coreLabel = activeAgent.shortCode,
                activityLevel = when {
                    isListening -> 1.15f
                    isBusy -> 0.95f
                    isSpeaking -> 0.8f
                    else -> 0.45f
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
                text = "Multi-thread cloud history, live web search, and plugin-powered reasoning.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            listOf(
                "Explain a complex topic",
                "Write a Kotlin coroutine",
                "100 km to mi"
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
