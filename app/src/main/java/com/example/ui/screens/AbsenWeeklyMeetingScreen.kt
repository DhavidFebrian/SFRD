package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.ScheduleViewModel
import com.example.ui.SyncState
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AbsenWeeklyMeetingScreen(
    viewModel: ScheduleViewModel,
    onOpenDrawer: () -> Unit,
    showTopBar: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val absensiData by viewModel.absensiData.collectAsStateWithLifecycle()
    val syncStatus by viewModel.absensiSyncStatus.collectAsStateWithLifecycle()

    val monthsList = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    // Default to current month index (0-based)
    val calendar = Calendar.getInstance()
    var selectedMonthIndex by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
    var selectedDateIdx by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    // Fetch data when selected month changes
    LaunchedEffect(selectedMonthIndex) {
        viewModel.fetchAbsensiMeeting(selectedMonthIndex)
    }

    // Auto-select relevant date index based on today's date when absensiData is loaded for a month
    var lastAutoSelectedMonthIndex by remember { mutableStateOf<Int?>(null) }
    LaunchedEffect(absensiData) {
        val currentDates = absensiData?.dates
        if (currentDates != null && currentDates.isNotEmpty() && lastAutoSelectedMonthIndex != selectedMonthIndex) {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            val todayStr = sdf.format(java.util.Date())

            val monthNum = selectedMonthIndex + 1
            val monthStr = monthNum.toString().padStart(2, '0')
            val dateStrList = currentDates.map { dateObj ->
                val label = dateObj.label
                // Extract first number from label (supports "14/07/2026", "Selasa, 14 Juli 2026", etc.)
                val dayNum = Regex("""\b(\d{1,2})\b""").find(label)?.groupValues?.get(1)?.toIntOrNull()
                val dayFormatted = (dayNum ?: 1).toString().padStart(2, '0')
                // Extract year from label (4-digit number), fallback to Calendar year
                val yearNum = Regex("""\b(20\d{2})\b""").find(label)?.groupValues?.get(1)
                    ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR).toString()
                "$yearNum-$monthStr-$dayFormatted"
            }

            selectedDateIdx = viewModel.findRelevantDateIndex(dateStrList, todayStr)
            lastAutoSelectedMonthIndex = selectedMonthIndex
        }
    }


    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (showTopBar) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Absen Weekly Meeting",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Database Absensi Meeting 2026",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.testTag("drawer_menu_button")
                        ) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Buka Menu")
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.fetchAbsensiMeeting(selectedMonthIndex) },
                            modifier = Modifier.testTag("refresh_absensi_button")
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh Data")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            val dates = absensiData?.dates ?: emptyList()
            val rawList = absensiData?.marketingList ?: emptyList()
            val marketingList = remember(rawList, searchQuery) {
                if (searchQuery.isBlank()) rawList else {
                    rawList.filter { it.name.contains(searchQuery, ignoreCase = true) }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("marketing_absensi_list"),
                contentPadding = PaddingValues(bottom = 8.dp)
            ) {
                // 1. Month Selector, Sync Status, and Search Bar (Scrollable Header Items)
                item {
                    var showMonthDropdown by remember { mutableStateOf(false) }
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMonthDropdown = true }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = "Bulan Absensi",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "${monthsList[selectedMonthIndex]} 2026",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Pilih Bulan"
                            )

                            DropdownMenu(
                                expanded = showMonthDropdown,
                                onDismissRequest = { showMonthDropdown = false }
                            ) {
                                monthsList.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = { Text(name, fontWeight = if (selectedMonthIndex == index) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            selectedMonthIndex = index
                                            selectedDateIdx = 0 // Reset date selection
                                            showMonthDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Sync Status Indicator
                    AnimatedVisibility(
                        visible = syncStatus is SyncState.Loading || syncStatus is SyncState.Error || syncStatus is SyncState.Success,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            color = when (syncStatus) {
                                is SyncState.Loading -> MaterialTheme.colorScheme.primaryContainer
                                is SyncState.Error -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.tertiaryContainer
                            },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (syncStatus is SyncState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Sedang menyelaraskan dengan Google Sheets...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                } else if (syncStatus is SyncState.Error) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = (syncStatus as SyncState.Error).message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                } else if (syncStatus is SyncState.Success) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircleOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = (syncStatus as SyncState.Success).message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Compact Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .testTag("search_marketing_input"),
                        placeholder = { Text("Cari Marketing...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )
                }

                // 2. Sticky Header: Date Tabs stay pinned!
                if (dates.isNotEmpty()) {
                    stickyHeader {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 2.dp
                        ) {
                            ScrollableTabRow(
                                selectedTabIndex = selectedDateIdx,
                                edgePadding = 16.dp,
                                containerColor = Color.Transparent,
                                divider = {},
                                indicator = { tabPositions ->
                                    if (selectedDateIdx < tabPositions.size) {
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedDateIdx]),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                dates.forEachIndexed { idx, date ->
                                    Tab(
                                        selected = selectedDateIdx == idx,
                                        onClick = { selectedDateIdx = idx },
                                        text = {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = date.label,
                                                    fontWeight = if (selectedDateIdx == idx) FontWeight.Bold else FontWeight.Normal,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                val totalPresent = absensiData?.dateTotals?.getOrNull(idx) ?: 0
                                                Text(
                                                    text = "$totalPresent Hadir",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selectedDateIdx == idx) {
                                                        MaterialTheme.colorScheme.primary
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Marketing Attendance Cards (2-Column Grid using chunked Rows)
                if (marketingList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (absensiData == null && syncStatus is SyncState.Loading) {
                                CircularProgressIndicator()
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonOff,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = if (searchQuery.isNotBlank()) "Tidak ada hasil pencarian." else "Belum ada data absensi untuk bulan ini.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    val chunkedList = marketingList.chunked(2)
                    items(chunkedList, key = { it[0].row }) { rowItems ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 5.dp)
                        ) {
                            // Column 1
                            val m1 = rowItems[0]
                            val isChecked1 = m1.attendance.getOrNull(selectedDateIdx) ?: false
                            val dateColIndex = dates.getOrNull(selectedDateIdx)?.colIndex ?: 4
                            val cachedAvatar1 = viewModel.getAgentAvatarByName(m1.name)
                            val avatarUrl1 = if (!cachedAvatar1.isNullOrBlank()) {
                                cachedAvatar1
                            } else {
                                val cleanName = m1.name.replace(" ", "+")
                                "https://ui-avatars.com/api/?name=$cleanName&background=random&color=fff&size=128&bold=true"
                            }

                            Box(modifier = Modifier.weight(1f)) {
                                MarketingAttendanceCard(
                                    name = m1.name,
                                    row = m1.row,
                                    totalHadirBulan = m1.totalHadirBulan,
                                    isChecked = isChecked1,
                                    avatarUrl = avatarUrl1,
                                    onCheckedChange = { checked ->
                                        viewModel.updateAbsensiMeeting(
                                            row = m1.row,
                                            col = dateColIndex,
                                            present = checked,
                                            onResult = { success, msg, _, _ ->
                                                if (!success) {
                                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        )
                                    }
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Column 2
                            if (rowItems.size > 1) {
                                val m2 = rowItems[1]
                                val isChecked2 = m2.attendance.getOrNull(selectedDateIdx) ?: false
                                val cachedAvatar2 = viewModel.getAgentAvatarByName(m2.name)
                                val avatarUrl2 = if (!cachedAvatar2.isNullOrBlank()) {
                                    cachedAvatar2
                                } else {
                                    val cleanName = m2.name.replace(" ", "+")
                                    "https://ui-avatars.com/api/?name=$cleanName&background=random&color=fff&size=128&bold=true"
                                }

                                Box(modifier = Modifier.weight(1f)) {
                                    MarketingAttendanceCard(
                                        name = m2.name,
                                        row = m2.row,
                                        totalHadirBulan = m2.totalHadirBulan,
                                        isChecked = isChecked2,
                                        avatarUrl = avatarUrl2,
                                        onCheckedChange = { checked ->
                                            viewModel.updateAbsensiMeeting(
                                                row = m2.row,
                                                col = dateColIndex,
                                                present = checked,
                                                onResult = { success, msg, _, _ ->
                                                    if (!success) {
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            )
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

            // Summary Footer Card (Placed outside at the bottom)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        val activeDateLabel = dates.getOrNull(selectedDateIdx)?.label ?: "Meeting"
                        Text(
                            text = "Ringkasan Kehadiran",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$activeDateLabel (${monthsList[selectedMonthIndex]})",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    val activeTotalPresent = absensiData?.dateTotals?.getOrNull(selectedDateIdx) ?: 0
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "$activeTotalPresent Hadir",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MarketingAttendanceCard(
    name: String,
    row: Int,
    totalHadirBulan: Int,
    isChecked: Boolean,
    avatarUrl: String,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("marketing_row_$row"),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(
            containerColor = if (isChecked) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Avatar and Checkbox
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = "Foto $name",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )

                Checkbox(
                    checked = isChecked,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("checkbox_marketing_$row")
                )
            }

            // Body: Marketing Name and Stats
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Total hadir: $totalHadirBulan",
                    style = MaterialTheme.typography.bodySmall,
                    color = androidx.compose.ui.graphics.Color.White
                )
            }
        }
    }
}

private fun Modifier.size(size: Int) = size(size.dp)
