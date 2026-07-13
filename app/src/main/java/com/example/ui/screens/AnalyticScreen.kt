package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.clickable
import com.example.ui.ScheduleViewModel
import com.example.ui.screens.MonthlyAnalysisCard
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AnalyticScreen(
    viewModel: ScheduleViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val schedules by viewModel.allSchedules.collectAsState()
    val editFotoTasks by viewModel.allEditFotoTasks.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    
    // Filter out VERSION_CHECK items and filter by selectedMonth (while including all month-agnostic non-aktif schedules)
    val activeSchedules = schedules.filter { 
        val isNonAktif = it.idListing.isNotBlank() &&
                         it.namaMe.isNotBlank() &&
                         it.lokasi.isNotBlank() &&
                         it.tanggal.isBlank() &&
                         it.jam.isBlank()
        it.idListing.trim() != "VERSION_CHECK" && (isNonAktif || isDateInSelectedMonth(it.tanggal, selectedMonth))
    }
    
    // Page 1 Statistics
    val totalCount = activeSchedules.size
    val doneCount = activeSchedules.count { 
        val typeLower = it.type.lowercase().trim()
        val statusLower = it.status.lowercase().trim()
        typeLower.startsWith("done") || statusLower == "done" || statusLower == "selesai"
    }
    val pendingCount = totalCount - doneCount
    
    val fotoCount = activeSchedules.count { 
        val t = it.type.lowercase().trim()
        t.contains("done") && t.contains("foto")
    }
    val videoCount = activeSchedules.count { 
        it.type.lowercase().contains("video")
    }
    val droneCount = activeSchedules.count {
        val t = it.type.lowercase().trim()
        t.contains("done") && t.contains("drone")
    }

    // State to toggle between showing all months or only selected month for Instagram Posting
    var showAllMonthsIg by remember { mutableStateOf(true) }

    // Page 2 Statistics (Instagram Posting)
    val filteredEditFotoTasks = remember(editFotoTasks, selectedMonth, showAllMonthsIg) {
        if (showAllMonthsIg) {
            editFotoTasks
        } else {
            editFotoTasks.filter {
                isDateInSelectedMonth(it.jadwalPosting, selectedMonth)
            }
        }
    }
    val totalIgTasks = filteredEditFotoTasks.size
    val igPostedCount = filteredEditFotoTasks.count { it.postingIg }
    val igDoneNotPostedCount = filteredEditFotoTasks.count { it.done && !it.postingIg }
    val igNotDoneCount = filteredEditFotoTasks.count { !it.done }
    val igNotPostedCount = filteredEditFotoTasks.count { !it.postingIg }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("analytic_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Ringkasan Analisis",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali ke Dashboard"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Scrollable TabRow to switch between Media and IG Posting analytic pages
            TabRow(
                selectedTabIndex = pagerState.currentPage,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = pagerState.currentPage == 0,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(0)
                        }
                    },
                    text = {
                        Text(
                            text = "Media & Proyek",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                )
                Tab(
                    selected = pagerState.currentPage == 1,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    text = {
                        Text(
                            text = "Postingan IG",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        // PAGE 1: Media & Proyek Overview
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "DISTRIBUSI MEDIA LISTING",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                            )
                            
                            MonthlyAnalysisCard(schedules = activeSchedules)

                            Text(
                                text = "METRIK KESELURUHAN",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Completed Projects Card
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.AssignmentTurnedIn, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text("Selesai", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("$doneCount", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black))
                                        Text(
                                            text = if (totalCount > 0) "${((doneCount.toFloat() / totalCount.toFloat()) * 100).toInt()}% dari total" else "0%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Pending Projects Card
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Outlined.EventNote, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.secondary)
                                            Text("Pending", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("$pendingCount", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black))
                                        Text(
                                            text = if (totalCount > 0) "${((pendingCount.toFloat() / totalCount.toFloat()) * 100).toInt()}% dari total" else "0%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Text(
                                        text = "Rasio Volume Kerja",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    // Foto Progress
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                                Text("Sesi Foto", style = MaterialTheme.typography.labelMedium)
                                            }
                                            Text("$fotoCount", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                        val fotoRatio = if (totalCount > 0) fotoCount.toFloat() / totalCount.toFloat() else 0f
                                        LinearProgressIndicator(
                                            progress = { fotoRatio },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }

                                    // Video Progress
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                                                Text("Sesi Video", style = MaterialTheme.typography.labelMedium)
                                            }
                                            Text("$videoCount", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                        val videoRatio = if (totalCount > 0) videoCount.toFloat() / totalCount.toFloat() else 0f
                                        LinearProgressIndicator(
                                            progress = { videoRatio },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                            color = MaterialTheme.colorScheme.secondary,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }

                                    // Drone Progress
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Icon(Icons.Default.Flight, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF2E7D32))
                                                Text("Sesi Drone", style = MaterialTheme.typography.labelMedium)
                                            }
                                            Text("$droneCount", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                                        }
                                        val droneRatio = if (totalCount > 0) droneCount.toFloat() / totalCount.toFloat() else 0f
                                        LinearProgressIndicator(
                                            progress = { droneRatio },
                                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                                            color = Color(0xFF2E7D32),
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }

                            // SHARE MONTHLY REPORT CARD
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "Bagikan Laporan Bulanan",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Text(
                                        text = if (doneCount > 0) {
                                            "Terdapat $doneCount jadwal selesai pada bulan $selectedMonth. Anda dapat membagikan laporan rekapan ini langsung ke WhatsApp Business atau menyalin teksnya."
                                        } else {
                                            "Belum ada jadwal selesai pada bulan $selectedMonth untuk dibuatkan laporan."
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )

                                    if (doneCount > 0) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            // WhatsApp Business Share Button
                                            Button(
                                                onClick = {
                                                    val doneSchedulesList = activeSchedules.filter {
                                                        val typeLower = it.type.lowercase().trim()
                                                        val statusLower = it.status.lowercase().trim()
                                                        typeLower.startsWith("done") || statusLower == "done" || statusLower == "selesai"
                                                    }.sortedWith(compareBy<com.example.data.Schedule> { it.tanggal }.thenBy { it.jam })

                                                    val reportText = buildReportText(selectedMonth, doneSchedulesList)
 
                                                    val whatsappBusinessPackage = "com.whatsapp.w4b"
                                                    val isWaBusinessInstalled = try {
                                                        context.packageManager.getPackageInfo(whatsappBusinessPackage, 0)
                                                        true
                                                    } catch (e: Exception) {
                                                        false
                                                    }
 
                                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(Intent.EXTRA_TEXT, reportText)
                                                        if (isWaBusinessInstalled) {
                                                            setPackage(whatsappBusinessPackage)
                                                        }
                                                    }
 
                                                    try {
                                                        val title = if (isWaBusinessInstalled) "Bagikan ke WhatsApp Business" else "Bagikan Laporan"
                                                        val chooser = Intent.createChooser(shareIntent, title)
                                                        context.startActivity(chooser)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Gagal membagikan laporan: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1.5f),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary
                                                ),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Share,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Bagikan WA", style = MaterialTheme.typography.labelLarge)
                                            }
 
                                            // Copy to Clipboard Button
                                            OutlinedButton(
                                                onClick = {
                                                    val doneSchedulesList = activeSchedules.filter {
                                                        val typeLower = it.type.lowercase().trim()
                                                        val statusLower = it.status.lowercase().trim()
                                                        typeLower.startsWith("done") || statusLower == "done" || statusLower == "selesai"
                                                    }.sortedWith(compareBy<com.example.data.Schedule> { it.tanggal }.thenBy { it.jam })

                                                    val reportText = buildReportText(selectedMonth, doneSchedulesList)
 
                                                    clipboardManager.setText(AnnotatedString(reportText))
                                                    Toast.makeText(context, "Laporan disalin ke papan klip!", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1.2f),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.ContentCopy,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Salin Teks", style = MaterialTheme.typography.labelLarge)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        // PAGE 2: Instagram Posting Diagram & Overview
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = "ANALISIS POSTINGAN INSTAGRAM",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                            )

                            // TABS / SEGMENTED CONTROL FOR FILTER TYPE
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (!showAllMonthsIg) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { showAllMonthsIg = false }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = selectedMonth,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (!showAllMonthsIg) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (showAllMonthsIg) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable { showAllMonthsIg = true }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Semua Bulan (Total)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (showAllMonthsIg) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Calculate floating percentages for precision and readability
                            val pctPosted = if (totalIgTasks > 0) (igPostedCount.toFloat() / totalIgTasks * 100f) else 0f
                            val pctDoneNotPosted = if (totalIgTasks > 0) (igDoneNotPostedCount.toFloat() / totalIgTasks * 100f) else 0f
                            val pctNotDone = if (totalIgTasks > 0) (igNotDoneCount.toFloat() / totalIgTasks * 100f) else 0f

                            // Custom Donut/Arc Diagram for Instagram Posting
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                ),
                                shape = RoundedCornerShape(20.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Ringkasan Publikasi",
                                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = if (showAllMonthsIg) "Status upload feed untuk seluruh data" else "Status upload feed untuk bulan $selectedMonth",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(100.dp))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "Total: $totalIgTasks",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(20.dp))

                                    if (totalIgTasks == 0) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudQueue,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "Belum ada data di bulan $selectedMonth",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                )
                                            }
                                        }
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            val colorPosted = Color(0xFF4CAF50) // Beautiful Green
                                            val colorDoneNotPosted = Color(0xFFFF9800) // Vibrant Orange
                                            val colorNotDone = Color(0xFF9E9E9E) // Sleek Grey

                                            Box(
                                                modifier = Modifier.size(110.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.fillMaxSize()) {
                                                    val strokeWidthPx = 14.dp.toPx()
                                                    val diameter = size.minDimension - strokeWidthPx
                                                    val topLeftOffset = Offset(
                                                        (size.width - diameter) / 2f,
                                                        (size.height - diameter) / 2f
                                                    )
                                                    val arcSize = Size(diameter, diameter)

                                                    // Calculate degrees
                                                    val degPosted = (igPostedCount.toFloat() / totalIgTasks) * 360f
                                                    val degDoneNotPosted = (igDoneNotPostedCount.toFloat() / totalIgTasks) * 360f
                                                    val degNotDone = (igNotDoneCount.toFloat() / totalIgTasks) * 360f

                                                    var startAngle = -90f

                                                    if (degPosted > 0) {
                                                        drawArc(
                                                            color = colorPosted,
                                                            startAngle = startAngle,
                                                            sweepAngle = degPosted,
                                                            useCenter = false,
                                                            topLeft = topLeftOffset,
                                                            size = arcSize,
                                                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                                        )
                                                        startAngle += degPosted
                                                    }

                                                    if (degDoneNotPosted > 0) {
                                                        drawArc(
                                                            color = colorDoneNotPosted,
                                                            startAngle = startAngle,
                                                            sweepAngle = degDoneNotPosted,
                                                            useCenter = false,
                                                            topLeft = topLeftOffset,
                                                            size = arcSize,
                                                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                                        )
                                                        startAngle += degDoneNotPosted
                                                    }

                                                    if (degNotDone > 0) {
                                                        drawArc(
                                                            color = colorNotDone,
                                                            startAngle = startAngle,
                                                            sweepAngle = degNotDone,
                                                            useCenter = false,
                                                            topLeft = topLeftOffset,
                                                            size = arcSize,
                                                            style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                                                        )
                                                    }
                                                }

                                                // Center percentage text
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(
                                                        text = "${String.format(Locale.US, "%.1f", pctPosted)}%",
                                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Terbit",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            // Legends
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                LegendItem(
                                                    color = colorPosted,
                                                    label = "Sudah Diposting",
                                                    count = igPostedCount,
                                                    percentage = pctPosted
                                                )
                                                LegendItem(
                                                    color = colorDoneNotPosted,
                                                    label = "Selesai Edit, Belum Post",
                                                    count = igDoneNotPostedCount,
                                                    percentage = pctDoneNotPosted
                                                )
                                                LegendItem(
                                                    color = colorNotDone,
                                                    label = "Belum Selesai Edit",
                                                    count = igNotDoneCount,
                                                    percentage = pctNotDone
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (totalIgTasks > 0) {
                                // Funnel Progress / Additional details
                                Text(
                                    text = "RINCIAN STATUS SEBARAN FEED",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                                )

                                val sudahDipostingSubItems = remember(filteredEditFotoTasks) {
                                    filteredEditFotoTasks.filter { it.postingIg }.map { item ->
                                        AnalyticSubItem(
                                            idListing = item.idListing,
                                            title = item.judul.ifBlank { "Listing ${item.idListing}" },
                                            subtitle = "ME: ${item.namaMe}",
                                            extraInfo = "Notes: ${item.editNotes}"
                                        )
                                    }
                                }

                                val selesaiEditMenungguPostSubItems = remember(filteredEditFotoTasks) {
                                    filteredEditFotoTasks.filter { it.done && !it.postingIg }.map { item ->
                                        AnalyticSubItem(
                                            idListing = item.idListing,
                                            title = item.judul.ifBlank { "Listing ${item.idListing}" },
                                            subtitle = "ME: ${item.namaMe}",
                                            extraInfo = "Notes: ${item.editNotes}"
                                        )
                                    }
                                }

                                val belumSelesaiSedangDieditSubItems = remember(activeSchedules) {
                                    activeSchedules.filter {
                                        val typeLower = it.type.lowercase().trim()
                                        val statusLower = it.status.lowercase().trim()
                                        typeLower.startsWith("done") && statusLower != "done" && statusLower != "selesai"
                                    }.map { item ->
                                        AnalyticSubItem(
                                            idListing = item.idListing,
                                            title = item.lokasi.ifBlank { "Listing ${item.idListing}" },
                                            subtitle = "ME: ${item.namaMe}",
                                            extraInfo = "Status: ${item.status}"
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // 1. Sudah Diposting
                                    StatusProgressCard(
                                        title = "Sudah Diposting ke Instagram",
                                        count = igPostedCount,
                                        total = totalIgTasks,
                                        percentage = pctPosted,
                                        color = Color(0xFF4CAF50),
                                        icon = Icons.Default.CheckCircle,
                                        subItems = sudahDipostingSubItems
                                    )

                                    // 2. Selesai Edit, Belum Post
                                    StatusProgressCard(
                                        title = "Selesai Edit, Menunggu Post",
                                        count = igDoneNotPostedCount,
                                        total = totalIgTasks,
                                        percentage = pctDoneNotPosted,
                                        color = Color(0xFFFF9800),
                                        icon = Icons.Default.HourglassEmpty,
                                        subItems = selesaiEditMenungguPostSubItems
                                    )

                                    // 3. Belum Selesai Edit
                                    StatusProgressCard(
                                        title = "Belum Selesai / Sedang Diedit",
                                        count = igNotDoneCount,
                                        total = totalIgTasks,
                                        percentage = pctNotDone,
                                        color = Color(0xFF9E9E9E),
                                        icon = Icons.Default.Edit,
                                        subItems = belumSelesaiSedangDieditSubItems
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

@Composable
private fun LegendItem(color: Color, label: String, count: Int, percentage: Float) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count data (${String.format(Locale.US, "%.1f", percentage)}%)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusProgressCard(
    title: String,
    count: Int,
    total: Int,
    percentage: Float,
    color: Color,
    icon: ImageVector,
    subItems: List<AnalyticSubItem>
) {
    var isExpanded by remember { mutableStateOf(false) }
    var expandedSubCategory by remember { mutableStateOf<String?>(null) }

    val groupedItems = remember(subItems) {
        val groups = mutableMapOf<String, List<AnalyticSubItem>>(
            "Up Foto" to emptyList(),
            "Edit Video" to emptyList(),
            "Garis Tanah" to emptyList()
        )
        subItems.forEach { item ->
            val combined = (item.title + " " + item.extraInfo).lowercase()
            val cat = when {
                combined.contains("video", ignoreCase = true) -> "Edit Video"
                combined.contains("garis", ignoreCase = true) || combined.contains("tanah", ignoreCase = true) -> "Garis Tanah"
                else -> "Up Foto"
            }
            groups[cat] = (groups[cat] ?: emptyList()) + item
        }
        groups
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Icon container
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Info column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$count dari $total data",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.1f", percentage)}%",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = color
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { if (total > 0) count.toFloat() / total.toFloat() else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = color,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
                
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.05f))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    
                    listOf("Up Foto", "Edit Video", "Garis Tanah").forEach { subCat ->
                        val items = groupedItems[subCat] ?: emptyList()
                        val isSubExpanded = expandedSubCategory == subCat
                        
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedSubCategory = if (isSubExpanded) null else subCat
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val subIcon = when (subCat) {
                                        "Up Foto" -> Icons.Default.Photo
                                        "Edit Video" -> Icons.Default.Videocam
                                        else -> Icons.Default.Terrain
                                    }
                                    Icon(
                                        imageVector = subIcon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = subCat,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Badge(
                                        containerColor = if (items.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = if (items.isNotEmpty()) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Text(items.size.toString(), style = MaterialTheme.typography.labelSmall)
                                    }
                                    Icon(
                                        imageVector = if (isSubExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            
                            AnimatedVisibility(visible = isSubExpanded) {
                                if (items.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Tidak ada data",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items.forEach { subItem ->
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surface
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(8.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = subItem.idListing,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Text(
                                                            text = subItem.subtitle,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(
                                                        text = subItem.title,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    if (subItem.extraInfo.isNotBlank()) {
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = subItem.extraInfo,
                                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                                            color = MaterialTheme.colorScheme.secondary,
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
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

private fun getWeekDates(weekOffset: Int): List<Date> {
    val calendar = Calendar.getInstance(Locale("id", "ID")).apply {
        firstDayOfWeek = Calendar.MONDAY
        time = Date()
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }
    calendar.add(Calendar.DAY_OF_YEAR, weekOffset * 7)
    
    val dates = mutableListOf<Date>()
    for (i in 0 until 7) {
        dates.add(calendar.time)
        calendar.add(Calendar.DAY_OF_YEAR, 1)
    }
    return dates
}

private fun parseDateToYmd(dateStr: String): String {
    val result = com.example.data.normalizeDate(dateStr)
    return if (result.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) result else ""
}

private fun calculateBestWeekPage(tasks: List<com.example.data.EditFotoTask>): Int {
    if (tasks.isEmpty()) return 500
    
    val sdfYmd = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayCalendar = Calendar.getInstance(Locale("id", "ID")).apply {
        firstDayOfWeek = Calendar.MONDAY
        time = Date()
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    while (todayCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
        todayCalendar.add(Calendar.DAY_OF_YEAR, -1)
    }
    
    val weekCounts = mutableMapOf<Int, Int>()
    
    for (task in tasks) {
        val ymd = parseDateToYmd(task.jadwalPosting)
        if (ymd.isEmpty()) continue
        try {
            val taskDate = sdfYmd.parse(ymd) ?: continue
            val taskCalendar = Calendar.getInstance(Locale("id", "ID")).apply {
                firstDayOfWeek = Calendar.MONDAY
                time = taskDate
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            while (taskCalendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                taskCalendar.add(Calendar.DAY_OF_YEAR, -1)
            }
            
            val diffMs = taskCalendar.timeInMillis - todayCalendar.timeInMillis
            val weeksDiff = Math.round(diffMs.toDouble() / (1000.0 * 60 * 60 * 24 * 7)).toInt()
            val page = 500 + weeksDiff
            if (page in 0..999) {
                weekCounts[page] = (weekCounts[page] ?: 0) + 1
            }
        } catch (e: Exception) { }
    }
    
    // Find page with max tasks, if none match fallback to 500
    if (weekCounts.isEmpty()) return 500
    return weekCounts.maxByOrNull { it.value }?.key ?: 500
}

private fun isDateInSelectedMonth(dateStr: String, selectedMonth: String): Boolean {
    val normalized = com.example.data.normalizeDate(dateStr)
    if (normalized.length < 7) return false
    val yearPart = normalized.substring(0, 4) // "2026"
    val monthPart = normalized.substring(5, 7) // "06"
    
    val monthName = when (monthPart) {
        "01" -> "Januari"
        "02" -> "Februari"
        "03" -> "Maret"
        "04" -> "April"
        "05" -> "Mei"
        "06" -> "Juni"
        "07" -> "Juli"
        "08" -> "Agustus"
        "09" -> "September"
        "10" -> "Oktober"
        "11" -> "November"
        "12" -> "Desember"
        else -> ""
    }
    val targetMonthYear = "$monthName $yearPart".lowercase().trim()
    return targetMonthYear == selectedMonth.lowercase().trim()
}

private fun formatReportDate(dateStr: String): String {
    return try {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr)
        if (date != null) {
            val sdf = SimpleDateFormat("EEEE, dd-MMMM-yyyy", Locale("id", "ID"))
            val formatted = sdf.format(date)
            val parts = formatted.split("-")
            if (parts.size == 3) {
                val day = parts[0]
                val month = parts[1].replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("id", "ID")) else it.toString() }
                val year = parts[2]
                "$day-$month-$year"
            } else {
                formatted
            }
        } else {
            dateStr
        }
    } catch (e: Exception) {
        dateStr
    }
}

private fun buildReportText(selectedMonth: String, doneSchedulesList: List<com.example.data.Schedule>): String {
    return buildString {
        append("Update Foto Raffa - David Bulan ").append(selectedMonth).append("\n\n")
        
        var lastDate: String? = null
        var overallIndex = 1
        
        doneSchedulesList.forEach { schedule ->
            val currentDateStr = schedule.tanggal
            if (currentDateStr != lastDate) {
                if (lastDate != null) {
                    append("\n") // Empty line before new date header
                }
                append(formatReportDate(currentDateStr)).append("\n")
                lastDate = currentDateStr
            }
            
            val cleanType = schedule.type
                .replace("(?i)^done\\s*".toRegex(), "")
                .trim()
                .ifEmpty { "Foto" }
                
            val idTrimmed = schedule.idListing.trim()
            append("${overallIndex}. ${schedule.lokasi} : $cleanType\n")
            append("https://raywhitecipete.net/ListingView/Detail/$idTrimmed\n")
            overallIndex++
        }
        
        val reportFotoCount = doneSchedulesList.count { 
            val t = it.type.lowercase()
            t.contains("foto") || (!t.contains("video") && !t.contains("drone"))
        }
        val reportVideoCount = doneSchedulesList.count { 
            it.type.lowercase().contains("video")
        }
        val reportDroneCount = doneSchedulesList.count { 
            it.type.lowercase().contains("drone")
        }
        
        append("\n")
        append("📸 Sesi Foto : $reportFotoCount\n")
        append("📹 Sesi Video : $reportVideoCount\n")
        append("🛸 Sesi Drone : $reportDroneCount")
    }
}

data class AnalyticSubItem(
    val idListing: String,
    val title: String,
    val subtitle: String,
    val extraInfo: String
)

