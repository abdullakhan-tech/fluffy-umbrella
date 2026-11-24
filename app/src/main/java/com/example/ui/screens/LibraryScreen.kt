package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Psychology
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
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreChatSession
import com.example.data.remote.FirestoreKnowledgeFact
import com.example.data.remote.FirestoreMemoryNote
import com.example.domain.AgentRole
import com.example.domain.UiState
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.theme.LocalCyberColors

private enum class LibraryTab(val label: String) {
    CHATS("Chats & Projects"),
    MEMORY("Memory Vault"),
    INSTRUCTIONS("Custom Personas")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LibraryScreen(
    prefs: ZaytharPreferences,
    sessionsState: UiState<List<FirestoreChatSession>>,
    activeSessionId: String,
    notesState: UiState<List<FirestoreMemoryNote>>,
    factsState: UiState<List<FirestoreKnowledgeFact>>,
    onOpenSession: (String) -> Unit,
    onCreateSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    onSelectAgent: (AgentRole) -> Unit,
    onNavigate: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    var selectedTab by remember { mutableStateOf(LibraryTab.CHATS) }
    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val notes = (notesState as? UiState.Success)?.data.orEmpty()
    val facts = (factsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "LIBRARY & PROJECTS VAULT",
                titlePrefix = "Zaythar",
                titleAccent = "Library",
                trailing = {
                    CyberActionButton(
                        text = "+ NEW CHAT",
                        onClick = { onCreateSession("New Project Thread") },
                        testTag = "library_new_thread_button"
                    )
                }
            )
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryTab.entries.forEach { tab ->
                    val active = tab == selectedTab
                    Surface(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clickable { selectedTab = tab }
                            .testTag("library_tab_${tab.name.lowercase()}"),
                        shape = MaterialTheme.shapes.small,
                        color = if (active) cyber.accent.copy(alpha = 0.2f) else cyber.panelElevated,
                        border = BorderStroke(1.dp, if (active) cyber.accent else MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            LibraryTab.CHATS -> {
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        CyberCardHeader(
                            title = "CONVERSATION PROJECTS",
                            badge = "${sessions.size} THREADS",
                            badgeColor = cyber.cyberGreen
                        )
                        Text(
                            text = "Organize multi-turn conversations, code threads, and research topics backed by Cloud Sync.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                items(sessions, key = { it.id }) { session ->
                    val active = session.id == activeSessionId
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clickable { onOpenSession(session.id) }
                            .testTag("library_session_${session.id}"),
                        shape = MaterialTheme.shapes.medium,
                        color = cyber.panelElevated,
                        border = BorderStroke(1.dp, if (active) cyber.accent else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CyberIconBadge(
                                icon = Icons.Rounded.FolderOpen,
                                contentDescription = session.title,
                                sizeDp = 42.dp,
                                iconSizeDp = 20.dp,
                                isActive = active
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = session.title.ifBlank { "Untitled Thread" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${session.messageCount} messages · ${session.agent.uppercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { onDeleteSession(session.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete thread",
                                    tint = cyber.accentSoft
                                )
                            }
                        }
                    }
                }
            }

            LibraryTab.MEMORY -> {
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        CyberCardHeader(
                            title = "HYBRID RAG MEMORY VAULT",
                            badge = "${notes.size} NOTES · ${facts.size} FACTS",
                            badgeColor = cyber.cyberGreen
                        )
                        CyberSecondaryButton(
                            text = "OPEN FULL KNOWLEDGE BASE & RAG SANDBOX",
                            onClick = { onNavigate("knowledge") },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "library_open_knowledge_button"
                        )
                    }
                }
                items(notes, key = { it.id }) { note ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = cyber.panelElevated,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onDeleteNote(note.id) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(Icons.Rounded.Delete, contentDescription = "Delete note", tint = cyber.accentSoft)
                            }
                        }
                    }
                }
            }

            LibraryTab.INSTRUCTIONS -> {
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        CyberCardHeader(
                            title = "ACTIVE PERSONA & SPECIALISTS",
                            badge = prefs.personaStyle.uppercase(),
                            badgeColor = cyber.accentSoft
                        )
                        Text(
                            text = "Empathy: ${prefs.empathyRatio.toInt()}% · Cognitive Depth: ${prefs.cognitiveCapacity.toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        CyberSecondaryButton(
                            text = "CUSTOMIZE IN PERSONA BUILDER",
                            onClick = { onNavigate("persona") },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "library_open_persona_button"
                        )
                    }
                }
                items(AgentRole.entries, key = { it.id }) { role ->
                    SearchResultCard(
                        icon = Icons.Rounded.Psychology,
                        title = "${role.title} Specialist",
                        subtitle = role.subtitle,
                        badge = "USE IN CHAT",
                        tag = "library_agent_${role.id}",
                        onClick = { onSelectAgent(role) }
                    )
                }
            }
        }
    }
}
