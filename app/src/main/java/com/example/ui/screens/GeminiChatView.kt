package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.example.domain.ChatMessage
import com.example.domain.GroundedWebSource
import com.example.ui.components.CyberActionButton
import com.example.ui.theme.LocalCyberColors

@Composable
fun ColumnScope.GeminiChatView(
    messages: List<ChatMessage>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier,
    isGenerating: Boolean = false,
    streamingReply: String? = null,
    streamingSearchQueries: List<String> = emptyList(),
    streamingWebSources: List<GroundedWebSource> = emptyList(),
    isInitialLoading: Boolean = false,
    operatorName: String = "OPERATOR 01",
    placeholderText: String = "Message Gemini…",
    emptyStateText: String = "Thread ready.\nSend a message to start chatting with Gemini.",
    loadingPromptText: String = "PROCESSING REQUEST…",
    listState: LazyListState = rememberLazyListState(),
    onToast: (String) -> Unit = {},
    composerControls: (@Composable RowScope.() -> Unit)? = null
) {
    val cyber = LocalCyberColors.current

    Box(
        modifier = modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        when {
            isInitialLoading && messages.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Syncing cloud thread…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
            messages.isEmpty() && !isGenerating -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = cyber.accentSoft,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = emptyStateText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("gemini_chat_empty_state")
                    )
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("gemini_chat_lazy_column"),
                    contentPadding = PaddingValues(vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageBubble(
                            msg = msg,
                            operatorName = operatorName,
                            onToast = onToast
                        )
                    }

                    if (isGenerating || !streamingReply.isNullOrBlank()) {
                        item(key = "streaming_assistant_reply") {
                            StreamingChatBubble(
                                liveText = streamingReply?.takeIf { it.isNotBlank() } ?: loadingPromptText,
                                searchQueries = streamingSearchQueries,
                                webSources = streamingWebSources
                            )
                        }
                    }
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(10.dp))

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF7D858F).copy(alpha = 0.55f), RoundedCornerShape(24.dp))
            .background(Color(0xFF101216), RoundedCornerShape(24.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { if (it.length <= 12_000) onInputChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chat_prompt_input"),
            placeholder = {
                Text(
                    text = placeholderText,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            maxLines = 4,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (!isGenerating && inputText.isNotBlank()) {
                        onSendMessage(inputText)
                    }
                }
            ),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF2638),
                unfocusedBorderColor = Color(0xFF555B65),
                focusedContainerColor = Color(0xFF191C22),
                unfocusedContainerColor = Color(0xFF191C22),
                cursorColor = Color(0xFFFF2638)
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (composerControls != null) Arrangement.SpaceBetween else Arrangement.End
        ) {
            if (composerControls != null) {
                composerControls()
            }
            CyberActionButton(
                text = if (isGenerating) "…" else "SEND",
                onClick = { onSendMessage(inputText) },
                enabled = !isGenerating && inputText.isNotBlank(),
                testTag = "send_chat_button"
            )
        }
    }
}
