package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CodeCopilotMode
import com.example.domain.CodeLanguage
import com.example.domain.UiState
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.theme.LocalCyberColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CodeCopilotScreen(
    codeCopilotState: UiState<String>,
    onRunCopilot: (String, CodeLanguage, CodeCopilotMode, String) -> Unit,
    onSaveToKnowledge: (String) -> Unit,
    onToast: (String) -> Unit
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current
    var selectedLanguage by remember { mutableStateOf(CodeLanguage.KOTLIN) }
    var selectedMode by remember { mutableStateOf(CodeCopilotMode.GENERATE) }
    var taskPrompt by remember { mutableStateOf("") }
    var sourceCode by remember { mutableStateOf("") }

    val isBusy = codeCopilotState is UiState.Loading || codeCopilotState is UiState.Streaming
    val outputText = when (codeCopilotState) {
        is UiState.Streaming -> codeCopilotState.partialText
        is UiState.Success -> codeCopilotState.data
        else -> ""
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "PRINCIPAL ENGINEERING WORKBENCH",
                titlePrefix = "Code",
                titleAccent = "Copilot",
                trailing = {
                    Text(
                        text = "${selectedLanguage.title.uppercase()} · AI CORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                CyberCardHeader(
                    title = "TARGET STACK & OPERATION",
                    badge = selectedMode.title.uppercase()
                )

                Text(
                    text = "LANGUAGE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CodeLanguage.entries.forEach { lang ->
                        val active = lang == selectedLanguage
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { selectedLanguage = lang }
                                .testTag("code_lang_${lang.extension}"),
                            shape = MaterialTheme.shapes.small,
                            color = if (active) cyber.accent.copy(alpha = 0.2f) else cyber.panelElevated,
                            border = BorderStroke(1.dp, if (active) cyber.accent else MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = lang.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "OPERATION MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CodeCopilotMode.entries.forEach { mode ->
                        val active = mode == selectedMode
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { selectedMode = mode }
                                .testTag("code_mode_${mode.name.lowercase()}"),
                            shape = MaterialTheme.shapes.small,
                            color = if (active) cyber.cyberGreen.copy(alpha = 0.16f) else cyber.panelElevated,
                            border = BorderStroke(1.dp, if (active) cyber.cyberGreen else MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = mode.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) cyber.cyberGreen else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = taskPrompt,
                    onValueChange = { if (it.length <= 4000) taskPrompt = it },
                    label = { Text("ENGINEERING SPECIFICATION") },
                    placeholder = { Text("Describe what to build, debug, refactor, audit, or test…") },
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("code_task_input")
                )

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = sourceCode,
                    onValueChange = { if (it.length <= 10_000) sourceCode = it },
                    label = { Text("SOURCE CODE CONTEXT (OPTIONAL)") },
                    placeholder = { Text("Paste existing code to debug, refactor, audit, or cover with unit tests…") },
                    minLines = 3,
                    maxLines = 7,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("code_source_input")
                )

                Spacer(modifier = Modifier.height(12.dp))
                CyberActionButton(
                    text = if (isBusy) "SYNTHESIZING CODE…" else "EXECUTE ${selectedMode.title.uppercase()}",
                    onClick = { onRunCopilot(taskPrompt, selectedLanguage, selectedMode, sourceCode) },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "execute_copilot_button"
                )
            }
        }

        if (codeCopilotState is UiState.Loading) {
            item {
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Connecting to Gemini Code Core…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cyber.cyberGreen
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = cyber.accent)
                }
            }
        }

        if (codeCopilotState is UiState.Error) {
            item {
                CyberCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Copilot error: ${codeCopilotState.message}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (outputText.isNotBlank()) {
            item {
                CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "● ● ●   Output.${selectedLanguage.extension}",
                            style = MaterialTheme.typography.labelLarge,
                            color = cyber.cyberGreen
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CyberSecondaryButton(
                                text = "SAVE NOTE",
                                onClick = {
                                    onSaveToKnowledge("[CODE COPILOT · ${selectedLanguage.title}]\n${outputText.take(1900)}")
                                }
                            )
                            CyberSecondaryButton(
                                text = "COPY",
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("Zaythar Code Copilot", outputText))
                                    onToast("Generated code copied to clipboard.")
                                },
                                testTag = "copy_code_button"
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090C11), MaterialTheme.shapes.small)
                            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                            .horizontalScroll(rememberScrollState())
                            .padding(14.dp)
                    ) {
                        Text(
                            text = outputText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp,
                            color = Color(0xFFE2C7CF)
                        )
                    }
                }
            }
        }
    }
}
