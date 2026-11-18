package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.GeneratedImageResult
import com.example.domain.StudioAspectRatio
import com.example.domain.UiState
import com.example.domain.defaultPromptTemplates
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.components.ZaytharIcons
import com.example.ui.theme.LocalCyberColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class StudioWorkbenchMode { IMAGE_ENGINE, CREATIVE_BRIEF }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
    briefState: UiState<String>,
    onGenerateBrief: (String, String, String) -> Unit,
    imageState: UiState<GeneratedImageResult> = UiState.Idle,
    onGenerateImage: (String, String, StudioAspectRatio, Bitmap?) -> Unit = { _, _, _, _ -> },
    onToast: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current
    val scope = rememberCoroutineScope()

    var activeMode by remember {
        mutableStateOf(
            if (briefState is UiState.Success && imageState is UiState.Idle) {
                StudioWorkbenchMode.CREATIVE_BRIEF
            } else {
                StudioWorkbenchMode.IMAGE_ENGINE
            }
        )
    }

    var prompt by remember { mutableStateOf("") }
    val outputs = listOf("Image concept", "Video storyboard", "Visual asset brief")
    val styles = listOf("Cinematic sci-fi", "Photorealistic", "Cyberpunk", "3D render", "Minimal vector")
    var selectedOutput by remember { mutableStateOf(outputs.first()) }
    var selectedStyle by remember { mutableStateOf(styles.first()) }
    var selectedAspectRatio by remember { mutableStateOf(StudioAspectRatio.SQUARE_1_1) }
    var referenceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var outputExpanded by remember { mutableStateOf(false) }
    var styleExpanded by remember { mutableStateOf(false) }

    val pickReferenceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val bmp = decodeStudioBitmap(context, uri, 1024)
                if (bmp != null) {
                    referenceBitmap = bmp
                    onToast("Reference image loaded for AI editing.")
                } else {
                    onToast("Could not decode reference image.")
                }
            }
        }
    }

    val isBriefLoading = briefState is UiState.Loading
    val isImageLoading = imageState is UiState.Loading

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "NANO BANANA 2.1 + CREATIVE DIRECTOR",
                titlePrefix = "AI Image & Video",
                titleAccent = "Studio",
                trailing = {
                    Text(
                        text = if (activeMode == StudioWorkbenchMode.IMAGE_ENGINE) "NANO BANANA 2.1" else "CREATIVE CORE",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        // Mode Switcher
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StudioModeChip(
                    label = "AI IMAGE ENGINE",
                    icon = Icons.Rounded.AutoFixHigh,
                    selected = activeMode == StudioWorkbenchMode.IMAGE_ENGINE,
                    testTag = "studio_mode_image",
                    onClick = { activeMode = StudioWorkbenchMode.IMAGE_ENGINE },
                    modifier = Modifier.weight(1f)
                )
                StudioModeChip(
                    label = "STORYBOARD BRIEF",
                    icon = Icons.Rounded.AutoAwesome,
                    selected = activeMode == StudioWorkbenchMode.CREATIVE_BRIEF,
                    testTag = "studio_mode_brief",
                    onClick = { activeMode = StudioWorkbenchMode.CREATIVE_BRIEF },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = activeMode == StudioWorkbenchMode.IMAGE_ENGINE) {
                if (activeMode == StudioWorkbenchMode.IMAGE_ENGINE) {
                    // Runtime AI Image Viewport
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .border(1.dp, cyber.accent.copy(alpha = 0.55f), MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        when (imageState) {
                            is UiState.Loading -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    CircularProgressIndicator(color = cyber.accentSoft)
                                    Text(
                                        text = "Synthesizing with gemini-nano-banana-2.1 (30–60s)…",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = cyber.cyberGreen
                                    )
                                }
                            }
                            is UiState.Success -> {
                                Image(
                                    bitmap = imageState.data.bitmap.asImageBitmap(),
                                    contentDescription = "AI generated image: ${imageState.data.prompt}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .testTag("studio_generated_image")
                                )
                            }
                            else -> {
                                Image(
                                    painter = painterResource(id = R.drawable.img_studio_canvas),
                                    contentDescription = "Creative holographic studio canvas",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.52f))
                                )
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    CyberIconBadge(
                                        icon = Icons.Rounded.Image,
                                        contentDescription = null,
                                        sizeDp = 56.dp,
                                        iconSizeDp = 28.dp
                                    )
                                    Text(
                                        text = "AI IMAGE GENERATION & EDITING",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Create 1K artwork or edit any photo with Nano Banana 2.1",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                        }
                    }

                    if (referenceBitmap != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = cyber.panelElevated,
                            border = BorderStroke(1.dp, cyber.cyberGreen.copy(alpha = 0.6f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Image(
                                    bitmap = referenceBitmap!!.asImageBitmap(),
                                    contentDescription = "Reference image for editing",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("IMAGE-TO-IMAGE EDIT MODE", style = MaterialTheme.typography.labelSmall, color = cyber.cyberGreen)
                                    Text("Prompt will edit this reference photo.", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(
                                    onClick = { referenceBitmap = null },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear reference image")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { if (it.length <= 2000) prompt = it },
                        label = { Text(if (referenceBitmap != null) "IMAGE EDIT INSTRUCTION" else "IMAGE GENERATION PROMPT") },
                        placeholder = { Text("Describe subject, lighting, mood, or edit instructions…") },
                        minLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("studio_image_prompt_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StudioAspectRatio.entries.forEach { ratio ->
                            val active = ratio == selectedAspectRatio
                            Surface(
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .clickable { selectedAspectRatio = ratio }
                                    .testTag("studio_ratio_${ratio.id}"),
                                shape = MaterialTheme.shapes.small,
                                color = if (active) cyber.accent.copy(alpha = 0.2f) else cyber.panelElevated,
                                border = BorderStroke(1.dp, if (active) cyber.accent else MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = ratio.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
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
                                    DropdownMenuItem(text = { Text(item) }, onClick = { selectedStyle = item; styleExpanded = false })
                                }
                            }
                        }

                        CyberSecondaryButton(
                            text = if (referenceBitmap == null) "EDIT PHOTO" else "SWAP PHOTO",
                            onClick = {
                                pickReferenceLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "studio_pick_reference_button"
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    CyberActionButton(
                        text = if (isImageLoading) "GENERATING IMAGE…" else if (referenceBitmap != null) "APPLY AI IMAGE EDIT" else "GENERATE AI IMAGE",
                        onClick = { onGenerateImage(prompt, selectedStyle, selectedAspectRatio, referenceBitmap) },
                        enabled = !isImageLoading,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "generate_image_button"
                    )

                    if (imageState is UiState.Error) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Image engine error: ${imageState.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (imageState is UiState.Success) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${imageState.data.modelUsed} · ${imageState.data.aspectRatio.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = cyber.cyberGreen
                            )
                            CyberSecondaryButton(
                                text = "SAVE TO GALLERY",
                                onClick = {
                                    scope.launch {
                                        val ok = saveBitmapToGallery(context, imageState.data.bitmap)
                                        onToast(if (ok) "Saved to Pictures/Zaythar Studio." else "Unable to save image.")
                                    }
                                },
                                testTag = "save_generated_image_button"
                            )
                        }
                    }
                } else {
                    // Creative Brief & Storyboard Workbench
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
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
                                .background(Color.Black.copy(alpha = 0.48f))
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(16.dp)
                        ) {
                            CyberIconBadge(
                                icon = ZaytharIcons.StudioSpark,
                                contentDescription = null,
                                sizeDp = 50.dp,
                                iconSizeDp = 26.dp
                            )
                            Text(
                                text = "YOUR CREATIVE CANVAS",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Generate live AI art direction & shot-by-shot storyboards",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.82f)
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
                                    DropdownMenuItem(text = { Text(item) }, onClick = { selectedOutput = item; outputExpanded = false })
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
                                    DropdownMenuItem(text = { Text(item) }, onClick = { selectedStyle = item; styleExpanded = false })
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    CyberActionButton(
                        text = if (isBriefLoading) "DIRECTING CONCEPT…" else "GENERATE CREATIVE CONCEPT",
                        onClick = { onGenerateBrief(prompt, selectedOutput, selectedStyle) },
                        enabled = !isBriefLoading,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "make_brief_button"
                    )

                    if (isBriefLoading) {
                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = cyber.accent)
                    } else if (briefState is UiState.Error) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = briefState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else if (briefState is UiState.Success && briefState.data.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = briefState.data, style = MaterialTheme.typography.bodyMedium, color = cyber.cyberGreen)
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioModeChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cyber = LocalCyberColors.current
    Surface(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = MaterialTheme.shapes.small,
        color = if (selected) cyber.accent.copy(alpha = 0.2f) else cyber.panelElevated,
        border = BorderStroke(1.dp, if (selected) cyber.accent else MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) cyber.accentSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) cyber.accentSoft else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun PromptNexusScreen(onSelectPrompt: (String) -> Unit) {
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
            CyberSectionTitle(eyebrow = "PROMPT LIBRARY", titlePrefix = "Prompt", titleAccent = "Nexus")
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search prompts") },
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
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CyberIconBadge(
                        icon = ZaytharIcons.PromptBook,
                        contentDescription = null,
                        sizeDp = 42.dp,
                        iconSizeDp = 20.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = tpl.title, style = MaterialTheme.typography.titleMedium, color = cyber.accentSoft)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = tpl.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private suspend fun decodeStudioBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }.getOrNull()
}

private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "zaythar_${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Zaythar Studio")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return@withContext false
        resolver.openOutputStream(uri)?.use { out ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) return@withContext false
        } ?: return@withContext false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    }.getOrDefault(false)
}
