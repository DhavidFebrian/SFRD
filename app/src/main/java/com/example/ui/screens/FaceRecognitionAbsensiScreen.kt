package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.AGENT_CONTACT_LIST
import com.example.ui.ScheduleViewModel
import com.example.util.DetectedPerson
import com.example.util.DetectionType
import com.example.util.FaceRecognitionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Full-screen camera view with real-time face recognition and body detection
 * for automated attendance. Uses the BACK camera with pinch-to-zoom.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceRecognitionAbsensiScreen(
    viewModel: ScheduleViewModel,
    selectedDateIdx: Int,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    var detectedPersons by remember { mutableStateOf<List<DetectedPerson>>(emptyList()) }
    var isLoadingRefs by remember { mutableStateOf(true) }
    var loadingMessage by remember { mutableStateOf("Mempersiapkan face recognition...") }

    // Zoom state
    var currentZoom by remember { mutableFloatStateOf(1f) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }

    // Attendance session
    val sessionAttendance = remember { mutableStateMapOf<String, String>() }

    // Helper
    val faceHelper = remember { FaceRecognitionHelper(context) }
    val isProcessing = remember { AtomicBoolean(false) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    // Permission
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // Load reference faces — from cache if available
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect

        isLoadingRefs = true
        val absensiData = viewModel.absensiData.value
        if (absensiData == null) {
            loadingMessage = "Data absensi belum dimuat. Kembali dan refresh dulu."
            return@LaunchedEffect
        }

        loadingMessage = "Memuat data wajah..."

        val agents = absensiData.marketingList.map { marketing ->
            val avatarUrl = viewModel.getAgentAvatarByName(marketing.name) ?: run {
                val cleanName = marketing.name.trim().lowercase()
                AGENT_CONTACT_LIST.find { agent ->
                    cleanName.contains(agent.nameKey) || agent.nameKey.contains(cleanName)
                }?.avatarUrl ?: ""
            }
            Triple(marketing.name, avatarUrl, marketing.row)
        }

        withContext(Dispatchers.IO) {
            faceHelper.loadReferenceFaces(agents)
        }

        loadingMessage = if (faceHelper.loadedCount > 0) {
            "Siap! ${faceHelper.loadedCount} wajah dimuat."
        } else {
            "Gagal memuat data wajah. Coba kembali dan refresh."
        }
        isLoadingRefs = false
    }

    DisposableEffect(Unit) {
        onDispose {
            faceHelper.release()
            cameraExecutor.shutdown()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Face Scan Absensi",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        val absensiData = viewModel.absensiData.value
                        val dateLabel = absensiData?.dates?.getOrNull(selectedDateIdx)?.label ?: ""
                        if (dateLabel.isNotBlank()) {
                            Text(dateLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Kembali") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.85f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->

        if (!hasCameraPermission) {
            Box(Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(32.dp)) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("Izin kamera diperlukan.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Berikan Izin Kamera") }
                }
            }
            return@Scaffold
        }

        Box(Modifier.fillMaxSize().padding(paddingValues).background(Color.Black)) {

            // Camera Preview with pinch-to-zoom
            if (!isLoadingRefs) {
                val absensiData by viewModel.absensiData.collectAsStateWithLifecycle()

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val provider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                                    .build()

                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                    if (!isProcessing.compareAndSet(false, true)) {
                                        imageProxy.close()
                                        return@setAnalyzer
                                    }
                                    coroutineScope.launch {
                                        try {
                                            faceHelper.processFrame(imageProxy) { results ->
                                                detectedPersons = results

                                                // Auto check-in for confirmed faces
                                                val data = absensiData
                                                val dateColIndex = data?.dates?.getOrNull(selectedDateIdx)?.colIndex

                                                if (dateColIndex != null) {
                                                    for (person in results) {
                                                        if (person.type == DetectionType.FACE_RECOGNIZED &&
                                                            person.row > 0 &&
                                                            !faceHelper.checkedInNames.contains(person.name)
                                                        ) {
                                                            val marketing = data?.marketingList?.find { it.row == person.row }
                                                            val alreadyAttended = marketing?.attendance?.getOrNull(selectedDateIdx) ?: false

                                                            if (!alreadyAttended) {
                                                                faceHelper.checkedInNames.add(person.name)
                                                                val ts = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                                                                sessionAttendance[person.name] = ts

                                                                viewModel.updateAbsensiMeeting(
                                                                    row = person.row,
                                                                    col = dateColIndex,
                                                                    present = true,
                                                                    onResult = { success, msg, _, _ ->
                                                                        val display = person.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                                                                        if (success) {
                                                                            Toast.makeText(context, "✓ $display — Absensi berhasil!", Toast.LENGTH_SHORT).show()
                                                                        } else {
                                                                            faceHelper.checkedInNames.remove(person.name)
                                                                            sessionAttendance.remove(person.name)
                                                                            Toast.makeText(context, "✗ $display — $msg", Toast.LENGTH_SHORT).show()
                                                                        }
                                                                    }
                                                                )
                                                            } else {
                                                                faceHelper.checkedInNames.add(person.name)
                                                                if (!sessionAttendance.containsKey(person.name)) {
                                                                    sessionAttendance[person.name] = "Sudah hadir"
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } finally { isProcessing.set(false) }
                                    }
                                }

                                provider.unbindAll()
                                val camera = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControl = camera.cameraControl
                                cameraInfo = camera.cameraInfo

                            } catch (e: Exception) {
                                Log.e("FaceRecogScreen", "Camera bind failed: ${e.message}")
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, _, zoom, _ ->
                                val camInfo = cameraInfo ?: return@detectTransformGestures
                                val maxZoom = camInfo.zoomState.value?.maxZoomRatio ?: 10f
                                val minZoom = camInfo.zoomState.value?.minZoomRatio ?: 1f
                                val newZoom = (currentZoom * zoom).coerceIn(minZoom, maxZoom)
                                currentZoom = newZoom
                                cameraControl?.setZoomRatio(newZoom)
                            }
                        }
                )

                // Detection overlay
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cW = size.width
                    val cH = size.height

                    for (person in detectedPersons) {
                        val box = person.boundingBox
                        val left = box.left * cW
                        val top = box.top * cH
                        val right = box.right * cW
                        val bottom = box.bottom * cH
                        val w = right - left
                        val h = bottom - top

                        val (boxColor, labelText) = when (person.type) {
                            DetectionType.FACE_RECOGNIZED -> {
                                val isCheckedIn = faceHelper.checkedInNames.contains(person.name.removeSuffix("?"))
                                if (isCheckedIn) {
                                    Color(0xFF4CAF50) to "✓ ${person.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
                                } else {
                                    Color(0xFF2196F3) to "▶ ${person.name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
                                }
                            }
                            DetectionType.FACE_SCANNING -> {
                                Color(0xFFFFC107) to "${person.name.removeSuffix("?").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}?"
                            }
                            DetectionType.FACE_UNKNOWN -> {
                                Color(0xFFFF5722) to "?"
                            }
                            DetectionType.BODY_ONLY -> {
                                Color(0xFFFF9800) to "🧍 Orang terdeteksi"
                            }
                        }

                        val confidence = if (person.confidence > 0f) {
                            val scoreOutOfTen = String.format(Locale.US, "%.1f", person.confidence * 10f)
                            " ($scoreOutOfTen/10)"
                        } else ""

                        // Bounding box
                        val strokeStyle = if (person.type == DetectionType.BODY_ONLY) {
                            Stroke(width = 3f, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 10f)))
                        } else {
                            Stroke(width = 4f)
                        }

                        drawRoundRect(
                            color = boxColor,
                            topLeft = Offset(left, top),
                            size = Size(w, h),
                            cornerRadius = CornerRadius(12f, 12f),
                            style = strokeStyle
                        )

                        // Corner accents (only for face detections)
                        if (person.type != DetectionType.BODY_ONLY) {
                            val cLen = minOf(w, h) * 0.2f
                            val cs = 8f
                            drawLine(boxColor, Offset(left, top), Offset(left + cLen, top), cs)
                            drawLine(boxColor, Offset(left, top), Offset(left, top + cLen), cs)
                            drawLine(boxColor, Offset(right, top), Offset(right - cLen, top), cs)
                            drawLine(boxColor, Offset(right, top), Offset(right, top + cLen), cs)
                            drawLine(boxColor, Offset(left, bottom), Offset(left + cLen, bottom), cs)
                            drawLine(boxColor, Offset(left, bottom), Offset(left, bottom - cLen), cs)
                            drawLine(boxColor, Offset(right, bottom), Offset(right - cLen, bottom), cs)
                            drawLine(boxColor, Offset(right, bottom), Offset(right, bottom - cLen), cs)
                        }

                        // Label
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 34f
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.DEFAULT_BOLD
                            setShadowLayer(4f, 2f, 2f, android.graphics.Color.BLACK)
                        }
                        val textW = paint.measureText("$labelText$confidence")
                        val lPad = 8f
                        val lH = 44f

                        drawRoundRect(
                            color = boxColor.copy(alpha = 0.8f),
                            topLeft = Offset(left, top - lH - 4f),
                            size = Size(maxOf(textW + lPad * 2, w), lH),
                            cornerRadius = CornerRadius(8f, 8f)
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "$labelText$confidence", left + lPad, top - 14f, paint
                        )
                    }
                }
            }

            // Loading overlay
            AnimatedVisibility(visible = isLoadingRefs, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(32.dp)) {
                        CircularProgressIndicator(Modifier.size(56.dp), color = Color(0xFF4CAF50), strokeWidth = 5.dp)
                        Text(loadingMessage, style = MaterialTheme.typography.bodyLarge, color = Color.White, textAlign = TextAlign.Center)
                        if (faceHelper.totalCount > 0) {
                            LinearProgressIndicator(
                                progress = { faceHelper.loadedCount.toFloat() / faceHelper.totalCount },
                                modifier = Modifier.fillMaxWidth(0.7f).height(6.dp).clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF4CAF50), trackColor = Color.White.copy(alpha = 0.2f)
                            )
                            Text("${faceHelper.loadedCount} / ${faceHelper.totalCount} wajah", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                        }
                    }
                }
            }

            // Zoom indicator
            if (!isLoadingRefs && currentZoom > 1.05f) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = "🔍 ${String.format("%.1f", currentZoom)}x",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Attendance summary panel
            if (!isLoadingRefs && sessionAttendance.isNotEmpty()) {
                Card(
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.85f))
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("✓ Tercatat via Face Scan", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color(0xFF4CAF50))
                            Surface(color = Color(0xFF4CAF50), shape = RoundedCornerShape(8.dp)) {
                                Text("${sessionAttendance.size} orang", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.White), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        val entries = sessionAttendance.entries.toList()
                        entries.takeLast(4).forEach { (name, time) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("✓ ${name.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                Text(time, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                            }
                        }
                        if (entries.size > 4) Text("+${entries.size - 4} lainnya...", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.4f))
                    }
                }
            }

            // Status indicator
            if (!isLoadingRefs) {
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 8.dp),
                    color = if (detectedPersons.isNotEmpty()) Color(0xFF4CAF50).copy(alpha = 0.85f) else Color.Gray.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (detectedPersons.isNotEmpty()) Color.White else Color.White.copy(alpha = 0.5f)))
                        val faceCount = detectedPersons.count { it.type != DetectionType.BODY_ONLY }
                        val bodyCount = detectedPersons.count { it.type == DetectionType.BODY_ONLY }
                        val statusText = buildString {
                            if (faceCount > 0) append("$faceCount wajah")
                            if (bodyCount > 0) {
                                if (faceCount > 0) append(", ")
                                append("$bodyCount badan")
                            }
                            if (faceCount == 0 && bodyCount == 0) append("Scanning...")
                        }
                        Text(statusText, style = MaterialTheme.typography.labelSmall, color = Color.White)
                    }
                }
            }
        }
    }
}
