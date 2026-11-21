package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.domain.AudioTranscriptionResult
import com.example.domain.LiveTranscriptLine
import com.example.domain.LiveVoiceStatus
import com.example.domain.UiState
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.theme.LocalCyberColors
import java.util.Locale

private val liveVoices = listOf("Aoede", "Kore", "Puck", "Fenrir")

@Composable
fun VoiceAndAudioScreen(
    liveStatus: LiveVoiceStatus,
    liveTranscripts: List<LiveTranscriptLine>,
    liveError: String?,
    isRecordingAudio: Boolean,
    recordingSeconds: Long,
    transcribeState: UiState<AudioTranscriptionResult>,
    onStartLiveVoice: (String) -> Unit,
    onStopLiveVoice: () -> Unit,
    onSendLiveText: (String) -> Unit,
    onClearLiveTranscripts: () -> Unit,
    onStartRecording: (Context) -> Unit,
    onStopRecordingAndTranscribe: () -> Unit,
    onSendTranscriptToChat: (String) -> Unit,
    onSaveTranscriptToRag: (String) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current
    var selectedVoice by remember { mutableStateOf("Aoede") }
    var pendingAction by remember { mutableStateOf<String?>(null) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            onToast("Microphone permission is required for Live Voice & Audio Transcription.")
            pendingAction = null
            return@rememberLauncherForActivityResult
        }
        when (pendingAction) {
            "live" -> onStartLiveVoice(selectedVoice)
            "transcribe" -> onStartRecording(context)
        }
        pendingAction = null
    }

    fun requestMicAndRun(action: String, block: () -> Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            block()
        } else {
            pendingAction = action
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "BIDIRECTIONAL AUDIO & SPEECH AI",
                titlePrefix = "Voice &",
                titleAccent = "Audio Nexus",
                trailing = {
                    Text(
                        text = liveStatus.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (liveStatus == LiveVoiceStatus.ACTIVE) cyber.cyberGreen else cyber.accentSoft
                    )
                }
            )
        }

        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val wide = maxWidth >= 820.dp
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        LiveVoiceCard(
                            modifier = Modifier.weight(1f),
                            liveStatus = liveStatus,
                            liveTranscripts = liveTranscripts,
                            liveError = liveError,
                            selectedVoice = selectedVoice,
                            onSelectVoice = { selectedVoice = it },
                            onToggleLiveVoice = {
                                if (liveStatus == LiveVoiceStatus.ACTIVE || liveStatus == LiveVoiceStatus.CONNECTING) {
                                    onStopLiveVoice()
                                } else {
                                    requestMicAndRun("live") { onStartLiveVoice(selectedVoice) }
                                }
                            },
                            onSendLiveText = onSendLiveText,
                            onClearTranscripts = onClearLiveTranscripts,
                            onSaveTranscriptToRag = onSaveTranscriptToRag
                        )
                        AudioTranscribeCard(
                            modifier = Modifier.weight(1f),
                            isRecordingAudio = isRecordingAudio,
                            recordingSeconds = recordingSeconds,
                            transcribeState = transcribeState,
                            onToggleRecording = {
                                if (isRecordingAudio) {
                                    onStopRecordingAndTranscribe()
                                } else {
                                    requestMicAndRun("transcribe") { onStartRecording(context) }
                                }
                            },
                            onSendToChat = onSendTranscriptToChat,
                            onSaveToRag = onSaveTranscriptToRag,
                            onToast = onToast
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        LiveVoiceCard(
                            modifier = Modifier.fillMaxWidth(),
                            liveStatus = liveStatus,
                            liveTranscripts = liveTranscripts,
                            liveError = liveError,
                            selectedVoice = selectedVoice,
                            onSelectVoice = { selectedVoice = it },
                            onToggleLiveVoice = {
                                if (liveStatus == LiveVoiceStatus.ACTIVE || liveStatus == LiveVoiceStatus.CONNECTING) {
                                    onStopLiveVoice()
                                } else {
                                    requestMicAndRun("live") { onStartLiveVoice(selectedVoice) }
                                }
                            },
                            onSendLiveText = onSendLiveText,
                            onClearTranscripts = onClearLiveTranscripts,
                            onSaveTranscriptToRag = onSaveTranscriptToRag
                        )
                        AudioTranscribeCard(
                            modifier = Modifier.fillMaxWidth(),
                            isRecordingAudio = isRecordingAudio,
                            recordingSeconds = recordingSeconds,
                            transcribeState = transcribeState,
                            onToggleRecording = {
                                if (isRecordingAudio) {
                                    onStopRecordingAndTranscribe()
                                } else {
                                    requestMicAndRun("transcribe") { onStartRecording(context) }
                                }
                            },
                            onSendToChat = onSendTranscriptToChat,
                            onSaveToRag = onSaveTranscriptToRag,
                            onToast = onToast
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LiveVoiceCard(
    modifier: Modifier = Modifier,
    liveStatus: LiveVoiceStatus,
    liveTranscripts: List<LiveTranscriptLine>,
    liveError: String?,
    selectedVoice: String,
    onSelectVoice: (String) -> Unit,
    onToggleLiveVoice: () -> Unit,
    onSendLiveText: (String) -> Unit,
    onClearTranscripts: () -> Unit,
    onSaveTranscriptToRag: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    var liveTextInput by remember { mutableStateOf("") }
    val isLive = liveStatus == LiveVoiceStatus.ACTIVE || liveStatus == LiveVoiceStatus.CONNECTING

    CyberCard(modifier = modifier, highlighted = isLive) {
        CyberCardHeader(
            title = "GEMINI 3.8 LIVE VOICE",
            badge = if (liveStatus == LiveVoiceStatus.ACTIVE) "● AUDIO ACTIVE" else "gemini-3.8-live",
            badgeColor = if (liveStatus == LiveVoiceStatus.ACTIVE) cyber.cyberGreen else cyber.accentSoft,
            trailing = {
                if (liveTranscripts.isNotEmpty()) {
                    IconButton(
                        onClick = onClearTranscripts,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_live_transcript_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = "Clear live voice transcript",
                            tint = cyber.accentSoft
                        )
                    }
                }
            }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CyberIconBadge(
                icon = Icons.Rounded.RecordVoiceOver,
                contentDescription = "Gemini Live Voice Conversation",
                sizeDp = 50.dp,
                iconSizeDp = 26.dp,
                isActive = isLive,
                tint = if (isLive) cyber.cyberGreen else cyber.accentSoft
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Real-Time Voice Conversations", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Two-way streaming speech with natural interruptions & live captions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            liveVoices.forEach { voice ->
                val selected = voice == selectedVoice
                Surface(
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable(enabled = !isLive) { onSelectVoice(voice) }
                        .testTag("live_voice_chip_${voice.lowercase(Locale.ROOT)}"),
                    shape = MaterialTheme.shapes.small,
                    color = if (selected) cyber.accent.copy(alpha = 0.2f) else cyber.panelElevated,
                    border = BorderStroke(1.dp, if (selected) cyber.accent else MaterialTheme.colorScheme.outline)
                ) {
                    Text(
                        text = voice.uppercase(Locale.ROOT),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) cyber.accentSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        CyberActionButton(
            text = when (liveStatus) {
                LiveVoiceStatus.CONNECTING -> "CONNECTING LIVE SESSION…"
                LiveVoiceStatus.ACTIVE -> "END LIVE VOICE CONVERSATION"
                LiveVoiceStatus.STOPPING -> "CLOSING LIVE AUDIO…"
                else -> "START LIVE VOICE ($selectedVoice)"
            },
            onClick = onToggleLiveVoice,
            enabled = liveStatus != LiveVoiceStatus.STOPPING,
            modifier = Modifier.fillMaxWidth(),
            testTag = "live_voice_toggle_button"
        )

        if (!liveError.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = liveError,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("live_voice_transcript_list"),
            shape = MaterialTheme.shapes.small,
            color = cyber.panelElevated,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (liveTranscripts.isEmpty()) {
                    Text(
                        text = "Live voice captions appear here once connected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    liveTranscripts.takeLast(12).forEach { line ->
                        val speakerColor = when (line.speaker) {
                            "YOU" -> cyber.accentSoft
                            "ZAYTHAR" -> cyber.cyberGreen
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = "${line.speaker}: ${line.text}",
                            style = MaterialTheme.typography.bodySmall,
                            color = speakerColor
                        )
                    }
                }
            }
        }

        if (isLive) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = liveTextInput,
                    onValueChange = { if (it.length <= 500) liveTextInput = it },
                    label = { Text("TYPE IN LIVE SESSION") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("live_text_turn_input")
                )
                IconButton(
                    onClick = {
                        onSendLiveText(liveTextInput)
                        liveTextInput = ""
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("send_live_text_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Send text to live voice session",
                        tint = cyber.cyberGreen
                    )
                }
            }
        }

        if (liveTranscripts.any { it.speaker == "YOU" || it.speaker == "ZAYTHAR" }) {
            Spacer(modifier = Modifier.height(10.dp))
            CyberSecondaryButton(
                text = "SAVE LIVE TRANSCRIPT TO RAG",
                onClick = {
                    val text = liveTranscripts
                        .filter { it.speaker != "SYSTEM" }
                        .joinToString("\n") { "${it.speaker}: ${it.text}" }
                    onSaveTranscriptToRag("[LIVE VOICE SESSION]\n$text")
                },
                modifier = Modifier.fillMaxWidth(),
                testTag = "save_live_transcript_button"
            )
        }
    }
}
