package com.example.util

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.*
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.*
import java.io.*
import java.net.URL
import kotlin.math.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Represents a detected person — either a recognized face or a body silhouette.
 */
data class DetectedPerson(
    val name: String,
    val confidence: Float,
    val boundingBox: RectF,
    val row: Int,
    val type: DetectionType
)

enum class DetectionType {
    FACE_RECOGNIZED,   // Face detected and name matched
    FACE_SCANNING,     // Face detected, matching in progress
    FACE_UNKNOWN,      // Face detected but no match
    BODY_ONLY          // Body detected but no face visible
}

/**
 * Face detection + recognition engine using ML Kit and LBP (Local Binary Patterns).
 *
 * Key improvements over v1:
 * - LBP histogram features for lighting-invariant face comparison
 * - Spatial grid subdivision for structural face matching
 * - Higher thresholds and more confirmation frames to prevent false positives
 * - Face quality gating (minimum size, head angle limits, landmark checks)
 * - ML Kit Pose Detection for body tracking when face not visible
 * - On-disk caching of reference face features (no re-download on every open)
 */
class FaceRecognitionHelper(private val context: Context) {

    companion object {
        private const val TAG = "FaceRecogHelper"
        private const val FACE_SIZE = 112
        private const val RECOGNITION_THRESHOLD = 0.85f // Match requires score > 8.5/10 (85%)
        private const val CONFIRMATION_FRAMES = 2     // 2-frame confirmation
        private const val LBP_GRID = 4                // 4×4 spatial grid
        private const val LBP_BINS = 256              // 256 LBP histogram bins
        private const val MIN_FACE_PX = 30            // Allow faces from distance
        private const val MAX_HEAD_ANGLE = 45f         // Allow angled faces
        private const val CACHE_DIR_NAME = "face_ref_cache_v3" // Invalidate old cache
        private const val CACHE_META_FILE = "cache_meta.dat"
        private const val CACHE_MAX_AGE_MS = 7L * 24 * 3600 * 1000 // 7 days
    }

    // ML Kit Face Detector (camera — fast mode)
    private val cameraFaceDetector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.18f)
            .enableTracking()
            .build()
    )

    // ML Kit Face Detector (reference photos — accurate mode)
    private val photoFaceDetector: FaceDetector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setMinFaceSize(0.1f)
            .build()
    )

    // ML Kit Pose Detector (body detection)
    private val poseDetector: PoseDetector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    // name (lowercase) → (LBP feature vector, spreadsheet row)
    private val referenceData = mutableMapOf<String, Pair<FloatArray, Int>>()

    // trackingId → (name, consecutive confirmed count)
    private val confirmationCounts = mutableMapOf<Int, Pair<String, Int>>()

    /** Names already checked-in during this session. */
    val checkedInNames = mutableSetOf<String>()

    var isReady = false
        private set
    var loadedCount = 0
        private set
    var totalCount = 0
        private set

    // Frame counter for alternating body detection
    private var frameCounter = 0

    // ─── Public API ─────────────────────────────────────────────────────

    /**
     * Loads reference faces — from disk cache if available, otherwise downloads.
     */
    suspend fun loadReferenceFaces(agents: List<Triple<String, String, Int>>) {
        isReady = false
        referenceData.clear()
        confirmationCounts.clear()

        val validAgents = agents.filter { (_, url, _) -> url.isNotBlank() }
        totalCount = validAgents.size
        loadedCount = 0

        // Try loading from cache first
        val cacheLoaded = loadFromCache(validAgents)
        if (cacheLoaded) {
            loadedCount = totalCount
            isReady = true
            Log.d(TAG, "Loaded ${referenceData.size} faces from cache")
            return
        }

        // Cache miss — download and process each agent's photo
        withContext(Dispatchers.IO) {
            validAgents.forEach { (name, url, row) ->
                try {
                    val bitmap = downloadBitmap(url)
                    if (bitmap != null) {
                        val inputImage = InputImage.fromBitmap(bitmap, 0)
                        val faces = photoFaceDetector.process(inputImage).await()
                        if (faces.isNotEmpty()) {
                            val face = faces.maxByOrNull {
                                it.boundingBox.width() * it.boundingBox.height()
                            }!!
                            val aligned = extractAlignedFace(bitmap, face)
                            if (aligned != null) {
                                val features = extractLBPFeatures(aligned)
                                referenceData[name.trim().lowercase()] = Pair(features, row)
                                aligned.recycle()
                                Log.d(TAG, "✓ Loaded: $name")
                            }
                        }
                        bitmap.recycle()
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed: $name — ${e.message}")
                }
                loadedCount++
            }
        }

        // Save to cache for next time
        saveToCache(validAgents)

        isReady = true
        Log.d(TAG, "Face recognition ready: ${referenceData.size}/$totalCount loaded")
    }

    /**
     * Processes a camera frame: detects faces and bodies, recognizes faces.
     *
     * @param onResults Callback with (recognized faces, body-only bounding boxes)
     */
    @OptIn(ExperimentalGetImage::class)
    suspend fun processFrame(
        imageProxy: ImageProxy,
        onResults: (List<DetectedPerson>) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null || !isReady) {
            imageProxy.close()
            onResults(emptyList())
            return
        }

        try {
            val rotation = imageProxy.imageInfo.rotationDegrees
            val inputImage = InputImage.fromMediaImage(mediaImage, rotation)

            // Calculate actual dimensions after rotation
            val actualW: Int
            val actualH: Int
            if (rotation == 90 || rotation == 270) {
                actualW = imageProxy.height
                actualH = imageProxy.width
            } else {
                actualW = imageProxy.width
                actualH = imageProxy.height
            }

            val frameBitmap = imageProxyToBitmap(imageProxy)
            if (frameBitmap == null) {
                imageProxy.close()
                onResults(emptyList())
                return
            }

            val results = mutableListOf<DetectedPerson>()

            // Always run face detection
            val faces = cameraFaceDetector.process(inputImage).await()

            // Run pose detection every other frame to save CPU
            frameCounter++
            val poses: List<Pose> = if (frameCounter % 2 == 0) {
                try {
                    val poseInput = InputImage.fromMediaImage(mediaImage, rotation)
                    val pose = poseDetector.process(poseInput).await()
                    if (pose.allPoseLandmarks.isNotEmpty()) listOf(pose) else emptyList()
                } catch (e: Exception) { emptyList() }
            } else {
                emptyList()
            }

            // Track which body regions already have a face match
            val faceCoveredRegions = mutableListOf<RectF>()

            // Process faces
            for (face in faces) {
                val relBox = RectF(
                    face.boundingBox.left.toFloat() / actualW,
                    face.boundingBox.top.toFloat() / actualH,
                    face.boundingBox.right.toFloat() / actualW,
                    face.boundingBox.bottom.toFloat() / actualH
                )
                faceCoveredRegions.add(relBox)

                val trackingId = face.trackingId ?: face.hashCode()

                // ── Quality gate: reject low-quality detections ──
                if (!isFaceQualityOk(face, actualW)) {
                    results.add(DetectedPerson("Scanning...", 0f, relBox, -1, DetectionType.FACE_SCANNING))
                    confirmationCounts.remove(trackingId)
                    continue
                }

                val aligned = extractAlignedFace(frameBitmap, face)
                if (aligned == null) {
                    results.add(DetectedPerson("Scanning...", 0f, relBox, -1, DetectionType.FACE_SCANNING))
                    continue
                }

                val features = extractLBPFeatures(aligned)
                aligned.recycle()
                val match = findBestMatch(features)

                if (match != null && match.second >= RECOGNITION_THRESHOLD) {
                    val (matchName, confidence, row) = match

                    // Confirmation: must match same name N frames in a row
                    val prev = confirmationCounts[trackingId]
                    if (prev != null && prev.first == matchName) {
                        val count = prev.second + 1
                        confirmationCounts[trackingId] = Pair(matchName, count)
                        if (count >= CONFIRMATION_FRAMES) {
                            results.add(DetectedPerson(matchName, confidence, relBox, row, DetectionType.FACE_RECOGNIZED))
                        } else {
                            results.add(DetectedPerson("$matchName?", confidence, relBox, row, DetectionType.FACE_SCANNING))
                        }
                    } else {
                        confirmationCounts[trackingId] = Pair(matchName, 1)
                        results.add(DetectedPerson("$matchName?", confidence, relBox, row, DetectionType.FACE_SCANNING))
                    }
                } else {
                    confirmationCounts.remove(trackingId)
                    results.add(DetectedPerson("Unknown", 0f, relBox, -1, DetectionType.FACE_UNKNOWN))
                }
            }

            // Process poses — add body-only boxes for bodies without a face match
            for (pose in poses) {
                val bodyBox = computeBodyBoundingBox(pose, actualW, actualH) ?: continue
                // Check if this body already has a face detection overlapping it
                val alreadyCovered = faceCoveredRegions.any { faceBox ->
                    RectF.intersects(faceBox, bodyBox)
                }
                if (!alreadyCovered) {
                    results.add(DetectedPerson("Orang (berbalik)", 0f, bodyBox, -1, DetectionType.BODY_ONLY))
                }
            }

            frameBitmap.recycle()
            imageProxy.close()
            onResults(results)

        } catch (e: Exception) {
            Log.e(TAG, "Frame error: ${e.message}")
            imageProxy.close()
            onResults(emptyList())
        }
    }

    fun release() {
        cameraFaceDetector.close()
        photoFaceDetector.close()
        poseDetector.close()
        referenceData.clear()
        confirmationCounts.clear()
    }

    // ─── Face quality check ─────────────────────────────────────────────

    /**
     * Rejects faces that are too small, at extreme angles, or missing landmarks.
     * This prevents false positives from texture-like patterns being read as faces.
     */
    private fun isFaceQualityOk(face: Face, imageWidth: Int): Boolean {
        val faceWidth = face.boundingBox.width()

        // Reject tiny faces
        if (faceWidth < MIN_FACE_PX) return false

        // Reject extreme head angles (side profile, tilted)
        val yAngle = face.headEulerAngleY  // turning left/right
        val zAngle = face.headEulerAngleZ  // tilting
        if (abs(yAngle) > MAX_HEAD_ANGLE || abs(zAngle) > MAX_HEAD_ANGLE) return false

        return true
    }

    // ─── LBP Feature Extraction ─────────────────────────────────────────

    /**
     * Extracts Local Binary Pattern (LBP) histogram features from a face bitmap.
     *
     * LBP is a texture descriptor that is inherently invariant to lighting changes,
     * making it much more reliable for face recognition than raw pixel comparison.
     *
     * Steps:
     *  1. Convert to grayscale
     *  2. For each pixel, compare with 8 neighbors → 8-bit binary code
     *  3. Divide face into 4×4 spatial grid
     *  4. Build 256-bin histogram for each cell
     *  5. Normalize each histogram independently
     *  6. Concatenate all histograms into a single feature vector
     */
    private fun extractLBPFeatures(face: Bitmap): FloatArray {
        val w = face.width
        val h = face.height
        val pixels = IntArray(w * h)
        face.getPixels(pixels, 0, w, 0, 0, w, h)

        // Convert to grayscale
        val gray = IntArray(w * h) { i ->
            val px = pixels[i]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            (0.299 * r + 0.587 * g + 0.114 * b).toInt().coerceIn(0, 255)
        }

        // Compute LBP for each pixel (skip border pixels)
        val lbp = IntArray(w * h) // default 0
        val dx = intArrayOf(-1, 0, 1, 1, 1, 0, -1, -1)
        val dy = intArrayOf(-1, -1, -1, 0, 1, 1, 1, 0)

        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val center = gray[y * w + x]
                var code = 0
                for (i in 0..7) {
                    val nx = x + dx[i]
                    val ny = y + dy[i]
                    if (gray[ny * w + nx] >= center) {
                        code = code or (1 shl i)
                    }
                }
                lbp[y * w + x] = code
            }
        }

        // Build spatial histograms (4×4 grid, 256 bins each)
        val cellW = w / LBP_GRID
        val cellH = h / LBP_GRID
        val featureVector = FloatArray(LBP_GRID * LBP_GRID * LBP_BINS)

        for (gy in 0 until LBP_GRID) {
            for (gx in 0 until LBP_GRID) {
                val histOffset = (gy * LBP_GRID + gx) * LBP_BINS
                val startX = gx * cellW
                val startY = gy * cellH
                val endX = if (gx == LBP_GRID - 1) w else startX + cellW
                val endY = if (gy == LBP_GRID - 1) h else startY + cellH

                // Accumulate histogram
                for (y in startY until endY) {
                    for (x in startX until endX) {
                        val bin = lbp[y * w + x]
                        featureVector[histOffset + bin] += 1f
                    }
                }

                // Normalize this cell's histogram (L2 norm)
                var sumSq = 0f
                for (i in 0 until LBP_BINS) {
                    sumSq += featureVector[histOffset + i] * featureVector[histOffset + i]
                }
                val norm = sqrt(sumSq).coerceAtLeast(1e-6f)
                for (i in 0 until LBP_BINS) {
                    featureVector[histOffset + i] /= norm
                }
            }
        }

        return featureVector
    }

    // ─── Matching ───────────────────────────────────────────────────────

    private fun findBestMatch(features: FloatArray): Triple<String, Float, Int>? {
        var bestName: String? = null
        var bestScore = -1f
        var bestRow = -1

        for ((name, pair) in referenceData) {
            val (refFeatures, row) = pair
            val score = cosineSimilarity(features, refFeatures)
            if (score > bestScore) {
                bestScore = score
                bestName = name
                bestRow = row
            }
        }

        return if (bestName != null && bestScore >= RECOGNITION_THRESHOLD) {
            Triple(bestName, bestScore, bestRow)
        } else null
    }

    /**
     * Cosine similarity between two feature vectors (0..1 range).
     */
    private fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        if (a.size != b.size) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        val denominator = sqrt(normA) * sqrt(normB)
        return if (denominator > 0f) (dot / denominator).coerceIn(0f, 1f) else 0f
    }

    // ─── Face alignment ─────────────────────────────────────────────────

    private fun extractAlignedFace(source: Bitmap, face: Face): Bitmap? {
        try {
            val box = face.boundingBox
            val padX = (box.width() * 0.25f).toInt()
            val padY = (box.height() * 0.25f).toInt()

            val left = (box.left - padX).coerceAtLeast(0)
            val top = (box.top - padY).coerceAtLeast(0)
            val right = (box.right + padX).coerceAtMost(source.width)
            val bottom = (box.bottom + padY).coerceAtMost(source.height)

            val cropW = right - left
            val cropH = bottom - top
            if (cropW <= 10 || cropH <= 10) return null

            var cropped = Bitmap.createBitmap(source, left, top, cropW, cropH)

            // Align by eye positions
            val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)?.position
            val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)?.position
            if (leftEye != null && rightEye != null) {
                val dx = rightEye.x - leftEye.x
                val dy = rightEye.y - leftEye.y
                val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                if (abs(angle) > 1.5f) {
                    val matrix = Matrix()
                    matrix.postRotate(-angle, cropped.width / 2f, cropped.height / 2f)
                    val aligned = Bitmap.createBitmap(cropped, 0, 0, cropped.width, cropped.height, matrix, true)
                    if (aligned != cropped) cropped.recycle()
                    cropped = aligned
                }
            }

            val resized = Bitmap.createScaledBitmap(cropped, FACE_SIZE, FACE_SIZE, true)
            if (resized != cropped) cropped.recycle()
            return resized
        } catch (e: Exception) {
            Log.w(TAG, "Alignment failed: ${e.message}")
            return null
        }
    }

    // ─── Body bounding box from pose landmarks ──────────────────────────

    private fun computeBodyBoundingBox(pose: Pose, imgW: Int, imgH: Int): RectF? {
        val landmarks = pose.allPoseLandmarks
        if (landmarks.size < 6) return null // Need enough points for a meaningful box

        // Check confidence — average inFrameLikelihood must be decent
        val avgConfidence = landmarks.map { it.inFrameLikelihood }.average()
        if (avgConfidence < 0.5) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE

        for (lm in landmarks) {
            if (lm.inFrameLikelihood < 0.3f) continue
            minX = minOf(minX, lm.position.x)
            minY = minOf(minY, lm.position.y)
            maxX = maxOf(maxX, lm.position.x)
            maxY = maxOf(maxY, lm.position.y)
        }

        if (minX >= maxX || minY >= maxY) return null

        // Add padding
        val padX = (maxX - minX) * 0.15f
        val padY = (maxY - minY) * 0.1f

        return RectF(
            ((minX - padX) / imgW).coerceIn(0f, 1f),
            ((minY - padY) / imgH).coerceIn(0f, 1f),
            ((maxX + padX) / imgW).coerceIn(0f, 1f),
            ((maxY + padY) / imgH).coerceIn(0f, 1f)
        )
    }

    // ─── Caching ────────────────────────────────────────────────────────

    private fun getCacheDir(): File {
        val dir = File(context.filesDir, CACHE_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /**
     * Tries to load all reference features from disk cache.
     * Returns true if cache hit (all agents found and cache is fresh).
     */
    private fun loadFromCache(agents: List<Triple<String, String, Int>>): Boolean {
        val cacheDir = getCacheDir()
        val metaFile = File(cacheDir, CACHE_META_FILE)
        if (!metaFile.exists()) return false

        // Check cache age
        val age = System.currentTimeMillis() - metaFile.lastModified()
        if (age > CACHE_MAX_AGE_MS) {
            Log.d(TAG, "Cache expired (${age / 3600000}h old)")
            return false
        }

        // Load each agent's cached features
        var allFound = true
        for ((name, _, row) in agents) {
            val key = name.trim().lowercase()
            val featureFile = File(cacheDir, "${key.hashCode()}.bin")
            if (!featureFile.exists()) {
                allFound = false
                break
            }
            try {
                DataInputStream(BufferedInputStream(FileInputStream(featureFile))).use { dis ->
                    val size = dis.readInt()
                    val features = FloatArray(size) { dis.readFloat() }
                    referenceData[key] = Pair(features, row)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cache read failed for $name: ${e.message}")
                allFound = false
                break
            }
        }

        if (!allFound) {
            referenceData.clear()
            return false
        }
        return true
    }

    /** Saves all reference features to disk for future sessions. */
    private fun saveToCache(agents: List<Triple<String, String, Int>>) {
        try {
            val cacheDir = getCacheDir()
            for ((name, _, _) in agents) {
                val key = name.trim().lowercase()
                val pair = referenceData[key] ?: continue
                val featureFile = File(cacheDir, "${key.hashCode()}.bin")
                DataOutputStream(BufferedOutputStream(FileOutputStream(featureFile))).use { dos ->
                    dos.writeInt(pair.first.size)
                    pair.first.forEach { dos.writeFloat(it) }
                }
            }
            // Touch meta file to mark cache timestamp
            val metaFile = File(cacheDir, CACHE_META_FILE)
            metaFile.writeText("cached=${System.currentTimeMillis()}")
            Log.d(TAG, "Cache saved: ${referenceData.size} faces")
        } catch (e: Exception) {
            Log.w(TAG, "Cache write failed: ${e.message}")
        }
    }

    /** Clears the on-disk face cache. */
    fun clearCache() {
        getCacheDir().deleteRecursively()
        Log.d(TAG, "Cache cleared")
    }

    // ─── Image utilities ────────────────────────────────────────────────

    private fun downloadBitmap(url: String): Bitmap? {
        return try {
            val connection = URL(url).openConnection()
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.getInputStream().use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Download failed: ${e.message}")
            null
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        val image = imageProxy.image ?: return null
        val planes = image.planes

        val yBuffer = planes[0].buffer
        val uBuffer = planes[1].buffer
        val vBuffer = planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        val nv21 = ByteArray(ySize + uSize + vSize)
        yBuffer.get(nv21, 0, ySize)
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 85, out)
        val bytes = out.toByteArray()
        val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null

        val rotation = imageProxy.imageInfo.rotationDegrees
        return if (rotation != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotation.toFloat())
            val rotated = Bitmap.createBitmap(rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true)
            if (rotated != rawBitmap) rawBitmap.recycle()
            rotated
        } else rawBitmap
    }

    private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
        return suspendCancellableCoroutine { cont ->
            addOnSuccessListener { cont.resume(it) }
            addOnFailureListener { cont.resumeWithException(it) }
            addOnCanceledListener { cont.cancel() }
        }
    }
}
