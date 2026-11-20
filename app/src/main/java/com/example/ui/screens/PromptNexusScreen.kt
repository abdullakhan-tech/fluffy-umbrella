package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.UiState
import com.example.domain.defaultPromptTemplates
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSectionTitle
import com.example.ui.components.ZaytharIcons
import com.example.ui.theme.LocalCyberColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun StudioModeChip(
    label: String,
    icon: ImageVector,
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
internal fun StudioCreativeBriefPanel(
    prompt: String,
    onPromptChange: (String) -> Unit,
    outputs: List<String>,
    styles: List<String>,
    selectedOutput: String,
    onSelectOutput: (String) -> Unit,
    selectedStyle: String,
    onSelectStyle: (String) -> Unit,
    briefState: UiState<String>,
    onGenerateBrief: (String, String, String) -> Unit
) {
    val cyber = LocalCyberColors.current
    val isBriefLoading = briefState is UiState.Loading
    var outputExpanded by remember { mutableStateOf(false) }
    var styleExpanded by remember { mutableStateOf(false) }

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
        onValueChange = onPromptChange,
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
                    DropdownMenuItem(text = { Text(item) }, onClick = { onSelectOutput(item); outputExpanded = false })
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
                    DropdownMenuItem(text = { Text(item) }, onClick = { onSelectStyle(item); styleExpanded = false })
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

internal suspend fun decodeStudioBitmap(context: Context, uri: Uri, maxDim: Int): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > maxDim || bounds.outHeight / sample > maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
    }.getOrNull()
}

internal suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean = withContext(Dispatchers.IO) {
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
