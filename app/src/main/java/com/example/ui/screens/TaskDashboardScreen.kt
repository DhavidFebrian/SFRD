package com.example.ui.screens

import androidx.compose.animation.*
import android.app.DatePickerDialog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.EditFotoTask
import com.example.data.Schedule
import com.example.ui.ScheduleViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import com.example.ui.SyncState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDashboardScreen(
    viewModel: ScheduleViewModel,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier,
    initialSubTab: Int = 0,
    isUploadIgOnly: Boolean = false,
    onNavigateToChat: (() -> Unit)? = null,
    onNavigateToForm: (() -> Unit)? = null
) {
    var selectedSubTab by remember(initialSubTab) { mutableStateOf(initialSubTab) } // 0 = Foto Ulang, 1 = Edit Jadwal, 2 = Upload IG
    
    LaunchedEffect(isUploadIgOnly, initialSubTab) {
        if (isUploadIgOnly) {
            selectedSubTab = 2
        } else if (selectedSubTab == 2) {
            selectedSubTab = 0
        }
    }
    val schedules by viewModel.allSchedules.collectAsState()
    val editFotoTasks by viewModel.allEditFotoTasks.collectAsState()
    val weeklyMeetingIgListings by viewModel.weeklyMeetingIgListings.collectAsState()

    val selectedMonth by viewModel.selectedMonth.collectAsState()
    // Independent month for Upload IG tab — initialized from WeeklyMeeting selected month
    var selectedUploadIgMonth by remember { mutableStateOf(selectedMonth) }
    val igSyncStatus by viewModel.weeklyMeetingIgSyncStatus.collectAsState()

    // When user switches to Upload IG tab, sync the month from WeeklyMeeting selection
    LaunchedEffect(selectedSubTab, selectedMonth) {
        if (selectedSubTab == 2) {
            selectedUploadIgMonth = selectedMonth
            viewModel.fetchWeeklyMeetingIgListings(selectedUploadIgMonth)
        }
    }
    // Fetch when month changes within Upload IG tab
    LaunchedEffect(selectedUploadIgMonth) {
        viewModel.fetchWeeklyMeetingIgListings(selectedUploadIgMonth)
    }
    val syncStatus by viewModel.syncStatus.collectAsState()
    val listingImagesMap by viewModel.listingImagesMap.collectAsState()
    val listingSoldMap by viewModel.listingSoldMap.collectAsState()
    val listingImagesGalleryMap by viewModel.listingImagesGalleryMap.collectAsState()
    val agentInfoMap by viewModel.agentInfoMap.collectAsState()
    val listingTitleMap by viewModel.listingTitleMap.collectAsState()
    val listingDescMap by viewModel.listingDescMap.collectAsState()
    val listingPriceMap by viewModel.listingPriceMap.collectAsState()

    val unreadCount by viewModel.unreadChatCount.collectAsState()
    var selectedScheduleForDetail by remember { mutableStateOf<Schedule?>(null) }
    var activeTaskForDownload by remember { mutableStateOf<EditFotoTask?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTypeFilter by remember { mutableStateOf("Semua") }
    
    var scheduleToDelete by remember { mutableStateOf<Schedule?>(null) }
    var taskEditToDelete by remember { mutableStateOf<EditFotoTask?>(null) }
    var selectedTaskForIgMockup by remember { mutableStateOf<EditFotoTask?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Filter Task Foto Ulang (type starts with "done" and status != "done")
    val taskFotoUlangList = remember(schedules, searchQuery, selectedTypeFilter) {
        schedules.filter {
            val typeLower = it.type.lowercase().trim()
            val statusLower = it.status.lowercase().trim()
            val isTask = typeLower.startsWith("done") && statusLower != "done"
            val matchesQuery = searchQuery.isBlank() ||
                    it.namaMe.contains(searchQuery, ignoreCase = true) ||
                    it.idListing.contains(searchQuery, ignoreCase = true) ||
                    it.lokasi.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (selectedTypeFilter) {
                "Up Foto" -> typeLower.contains("up foto", ignoreCase = true) || statusLower.contains("up foto", ignoreCase = true)
                "Edit Video" -> typeLower.contains("video", ignoreCase = true) || statusLower.contains("video", ignoreCase = true)
                "Garis Tanah" -> typeLower.contains("garis", ignoreCase = true) || typeLower.contains("tanah", ignoreCase = true) || statusLower.contains("garis", ignoreCase = true) || statusLower.contains("tanah", ignoreCase = true)
                else -> true
            }
            
            isTask && matchesQuery && matchesFilter
        }
    }

    // Filter Task Edit Foto (by search query and only showing non-completed ones across all months)
    val taskEditFotoList = remember(editFotoTasks, searchQuery, selectedTypeFilter) {
        editFotoTasks.filter {
            val isNotDone = !it.done
            val matchesQuery = searchQuery.isBlank() ||
                    it.namaMe.contains(searchQuery, ignoreCase = true) ||
                    it.idListing.contains(searchQuery, ignoreCase = true) ||
                    it.editNotes.contains(searchQuery, ignoreCase = true) ||
                    it.judul.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (selectedTypeFilter) {
                "Up Foto" -> it.editNotes.contains("up foto", ignoreCase = true) || it.judul.contains("up foto", ignoreCase = true)
                "Edit Video" -> it.editNotes.contains("video", ignoreCase = true) || it.judul.contains("video", ignoreCase = true)
                "Garis Tanah" -> it.editNotes.contains("garis", ignoreCase = true) || it.editNotes.contains("tanah", ignoreCase = true) || it.judul.contains("garis", ignoreCase = true) || it.judul.contains("tanah", ignoreCase = true)
                else -> true
            }
            
            isNotDone && matchesQuery && matchesFilter
        }
    }

    var selectedUploadIgDateFilter by remember { mutableStateOf<String?>(null) }

    // Filtered Upload IG List
    val uploadIgList = remember(weeklyMeetingIgListings, searchQuery, selectedUploadIgDateFilter) {
        weeklyMeetingIgListings.map { listing ->
            val isPosted = listing.postingIg.trim().lowercase() in listOf("done", "ya", "yes", "true", "✔", "1")
            com.example.data.EditFotoTask(
                id = listing.no,
                no = listing.no,
                idListing = listing.idListing.trim(),
                namaMe = listing.namaMe.trim(),
                postingIg = isPosted,
                jadwalPosting = listing.jadwalPosting.trim(),
                editNotes = listing.catatan.trim(),
                done = isPosted,
                judul = listing.keterangan.trim(),
                source = "${listing.date}|||${listing.colIndex}"
            )
        }.filter { task ->
            val matchesSearch = if (searchQuery.isBlank()) true else {
                task.idListing.contains(searchQuery, ignoreCase = true) ||
                task.namaMe.contains(searchQuery, ignoreCase = true)
            }
            
            val matchesDate = if (selectedUploadIgDateFilter == null) true else {
                val normalizedTaskDate = try {
                    val sdfIn = SimpleDateFormat("dd/MM/yyyy", Locale.US)
                    val sdfOut = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val d = sdfIn.parse(task.jadwalPosting.trim())
                    sdfOut.format(d!!)
                } catch (e: Exception) {
                    try {
                        val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val sdfOut = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                        val d = sdfIn.parse(task.jadwalPosting.trim())
                        sdfOut.format(d!!)
                    } catch (ex: Exception) {
                        task.jadwalPosting.trim()
                    }
                }
                normalizedTaskDate == selectedUploadIgDateFilter
            }
            
            matchesSearch && matchesDate
        }
    }

    val editFotoNotDoneCount = remember(taskEditFotoList) {
        taskEditFotoList.size
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            MediumTopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isUploadIgOnly) "Upload Instagram" else "Dashboard Task",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (isUploadIgOnly) "Daftar postingan siap upload ke Instagram" else "Kelola tugas foto ulang dan editing foto RWC",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigateToChat?.invoke() }) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge {
                                        Text(unreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Forum,
                                contentDescription = "Chat",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.mediumTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Sync status banner for visual feedback
            AnimatedVisibility(
                visible = syncStatus is com.example.ui.SyncState.Error,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val backgroundColor = when (syncStatus) {
                    is com.example.ui.SyncState.Loading -> MaterialTheme.colorScheme.primaryContainer
                    is com.example.ui.SyncState.Success -> Color(0xFFE8F5E9) // Light green
                    is com.example.ui.SyncState.Error -> Color(0xFFFFEBEE)   // Light red
                    else -> MaterialTheme.colorScheme.surface
                }
                val contentColor = when (syncStatus) {
                    is com.example.ui.SyncState.Loading -> MaterialTheme.colorScheme.onPrimaryContainer
                    is com.example.ui.SyncState.Success -> Color(0xFF2E7D32) // Dark green
                    is com.example.ui.SyncState.Error -> Color(0xFFC62828)   // Dark red
                    else -> MaterialTheme.colorScheme.onSurface
                }
                Surface(
                    color = backgroundColor,
                    contentColor = contentColor,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (syncStatus is com.example.ui.SyncState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = contentColor
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Menghubungkan ke Google Sheets...",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            val msg = when (val s = syncStatus) {
                                is com.example.ui.SyncState.Success -> s.message
                                is com.example.ui.SyncState.Error -> s.message
                                else -> ""
                            }
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                textAlign = TextAlign.Center
                            )
                            // Automatically clear non-loading status after a delay
                            LaunchedEffect(syncStatus) {
                                delay(3000)
                                viewModel.dismissSyncStatus()
                            }
                        }
                    }
                }
            }

            // Search Bar & Filters - visible only in Foto Ulang and Edit Foto tabs
            if (selectedSubTab != 2) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { 
                            Text("Cari task (Nama ME, ID, dll)...") 
                        },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Hapus")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Custom Interactive Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filters = listOf("Semua", "Up Foto", "Edit Video", "Garis Tanah")
                        filters.forEach { filter ->
                            val isSelected = selectedTypeFilter == filter
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    )
                                    .clickable { selectedTypeFilter = filter }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (!isUploadIgOnly) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    // Custom Segmented Control Tab Row (not Scrollable so it splits screen width perfectly)
                    TabRow(
                        selectedTabIndex = selectedSubTab.coerceIn(0, 1),
                        containerColor = Color.Transparent,
                        indicator = { tabPositions ->
                            val currentIdx = selectedSubTab.coerceIn(0, 1)
                            if (currentIdx < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    Modifier.tabIndicatorOffset(tabPositions[currentIdx]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        divider = {}
                    ) {
                        Tab(
                            selected = selectedSubTab == 0,
                            onClick = { selectedSubTab = 0 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Foto Ulang (${taskFotoUlangList.size})",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                    )
                                }
                            }
                        )
                        Tab(
                            selected = selectedSubTab == 1,
                            onClick = { selectedSubTab = 1 },
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Text(
                                        text = "Edit Foto ($editFotoNotDoneCount)",
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp)
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Main Content Lists
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedSubTab) {
                    0 -> {
                        // Task Foto Ulang
                        if (taskFotoUlangList.isEmpty()) {
                            EmptyStateTask(
                                title = "Tidak Ada Task Foto Ulang",
                                subtitle = "Semua jadwal berjalan lancar tanpa request foto ulang."
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(taskFotoUlangList) { item ->
                                    TaskFotoUlangCard(
                                        schedule = item,
                                        listingImagesMap = listingImagesMap,
                                        listingSoldMap = listingSoldMap,
                                        onFetchImage = { id -> viewModel.fetchListingImageIfNeeded(id, item.namaMe) },
                                        onDelete = { scheduleToDelete = item },
                                        onClick = { selectedScheduleForDetail = item }
                                    )
                                }
                            }
                        }
                    }
                    1 -> {
                        // Task Edit Foto
                        if (taskEditFotoList.isEmpty()) {
                            EmptyStateTask(
                                title = "Tidak Ada Task Edit Jadwal",
                                subtitle = "Antrean editing foto kosong. Kerja bagus!"
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(taskEditFotoList) { item ->
                                    TaskEditFotoCard(
                                        task = item,
                                        listingImagesMap = listingImagesMap,
                                        listingSoldMap = listingSoldMap,
                                        onFetchImage = { id -> viewModel.fetchListingImageIfNeeded(id, item.namaMe) },
                                        onToggleDone = { viewModel.toggleEditFotoDone(item) },
                                        onTogglePosting = { viewModel.toggleEditFotoPostingIg(item) },
                                        onDownloadPhotos = { activeTaskForDownload = item },
                                        onDelete = { taskEditToDelete = item },
                                        onClick = {
                                            selectedScheduleForDetail = mapEditFotoToSchedule(item)
                                        }
                                    )
                                }
                            }
                        }
                    }
                    2 -> {
                        // Task Upload IG with 2 sub-pages (Belum Post & Sudah Post)
                        // Month selector for Upload IG
                        val allIgMonths = listOf("Januari","Februari","Maret","April","Mei","Juni",
                            "Juli","Agustus","September","Oktober","November","Desember")
                        var igMonthExpanded by remember { mutableStateOf(false) }

                        // Belum Post: has jadwalPosting AND postingIg=false
                        val unpostedList = remember(uploadIgList) {
                            val todayMillis = System.currentTimeMillis()
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                            uploadIgList.filter { task ->
                                val jadwal = task.jadwalPosting.trim()
                                val hasJadwal = jadwal.isNotEmpty() && jadwal != "-" && !jadwal.lowercase().contains("belum")
                                hasJadwal && !task.postingIg
                            }.sortedWith(
                                    compareBy<com.example.data.EditFotoTask> { item ->
                                        val norm = com.example.data.normalizeDate(item.jadwalPosting)
                                        val time = try {
                                            sdf.parse(norm)?.time ?: Long.MAX_VALUE
                                        } catch (e: Exception) {
                                            Long.MAX_VALUE
                                        }
                                        if (time == Long.MAX_VALUE) Long.MAX_VALUE
                                        else Math.abs(time - todayMillis)
                                    }.thenByDescending { it.no }
                                )
                        }
                        // Sudah Post: has jadwalPosting AND postingIg=true
                        val postedList = remember(uploadIgList) {
                            uploadIgList.filter { task ->
                                val jadwal = task.jadwalPosting.trim()
                                val hasJadwal = jadwal.isNotEmpty() && jadwal != "-" && !jadwal.lowercase().contains("belum")
                                hasJadwal && task.postingIg
                            }
                        }
                        
                        val pagerState = rememberPagerState(pageCount = { 2 })
                        val scope = rememberCoroutineScope()
                        
                        // Collapsing header states
                        var headerHeight by remember { mutableStateOf(0) }
                        var headerOffset by remember { mutableStateOf(0f) }
                        
                        val nestedScrollConnection = remember(headerHeight) {
                            object : NestedScrollConnection {
                                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                                    val delta = available.y
                                    val newOffset = headerOffset + delta
                                    headerOffset = newOffset.coerceIn(-headerHeight.toFloat(), 0f)
                                    return if (delta < 0 && headerOffset > -headerHeight) {
                                        Offset(0f, delta)
                                    } else {
                                        Offset.Zero
                                    }
                                }
                                
                                override fun onPostScroll(
                                    consumed: Offset,
                                    available: Offset,
                                    source: NestedScrollSource
                                ): Offset {
                                    val delta = available.y
                                    val newOffset = headerOffset + delta
                                    headerOffset = newOffset.coerceIn(-headerHeight.toFloat(), 0f)
                                    return Offset(0f, delta)
                                }
                            }
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(nestedScrollConnection)
                        ) {
                            // Header: Search ID, Date Filter, Month Selector
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onGloballyPositioned { layoutCoordinates ->
                                        headerHeight = layoutCoordinates.size.height
                                    }
                                    .offset { IntOffset(0, headerOffset.roundToInt()) }
                                    .background(MaterialTheme.colorScheme.background)
                                    .padding(vertical = 4.dp)
                            ) {
                                // Unified Premium Filter Card
                                val context = LocalContext.current
                                val calendar = Calendar.getInstance()
                                val dateCalendar = Calendar.getInstance().apply {
                                    selectedUploadIgDateFilter?.let {
                                        try {
                                            time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)!!
                                        } catch (e: Exception) {}
                                    }
                                }
                                val datePickerDialog = DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
                                        selectedUploadIgDateFilter = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
                                    },
                                    dateCalendar.get(Calendar.YEAR),
                                    dateCalendar.get(Calendar.MONTH),
                                    dateCalendar.get(Calendar.DAY_OF_MONTH)
                                )

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. Search Bar
                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = { Text("Cari listing (ID, ME)...", style = MaterialTheme.typography.bodyMedium) },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                                            trailingIcon = {
                                                if (searchQuery.isNotEmpty()) {
                                                    IconButton(onClick = { searchQuery = "" }) {
                                                        Icon(Icons.Default.Close, contentDescription = "Hapus")
                                                    }
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                containerColor = MaterialTheme.colorScheme.surface
                                            )
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // 2. Month Selector
                                            ExposedDropdownMenuBox(
                                                expanded = igMonthExpanded,
                                                onExpandedChange = { igMonthExpanded = it },
                                                modifier = Modifier.weight(1.3f)
                                            ) {
                                                OutlinedTextField(
                                                    value = selectedUploadIgMonth,
                                                    onValueChange = {},
                                                    readOnly = true,
                                                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp)) },
                                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = igMonthExpanded) },
                                                    singleLine = true,
                                                    shape = RoundedCornerShape(10.dp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                                        containerColor = MaterialTheme.colorScheme.surface
                                                    ),
                                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = igMonthExpanded,
                                                    onDismissRequest = { igMonthExpanded = false }
                                                ) {
                                                    allIgMonths.forEach { m ->
                                                        DropdownMenuItem(
                                                            text = { Text(m, style = MaterialTheme.typography.bodyMedium) },
                                                            onClick = {
                                                                selectedUploadIgMonth = m
                                                                igMonthExpanded = false
                                                            }
                                                        )
                                                    }
                                                }
                                            }

                                            // 3. Date Filter Chip
                                            val dateText = selectedUploadIgDateFilter?.let {
                                                try {
                                                    val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(it)
                                                    SimpleDateFormat("d MMM", Locale("id", "ID")).format(d!!)
                                                } catch (e: Exception) {
                                                    it
                                                }
                                            } ?: "Pilih Tanggal"

                                            val isDateFiltered = selectedUploadIgDateFilter != null

                                            Button(
                                                onClick = { datePickerDialog.show() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isDateFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                    contentColor = if (isDateFiltered) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                                ),
                                                border = BorderStroke(1.dp, if (isDateFiltered) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.weight(1.1f).height(48.dp),
                                                contentPadding = PaddingValues(horizontal = 6.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Icon(
                                                        imageVector = if (isDateFiltered) Icons.Default.EventAvailable else Icons.Default.CalendarToday,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = dateText,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis,
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                                    )
                                                    if (isDateFiltered) {
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Clear",
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .clickable { selectedUploadIgDateFilter = null }
                                                        )
                                                    }
                                                }
                                            }

                                            // 4. Refresh Button
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)), RoundedCornerShape(10.dp))
                                                    .clickable { viewModel.fetchWeeklyMeetingIgListings(selectedUploadIgMonth) },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (igSyncStatus is SyncState.Loading) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Default.Refresh,
                                                        contentDescription = "Refresh",
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            
                            // Body content: TabRow + HorizontalPager
                            val density = LocalDensity.current
                            val headerHeightDp = remember(headerHeight) {
                                with(density) { headerHeight.toDp() }
                            }
                            val headerOffsetDp = remember(headerOffset) {
                                with(density) { headerOffset.toDp() }
                            }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = headerHeightDp + headerOffsetDp)
                            ) {
                                // Sub-tabs at the top
                                TabRow(
                                    selectedTabIndex = pagerState.currentPage,
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    contentColor = MaterialTheme.colorScheme.primary,
                                    indicator = { tabPositions ->
                                        if (pagerState.currentPage < tabPositions.size) {
                                            TabRowDefaults.SecondaryIndicator(
                                                Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                ) {
                                    Tab(
                                        selected = pagerState.currentPage == 0,
                                        onClick = {
                                            scope.launch {
                                                pagerState.animateScrollToPage(0)
                                            }
                                        },
                                        text = {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CloudUpload,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (pagerState.currentPage == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                )
                                                Text(
                                                    "Belum Post (${unpostedList.size})",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
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
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp),
                                                    tint = if (pagerState.currentPage == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                                )
                                                Text(
                                                    "Sudah Post (${postedList.size})",
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    )
                                }
                                
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) { page ->
                                    val listToUse = if (page == 0) unpostedList else postedList
                                    val emptyTitle = if (page == 0) "Tidak Ada Task Belum Post" else "Tidak Ada Task Sudah Post"
                                    val emptySub = if (page == 0) "Semua tugas editing selesai telah diposting di Instagram." else "Belum ada postingan yang ditandai sudah diposting."
                                    
                                    if (listToUse.isEmpty()) {
                                        EmptyStateTask(
                                            title = emptyTitle,
                                            subtitle = emptySub
                                        )
                                    } else {
                                        LazyColumn(
                                            contentPadding = PaddingValues(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(12.dp),
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            items(listToUse) { item ->
                                                val context = LocalContext.current
                                                TaskUploadIgCard(
                                                    task = item,
                                                    listingImagesMap = listingImagesMap,
                                                    listingSoldMap = listingSoldMap,
                                                    listingTitleMap = listingTitleMap,
                                                    listingDescMap = listingDescMap,
                                                    listingPriceMap = listingPriceMap,
                                                    schedules = schedules,
                                                    onFetchImage = { id -> viewModel.fetchListingImageIfNeeded(id, item.namaMe) },
                                                    onTogglePosting = {
                                                        val parts = item.source.split("|||")
                                                        val date = parts.getOrNull(0) ?: ""
                                                        val colIndex = parts.getOrNull(1)?.toIntOrNull() ?: 0
                                                        viewModel.updateWeeklyMeetingIgPost(
                                                            photoMonth = selectedMonth,
                                                            dateStr = date,
                                                            row = item.no,
                                                            colIndex = colIndex,
                                                            postingIg = !item.postingIg,
                                                            onResult = { success, msg ->
                                                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    },
                                                    onDelete = {},
                                                    onClick = {
                                                        selectedTaskForIgMockup = item
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
            }
        }

        // Delete Confirmation Dialog for Schedule
        scheduleToDelete?.let { schedule ->
            AlertDialog(
                onDismissRequest = { scheduleToDelete = null },
                title = { Text("Hapus Jadwal", fontWeight = FontWeight.Bold) },
                text = { Text("Apakah Anda yakin ingin menghapus jadwal '${schedule.idListing.ifBlank { schedule.namaMe }}' dari HP dan Google Sheets secara online?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteSchedule(schedule)
                            scheduleToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { scheduleToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Delete Confirmation Dialog for EditFotoTask
        taskEditToDelete?.let { task ->
            AlertDialog(
                onDismissRequest = { taskEditToDelete = null },
                title = { Text("Hapus Jadwal Edit", fontWeight = FontWeight.Bold) },
                text = { Text("Apakah Anda yakin ingin menghapus jadwal edit '${task.idListing.ifBlank { task.namaMe }}' dari HP dan Google Sheets secara online?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteEditFotoTask(task)
                            taskEditToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hapus")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { taskEditToDelete = null }) {
                        Text("Batal")
                    }
                }
            )
        }

        // Detail Pager Screen Overlay Integration
        selectedScheduleForDetail?.let { schedule ->
            DetailPagerScreen(
                schedule = schedule,
                listingImagesMap = listingImagesMap,
                listingImagesGalleryMap = listingImagesGalleryMap,
                agentInfoMap = agentInfoMap,
                viewModel = viewModel,
                onDismiss = { selectedScheduleForDetail = null },
                onNavigateToChat = onNavigateToChat
            )
        }

        // Instagram Post Mockup Screen Overlay Integration
        selectedTaskForIgMockup?.let { task ->
            InstagramPostMockupScreen(
                task = task,
                listingImagesMap = listingImagesMap,
                listingImagesGalleryMap = listingImagesGalleryMap,
                listingDescMap = viewModel.listingDescMap.collectAsState().value,
                listingPriceMap = viewModel.listingPriceMap.collectAsState().value,
                listingTitleMap = viewModel.listingTitleMap.collectAsState().value,
                onDismiss = { selectedTaskForIgMockup = null },
                onViewDetails = {
                    selectedScheduleForDetail = mapEditFotoToSchedule(task)
                    selectedTaskForIgMockup = null
                }
            )
        }

        // Download Images Screen Overlay Integration
        activeTaskForDownload?.let { task ->
            DownloadImagesScreen(
                task = task,
                listingImagesGalleryMap = listingImagesGalleryMap,
                onDismiss = { activeTaskForDownload = null }
            )
        }
    }
}

@Composable
private fun SoldWatermark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .background(Color.Red, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .rotate(-20f)
        ) {
            Text(
                text = "SOLD",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun TaskFotoUlangCard(
    schedule: Schedule,
    listingImagesMap: Map<String, String>,
    listingSoldMap: Map<String, Boolean> = emptyMap(),
    onFetchImage: (String) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val cleanId = schedule.idListing.trim()
    LaunchedEffect(cleanId) {
        if (cleanId.isNotBlank()) {
            onFetchImage(cleanId)
        }
    }

    val indicatorColor = MaterialTheme.colorScheme.error

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = indicatorColor.copy(alpha = 0.05f)
        ),
        border = BorderStroke(1.dp, indicatorColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Distinctive visual color indicator bar
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(indicatorColor)
                )

                // Large property image display on the left
                if (cleanId.isNotBlank()) {
                    val imageUrl = listingImagesMap[cleanId]
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = "Foto Listing $cleanId",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = indicatorColor.copy(alpha = 0.5f)
                            )
                        }
                        
                        if (listingSoldMap[cleanId] == true) {
                            SoldWatermark()
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .width(115.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "No Image",
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Text Content on the Right side
                Column(
                    modifier = Modifier
                        .weight(1.0f)
                        .padding(start = 16.dp, top = 12.dp, end = 12.dp, bottom = 12.dp)
                ) {
                    // Listing ID label & Sync Tag row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (schedule.idListing.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (schedule.no > 0) {
                                        Text(
                                            text = "#${schedule.no}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        text = schedule.idListing,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (schedule.no > 0) {
                                        Text(
                                            text = "#${schedule.no}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                    }
                                    Text(
                                        text = "Manual Input",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                        }

                        // Sync indicators & Delete Button row
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val isSynced = schedule.synced
                            Box(
                                modifier = Modifier
                                    .background(
                                        if (isSynced) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        else MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                                        RoundedCornerShape(100.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Icon(
                                        imageVector = if (isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                        contentDescription = null,
                                        tint = if (isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = if (isSynced) "Synced" else "Pending",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                        color = if (isSynced) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Jadwal",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Location details
                    val displayLocation = schedule.lokasi.ifBlank { "Lokasi tidak tersedia" }
                    Text(
                        text = displayLocation,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // ME and Staff details
                    val displayName = schedule.namaMe.ifBlank { "ME tidak diketahui" }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "ME Name Icon",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "ME: $displayName",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Staff Icon",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Staff: ${schedule.staff}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Date & Time row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                            Text(formatIndonesianDate(schedule.tanggal), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Default.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(14.dp))
                            Text(formatTwelveHourTime(schedule.jam), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Type Badge & Status Badge row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.errorContainer, 
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Tipe: ${schedule.type}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant, 
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "Status: ${schedule.status}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskEditFotoCard(
    task: EditFotoTask,
    listingImagesMap: Map<String, String>,
    listingSoldMap: Map<String, Boolean> = emptyMap(),
    onFetchImage: (String) -> Unit,
    onToggleDone: () -> Unit,
    onTogglePosting: () -> Unit,
    onDownloadPhotos: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val cleanId = task.idListing.trim()
    LaunchedEffect(cleanId) {
        if (cleanId.isNotBlank()) {
            onFetchImage(cleanId)
        }
    }

    val accentColor = if (task.done) Color(0xFF4CAF50) else Color(0xFFFF9800) // Green for done, Orange/Amber for pending

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (task.done) MaterialTheme.colorScheme.surface else Color(0xFFFF9800).copy(alpha = 0.04f)
        ),
        border = BorderStroke(
            width = if (task.done) 1.dp else 1.5.dp,
            color = if (task.done) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f) else Color(0xFFFF9800).copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .fillMaxHeight()
                        .background(accentColor)
                )

                // Image Thumbnail
                Box(
                    modifier = Modifier
                        .width(100.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (cleanId.isNotBlank()) {
                        val imageUrl = listingImagesMap[cleanId]
                        if (imageUrl != null) {
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        }
                        
                        if (listingSoldMap[cleanId] == true) {
                            SoldWatermark()
                        }
                    } else {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                    }
                }

                // Text Info
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ID: ${task.idListing}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "#${task.no}",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.outline
                            )

                            if (!task.synced) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Pending Sync",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(accentColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (task.done) "DONE EDIT" else "PENDING EDIT",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = accentColor
                                )
                            }

                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Hapus Jadwal Edit",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = task.namaMe,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (task.judul.isNotBlank()) {
                        Text(
                            text = "Judul: ${task.judul}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (task.editNotes.isNotBlank()) {
                        Text(
                            text = "Notes: ${task.editNotes}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "Upload IG #${task.no} : ${formatJadwalPostingDate(task.jadwalPosting)}",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // Checkbox buttons row at the bottom
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Button 1: Posting IG Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onTogglePosting() }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = task.postingIg,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "Posting IG",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.postingIg) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Button 3: Download Foto Listing (di tengah)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onDownloadPhotos() }
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Foto",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Download",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Button 2: Done Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onToggleDone() }
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = task.done,
                        onCheckedChange = null,
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50)) // Clean Green for completed edit
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        "Done Edit",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.done) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TaskUploadIgCard(
    task: EditFotoTask,
    listingImagesMap: Map<String, String>,
    listingSoldMap: Map<String, Boolean> = emptyMap(),
    listingTitleMap: Map<String, String> = emptyMap(),
    listingDescMap: Map<String, String> = emptyMap(),
    listingPriceMap: Map<String, String> = emptyMap(),
    schedules: List<Schedule> = emptyList(),
    onFetchImage: (String) -> Unit,
    onTogglePosting: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val cleanId = task.idListing.trim()
    LaunchedEffect(cleanId) {
        if (cleanId.isNotBlank()) {
            onFetchImage(cleanId)
        }
    }

    val displayLocation = remember(task.judul, task.idListing, schedules, listingTitleMap, listingDescMap) {
        getDisplayLocation(task, schedules, listingTitleMap, listingDescMap)
    }

    val desc = remember(cleanId, listingDescMap) {
        if (cleanId.isNotBlank()) listingDescMap[cleanId] ?: "" else ""
    }

    // Parse specs for card layout
    val specs = remember(desc) {
        if (desc.isBlank()) null else {
            val descLower = desc.lowercase()
            val lt = Regex("(?i)\\b(?:lt)[:\\s\\.]*(\\d+)").find(descLower)?.groupValues?.get(1)
            val lb = Regex("(?i)\\b(?:lb)[:\\s\\.]*(\\d+)").find(descLower)?.groupValues?.get(1)
            val kt = Regex("(?i)\\b(?:kt|kamar\\s*tidur)[:\\s\\.]*(\\d+(?:\\s*\\+\\s*\\d+)?)").find(descLower)?.groupValues?.get(1)
            val km = Regex("(?i)\\b(?:km|kamar\\s*mandi)[:\\s\\.]*(\\d+(?:\\s*\\+\\s*\\d+)?)").find(descLower)?.groupValues?.get(1)
            if (lt != null || lb != null) {
                mapOf("lt" to (lt ?: "-"), "lb" to (lb ?: "-"), "kt" to (kt ?: "-"), "km" to (km ?: "-"))
            } else null
        }
    }

    // Parse price for card layout
    val priceVal = remember(cleanId, listingPriceMap, desc) {
        val rawPrice = if (cleanId.isNotBlank()) listingPriceMap[cleanId] ?: "" else ""
        if (rawPrice.isNotBlank() && !rawPrice.contains("Hubungi", ignoreCase = true)) {
            rawPrice
        } else {
            val priceRegex = Regex("(?i)(?:harga|rp|idr)[:\\s-]*([\\d\\.,]+(?:\\s*(?:milyar|miliar|juta|m|jt|b|t))?)")
            val match = priceRegex.find(desc.lowercase())
            if (match != null) {
                val pVal = match.groupValues[1].uppercase().trim()
                if (pVal.endsWith("M") || pVal.contains("MILYAR") || pVal.contains("MILIAR")) {
                    "Rp. ${pVal.replace("MILYAR", "M").replace("MILIAR", "M").trim()}"
                } else if (pVal.endsWith("JT") || pVal.contains("JUTA")) {
                    "Rp. ${pVal.replace("JUTA", "Jt").trim()}"
                } else {
                    "Rp. $pVal"
                }
            } else null
        }
    }

    val statusColor = if (task.postingIg) Color(0xFF4CAF50) else Color(0xFFE1306C)
    
    val cardBg = MaterialTheme.colorScheme.surface
    val cardBorderColor = if (task.postingIg) {
        Color(0xFF4CAF50).copy(alpha = 0.25f)
    } else {
        Color(0xFFE1306C).copy(alpha = 0.25f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorderColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Accent Status Strip on left
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(6.dp)
                    .background(statusColor)
            )

            // Property Image
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp, horizontal = 8.dp)
                    .size(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (cleanId.isNotBlank()) {
                    val imageUrl = listingImagesMap[cleanId]
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Foto Listing $cleanId",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = statusColor
                        )
                    }
                    if (listingSoldMap[cleanId] == true) {
                        SoldWatermark()
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "No Image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Info Column
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp, top = 10.dp, bottom = 10.dp)
            ) {
                // Header tags & Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (task.no > 0) {
                            Text(
                                text = "#${task.no}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                modifier = Modifier
                                    .background(statusColor, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = if (task.idListing.isNotBlank()) "ID: ${task.idListing}" else "Manual Input",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!task.source.contains("|||")) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Jadwal Upload",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Location Title
                Text(
                    text = displayLocation,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Price Tag if available
                if (!priceVal.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = priceVal,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFD4AF37)), // Gold-ish
                        fontSize = 11.sp
                    )
                }

                // Property Specs row if parsed
                if (specs != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val specItems = listOf(
                            Icons.Default.AspectRatio to "LT ${specs["lt"]}",
                            Icons.Default.Home to "LB ${specs["lb"]}",
                            Icons.Default.Bed to specs["kt"],
                            Icons.Default.Bathtub to specs["km"]
                        )
                        specItems.forEach { (icon, text) ->
                            if (text != "-" && text != null) {
                                Row(
                                    modifier = Modifier
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(9.dp))
                                    Text(text, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // ME & Jadwal
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(11.dp))
                        Text(
                            text = "ME: ${task.namaMe.ifBlank { "Tidak ada" }}",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(11.dp))
                        Text(
                            text = formatJadwalPostingDate(task.jadwalPosting),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Notes inside card if present
                if (task.editNotes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(10.dp))
                        Text(
                            text = task.editNotes,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp, lineHeight = 11.sp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Beautiful interactive Status Toggle Button
                Surface(
                    color = statusColor.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.3f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTogglePosting() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (task.postingIg) Icons.Default.CheckCircle else Icons.Default.CloudUpload,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (task.postingIg) "Sudah Diposting ke IG" else "Tandai Sudah Diposting",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = statusColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateTask(
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.TaskAlt,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// Maps EditFotoTask to Schedule for DetailPagerScreen integration
private fun mapEditFotoToSchedule(task: EditFotoTask): Schedule {
    return Schedule(
        id = task.id,
        no = task.no,
        idListing = task.idListing,
        namaMe = task.namaMe,
        lokasi = "${if (task.postingIg) "[Posting IG: Ya] " else "[Posting IG: Tidak] "}${task.editNotes}",
        tanggal = task.jadwalPosting,
        jam = task.judul, // store judul here
        staff = task.source, // store source here
        type = "Edit Foto",
        status = if (task.done) "Done" else "Pending",
        synced = task.synced
    )
}

private fun formatJadwalPostingDate(rawDate: String): String {
    val trimmed = rawDate.trim()
    if (trimmed.isEmpty()) return ""

    // 1. Try parsing ISO/UTC standard format first, e.g., 2026-06-26T10:00:00.000Z
    try {
        if (trimmed.contains("T")) {
            val datePart = trimmed.substringBefore("T")
            val parser = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val output = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("id", "ID"))
            parser.parse(datePart)?.let { return output.format(it) }
        }
    } catch (e: Exception) {}

    // 2. Try parsing typical JavaScript Date string: e.g., "Fri Jun 26 2026 10:00:00 GMT+0700 (WIB)" or similar
    try {
        val tokens = trimmed.split(" ").filter { it.isNotBlank() }
        if (tokens.size >= 4) {
            val firstFour = tokens.take(4).joinToString(" ")
            val parser = java.text.SimpleDateFormat("EEE MMM dd yyyy", java.util.Locale.US)
            val output = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("id", "ID"))
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
            val parser = java.text.SimpleDateFormat(fmt, java.util.Locale("id", "ID"))
            parser.parse(trimmed)?.let {
                val output = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("id", "ID"))
                return output.format(it)
            }
        } catch (e: Exception) {}
        try {
            val parser = java.text.SimpleDateFormat(fmt, java.util.Locale.US)
            parser.parse(trimmed)?.let {
                val output = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("id", "ID"))
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
            val parser = java.text.SimpleDateFormat("EEE MMM dd yyyy", java.util.Locale.US)
            val output = java.text.SimpleDateFormat("EEEE, d MMMM yyyy", java.util.Locale("id", "ID"))
            parser.parse(firstFour)?.let { return output.format(it) }
        }
    } catch (e: Exception) {}

    return clean
}

private fun getDisplayLocation(
    task: EditFotoTask,
    schedules: List<Schedule>,
    listingTitleMap: Map<String, String>,
    listingDescMap: Map<String, String>
): String {
    val cleanId = task.idListing.trim()
    val rawLoc = if (cleanId.isNotBlank()) {
        val matchingSchedule = schedules.find { it.idListing.trim().equals(cleanId, ignoreCase = true) }
        if (matchingSchedule != null && matchingSchedule.lokasi.isNotBlank()) {
            matchingSchedule.lokasi
        } else {
            val parsed = getScrapedOrFallbackLocation(cleanId, task.judul, listingTitleMap, listingDescMap)
            if (parsed.isNotBlank()) parsed else task.judul
        }
    } else {
        task.judul
    }
    
    return toSentenceCase(rawLoc)
}

private fun getScrapedOrFallbackLocation(
    cleanId: String,
    taskJudul: String,
    listingTitleMap: Map<String, String>,
    listingDescMap: Map<String, String>
): String {
    val desc = (listingDescMap[cleanId] ?: "").replace("<[^>]*>".toRegex(), "")
    val descLower = desc.lowercase()
    val title = taskJudul
    val scrapedTitle = (listingTitleMap[cleanId] ?: "")
    
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
        if (descLower.contains(loc) || title.lowercase().contains(loc) || scrapedTitle.lowercase().contains(loc)) {
            return loc
        }
    }
    
    if (title.isNotBlank() && title != "(Tidak ada judul listing)") {
        return title
    }
    
    return ""
}

private fun toSentenceCase(input: String): String {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return ""
    return trimmed.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale("id", "ID")) else it.toString() }
}


private fun getDisplayTitle(
    taskJudul: String,
    idListing: String,
    listingTitleMap: Map<String, String>,
    listingDescMap: Map<String, String>
): String {
    val cleanId = idListing.trim()
    
    // 1. Check direct task.judul
    val t = taskJudul.replace("<[^>]*>".toRegex(), "").trim()
    if (t.isNotBlank() && t != "(Tidak ada judul listing)" && !isLineJustNumbersOrStats(t)) {
        return t
    }
    
    // 2. Check scraped title map
    val scraped = (listingTitleMap[cleanId] ?: "").replace("<[^>]*>".toRegex(), "").trim()
    if (scraped.isNotBlank() && !isLineJustNumbersOrStats(scraped)) {
        return scraped
    }
    
    // 3. Fallback to description first clean non-metadata line
    val desc = (listingDescMap[cleanId] ?: "").replace("<[^>]*>".toRegex(), "").trim()
    if (desc.isNotBlank()) {
        val lines = desc.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            val lower = line.lowercase()
            if (line.isNotEmpty() &&
                !lower.startsWith("dengan spek") &&
                !lower.startsWith("listing id") &&
                !lower.startsWith("luas tanah") &&
                !lower.startsWith("luas bangunan") &&
                !lower.contains("deskripsi lengkap") &&
                !lower.contains("for sale") &&
                !lower.contains("hubungi") &&
                !lower.contains("contact") &&
                !isLineJustNumbersOrStats(line)
            ) {
                return line
            }
        }
    }
    
    // 4. Ultimate fallback
    return "Rumah Cantik di Lokasi Strategis"
}

private fun isLineJustNumbersOrStats(line: String): Boolean {
    val trimmed = line.trim()
    val cleaned = trimmed.replace("[•\\-\\+\\*\\s\\t/\\|\\.,]".toRegex(), "")
    if (cleaned.all { it.isDigit() } && cleaned.isNotEmpty()) {
        return true
    }
    val digitsCount = trimmed.count { it.isDigit() }
    val lettersCount = trimmed.count { it.isLetter() }
    if (lettersCount == 0 && digitsCount > 0) {
        return true
    }
    if (trimmed.matches("^\\s*\\d+\\s+\\d+\\s+\\d+\\s*$".toRegex()) ||
        trimmed.matches("^\\s*\\d+([\\s/\\|\\+\\-]+)\\d+([\\s/\\|\\+\\-]+)\\d+\\s*$".toRegex())) {
        return true
    }
    return false
}

