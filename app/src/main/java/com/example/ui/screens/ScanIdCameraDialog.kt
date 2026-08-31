package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max

data class ScannedIdCandidate(
    val id: String,
    val isFromUrl: Boolean = false,
    val cornerPoints: List<Point>? = null,
    val boundingBox: Rect? = null,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val rotationDegrees: Int = 0
)

/**
 * 3D Live AR Camera Scanner Dialog for scanning numeric Listing IDs from projection screens or documents.
 * Features:
 * - 3D live perspective tracking quad that tilts and follows the projected ID in real-time
 * - Left side zoom preset buttons
 * - Right side vertical zoom slider (Slide UP = Zoom OUT, Slide DOWN = Zoom IN)
 * - Two-finger Pinch-to-zoom
 * - Clean live frame detection without static bounding boxes or lasers
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun ScanIdCameraDialog(
    onIdSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var currentZoom by remember { mutableFloatStateOf(1f) }
    var maxZoomRatio by remember { mutableFloatStateOf(10f) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }

    // Live Candidates detected by OCR in the current frame
    var liveCandidates by remember { mutableStateOf<List<ScannedIdCandidate>>(emptyList()) }
    var userSelectedId by remember { mutableStateOf<String?>(null) }

    // ML Kit text recognizer
    val textRecognizer = remember {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }
    val isAnalyzing = remember { AtomicBoolean(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            textRecognizer.close()
            cameraExecutor.shutdown()
        }
    }

    // Active primary candidate
    val activeCandidate = remember(liveCandidates, userSelectedId) {
        if (liveCandidates.isEmpty()) {
            null
        } else {
            val matched = liveCandidates.find { it.id == userSelectedId }
            matched ?: liveCandidates.first()
        }
    }

    val activeId = activeCandidate?.id

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            if (!hasCameraPermission) {
                // Permission Denied View
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideocamOff,
                            contentDescription = "No Camera Permission",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Izin Kamera Diperlukan",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Aplikasi membutuhkan akses kamera untuk memindai ID listing angka pada layar proyeksi atau dokumen.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(onClick = onDismiss) {
                                Text("Batal")
                            }
                            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                                Text("Berikan Izin")
                            }
                        }
                    }
                }
            } else {
                // Main Camera Scanner View with Pinch-to-Zoom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(maxZoomRatio) {
                            detectTransformGestures { _, _, zoom, _ ->
                                if (zoom != 1f) {
                                    val newZoom = (currentZoom * zoom).coerceIn(1f, maxZoomRatio)
                                    currentZoom = newZoom
                                    cameraControl?.setZoomRatio(newZoom)
                                }
                            }
                        }
                ) {
                    // CameraX Preview
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }

                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null && !isAnalyzing.get()) {
                                        isAnalyzing.set(true)
                                        val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                        val imageWidth = imageProxy.width
                                        val imageHeight = imageProxy.height
                                        val image = InputImage.fromMediaImage(
                                            mediaImage,
                                            rotationDegrees
                                        )

                                        textRecognizer.process(image)
                                            .addOnSuccessListener { visionText ->
                                                val candidates = extractIdCandidatesWithBoxes(
                                                    visionText,
                                                    imageWidth,
                                                    imageHeight,
                                                    rotationDegrees
                                                )
                                                liveCandidates = candidates
                                            }
                                            .addOnFailureListener {
                                                liveCandidates = emptyList()
                                            }
                                            .addOnCompleteListener {
                                                isAnalyzing.set(false)
                                                imageProxy.close()
                                            }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                                try {
                                    cameraProvider.unbindAll()
                                    val camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalysis
                                    )
                                    cameraControl = camera.cameraControl
                                    cameraInfo = camera.cameraInfo
                                    camera.cameraInfo.zoomState.observe(lifecycleOwner) { zoomState ->
                                        if (zoomState != null) {
                                            maxZoomRatio = zoomState.maxZoomRatio.coerceIn(5f, 15f)
                                        }
                                    }
                                } catch (exc: Exception) {
                                    // Log or handle binding failure
                                }
                            }, ContextCompat.getMainExecutor(ctx))

                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // 3D Live Perspective Tracking Overlay on Camera
                    Live3DIdTrackingOverlay(
                        activeCandidate = activeCandidate,
                        otherCandidates = liveCandidates.filter { it.id != activeId }
                    )

                    // Top Toolbar (Header instructions, Torch, Close)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(42.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Tutup",
                                tint = Color.White
                            )
                        }

                        // Top Status Pill
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.65f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (activeId != null) Icons.Default.CheckCircle else Icons.Default.CenterFocusWeak,
                                    contentDescription = null,
                                    tint = if (activeId != null) Color(0xFF22C55E) else Color(0xFF38BDF8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (activeId != null) "ID: $activeId" else "Arahkan ke ID / Link Layar Admin",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.White
                                )
                            }
                        }

                        // Torch Button
                        IconButton(
                            onClick = {
                                val nextFlash = !isFlashOn
                                isFlashOn = nextFlash
                                cameraControl?.enableTorch(nextFlash)
                            },
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (isFlashOn) Color(0xFFFBBF24).copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.5f),
                                    CircleShape
                                )
                                .border(
                                    1.dp,
                                    if (isFlashOn) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                contentDescription = "Senter",
                                tint = if (isFlashOn) Color(0xFFFBBF24) else Color.White
                            )
                        }
                    }

                    // Left Side: Preset Zoom Buttons Panel
                    LeftZoomControlPanel(
                        currentZoom = currentZoom,
                        maxZoomRatio = maxZoomRatio,
                        onZoomChanged = { targetZoom ->
                            val clamped = targetZoom.coerceIn(1f, maxZoomRatio)
                            currentZoom = clamped
                            cameraControl?.setZoomRatio(clamped)
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 12.dp)
                    )

                    // Right Side: Interactive Vertical Zoom Slider (Slide UP = Zoom OUT, Slide DOWN = Zoom IN)
                    RightVerticalZoomSlider(
                        currentZoom = currentZoom,
                        maxZoomRatio = maxZoomRatio,
                        onZoomChanged = { targetZoom ->
                            val clamped = targetZoom.coerceIn(1f, maxZoomRatio)
                            currentZoom = clamped
                            cameraControl?.setZoomRatio(clamped)
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                    )

                    // Bottom Confirmation Card (Live when ID detected)
                    AnimatedVisibility(
                        visible = activeCandidate != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut(),
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(16.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 14.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                Brush.horizontalGradient(listOf(Color(0xFF16A34A), Color(0xFF38BDF8)))
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .background(Color(0xFF16A34A), CircleShape)
                                        )
                                        Text(
                                            text = "ID Terdeteksi (Live)",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    if (activeCandidate?.isFromUrl == true) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFF0284C7))
                                        ) {
                                            Text(
                                                text = "Dari Link Website",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 10.sp
                                                ),
                                                color = Color(0xFF0284C7)
                                            )
                                        }
                                    }
                                }

                                // Large ID display Box
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 10.dp, horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Apakah ID ini benar?",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                            Text(
                                                text = activeId ?: "",
                                                style = MaterialTheme.typography.headlineMedium.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontFamily = FontFamily.Monospace,
                                                    letterSpacing = 2.sp
                                                ),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.QrCodeScanner,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                // Live alternative numbers detected in the current frame
                                if (liveCandidates.size > 1) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.Start
                                    ) {
                                        Text(
                                            text = "Angka lain di dalam frame kamera:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            liveCandidates.forEach { candidate ->
                                                val isSelected = candidate.id == activeId
                                                FilterChip(
                                                    selected = isSelected,
                                                    onClick = {
                                                        userSelectedId = candidate.id
                                                    },
                                                    label = {
                                                        Text(
                                                            text = candidate.id,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    },
                                                    leadingIcon = if (isSelected) {
                                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                                                    } else null,
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Checkmark Confirmation Action Button
                                Button(
                                    onClick = {
                                        activeId?.let { id ->
                                            triggerVibration(context)
                                            onIdSelected(id)
                                            onDismiss()
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF16A34A),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Konfirmasi ID",
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "Gunakan ID Listing (${activeId ?: ""})",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Left Side Zoom Control Panel with vertical presets
 */
@Composable
private fun LeftZoomControlPanel(
    currentZoom: Float,
    maxZoomRatio: Float,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Black.copy(alpha = 0.65f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Preset Zoom Buttons (10x, 5x, 2x, 1x)
            val presets = listOf(10f, 5f, 2f, 1f).filter { it <= maxZoomRatio + 0.5f || it == 1f }
            presets.forEach { presetLevel ->
                val isSelected = (currentZoom >= presetLevel - 0.4f && currentZoom <= presetLevel + 0.4f)
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) Color(0xFF2563EB) else Color.White.copy(alpha = 0.12f)
                        )
                        .clickable {
                            onZoomChanged(presetLevel)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${presetLevel.toInt()}x",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * Right Side Vertical Zoom Slider:
 * Slide UP -> Zoom OUT (towards 1x)
 * Slide DOWN -> Zoom IN (towards maxZoomRatio)
 */
@Composable
private fun RightVerticalZoomSlider(
    currentZoom: Float,
    maxZoomRatio: Float,
    onZoomChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val sliderHeight = 220.dp
    val sliderWidth = 44.dp

    // Zoom fraction: 0f = 1x (Top), 1f = maxZoomRatio (Bottom)
    val zoomFraction = remember(currentZoom, maxZoomRatio) {
        ((currentZoom - 1f) / (maxZoomRatio - 1f)).coerceIn(0f, 1f)
    }

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color.Black.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
        modifier = modifier
            .width(sliderWidth)
            .height(sliderHeight)
            .pointerInput(maxZoomRatio) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val y = change.position.y.coerceIn(0f, size.height.toFloat())
                    // y = 0 is Top (1x / Zoom Out), y = size.height is Bottom (Max / Zoom In)
                    val fraction = (y / size.height.toFloat()).coerceIn(0f, 1f)
                    val targetZoom = 1f + fraction * (maxZoomRatio - 1f)
                    onZoomChanged(targetZoom)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            // Track line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
                    .align(Alignment.Center)
            )

            // Top Label (Zoom Out indicator)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.TopCenter)
            ) {
                Icon(
                    imageVector = Icons.Default.ZoomOut,
                    contentDescription = "Zoom Out (Geser Ke Atas)",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "1x",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Bottom Label (Zoom In indicator)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Text(
                    text = "${maxZoomRatio.toInt()}x",
                    color = Color(0xFF38BDF8),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ZoomIn,
                    contentDescription = "Zoom In (Geser Ke Bawah)",
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Draggable Thumb Indicator
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = ((sliderHeight - 48.dp) * zoomFraction))
                        .size(34.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF2563EB)))
                        )
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", currentZoom)}x",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

/**
 * 3D Live Perspective Tracking Overlay on Camera View
 * Maps 4 corner points in 3D perspective space directly to screen coordinates with animated glowing border.
 */
@Composable
private fun Live3DIdTrackingOverlay(
    activeCandidate: ScannedIdCandidate?,
    otherCandidates: List<ScannedIdCandidate>
) {
    val infiniteTransition = rememberInfiniteTransition(label = "TrackingPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val canvasW = size.width
        val canvasH = size.height

        // Draw 3D tracking box on other candidates (subtle cyan border)
        otherCandidates.forEach { candidate ->
            drawCandidate3DQuad(
                candidate = candidate,
                canvasWidth = canvasW,
                canvasHeight = canvasH,
                borderColor = Color(0xFF38BDF8).copy(alpha = 0.65f),
                fillColor = Color(0xFF38BDF8).copy(alpha = 0.08f),
                strokeWidth = 2.dp.toPx(),
                scaleFactor = 1f
            )
        }

        // Draw primary active candidate tracking 3D box (vibrant green with pulse and corner markers)
        if (activeCandidate != null) {
            drawCandidate3DQuad(
                candidate = activeCandidate,
                canvasWidth = canvasW,
                canvasHeight = canvasH,
                borderColor = Color(0xFF22C55E),
                fillColor = Color(0xFF22C55E).copy(alpha = 0.16f),
                strokeWidth = 3.dp.toPx(),
                scaleFactor = pulseScale
            )
        }
    }
}

/**
 * Draws a 3D perspective quad (with true projector tilt) or bounding box on Canvas
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCandidate3DQuad(
    candidate: ScannedIdCandidate,
    canvasWidth: Float,
    canvasHeight: Float,
    borderColor: Color,
    fillColor: Color,
    strokeWidth: Float,
    scaleFactor: Float
) {
    val rot = candidate.rotationDegrees
    val imgW = if (rot == 90 || rot == 270) candidate.imageHeight else candidate.imageWidth
    val imgH = if (rot == 90 || rot == 270) candidate.imageWidth else candidate.imageHeight

    if (imgW <= 0 || imgH <= 0) return

    val scale = max(canvasWidth / imgW.toFloat(), canvasHeight / imgH.toFloat())
    val scaledW = imgW * scale
    val scaledH = imgH * scale
    val offsetX = (canvasWidth - scaledW) / 2f
    val offsetY = (canvasHeight - scaledH) / 2f

    val corners = candidate.cornerPoints
    if (corners != null && corners.size == 4) {
        // Map 4 corner points to Screen Coordinates
        val pts = corners.map { p ->
            Offset(p.x * scale + offsetX, p.y * scale + offsetY)
        }

        // Compute centroid for scaling
        val cx = pts.map { it.x }.average().toFloat()
        val cy = pts.map { it.y }.average().toFloat()

        // Expand points slightly with scaleFactor
        val scaledPts = pts.map { p ->
            Offset(
                cx + (p.x - cx) * scaleFactor * 1.05f,
                cy + (p.y - cy) * scaleFactor * 1.05f
            )
        }

        val path = Path().apply {
            moveTo(scaledPts[0].x, scaledPts[0].y)
            lineTo(scaledPts[1].x, scaledPts[1].y)
            lineTo(scaledPts[2].x, scaledPts[2].y)
            lineTo(scaledPts[3].x, scaledPts[3].y)
            close()
        }

        // 3D translucent plane fill
        drawPath(path, color = fillColor)
        // 3D perimeter stroke
        drawPath(path, color = borderColor, style = Stroke(width = strokeWidth))

        // 3D corner bracket highlights
        for (i in 0..3) {
            val curr = scaledPts[i]
            val prev = scaledPts[(i + 3) % 4]
            val next = scaledPts[(i + 1) % 4]

            val v1 = (next - curr) * 0.35f
            val v2 = (prev - curr) * 0.35f

            drawLine(borderColor, curr, curr + v1, strokeWidth * 1.5f)
            drawLine(borderColor, curr, curr + v2, strokeWidth * 1.5f)
        }
    } else {
        // Fallback to bounding box
        val box = candidate.boundingBox ?: return
        val rawLeft = box.left * scale + offsetX
        val rawTop = box.top * scale + offsetY
        val rawW = (box.right - box.left) * scale
        val rawH = (box.bottom - box.top) * scale

        val padding = 6.dp.toPx()
        val paddedW = (rawW + padding * 2) * scaleFactor
        val paddedH = (rawH + padding * 2) * scaleFactor
        val paddedLeft = rawLeft - padding - (paddedW - (rawW + padding * 2)) / 2f
        val paddedTop = rawTop - padding - (paddedH - (rawH + padding * 2)) / 2f

        if (paddedW > 0 && paddedH > 0) {
            drawRoundRect(
                color = fillColor,
                topLeft = Offset(paddedLeft, paddedTop),
                size = Size(paddedW, paddedH),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
            drawRoundRect(
                color = borderColor,
                topLeft = Offset(paddedLeft, paddedTop),
                size = Size(paddedW, paddedH),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                style = Stroke(width = strokeWidth)
            )
        }
    }
}

/**
 * Intelligent parser to extract Listing IDs and their 3D Corner Points
 */
fun extractIdCandidatesWithBoxes(
    visionText: Text,
    imageWidth: Int,
    imageHeight: Int,
    rotationDegrees: Int
): List<ScannedIdCandidate> {
    val candidates = mutableListOf<ScannedIdCandidate>()
    val seenIds = mutableSetOf<String>()

    for (block in visionText.textBlocks) {
        for (line in block.lines) {
            val lineText = line.text

            // 1. Check for URL patterns (e.g. website link ending in ID or containing /12193 or id=11091)
            val urlRegex = Regex("(?:https?://|www\\.|/[a-zA-Z0-9_-]+/)[^\\s]*?(?:/|=|id=|listing=)(\\d{3,8})(?:[^0-9a-zA-Z]|$)", RegexOption.IGNORE_CASE)
            urlRegex.findAll(lineText).forEach { match ->
                val id = match.groupValues[1]
                if (isValidId(id) && !seenIds.contains(id)) {
                    candidates.add(
                        ScannedIdCandidate(
                            id = id,
                            isFromUrl = true,
                            cornerPoints = line.cornerPoints?.toList(),
                            boundingBox = line.boundingBox,
                            imageWidth = imageWidth,
                            imageHeight = imageHeight,
                            rotationDegrees = rotationDegrees
                        )
                    )
                    seenIds.add(id)
                }
            }

            // 2. Token based search for URL end digits
            val tokenRegex = Regex("(?:https?://[^\\s]+|/[^\\s]+)")
            tokenRegex.findAll(lineText).forEach { match ->
                val token = match.value
                val endDigits = Regex("(\\d{3,8})\\D*$").find(token)?.groupValues?.get(1)
                if (endDigits != null && isValidId(endDigits) && !seenIds.contains(endDigits)) {
                    candidates.add(
                        ScannedIdCandidate(
                            id = endDigits,
                            isFromUrl = true,
                            cornerPoints = line.cornerPoints?.toList(),
                            boundingBox = line.boundingBox,
                            imageWidth = imageWidth,
                            imageHeight = imageHeight,
                            rotationDegrees = rotationDegrees
                        )
                    )
                    seenIds.add(endDigits)
                }
            }

            // 3. Standalone numbers with element corner points
            for (element in line.elements) {
                val elemText = element.text.trim()
                val numberMatch = Regex("^\\D*(\\d{4,7})\\D*$").find(elemText)
                if (numberMatch != null) {
                    val id = numberMatch.groupValues[1]
                    if (isValidId(id) && !seenIds.contains(id)) {
                        candidates.add(
                            ScannedIdCandidate(
                                id = id,
                                isFromUrl = false,
                                cornerPoints = element.cornerPoints?.toList() ?: line.cornerPoints?.toList(),
                                boundingBox = element.boundingBox ?: line.boundingBox,
                                imageWidth = imageWidth,
                                imageHeight = imageHeight,
                                rotationDegrees = rotationDegrees
                            )
                        )
                        seenIds.add(id)
                    }
                }
            }

            // 4. Standalone 4-7 digit number across the line
            val standaloneNumberRegex = Regex("\\b(\\d{4,7})\\b")
            standaloneNumberRegex.findAll(lineText).forEach { match ->
                val id = match.groupValues[1]
                if (isValidId(id) && !seenIds.contains(id)) {
                    candidates.add(
                        ScannedIdCandidate(
                            id = id,
                            isFromUrl = false,
                            cornerPoints = line.cornerPoints?.toList(),
                            boundingBox = line.boundingBox,
                            imageWidth = imageWidth,
                            imageHeight = imageHeight,
                            rotationDegrees = rotationDegrees
                        )
                    )
                    seenIds.add(id)
                }
            }
        }
    }

    return candidates.sortedWith(
        compareByDescending<ScannedIdCandidate> { it.isFromUrl }
            .thenByDescending { it.id.length in 4..6 }
    )
}

/**
 * Filter out invalid noise numbers (e.g. years 2024-2028, timestamps, dates)
 */
private fun isValidId(id: String): Boolean {
    if (id.length !in 3..8) return false
    if (id in listOf("2023", "2024", "2025", "2026", "2027", "2028")) return false
    if (id.all { it == id.first() }) return false
    return true
}

/**
 * Intelligent parser fallback for raw text strings (used in unit testing)
 */
fun extractIdCandidates(rawText: String): List<ScannedIdCandidate> {
    if (rawText.isBlank()) return emptyList()

    val candidates = mutableListOf<ScannedIdCandidate>()
    val seenIds = mutableSetOf<String>()

    val urlRegex = Regex("(?:https?://|www\\.|/[a-zA-Z0-9_-]+/)[^\\s]*?(?:/|=|id=|listing=)(\\d{3,8})(?:[^0-9a-zA-Z]|$)", RegexOption.IGNORE_CASE)
    urlRegex.findAll(rawText).forEach { match ->
        val id = match.groupValues[1]
        if (isValidId(id) && !seenIds.contains(id)) {
            candidates.add(ScannedIdCandidate(id = id, isFromUrl = true))
            seenIds.add(id)
        }
    }

    val standaloneNumberRegex = Regex("\\b(\\d{4,7})\\b")
    standaloneNumberRegex.findAll(rawText).forEach { match ->
        val id = match.groupValues[1]
        if (isValidId(id) && !seenIds.contains(id)) {
            candidates.add(ScannedIdCandidate(id = id, isFromUrl = false))
            seenIds.add(id)
        }
    }

    return candidates.sortedWith(
        compareByDescending<ScannedIdCandidate> { it.isFromUrl }
            .thenByDescending { it.id.length in 4..6 }
    )
}

/**
 * Triggers a subtle tactile haptic vibration when an ID is confirmed
 */
private fun triggerVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            @Suppress("DEPRECATION")
            vibrator?.vibrate(40)
        }
    } catch (e: Exception) {
        // Ignore vibration failure
    }
}
