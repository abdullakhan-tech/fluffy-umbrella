package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ZaytharPreferences
import com.example.data.remote.FirestoreMemoryNote
import com.example.domain.ProviderRoute
import com.example.domain.UiState
import com.example.domain.defaultPromptTemplates
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberSectionTitle
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
                val wide = maxWidth >= 700.dp
                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ProviderRoute.entries.forEach { route ->
                            ModelRouteCard(
                                route = route,
                                selected = route == currentRoute,
                                onClick = { onSelectRoute(route) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProviderRoute.entries.forEach { route ->
                            ModelRouteCard(
                                route = route,
                                selected = route == currentRoute,
                                onClick = { onSelectRoute(route) },
                                modifier = Modifier.fillMaxWidth()
                            )
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
    val glyph = when (route) {
        ProviderRoute.FLASH -> "◎"
        ProviderRoute.HYBRID -> "◉"
        ProviderRoute.LITE -> "✧"
    }
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
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(cyber.accent.copy(alpha = 0.35f), cyber.panelGlass)
                        )
                    )
                    .border(1.dp, cyber.accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = glyph, fontSize = 36.sp, color = Color.White)
            }
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
fun StudioScreen(
    briefState: UiState<String>,
    onGenerateBrief: (String, String, String) -> Unit
) {
    val cyber = LocalCyberColors.current
    var prompt by remember { mutableStateOf("") }
    val outputs = listOf("Image concept", "Video storyboard", "Visual asset brief")
    val styles = listOf("Cinematic sci-fi", "Photorealistic", "Cyberpunk", "3D render")
    var selectedOutput by remember { mutableStateOf(outputs.first()) }
    var selectedStyle by remember { mutableStateOf(styles.first()) }
    var outputExpanded by remember { mutableStateOf(false) }
    var styleExpanded by remember { mutableStateOf(false) }

    val isLoading = briefState is UiState.Loading

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "CREATIVE WORKBENCH",
                titlePrefix = "AI Image & Video",
                titleAccent = "Studio",
                trailing = {
                    Text(
                        text = "GEMINI CREATIVE CORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .border(1.dp, cyber.accent.copy(alpha = 0.45f), MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_studio_canvas),
                        contentDescription = "Creative holographic studio canvas",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.45f))
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(text = "✧", fontSize = 42.sp, color = cyber.accentSoft)
                        Text(
                            text = "YOUR CREATIVE CANVAS",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Generate live AI art direction & shot-by-shot storyboards",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    label = { Text("CREATIVE PROMPT") },
                    placeholder = { Text("Describe a cinematic scene, visual concept, or video idea…") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_prompt_input")
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { outputExpanded = true },
                            shape = MaterialTheme.shapes.small,
                            color = cyber.panelElevated,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("OUTPUT", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedOutput, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        DropdownMenu(expanded = outputExpanded, onDismissRequest = { outputExpanded = false }) {
                            outputs.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = { selectedOutput = item; outputExpanded = false }
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable { styleExpanded = true },
                            shape = MaterialTheme.shapes.small,
                            color = cyber.panelElevated,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("STYLE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(selectedStyle, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        DropdownMenu(expanded = styleExpanded, onDismissRequest = { styleExpanded = false }) {
                            styles.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item) },
                                    onClick = { selectedStyle = item; styleExpanded = false }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                CyberActionButton(
                    text = if (isLoading) "DIRECTING CONCEPT…" else "GENERATE CREATIVE CONCEPT",
                    onClick = { onGenerateBrief(prompt, selectedOutput, selectedStyle) },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "make_brief_button"
                )

                if (isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = cyber.accent)
                } else if (briefState is UiState.Error) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = briefState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (briefState is UiState.Success && briefState.data.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = briefState.data,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cyber.cyberGreen
                    )
                }
            }
        }
    }
}

@Composable
fun PromptNexusScreen(
    onSelectPrompt: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        val q = query.trim().lowercase()
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
                eyebrow = "PROMPT LIBRARY",
                titlePrefix = "Prompt",
                titleAccent = "Nexus"
            )
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search prompts") },
                placeholder = { Text("Search prompt templates…") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("prompt_search_input")
            )
        }

        items(filtered, key = { it.id }) { tpl ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .clickable { onSelectPrompt(tpl.promptPrefix) }
                    .testTag("prompt_card_${tpl.id}"),
                shape = MaterialTheme.shapes.medium,
                color = cyber.panelElevated,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = tpl.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = cyber.accentSoft
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = tpl.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun KnowledgeBaseScreen(
    notesState: UiState<List<FirestoreMemoryNote>>,
    onAddNote: (String) -> Unit,
    onDeleteNote: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    var noteInput by remember { mutableStateOf("") }
    val notes = (notesState as? UiState.Success)?.data ?: emptyList()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "PERSONAL KNOWLEDGE SYSTEM",
                titlePrefix = "Knowledge",
                titleAccent = "Base"
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "⌬", fontSize = 46.sp, color = cyber.accent)
                    Text(
                        text = "Build your knowledge nexus",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Saved notes are synced with Cloud Firestore and injected into AI Assistant, AI Brain, and Multi-Agent runs.",
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
                    placeholder = { Text("Paste notes or reference material here…") },
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
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete note",
                            tint = cyber.accentSoft
                        )
                    }
                }
            }
        }
    }
}
