package com.example.ui.screens

import android.graphics.Bitmap
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.theme.LocalCyberColors
import kotlinx.coroutines.launch

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
                    StudioCreativeBriefPanel(
                        prompt = prompt,
                        onPromptChange = { prompt = it },
                        outputs = outputs,
                        styles = styles,
                        selectedOutput = selectedOutput,
                        onSelectOutput = { selectedOutput = it },
                        selectedStyle = selectedStyle,
                        onSelectStyle = { selectedStyle = it },
                        briefState = briefState,
                        onGenerateBrief = onGenerateBrief
                    )
                }
            }
        }
    }
}

