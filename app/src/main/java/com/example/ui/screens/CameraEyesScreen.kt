package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.pm.PackageManager
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.CameraController
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.domain.CameraActivationPolicy
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
    var cameraEnabled by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var cameraPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraController = remember(context) {
        LifecycleCameraController(context).apply {
            cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        cameraEnabled = granted
        if (!granted) onToast("Camera permission was denied. Camera Eyes remains off.")
    }

    DisposableEffect(cameraController, lifecycleOwner, cameraEnabled, cameraPermissionGranted) {
        if (CameraActivationPolicy.shouldBindCamera(cameraEnabled, cameraPermissionGranted)) {
            cameraController.bindToLifecycle(lifecycleOwner)
        } else {
            cameraController.unbind()
        }
        onDispose { cameraController.unbind() }
    }

    fun setCameraEnabled(enabled: Boolean) {
        if (!enabled) {
            cameraEnabled = false
            return
        }
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        cameraPermissionGranted = granted
        if (granted) cameraEnabled = true else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
    }

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

    fun captureLiveFrame() {
        if (!CameraActivationPolicy.shouldBindCamera(cameraEnabled, cameraPermissionGranted)) {
            onToast("Turn Camera Eyes on and grant camera permission before capturing a frame.")
            return
        }
        runCatching {
            val directory = File(context.cacheDir, "camera_eyes").apply { mkdirs() }
            val file = File(directory, "frame_${System.currentTimeMillis()}.jpg")
            val output = ImageCapture.OutputFileOptions.Builder(file).build()
            cameraController.takePicture(
                output,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                        loadScaledBitmap(Uri.fromFile(file))
                        onToast("Frame captured. Select ANALYZE when ready.")
                    }

                    override fun onError(exception: ImageCaptureException) {
                        onToast("Camera capture failed. Check permission and try again.")
                    }
                }
            )
        }.onFailure {
            onToast("Camera could not capture a frame. Turn Camera Eyes off and on to retry.")
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
                    badge = if (cameraEnabled && cameraPermissionGranted) "LIVE OPTICS" else if (selectedBitmap != null) "FRAME LOCKED" else "OPTICS OFF"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = if (cameraEnabled && cameraPermissionGranted) "CAMERA EYES ONLINE" else "CAMERA EYES OFFLINE",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (cameraEnabled && cameraPermissionGranted) cyber.cyberGreen else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = if (cameraEnabled) "Live preview active on this screen only" else "No camera preview or capture is running",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = cameraEnabled && cameraPermissionGranted,
                        onCheckedChange = ::setCameraEnabled,
                        modifier = Modifier.testTag("camera_eyes_power_switch")
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .border(1.dp, cyber.accent.copy(alpha = 0.55f), MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (cameraEnabled && cameraPermissionGranted) {
                        AndroidView(
                            factory = { viewContext ->
                                PreviewView(viewContext).apply {
                                    scaleType = PreviewView.ScaleType.FILL_CENTER
                                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                    controller = cameraController
                                }
                            },
                            update = { it.controller = cameraController },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
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
                                    text = "Turn Camera Eyes on to start a permission-controlled live preview",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CyberActionButton(
                        text = "CAPTURE FRAME",
                        onClick = ::captureLiveFrame,
                        enabled = cameraEnabled && cameraPermissionGranted,
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

                Spacer(modifier = Modifier.height(8.dp))
                CyberSecondaryButton(
                    text = if (useFrontCamera) "SWITCH TO REAR CAMERA" else "SWITCH TO FRONT CAMERA",
                    onClick = {
                        val nextUsesFrontCamera = !useFrontCamera
                        runCatching {
                            cameraController.cameraSelector = if (nextUsesFrontCamera) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        }.onSuccess {
                            useFrontCamera = nextUsesFrontCamera
                        }.onFailure {
                            onToast("Unable to switch camera lens on this device.")
                        }
                    },
                    enabled = cameraEnabled && cameraPermissionGranted,
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "camera_lens_switch_button"
                )

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
