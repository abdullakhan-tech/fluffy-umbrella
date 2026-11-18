package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreKnowledgeFact
import com.example.data.remote.FirestoreMemoryNote
import com.example.domain.ProviderRoute
import com.example.domain.RagRetrievalEngine
import com.example.domain.UiState
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSectionTitle
import com.example.ui.components.ZaytharIcons
import com.example.ui.theme.LocalCyberColors
import com.example.ui.theme.SpaceGroteskFontFamily

@Composable
fun ModelMatrixScreen(
    prefs: ZaytharPreferences,
    onSelectRoute: (ProviderRoute) -> Unit,
    onOpenAssistant: () -> Unit
) {
    val cyber = LocalCyberColors.current
    val currentRoute = ProviderRoute.fromId(prefs.selectedProviderRoute)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "INTELLIGENCE ROUTING",
                titlePrefix = "AI Model",
                titleAccent = "Selection Matrix",
                trailing = {
                    Text(
                        text = "AI LOGIC ROUTING",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        item {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val cols = when {
                    maxWidth >= 900.dp -> 3
                    maxWidth >= 600.dp -> 2
                    else -> 1
                }
                val rows = ProviderRoute.entries.chunked(cols)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { route ->
                                ModelRouteCard(
                                    route = route,
                                    selected = route == currentRoute,
                                    onClick = { onSelectRoute(route) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(cols - rowItems.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                CyberCardHeader(title = "ROUTING STATUS", badge = "LOCAL PREFERENCE")
                Text(
                    text = "Selected route: ${currentRoute.title} (${currentRoute.modelName})",
                    style = MaterialTheme.typography.titleMedium,
                    color = cyber.accentSoft
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "This selects the Firebase AI Logic Gemini model route used for future chat, AI Brain, Code Copilot, and multi-agent requests.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                CyberActionButton(
                    text = "OPEN AI ASSISTANT",
                    onClick = onOpenAssistant,
                    testTag = "open_assistant_from_models"
                )
            }
        }
    }
}

@Composable
private fun ModelRouteCard(
    route: ProviderRoute,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyber = LocalCyberColors.current
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag("model_route_${route.id}"),
        shape = MaterialTheme.shapes.medium,
        color = cyber.panelElevated,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) cyber.accent else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CyberIconBadge(
                icon = ZaytharIcons.forProviderRoute(route),
                contentDescription = route.title,
                sizeDp = 76.dp,
                iconSizeDp = 36.dp,
                isActive = selected
            )
            Text(
                text = route.title,
                fontFamily = SpaceGroteskFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center
            )
            Text(
                text = route.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (selected) "ACTIVE ROUTE" else "SELECT ROUTE",
                style = MaterialTheme.typography.labelSmall,
                color = cyber.accentSoft
            )
        }
    }
}

@Composable
fun KnowledgeBaseScreen(
    notesState: UiState<List<FirestoreMemoryNote>>,
    onAddNote: (String) -> Unit,
    onDeleteNote: (String) -> Unit,
    factsState: UiState<List<FirestoreKnowledgeFact>> = UiState.Success(emptyList())
) {
    val cyber = LocalCyberColors.current
    var noteInput by remember { mutableStateOf("") }
    var ragQuery by remember { mutableStateOf("") }
    val notes = (notesState as? UiState.Success)?.data ?: emptyList()
    val facts = (factsState as? UiState.Success)?.data ?: emptyList()
    val ragBundle = remember(ragQuery, notes, facts) {
        RagRetrievalEngine.retrieve(
            query = ragQuery,
            memories = notes,
            facts = facts,
            topK = 5
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "HYBRID BM25 + GRAPH RAG ENGINE",
                titlePrefix = "Knowledge",
                titleAccent = "Base",
                trailing = {
                    Text(
                        text = "${ragBundle.indexedDocuments} DOCS · ${ragBundle.indexedChunks} CHUNKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CyberIconBadge(
                        icon = ZaytharIcons.KnowledgeNode,
                        contentDescription = "Knowledge Graph & Hybrid RAG Core",
                        sizeDp = 64.dp,
                        iconSizeDp = 32.dp
                    )
                    Text(
                        text = "Build your knowledge nexus",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Saved notes & knowledge graph facts are chunked and ranked via Hybrid RAG into Chat, AI Brain, Code Copilot, and Multi-Agent runs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = noteInput,
                    onValueChange = { if (it.length <= 2000) noteInput = it },
                    label = { Text("KNOWLEDGE NOTE") },
                    placeholder = { Text("Paste notes, specs, or reference material here…") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("knowledge_note_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                CyberActionButton(
                    text = "SAVE NOTE TO CLOUD",
                    onClick = {
                        onAddNote(noteInput)
                        if (noteInput.isNotBlank()) noteInput = ""
                    },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "save_note_button"
                )
                if (notesState is UiState.Error) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Sync warning: ${notesState.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                CyberCardHeader(
                    title = "HYBRID RAG RETRIEVAL SANDBOX",
                    badge = "BM25 + BIGRAM + GRAPH",
                    badgeColor = cyber.cyberGreen
                )
                OutlinedTextField(
                    value = ragQuery,
                    onValueChange = { if (it.length <= 250) ragQuery = it },
                    label = { Text("TEST HYBRID RAG QUERY") },
                    placeholder = { Text("Search across cloud memories & knowledge graph…") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rag_search_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (ragBundle.chunks.isEmpty()) {
                    Text(
                        text = if (ragQuery.isBlank()) "Save notes or knowledge triples to inspect live RAG chunk retrieval." else "No semantic/lexical matches found for \"$ragQuery\".",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ragBundle.chunks.forEach { chunk ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = cyber.panelElevated,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.testTag("rag_match_card_${chunk.id}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${chunk.sourceType.tag} · ${chunk.title}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cyber.accentSoft
                                        )
                                        Text(
                                            text = "${chunk.scorePercent}% MATCH",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cyber.cyberGreen
                                        )
                                    }
                                    Text(
                                        text = chunk.text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (chunk.matchedTerms.isNotEmpty()) {
                                        Text(
                                            text = "Matched tokens: ${chunk.matchedTerms.joinToString(", ")}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cyber.cyberGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        items(notes, key = { it.id.ifBlank { it.content.hashCode().toString() } }) { note ->
            Surface(
                shape = MaterialTheme.shapes.small,
                color = cyber.panelGlass,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = "Delete note",
                            tint = cyber.accentSoft
                        )
                    }
                }
            }
        }
    }
}
