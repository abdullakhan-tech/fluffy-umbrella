package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.NeuralCoreActivity
import com.example.ui.components.NeuralCoreVisualizer
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
                                motionEnabled = prefs.motion3dEnabled,
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
                                motionEnabled = prefs.motion3dEnabled,
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
    motionEnabled: Boolean,
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
            NeuralCoreVisualizer(
                size = 84.dp,
                motionEnabled = motionEnabled && selected,
                coreLabel = route.id.take(1).uppercase(),
                activity = NeuralCoreActivity.READY
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
fun StudioScreen(
    briefState: UiState<String>,
    imageState: UiState<Bitmap>,
    onGenerateBrief: (String, String, String) -> Unit,
    onGenerateImage: (String, String) -> Unit,
    onSaveToKnowledge: (String) -> Unit,
    onToast: (String) -> Unit
) {
    val cyber = LocalCyberColors.current
    val context = LocalContext.current
    var prompt by remember { mutableStateOf("") }
    val outputs = listOf("Image generation", "Video storyboard", "Visual asset brief")
    val styles = listOf("Cinematic sci-fi", "Photorealistic", "Cyberpunk", "3D render")
    var selectedOutput by remember { mutableStateOf(outputs.first()) }
    var selectedStyle by remember { mutableStateOf(styles.first()) }
    var outputExpanded by remember { mutableStateOf(false) }
    var styleExpanded by remember { mutableStateOf(false) }
    val imageMode = selectedOutput == "Image generation"
    val isLoading = if (imageMode) imageState is UiState.Loading else briefState is UiState.Loading

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
                        text = if (imageMode) "NANO BANANA 2.1" else "GEMINI CREATIVE CORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = imageMode) {
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
                            text = if (imageMode) "IMAGE GENERATION CORE" else "CINEMATIC STORYBOARD CORE",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            text = "Create a visual asset or a production-ready shot-by-shot plan",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { if (it.length <= 4000) prompt = it },
                    label = { Text("CREATIVE PROMPT") },
                    placeholder = { Text("Describe the scene, subject, lighting, camera angle, mood, and details…") },
                    minLines = 3,
                    maxLines = 6,
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
                    text = when {
                        isLoading -> "PROCESSING CREATIVE REQUEST…"
                        imageMode -> "GENERATE AI IMAGE"
                        selectedOutput == "Video storyboard" -> "GENERATE VIDEO STORYBOARD"
                        else -> "GENERATE VISUAL ASSET BRIEF"
                    },
                    onClick = {
                        if (imageMode) onGenerateImage(prompt, selectedStyle)
                        else onGenerateBrief(prompt, selectedOutput, selectedStyle)
                    },
                    enabled = !isLoading && prompt.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "make_brief_button"
                )

                if (isLoading) {
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = cyber.accent)
                    Text(
                        text = if (imageMode) "Nano Banana is rendering your image…" else "Gemini is building your creative brief…",
                        style = MaterialTheme.typography.bodySmall,
                        color = cyber.cyberGreen
                    )
                }

                if (imageMode) {
                    when (imageState) {
                        is UiState.Error -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = imageState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is UiState.Success -> {
                            Spacer(modifier = Modifier.height(14.dp))
                            CyberCardHeader(title = "GENERATED IMAGE", badge = "PNG · 16:9")
                            Image(
                                bitmap = imageState.data.asImageBitmap(),
                                contentDescription = "Generated Zaythar AI studio image",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 420.dp)
                                    .testTag("studio_generated_image")
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            CyberActionButton(
                                text = "SAVE IMAGE TO GALLERY",
                                onClick = {
                                    val saved = saveStudioImageToGallery(context, imageState.data)
                                    onToast(
                                        if (saved) "Image saved to Pictures/Zaythar AI."
                                        else "Could not save image. Gallery export requires Android 10 or newer."
                                    )
                                },
                                enabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q,
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "save_studio_image_button"
                            )
                        }
                        else -> Unit
                    }
                } else {
                    when (briefState) {
                        is UiState.Error -> {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = briefState.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        is UiState.Success -> {
                            if (briefState.data.isNotBlank()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = briefState.data,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cyber.cyberGreen
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CyberSecondaryButton(
                                        text = "COPY BRIEF",
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            clipboard.setPrimaryClip(ClipData.newPlainText("Zaythar Studio brief", briefState.data))
                                            onToast("Creative brief copied.")
                                        },
                                        modifier = Modifier.weight(1f),
                                        testTag = "copy_studio_brief_button"
                                    )
                                    CyberActionButton(
                                        text = "SAVE TO KNOWLEDGE",
                                        onClick = {
                                            onSaveToKnowledge(
                                                "Zaythar Studio · $selectedOutput · $selectedStyle\n\n${briefState.data}"
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        testTag = "save_studio_brief_button"
                                    )
                                }
                            }
                        }
                        else -> Unit
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Image mode creates a real PNG. Video mode creates a shot-by-shot storyboard; video-file rendering is not available through the current Firebase AI Logic integration.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun saveStudioImageToGallery(context: Context, bitmap: Bitmap): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false

    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "zaythar_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Zaythar AI")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false

    return try {
        val written = resolver.openOutputStream(uri)?.use { output ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        } ?: false
        if (!written) throw IllegalStateException("Image stream could not be written.")
        val ready = ContentValues().apply {
            put(MediaStore.Images.Media.IS_PENDING, 0)
        }
        resolver.update(uri, ready, null, null)
        true
    } catch (_: Exception) {
        resolver.delete(uri, null, null)
        false
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
