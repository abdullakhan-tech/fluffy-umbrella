package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.AudioTranscriptionResult
import com.example.domain.UiState
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.theme.LocalCyberColors
import java.util.Locale

@Composable
internal fun AudioTranscribeCard(
    modifier: Modifier = Modifier,
    isRecordingAudio: Boolean,
    recordingSeconds: Long,
    transcribeState: UiState<AudioTranscriptionResult>,
    onToggleRecording: () -> Unit,
    onSendToChat: (String) -> Unit,
    onSaveToRag: (String) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current
    val isLoading = transcribeState is UiState.Loading
    val timerLabel = String.format(Locale.US, "%02d:%02d", recordingSeconds / 60, recordingSeconds % 60)

    CyberCard(modifier = modifier, highlighted = isRecordingAudio) {
        CyberCardHeader(
            title = "AUDIO NOTE TRANSCRIBER",
            badge = if (isRecordingAudio) "REC $timerLabel" else "gemini-3.5-transcribe",
            badgeColor = if (isRecordingAudio) cyber.accent else cyber.cyberGreen
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CyberIconBadge(
                icon = if (isRecordingAudio) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                contentDescription = "Record Audio Note",
                sizeDp = 50.dp,
                iconSizeDp = 26.dp,
                isActive = isRecordingAudio,
                tint = if (isRecordingAudio) cyber.accent else cyber.cyberGreen
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Record & Transcribe Speech", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Captures voice notes (AAC/M4A) and transcribes with structured bullet takeaways.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        CyberActionButton(
            text = when {
                isRecordingAudio -> "STOP & TRANSCRIBE ($timerLabel)"
                isLoading -> "TRANSCRIBING AUDIO…"
                else -> "RECORD AUDIO FOR TRANSCRIPTION"
            },
            onClick = onToggleRecording,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
            testTag = "record_audio_transcribe_button"
        )

        when (transcribeState) {
            is UiState.Loading -> {
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = cyber.accent)
            }
            is UiState.Error -> {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = transcribeState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            is UiState.Success -> {
                val res = transcribeState.data
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transcribe_result_card"),
                    shape = MaterialTheme.shapes.small,
                    color = cyber.panelElevated,
                    border = BorderStroke(1.dp, cyber.cyberGreen.copy(alpha = 0.55f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TRANSCRIBED · ${res.modelUsed.uppercase(Locale.ROOT)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = cyber.cyberGreen,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("Zaythar Transcript", res.transcript))
                                    onToast("Transcript copied to clipboard.")
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_transcript_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy transcript",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        AiMarkdownContent(markdown = res.transcript, modifier = Modifier.fillMaxWidth(), onToast = onToast)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CyberSecondaryButton(
                                text = "SEND TO CHAT",
                                onClick = { onSendToChat(res.transcript) },
                                modifier = Modifier.weight(1f),
                                testTag = "send_transcript_chat_button"
                            )
                            CyberSecondaryButton(
                                text = "SAVE TO RAG",
                                onClick = { onSaveToRag("[AUDIO TRANSCRIPT]\n${res.transcript}") },
                                modifier = Modifier.weight(1f),
                                testTag = "save_transcript_rag_button"
                            )
                        }
                    }
                }
            }
            else -> {}
        }
    }
}
