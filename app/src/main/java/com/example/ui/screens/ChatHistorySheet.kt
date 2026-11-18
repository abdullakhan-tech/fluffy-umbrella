package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.remote.FirestoreChatSession
import com.example.ui.components.CyberActionButton
import com.example.ui.theme.LocalCyberColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatHistorySheet(
    sessions: List<FirestoreChatSession>,
    activeSessionId: String,
    onSelectSession: (String) -> Unit,
    onCreateSession: (String) -> Unit,
    onRenameSession: (String, String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val cyber = LocalCyberColors.current
    var renameTarget by remember { mutableStateOf<FirestoreChatSession?>(null) }
    var renameText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cyber.panelGlass,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("chat_history_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "CLOUD CONVERSATION THREADS",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                    Text(
                        text = "Chat History (${sessions.size})",
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                CyberActionButton(
                    text = "NEW THREAD",
                    onClick = {
                        onCreateSession("New Nexus Thread")
                        onDismiss()
                    },
                    testTag = "sheet_new_thread_button"
                )
            }

            if (sessions.isEmpty()) {
                Text(
                    text = "No saved threads yet. Start chatting to create one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        val isActive = session.id == activeSessionId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable {
                                    onSelectSession(session.id)
                                    onDismiss()
                                }
                                .testTag("session_item_${session.id}"),
                            shape = MaterialTheme.shapes.small,
                            color = if (isActive) cyber.accent.copy(alpha = 0.16f) else cyber.panelElevated,
                            border = BorderStroke(
                                width = if (isActive) 1.5.dp else 1.dp,
                                color = if (isActive) cyber.accent else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (isActive) "${session.title} · ACTIVE" else session.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (isActive) cyber.accentSoft else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = session.lastMessagePreview.ifBlank { "Empty thread · ${session.agent}" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${session.messageCount} messages · ${session.agent.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cyber.cyberGreen
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = {
                                            renameTarget = session
                                            renameText = session.title
                                        },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename thread",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteSession(session.id) },
                                        modifier = Modifier.minimumInteractiveComponentSize()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete thread",
                                            tint = cyber.accentSoft
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }

    renameTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Thread") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { if (it.length <= 120) renameText = it },
                    label = { Text("Thread Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onRenameSession(target.id, renameText)
                        renameTarget = null
                    }
                ) {
                    Text("SAVE")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("CANCEL")
                }
            }
        )
    }
}

@Composable
fun ClearSessionConfirmationDialog(
    sessionTitle: String,
    messageCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val cyber = LocalCyberColors.current
    val countSummary = if (messageCount > 0) {
        "$messageCount message${if (messageCount == 1) "" else "s"} in \"$sessionTitle\""
    } else {
        "\"$sessionTitle\""
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("clear_session_confirm_dialog"),
        containerColor = cyber.panelGlass,
        icon = {
            Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Clear Chat & Start New Session?",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "This will permanently delete $countSummary from Cloud Firestore and start a fresh conversation session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_clear_new_session_button")
            ) {
                Text("DELETE & START NEW")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("cancel_clear_new_session_button")
            ) {
                Text("CANCEL")
            }
        }
    )
}
