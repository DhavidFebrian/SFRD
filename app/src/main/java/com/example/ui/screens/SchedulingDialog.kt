package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.ScheduleViewModel
import com.example.ui.SyncState
import com.example.ui.AgentInfo
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SchedulingDialog(
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val igSyncStatus by viewModel.weeklyMeetingIgSyncStatus.collectAsStateWithLifecycle()
    val igListings by viewModel.weeklyMeetingIgListings.collectAsStateWithLifecycle()

    // Collect detail maps from ViewModel
    val listingImagesMap by viewModel.listingImagesMap.collectAsStateWithLifecycle()
    val listingTitleMap by viewModel.listingTitleMap.collectAsStateWithLifecycle()
    val listingPriceMap by viewModel.listingPriceMap.collectAsStateWithLifecycle()
    val agentInfoMap by viewModel.agentInfoMap.collectAsStateWithLifecycle()

    var selectedTabState by remember { mutableStateOf(0) } // 0 = Unscheduled, 1 = Scheduled
    var searchQuery by remember { mutableStateOf("") }

    // Month synced with WeeklyMeeting selected month, but overridable inside this dialog
    val globalSelectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    var dialogMonth by remember { mutableStateOf(globalSelectedMonth) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    val allMonths = listOf("Januari","Februari","Maret","April","Mei","Juni",
        "Juli","Agustus","September","Oktober","November","Desember")

    // State for the native Date Picker
    var showDatePickerForListing by remember { mutableStateOf<com.example.network.MeetingListing?>(null) }
    var isSubmittingUpdate by remember { mutableStateOf<com.example.network.MeetingListing?>(null) }
    
    val snackbarHostState = remember { SnackbarHostState() }

    // Fetch when dialog opens or month changes
    LaunchedEffect(dialogMonth) {
        viewModel.fetchWeeklyMeetingIgListings(dialogMonth)
    }

    // Process lists: separate Unscheduled vs Scheduled
    val processedLists = remember(igListings, searchQuery) {
        val filteredListings = if (searchQuery.isBlank()) {
            igListings
        } else {
            igListings.filter {
                it.idListing.contains(searchQuery, ignoreCase = true) ||
                it.namaMe.contains(searchQuery, ignoreCase = true)
            }
        }

        // Unscheduled: no jadwal posting AND postingIg not checked (exclude those already done)
        val unscheduled = filteredListings.filter {
            val jadwal = it.jadwalPosting.trim()
            val noJadwal = jadwal.isEmpty() || jadwal == "-" || jadwal.lowercase().contains("belum")
            val isPosted = it.postingIg.trim().lowercase() in listOf("done", "ya", "yes", "true", "\u2714", "1")
            noJadwal && !isPosted
        }

        // Scheduled: has jadwal posting (regardless of whether already posted or not)
        val scheduled = filteredListings.filter {
            val jadwal = it.jadwalPosting.trim()
            jadwal.isNotEmpty() && jadwal != "-" && !jadwal.lowercase().contains("belum")
        }.sortedByDescending { listing ->
            parseJadwalPostingDateToDate(listing.jadwalPosting)
        }

        Pair(unscheduled, scheduled)
    }

    val (unscheduledList, scheduledList) = processedLists

    // Create a pager state with 2 pages
    val pagerState = rememberPagerState(pageCount = { 2 })

    // Keep TabRow and HorizontalPager synchronized
    LaunchedEffect(selectedTabState) {
        if (pagerState.currentPage != selectedTabState) {
            pagerState.animateScrollToPage(selectedTabState)
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        selectedTabState = pagerState.currentPage
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Instagram Scheduling Desk",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Mengatur jadwal posting konten IG dari Google Sheets",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.fetchWeeklyMeetingIgListings(dialogMonth) },
                            enabled = igSyncStatus !is SyncState.Loading
                        ) {
                            if (igSyncStatus is SyncState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari ID / Nama ME di daftar IG...", fontSize = 14.sp) },
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
                        .padding(16.dp)
                )

                // Month selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Bulan:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    ExposedDropdownMenuBox(
                        expanded = monthDropdownExpanded,
                        onExpandedChange = { monthDropdownExpanded = it },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = dialogMonth,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = monthDropdownExpanded,
                            onDismissRequest = { monthDropdownExpanded = false }
                        ) {
                            allMonths.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m) },
                                    onClick = {
                                        dialogMonth = m
                                        monthDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Tab selectors
                TabRow(
                    selectedTabIndex = selectedTabState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        if (selectedTabState < tabPositions.size) {
                            TabRowDefaults.SecondaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabState]),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                ) {
                    Tab(
                        selected = selectedTabState == 0,
                        onClick = { selectedTabState = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.HourglassEmpty, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Unscheduled (${unscheduledList.size})", fontWeight = if (selectedTabState == 0) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTabState == 1,
                        onClick = { selectedTabState = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EventAvailable, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scheduled (${scheduledList.size})", fontWeight = if (selectedTabState == 1) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    )
                }

                // Horizontal Pager allows swiping between pages with touch gestures
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) { page ->
                    val pageList = if (page == 0) unscheduledList else scheduledList

                    if (igSyncStatus is SyncState.Loading && igListings.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (pageList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (page == 0) Icons.Default.CheckCircleOutline else Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (page == 0) "Semua Terjadwal!" else "Belum Ada Jadwal",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (page == 0) "Semua postingan IG sudah diatur tanggal postingnya." else "Tidak ada postingan IG yang telah terjadwal.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(pageList, key = { "${it.idListing}_${it.no}_${it.colIndex}" }) { listing ->
                                val cleanId = listing.idListing.trim()
                                val imageUrl = listingImagesMap[cleanId]
                                val title = listingTitleMap[cleanId]
                                val price = listingPriceMap[cleanId]
                                val agentInfo = agentInfoMap[cleanId]

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        // 1. Image and Listing Info Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            // Left Thumbnail Image
                                            Box(
                                                modifier = Modifier
                                                    .size(80.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (imageUrl != null) {
                                                    AsyncImage(
                                                        model = imageUrl,
                                                        contentDescription = "Listing image",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.HomeWork,
                                                        contentDescription = "Placeholder",
                                                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }

                                            // Right Info Details
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // ID Badge
                                                    Box(
                                                        modifier = Modifier
                                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = "ID: $cleanId",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }

                                                    // Meeting Date badge
                                                    Surface(
                                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Text(
                                                            text = try {
                                                                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(listing.date)
                                                                SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(parsed!!)
                                                            } catch (e: Exception) {
                                                                listing.date
                                                            },
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                // Title
                                                Text(
                                                    text = title ?: "Memuat info listing...",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                // Price
                                                if (!price.isNullOrBlank()) {
                                                    Text(
                                                        text = price,
                                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }

                                                // ME / Agent Info
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.outline,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = "ME: ${listing.namaMe.uppercase()}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        // Catatan if present
                                        if (listing.catatan.trim().isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.StickyNote2,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.outline,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Text(
                                                        text = "Catatan: ${listing.catatan}",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
                                        Spacer(modifier = Modifier.height(8.dp))

                                        // 2. Scheduled Date and Action Buttons Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "JADWAL POSTING",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                                val isScheduled = page == 1
                                                Surface(
                                                    color = if (isScheduled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = if (isScheduled) {
                                                            formatJadwalPostingDate(listing.jadwalPosting)
                                                        } else {
                                                            "Belum Terjadwal"
                                                        },
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (isScheduled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                    )
                                                }
                                            }

                                            if (isSubmittingUpdate == listing) {
                                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                            } else {
                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    if (page == 1) {
                                                        // Allow clearing schedule
                                                        FilledTonalIconButton(
                                                            onClick = {
                                                                isSubmittingUpdate = listing
                                                                viewModel.updateWeeklyMeetingSchedule(
                                                                    dateStr = listing.date,
                                                                    row = listing.no,
                                                                    colIndex = listing.colIndex,
                                                                    jadwalPosting = "-",
                                                                    photoMonth = dialogMonth,
                                                                    onResult = { success, msg ->
                                                                        isSubmittingUpdate = null
                                                                    }
                                                                )
                                                            },
                                                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                                                contentColor = MaterialTheme.colorScheme.error
                                                            ),
                                                            modifier = Modifier.size(36.dp)
                                                        ) {
                                                            Icon(Icons.Default.Delete, contentDescription = "Hapus Jadwal", modifier = Modifier.size(18.dp))
                                                        }
                                                    }

                                                    Button(
                                                        onClick = { showDatePickerForListing = listing },
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    ) {
                                                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(if (page == 0) "Atur Tanggal" else "Edit", fontSize = 12.sp)
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

    // Modern Native Material 3 Date Picker Dialog
    if (showDatePickerForListing != null) {
        val listing = showDatePickerForListing!!
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePickerForListing = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedDateMillis = datePickerState.selectedDateMillis
                        if (selectedDateMillis != null) {
                            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(selectedDateMillis))
                            showDatePickerForListing = null
                            isSubmittingUpdate = listing
                            
                            viewModel.updateWeeklyMeetingSchedule(
                                dateStr = listing.date,
                                row = listing.no,
                                colIndex = listing.colIndex,
                                jadwalPosting = formattedDate,
                                photoMonth = dialogMonth,
                                onResult = { success, msg ->
                                    isSubmittingUpdate = null
                                }
                            )
                        }
                    }
                ) {
                    Text("PILIH")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerForListing = null }) {
                    Text("BATAL")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Atur Tanggal Posting IG",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp)
                    )
                }
            )
        }
    }
}

/**
 * Standardize dates format: output "EEEE, d MMMM yyyy" (e.g. "Senin, 11 Juni 2026")
 * cleanly removing any GMT, time or zone abbreviations.
 */
private fun formatJadwalPostingDate(rawDate: String): String {
    val trimmed = rawDate.trim()
    if (trimmed.isEmpty() || trimmed == "-") return "Belum Terjadwal"

    // 1. Try parsing ISO/UTC standard format first, e.g., 2026-06-26T10:00:00.000Z
    try {
        if (trimmed.contains("T")) {
            val datePart = trimmed.substringBefore("T")
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val output = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            parser.parse(datePart)?.let { return output.format(it) }
        }
    } catch (e: Exception) {}

    // 2. Try parsing typical JavaScript Date string: e.g., "Fri Jun 26 2026 10:00:00 GMT+0700 (WIB)" or similar
    try {
        val tokens = trimmed.split(" ").filter { it.isNotBlank() }
        if (tokens.size >= 4) {
            val firstFour = tokens.take(4).joinToString(" ")
            val parser = SimpleDateFormat("EEE MMM dd yyyy", Locale.US)
            val output = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            parser.parse(firstFour)?.let { return output.format(it) }
        }
    } catch (e: Exception) {}

    // 3. Try typical SimpleDateFormat patterns
    val formats = listOf(
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "EEEE, dd MMMM yyyy",
        "EEEE, d MMMM yyyy",
        "EEEE, dd-MMMM-yyyy",
        "dd MMMM yyyy",
        "d MMMM yyyy",
        "dd MMM yyyy",
        "d MMM yyyy"
    )

    for (fmt in formats) {
        try {
            val parser = SimpleDateFormat(fmt, Locale("id", "ID"))
            parser.parse(trimmed)?.let {
                val output = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
                return output.format(it)
            }
        } catch (e: Exception) {}
        try {
            val parser = SimpleDateFormat(fmt, Locale.US)
            parser.parse(trimmed)?.let {
                val output = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
                return output.format(it)
            }
        } catch (e: Exception) {}
    }

    // 4. Last resort: manual clean-up of any GMT, timezone, parenthetical info, or time-related garbage
    var clean = trimmed
    // Remove time components like 10:00:00 or 10.00.00
    clean = clean.replace(Regex("\\b\\d{1,2}[:\\.]\\d{2}([:\\.]\\d{2})?\\b"), "")
    // Remove GMT parts like GMT+0700 or (GMT+07:00) or GMT
    clean = clean.replace(Regex("(?i)\\bGMT[+\\-\\d:]*\\b"), "")
    // Remove parenthesized info like (Indochina Time) or (Western Indonesia Time) or (WIB)
    clean = clean.replace(Regex("\\([^)]*\\)"), "")
    // Remove specific timezone abbreviations
    val tzAbbrev = listOf("WIB", "WITA", "WIT", "UTC", "PST", "PDT", "EST", "EDT")
    for (abbrev in tzAbbrev) {
        clean = clean.replace(Regex("(?i)\\b$abbrev\\b"), "")
    }
    // Remove multiple spaces
    clean = clean.replace(Regex("\\s+"), " ").trim()

    // If clean has something like "Fri Jun 26 2026", try parsing it again
    try {
        val tokens = clean.split(" ").filter { it.isNotBlank() }
        if (tokens.size >= 4) {
            val firstFour = tokens.take(4).joinToString(" ")
            val parser = SimpleDateFormat("EEE MMM dd yyyy", Locale.US)
            val output = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
            parser.parse(firstFour)?.let { return output.format(it) }
        }
    } catch (e: Exception) {}

    return clean
}

/**
 * Robustly parse arbitrary date formats from the database back to a Date object for comparisons.
 * Returns Date(0) if parsing fails.
 */
private fun parseJadwalPostingDateToDate(rawDate: String): Date {
    val trimmed = rawDate.trim()
    if (trimmed.isEmpty() || trimmed == "-") return Date(0)

    // 1. Try parsing ISO/UTC standard format first, e.g., 2026-06-26T10:00:00.000Z
    try {
        if (trimmed.contains("T")) {
            val datePart = trimmed.substringBefore("T")
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            parser.parse(datePart)?.let { return it }
        }
    } catch (e: Exception) {}

    // 2. Try parsing typical JavaScript Date string: e.g., "Fri Jun 26 2026 10:00:00 GMT+0700 (WIB)" or similar
    try {
        val tokens = trimmed.split(" ").filter { it.isNotBlank() }
        if (tokens.size >= 4) {
            val firstFour = tokens.take(4).joinToString(" ")
            val parser = SimpleDateFormat("EEE MMM dd yyyy", Locale.US)
            parser.parse(firstFour)?.let { return it }
        }
    } catch (e: Exception) {}

    // 3. Try typical SimpleDateFormat patterns
    val formats = listOf(
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "EEEE, dd MMMM yyyy",
        "EEEE, d MMMM yyyy",
        "EEEE, dd-MMMM-yyyy",
        "dd MMMM yyyy",
        "d MMMM yyyy",
        "dd MMM yyyy",
        "d MMM yyyy"
    )

    for (fmt in formats) {
        try {
            val parser = SimpleDateFormat(fmt, Locale("id", "ID"))
            parser.parse(trimmed)?.let { return it }
        } catch (e: Exception) {}
        try {
            val parser = SimpleDateFormat(fmt, Locale.US)
            parser.parse(trimmed)?.let { return it }
        } catch (e: Exception) {}
    }

    // 4. Last resort: manual clean-up of time components and timezone abbreviations
    try {
        var clean = trimmed
        clean = clean.replace(Regex("\\b\\d{1,2}[:\\.]\\d{2}([:\\.]\\d{2})?\\b"), "")
        clean = clean.replace(Regex("(?i)\\bGMT[+\\-\\d:]*\\b"), "")
        clean = clean.replace(Regex("\\([^)]*\\)"), "")
        val tzAbbrev = listOf("WIB", "WITA", "WIT", "UTC", "PST", "PDT", "EST", "EDT")
        for (abbrev in tzAbbrev) {
            clean = clean.replace(Regex("(?i)\\b$abbrev\\b"), "")
        }
        clean = clean.replace(Regex("\\s+"), " ").trim()
        val tokens = clean.split(" ").filter { it.isNotBlank() }
        if (tokens.size >= 4) {
            val firstFour = tokens.take(4).joinToString(" ")
            val parser = SimpleDateFormat("EEE MMM dd yyyy", Locale.US)
            parser.parse(firstFour)?.let { return it }
        }
    } catch (e: Exception) {}

    return Date(0)
}
