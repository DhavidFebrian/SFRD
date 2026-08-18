package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Schedule
import com.example.data.normalizeDate
import com.example.network.MeetingListing
import com.example.ui.ScheduleViewModel
import com.example.ui.SyncState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowDashboardScreen(
    viewModel: ScheduleViewModel,
    onNavigateToMeeting: () -> Unit,
    onNavigateToMedia: () -> Unit,
    onNavigateToContent: () -> Unit,
    onNavigateToPublish: () -> Unit,
    onNavigateToChat: () -> Unit,
    onOpenDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allSchedules by viewModel.allSchedules.collectAsStateWithLifecycle()
    val editFotoTasks by viewModel.allEditFotoTasks.collectAsStateWithLifecycle()
    val meetingListings by viewModel.meetingListings.collectAsStateWithLifecycle()
    val allMonthlyMeetingListings by viewModel.allMonthlyMeetingListings.collectAsStateWithLifecycle()
    val igListings by viewModel.weeklyMeetingIgListings.collectAsStateWithLifecycle()
    val listingImagesMap by viewModel.listingImagesMap.collectAsStateWithLifecycle()
    val listingImagesGalleryMap by viewModel.listingImagesGalleryMap.collectAsStateWithLifecycle()
    val agentInfoMap by viewModel.agentInfoMap.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadChatCount.collectAsStateWithLifecycle()
    val selectedMonth by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val selectedMeetingDate by viewModel.selectedMeetingDate.collectAsStateWithLifecycle()

    var selectedScheduleForDetail by remember { mutableStateOf<Schedule?>(null) }
    var isCalendarScheduleExpanded by remember { mutableStateOf(false) }

    // 1. Current dates computation (Today & Tomorrow)
    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    val tomorrowStr = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    // 2. Photo Sessions filtered for Selected Month & This Week
    val currentMonthSchedules = remember(allSchedules, selectedMonth) {
        val monthNorm = selectedMonth.replace("Recap Meeting ", "").trim().lowercase()
        val targetMonthNum = when {
            monthNorm.contains("jan") -> "01"
            monthNorm.contains("feb") -> "02"
            monthNorm.contains("mar") -> "03"
            monthNorm.contains("apr") -> "04"
            monthNorm.contains("mei") || monthNorm.contains("may") -> "05"
            monthNorm.contains("jun") -> "06"
            monthNorm.contains("jul") -> "07"
            monthNorm.contains("agu") || monthNorm.contains("aug") -> "08"
            monthNorm.contains("sep") -> "09"
            monthNorm.contains("okt") || monthNorm.contains("oct") -> "10"
            monthNorm.contains("nov") -> "11"
            monthNorm.contains("des") || monthNorm.contains("dec") -> "12"
            else -> ""
        }
        if (targetMonthNum.isEmpty()) {
            allSchedules
        } else {
            allSchedules.filter { s ->
                val norm = normalizeDate(s.tanggal)
                if (norm.length >= 7) norm.substring(5, 7) == targetMonthNum else true
            }
        }
    }

    val thisWeekPhotosCount = remember(allSchedules) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startOfWeek = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        cal.add(Calendar.DAY_OF_WEEK, 6)
        val endOfWeek = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)

        allSchedules.count { s ->
            val norm = normalizeDate(s.tanggal)
            norm.isNotEmpty() && norm in startOfWeek..endOfWeek
        }
    }

    val mediaStats = remember(currentMonthSchedules, allSchedules, todayStr) {
        val aktif = currentMonthSchedules.count { s ->
            val typeLower = s.type.lowercase().trim()
            val isNonAktif = s.idListing.isNotBlank() && s.namaMe.isNotBlank() &&
                    s.lokasi.isNotBlank() && s.tanggal.isBlank() && s.jam.isBlank()
            !typeLower.startsWith("done") && typeLower.isNotBlank() && s.tanggal.trim().isNotBlank() && !isNonAktif
        }
        val selesai = currentMonthSchedules.count { s -> s.type.lowercase().trim().startsWith("done") }
        val nonAktif = currentMonthSchedules.count { s ->
            s.idListing.isNotBlank() && s.namaMe.isNotBlank() &&
                    s.lokasi.isNotBlank() && s.tanggal.isBlank() && s.jam.isBlank()
        }
        val hariIni = allSchedules.count { s -> normalizeDate(s.tanggal) == todayStr }
        MediaStats(
            total = currentMonthSchedules.size,
            aktif = aktif,
            selesai = selesai,
            nonAktif = nonAktif,
            hariIni = hariIni,
            mingguIni = thisWeekPhotosCount
        )
    }

    // 3. Foto Besok Count (scheduled for tomorrow across all schedules)
    val fotoBesokCount = remember(allSchedules, tomorrowStr) {
        allSchedules.count { s -> normalizeDate(s.tanggal) == tomorrowStr }
    }

    val schedulesByDate = remember(allSchedules) {
        allSchedules.groupBy { normalizeDate(it.tanggal) }
    }

    var selectedCalendarDate by remember { mutableStateOf<String?>(null) }

    // 4. Content Stats (5 Categories: Semua, Up Foto, Edit Video, Garis Tanah, Edit Foto)
    val contentStats = remember(allSchedules, editFotoTasks) {
        val pendingSchedules = allSchedules.filter { s ->
            val typeLower = s.type.lowercase().trim()
            val statusLower = s.status.lowercase().trim()
            typeLower.startsWith("done") && statusLower != "done"
        }
        val pendingEditTasks = editFotoTasks.filter { !it.done }

        val upFotoCount = pendingSchedules.count { s ->
            val t = s.type.lowercase()
            val st = s.status.lowercase()
            t.contains("up foto") || st.contains("up foto")
        } + pendingEditTasks.count {
            it.editNotes.contains("up foto", ignoreCase = true) || it.judul.contains("up foto", ignoreCase = true)
        }

        val videoCount = pendingSchedules.count { s ->
            val t = s.type.lowercase()
            val st = s.status.lowercase()
            t.contains("video") || st.contains("video")
        } + pendingEditTasks.count {
            it.editNotes.contains("video", ignoreCase = true) || it.judul.contains("video", ignoreCase = true)
        }

        val garisTanahCount = pendingSchedules.count { s ->
            val t = s.type.lowercase()
            val st = s.status.lowercase()
            t.contains("garis") || t.contains("tanah") || st.contains("garis") || st.contains("tanah")
        } + pendingEditTasks.count {
            it.editNotes.contains("garis", ignoreCase = true) || it.editNotes.contains("tanah", ignoreCase = true) || it.judul.contains("garis", ignoreCase = true) || it.judul.contains("tanah", ignoreCase = true)
        }

        val editFotoCount = pendingEditTasks.size
        val totalPending = pendingSchedules.size + pendingEditTasks.size

        ContentStats(
            total = totalPending,
            upFoto = upFotoCount,
            editVideo = videoCount,
            garisTanah = garisTanahCount,
            editFoto = editFotoCount
        )
    }

    // 5. Weekly Meeting Stats (for selected meeting date)
    val meetingStats = remember(meetingListings) {
        val total = meetingListings.size
        val igCount = meetingListings.count {
            it.keterangan.contains("ig", ignoreCase = true) || it.keterangan.contains("instagram", ignoreCase = true)
        }
        val published = meetingListings.count {
            val isIg = it.keterangan.contains("ig", ignoreCase = true) || it.keterangan.contains("instagram", ignoreCase = true)
            val isPosted = it.postingIg.lowercase().trim() in listOf("done", "ya", "yes", "true", "✔", "1")
            isIg && isPosted
        }
        val unpublished = meetingListings.count {
            val isIg = it.keterangan.contains("ig", ignoreCase = true) || it.keterangan.contains("instagram", ignoreCase = true)
            val isPosted = it.postingIg.lowercase().trim() in listOf("done", "ya", "yes", "true", "✔", "1")
            isIg && !isPosted
        }
        val fotoUlang = meetingListings.count {
            it.keterangan.contains("foto ulang", ignoreCase = true) || it.catatan.contains("foto ulang", ignoreCase = true)
        }
        MeetingStats(
            total = total,
            igListings = igCount,
            published = published,
            unpublished = unpublished,
            fotoUlang = fotoUlang
        )
    }

    // 6. Weekly Meeting Breakdown for Bar Chart
    val meetingWeeksData = remember(allMonthlyMeetingListings, meetingListings, selectedMeetingDate, selectedMonth) {
        val cal = Calendar.getInstance(Locale("id", "ID"))
        val monthIdx = when {
            selectedMonth.contains("Jan", ignoreCase = true) -> 0
            selectedMonth.contains("Feb", ignoreCase = true) -> 1
            selectedMonth.contains("Mar", ignoreCase = true) -> 2
            selectedMonth.contains("Apr", ignoreCase = true) -> 3
            selectedMonth.contains("Mei", ignoreCase = true) || selectedMonth.contains("May", ignoreCase = true) -> 4
            selectedMonth.contains("Jun", ignoreCase = true) -> 5
            selectedMonth.contains("Jul", ignoreCase = true) -> 6
            selectedMonth.contains("Agu", ignoreCase = true) || selectedMonth.contains("Aug", ignoreCase = true) -> 7
            selectedMonth.contains("Sep", ignoreCase = true) -> 8
            selectedMonth.contains("Okt", ignoreCase = true) || selectedMonth.contains("Oct", ignoreCase = true) -> 9
            selectedMonth.contains("Nov", ignoreCase = true) -> 10
            selectedMonth.contains("Des", ignoreCase = true) || selectedMonth.contains("Dec", ignoreCase = true) -> 11
            else -> cal.get(Calendar.MONTH)
        }
        val meetingDates = viewModel.getMeetingDatesForMonth(monthIdx)
        val monthNumStr = (monthIdx + 1).toString().padStart(2, '0')

        meetingDates.mapIndexed { index, (day, fullName) ->
            val dateStr = "2026-$monthNumStr-${day.padStart(2, '0')}"
            val dayLabel = try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
                SimpleDateFormat("d MMM", Locale("id", "ID")).format(d!!)
            } catch (e: Exception) {
                "$day ${selectedMonth.take(3)}"
            }

            // Count listings from allMonthlyMeetingListings or current meetingListings
            val matchingListings = allMonthlyMeetingListings.filter {
                val nDate = normalizeDate(it.date)
                nDate == dateStr || it.date.contains(day)
            }
            val count = if (matchingListings.isNotEmpty()) {
                matchingListings.size
            } else if (selectedMeetingDate == dateStr && meetingListings.isNotEmpty()) {
                meetingListings.size
            } else {
                0
            }

            val isSelected = selectedMeetingDate == dateStr
            MeetingWeekBarItem(
                weekNumber = index + 1,
                dateStr = dateStr,
                dayLabel = dayLabel,
                totalListings = count,
                isSelected = isSelected
            )
        }
    }

    // 7. Publish Weekly Stats (corresponding directly to the selected weekly meeting)
    val weeklyMeetingIgItems = remember(meetingListings) {
        meetingListings.filter {
            it.keterangan.contains("ig", ignoreCase = true) || it.keterangan.contains("instagram", ignoreCase = true)
        }
    }

    // Hitung berapa ID Listing yang dijadwalkan untuk Post Hari Ini (dari data Publish / IG listings)
    val postHariIniCount = remember(igListings, allMonthlyMeetingListings, editFotoTasks, todayStr) {
        val targetIds = mutableSetOf<String>()
        igListings.forEach { item ->
            val d = normalizeDate(item.jadwalPosting)
            if (d == todayStr && item.idListing.trim().isNotBlank()) {
                targetIds.add(item.idListing.trim())
            }
        }
        allMonthlyMeetingListings.forEach { item ->
            val d = normalizeDate(item.jadwalPosting)
            if (d == todayStr && item.idListing.trim().isNotBlank()) {
                targetIds.add(item.idListing.trim())
            }
        }
        editFotoTasks.forEach { task ->
            val d = normalizeDate(task.jadwalPosting)
            if (d == todayStr && task.idListing.trim().isNotBlank()) {
                targetIds.add(task.idListing.trim())
            }
        }
        targetIds.size
    }

    val publishWeeklyStats = remember(weeklyMeetingIgItems) {
        val total = weeklyMeetingIgItems.size
        val scheduled = weeklyMeetingIgItems.count { l ->
            val j = l.jadwalPosting.trim()
            j.isNotEmpty() && j != "-" && !j.lowercase().contains("belum")
        }
        val posted = weeklyMeetingIgItems.count { l ->
            l.postingIg.trim().lowercase() in listOf("done", "ya", "yes", "true", "✔", "1")
        }
        val unposted = weeklyMeetingIgItems.count { l ->
            l.postingIg.trim().lowercase() !in listOf("done", "ya", "yes", "true", "✔", "1")
        }
        PublishWeeklyStats(
            total = total,
            scheduled = scheduled,
            posted = posted,
            unposted = unposted
        )
    }

    // 8. Overall Progress Computation
    val overallProgress = remember(mediaStats, contentStats, publishWeeklyStats) {
        val totalTasks = mediaStats.total + contentStats.total + publishWeeklyStats.total
        val completedTasks = mediaStats.selesai + publishWeeklyStats.posted
        if (totalTasks > 0) (completedTasks.toFloat() / totalTasks).coerceIn(0f, 1f) else 0f
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = "WORKFLOW MONITOR",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Dashboard Utama",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToChat,
                        modifier = Modifier.testTag("dashboard_chat_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ) { Text(unreadCount.toString()) }
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
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Sync status banner
            item(key = "sync_status") {
                if (syncStatus is SyncState.Loading) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Menyelaraskan data terkini...",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Month Picker Selector for Dashboard
            item(key = "month_picker") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Juni 2026", "Juli 2026", "Agustus 2026").forEach { month ->
                        val isSelected = selectedMonth == month
                        Surface(
                            onClick = { viewModel.selectMonth(month) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(32.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = month,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 1. Overall Progress Card with Live Looping Visual & Interactive Glow
            item(key = "overall_progress") {
                OverallProgressCard(
                    progress = overallProgress,
                    todayCount = mediaStats.hariIni,
                    tomorrowCount = fotoBesokCount,
                    pendingTasks = mediaStats.aktif,
                    postHariIniCount = postHariIniCount,
                    unscheduledPublish = publishWeeklyStats.unposted
                )
            }

            // 2. Interactive Calendar Section (Pindah ke Atas, Langsung di Bawah Progress Keseluruhan)
            item(key = "calendar_section_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
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
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Kalender Jadwal Kegiatan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (selectedCalendarDate != null) {
                        Surface(
                            onClick = { selectedCalendarDate = null },
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = "Reset Tanggal",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item(key = "calendar_view") {
                DashboardCalendarView(
                    schedules = allSchedules,
                    selectedDate = selectedCalendarDate,
                    onDateSelected = { date -> selectedCalendarDate = date }
                )
            }

            // Schedules Review 2x2 Grid for selected calendar date (with Expand/Collapse if > 4)
            item(key = "calendar_schedules_list") {
                val targetNormDate = selectedCalendarDate?.let { normalizeDate(it) } ?: ""
                val matchingSchedules = if (targetNormDate.isNotBlank()) schedulesByDate[targetNormDate] ?: emptyList() else emptyList()

                if (selectedCalendarDate != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Review Jadwal: $selectedCalendarDate (${matchingSchedules.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (matchingSchedules.size > 4) {
                                TextButton(
                                    onClick = { isCalendarScheduleExpanded = !isCalendarScheduleExpanded },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isCalendarScheduleExpanded) "Tampilkan 4 Saja" else "Lihat Semua (${matchingSchedules.size})",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        if (matchingSchedules.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Tidak ada jadwal foto pada tanggal ini.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(14.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            val displayedList = if (isCalendarScheduleExpanded) matchingSchedules else matchingSchedules.take(4)
                            val chunkedPairs = displayedList.chunked(2)

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                chunkedPairs.forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        rowItems.forEach { schedule ->
                                            Box(modifier = Modifier.weight(1f)) {
                                                DetailedScheduleReviewGridItem(
                                                    schedule = schedule,
                                                    listingImagesMap = listingImagesMap,
                                                    onFetchImage = { id -> viewModel.fetchListingImageIfNeeded(id, schedule.namaMe) },
                                                    onClick = { selectedScheduleForDetail = schedule }
                                                )
                                            }
                                        }
                                        if (rowItems.size == 1) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Section Header: Area Kerja
            item(key = "section_title") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Area Kerja Workflow",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = selectedMonth,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 4. Meeting Card (Total Listing, IG, Published, Unpublished, Foto Ulang + Weekly Bar Chart)
            item(key = "card_meeting") {
                MeetingWorkflowCard(
                    stats = meetingStats,
                    selectedMeetingDate = selectedMeetingDate,
                    weeksData = meetingWeeksData,
                    onSelectMeetingWeek = { weekDate ->
                        viewModel.fetchMeetingListings(
                            viewModel.getWeeklyMeetingSheetNameForMonth(selectedMonth),
                            weekDate
                        )
                    },
                    onClick = onNavigateToMeeting
                )
            }

            // 5. Media Card: Foto Ulang (Schedule Foto RWC David)
            item(key = "card_media") {
                MediaWorkflowCard(
                    stats = mediaStats,
                    selectedMonth = selectedMonth,
                    onClick = onNavigateToMedia
                )
            }

            // 6. Content Card: 5 Content Tabs (Semua, Up Foto, Edit Video, Garis Tanah, Edit Foto)
            item(key = "card_content") {
                ContentWorkflowCard(
                    stats = contentStats,
                    onClick = onNavigateToContent
                )
            }

            // 7. Publish Card: Upload & Scheduling IG (Weekly Stats)
            item(key = "card_publish") {
                PublishWorkflowCard(
                    stats = publishWeeklyStats,
                    selectedMeetingDate = selectedMeetingDate,
                    onClick = onNavigateToPublish
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

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
}

// ── Data Classes ─────────────────────────────────────────────────────────────────
private data class MediaStats(
    val total: Int,
    val aktif: Int,
    val selesai: Int,
    val nonAktif: Int,
    val hariIni: Int,
    val mingguIni: Int
)

private data class ContentStats(
    val total: Int,
    val upFoto: Int,
    val editVideo: Int,
    val garisTanah: Int,
    val editFoto: Int
)

private data class MeetingStats(
    val total: Int,
    val igListings: Int,
    val published: Int,
    val unpublished: Int,
    val fotoUlang: Int
)

private data class MeetingWeekBarItem(
    val weekNumber: Int,
    val dateStr: String,
    val dayLabel: String,
    val totalListings: Int,
    val isSelected: Boolean
)

private data class PublishWeeklyStats(
    val total: Int,
    val scheduled: Int,
    val posted: Int,
    val unposted: Int
)

// ── Overall Progress Card with Live Looping Visual & Interactive Glow ─────────────
@Composable
private fun OverallProgressCard(
    progress: Float,
    todayCount: Int,
    tomorrowCount: Int,
    pendingTasks: Int,
    postHariIniCount: Int,
    unscheduledPublish: Int
) {
    val infiniteTransition = rememberInfiniteTransition(label = "overall_progress_loop")

    val borderAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_angle"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 0.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Continuous rotating border beam wrapper
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .drawWithContent {
                val brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF38BDF8),
                        Color(0xFF818CF8),
                        Color(0x2238BDF8),
                        Color(0xFF34D399),
                        Color(0xFF38BDF8)
                    )
                )
                rotate(borderAngle) {
                    drawCircle(
                        brush = brush,
                        radius = size.maxDimension
                    )
                }
                drawContent()
            }
            .padding(1.5.dp) // Border thickness
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(21.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E3A8A),
                            Color(0xFF0F766E),
                            Color(0xFF1E1B4B)
                        ),
                        start = androidx.compose.ui.geometry.Offset(
                            (kotlin.math.cos(Math.toRadians(borderAngle.toDouble())) * 300).toFloat(),
                            (kotlin.math.sin(Math.toRadians(borderAngle.toDouble())) * 300).toFloat()
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            (-kotlin.math.cos(Math.toRadians(borderAngle.toDouble())) * 300).toFloat() + 800f,
                            (-kotlin.math.sin(Math.toRadians(borderAngle.toDouble())) * 300).toFloat() + 800f
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(Color(0xFF4ADE80), CircleShape)
                            )
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFF67E8F9),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Progress Keseluruhan",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.3.sp
                                ),
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.5).sp
                            ),
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier.size(68.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulsing outer beacon ring
                        Box(
                            modifier = Modifier
                                .size(64.dp * pulseScale)
                                .background(Color(0xFF38BDF8).copy(alpha = pulseAlpha * 0.4f), CircleShape)
                        )
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = Color(0xFF38BDF8),
                            trackColor = Color.White.copy(alpha = 0.15f),
                            strokeWidth = 6.dp
                        )
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFFFDE047),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(7.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF38BDF8),
                    trackColor = Color.White.copy(alpha = 0.18f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row with 5 Metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProgressStatItem(label = "Hari Ini", value = todayCount.toString(), badgeColor = Color(0xFF34D399))
                    ProgressStatItem(label = "Foto Besok", value = tomorrowCount.toString(), badgeColor = Color(0xFFFBBF24))
                    ProgressStatItem(label = "Pending", value = pendingTasks.toString(), badgeColor = Color(0xFFF87171))
                    ProgressStatItem(label = "Post Hari Ini", value = postHariIniCount.toString(), badgeColor = Color(0xFF38BDF8))
                    ProgressStatItem(label = "Belum Post", value = unscheduledPublish.toString(), badgeColor = Color(0xFFC084FC))
                }
            }
        }
    }
}

@Composable
private fun ProgressStatItem(
    label: String,
    value: String,
    badgeColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(badgeColor, CircleShape)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
            color = Color.White.copy(alpha = 0.75f)
        )
    }
}

// ── Meeting Workflow Card ────────────────────────────────────────────────────────
@Composable
private fun MeetingWorkflowCard(
    stats: MeetingStats,
    selectedMeetingDate: String?,
    weeksData: List<MeetingWeekBarItem>,
    onSelectMeetingWeek: (String) -> Unit,
    onClick: () -> Unit
) {
    val formattedDateText = remember(selectedMeetingDate) {
        if (!selectedMeetingDate.isNullOrBlank()) {
            try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedMeetingDate)
                SimpleDateFormat("d MMMM yyyy", Locale("id", "ID")).format(d!!)
            } catch (e: Exception) { selectedMeetingDate }
        } else {
            "Pilih Tanggal Meeting"
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF1E88E5), Color(0xFF42A5F5))),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Groups, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            text = "Meeting",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Tanggal: $formattedDateText",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Buka",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5 Info Metrics: Total Listing, IG, Published, Unpublished, Foto Ulang
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumn(label = "Total Listing", value = stats.total.toString(), color = Color(0xFF1E88E5), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "IG", value = stats.igListings.toString(), color = Color(0xFF0284C7), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Published", value = stats.published.toString(), color = Color(0xFF16A34A), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Unpublished", value = stats.unpublished.toString(), color = Color(0xFFDC2626), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Foto Ulang", value = stats.fotoUlang.toString(), color = Color(0xFFEA580C), modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Weekly Meeting Bar Chart (Diagram Batang)
            Text(
                text = "Rincian Total Meeting Tiap Minggu:",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            MeetingWeeklyBarChart(
                weeksData = weeksData,
                onSelectWeek = onSelectMeetingWeek
            )
        }
    }
}

// ── Meeting Weekly Bar Chart Component ──────────────────────────────────────────
@Composable
private fun MeetingWeeklyBarChart(
    weeksData: List<MeetingWeekBarItem>,
    onSelectWeek: (String) -> Unit
) {
    val maxCount = remember(weeksData) {
        weeksData.maxOfOrNull { it.totalListings }?.coerceAtLeast(1) ?: 1
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        weeksData.forEach { item ->
            val heightFraction = (item.totalListings.toFloat() / maxCount).coerceIn(0.12f, 1f)
            val barColor = if (item.isSelected) {
                Brush.verticalGradient(listOf(Color(0xFF2563EB), Color(0xFF60A5FA)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1)))
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelectWeek(item.dateStr) }
                    .padding(horizontal = 4.dp)
            ) {
                // Count pill on top of bar
                Text(
                    text = "${item.totalListings}",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (item.isSelected) FontWeight.Black else FontWeight.Bold,
                        fontSize = 10.sp
                    ),
                    color = if (item.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(3.dp))

                // The visual bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .fillMaxHeight(heightFraction * 0.65f)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(barColor)
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Week date label
                Text(
                    text = item.dayLabel,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = if (item.isSelected) FontWeight.ExtraBold else FontWeight.Medium
                    ),
                    color = if (item.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ── Media Workflow Card (Foto Ulang / Schedule Foto RWC David) ───────────────────
@Composable
private fun MediaWorkflowCard(
    stats: MediaStats,
    selectedMonth: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF00897B), Color(0xFF26A69A))),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            text = "Foto Ulang",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Foto RWC David · $selectedMonth",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF00897B),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Buka",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats row (Aktif, Selesai, Minggu Ini, Hari Ini)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumn(label = "Aktif", value = stats.aktif.toString(), color = Color(0xFF00897B), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Selesai", value = stats.selesai.toString(), color = Color(0xFF16A34A), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Minggu Ini", value = stats.mingguIni.toString(), color = Color(0xFF0284C7), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Hari Ini", value = stats.hariIni.toString(), color = Color(0xFFEAB308), modifier = Modifier.weight(1f))
            }
        }
    }
}

// ── Content Workflow Card (5 Content Tabs) ──────────────────────────────────────
@Composable
private fun ContentWorkflowCard(
    stats: ContentStats,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (stats.total > 0) Color(0xFFEA580C).copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFFEA580C), Color(0xFFFB923C))),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Article, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            text = "Content Desk",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "5 Kategori Konten",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFEA580C),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Buka",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5 Info Metrics: Semua, Up Foto, Edit Video, Garis Tanah, Edit Foto
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumn(label = "Semua", value = stats.total.toString(), color = Color(0xFFEA580C), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Up Foto", value = stats.upFoto.toString(), color = Color(0xFF0284C7), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Edit Video", value = stats.editVideo.toString(), color = Color(0xFF8B5CF6), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Garis Tanah", value = stats.garisTanah.toString(), color = Color(0xFF10B981), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Edit Foto", value = stats.editFoto.toString(), color = Color(0xFFF59E0B), modifier = Modifier.weight(1f))
            }
        }
    }
}

// ── Publish Workflow Card (Weekly Stats for Selected Meeting Date) ────────────────
@Composable
private fun PublishWorkflowCard(
    stats: PublishWeeklyStats,
    selectedMeetingDate: String?,
    onClick: () -> Unit
) {
    val dateLabel = remember(selectedMeetingDate) {
        if (!selectedMeetingDate.isNullOrBlank()) {
            try {
                val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedMeetingDate)
                SimpleDateFormat("d MMM yyyy", Locale("id", "ID")).format(d!!)
            } catch (e: Exception) { selectedMeetingDate }
        } else {
            "Mingguan"
        }
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF7E22CE), Color(0xFFA855F7))),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            text = "Publish Desk",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Mingguan: $dateLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF7E22CE),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Buka",
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4 Stats: Total IG, Terjadwal, Unpublished, Published
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MetricColumn(label = "Total IG", value = stats.total.toString(), color = Color(0xFF7E22CE), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Terjadwal", value = stats.scheduled.toString(), color = Color(0xFF0284C7), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Unpublished", value = stats.unposted.toString(), color = Color(0xFFEA580C), modifier = Modifier.weight(1f))
                DividerItem()
                MetricColumn(label = "Published", value = stats.posted.toString(), color = Color(0xFF16A34A), modifier = Modifier.weight(1f))
            }
        }
    }
}

// ── Shared Metric Column Item ────────────────────────────────────────────────────
@Composable
private fun MetricColumn(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier.padding(horizontal = 2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DividerItem() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(24.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    )
}

// ── Detailed 2x2 Grid Schedule Review Item for Dashboard Calendar ───────────────
@Composable
private fun DetailedScheduleReviewGridItem(
    schedule: Schedule,
    listingImagesMap: Map<String, String>,
    onFetchImage: (String) -> Unit,
    onClick: () -> Unit
) {
    val cleanId = schedule.idListing.trim()
    LaunchedEffect(cleanId) {
        if (cleanId.isNotBlank()) {
            onFetchImage(cleanId)
        }
    }
    val typeLower = schedule.type.lowercase().trim()
    val isDone = typeLower.startsWith("done")
    val statusColor = if (isDone) MaterialTheme.colorScheme.primary else Color(0xFF38BDF8)
    val imageUrl = listingImagesMap[cleanId]

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image Header with status badge & ID
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Foto Listing $cleanId",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Status Badge Top Right
                Surface(
                    shape = RoundedCornerShape(topStart = 0.dp, bottomStart = 8.dp, topEnd = 0.dp, bottomEnd = 0.dp),
                    color = statusColor.copy(alpha = 0.92f),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = if (isDone) "SELESAI" else "AKTIF",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 8.5.sp
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Listing ID badge Bottom Left
                if (cleanId.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.7f),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(5.dp)
                    ) {
                        Text(
                            text = "#$cleanId",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.5.sp),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            // Info Body
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = schedule.namaMe.ifBlank { "Listing #$cleanId" },
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = schedule.lokasi.ifBlank { "Lokasi belum diset" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (schedule.jam.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.padding(top = 1.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = schedule.jam,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 9.5.sp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
