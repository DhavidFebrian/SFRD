package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.ScheduleViewModel
import com.example.ui.SyncState
import java.text.SimpleDateFormat
import java.util.*

// List of Jakarta Selatan keywords for Trade Area classification
private val JAKSEL_AREA_KEYWORDS = listOf(
    "tb simatupang", "simatupang", "kebagusan", "cilandak barat", "cilandak timur", "cilandak",
    "cipete dalam", "cipete utara", "cipete selatan", "cipete", "kemang", "jagakarsa",
    "pondok indah", "ampera", "kebayoran baru", "kebayoran lama utara", "kebayoran lama selatan",
    "kebayoran lama", "kebayoran", "senopati", "bintaro", "tebet barat", "tebet timur", "tebet",
    "pejaten barat", "pejaten timur", "pejaten", "cilodong", "pasar minggu", "gandaria utara",
    "gandaria selatan", "gandaria", "mampang prapatan", "mampang", "pancoran", "setiabudi",
    "kalibata", "ciganjur", "lenteng agung", "ragunan", "tanjung barat", "pesanggrahan",
    "cipulir", "pondok pinang", "lebak bulus", "fatmawati", "blok m", "radio dalam",
    "dharmawangsa", "darmawangsa", "panglima polim", "permata hijau", "senayan", "sudirman",
    "kuningan", "menteng dalam", "menteng", "prapanca", "wijaya", "pondok labu", "petukangan",
    "ulujami", "kebon baru", "manggarai", "pasar manggis", "karet semanggi", "karet pedurenan",
    "karet tengsin", "karet", "gatot subroto", "gatsu", "rasuna said", "mega kuningan", "scbd",
    "pengadegan", "jatipadang", "buncit", "warung buncit", "duren tiga", "bangka",
    "tendean", "kapten tendean", "petogogan", "melawai", "cipulo", "tanah kusir",
    "jakarta selatan", "jaksel"
).sortedByDescending { it.length }

private fun isTradeAreaJaksel(catatan: String, scrapedTitle: String): Boolean {
    val combined = "${catatan.lowercase()} ${scrapedTitle.lowercase()}"
    return JAKSEL_AREA_KEYWORDS.any { keyword -> combined.contains(keyword) }
}

private fun isTurunHarga(catatan: String): Boolean {
    return catatan.lowercase().contains("turun harga")
}

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
    val listingDescMap by viewModel.listingDescMap.collectAsStateWithLifecycle()
    val agentInfoMap by viewModel.agentInfoMap.collectAsStateWithLifecycle()

    var selectedTabState by remember { mutableStateOf(0) } // 0 = Unscheduled, 1 = Scheduled
    var searchQuery by remember { mutableStateOf("") }
    
    // Collapse state for search/month filters - default COLLAPSED
    var isFilterExpanded by remember { mutableStateOf(false) }

    // Month synced with WeeklyMeeting selected month, but overridable inside this dialog
    val globalSelectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    var dialogMonth by remember { mutableStateOf(globalSelectedMonth) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    val allMonths = listOf("Januari","Februari","Maret","April","Mei","Juni",
        "Juli","Agustus","September","Oktober","November","Desember")

    // State for the native Date Picker
    var showDatePickerForListing by remember { mutableStateOf<com.example.network.MeetingListing?>(null) }
    var isSubmittingUpdate by remember { mutableStateOf<com.example.network.MeetingListing?>(null) }
    
    // State for detail popup when card is clicked (Unscheduled section)
    var detailListing by remember { mutableStateOf<com.example.network.MeetingListing?>(null) }
    
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
            val isPosted = it.postingIg.trim().lowercase() in listOf("done", "ya", "yes", "true", "✔", "1")
            noJadwal && !isPosted
        }

        // Scheduled: has jadwal posting (regardless of whether already posted or not)
        val scheduled = filteredListings.filter {
            val jadwal = it.jadwalPosting.trim()
            jadwal.isNotEmpty() && jadwal != "-" && !jadwal.lowercase().contains("belum")
        }.sortedBy { listing ->
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
                            onClick = { viewModel.fetchWeeklyMeetingIgListings(dialogMonth, forceRefresh = true) },
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
                // ── Collapsible Filter Area (Search + Month) ────────────────────────
                // Toggle button row (always visible)
                Surface(
                    onClick = { isFilterExpanded = !isFilterExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isFilterExpanded || searchQuery.isNotBlank())
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = BorderStroke(
                        1.dp,
                        if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "Filter: \"$searchQuery\" · $dialogMonth"
                                   else "Cari ID / Bulan ($dialogMonth) · Ketuk untuk filter",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(20.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Icon(
                            imageVector = if (isFilterExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Expanded filter: Search + Month
                AnimatedVisibility(
                    visible = isFilterExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Month selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                                isFilterExpanded = false
                                            }
                                        )
                                    }
                                }
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
                    if (igSyncStatus is SyncState.Loading && igListings.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (page == 0) {
                        // ── UNSCHEDULED: 3-section split ─────────────────────────────────
                        UnscheduledThreeSectionContent(
                            unscheduledList = unscheduledList,
                            listingImagesMap = listingImagesMap,
                            listingTitleMap = listingTitleMap,
                            listingPriceMap = listingPriceMap,
                            listingDescMap = listingDescMap,
                            onScheduleClick = { listing -> showDatePickerForListing = listing },
                            isSubmittingUpdate = isSubmittingUpdate,
                            onDetailClick = { listing -> detailListing = listing }
                        )
                    } else {
                        // ── SCHEDULED: existing list layout ──────────────────────────────
                        if (scheduledList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarToday,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Belum Ada Jadwal",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Tidak ada postingan IG yang telah terjadwal.",
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
                                items(scheduledList, key = { "${it.idListing}_${it.no}_${it.colIndex}" }) { listing ->
                                    val cleanId = listing.idListing.trim()
                                    val imageUrl = listingImagesMap[cleanId]
                                    val title = listingTitleMap[cleanId]
                                    val price = listingPriceMap[cleanId]

                                    ScheduledListingCard(
                                        listing = listing,
                                        imageUrl = imageUrl,
                                        title = title,
                                        price = price,
                                        isSubmittingUpdate = isSubmittingUpdate,
                                        onScheduleClick = { showDatePickerForListing = listing },
                                        onClearSchedule = {
                                            isSubmittingUpdate = listing
                                            viewModel.updateWeeklyMeetingSchedule(
                                                dateStr = listing.date,
                                                row = listing.no,
                                                colIndex = listing.colIndex,
                                                jadwalPosting = "-",
                                                photoMonth = dialogMonth,
                                                onResult = { _, _ -> isSubmittingUpdate = null }
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Detail popup for unscheduled card click ────────────────────────────────
    if (detailListing != null) {
        val listing = detailListing!!
        val cleanId = listing.idListing.trim()
        val title = listingTitleMap[cleanId] ?: "Memuat judul..."
        val price = listingPriceMap[cleanId]
        val desc = listingDescMap[cleanId]
        val imageUrl = listingImagesMap[cleanId]

        Dialog(
            onDismissRequest = { detailListing = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "ID: $cleanId",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Detail Listing",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        IconButton(onClick = { detailListing = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }

                    HorizontalDivider()

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Thumbnail + basic info
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (imageUrl != null) {
                                    AsyncImage(
                                        model = imageUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(Icons.Default.HomeWork, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
                                }
                            }

                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 3, overflow = TextOverflow.Ellipsis
                                )
                                if (!price.isNullOrBlank()) {
                                    Text(
                                        text = price,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Person, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "ME: ${listing.namaMe.uppercase()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Status badges row
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Pernah diposting badge
                            val wasPosted = listing.postingIg.trim().lowercase() in listOf("done", "ya", "yes", "true", "✔", "1")
                            val hadSchedule = listing.jadwalPosting.trim().let { j ->
                                j.isNotEmpty() && j != "-" && !j.lowercase().contains("belum")
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (wasPosted) Color(0xFF4CAF50).copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (wasPosted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (wasPosted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = if (wasPosted) "Sudah Pernah Diposting" else "Belum Pernah Diposting",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (wasPosted) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (hadSchedule && !wasPosted) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f)
                                ) {
                                    Text(
                                        text = "Pernah dijadwalkan",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Catatan
                        if (listing.catatan.trim().isNotEmpty()) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.StickyNote2, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Catatan: ${listing.catatan}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Deskripsi lengkap dari web
                        if (!desc.isNullOrBlank()) {
                            Column {
                                Text(
                                    text = "Deskripsi Listing (Web)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(12.dp),
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Deskripsi belum dimuat. Buka app dan biarkan data listing ter-fetch.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                            )
                        }
                    }

                    // Bottom: Atur Tanggal button
                    HorizontalDivider()
                    Button(
                        onClick = {
                            detailListing = null
                            showDatePickerForListing = listing
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Atur Tanggal Posting")
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

// ── Unscheduled 3-section content ─────────────────────────────────────────────
@Composable
private fun UnscheduledThreeSectionContent(
    unscheduledList: List<com.example.network.MeetingListing>,
    listingImagesMap: Map<String, String>,
    listingTitleMap: Map<String, String>,
    listingPriceMap: Map<String, String>,
    listingDescMap: Map<String, String>,
    onScheduleClick: (com.example.network.MeetingListing) -> Unit,
    isSubmittingUpdate: com.example.network.MeetingListing?,
    onDetailClick: (com.example.network.MeetingListing) -> Unit
) {
    // Classify into 3 buckets
    val turunHargaList = remember(unscheduledList) {
        unscheduledList.filter { isTurunHarga(it.catatan) }
    }
    val tradeAreaList = remember(unscheduledList, listingTitleMap) {
        unscheduledList.filter { listing ->
            !isTurunHarga(listing.catatan) &&
            isTradeAreaJaksel(listing.catatan, listingTitleMap[listing.idListing.trim()] ?: "")
        }
    }
    val igList = remember(unscheduledList, listingTitleMap) {
        unscheduledList.filter { listing ->
            !isTurunHarga(listing.catatan) &&
            !isTradeAreaJaksel(listing.catatan, listingTitleMap[listing.idListing.trim()] ?: "")
        }
    }

    if (unscheduledList.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Semua Terjadwal!",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Semua postingan IG sudah diatur tanggal postingnya.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    // Section expand states
    var turunHargaExpanded by remember { mutableStateOf(false) }
    var tradeAreaExpanded by remember { mutableStateOf(false) }
    var igExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // ── Section 1: Turun Harga ─────────────────────────────────────────────
        item(key = "section_turun_harga") {
            UnscheduledSectionHeader(
                icon = Icons.Default.TrendingDown,
                iconTint = Color(0xFFE53935),
                title = "Turun Harga",
                count = turunHargaList.size,
                isExpanded = turunHargaExpanded,
                onToggle = { turunHargaExpanded = !turunHargaExpanded },
                color = Color(0xFFE53935)
            )
        }
        item(key = "section_turun_harga_grid") {
            AnimatedVisibility(
                visible = turunHargaExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (turunHargaList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tidak ada listing turun harga",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    UnscheduledGrid(
                        listings = turunHargaList,
                        listingImagesMap = listingImagesMap,
                        listingTitleMap = listingTitleMap,
                        listingPriceMap = listingPriceMap,
                        accentColor = Color(0xFFE53935),
                        onScheduleClick = onScheduleClick,
                        isSubmittingUpdate = isSubmittingUpdate,
                        onDetailClick = onDetailClick
                    )
                }
            }
        }

        item(key = "divider1") { Spacer(modifier = Modifier.height(8.dp)) }

        // ── Section 2: Trade Area (Jakarta Selatan) ────────────────────────────
        item(key = "section_trade_area") {
            UnscheduledSectionHeader(
                icon = Icons.Default.Map,
                iconTint = Color(0xFF1565C0),
                title = "Trade Area Jakarta Selatan",
                count = tradeAreaList.size,
                isExpanded = tradeAreaExpanded,
                onToggle = { tradeAreaExpanded = !tradeAreaExpanded },
                color = Color(0xFF1565C0)
            )
        }
        item(key = "section_trade_area_grid") {
            AnimatedVisibility(
                visible = tradeAreaExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (tradeAreaList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tidak ada listing area Jakarta Selatan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    UnscheduledGrid(
                        listings = tradeAreaList,
                        listingImagesMap = listingImagesMap,
                        listingTitleMap = listingTitleMap,
                        listingPriceMap = listingPriceMap,
                        accentColor = Color(0xFF1565C0),
                        onScheduleClick = onScheduleClick,
                        isSubmittingUpdate = isSubmittingUpdate,
                        onDetailClick = onDetailClick
                    )
                }
            }
        }

        item(key = "divider2") { Spacer(modifier = Modifier.height(8.dp)) }

        // ── Section 3: IG (Others) ─────────────────────────────────────────────
        item(key = "section_ig") {
            UnscheduledSectionHeader(
                icon = Icons.Default.CameraAlt,
                iconTint = Color(0xFF7B1FA2),
                title = "IG (Lainnya)",
                count = igList.size,
                isExpanded = igExpanded,
                onToggle = { igExpanded = !igExpanded },
                color = Color(0xFF7B1FA2)
            )
        }
        item(key = "section_ig_grid") {
            AnimatedVisibility(
                visible = igExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (igList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tidak ada listing lainnya",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    UnscheduledGrid(
                        listings = igList,
                        listingImagesMap = listingImagesMap,
                        listingTitleMap = listingTitleMap,
                        listingPriceMap = listingPriceMap,
                        accentColor = Color(0xFF7B1FA2),
                        onScheduleClick = onScheduleClick,
                        isSubmittingUpdate = isSubmittingUpdate,
                        onDetailClick = onDetailClick
                    )
                }
            }
        }

        item(key = "bottom_space") { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun UnscheduledSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    count: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    color: Color
) {
    Surface(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = color,
                modifier = Modifier.weight(1f)
            )
            Surface(
                shape = CircleShape,
                color = color.copy(alpha = 0.15f)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                    color = color,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = color.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun UnscheduledGrid(
    listings: List<com.example.network.MeetingListing>,
    listingImagesMap: Map<String, String>,
    listingTitleMap: Map<String, String>,
    listingPriceMap: Map<String, String>,
    accentColor: Color,
    onScheduleClick: (com.example.network.MeetingListing) -> Unit,
    isSubmittingUpdate: com.example.network.MeetingListing?,
    onDetailClick: (com.example.network.MeetingListing) -> Unit
) {
    // Non-lazy grid: manual 2-column layout using chunked rows
    // (LazyVerticalGrid cannot be nested in LazyColumn directly)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listings.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { listing ->
                    UnscheduledCard(
                        listing = listing,
                        imageUrl = listingImagesMap[listing.idListing.trim()],
                        title = listingTitleMap[listing.idListing.trim()],
                        price = listingPriceMap[listing.idListing.trim()],
                        accentColor = accentColor,
                        onScheduleClick = onScheduleClick,
                        isSubmitting = isSubmittingUpdate == listing,
                        onDetailClick = onDetailClick,
                        modifier = Modifier.weight(1f)
                    )
                }
                // If odd count, fill the remaining space
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun UnscheduledCard(
    listing: com.example.network.MeetingListing,
    imageUrl: String?,
    title: String?,
    price: String?,
    accentColor: Color,
    onScheduleClick: (com.example.network.MeetingListing) -> Unit,
    isSubmitting: Boolean,
    onDetailClick: (com.example.network.MeetingListing) -> Unit,
    modifier: Modifier = Modifier
) {
    val cleanId = listing.idListing.trim()

    Card(
        onClick = { onDetailClick(listing) },
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.3f)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.HomeWork,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }
                // ID badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "ID: $cleanId",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        fontSize = 9.sp
                    )
                }
            }

            // Card body
            Column(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title ?: "Memuat...",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.sp
                )
                if (!price.isNullOrBlank()) {
                    Text(
                        text = price,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = accentColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "ME: ${listing.namaMe.uppercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 9.sp
                )

                // Notes from catatan spreadsheet
                if (listing.catatan.trim().isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                Icons.Default.StickyNote2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(8.dp)
                            )
                            Text(
                                text = listing.catatan.trim(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                fontSize = 8.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Atur Tanggal button
                if (isSubmitting) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    }
                } else {
                    Button(
                        onClick = { onScheduleClick(listing) },
                        modifier = Modifier.fillMaxWidth().height(28.dp),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor.copy(alpha = 0.12f),
                            contentColor = accentColor
                        )
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Atur Tanggal", fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            }
        }
    }
}

// ── Scheduled listing card (extracted for clarity) ─────────────────────────────
@Composable
private fun ScheduledListingCard(
    listing: com.example.network.MeetingListing,
    imageUrl: String?,
    title: String?,
    price: String?,
    isSubmittingUpdate: com.example.network.MeetingListing?,
    onScheduleClick: () -> Unit,
    onClearSchedule: () -> Unit
) {
    val cleanId = listing.idListing.trim()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
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
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = try {
                                    val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(listing.date)
                                    SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(parsed!!)
                                } catch (e: Exception) { listing.date },
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = title ?: "Memuat info listing...",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!price.isNullOrBlank()) {
                        Text(
                            text = price,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
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
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = formatJadwalPostingDate(listing.jadwalPosting),
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                if (isSubmittingUpdate == listing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalIconButton(
                            onClick = onClearSchedule,
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Hapus Jadwal", modifier = Modifier.size(18.dp))
                        }

                        Button(
                            onClick = onScheduleClick,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 12.sp)
                        }
                    }
                }
            }
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
    clean = clean.replace(Regex("\\b\\d{1,2}[:\\.]\\d{2}([:\\.]\\d{2})?\\b"), "")
    clean = clean.replace(Regex("(?i)\\bGMT[+\\-\\d:]*\\b"), "")
    clean = clean.replace(Regex("\\([^)]*\\)"), "")
    val tzAbbrev = listOf("WIB", "WITA", "WIT", "UTC", "PST", "PDT", "EST", "EDT")
    for (abbrev in tzAbbrev) {
        clean = clean.replace(Regex("(?i)\\b$abbrev\\b"), "")
    }
    clean = clean.replace(Regex("\\s+"), " ").trim()

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
    val fallbackFuture = Date(253402300799000L)
    if (trimmed.isEmpty() || trimmed == "-" || trimmed.lowercase().contains("belum")) return fallbackFuture

    try {
        if (trimmed.contains("T")) {
            val datePart = trimmed.substringBefore("T")
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            parser.parse(datePart)?.let { return it }
        }
    } catch (e: Exception) {}

    try {
        val tokens = trimmed.split(" ").filter { it.isNotBlank() }
        if (tokens.size >= 4) {
            val firstFour = tokens.take(4).joinToString(" ")
            val parser = SimpleDateFormat("EEE MMM dd yyyy", Locale.US)
            parser.parse(firstFour)?.let { return it }
        }
    } catch (e: Exception) {}

    val formats = listOf(
        "yyyy-MM-dd",
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "yyyy/MM/dd",
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

    val norm = com.example.data.normalizeDate(trimmed)
    if (norm.isNotBlank()) {
        try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            parser.parse(norm)?.let { return it }
        } catch (e: Exception) {}
    }

    return fallbackFuture
}

/**
 * Embeddable version of SchedulingDialog content.
 * This renders the same scheduling UI but as a regular composable (not a Dialog),
 * suitable for embedding in PublishScreen tabs.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SchedulingScreenContent(
    viewModel: ScheduleViewModel,
    modifier: Modifier = Modifier
) {
    val igSyncStatus by viewModel.weeklyMeetingIgSyncStatus.collectAsStateWithLifecycle()
    val igListings by viewModel.weeklyMeetingIgListings.collectAsStateWithLifecycle()

    val listingImagesMap by viewModel.listingImagesMap.collectAsStateWithLifecycle()
    val listingTitleMap by viewModel.listingTitleMap.collectAsStateWithLifecycle()
    val listingPriceMap by viewModel.listingPriceMap.collectAsStateWithLifecycle()
    val listingDescMap by viewModel.listingDescMap.collectAsStateWithLifecycle()
    val agentInfoMap by viewModel.agentInfoMap.collectAsStateWithLifecycle()

    var selectedTabState by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isFilterExpanded by remember { mutableStateOf(false) }

    val globalSelectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    var dialogMonth by remember { mutableStateOf(globalSelectedMonth) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    val allMonths = listOf("Januari","Februari","Maret","April","Mei","Juni",
        "Juli","Agustus","September","Oktober","November","Desember")

    var showDatePickerForListing by remember { mutableStateOf<com.example.network.MeetingListing?>(null) }
    var isSubmittingUpdate by remember { mutableStateOf<com.example.network.MeetingListing?>(null) }
    var detailListing by remember { mutableStateOf<com.example.network.MeetingListing?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(dialogMonth) {
        viewModel.fetchWeeklyMeetingIgListings(dialogMonth)
    }

    val processedLists = remember(igListings, searchQuery) {
        val filteredListings = if (searchQuery.isBlank()) igListings
        else igListings.filter {
            it.idListing.contains(searchQuery, ignoreCase = true) ||
            it.namaMe.contains(searchQuery, ignoreCase = true)
        }
        val unscheduled = filteredListings.filter {
            val jadwal = it.jadwalPosting.trim()
            val noJadwal = jadwal.isEmpty() || jadwal == "-" || jadwal.lowercase().contains("belum")
            val isPosted = it.postingIg.trim().lowercase() in listOf("done", "ya", "yes", "true", "✔", "1")
            noJadwal && !isPosted
        }
        val scheduled = filteredListings.filter {
            val jadwal = it.jadwalPosting.trim()
            jadwal.isNotEmpty() && jadwal != "-" && !jadwal.lowercase().contains("belum")
        }.sortedBy { parseJadwalPostingDateToDate(it.jadwalPosting) }
        Pair(unscheduled, scheduled)
    }

    val (unscheduledList, scheduledList) = processedLists
    val pagerState = androidx.compose.foundation.pager.rememberPagerState(pageCount = { 2 })

    LaunchedEffect(selectedTabState) {
        if (pagerState.currentPage != selectedTabState) pagerState.animateScrollToPage(selectedTabState)
    }
    LaunchedEffect(pagerState.currentPage) {
        selectedTabState = pagerState.currentPage
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Collapsible Filter Area
            Surface(
                onClick = { isFilterExpanded = !isFilterExpanded },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isFilterExpanded || searchQuery.isNotBlank())
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                border = BorderStroke(
                    1.dp,
                    if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "Filter: \"$searchQuery\" · $dialogMonth"
                               else "Cari ID / Bulan ($dialogMonth) · Ketuk untuk filter",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = if (searchQuery.isNotBlank()) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Icon(
                        imageVector = if (isFilterExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isFilterExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp).padding(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Bulan:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
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
                                            isFilterExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Tabs
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

            // Pager content
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) { page ->
                if (igSyncStatus is SyncState.Loading && igListings.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (page == 0) {
                    UnscheduledThreeSectionContent(
                        unscheduledList = unscheduledList,
                        listingImagesMap = listingImagesMap,
                        listingTitleMap = listingTitleMap,
                        listingPriceMap = listingPriceMap,
                        listingDescMap = listingDescMap,
                        onScheduleClick = { listing -> showDatePickerForListing = listing },
                        isSubmittingUpdate = isSubmittingUpdate,
                        onDetailClick = { listing -> detailListing = listing }
                    )
                } else {
                    if (scheduledList.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Belum Ada Jadwal", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tidak ada postingan IG yang telah terjadwal.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline, textAlign = TextAlign.Center)
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(scheduledList, key = { "${it.idListing}_${it.no}_${it.colIndex}" }) { listing ->
                                val cleanId = listing.idListing.trim()
                                ScheduledListingCard(
                                    listing = listing,
                                    imageUrl = listingImagesMap[cleanId],
                                    title = listingTitleMap[cleanId],
                                    price = listingPriceMap[cleanId],
                                    isSubmittingUpdate = isSubmittingUpdate,
                                    onScheduleClick = { showDatePickerForListing = listing },
                                    onClearSchedule = {
                                        isSubmittingUpdate = listing
                                        viewModel.updateWeeklyMeetingSchedule(
                                            dateStr = listing.date, row = listing.no, colIndex = listing.colIndex,
                                            jadwalPosting = "-", photoMonth = dialogMonth,
                                            onResult = { _, _ -> isSubmittingUpdate = null }
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail popup
    if (detailListing != null) {
        val listing = detailListing!!
        val cleanId = listing.idListing.trim()
        val title = listingTitleMap[cleanId] ?: "Memuat judul..."
        val price = listingPriceMap[cleanId]
        val desc = listingDescMap[cleanId]
        val imageUrl = listingImagesMap[cleanId]

        Dialog(
            onDismissRequest = { detailListing = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f).padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("ID: $cleanId",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Detail Listing", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        }
                        IconButton(onClick = { detailListing = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Tutup")
                        }
                    }
                    HorizontalDivider()
                    Column(
                        modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier.size(90.dp).clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                if (imageUrl != null) {
                                    coil.compose.AsyncImage(model = imageUrl, contentDescription = null,
                                        modifier = Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Crop)
                                } else {
                                    Icon(Icons.Default.HomeWork, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
                                }
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 3, overflow = TextOverflow.Ellipsis)
                                if (!price.isNullOrBlank()) {
                                    Text(price, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black),
                                        color = MaterialTheme.colorScheme.primary)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Person, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(12.dp))
                                    Text("ME: ${listing.namaMe.uppercase()}", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (listing.catatan.trim().isNotEmpty()) {
                            Surface(modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                shape = RoundedCornerShape(10.dp)) {
                                Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Icon(Icons.Default.StickyNote2, contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(16.dp))
                                    Text("Catatan: ${listing.catatan}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (!desc.isNullOrBlank()) {
                            Column {
                                Text("Deskripsi Listing (Web)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.background,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
                                    Text(desc, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                                        modifier = Modifier.padding(12.dp), lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Button(
                        onClick = { detailListing = null; showDatePickerForListing = listing },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Atur Tanggal Posting")
                    }
                }
            }
        }
    }

    // Date picker
    if (showDatePickerForListing != null) {
        val listing = showDatePickerForListing!!
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePickerForListing = null },
            confirmButton = {
                TextButton(onClick = {
                    val selectedDateMillis = datePickerState.selectedDateMillis
                    if (selectedDateMillis != null) {
                        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(selectedDateMillis))
                        showDatePickerForListing = null
                        isSubmittingUpdate = listing
                        viewModel.updateWeeklyMeetingSchedule(
                            dateStr = listing.date, row = listing.no, colIndex = listing.colIndex,
                            jadwalPosting = formattedDate, photoMonth = dialogMonth,
                            onResult = { _, _ -> isSubmittingUpdate = null }
                        )
                    }
                }) { Text("PILIH") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerForListing = null }) { Text("BATAL") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text("Atur Tanggal Posting IG",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 24.dp, top = 24.dp))
                }
            )
        }
    }
}
