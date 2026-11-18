package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Visibility
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.domain.CameraVisionResult
import com.example.domain.UiState
import com.example.domain.VisionMode
import com.example.ui.components.CyberActionButton
import com.example.ui.components.CyberCard
import com.example.ui.components.CyberCardHeader
import com.example.ui.components.CyberIconBadge
import com.example.ui.components.CyberSecondaryButton
import com.example.ui.components.CyberSectionTitle
import com.example.ui.components.ZaytharIcons
import com.example.ui.theme.LocalCyberColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CameraEyesScreen(
    visionState: UiState<CameraVisionResult>,
    onAnalyzeImage: (Bitmap?, String, VisionMode) -> Unit,
    onToast: (String) -> Unit,
    onSaveToRag: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val cyber = LocalCyberColors.current
    val scope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf(VisionMode.SCENE) }
    var prompt by remember { mutableStateOf("") }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pendingCaptureUri by remember { mutableStateOf<Uri?>(null) }

    fun loadScaledBitmap(uri: Uri) {
        scope.launch {
            val bmp = decodeScaledBitmapFromUri(context, uri, maxDim = 1280)
            if (bmp != null) {
                selectedBitmap = bmp
            } else {
                onToast("Could not decode selected image.")
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            loadScaledBitmap(uri)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = pendingCaptureUri
        if (success && uri != null) {
            loadScaledBitmap(uri)
        } else if (!success) {
            onToast("Camera capture cancelled.")
        }
    }

    fun launchCamera() {
        runCatching {
            val dir = File(context.cacheDir, "camera_eyes").apply { mkdirs() }
            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            pendingCaptureUri = uri
            takePictureLauncher.launch(uri)
        }.onFailure {
            onToast("No camera app available on this device. Use Pick Photo instead.")
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            CyberSectionTitle(
                eyebrow = "MULTIMODAL VISUAL SENSOR",
                titlePrefix = "Camera",
                titleAccent = "Eyes",
                trailing = {
                    Text(
                        text = "GEMINI 3.8 VISION",
                        style = MaterialTheme.typography.labelSmall,
                        color = cyber.cyberGreen
                    )
                }
            )
        }

        item {
            CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                CyberCardHeader(
                    title = "VISUAL INPUT VIEWPORT",
                    badge = if (selectedBitmap != null) "FRAME LOCKED" else "AWAITING OPTICS"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .border(1.dp, cyber.accent.copy(alpha = 0.55f), MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = selectedBitmap
                    if (bmp != null) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Captured or selected image for Camera Eyes vision analysis",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CyberIconBadge(
                                icon = Icons.Rounded.Visibility,
                                contentDescription = null,
                                sizeDp = 56.dp,
                                iconSizeDp = 28.dp
                            )
                            Text(
                                text = "Capture a live photo or select an image via Android Photo Picker",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CyberActionButton(
                        text = "CAPTURE PHOTO",
                        onClick = ::launchCamera,
                        modifier = Modifier.weight(1f),
                        testTag = "camera_capture_button"
                    )
                    CyberSecondaryButton(
                        text = "PICK PHOTO",
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier.weight(1f),
                        testTag = "photo_picker_button"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "VISION INSPECTION MODE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VisionMode.entries.forEach { mode ->
                        val active = mode == selectedMode
                        Surface(
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .clickable { selectedMode = mode }
                                .testTag("vision_mode_${mode.id}"),
                            shape = MaterialTheme.shapes.small,
                            color = if (active) cyber.accent.copy(alpha = 0.2f) else cyber.panelElevated,
                            border = BorderStroke(
                                1.dp,
                                if (active) cyber.accent else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = ZaytharIcons.forVisionMode(mode),
                                    contentDescription = null,
                                    tint = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = mode.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (active) cyber.accentSoft else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { if (it.length <= 2000) prompt = it },
                    label = { Text("VISION QUESTION (OPTIONAL)") },
                    placeholder = { Text("Ask anything about the image, error screen, hardware, or document…") },
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("vision_prompt_input")
                )

                Spacer(modifier = Modifier.height(12.dp))
                CyberActionButton(
                    text = if (visionState is UiState.Loading) "ANALYZING FRAME…" else "ANALYZE WITH CAMERA EYES",
                    onClick = { onAnalyzeImage(selectedBitmap, prompt, selectedMode) },
                    enabled = selectedBitmap != null && visionState !is UiState.Loading,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "analyze_vision_button"
                )
            }
        }

        when (visionState) {
            is UiState.Loading -> {
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Sending compressed frame to Gemini 3.8 Flash Vision…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cyber.cyberGreen
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = cyber.accent,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
            is UiState.Error -> {
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Vision error: ${visionState.message}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            is UiState.Success -> {
                val res = visionState.data
                item {
                    CyberCard(modifier = Modifier.fillMaxWidth(), highlighted = true) {
                        CyberCardHeader(
                            title = "${res.mode.title.uppercase()} RESULT",
                            badge = "${res.modelUsed} · SYNCED TO CHAT",
                            trailing = {
                                CyberSecondaryButton(
                                    text = "COPY",
                                    onClick = {
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("Zaythar Camera Eyes", res.analysis))
                                        onToast("Vision analysis copied.")
                                    }
                                )
                            }
                        )
                        Text(
                            text = res.analysis,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        CyberSecondaryButton(
                            text = "SAVE ANALYSIS TO RAG MEMORY",
                            onClick = {
                                onSaveToRag("[CAMERA EYES · ${res.mode.title}]\n${res.analysis}".take(1950))
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "save_vision_to_rag_button"
                        )
                    }
                }
            }
            else -> {}
        }
    }
}

private suspend fun decodeScaledBitmapFromUri(
    context: Context,
    uri: Uri,
    maxDim: Int
): Bitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        }
        var sampleSize = 1
        while (boundsOpts.outWidth / sampleSize > maxDim || boundsOpts.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        }
    }.getOrNull()
}
