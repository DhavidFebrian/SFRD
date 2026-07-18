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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.clickable
import com.example.ui.ScheduleViewModel
import com.example.ui.SyncState
import com.example.network.MeetingListing
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
    val listings by viewModel.weeklyMeetingAnalyticsListings.collectAsState()
    val syncStatus by viewModel.analyticSyncStatus.collectAsState()
    val listingTitleMap by viewModel.listingTitleMap.collectAsState()
    val listingDescMap by viewModel.listingDescMap.collectAsState()
    
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    
    // Month filter selection states
    val indonesianMonthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val currentMonthIndex = Calendar.getInstance().get(Calendar.MONTH) // 0-based
    val defaultMonthFilter = "${indonesianMonthNames[currentMonthIndex]} $currentYear"
    var selectedMonthFilter by remember { mutableStateOf(defaultMonthFilter) }
    var isMonthDropdownExpanded by remember { mutableStateOf(false) }
    
    val monthsList = listOf(
        "Semua Bulan $currentYear"
    ) + indonesianMonthNames.map { "$it $currentYear" }
    
    LaunchedEffect(selectedMonthFilter) {
        viewModel.fetchWeeklyMeetingAnalyticsData(selectedMonthFilter)
    }

    // Calculations based on Laporan Weekly Meeting listings
    val totalCount = listings.size
    
    val hotPropertyCount = listings.count { 
        it.keterangan.trim().uppercase() == "HOT PROPERTY" 
    }
    val igCount = listings.count { 
        it.keterangan.trim().uppercase() == "IG" 
    }
    val fotoUlangCount = listings.count { 
        it.keterangan.trim().uppercase() == "FOTO ULANG" 
    }
    val lainnyaCount = totalCount - (hotPropertyCount + igCount + fotoUlangCount)
    
    // Instagram posting status
    val isPostedPredicate = { it: MeetingListing ->
        val p = it.postingIg.trim().lowercase()
        p == "✔" || p == "true" || p == "yes" || p == "1" || p == "done" || p == "ya"
    }
    val igPostedCount = listings.count { isPostedPredicate(it) }
    val igPendingCount = totalCount - igPostedCount

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
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Month Selector Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Periode Analisis",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = selectedMonthFilter,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    Box {
                        Button(
                            onClick = { isMonthDropdownExpanded = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Pilih Bulan", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                        
                        DropdownMenu(
                            expanded = isMonthDropdownExpanded,
                            onDismissRequest = { isMonthDropdownExpanded = false }
                        ) {
                            monthsList.forEach { month ->
                                DropdownMenuItem(
                                    text = { Text(month, fontWeight = if (month == selectedMonthFilter) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedMonthFilter = month
                                        isMonthDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Sync Status Indicator / Loader Overlay
            if (syncStatus is SyncState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(36.dp))
                        Text(
                            "Memuat data meeting dari spreadsheet...",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                // Tab Selection
                TabRow(
                    selectedTabIndex = pagerState.currentPage,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = pagerState.currentPage == 0,
                        onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                        text = {
                            Text(
                                text = "Jadwal Foto",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    )
                    Tab(
                        selected = pagerState.currentPage == 1,
                        onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                        text = {
                            Text(
                                text = "Status Instagram",
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
                    if (totalCount == 0) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FolderOpen,
                                    contentDescription = null,
                                    modifier = Modifier.size(56.dp),
                                    tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "Tidak Ada Data Listing",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Tidak ditemukan listing pada Google Sheet untuk periode $selectedMonthFilter.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        when (page) {
                            0 -> {
                                // Tab 0: Distribusi Target Keterangan
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "LAPORAN JADWAL FOTO BULANAN",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                                    )

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                        ),
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            val listingsByDate = listings.groupBy { it.date }.toSortedMap()
                                            var globalIndex = 1
                                            
                                            listingsByDate.forEach { (dateStr, dateList) ->
                                                Text(
                                                    text = formatToIndonesianDayDate(dateStr),
                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(vertical = 6.dp)
                                                )
                                                
                                                dateList.forEach { item ->
                                                    val location = getListingLocation(item, listingTitleMap, listingDescMap)
                                                    val sessionType = getSessionType(item)
                                                    val cleanId = item.idListing.trim()
                                                    
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "$globalIndex. $location : $sessionType",
                                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                                color = MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = "https://raywhitecipete.net/ListingView/Detail/$cleanId",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.primary,
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis
                                                            )
                                                        }
                                                    }
                                                    globalIndex++
                                                }
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                            
                                            HorizontalDivider(
                                                modifier = Modifier.padding(vertical = 12.dp),
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            )
                                            
                                            // Count sessions
                                            val fotoCount = listings.count { getSessionType(it).contains("Foto") }
                                            val videoCount = listings.count { getSessionType(it).contains("Video") }
                                            val droneCount = listings.count { getSessionType(it).contains("Drone") }
                                            
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                Text("📸 Sesi Foto : $fotoCount", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                Text("📹 Sesi Video : $videoCount", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                                Text("🛸 Sesi Drone : $droneCount", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                        }
                                    }

                                    // Share Report Card for Jadwal Foto
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
                                                    text = "Bagikan Laporan Jadwal Foto",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Text(
                                                text = "Anda dapat membagikan rekap target jadwal foto ini secara terformat ke WhatsApp Business atau menyalin teksnya.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val report = buildJadwalFotoReportText(selectedMonthFilter, listings, listingTitleMap, listingDescMap)
                                                        val whatsappBusinessPackage = "com.whatsapp.w4b"
                                                        val isWaBusinessInstalled = try {
                                                            context.packageManager.getPackageInfo(whatsappBusinessPackage, 0)
                                                            true
                                                        } catch (e: Exception) {
                                                            false
                                                        }
                                                        
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(Intent.EXTRA_TEXT, report)
                                                            if (isWaBusinessInstalled) {
                                                                setPackage(whatsappBusinessPackage)
                                                            }
                                                        }
                                                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan"))
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Share WA", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        val report = buildJadwalFotoReportText(selectedMonthFilter, listings, listingTitleMap, listingDescMap)
                                                        clipboardManager.setText(AnnotatedString(report))
                                                        Toast.makeText(context, "Laporan disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                                                    Spacer(Modifier.width(6.dp))
                                                    Text("Salin Teks", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            1 -> {
                                // Tab 1: Status Publikasi Instagram
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text(
                                        text = "ANALISIS PUBLIKASI INSTAGRAM",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                                    )

                                    // Donut Chart Posted vs Pending
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
                                                        text = "Status Publikasi",
                                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "Realisasi upload Instagram",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFF2E7D32).copy(alpha = 0.15f), RoundedCornerShape(100.dp))
                                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "Posted: $igPostedCount / $totalCount",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = Color(0xFF2E7D32)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(18.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                                            ) {
                                                val colorPosted = Color(0xFF2E7D32)
                                                val colorPending = Color(0xFFD84315)

                                                Box(
                                                    modifier = Modifier.size(110.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                                        val strokeWidth = 14.dp.toPx()
                                                        val diameter = size.minDimension - strokeWidth
                                                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                                                        val arcSize = Size(diameter, diameter)

                                                        val floatPosted = igPostedCount.toFloat() / totalCount
                                                        val floatPending = igPendingCount.toFloat() / totalCount

                                                        drawArc(colorPosted, -90f, floatPosted * 360f, false, topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                                                        if (floatPending > 0f) {
                                                            drawArc(colorPending, -90f + (floatPosted * 360f), floatPending * 360f, false, topLeft, arcSize, style = Stroke(strokeWidth, cap = StrokeCap.Round))
                                                        }
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        val pct = if (totalCount > 0) ((igPostedCount.toFloat() / totalCount) * 100).toInt() else 0
                                                        Text("$pct%", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black))
                                                        Text("Selesai", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }

                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    LegendItem(colorPosted, "Sudah Posting", igPostedCount, (igPostedCount.toFloat() / totalCount * 100))
                                                    LegendItem(colorPending, "Pending / Belum", igPendingCount, (igPendingCount.toFloat() / totalCount * 100))
                                                }
                                            }
                                        }
                                    }

                                    // Share Report Card
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
                                                    text = "Bagikan Laporan Weekly Meeting",
                                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }

                                            Text(
                                                text = "Anda dapat membagikan rangkuman target dan publikasi weekly meeting ini secara terformat ke WhatsApp Business atau menyalin teksnya.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        val report = buildWeeklyReportText(selectedMonthFilter, listings, igPostedCount, igPendingCount, hotPropertyCount, fotoUlangCount, igCount)
                                                        val whatsappBusinessPackage = "com.whatsapp.w4b"
                                                        val isWaBusinessInstalled = try {
                                                            context.packageManager.getPackageInfo(whatsappBusinessPackage, 0)
                                                            true
                                                        } catch (e: Exception) {
                                                            false
                                                        }
                                                        
                                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                            type = "text/plain"
                                                            putExtra(Intent.EXTRA_TEXT, report)
                                                            if (isWaBusinessInstalled) {
                                                                setPackage(whatsappBusinessPackage)
                                                            }
                                                        }
                                                        try {
                                                            context.startActivity(Intent.createChooser(shareIntent, "Bagikan Laporan via"))
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Gagal membagikan laporan", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("WhatsApp Business")
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        val report = buildWeeklyReportText(selectedMonthFilter, listings, igPostedCount, igPendingCount, hotPropertyCount, fotoUlangCount, igCount)
                                                        clipboardManager.setText(AnnotatedString(report))
                                                        Toast.makeText(context, "Laporan disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(8.dp))
                                                    Text("Salin Teks")
                                                }
                                            }
                                        }
                                    }

                                    // List of Pending Listings for Quick Reference
                                    if (igPendingCount > 0) {
                                        Text(
                                            text = "DAFTAR LISTING PENDING PUBLIKASI (${igPendingCount})",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                                        )

                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            listings.filter { !isPostedPredicate(it) }.forEach { item ->
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                                                    ),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.SpaceBetween
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                val ketColor = when (item.keterangan.trim().uppercase()) {
                                                                    "HOT PROPERTY" -> Color(0xFFFF5722)
                                                                    "IG" -> Color(0xFFE1306C)
                                                                    "FOTO ULANG" -> Color(0xFF2196F3)
                                                                    else -> MaterialTheme.colorScheme.primary
                                                                }
                                                                Box(
                                                                    modifier = Modifier
                                                                        .background(ketColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = item.keterangan.ifBlank { "MEETING" },
                                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                                        color = ketColor
                                                                    )
                                                                }
                                                                Text(
                                                                    text = "ID: ${item.idListing}",
                                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                                    color = MaterialTheme.colorScheme.onSurface
                                                                )
                                                            }
                                                            Spacer(Modifier.height(4.dp))
                                                            Text(
                                                                text = "ME: ${item.namaMe.ifBlank { "Tidak Diketahui" }}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                            if (item.catatan.isNotBlank()) {
                                                                Text(
                                                                    text = "Catatan: ${item.catatan}",
                                                                    style = MaterialTheme.typography.bodySmall,
                                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                            }
                                                        }
                                                        
                                                        IconButton(
                                                            onClick = {
                                                                val itemText = "Pending Instagram:\nID: ${item.idListing}\nME: ${item.namaMe}\nKet: ${item.keterangan}\nCatatan: ${item.catatan}\nhttps://raywhitecipete.net/ListingView/Detail/${item.idListing}"
                                                                clipboardManager.setText(AnnotatedString(itemText))
                                                                Toast.makeText(context, "Listing disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ContentCopy,
                                                                contentDescription = "Salin Info",
                                                                tint = MaterialTheme.colorScheme.outline
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
private fun MetricProgressBarItem(
    title: String,
    icon: ImageVector,
    count: Int,
    total: Int,
    color: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
                Text(title, style = MaterialTheme.typography.labelMedium)
            }
            Text("$count data", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
        val ratio = if (total > 0) count.toFloat() / total.toFloat() else 0f
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun buildWeeklyReportText(
    monthFilter: String,
    listings: List<MeetingListing>,
    igPostedCount: Int,
    igPendingCount: Int,
    hotPropertyCount: Int,
    fotoUlangCount: Int,
    igCount: Int
): String {
    return buildString {
        append("📝 LAPORAN REKAPITULASI WEEKLY MEETING RAY WHITE CIPETE\n")
        append("📅 Periode: ").append(monthFilter).append("\n")
        append("📂 Total Listing Dibahas: ").append(listings.size).append(" unit\n\n")
        
        append("🔥 HOT PROPERTY: ").append(hotPropertyCount).append(" listing\n")
        append("📸 FOTO ULANG: ").append(fotoUlangCount).append(" listing\n")
        append("📱 TARGET INSTAGRAM: ").append(igCount).append(" listing\n")
        append("   ✅ Sudah Diposting: ").append(igPostedCount).append(" listing\n")
        append("   ❌ Pending / Belum: ").append(igPendingCount).append(" listing\n\n")
        
        val pendingList = listings.filter { 
            val p = it.postingIg.trim().lowercase()
            p != "✔" && p != "true" && p != "yes" && p != "1" && p != "done" && p != "ya"
        }
        
        if (pendingList.isNotEmpty()) {
            append("⚠️ DAFTAR PENDING PUBLIKASI / PROSES:\n")
            pendingList.forEachIndexed { index, item ->
                append("${index + 1}. [${item.keterangan.trim().ifEmpty { "MEETING" }}] ID: ${item.idListing} - ME: ${item.namaMe}\n")
                if (item.catatan.isNotBlank()) {
                    append("   Catatan: ${item.catatan}\n")
                }
                append("   Link: https://raywhitecipete.net/ListingView/Detail/${item.idListing}\n")
            }
        } else {
            append("🎉 Semua target weekly meeting periode ini telah selesai dikerjakan!")
        }
    }
}

private fun buildJadwalFotoReportText(
    monthFilter: String,
    listings: List<MeetingListing>,
    titleMap: Map<String, String>,
    descMap: Map<String, String>
): String {
    val cleanMonthFilter = if (monthFilter.contains("Semua")) {
        "Tahun 2026"
    } else {
        monthFilter
    }
    
    return buildString {
        append("Update Foto Raffa - David Bulan ").append(cleanMonthFilter).append("\n\n")
        
        val listingsByDate = listings.groupBy { it.date }.toSortedMap()
        
        var globalIndex = 1
        listingsByDate.forEach { (dateStr, dateList) ->
            append(formatToIndonesianDayDate(dateStr)).append("\n")
            dateList.forEach { item ->
                val location = getListingLocation(item, titleMap, descMap)
                val sessionType = getSessionType(item)
                val cleanId = item.idListing.trim()
                
                append(globalIndex).append(". ").append(location).append(" : ").append(sessionType).append("\n")
                append("https://raywhitecipete.net/ListingView/Detail/").append(cleanId).append("\n")
                
                globalIndex++
            }
            append("\n")
        }
        
        val fotoCount = listings.count { getSessionType(it).contains("Foto") }
        val videoCount = listings.count { getSessionType(it).contains("Video") }
        val droneCount = listings.count { getSessionType(it).contains("Drone") }
        
        append("📸 Sesi Foto : ").append(fotoCount).append("\n")
        append("📹 Sesi Video : ").append(videoCount).append("\n")
        append("🛸 Sesi Drone : ").append(droneCount)
    }
}

private fun extractLocationFromText(text: String): String? {
    if (text.isBlank()) return null
    val textLower = text.lowercase()
    val locations = listOf(
        "kebagusan", "cilandak", "cipete", "kemang", "jagakarsa", "pondok indah", "ampera", "kebayoran baru", "kebayoran lama", "kebayoran", "senopati", "bintaro", "tebet", "pejaten", "cilodong", "pasar minggu", "gandaria", "mampang prapatan", "mampang", "pancoran", "setiabudi", "kalibata", "ciganjur", "lenteng agung", "ragunan", "tanjung barat", "pesanggrahan", "cipulir", "pondok pinang", "lebak bulus", "fatmawati", "blok m", "radio dalam", "dharmawangsa", "darmawangsa", "panglima polim", "permata hijau", "senayan", "sudirman", "kuningan", "menteng", "prapanca", "wijaya", "cipete dalam", "cipete utara", "cipete selatan", "gandaria utara", "gandaria selatan", "pondok labu", "petukangan", "ulujami", "kebon baru", "manggarai", "pasar manggis", "karet semanggi", "karet pedurenan", "karet tengsin", "karet", "gatot subroto", "gatsu", "rasuna said", "mega kuningan", "scbd", "tebet barat", "tebet timur", "menteng dalam", "pengadegan", "pejaten barat", "pejaten timur", "jatipadang", "buncit", "warung buncit", "duren tiga", "bangka", "tendean", "kapten tendean", "petogogan", "melawai", "pulo", "cipulo", "kebayoran lama utara", "kebayoran lama selatan", "cilandak barat", "cilandak timur", "tanah kusir",
        "cinere", "depok", "sawangan", "margonda", "cimanggis", "limo", "beji", "pancoran mas", "sentul", "bogor", "cibubur", "bedahan", "beji timur", "gandul", "pangkalan jati", "krukut", "meruyung", "grogol", "mampang depok", "depok jaya", "sukmajaya", "tapos", "harjamukti", "bojonggede", "citayam", "sentul city", "tanah sareal", "bogor utara", "bogor selatan", "bogor timur", "bogor barat",
        "bsd city", "bsd", "serpong", "alam sutera", "gading serpong", "karawaci", "ciputat", "pamulang", "bintaro jaya", "ciledug", "tangerang", "serpong utara", "bintaro sektor 1", "bintaro sektor 2", "bintaro sektor 3", "bintaro sektor 4", "bintaro sektor 5", "bintaro sektor 6", "bintaro sektor 7", "bintaro sektor 8", "bintaro sektor 9", "graha raya", "pondok cabe", "cirendeu", "rempoa", "jombang", "sawah baru", "serua", "setu", "cisauk", "pagedangan", "legok", "curug", "cikokol", "tangerang kota", "larangan", "pondok aren",
        "puri indah", "kembangan", "kebon jeruk", "meruya", "tanjung duren", "tomang", "grogol", "slipi", "palmerah", "kalideres", "cengkareng", "meruya utara", "meruya selatan", "kembangan utara", "kembangan selatan", "permata buana", "taman aries", "intercon", "semesta", "kemanggisan", "jelambar", "kapuk",
        "rawamangun", "duren sawit", "pulomas", "ciracas", "kramat jati", "makasar", "matraman", "pasar rebo", "cakung", "cipayung", "jatinegara", "kayu putih", "pondok kelapa", "pondok bambu", "klender", "condet", "halim", "cililitan",
        "salemba", "tanah abang", "gambir", "kemayoran", "cempaka putih", "sawah besar", "cikini", "gondangdia", "senen", "benhil", "bendungan hilir", "petamburan",
        "pantai indah kapuk", "pik", "kelapa gading", "pluit", "sunter", "ancol", "cilincing", "koja", "pademangan", "penjaringan", "pik 2", "muara karang",
        "jatiasih", "tambun", "cikarang", "harapan indah", "summarecon bekasi", "bekasi", "grand wisata", "galaxy", "taman galaxy", "kemang pratama", "jatibening", "pondok gede"
    )
    val sortedLocations = locations.sortedByDescending { it.length }
    for (loc in sortedLocations) {
        if (textLower.contains(loc)) {
            return loc.split(" ").joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
            }
        }
    }
    return null
}

private fun toTitleCase(input: String): String {
    if (input.isBlank()) return ""
    return input.split(" ").joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
    }
}

private fun getListingLocation(
    listing: MeetingListing,
    titleMap: Map<String, String>,
    descMap: Map<String, String>
): String {
    val cleanId = listing.idListing.trim()
    if (cleanId.isNotBlank()) {
        val title = titleMap[cleanId] ?: ""
        val desc = descMap[cleanId] ?: ""
        
        val locFromTitle = extractLocationFromText(title)
        if (locFromTitle != null) return locFromTitle
        
        val locFromDesc = extractLocationFromText(desc)
        if (locFromDesc != null) return locFromDesc
    }
    
    val locFromCatatan = extractLocationFromText(listing.catatan)
    if (locFromCatatan != null) return locFromCatatan
    
    val locFromNamaMe = extractLocationFromText(listing.namaMe)
    if (locFromNamaMe != null) return locFromNamaMe
    
    return if (listing.namaMe.isNotBlank()) toTitleCase(listing.namaMe) else "Jakarta Selatan"
}

private fun getSessionType(listing: MeetingListing): String {
    val ket = listing.keterangan.lowercase()
    val cat = listing.catatan.lowercase()
    
    val hasVideo = ket.contains("video") || cat.contains("video")
    val hasDrone = ket.contains("drone") || cat.contains("drone")
    
    return when {
        hasVideo && hasDrone -> "Foto + Video + Drone"
        hasVideo -> "Foto + Video"
        hasDrone -> "Foto + Drone"
        else -> "Foto"
    }
}

private fun formatToIndonesianDayDate(dateStr: String): String {
    val parts = dateStr.split("-")
    if (parts.size != 3) return dateStr
    
    val year = parts[0].toIntOrNull() ?: return dateStr
    val month = parts[1].toIntOrNull() ?: return dateStr
    val day = parts[2].toIntOrNull() ?: return dateStr
    
    val calendar = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
    }
    
    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
    val indonesianDays = listOf("", "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
    val indonesianMonths = listOf(
        "", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    
    val dayName = indonesianDays.getOrNull(dayOfWeek) ?: ""
    val monthName = indonesianMonths.getOrNull(month) ?: ""
    
    val dayFormatted = String.format(Locale.US, "%02d", day)
    
    return "$dayName, $dayFormatted-$monthName-$year"
}
