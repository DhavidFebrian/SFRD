package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val igListings by viewModel.weeklyMeetingIgListings.collectAsStateWithLifecycle()
    val syncStatus by viewModel.syncStatus.collectAsStateWithLifecycle()
    val unreadCount by viewModel.unreadChatCount.collectAsStateWithLifecycle()

    // Computed stats
    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    val mediaStats = remember(allSchedules) {
        val aktif = allSchedules.count { s ->
            val typeLower = s.type.lowercase().trim()
            val isNonAktif = s.idListing.isNotBlank() && s.namaMe.isNotBlank() &&
                    s.lokasi.isNotBlank() && s.tanggal.isBlank() && s.jam.isBlank()
            !typeLower.startsWith("done") && typeLower.isNotBlank() && s.tanggal.trim().isNotBlank() && !isNonAktif
        }
        val selesai = allSchedules.count { s -> s.type.lowercase().trim().startsWith("done") }
        val nonAktif = allSchedules.count { s ->
            s.idListing.isNotBlank() && s.namaMe.isNotBlank() &&
                    s.lokasi.isNotBlank() && s.tanggal.isBlank() && s.jam.isBlank()
        }
        val hariIni = allSchedules.count { s -> s.tanggal.trim() == todayStr }
        MediaStats(total = allSchedules.size, aktif = aktif, selesai = selesai, nonAktif = nonAktif, hariIni = hariIni)
    }

    val contentStats = remember(allSchedules, editFotoTasks) {
        val fotoUlang = allSchedules.count { s ->
            val typeLower = s.type.lowercase().trim()
            val statusLower = s.status.lowercase().trim()
            typeLower.startsWith("done") && statusLower != "done"
        }
        val editFotoPending = editFotoTasks.count { !it.done }
        ContentStats(fotoUlangPending = fotoUlang, editFotoPending = editFotoPending)
    }

    val meetingStats = remember(meetingListings) {
        val total = meetingListings.size
        val igCount = meetingListings.count { it.keterangan.contains("ig", ignoreCase = true) || it.keterangan.contains("instagram", ignoreCase = true) }
        val posted = meetingListings.count { it.postingIg.lowercase().trim() in listOf("done", "ya", "yes", "true", "✔", "1") }
        MeetingStats(total = total, igListings = igCount, posted = posted)
    }

    val publishStats = remember(igListings) {
        val total = igListings.size
        val scheduled = igListings.count { l ->
            val j = l.jadwalPosting.trim()
            j.isNotEmpty() && j != "-" && !j.lowercase().contains("belum")
        }
        val posted = igListings.count { l ->
            l.postingIg.trim().lowercase() in listOf("done", "ya", "yes", "true", "✔", "1")
        }
        val unscheduled = igListings.count { l ->
            val j = l.jadwalPosting.trim()
            val noJadwal = j.isEmpty() || j == "-" || j.lowercase().contains("belum")
            val isPosted = l.postingIg.trim().lowercase() in listOf("done", "ya", "yes", "true", "✔", "1")
            noJadwal && !isPosted
        }
        PublishStats(total = total, scheduled = scheduled, posted = posted, unscheduled = unscheduled)
    }

    // Overall progress
    val overallProgress = remember(mediaStats, contentStats, publishStats) {
        val totalTasks = mediaStats.total + contentStats.fotoUlangPending + contentStats.editFotoPending + publishStats.total
        val completedTasks = mediaStats.selesai + publishStats.posted
        if (totalTasks > 0) (completedTasks.toFloat() / totalTasks).coerceIn(0f, 1f) else 0f
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            CenterAlignedTopAppBar(
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                    }
                },
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "WORKFLOW MONITOR",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Dashboard",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
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
                                    Badge { Text(unreadCount.toString()) }
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Sync status banner
            item(key = "sync_status") {
                if (syncStatus is SyncState.Loading) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Text(
                                "Memuat data terbaru...",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Overall progress card
            item(key = "overall_progress") {
                OverallProgressCard(
                    progress = overallProgress,
                    totalSchedules = mediaStats.total,
                    todayCount = mediaStats.hariIni,
                    pendingTasks = contentStats.fotoUlangPending + contentStats.editFotoPending,
                    unscheduledPublish = publishStats.unscheduled
                )
            }

            // Today's summary
            item(key = "today_summary") {
                TodaySummaryRow(
                    todaySchedules = mediaStats.hariIni,
                    meetingListings = meetingStats.total,
                    pendingEdits = contentStats.editFotoPending
                )
            }

            // Section: Workflow Overview Cards
            item(key = "section_title") {
                Text(
                    text = "Area Kerja",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Meeting card
            item(key = "card_meeting") {
                WorkflowCard(
                    title = "Meeting",
                    subtitle = "Weekly Meeting & Absensi",
                    icon = Icons.Default.Groups,
                    gradientColors = listOf(Color(0xFF1565C0), Color(0xFF42A5F5)),
                    stats = listOf(
                        StatItem("Total Listing", meetingStats.total.toString()),
                        StatItem("Listing IG", meetingStats.igListings.toString()),
                        StatItem("Sudah Post", meetingStats.posted.toString())
                    ),
                    progress = if (meetingStats.igListings > 0) meetingStats.posted.toFloat() / meetingStats.igListings else 0f,
                    progressLabel = "IG Posting Progress",
                    onClick = onNavigateToMeeting
                )
            }

            // Media card
            item(key = "card_media") {
                WorkflowCard(
                    title = "Media",
                    subtitle = "Schedule Foto RWC David",
                    icon = Icons.Default.PhotoLibrary,
                    gradientColors = listOf(Color(0xFF00897B), Color(0xFF4DB6AC)),
                    stats = listOf(
                        StatItem("Aktif", mediaStats.aktif.toString()),
                        StatItem("Selesai", mediaStats.selesai.toString()),
                        StatItem("Non-Aktif", mediaStats.nonAktif.toString()),
                        StatItem("Hari Ini", mediaStats.hariIni.toString())
                    ),
                    progress = if (mediaStats.total > 0) mediaStats.selesai.toFloat() / mediaStats.total else 0f,
                    progressLabel = "Jadwal Selesai",
                    onClick = onNavigateToMedia
                )
            }

            // Content card
            item(key = "card_content") {
                WorkflowCard(
                    title = "Content",
                    subtitle = "Task Foto Ulang & Edit Foto",
                    icon = Icons.Default.Article,
                    gradientColors = listOf(Color(0xFFE65100), Color(0xFFFF8A65)),
                    stats = listOf(
                        StatItem("Foto Ulang", contentStats.fotoUlangPending.toString()),
                        StatItem("Edit Foto", contentStats.editFotoPending.toString()),
                        StatItem("Total Pending", (contentStats.fotoUlangPending + contentStats.editFotoPending).toString())
                    ),
                    progress = 0f,
                    progressLabel = "Pending Tasks",
                    onClick = onNavigateToContent,
                    isWarning = (contentStats.fotoUlangPending + contentStats.editFotoPending) > 0
                )
            }

            // Publish card
            item(key = "card_publish") {
                WorkflowCard(
                    title = "Publish",
                    subtitle = "Upload & Scheduling IG",
                    icon = Icons.Default.Send,
                    gradientColors = listOf(Color(0xFF7B1FA2), Color(0xFFCE93D8)),
                    stats = listOf(
                        StatItem("Total IG", publishStats.total.toString()),
                        StatItem("Terjadwal", publishStats.scheduled.toString()),
                        StatItem("Belum", publishStats.unscheduled.toString()),
                        StatItem("Posted", publishStats.posted.toString())
                    ),
                    progress = if (publishStats.total > 0) publishStats.posted.toFloat() / publishStats.total else 0f,
                    progressLabel = "IG Posting Done",
                    onClick = onNavigateToPublish
                )
            }

            // Quick actions
            item(key = "quick_actions_title") {
                Text(
                    text = "Aksi Cepat",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item(key = "quick_actions") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionChip(
                        icon = Icons.Default.Groups,
                        label = "Meeting",
                        color = Color(0xFF1565C0),
                        onClick = onNavigateToMeeting,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionChip(
                        icon = Icons.Default.PhotoLibrary,
                        label = "Media",
                        color = Color(0xFF00897B),
                        onClick = onNavigateToMedia,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionChip(
                        icon = Icons.Default.Article,
                        label = "Content",
                        color = Color(0xFFE65100),
                        onClick = onNavigateToContent,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionChip(
                        icon = Icons.Default.Send,
                        label = "Publish",
                        color = Color(0xFF7B1FA2),
                        onClick = onNavigateToPublish,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// ── Data classes ─────────────────────────────────────────────────────────────────
private data class MediaStats(val total: Int, val aktif: Int, val selesai: Int, val nonAktif: Int, val hariIni: Int)
private data class ContentStats(val fotoUlangPending: Int, val editFotoPending: Int)
private data class MeetingStats(val total: Int, val igListings: Int, val posted: Int)
private data class PublishStats(val total: Int, val scheduled: Int, val posted: Int, val unscheduled: Int)
private data class StatItem(val label: String, val value: String)

// ── Overall Progress Card ────────────────────────────────────────────────────────
@Composable
private fun OverallProgressCard(
    progress: Float,
    totalSchedules: Int,
    todayCount: Int,
    pendingTasks: Int,
    unscheduledPublish: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
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
                        Text(
                            text = "Progress Keseluruhan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                            color = Color.White
                        )
                    }
                    Box(
                        modifier = Modifier.size(64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxSize(),
                            color = Color.White,
                            trackColor = Color.White.copy(alpha = 0.2f),
                            strokeWidth = 6.dp
                        )
                        Icon(
                            Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProgressStat("Total", totalSchedules.toString())
                    ProgressStat("Hari Ini", todayCount.toString())
                    ProgressStat("Pending", pendingTasks.toString())
                    ProgressStat("Unscheduled", unscheduledPublish.toString())
                }
            }
        }
    }
}

@Composable
private fun ProgressStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

// ── Today Summary Row ────────────────────────────────────────────────────────────
@Composable
private fun TodaySummaryRow(
    todaySchedules: Int,
    meetingListings: Int,
    pendingEdits: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SummaryChip(
            icon = Icons.Default.Today,
            label = "Foto Hari Ini",
            value = todaySchedules.toString(),
            color = Color(0xFF00897B),
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            icon = Icons.Default.Groups,
            label = "Meeting",
            value = meetingListings.toString(),
            color = Color(0xFF1565C0),
            modifier = Modifier.weight(1f)
        )
        SummaryChip(
            icon = Icons.Default.Edit,
            label = "Edit Pending",
            value = pendingEdits.toString(),
            color = Color(0xFFE65100),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SummaryChip(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(color.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Workflow Card ─────────────────────────────────────────────────────────────────
@Composable
private fun WorkflowCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    gradientColors: List<Color>,
    stats: List<StatItem>,
    progress: Float,
    progressLabel: String,
    onClick: () -> Unit,
    isWarning: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            1.dp,
            if (isWarning) Color(0xFFE65100).copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
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
                                Brush.linearGradient(gradientColors),
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                stats.forEach { stat ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stat.value,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                            color = gradientColors.first()
                        )
                        Text(
                            text = stat.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Progress bar
            if (progress > 0f || progressLabel.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    LinearProgressIndicator(
                        progress = { progress.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = gradientColors.first(),
                        trackColor = gradientColors.first().copy(alpha = 0.12f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = gradientColors.first()
                    )
                }
                Text(
                    text = progressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

// ── Quick Action Chip ────────────────────────────────────────────────────────────
@Composable
private fun QuickActionChip(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
