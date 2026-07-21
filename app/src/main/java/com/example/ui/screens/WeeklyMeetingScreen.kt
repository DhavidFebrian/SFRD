package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.Schedule
import com.example.network.MeetingListing
import com.example.ui.ScheduleViewModel
import com.example.ui.SyncState
import com.example.ui.AgentInfo
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.animation.core.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.Brush
import com.example.util.JarvisVoiceManager
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.core.content.ContextCompat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyMeetingScreen(
    viewModel: ScheduleViewModel,
    onOpenDrawer: () -> Unit,
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    val listings by viewModel.meetingListings.collectAsState()
    val selectedDate by viewModel.selectedMeetingDate.collectAsState()
    val selectedMonth by viewModel.selectedMeetingMonth.collectAsState()
    val syncStatus by viewModel.meetingSyncStatus.collectAsState()
    val yearlyIgPostingHistoryState by viewModel.yearlyIgPostingHistory.collectAsState()

    val listingImagesMap by viewModel.listingImagesMap.collectAsState()
    val listingImagesGalleryMap by viewModel.listingImagesGalleryMap.collectAsState()
    val listingTitleMap by viewModel.listingTitleMap.collectAsState()
    val listingPriceMap by viewModel.listingPriceMap.collectAsState()
    val listingDescMap by viewModel.listingDescMap.collectAsState()
    val agentInfoMap by viewModel.agentInfoMap.collectAsState()
    val listingSoldMap by viewModel.listingSoldMap.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("Semua") }

    // Date picker expand states
    var isDatePickerExpanded by remember { mutableStateOf(false) }
    var expandedMonthIndex by remember {
        val curMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH)
        mutableStateOf(curMonth)
    }
    var selectMonthDropdownExpanded by remember { mutableStateOf(false) }
    
    val monthsList = remember {
        listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
    }

    LaunchedEffect(selectedMonth) {
        selectedMonth?.let { monthStr ->
            val mName = monthStr.replace("Recap Meeting ", "").trim()
            val idx = monthsList.indexOf(mName)
            if (idx != -1) {
                expandedMonthIndex = idx
            }
        }
    }
    
    // Newsletter Dialog expand state
    var showNewsletterDialog by remember { mutableStateOf(false) }
    var showAddMeetingDialog by remember { mutableStateOf(false) }
    var showAutoScrapeDialog by remember { mutableStateOf(false) }
    var showPinVerificationDialog by remember { mutableStateOf(false) }
    var showSchedulingDialog by remember { mutableStateOf(false) }

    var selectedListingForDetail by remember { mutableStateOf<MeetingListing?>(null) }
    var selectedListingForEdit by remember { mutableStateOf<MeetingListing?>(null) }
    val appsScriptUrl by viewModel.appsScriptUrl.collectAsState()

    // Reset filters when date changes
    LaunchedEffect(selectedDate) {
        searchQuery = ""
        selectedFilter = "Semua"
    }

    // Smart auto-selection on screen open
    LaunchedEffect(Unit) {
        if (selectedDate == null) {
            viewModel.selectRelevantMeetingDateAutomatically()
        }
    }

    LaunchedEffect(appsScriptUrl) {
        if (appsScriptUrl.isNotBlank()) {
            viewModel.fetchYearlyIgPostingHistory()
        }
    }

    val displayDateText = remember(selectedDate) {
        if (selectedDate == null) "Weekly Meeting" else {
            try {
                val dateObj = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDate!!)
                SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(dateObj!!)
            } catch (e: Exception) {
                selectedDate!!
            }
        }
    }

    // Filter logic
    val filteredListings = remember(listings, searchQuery, selectedFilter) {
        listings.filter { listing ->
            // Search filter
            val matchesSearch = if (searchQuery.isBlank()) true else {
                listing.idListing.contains(searchQuery, ignoreCase = true) ||
                listing.namaMe.contains(searchQuery, ignoreCase = true)
            }
            
            // Category/Posting Status filter
            val matchesFilter = when (selectedFilter) {
                "Hot Property" -> listing.keterangan.contains("hot", ignoreCase = true)
                "Foto Ulang" -> listing.keterangan.contains("foto ulang", ignoreCase = true) || listing.keterangan.contains("ulang", ignoreCase = true)
                "IG" -> listing.keterangan.contains("ig", ignoreCase = true) || listing.keterangan.contains("instagram", ignoreCase = true)
                "Sudah Posting IG" -> listing.postingIg.lowercase() == "true"
                "Belum Posting IG" -> {
                    val isIgProperty = listing.keterangan.contains("ig", ignoreCase = true) || listing.keterangan.contains("instagram", ignoreCase = true)
                    val isPosted = listing.postingIg.lowercase() == "true"
                    isIgProperty && !isPosted
                }
                else -> true
            }
            
            matchesSearch && matchesFilter
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Weekly Meeting",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            if (selectedDate != null) {
                                Text(
                                    text = displayDateText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Buka Menu")
                        }
                    },
                    actions = {
                        if (selectedDate != null && selectedMonth != null) {
                            IconButton(
                                onClick = {
                                    viewModel.fetchMeetingListings(selectedMonth!!, selectedDate!!)
                                },
                                enabled = syncStatus !is SyncState.Loading
                            ) {
                                if (syncStatus is SyncState.Loading) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Segarkan")
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                    )
                )
            }
        },
        floatingActionButton = {
            if (selectedDate != null && selectedMonth != null) {
                FloatingActionButton(
                    onClick = { showAddMeetingDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Data")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val status = syncStatus) {
                is SyncState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Memuat data dari spreadsheet...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
                is SyncState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Terjadi Kesalahan",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = status.message,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        if (selectedDate != null && selectedMonth != null) {
                            Button(
                                onClick = {
                                    viewModel.fetchMeetingListings(selectedMonth!!, selectedDate!!)
                                }
                            ) {
                                Text("Coba Lagi")
                            }
                        }
                    }
                }
                else -> {
                    // LazyColumn wraps the entire content, allowing date picker, search bar, filters, and cards to scroll together.
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Unified Compact Control Desk Panel
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, top = 4.dp, end = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // Header
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Meeting Control Desk",
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        if (selectedDate != null) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = displayDateText,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Compact Action Buttons Row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. Newsletter Button
                                        val isNewsletterEnabled = selectedDate != null && listings.isNotEmpty()
                                        Button(
                                            onClick = { showNewsletterDialog = true },
                                            enabled = isNewsletterEnabled,
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PictureAsPdf,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Newsletter", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }

                                        // 2. Scheduling Button
                                        Button(
                                            onClick = { showSchedulingDialog = true },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.tertiary,
                                                contentColor = MaterialTheme.colorScheme.onTertiary
                                            ),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Scheduling", fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                    }

                                    // Auto Sync Web Button - below Newsletter & Scheduling
                                    Button(
                                        onClick = { showPinVerificationDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Sync,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Auto Sync Web",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }

                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                                    // Month selection Card (similar style to Absensi) - ALWAYS visible!
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectMonthDropdownExpanded = true }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = "Bulan Meeting",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "${monthsList[expandedMonthIndex]} 2026",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = "Pilih Bulan"
                                            )

                                            DropdownMenu(
                                                expanded = selectMonthDropdownExpanded,
                                                onDismissRequest = { selectMonthDropdownExpanded = false }
                                            ) {
                                                monthsList.forEachIndexed { index, name ->
                                                    DropdownMenuItem(
                                                        text = { Text(name, fontWeight = if (expandedMonthIndex == index) FontWeight.Bold else FontWeight.Normal) },
                                                        onClick = {
                                                            expandedMonthIndex = index
                                                            selectMonthDropdownExpanded = false
                                                            // Auto-load first date of selected month
                                                            val mName = monthsList[index]
                                                            val newMonthStr = "Recap Meeting $mName"
                                                            val datesList = viewModel.getMeetingDatesForMonth(index)
                                                            val firstDay = datesList.firstOrNull()?.first
                                                            val firstDateStr = if (firstDay != null) {
                                                                "2026-${(index + 1).toString().padStart(2, '0')}-${firstDay.padStart(2, '0')}"
                                                            } else ""
                                                            
                                                            viewModel.fetchMeetingListings(newMonthStr, firstDateStr)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Dates under the selected month as a scrollable row/tabrow!
                                    val datesList = viewModel.getMeetingDatesForMonth(expandedMonthIndex)
                                    if (datesList.isNotEmpty()) {
                                        ScrollableTabRow(
                                            selectedTabIndex = datesList.indexOfFirst { (day, _) ->
                                                val mNum = expandedMonthIndex + 1
                                                selectedDate == "2026-${mNum.toString().padStart(2, '0')}-${day.padStart(2, '0')}"
                                            }.let { if (it == -1) 0 else it },
                                            edgePadding = 4.dp,
                                            containerColor = Color.Transparent,
                                            divider = {},
                                            indicator = { tabPositions ->
                                                val selIdx = datesList.indexOfFirst { (day, _) ->
                                                    val mNum = expandedMonthIndex + 1
                                                    selectedDate == "2026-${mNum.toString().padStart(2, '0')}-${day.padStart(2, '0')}"
                                                }
                                                if (selIdx != -1 && selIdx < tabPositions.size) {
                                                    TabRowDefaults.SecondaryIndicator(
                                                        modifier = Modifier.tabIndicatorOffset(tabPositions[selIdx]),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            datesList.forEachIndexed { idx, (day, label) ->
                                                val monthNum = expandedMonthIndex + 1
                                                val dateStr = "2026-${monthNum.toString().padStart(2, '0')}-${day.padStart(2, '0')}"
                                                val isSelected = selectedDate == dateStr
                                                
                                                Tab(
                                                    selected = isSelected,
                                                    onClick = {
                                                        viewModel.fetchMeetingListings("Recap Meeting ${monthsList[expandedMonthIndex]}", dateStr)
                                                    },
                                                    text = {
                                                        Text(
                                                            text = label,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                            style = MaterialTheme.typography.bodySmall
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    } else {
                                        Text(
                                            text = "Belum ada jadwal meeting di bulan ini.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (selectedDate == null) {
                            // Empty Selection view inside lazy column
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp, vertical = 64.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Groups,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Pilih Tanggal Meeting",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Klik tombol 'Select Data (Tanggal Meeting)' di atas lalu pilih tanggal untuk menampilkan hasil meeting.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else if (listings.isEmpty()) {
                            // Empty Data view inside lazy column
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 32.dp, vertical = 64.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inbox,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Tidak Ada Data",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tidak ditemukan ID Listing pada kolom meeting tanggal ini di spreadsheet.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            // 3. Search Bar
                            item {
                                OutlinedTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    placeholder = { Text("Cari ID Listing / Nama ME...", fontSize = 14.sp) },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                                    trailingIcon = {
                                        if (searchQuery.isNotEmpty()) {
                                            IconButton(onClick = { searchQuery = "" }) {
                                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 2.dp)
                                )
                            }

                            // 4. Filter Chips
                            item {
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentPadding = PaddingValues(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val filterOptions = listOf(
                                        "Semua" to "📋 Semua",
                                        "Hot Property" to "🔥 Hot Property",
                                        "Foto Ulang" to "📸 Foto Ulang",
                                        "IG" to "📱 IG",
                                        "Sudah Posting IG" to "✅ Sudah Posting IG",
                                        "Belum Posting IG" to "⏳ Belum Posting"
                                    )
                                    items(filterOptions) { (filterKey, label) ->
                                        val isSelected = selectedFilter == filterKey
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedFilter = filterKey },
                                            label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }

                            // 5. Info Summary Card
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                                    ),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.DateRange,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(
                                                text = "Total Listing: ${listings.size}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                        if (filteredListings.size != listings.size) {
                                            Text(
                                                text = "Ditemukan: ${filteredListings.size}",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }

                            // 6. Property List Cards
                            if (filteredListings.isEmpty()) {
                                item {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 32.dp, vertical = 48.dp),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.SearchOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                                            modifier = Modifier.size(56.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "Hasil filter kosong",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            } else {
                                val chunkedListings = filteredListings.chunked(2)
                                items(chunkedListings) { pair ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 5.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            val listing = pair[0]
                                            MeetingListingCard(
                                                listing = listing,
                                                imageUrl = listingImagesMap[listing.idListing.trim()],
                                                title = listingTitleMap[listing.idListing.trim()],
                                                price = listingPriceMap[listing.idListing.trim()],
                                                agentInfo = agentInfoMap[listing.idListing.trim()],
                                                isSold = listingSoldMap[listing.idListing.trim()] == true,
                                                onClick = {
                                                    selectedListingForDetail = listing
                                                }
                                            )
                                        }
                                        if (pair.size > 1) {
                                            Box(modifier = Modifier.weight(1f)) {
                                                val listing = pair[1]
                                                MeetingListingCard(
                                                    listing = listing,
                                                    imageUrl = listingImagesMap[listing.idListing.trim()],
                                                    title = listingTitleMap[listing.idListing.trim()],
                                                    price = listingPriceMap[listing.idListing.trim()],
                                                    agentInfo = agentInfoMap[listing.idListing.trim()],
                                                    isSold = listingSoldMap[listing.idListing.trim()] == true,
                                                    onClick = {
                                                        selectedListingForDetail = listing
                                                    }
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.weight(1f))
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

    // Detail dialog overlay
    selectedListingForDetail?.let { listing ->
        val mockSchedule = remember(listing) {
            Schedule(
                no = listing.no,
                idListing = listing.idListing.trim(),
                namaMe = listing.namaMe.ifBlank { "ME tidak diketahui" },
                lokasi = listing.keterangan.ifBlank { "Lokasi/Keterangan tidak tersedia" },
                tanggal = selectedDate ?: "",
                jam = "",
                staff = "",
                type = "Meeting Result",
                status = if (listing.postingIg.lowercase().contains("done") || listing.postingIg.lowercase().contains("ya") || listing.postingIg.lowercase().contains("true")) "DONE" else "Pending"
            )
        }
        DetailPagerScreen(
            schedule = mockSchedule,
            listingImagesMap = listingImagesMap,
            listingImagesGalleryMap = listingImagesGalleryMap,
            agentInfoMap = agentInfoMap,
            viewModel = viewModel,
            onDismiss = { selectedListingForDetail = null },
            onEditMeetingListing = {
                selectedListingForEdit = listing
            }
        )
    }

    // Edit dialog overlay
    selectedListingForEdit?.let { listing ->
        EditMeetingListingDialog(
            listing = listing,
            month = selectedMonth ?: "",
            dateStr = selectedDate ?: "",
            viewModel = viewModel,
            onDismiss = { selectedListingForEdit = null }
        )
    }
    
    // Newsletter Dialog overlay
    if (showNewsletterDialog && selectedDate != null) {
        NewsletterDialog(
            dateStr = selectedDate!!,
            listings = listings,
            viewModel = viewModel,
            onDismiss = { showNewsletterDialog = false }
        )
    }

    if (showSchedulingDialog) {
        SchedulingDialog(
            viewModel = viewModel,
            onDismiss = { showSchedulingDialog = false }
        )
    }

    if (showAddMeetingDialog && selectedDate != null && selectedMonth != null) {
        AddMeetingListingDialog(
            month = selectedMonth!!,
            dateStr = selectedDate!!,
            viewModel = viewModel,
            onDismiss = { showAddMeetingDialog = false }
        )
    }

    if (showPinVerificationDialog) {
        PinVerificationDialog(
            onVerified = { 
                showPinVerificationDialog = false
                showAutoScrapeDialog = true 
            },
            onDismiss = { showPinVerificationDialog = false }
        )
    }

    if (showAutoScrapeDialog && selectedDate != null && selectedMonth != null) {
        AutoScrapeWebDialog(
            month = selectedMonth!!,
            dateStr = selectedDate!!,
            viewModel = viewModel,
            onDismiss = { showAutoScrapeDialog = false }
        )
    }
    }
}

@Composable
fun MeetingListingCard(
    listing: MeetingListing,
    imageUrl: String?,
    title: String?,
    price: String?,
    agentInfo: AgentInfo?,
    isSold: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Image Section on Top
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(125.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Foto Listing ${listing.idListing}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                }
                
                if (isSold) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(Color.Red, shape = RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "SOLD",
                                color = Color.White,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Stacked Agent avatars on bottom-right of the image
                if (agentInfo != null) {
                    val avatars = remember(agentInfo) {
                        agentInfo.avatarUrl.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    }
                    val names = remember(agentInfo, listing.namaMe) {
                        val rawName = if (agentInfo.name.isNotBlank()) agentInfo.name else listing.namaMe
                        com.example.ui.parseMultipleAgentNames(rawName)
                    }
                    if (names.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy((-8).dp) // overlapping stack!
                        ) {
                            names.forEachIndexed { idx, name ->
                                val avatarUrl = if (idx < avatars.size) avatars[idx] else ""
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .shadow(2.dp, CircleShape)
                                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (avatarUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = avatarUrl,
                                            contentDescription = "Avatar $name",
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        val initials = name.split(" ")
                                            .filter { it.isNotBlank() }
                                            .take(1)
                                            .map { it.first().uppercaseChar() }
                                            .joinToString("")
                                        Text(
                                            text = initials.ifEmpty { "ME" },
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Info section at the bottom
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
                    // ID Listing Badge
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "ID: ${listing.idListing}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // IG Posting Badge if available
                    if (listing.postingIg.isNotBlank()) {
                        val isDone = listing.postingIg.lowercase().contains("done") || listing.postingIg.lowercase().contains("ya") || listing.postingIg.lowercase().contains("true")
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isDone) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isDone) "DONE" else "Plan",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = if (isDone) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Title
                Text(
                    text = title ?: "Memuat Judul...",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Price
                Text(
                    text = price ?: "Rp. Hubungi Agent",
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 1.dp)
                )

                // ME details
                if (listing.namaMe.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = "ME: ${listing.namaMe}",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Keterangan / Notes
                if (listing.keterangan.isNotBlank()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Ket: ${listing.keterangan}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Catatan / Extra Notes
                if (listing.catatan.isNotBlank()) {
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Cat: ${listing.catatan}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.tertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMeetingListingDialog(
    month: String,
    dateStr: String,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var idListing by remember { mutableStateOf("") }
    var activePreviewImages by remember { mutableStateOf<List<String>?>(null) }
    var activePreviewIndex by remember { mutableStateOf(0) }
    var namaMe by remember { mutableStateOf("") }
    var selectedKeterangan by remember { mutableStateOf("") }
    
    var inputJudul by remember { mutableStateOf("") }
    var selectedEditOptions by remember { mutableStateOf(emptySet<String>()) }
    var catatanManual by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }

    var isVoiceListening by remember { mutableStateOf(false) }
    var voiceStatusText by remember { mutableStateOf<String?>(null) }
    var jarvisVoiceManager by remember { mutableStateOf<JarvisVoiceManager?>(null) }

    val listingImagesMap by viewModel.listingImagesMap.collectAsState()
    val listingImagesGalleryMap by viewModel.listingImagesGalleryMap.collectAsState()
    val listingTitleMap by viewModel.listingTitleMap.collectAsState()
    val listingPriceMap by viewModel.listingPriceMap.collectAsState()
    val listingDescMap by viewModel.listingDescMap.collectAsState()
    val agentInfoMap by viewModel.agentInfoMap.collectAsState()
    val listingSoldMap by viewModel.listingSoldMap.collectAsState()
    val existingMeetingListings by viewModel.meetingListings.collectAsState()

    // Live constructed notes preview
    val computedCatatan = remember(selectedEditOptions, inputJudul, catatanManual) {
        val formattedOptions = if (selectedEditOptions.isNotEmpty()) "edit ${selectedEditOptions.joinToString(" ")}" else ""
        val formattedJudul = if (inputJudul.isNotBlank()) "judul $inputJudul" else ""
        listOf(formattedOptions, formattedJudul, catatanManual).filter { it.isNotBlank() }.joinToString(", ")
    }

    DisposableEffect(Unit) {
        val manager = JarvisVoiceManager(
            context = context,
            onOptionRecognized = { option ->
                selectedKeterangan = option
                val currentCleanId = idListing.trim()
                val isDup = currentCleanId.isNotBlank() && existingMeetingListings.any { it.idListing.trim().equals(currentCleanId, ignoreCase = true) }
                if (currentCleanId.isNotBlank() && !isDup && !isLoading) {
                    isLoading = true
                    viewModel.addWeeklyMeetingListing(
                        month = month,
                        dateStr = dateStr,
                        idListing = currentCleanId,
                        namaMe = namaMe,
                        keterangan = option,
                        catatan = computedCatatan,
                        onResult = { success, message ->
                            isLoading = false
                            (context as? android.app.Activity)?.runOnUiThread {
                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                if (success) {
                                    onDismiss()
                                }
                            }
                        }
                    )
                }
            },
            onStateChanged = { listening, text ->
                isVoiceListening = listening
                voiceStatusText = text
            }
        )
        jarvisVoiceManager = manager
        onDispose {
            manager.destroy()
        }
    }

    val addTemplates = listOf("ratio", "perspective", "remove object")

    LaunchedEffect(idListing) {
        val trimmed = idListing.trim()
        if (trimmed.length >= 3) {
            val autofilled = viewModel.getAutofilledMeForListingId(trimmed)
            if (autofilled.isNotBlank()) {
                namaMe = autofilled
            }
            viewModel.fetchListingImageIfNeeded(trimmed)
            viewModel.fetchYearlyIgPostingHistory()
        } else if (trimmed.isEmpty()) {
            namaMe = ""
        }
    }

    val cleanId = idListing.trim()
    val isDuplicateId = remember(cleanId, existingMeetingListings) {
        cleanId.isNotBlank() && existingMeetingListings.any { it.idListing.trim().equals(cleanId, ignoreCase = true) }
    }
    val isListingValid = cleanId.isNotBlank() && cleanId.length >= 3
    var isDescExpanded by remember(cleanId) { mutableStateOf(false) }

    val agentInfo = if (isListingValid) agentInfoMap[cleanId] else null
    LaunchedEffect(agentInfo) {
        if (agentInfo != null && agentInfo.name.isNotBlank()) {
            namaMe = agentInfo.name
        }
    }

    val addOptions = listOf("HOT PROPERTY", "IG", "FOTO ULANG")

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Tambah Data Meeting",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss, enabled = !isLoading) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading
                            ) {
                                Text("Batal")
                            }

                            OutlinedButton(
                                onClick = {
                                    if (isVoiceListening) {
                                        jarvisVoiceManager?.stopListening()
                                    } else {
                                        jarvisVoiceManager?.startJarvisCommandFlow()
                                    }
                                },
                                modifier = Modifier.weight(1.1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isVoiceListening) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                    contentColor = if (isVoiceListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                ),
                                border = BorderStroke(1.dp, if (isVoiceListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isVoiceListening) Icons.Default.Mic else Icons.Default.MicNone,
                                        contentDescription = "Voice",
                                        tint = if (isVoiceListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isVoiceListening) (voiceStatusText ?: "Voice") else "Voice",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        maxLines = 1
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    if (idListing.isBlank()) {
                                        Toast.makeText(context, "ID Listing tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (isDuplicateId) {
                                        Toast.makeText(context, "ID sudah di input ke meeting di tanggal ini!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isLoading = true
                                    viewModel.addWeeklyMeetingListing(
                                        month = month,
                                        dateStr = dateStr,
                                        idListing = idListing,
                                        namaMe = namaMe,
                                        keterangan = selectedKeterangan,
                                        catatan = computedCatatan,
                                        onResult = { success, message ->
                                            isLoading = false
                                            (context as? android.app.Activity)?.runOnUiThread {
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                if (success) {
                                                    onDismiss()
                                                }
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading && !isDuplicateId
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Tambah")
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = idListing,
                            onValueChange = { idListing = it },
                            label = { Text("ID Listing") },
                            placeholder = { Text("Contoh: 12193") },
                            leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            isError = isDuplicateId,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )

                        // ADD_DIALOG_MARKER
                        OutlinedTextField(
                            value = namaMe,
                            onValueChange = { namaMe = it },
                            label = { Text("Nama ME") },
                            placeholder = { Text("Nama ME") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        )
                    }

                    if (isDuplicateId) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Peringatan Duplicate",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "ID $cleanId sudah di input ke meeting!",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // ADD_KETERANGAN_MARKER

                    // Real-time Listing Detail Preview
                    if (isListingValid) {
                        // UNIQUE_ADD_DIALOG_FLOW
                        // ADD_PREVIEW_ANCHOR
                        val igListingsState by viewModel.weeklyMeetingIgListings.collectAsState()
                        val editFotoTasksState by viewModel.allEditFotoTasks.collectAsState()
                        val localYearlyIgHistory by viewModel.yearlyIgPostingHistory.collectAsState()
                        val igHistory = remember(cleanId, igListingsState, editFotoTasksState, localYearlyIgHistory) {
                            viewModel.getIgPostingHistory(cleanId)
                        }
                        IgPostingInfoCard(history = igHistory)
                        Spacer(modifier = Modifier.height(4.dp))
                        val fallbackImg = listingImagesMap[cleanId]
                        val galleryList = listingImagesGalleryMap[cleanId] ?: emptyList()
                        val imagesToDisplay = remember(galleryList, fallbackImg, cleanId) {
                            val rawList = if (galleryList.isNotEmpty()) galleryList else listOfNotNull(fallbackImg)
                            rawList.filter { img ->
                                val lower = img.lowercase()
                                !lower.contains("agent") &&
                                !lower.contains("profile") &&
                                !lower.contains("team") &&
                                !lower.contains("member") &&
                                !lower.contains("staff") &&
                                !lower.contains("/me/") &&
                                !lower.contains("avatar")
                            }
                        }

                        val title = listingTitleMap[cleanId]
                        val price = listingPriceMap[cleanId] ?: "Rp. Hubungi Agent"
                        val isSold = listingSoldMap[cleanId] ?: false

                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("add_dialog_preview_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Detail Listing Preview",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.testTag("add_dialog_preview_title")
                                        )
                                    }
                                    
                                    if (isSold) {
                                        Surface(
                                            color = Color.Red.copy(alpha = 0.15f),
                                            contentColor = Color.Red,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = "SOLD / INACTIVE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // ADD_DISPLAY_LOADING_OR_DETAILS
                                if (title == null && imagesToDisplay.isEmpty() && !isSold) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Mengambil detail dari website...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    // Title
                                    if (!title.isNullOrBlank()) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Price
                                    if (!price.isNullOrBlank()) {
                                        Text(
                                            text = price,
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // ADD_LISTING_PHOTOS
                                    if (imagesToDisplay.isNotEmpty()) { // ADD_PHOTOS_CHECK
                                        val totalPhotos = imagesToDisplay.size
                                        if (totalPhotos == 1) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight()
                                                    .clickable {
                                                        activePreviewImages = imagesToDisplay
                                                        activePreviewIndex = 0
                                                    }
                                            ) {
                                                AsyncImage(
                                                    model = imagesToDisplay.first(),
                                                    contentDescription = "Foto Listing",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .wrapContentHeight(),
                                                    contentScale = ContentScale.FillWidth
                                                )
                                                // Photo Counter
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.6f),
                                                    contentColor = Color.White,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(8.dp)
                                                ) {
                                                    Text(
                                                        text = "1/1",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState())
                                            ) {
                                                imagesToDisplay.forEachIndexed { idx, imgUrl ->
                                                    Box(
                                                        modifier = Modifier
                                                            .height(320.dp) // Enlarged scale!
                                                            .wrapContentWidth()
                                                            .clickable {
                                                                activePreviewImages = imagesToDisplay
                                                                activePreviewIndex = idx
                                                            }
                                                    ) {
                                                        AsyncImage(
                                                            model = imgUrl,
                                                            contentDescription = "Foto Listing",
                                                            modifier = Modifier
                                                                .height(320.dp) // Enlarged scale!
                                                                .wrapContentWidth(),
                                                            contentScale = ContentScale.FillHeight
                                                        )
                                                        // Photo Counter
                                                        Surface(
                                                            color = Color.Black.copy(alpha = 0.6f),
                                                            contentColor = Color.White,
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier
                                                                .align(Alignment.BottomEnd)
                                                                .padding(8.dp)
                                                        ) {
                                                            Text(
                                                                text = "${idx + 1}/$totalPhotos",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // ADD_AGENT_INFO
                                    val finalAgentName = if (agentInfo != null && agentInfo.name.isNotBlank()) agentInfo.name else namaMe
                                    val agentNamesList = remember(finalAgentName) {
                                        com.example.ui.parseMultipleAgentNames(finalAgentName)
                                    }
                                    val avatarsList = remember(agentInfo) {
                                        agentInfo?.avatarUrl?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                                    }

                                    if (agentNamesList.isNotEmpty()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            agentNamesList.forEachIndexed { index, namePart ->
                                                val meAvatarUrl = if (index < avatarsList.size) avatarsList[index] else ""
                                                val contact = com.example.ui.findContact(namePart)
                                                val displayName = contact?.let { com.example.ui.capitalizeName(it.nameKey) } ?: com.example.ui.capitalizeName(namePart)
                                                
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    if (meAvatarUrl.isNotBlank()) {
                                                        Card(
                                                            shape = RoundedCornerShape(22.dp),
                                                            modifier = Modifier.size(44.dp)
                                                        ) {
                                                            AsyncImage(
                                                                model = meAvatarUrl,
                                                                contentDescription = "Foto Agent",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    } else {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                                            shape = RoundedCornerShape(22.dp),
                                                            modifier = Modifier.size(44.dp)
                                                        ) {
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier.fillMaxSize()
                                                            ) {
                                                                val initials = displayName.split(" ")
                                                                    .filter { it.isNotBlank() }
                                                                    .take(2)
                                                                    .map { it.first().uppercaseChar() }
                                                                    .joinToString("")
                                                                Text(
                                                                    text = initials.ifEmpty { "ME" },
                                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }
                                                    }
                                                    
                                                    Column {
                                                        Text(
                                                            text = displayName,
                                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "Marketing Executive",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                     val rawDesc = listingDescMap[cleanId] ?: "" // ADD_RAW_DESC
                                     val description = remember(rawDesc) {
                                         rawDesc
                                             .replace("<[^>]*>".toRegex(), "")
                                             .replace("\r", "")
                                             .replace("(?m)^[ \t]*\r?\n".toRegex(), "\n")
                                             .replace(" {2,}".toRegex(), " ")
                                             .trim()
                                     }

                                     if (description.isNotBlank()) {
                                         HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                         Column(
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .clickable { isDescExpanded = !isDescExpanded }
                                                 .padding(vertical = 4.dp),
                                             verticalArrangement = Arrangement.spacedBy(6.dp)
                                         ) {
                                             Row(
                                                 modifier = Modifier.fillMaxWidth(),
                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Text(
                                                     text = "Deskripsi Properti",
                                                     style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                     color = MaterialTheme.colorScheme.primary
                                                 )
                                                 Text(
                                                     text = if (isDescExpanded) "Sembunyikan" else "Selengkapnya",
                                                     style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                     color = MaterialTheme.colorScheme.secondary
                                                 )
                                             }
                                             Text(
                                                 text = description,
                                                 style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                 maxLines = if (isDescExpanded) Int.MAX_VALUE else 3,
                                                 overflow = TextOverflow.Ellipsis
                                             )
                                         }
                                     }
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Keterangan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val optionsData = listOf(
                                Triple("HOT PROPERTY", Icons.Default.Whatshot, Color(0xFFFF5722)),
                                Triple("IG", Icons.Default.AlternateEmail, Color(0xFFE1306C)),
                                Triple("FOTO ULANG", Icons.Default.PhotoCamera, Color(0xFF2196F3))
                            )

                            optionsData.forEach { (option, icon, baseColor) ->
                                val isSelected = selectedKeterangan == option
                                val backgroundColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.08f)
                                val borderColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.3f)
                                val contentColor = if (isSelected) Color.White else baseColor
                                val borderStroke = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (!isLoading) {
                                                selectedKeterangan = if (isSelected) "" else option
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                                    border = borderStroke
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = option,
                                            tint = contentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = when (option) {
                                                "HOT PROPERTY" -> "Hot Property"
                                                "IG" -> "Instagram"
                                                "FOTO ULANG" -> "Foto Ulang"
                                                else -> option
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = contentColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Template Catatan Edit Foto",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val templatesData = listOf(
                                Triple("ratio", Icons.Default.Crop, Color(0xFF9C27B0)),
                                Triple("perspective", Icons.Default.Transform, Color(0xFF4CAF50)),
                                Triple("remove object", Icons.Default.Brush, Color(0xFFFF9800))
                            )

                            templatesData.forEach { (template, icon, baseColor) ->
                                val isSelected = selectedEditOptions.contains(template)
                                val backgroundColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.08f)
                                val borderColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.3f)
                                val contentColor = if (isSelected) Color.White else baseColor
                                val borderStroke = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (!isLoading) {
                                                selectedEditOptions = if (isSelected) {
                                                    selectedEditOptions - template
                                                } else {
                                                    selectedEditOptions + template
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                                    border = borderStroke
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(vertical = 10.dp, horizontal = 6.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = template,
                                            tint = contentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when (template) {
                                                "ratio" -> "Ratio"
                                                "perspective" -> "Persp."
                                                "remove object" -> "Rem Obj"
                                                else -> template
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputJudul,
                        onValueChange = { inputJudul = it },
                        label = { Text("Judul") },
                        placeholder = { Text("Contoh: Rumah Mewah Minimalis") },
                        leadingIcon = { Icon(Icons.Default.Title, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )

                    OutlinedTextField(
                        value = catatanManual,
                        onValueChange = { catatanManual = it },
                        label = { Text("Catatan Manual") },
                        placeholder = { Text("Tulis catatan manual...") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Preview Catatan yang Disimpan (Kolom Sheet):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = computedCatatan.ifBlank { "(kosong)" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = if (computedCatatan.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
        if (activePreviewImages != null) {
            FullScreenImageGalleryDialog(
                images = activePreviewImages!!,
                initialIndex = activePreviewIndex,
                onDismiss = { activePreviewImages = null }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditMeetingListingDialog(
    listing: MeetingListing,
    month: String,
    dateStr: String,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var idListing by remember { mutableStateOf(listing.idListing) }
    var activePreviewImages by remember { mutableStateOf<List<String>?>(null) }
    var activePreviewIndex by remember { mutableStateOf(0) }
    var namaMe by remember { mutableStateOf(listing.namaMe) }
    var selectedKeterangan by remember { mutableStateOf(listing.keterangan) }
    
    var inputJudul by remember { mutableStateOf("") }
    var selectedEditOptions by remember { mutableStateOf(emptySet<String>()) }
    var catatanManual by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val listingImagesMap by viewModel.listingImagesMap.collectAsState()
    val listingImagesGalleryMap by viewModel.listingImagesGalleryMap.collectAsState()
    val listingTitleMap by viewModel.listingTitleMap.collectAsState()
    val listingPriceMap by viewModel.listingPriceMap.collectAsState()
    val listingDescMap by viewModel.listingDescMap.collectAsState()
    val agentInfoMap by viewModel.agentInfoMap.collectAsState()
    val listingSoldMap by viewModel.listingSoldMap.collectAsState()

    val editDialogTemplates = listOf("ratio", "perspective", "remove object")

    // Parse existing notes on start
    LaunchedEffect(listing.catatan) {
        val existing = listing.catatan
        var initialOptions = emptySet<String>()
        if (existing.contains("edit ")) {
            val editPart = existing.substringAfter("edit ").substringBefore(",")
            if (editPart.contains("ratio")) initialOptions = initialOptions + "ratio"
            if (editPart.contains("perspective")) initialOptions = initialOptions + "perspective"
            if (editPart.contains("remove object")) initialOptions = initialOptions + "remove object"
        }
        selectedEditOptions = initialOptions
        if (existing.contains("judul ")) {
            val judulPart = existing.substringAfter("judul ").substringBefore(",")
            inputJudul = judulPart.trim()
        }
        val parts = existing.split(",").map { it.trim() }
        val remainingParts = parts.filter { !it.startsWith("edit ") && !it.startsWith("judul ") }
        if (remainingParts.isNotEmpty()) {
            catatanManual = remainingParts.joinToString(", ")
        } else {
            catatanManual = ""
        }
    }

    LaunchedEffect(idListing) {
        val trimmed = idListing.trim()
        if (trimmed.length >= 3) {
            val autofilled = viewModel.getAutofilledMeForListingId(trimmed)
            if (autofilled.isNotBlank()) {
                namaMe = autofilled
            }
            viewModel.fetchListingImageIfNeeded(trimmed)
        } else if (trimmed.isEmpty()) {
            namaMe = ""
        }
    }

    val cleanId = idListing.trim()
    val isListingValid = cleanId.isNotBlank() && cleanId.length >= 3
    var isDescExpanded by remember(cleanId) { mutableStateOf(false) }

    val agentInfo = if (isListingValid) agentInfoMap[cleanId] else null
    LaunchedEffect(agentInfo) {
        if (agentInfo != null && agentInfo.name.isNotBlank()) {
            namaMe = agentInfo.name
        }
    }

    val editDialogOptions = listOf("HOT PROPERTY", "IG", "FOTO ULANG")

    // Live constructed notes preview
    val computedCatatan = remember(selectedEditOptions, inputJudul, catatanManual) {
        val formattedOptions = if (selectedEditOptions.isNotEmpty()) "edit ${selectedEditOptions.joinToString(" ")}" else ""
        val formattedJudul = if (inputJudul.isNotBlank()) "judul $inputJudul" else ""
        listOf(formattedOptions, formattedJudul, catatanManual).filter { it.isNotBlank() }.joinToString(", ")
    }

    Dialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Modifikasi Data Meeting",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss, enabled = !isLoading) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Tutup"
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            titleContentColor = MaterialTheme.colorScheme.onSurface,
                            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                },
                bottomBar = {
                    Surface(
                        tonalElevation = 8.dp,
                        shadowElevation = 8.dp,
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 48.dp) // raised bottom padding!
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading
                            ) {
                                Text("Batal")
                            }
                            OutlinedButton(
                                onClick = { showDeleteConfirmDialog = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text("Hapus")
                                }
                            }
                            Button(
                                onClick = {
                                    if (idListing.isBlank()) {
                                        Toast.makeText(context, "ID Listing tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isLoading = true
                                    viewModel.updateWeeklyMeetingDetails(
                                        month = month,
                                        dateStr = dateStr,
                                        row = listing.no,
                                        colIndex = listing.colIndex,
                                        idListing = idListing,
                                        namaMe = namaMe,
                                        keterangan = selectedKeterangan,
                                        catatan = computedCatatan,
                                        onResult = { success, message ->
                                            isLoading = false
                                            (context as? android.app.Activity)?.runOnUiThread {
                                                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                if (success) {
                                                    onDismiss()
                                                }
                                            }
                                        }
                                    )
                                },
                                modifier = Modifier.weight(1.2f),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Simpan")
                                }
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = idListing,
                            onValueChange = { idListing = it },
                            label = { Text("ID Listing") },
                            placeholder = { Text("Contoh: 12193") },
                            leadingIcon = { Icon(Icons.Default.Tag, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            )
                        )

                        // EDIT_DIALOG_MARKER
                        OutlinedTextField(
                            value = namaMe,
                            onValueChange = { namaMe = it },
                            label = { Text("Nama ME") },
                            placeholder = { Text("Nama ME") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        )
                    }

                    // EDIT_KETERANGAN_MARKER

                    // Real-time Listing Detail Preview
                    if (isListingValid) {
                        // UNIQUE_EDIT_DIALOG_FLOW
                        // EDIT_PREVIEW_ANCHOR
                        val fallbackImg = listingImagesMap[cleanId]
                        val galleryList = listingImagesGalleryMap[cleanId] ?: emptyList()
                        val imagesToDisplay = remember(galleryList, fallbackImg, cleanId) {
                            val rawList = if (galleryList.isNotEmpty()) galleryList else listOfNotNull(fallbackImg)
                            rawList.filter { img ->
                                val lower = img.lowercase()
                                !lower.contains("agent") &&
                                !lower.contains("profile") &&
                                !lower.contains("team") &&
                                !lower.contains("member") &&
                                !lower.contains("staff") &&
                                !lower.contains("/me/") &&
                                !lower.contains("avatar")
                            }
                        }

                        val title = listingTitleMap[cleanId]
                        val price = listingPriceMap[cleanId] ?: "Rp. Hubungi Agent"
                        val isSold = listingSoldMap[cleanId] ?: false

                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("edit_dialog_preview_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Detail Listing Preview",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.testTag("edit_dialog_preview_title")
                                        )
                                    }
                                    
                                    if (isSold) {
                                        Surface(
                                            color = Color.Red.copy(alpha = 0.15f),
                                            contentColor = Color.Red,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = "SOLD / INACTIVE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                // EDIT_DISPLAY_LOADING_OR_DETAILS
                                if (title == null && imagesToDisplay.isEmpty() && !isSold) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Mengambil detail dari website...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    // Title
                                    if (!title.isNullOrBlank()) {
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Price
                                    if (!price.isNullOrBlank()) {
                                        Text(
                                            text = price,
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // EDIT_LISTING_PHOTOS
                                    if (imagesToDisplay.isNotEmpty()) {
                                        val totalPhotos = imagesToDisplay.size
                                        if (totalPhotos == 1) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight()
                                                    .clickable {
                                                        activePreviewImages = imagesToDisplay
                                                        activePreviewIndex = 0
                                                    }
                                            ) {
                                                AsyncImage(
                                                    model = imagesToDisplay.first(),
                                                    contentDescription = "Foto Listing",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .wrapContentHeight(),
                                                    contentScale = ContentScale.FillWidth
                                                )
                                                // Photo Counter
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.6f),
                                                    contentColor = Color.White,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(8.dp)
                                                ) {
                                                    Text(
                                                        text = "1/1",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState())
                                            ) {
                                                imagesToDisplay.forEachIndexed { idx, imgUrl ->
                                                    Box(
                                                        modifier = Modifier
                                                            .height(320.dp) // Enlarged scale!
                                                            .wrapContentWidth()
                                                            .clickable {
                                                                activePreviewImages = imagesToDisplay
                                                                activePreviewIndex = idx
                                                            }
                                                    ) {
                                                        AsyncImage(
                                                            model = imgUrl,
                                                            contentDescription = "Foto Listing",
                                                            modifier = Modifier
                                                                .height(320.dp) // Enlarged scale!
                                                                .wrapContentWidth(),
                                                            contentScale = ContentScale.FillHeight
                                                        )
                                                        // Photo Counter
                                                        Surface(
                                                            color = Color.Black.copy(alpha = 0.6f),
                                                            contentColor = Color.White,
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier
                                                                .align(Alignment.BottomEnd)
                                                                .padding(8.dp)
                                                        ) {
                                                            Text(
                                                                text = "${idx + 1}/$totalPhotos",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Agent / ME Info (Supports multiple agents / co-listing!)
                                    val finalAgentName = if (agentInfo != null && agentInfo.name.isNotBlank()) agentInfo.name else namaMe
                                    val agentNamesList = remember(finalAgentName) {
                                        com.example.ui.parseMultipleAgentNames(finalAgentName)
                                    }
                                    val avatarsList = remember(agentInfo) {
                                        agentInfo?.avatarUrl?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                                    }

                                    if (agentNamesList.isNotEmpty()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            agentNamesList.forEachIndexed { index, namePart ->
                                                val meAvatarUrl = if (index < avatarsList.size) avatarsList[index] else ""
                                                val contact = com.example.ui.findContact(namePart)
                                                val displayName = contact?.let { com.example.ui.capitalizeName(it.nameKey) } ?: com.example.ui.capitalizeName(namePart)
                                                
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    if (meAvatarUrl.isNotBlank()) {
                                                        Card(
                                                            shape = RoundedCornerShape(22.dp),
                                                            modifier = Modifier.size(44.dp)
                                                        ) {
                                                            AsyncImage(
                                                                model = meAvatarUrl,
                                                                contentDescription = "Foto Agent",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    } else {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                                            shape = RoundedCornerShape(22.dp),
                                                            modifier = Modifier.size(44.dp)
                                                        ) {
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier.fillMaxSize()
                                                            ) {
                                                                val initials = displayName.split(" ")
                                                                    .filter { it.isNotBlank() }
                                                                    .take(2)
                                                                    .map { it.first().uppercaseChar() }
                                                                    .joinToString("")
                                                                Text(
                                                                    text = initials.ifEmpty { "ME" },
                                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }
                                                    }
                                                    
                                                    Column {
                                                        Text(
                                                            text = displayName,
                                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "Marketing Executive",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                     val rawDesc = listingDescMap[cleanId] ?: "" // EDIT_RAW_DESC
                                     val description = remember(rawDesc) {
                                         rawDesc
                                             .replace("<[^>]*>".toRegex(), "")
                                             .replace("\r", "")
                                             .replace("(?m)^[ \t]*\r?\n".toRegex(), "\n")
                                             .replace(" {2,}".toRegex(), " ")
                                             .trim()
                                     }

                                     if (description.isNotBlank()) {
                                         HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                         Column(
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .clickable { isDescExpanded = !isDescExpanded }
                                                 .padding(vertical = 4.dp),
                                             verticalArrangement = Arrangement.spacedBy(6.dp)
                                         ) {
                                             Row(
                                                 modifier = Modifier.fillMaxWidth(),
                                                 horizontalArrangement = Arrangement.SpaceBetween,
                                                 verticalAlignment = Alignment.CenterVertically
                                             ) {
                                                 Text(
                                                     text = "Deskripsi Properti",
                                                     style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                     color = MaterialTheme.colorScheme.primary
                                                 )
                                                 Text(
                                                     text = if (isDescExpanded) "Sembunyikan" else "Selengkapnya",
                                                     style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                     color = MaterialTheme.colorScheme.secondary
                                                 )
                                             }
                                             Text(
                                                 text = description,
                                                 style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                 maxLines = if (isDescExpanded) Int.MAX_VALUE else 3,
                                                 overflow = TextOverflow.Ellipsis
                                             )
                                         }
                                     }
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Keterangan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val optionsData = listOf(
                                Triple("HOT PROPERTY", Icons.Default.Whatshot, Color(0xFFFF5722)),
                                Triple("IG", Icons.Default.AlternateEmail, Color(0xFFE1306C)),
                                Triple("FOTO ULANG", Icons.Default.PhotoCamera, Color(0xFF2196F3))
                            )

                            optionsData.forEach { (option, icon, baseColor) ->
                                val isSelected = selectedKeterangan == option
                                val backgroundColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.08f)
                                val borderColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.3f)
                                val contentColor = if (isSelected) Color.White else baseColor
                                val borderStroke = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (!isLoading) {
                                                selectedKeterangan = if (isSelected) "" else option
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                                    border = borderStroke
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp).fillMaxWidth(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = option,
                                            tint = contentColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = when (option) {
                                                "HOT PROPERTY" -> "Hot Property"
                                                "IG" -> "Instagram"
                                                "FOTO ULANG" -> "Foto Ulang"
                                                else -> option
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = contentColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Column {
                        Text(
                            text = "Template Catatan Edit Foto",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val templatesData = listOf(
                                Triple("ratio", Icons.Default.Crop, Color(0xFF9C27B0)),
                                Triple("perspective", Icons.Default.Transform, Color(0xFF4CAF50)),
                                Triple("remove object", Icons.Default.Brush, Color(0xFFFF9800))
                            )

                            templatesData.forEach { (template, icon, baseColor) ->
                                val isSelected = selectedEditOptions.contains(template)
                                val backgroundColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.08f)
                                val borderColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.3f)
                                val contentColor = if (isSelected) Color.White else baseColor
                                val borderStroke = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            if (!isLoading) {
                                                selectedEditOptions = if (isSelected) {
                                                    selectedEditOptions - template
                                                } else {
                                                    selectedEditOptions + template
                                                }
                                            }
                                        },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = backgroundColor),
                                    border = borderStroke
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .padding(vertical = 10.dp, horizontal = 6.dp)
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = template,
                                            tint = contentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = when (template) {
                                                "ratio" -> "Ratio"
                                                "perspective" -> "Persp."
                                                "remove object" -> "Rem Obj"
                                                else -> template
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputJudul,
                        onValueChange = { inputJudul = it },
                        label = { Text("Judul") },
                        placeholder = { Text("Contoh: Rumah Mewah Minimalis") },
                        leadingIcon = { Icon(Icons.Default.Title, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )

                    OutlinedTextField(
                        value = catatanManual,
                        onValueChange = { catatanManual = it },
                        label = { Text("Catatan Manual") },
                        placeholder = { Text("Tulis catatan manual...") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Preview Catatan yang Disimpan (Kolom Sheet):",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = computedCatatan.ifBlank { "(kosong)" },
                                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                                color = if (computedCatatan.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
        if (activePreviewImages != null) {
            FullScreenImageGalleryDialog(
                images = activePreviewImages!!,
                initialIndex = activePreviewIndex,
                onDismiss = { activePreviewImages = null }
            )
        }

        if (showDeleteConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmDialog = false },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        text = "Hapus Data Meeting",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = "Apakah Anda yakin ingin menghapus data listing ${listing.idListing} dari meeting tanggal $dateStr?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteConfirmDialog = false
                            isLoading = true
                            viewModel.deleteWeeklyMeetingListing(
                                month = month,
                                dateStr = dateStr,
                                row = listing.no,
                                colIndex = listing.colIndex,
                                idListing = listing.idListing,
                                onResult = { success, message ->
                                    isLoading = false
                                    (context as? android.app.Activity)?.runOnUiThread {
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                        if (success) {
                                            onDismiss()
                                        }
                                    }
                                }
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmDialog = false }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}



@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FullScreenImageGalleryDialog(
    images: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = initialIndex, pageCount = { images.size })
    var isZoomed by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = !isZoomed
            ) { pageIndex ->
                val imageUrl = images[pageIndex]
                
                // Track scale & offset for zoom
                var scale by remember { mutableStateOf(1f) }
                var offset by remember { mutableStateOf(Offset.Zero) }

                // Reset zoom when page changes
                LaunchedEffect(pagerState.currentPage) {
                    if (pagerState.currentPage != pageIndex) {
                        scale = 1f
                        offset = Offset.Zero
                    }
                }

                // Update parent zoomed state
                LaunchedEffect(scale) {
                    if (pagerState.currentPage == pageIndex) {
                        isZoomed = scale > 1f
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                val newScale = (scale * zoom).coerceIn(1f, 5f)
                                scale = newScale
                                if (newScale > 1f) {
                                    offset = Offset(
                                        x = offset.x + pan.x,
                                        y = offset.y + pan.y
                                    )
                                } else {
                                    offset = Offset.Zero
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Zoomable Listing Photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            // --- Top UI overlays ---
            // Close button & Counter
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Photo Counter e.g. "2 of 5"
                Surface(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${images.size}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                // Close Button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.5f), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup",
                        tint = Color.White
                    )
                }
            }

            // --- Navigation Arrows (Only show when there are multiple photos and not zoomed in) ---
            if (images.size > 1 && !isZoomed) {
                // Left Arrow
                if (pagerState.currentPage > 0) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp)
                            .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "Sebelumnya",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Right Arrow
                if (pagerState.currentPage < images.size - 1) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .background(Color.Black.copy(alpha = 0.4f), shape = CircleShape)
                            .size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Selanjutnya",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoScrapeWebDialog(
    month: String,
    dateStr: String,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val isScrapingActive by viewModel.isAutoScrapingActive.collectAsState()
    val totalWebHotListings by viewModel.totalWebHotListings.collectAsState()
    val pendingList by viewModel.pendingScrapedListings.collectAsState()
    
    // Track session's successfully processed/added listing IDs
    var processedListings by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    // Existing listings to avoid scraping
    val listings by viewModel.meetingListings.collectAsState()
    
    // Start scraping when dialog opens
    LaunchedEffect(Unit) {
        val initialList = listings.map { it.idListing }
        viewModel.startAutoScraping(initialList)
    }
    
    // Stop scraping when dialog closes
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopAutoScraping()
            viewModel.clearPendingScrapedListings()
        }
    }

    var lastTriggeredPendingSize by remember { mutableStateOf(0) }

    // Add-listing dialog state for manual FAB
    var showAddManualDialog by remember { mutableStateOf(false) }

    var activePreviewImages by remember { mutableStateOf<List<String>?>(null) }
    var activePreviewIndex by remember { mutableStateOf(0) }

    // Trigger vibrate when new listing is scraped
    LaunchedEffect(pendingList.size) {
        if (pendingList.size > lastTriggeredPendingSize && pendingList.isNotEmpty()) {
            lastTriggeredPendingSize = pendingList.size

            // Vibrate
            try {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (v != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(500)
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Auto Sync",
                                    tint = if (isScrapingActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Column {
                                    Text(
                                        text = "Auto Sync Web",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = if (isScrapingActive) "Scraping: AKTIF" else "Scraping: JEDA",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isScrapingActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = "Tutup")
                            }
                        },
                        actions = {
                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Hot: $totalWebHotListings",
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    if (isScrapingActive) {
                                        viewModel.stopAutoScraping()
                                    } else {
                                        viewModel.startAutoScraping(listings.map { it.idListing } + processedListings.map { it.first })
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isScrapingActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isScrapingActive) "Jeda" else "Mulai",
                                    tint = if (isScrapingActive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { showAddManualDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 72.dp, end = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah Manual")
                    }
                },
                floatingActionButtonPosition = FabPosition.End
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .navigationBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 68.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val activeId = pendingList.firstOrNull()

                    if (activeId != null) {
                        val listingImagesMap by viewModel.listingImagesMap.collectAsState()
                        val listingImagesGalleryMap by viewModel.listingImagesGalleryMap.collectAsState()
                        val listingTitleMap by viewModel.listingTitleMap.collectAsState()
                        val listingPriceMap by viewModel.listingPriceMap.collectAsState()
                        val listingDescMap by viewModel.listingDescMap.collectAsState()
                        val agentInfoMap by viewModel.agentInfoMap.collectAsState()
                        val listingSoldMap by viewModel.listingSoldMap.collectAsState()

                        val title = listingTitleMap[activeId] ?: "Memuat info listing..."
                        val price = listingPriceMap[activeId] ?: ""
                        val agentInfo = agentInfoMap[activeId]

                        var selectedKeterangan by remember(activeId) { mutableStateOf("") }
                        var isVoiceListening by remember { mutableStateOf(false) }
                        var voiceStatusText by remember { mutableStateOf<String?>(null) }
                        var jarvisVoiceManager by remember { mutableStateOf<JarvisVoiceManager?>(null) }

                        var namaMe by remember(activeId, agentInfo) {
                            mutableStateOf(agentInfo?.name ?: "")
                        }
                        var selectedEditOptions by remember(activeId) { mutableStateOf(emptySet<String>()) }
                        var inputJudul by remember(activeId) { mutableStateOf("") }
                        var catatanManual by remember(activeId) { mutableStateOf("") }
                        var isSavingListing by remember { mutableStateOf(false) }
                        var isDescExpanded by remember(activeId) { mutableStateOf(false) }

                        val computedCatatan = remember(selectedEditOptions, inputJudul, catatanManual) {
                            val formattedOptions = if (selectedEditOptions.isNotEmpty()) "edit ${selectedEditOptions.joinToString(" ")}" else ""
                            val formattedJudul = if (inputJudul.isNotBlank()) "judul $inputJudul" else ""
                            listOf(formattedOptions, formattedJudul, catatanManual).filter { it.isNotBlank() }.joinToString(", ")
                        }

                        DisposableEffect(activeId) {
                            val manager = JarvisVoiceManager(
                                context = context,
                                onOptionRecognized = { option ->
                                    selectedKeterangan = option
                                    val cleanActiveId = activeId.trim()
                                    val isDup = cleanActiveId.isNotBlank() && (
                                        listings.any { it.idListing.trim().equals(cleanActiveId, ignoreCase = true) } ||
                                        processedListings.any { it.first.trim().equals(cleanActiveId, ignoreCase = true) }
                                    )
                                    if (!isDup && !isSavingListing) {
                                        isSavingListing = true
                                        viewModel.addWeeklyMeetingListing(
                                            month = month,
                                            dateStr = dateStr,
                                            idListing = activeId,
                                            namaMe = namaMe,
                                            keterangan = option,
                                            catatan = computedCatatan,
                                            onResult = { success, msg ->
                                                isSavingListing = false
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                if (success) {
                                                    processedListings = processedListings + (activeId to "$namaMe ($option)")
                                                    viewModel.removePendingScrapedListing(activeId)
                                                }
                                            }
                                        )
                                    }
                                },
                                onStateChanged = { listening, text ->
                                    isVoiceListening = listening
                                    voiceStatusText = text
                                }
                            )
                            jarvisVoiceManager = manager
                            onDispose {
                                manager.destroy()
                            }
                        }

                        // Prefetch details
                        LaunchedEffect(activeId) {
                            viewModel.fetchListingImageIfNeeded(activeId)
                            viewModel.fetchYearlyIgPostingHistory()
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "LISTING HOT BARU TERDETEKSI!",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    
                                    val isSold = listingSoldMap[activeId] ?: false
                                    if (isSold) {
                                        Surface(
                                            color = Color.Red.copy(alpha = 0.15f),
                                            contentColor = Color.Red,
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.padding(horizontal = 4.dp)
                                        ) {
                                            Text(
                                                text = "SOLD / INACTIVE",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                val igListingsState by viewModel.weeklyMeetingIgListings.collectAsState()
                                val editFotoTasksState by viewModel.allEditFotoTasks.collectAsState()
                                val localYearlyIgHistory by viewModel.yearlyIgPostingHistory.collectAsState()
                                val igHistory = remember(activeId, igListingsState, editFotoTasksState, localYearlyIgHistory) {
                                    viewModel.getIgPostingHistory(activeId)
                                }
                                IgPostingInfoCard(history = igHistory)
                                
                                val fallbackImg = listingImagesMap[activeId]
                                val galleryList = listingImagesGalleryMap[activeId] ?: emptyList()
                                val imagesToDisplay = remember(galleryList, fallbackImg, activeId) {
                                    val rawList = if (galleryList.isNotEmpty()) galleryList else listOfNotNull(fallbackImg)
                                    rawList.filter { img ->
                                        val lower = img.lowercase()
                                        !lower.contains("agent") &&
                                        !lower.contains("profile") &&
                                        !lower.contains("team") &&
                                        !lower.contains("member") &&
                                        !lower.contains("staff") &&
                                        !lower.contains("/me/") &&
                                        !lower.contains("avatar")
                                    }
                                }

                                if (title.startsWith("Memuat info") && imagesToDisplay.isEmpty()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Mengambil detail dari website...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    // Title
                                    Text(
                                        text = title,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Price
                                    if (price.isNotBlank()) {
                                        Text(
                                            text = price,
                                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Photos Display
                                    if (imagesToDisplay.isNotEmpty()) {
                                        val totalPhotos = imagesToDisplay.size
                                        if (totalPhotos == 1) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight()
                                                    .clickable {
                                                        activePreviewImages = imagesToDisplay
                                                        activePreviewIndex = 0
                                                    }
                                            ) {
                                                AsyncImage(
                                                    model = imagesToDisplay.first(),
                                                    contentDescription = "Foto Listing",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .wrapContentHeight(),
                                                    contentScale = ContentScale.FillWidth
                                                )
                                                Surface(
                                                    color = Color.Black.copy(alpha = 0.6f),
                                                    contentColor = Color.White,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .align(Alignment.BottomEnd)
                                                        .padding(8.dp)
                                                ) {
                                                    Text(
                                                        text = "1/1",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }
                                        } else {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState())
                                            ) {
                                                imagesToDisplay.forEachIndexed { idx, imgUrl ->
                                                    Box(
                                                        modifier = Modifier
                                                            .height(320.dp)
                                                            .wrapContentWidth()
                                                            .clickable {
                                                                activePreviewImages = imagesToDisplay
                                                                activePreviewIndex = idx
                                                            }
                                                    ) {
                                                        AsyncImage(
                                                            model = imgUrl,
                                                            contentDescription = "Foto Listing",
                                                            modifier = Modifier
                                                                .height(320.dp)
                                                                .wrapContentWidth(),
                                                            contentScale = ContentScale.FillHeight
                                                        )
                                                        Surface(
                                                            color = Color.Black.copy(alpha = 0.6f),
                                                            contentColor = Color.White,
                                                            shape = RoundedCornerShape(12.dp),
                                                            modifier = Modifier
                                                                .align(Alignment.BottomEnd)
                                                                .padding(8.dp)
                                                        ) {
                                                            Text(
                                                                text = "${idx + 1}/$totalPhotos",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Agent Info
                                    val finalAgentName = if (agentInfo != null && agentInfo.name.isNotBlank()) agentInfo.name else namaMe
                                    val agentNamesList = remember(finalAgentName) {
                                        com.example.ui.parseMultipleAgentNames(finalAgentName)
                                    }
                                    val avatarsList = remember(agentInfo) {
                                        agentInfo?.avatarUrl?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                                    }

                                    if (agentNamesList.isNotEmpty()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            agentNamesList.forEachIndexed { idx, namePart ->
                                                val meAvatarUrl = if (idx < avatarsList.size) avatarsList[idx] else ""
                                                val contact = com.example.ui.findContact(namePart)
                                                val displayName = contact?.let { com.example.ui.capitalizeName(it.nameKey) } ?: com.example.ui.capitalizeName(namePart)
                                                
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    if (meAvatarUrl.isNotBlank()) {
                                                        Card(
                                                            shape = RoundedCornerShape(22.dp),
                                                            modifier = Modifier.size(44.dp)
                                                        ) {
                                                            AsyncImage(
                                                                model = meAvatarUrl,
                                                                contentDescription = "Foto Agent",
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    } else {
                                                        Surface(
                                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                                            shape = RoundedCornerShape(22.dp),
                                                            modifier = Modifier.size(44.dp)
                                                        ) {
                                                            Box(
                                                                contentAlignment = Alignment.Center,
                                                                modifier = Modifier.fillMaxSize()
                                                            ) {
                                                                val initials = displayName.split(" ")
                                                                    .filter { it.isNotBlank() }
                                                                    .take(2)
                                                                    .map { it.first().uppercaseChar() }
                                                                    .joinToString("")
                                                                Text(
                                                                    text = initials.ifEmpty { "ME" },
                                                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                        }
                                                    }
                                                    
                                                    Column {
                                                        Text(
                                                            text = displayName,
                                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = "Marketing Executive",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    val rawDesc = listingDescMap[activeId] ?: ""
                                    val description = remember(rawDesc) {
                                        rawDesc
                                            .replace("<[^>]*>".toRegex(), "")
                                            .replace("\r", "")
                                            .replace("(?m)^[ \t]*\r?\n".toRegex(), "\n")
                                            .replace(" {2,}".toRegex(), " ")
                                            .trim()
                                    }

                                    if (description.isNotBlank()) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isDescExpanded = !isDescExpanded }
                                                .padding(vertical = 4.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Deskripsi Properti",
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Icon(
                                                    imageVector = if (isDescExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Text(
                                                text = description,
                                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 16.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = if (isDescExpanded) Int.MAX_VALUE else 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                                OutlinedTextField(
                                    value = namaMe,
                                    onValueChange = { namaMe = it },
                                    label = { Text("Nama Marketing Executive (ME)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
                                )

                                Text(
                                    text = "Keterangan",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val optionsData = listOf(
                                        Triple("HOT PROPERTY", Icons.Default.Whatshot, Color(0xFFFF5722)),
                                        Triple("IG", Icons.Default.AlternateEmail, Color(0xFFE1306C)),
                                        Triple("FOTO ULANG", Icons.Default.PhotoCamera, Color(0xFF2196F3))
                                    )

                                    optionsData.forEach { (option, icon, baseColor) ->
                                        val isSelected = selectedKeterangan == option
                                        val backgroundColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.08f)
                                        val borderColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.3f)
                                        val contentColor = if (isSelected) Color.White else baseColor
                                        val borderStroke = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)

                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    if (!isSavingListing) {
                                                        selectedKeterangan = if (isSelected) "" else option
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = backgroundColor),
                                            border = borderStroke
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp).fillMaxWidth(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = option,
                                                    tint = contentColor,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Text(
                                                    text = when (option) {
                                                        "HOT PROPERTY" -> "Hot Property"
                                                        "IG" -> "Instagram"
                                                        "FOTO ULANG" -> "Foto Ulang"
                                                        else -> option
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = contentColor,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Template Catatan Edit Foto",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val templatesData = listOf(
                                        Triple("ratio", Icons.Default.Crop, Color(0xFF9C27B0)),
                                        Triple("perspective", Icons.Default.Transform, Color(0xFF4CAF50)),
                                        Triple("remove object", Icons.Default.Brush, Color(0xFFFF9800))
                                    )

                                    templatesData.forEach { (template, icon, baseColor) ->
                                        val isSelected = selectedEditOptions.contains(template)
                                        val backgroundColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.08f)
                                        val borderColor = if (isSelected) baseColor else baseColor.copy(alpha = 0.3f)
                                        val contentColor = if (isSelected) Color.White else baseColor
                                        val borderStroke = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)

                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    if (!isSavingListing) {
                                                        selectedEditOptions = if (isSelected) {
                                                            selectedEditOptions - template
                                                        } else {
                                                            selectedEditOptions + template
                                                        }
                                                    }
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = backgroundColor),
                                            border = borderStroke
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 12.dp, horizontal = 4.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = icon,
                                                    contentDescription = template,
                                                    tint = contentColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = template,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = contentColor
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = inputJudul,
                                    onValueChange = { inputJudul = it },
                                    label = { Text("Judul Kustom (misal: Cluster Jasmine)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = catatanManual,
                                    onValueChange = { catatanManual = it },
                                    label = { Text("Catatan Lainnya") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )

                                val cleanActiveId = activeId.trim()
                                val isDuplicateActiveId = remember(cleanActiveId, listings, processedListings) {
                                    cleanActiveId.isNotBlank() && (
                                        listings.any { it.idListing.trim().equals(cleanActiveId, ignoreCase = true) } ||
                                        processedListings.any { it.first.trim().equals(cleanActiveId, ignoreCase = true) }
                                    )
                                }

                                if (isDuplicateActiveId) {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Warning,
                                                contentDescription = "Peringatan Duplicate",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = "ID $cleanActiveId sudah di input ke meeting!",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.removePendingScrapedListing(activeId)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Abaikan")
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            if (isVoiceListening) {
                                                jarvisVoiceManager?.stopListening()
                                            } else {
                                                jarvisVoiceManager?.startJarvisCommandFlow()
                                            }
                                        },
                                        modifier = Modifier.weight(1.1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = if (isVoiceListening) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                                            contentColor = if (isVoiceListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        ),
                                        border = BorderStroke(1.dp, if (isVoiceListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (isVoiceListening) Icons.Default.Mic else Icons.Default.MicNone,
                                                contentDescription = "Voice",
                                                tint = if (isVoiceListening) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = if (isVoiceListening) (voiceStatusText ?: "Voice") else "Voice",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (isDuplicateActiveId) {
                                                Toast.makeText(context, "ID sudah di input ke meeting di tanggal ini!", Toast.LENGTH_SHORT).show()
                                                return@Button
                                            }
                                            isSavingListing = true
                                            viewModel.addWeeklyMeetingListing(
                                                month = month,
                                                dateStr = dateStr,
                                                idListing = activeId,
                                                namaMe = namaMe,
                                                keterangan = selectedKeterangan,
                                                catatan = computedCatatan,
                                                onResult = { success, msg ->
                                                    isSavingListing = false
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    if (success) {
                                                        processedListings = processedListings + (activeId to "$namaMe ($selectedKeterangan)")
                                                        viewModel.removePendingScrapedListing(activeId)
                                                    }
                                                }
                                            )
                                        },
                                        modifier = Modifier.weight(1.2f),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !isSavingListing && !isDuplicateActiveId
                                    ) {
                                        if (isSavingListing) {
                                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Tambah")
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        HackerTerminalScreen(
                            isScrapingActive = isScrapingActive,
                            totalHotListings = totalWebHotListings,
                            modifier = Modifier.fillMaxWidth().weight(1f)
                        )
                    }
                }
                
                if (activePreviewImages != null) {
                    FullScreenImageGalleryDialog(
                        images = activePreviewImages!!,
                        initialIndex = activePreviewIndex,
                        onDismiss = { activePreviewImages = null }
                    )
                }
            }
        }
    }

    // FAB manual add meeting dialog
    if (showAddManualDialog) {
        AddMeetingListingDialog(
            viewModel = viewModel,
            month = month,
            dateStr = dateStr,
            onDismiss = {
                showAddManualDialog = false
                // Restart scraping in case it was paused while the dialog was open
                if (!isScrapingActive) {
                    viewModel.startAutoScraping(
                        listings.map { it.idListing } + processedListings.map { it.first }
                    )
                }
            }
        )
    }
}

@Composable
fun HackerTerminalScreen(
    isScrapingActive: Boolean,
    totalHotListings: String,
    modifier: Modifier = Modifier
) {
    val terminalLogs = remember {
        mutableStateListOf(
            "root@raywhite-bot:~# ./start_scrape.sh",
            "[SYSTEM] Initializing scraping daemon...",
            "[NET] Connecting to raywhitecipete.net:443..."
        )
    }

    val templateLogs = listOf(
        "[SECURE] TLS v1.3 handshake established.",
        "[PARSING] Scanning listing elements...",
        "[COMPARE] Comparing listing hashes...",
        "[MONITOR] Target: /listings/hot-properties",
        "[STATUS] 0 new HOT listings. Listening...",
        "[CRAWL] Fetching DOM content from Ray White...",
        "[HTTP] GET /agents/all HTTP/1.1 - 200 OK",
        "[USER-AGENT] Rotating client identities...",
        "[SYSTEM] Threadpool standard-worker-4 running...",
        "[STATUS] No updates. Delaying next crawl by 5000ms...",
        "[FILTER] Matching filter criteria: 'HOT'",
        "[PACKET] Payload size 142.4 KB verified.",
        "[HEALTH] Connection status: EXCELLENT | Latency: 32ms",
        "[DATABASE] Local cache synced. 0 inserts."
    )

    // Append new logs periodically when scraping is active
    LaunchedEffect(isScrapingActive) {
        if (isScrapingActive) {
            while (true) {
                kotlinx.coroutines.delay(1200)
                if (terminalLogs.size > 12) {
                    terminalLogs.removeAt(0)
                }
                terminalLogs.add(templateLogs.random())
            }
        } else {
            terminalLogs.add("[WARNING] Scraper paused by administrator.")
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "terminalPulse")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    val scanlineOffsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanline"
    )

    val mainColor = if (isScrapingActive) Color(0xFF00FF66) else MaterialTheme.colorScheme.error

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF050B05))
            .border(2.dp, mainColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
    ) {
        if (isScrapingActive) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val height = size.height
                val width = size.width
                val lineY = height * scanlineOffsetY
                
                drawLine(
                    color = mainColor.copy(alpha = 0.3f),
                    start = Offset(0f, lineY),
                    end = Offset(width, lineY),
                    strokeWidth = 3f
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            mainColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        startY = lineY - 40f,
                        endY = lineY + 40f
                    ),
                    topLeft = Offset(0f, lineY - 40f),
                    size = androidx.compose.ui.geometry.Size(width, 80f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(mainColor, shape = CircleShape)
                    )
                    Text(
                        text = "RAYWHITE_GATEWAY_MONITOR.sh",
                        color = mainColor,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Text(
                    text = if (isScrapingActive) "LIVE_CRAWL" else "PAUSED",
                    color = mainColor.copy(alpha = if (isScrapingActive) cursorAlpha else 1f),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = mainColor.copy(alpha = 0.2f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            // Real-time HUD Panel displaying total hot properties
            Card(
                colors = CardDefaults.cardColors(containerColor = mainColor.copy(alpha = 0.05f)),
                border = BorderStroke(1.dp, mainColor.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL HOT PROPERTIES (WEB)",
                            color = mainColor.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = mainColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = totalHotListings,
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                )
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .background(mainColor.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "LIVE TELEMETRY",
                            color = mainColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (isScrapingActive) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    reverseLayout = true
                ) {
                    val reversedList = terminalLogs.reversed()
                    items(reversedList) { log ->
                        Text(
                            text = log,
                            color = if (log.contains("root@")) Color(0xFF00E676) else if (log.contains("GET")) Color(0xFF29B6F6) else mainColor.copy(alpha = 0.85f),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Daemon Stopped",
                        tint = mainColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "DAEMON STANDBY",
                        color = mainColor,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap the start button at the top right corner to initiate the hot listings crawler process.",
                        color = mainColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer / prompt input
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "rw-bot@sys_ops:$ ",
                    color = mainColor.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                )
                Text(
                    text = if (isScrapingActive) "monitoring_raywhitecipete..." else "daemon_idle_waiting_sigstart",
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                )
                Box(
                    modifier = Modifier
                        .size(width = 6.dp, height = 12.dp)
                        .background(mainColor.copy(alpha = cursorAlpha))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinVerificationDialog(
    onVerified: () -> Unit,
    onDismiss: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Header / Icon
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 48.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(
                                color = if (pinError) MaterialTheme.colorScheme.errorContainer 
                                        else MaterialTheme.colorScheme.primaryContainer,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (pinError) Icons.Default.Warning else Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = if (pinError) MaterialTheme.colorScheme.error 
                                   else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Keamanan Ekstra",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Masukkan PIN 4-digit untuk mengakses fitur Auto Sync Web",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // PIN Dots Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val isFilled = i < enteredPin.length
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        color = if (isFilled) {
                                            if (pinError) MaterialTheme.colorScheme.error 
                                            else MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isFilled) Color.Transparent 
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    )
                            )
                        }
                    }

                    if (pinError) {
                        Text(
                            text = "PIN Salah! Silakan coba lagi.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }

                // Keypad Layout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("Batal", "0", "Hapus")
                    )

                    for (row in keys) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            modifier = Modifier.fillMaxWidth(0.85f)
                        ) {
                            for (key in row) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1.5f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (key == "Batal" || key == "Hapus") Color.Transparent 
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                        )
                                        .clickable {
                                            if (key == "Batal") {
                                                onDismiss()
                                            } else if (key == "Hapus") {
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                    pinError = false
                                                }
                                            } else {
                                                if (enteredPin.length < 4) {
                                                    enteredPin += key
                                                    pinError = false
                                                    if (enteredPin.length == 4) {
                                                        if (enteredPin == "2332") {
                                                            onVerified()
                                                        } else {
                                                            pinError = true
                                                            enteredPin = ""
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        .padding(8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = key,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = if (key == "Batal" || key == "Hapus") 16.sp else 24.sp
                                        ),
                                        color = if (key == "Batal") MaterialTheme.colorScheme.error 
                                                else MaterialTheme.colorScheme.onSurfaceVariant
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
fun IgPostingInfoCard(
    history: ScheduleViewModel.IgPostingHistory,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (history.isPosted) {
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (history.isPosted) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            }
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = if (history.isPosted) Icons.Default.CheckCircle else Icons.Default.Info,
                contentDescription = null,
                tint = if (history.isPosted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(24.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (history.isPosted) "Terdeteksi Post IG 2026" else "Belum Pernah Post IG 2026",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (history.isPosted) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (history.isPosted) {
                    Text(
                        text = "Diposting sebanyak ${history.count} kali.",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tanggal Post:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )
                    history.dates.forEach { date ->
                        Text(
                            text = "• $date",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Belum ada riwayat postingan di Instagram untuk tahun ini.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
