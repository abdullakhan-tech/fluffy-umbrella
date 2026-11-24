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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Search
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreChatSession
import com.example.data.remote.FirestoreKnowledgeFact
import com.example.data.remote.FirestoreMemoryNote
import com.example.domain.AgentRole
import com.example.domain.UiState
import com.example.domain.defaultPromptTemplates
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.theme.LocalCyberColors

private enum class SearchFilter(val label: String) {
    ALL("All"),
    CHATS("Chats"),
    MEMORIES("Memories"),
    AGENTS("Agents")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchHubScreen(
    sessionsState: UiState<List<FirestoreChatSession>>,
    notesState: UiState<List<FirestoreMemoryNote>>,
    factsState: UiState<List<FirestoreKnowledgeFact>>,
    onOpenSession: (String) -> Unit,
    onSelectAgent: (AgentRole) -> Unit,
    onSelectPrompt: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    var query by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(SearchFilter.ALL) }

    val sessions = (sessionsState as? UiState.Success)?.data.orEmpty()
    val notes = (notesState as? UiState.Success)?.data.orEmpty()
    val facts = (factsState as? UiState.Success)?.data.orEmpty()
    val q = query.trim().lowercase()

    val matchingSessions = remember(sessions, q) {
        if (q.isEmpty()) sessions
        else sessions.filter {
            it.title.lowercase().contains(q) || it.lastMessagePreview.lowercase().contains(q)
        }
    }
    val matchingNotes = remember(notes, q) {
        if (q.isEmpty()) notes else notes.filter { it.content.lowercase().contains(q) }
    }
    val matchingFacts = remember(facts, q) {
        if (q.isEmpty()) facts
        else facts.filter {
            "${it.subject} ${it.predicate} ${it.objectValue}".lowercase().contains(q)
        }
    }
    val matchingAgents = remember(q) {
        if (q.isEmpty()) AgentRole.entries
        else AgentRole.entries.filter {
            it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q)
        }
    }
    val matchingPrompts = remember(q) {
        if (q.isEmpty()) defaultPromptTemplates
        else defaultPromptTemplates.filter {
            it.title.lowercase().contains(q) || it.subtitle.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "UNIFIED COMMAND INDEX",
                titlePrefix = "Search",
                titleAccent = "Zaythar"
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { if (it.length <= 200) query = it },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search icon") },
                    placeholder = { Text("Search chats, saved memories, agents, or prompts…") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_hub_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SearchFilter.entries.forEach { filter ->
                        val active = filter == activeFilter
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { activeFilter = filter }
                                .testTag("search_filter_${filter.name.lowercase()}"),
                            shape = MaterialTheme.shapes.small,
                            color = if (active) cyber.accent.copy(alpha = 0.22f) else cyber.panelElevated,
                            border = BorderStroke(1.dp, if (active) cyber.accent else MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }
        }

        if (activeFilter == SearchFilter.ALL || activeFilter == SearchFilter.CHATS) {
            item {
                Text(
                    text = "CHAT THREADS (${matchingSessions.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = cyber.cyberGreen
                )
            }
            if (matchingSessions.isEmpty()) {
                item {
                    SearchEmptyHint("No matching chat threads found.")
                }
            } else {
                items(matchingSessions, key = { "s_${it.id}" }) { session ->
                    SearchResultCard(
                        icon = Icons.Rounded.ChatBubbleOutline,
                        title = session.title.ifBlank { "Untitled Thread" },
                        subtitle = session.lastMessagePreview.ifBlank { "${session.messageCount} messages" },
                        badge = "${session.messageCount} msgs",
                        tag = "search_result_session_${session.id}",
                        onClick = { onOpenSession(session.id) }
                    )
                }
            }
        }

        if (activeFilter == SearchFilter.ALL || activeFilter == SearchFilter.MEMORIES) {
            item {
                Text(
                    text = "SAVED MEMORIES & GRAPH (${matchingNotes.size + matchingFacts.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = cyber.cyberGreen
                )
            }
            if (matchingNotes.isEmpty() && matchingFacts.isEmpty()) {
                item {
                    SearchEmptyHint("No matching memories or knowledge triples yet.")
                }
            } else {
                items(matchingNotes, key = { "n_${it.id}" }) { note ->
                    SearchResultCard(
                        icon = Icons.Rounded.Memory,
                        title = note.content.take(72),
                        subtitle = "Saved RAG Memory Note",
                        badge = "MEMORY",
                        tag = "search_result_note_${note.id}",
                        onClick = { onNavigate("knowledge") }
                    )
                }
                items(matchingFacts, key = { "f_${it.id}" }) { fact ->
                    SearchResultCard(
                        icon = Icons.Rounded.Hub,
                        title = "${fact.subject} · ${fact.predicate} · ${fact.objectValue}",
                        subtitle = "Knowledge Graph Fact",
                        badge = "GRAPH",
                        tag = "search_result_fact_${fact.id}",
                        onClick = { onNavigate("knowledge") }
                    )
                }
            }
        }

        if (activeFilter == SearchFilter.ALL || activeFilter == SearchFilter.AGENTS) {
            item {
                Text(
                    text = "SPECIALIST AGENTS & PROMPTS (${matchingAgents.size + matchingPrompts.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = cyber.cyberGreen
                )
            }
            items(matchingAgents, key = { "a_${it.id}" }) { role ->
                SearchResultCard(
                    icon = Icons.Rounded.Psychology,
                    title = "${role.title} Agent",
                    subtitle = role.subtitle,
                    badge = "AGENT",
                    tag = "search_result_agent_${role.id}",
                    onClick = { onSelectAgent(role) }
                )
            }
            items(matchingPrompts, key = { "p_${it.id}" }) { tpl ->
                SearchResultCard(
                    icon = Icons.Rounded.AutoAwesome,
                    title = tpl.title,
                    subtitle = tpl.subtitle,
                    badge = "PROMPT",
                    tag = "search_result_prompt_${tpl.id}",
                    onClick = { onSelectPrompt(tpl.promptPrefix) }
                )
            }
        }
    }
}

@Composable
internal fun SearchResultCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    badge: String,
    tag: String,
    onClick: () -> Unit
) {
    val cyber = LocalCyberColors.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(tag),
        shape = MaterialTheme.shapes.medium,
        color = cyber.panelElevated,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CyberIconBadge(icon = icon, contentDescription = title, sizeDp = 40.dp, iconSizeDp = 20.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = cyber.accentSoft
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = cyber.accentSoft,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun SearchEmptyHint(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}
