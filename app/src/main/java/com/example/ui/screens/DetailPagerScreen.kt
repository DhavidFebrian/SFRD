package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.Schedule
import com.example.ui.AgentInfo
import com.example.ui.ScheduleViewModel
import com.example.ui.getAgentPhoneByName
import com.example.ui.getAgentEmailByName
import com.example.ui.getAgentInstagramByName
import com.example.ui.capitalizeName
import com.example.ui.cleanListingDescription
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DetailPagerScreen(
    schedule: Schedule,
    listingImagesMap: Map<String, String>,
    listingImagesGalleryMap: Map<String, List<String>>,
    agentInfoMap: Map<String, AgentInfo>,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToChat: (() -> Unit)? = null,
    onEditMeetingListing: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Automatically trigger scraping when entering screen to load price/description/images
    LaunchedEffect(schedule.idListing) {
        viewModel.fetchListingImageIfNeeded(schedule.idListing, schedule.namaMe)
        viewModel.fetchYearlyIgPostingHistory()
    }

    val listingPriceMap by viewModel.listingPriceMap.collectAsState()
    val listingDescMap by viewModel.listingDescMap.collectAsState()
    val listingSoldMap by viewModel.listingSoldMap.collectAsState()
    val listingImagesMapState by viewModel.listingImagesMap.collectAsState()
    val listingImagesGalleryMapState by viewModel.listingImagesGalleryMap.collectAsState()
    val agentInfoMapState by viewModel.agentInfoMap.collectAsState()
    val activeScrapes by viewModel.activeScrapes.collectAsState()
    
    // Pager state starting at Index 1 (Center: Detail Listing)
    val pagerState = rememberPagerState(initialPage = 1) { 3 }
    
    androidx.activity.compose.BackHandler(enabled = true) {
        if (pagerState.currentPage != 1) {
            coroutineScope.launch {
                pagerState.animateScrollToPage(1)
            }
        } else {
            onDismiss()
        }
    }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showAttachDialog by remember { mutableStateOf(false) }
    var attachComment by remember { mutableStateOf("") }

    val currentTab = when (pagerState.currentPage) {
        0 -> "EDIT"
        1 -> "DETAIL"
        else -> "FOLLOW UP"
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("detail_pager_screen"),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Jadwal ${schedule.idListing.ifBlank { "Detail" }}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = when (pagerState.currentPage) {
                                0 -> "Halaman Edit Jadwal"
                                1 -> "Halaman Detail"
                                else -> "Halaman Follow Up WhatsApp"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (pagerState.currentPage != 1) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(1)
                                }
                            } else {
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                actions = {
                    val isMeetingResult = schedule.type == "Meeting Result"
                    if (isMeetingResult) {
                        if (onEditMeetingListing != null) {
                            IconButton(onClick = { onEditMeetingListing() }) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Data Meeting",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Hapus Jadwal",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        },
        floatingActionButton = {
            val isMeetingResult = schedule.type == "Meeting Result"
            if (!isMeetingResult) {
                ExtendedFloatingActionButton(
                    onClick = { showAttachDialog = true },
                    icon = { Icon(Icons.Default.Forum, contentDescription = "Attach to Chat") },
                    text = { Text("Kirim Ke Chat") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.testTag("detail_chat_attach_fab")
                )
            }
        },
        bottomBar = {
            val isMeetingResult = schedule.type == "Meeting Result"
            if (!isMeetingResult) {
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val items = listOf(
                            Triple(0, Icons.Default.Edit, "Edit Jadwal"),
                            Triple(1, Icons.Default.Info, "Detail Listing"),
                            Triple(2, Icons.Default.Chat, "Follow Up")
                        )
                        
                        items.forEach { (index, icon, label) ->
                            val selected = pagerState.currentPage == index
                            val contentColor = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .shadow(
                                        elevation = if (selected) 3.dp else 1.dp,
                                        shape = RoundedCornerShape(12.dp),
                                        clip = false
                                    )
                                    .clickable {
                                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                    },
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f)
                                    } else {
                                        Color.Transparent
                                    }
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (selected) {
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label,
                                        tint = contentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 10.sp
                                        ),
                                        color = contentColor,
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
    ) { innerPadding ->
        val isMeetingResult = schedule.type == "Meeting Result"
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isMeetingResult,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { pageIndex ->
            when (pageIndex) {
                0 -> PageEdit(
                    schedule = schedule,
                    viewModel = viewModel,
                    onSaveSuccess = {
                        coroutineScope.launch {
                            Toast.makeText(context, "Perubahan berhasil disimpan lokal & Sheets!", Toast.LENGTH_SHORT).show()
                            pagerState.animateScrollToPage(1)
                        }
                    },
                    onBack = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                )
                1 -> PageDetailHome(
                    schedule = schedule,
                    listingImagesMap = listingImagesMapState,
                    listingImagesGalleryMap = listingImagesGalleryMapState,
                    agentInfoMap = agentInfoMapState,
                    listingPriceMap = listingPriceMap,
                    listingDescMap = listingDescMap,
                    viewModel = viewModel,
                    isSold = listingSoldMap[schedule.idListing.trim()] == true,
                    isScraping = activeScrapes.contains(schedule.idListing.trim()),
                    onNavigateToEdit = { coroutineScope.launch { pagerState.animateScrollToPage(0) } },
                    onNavigateToFollowUp = { coroutineScope.launch { pagerState.animateScrollToPage(2) } },
                    onBack = onDismiss
                )
                2 -> PageFollowUp(
                    schedule = schedule,
                    agentInfoMap = agentInfoMapState,
                    onBack = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                )
            }
        }
    }

    if (showDeleteConfirmDialog) {
        val isEditFoto = schedule.type == "Edit Foto"
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Konfirmasi Hapus", fontWeight = FontWeight.Bold) },
            text = {
                val itemTitle = schedule.idListing.ifBlank { schedule.namaMe }
                Text("Apakah Anda yakin ingin menghapus jadwal '$itemTitle' dari HP dan Google Sheets secara online?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        if (isEditFoto) {
                            val editFotoTask = com.example.data.EditFotoTask(
                                id = schedule.id,
                                no = schedule.no,
                                idListing = schedule.idListing,
                                namaMe = schedule.namaMe,
                                postingIg = schedule.lokasi.contains("[Posting IG: Ya]"),
                                jadwalPosting = schedule.tanggal,
                                editNotes = schedule.lokasi.replace("[Posting IG: Ya] ", "").replace("[Posting IG: Tidak] ", ""),
                                done = (schedule.status == "Done"),
                                judul = schedule.jam,
                                source = schedule.staff,
                                synced = schedule.synced
                            )
                            viewModel.deleteEditFotoTask(editFotoTask)
                        } else {
                            viewModel.deleteSchedule(schedule)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
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

    if (showAttachDialog) {
        val toastContext = LocalContext.current
        AlertDialog(
            onDismissRequest = { showAttachDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Attachment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Kirim Lampiran ke Chat", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Kirim detail jadwal '${schedule.idListing.ifBlank { schedule.namaMe }}' ini sebagai lampiran ke chat obrolan tim.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = attachComment,
                        onValueChange = { attachComment = it },
                        placeholder = { Text("Ketik pesan tambahan di sini (opsional)...") },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        maxLines = 3,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = {
                            val attachMsg = "[ATTACH:idListing=${schedule.idListing}&no=${schedule.no}&type=${schedule.type}&namaMe=${schedule.namaMe}] $attachComment"
                            coroutineScope.launch {
                                viewModel.sendChatMessage(attachMsg)
                                android.widget.Toast.makeText(toastContext, "Berhasil dikirim ke Chat!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            showAttachDialog = false
                            attachComment = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kirim saja")
                    }
                    Button(
                        onClick = {
                            val attachMsg = "[ATTACH:idListing=${schedule.idListing}&no=${schedule.no}&type=${schedule.type}&namaMe=${schedule.namaMe}] $attachComment"
                            coroutineScope.launch {
                                viewModel.sendChatMessage(attachMsg)
                                android.widget.Toast.makeText(toastContext, "Berhasil dikirim ke Chat!", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            showAttachDialog = false
                            attachComment = ""
                            onNavigateToChat?.invoke()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Kirim & Buka Chat")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAttachDialog = false }) {
                    Text("Batal")
                }
            }
        )
    }
}

// ============================================
// PAGE 0 : EDIT PANEL
// ============================================
@Composable
fun PageEdit(
    schedule: Schedule,
    viewModel: ScheduleViewModel,
    onSaveSuccess: () -> Unit,
    onBack: () -> Unit = {}
) {
    val isEditFotoTask = schedule.type == "Edit Foto"
    val context = LocalContext.current
    
    var idListing by remember { mutableStateOf(schedule.idListing) }
    var namaMe by remember { mutableStateOf(schedule.namaMe) }
    
    // For Edit Foto Task
    var postingIg by remember { mutableStateOf(schedule.lokasi.contains("[Posting IG: Ya]")) }
    var cleanNotes by remember { mutableStateOf(schedule.lokasi.replace("[Posting IG: Ya] ", "").replace("[Posting IG: Tidak] ", "")) }
    
    var lokasi by remember { mutableStateOf(schedule.lokasi) }
    var tanggal by remember { mutableStateOf(schedule.tanggal) }
    var jam by remember { mutableStateOf(schedule.jam) }
    var staff by remember { mutableStateOf(schedule.staff) }
    var type by remember { mutableStateOf(schedule.type) }
    var status by remember { mutableStateOf(schedule.status) }
    
    var useManualStatus by remember { mutableStateOf(status != "DONE") }
    var isSaving by remember { mutableStateOf(false) }

    // Date picker dialog
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val parsedDate = try {
        dateFormatter.parse(tanggal) ?: Date()
    } catch (e: Exception) {
        Date()
    }
    val dateCalendar = Calendar.getInstance().apply { time = parsedDate }

    val datePickerDialog = android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            tanggal = dateFormatter.format(cal.time)
        },
        dateCalendar.get(Calendar.YEAR),
        dateCalendar.get(Calendar.MONTH),
        dateCalendar.get(Calendar.DAY_OF_MONTH)
    )

    // Time picker dialog
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val parsedTime = try {
        timeFormatter.parse(jam) ?: Date()
    } catch (e: Exception) {
        Date()
    }
    val timeCalendar = Calendar.getInstance().apply { time = parsedTime }

    val timePickerDialog = android.app.TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            jam = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
        },
        timeCalendar.get(Calendar.HOUR_OF_DAY),
        timeCalendar.get(Calendar.MINUTE),
        true
    )

    // Real-time autofill reactive effect inside PageEdit
    LaunchedEffect(idListing) {
        val trimmedId = idListing.trim()
        if (trimmedId.isNotBlank() && trimmedId.length >= 3 && trimmedId != schedule.idListing) {
            val normalizedCurrentId = trimmedId.replace("[^0-9]".toRegex(), "")
            val schedules = viewModel.allSchedules.value
            val tasks = viewModel.allEditFotoTasks.value
            
            val existingSch = schedules.find {
                it.idListing.replace("[^0-9]".toRegex(), "").equals(normalizedCurrentId, ignoreCase = true) && 
                it.namaMe.isNotBlank()
            }
            val existingTsk = tasks.find {
                it.idListing.replace("[^0-9]".toRegex(), "").equals(normalizedCurrentId, ignoreCase = true) && 
                it.namaMe.isNotBlank()
            }
            
            val sheetMe = existingSch?.namaMe?.trim() ?: existingTsk?.namaMe?.trim()
            val sheetLocation = existingSch?.lokasi?.trim() ?: ""
            
            val scrapedAgent = viewModel.agentInfoMap.value[trimmedId] ?: viewModel.agentInfoMap.value[normalizedCurrentId]
            val title = viewModel.listingTitleMap.value[trimmedId] ?: viewModel.listingTitleMap.value[normalizedCurrentId]
            val desc = viewModel.listingDescMap.value[trimmedId] ?: viewModel.listingDescMap.value[normalizedCurrentId]
            
            val finalMe = if (!sheetMe.isNullOrBlank()) sheetMe else scrapedAgent?.name ?: ""
            val finalLoc = if (sheetLocation.isNotBlank()) {
                sheetLocation
            } else if (!title.isNullOrBlank() || !desc.isNullOrBlank()) {
                viewModel.parseLocationFromScraped(title ?: "", desc ?: "")
            } else {
                ""
            }
            
            if (finalMe.isNotBlank()) namaMe = finalMe
            if (finalLoc.isNotBlank()) lokasi = finalLoc
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.EditCalendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isEditFotoTask) "Edit Parameter Task Edit Jadwal RWC" else "Edit Parameter Jadwal Listing",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        if (isEditFotoTask) {
            OutlinedTextField(
                value = idListing,
                onValueChange = { idListing = it },
                label = { Text("ID Listing") },
                placeholder = { Text("Contoh: L-2241") },
                leadingIcon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = namaMe,
                onValueChange = { namaMe = it },
                label = { Text("Nama Marketing Executive (ME)") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            OutlinedTextField(
                value = cleanNotes,
                onValueChange = { cleanNotes = it },
                label = { Text("Keterangan Edit / Catatan") },
                placeholder = { Text("Masukkan catatan edit/revisi foto...") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                    Column {
                        Text("Posting ke Instagram", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        Text("Aktifkan jika postingan sudah siap", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                    }
                }
                Switch(
                    checked = postingIg,
                    onCheckedChange = { postingIg = it }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = tanggal,
                    onValueChange = { tanggal = it },
                    label = { Text("Jadwal Posting (YYYY-MM-DD)") },
                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = jam,
                    onValueChange = { jam = it },
                    label = { Text("Judul Postingan IG") },
                    leadingIcon = { Icon(Icons.Default.Title, contentDescription = null) },
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = staff,
                onValueChange = { staff = it },
                label = { Text("Source (Aplikasi / Sheets)") },
                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // Status for Edit Foto Task
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Status Edit:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("Pending", "Done").forEach { preset ->
                        FilterChip(
                            selected = status == preset,
                            onClick = { status = preset },
                            label = { Text(preset) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        } else {
            // Standard Listing Schedule fields matching Add Form design
            OutlinedTextField(
                value = idListing,
                onValueChange = { idListing = it },
                label = { Text("ID Listing (Opsional)") },
                placeholder = { Text("Contoh: L-2384") },
                leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().testTag("edit_input_id_listing")
            )

            val isListingIdEmpty = idListing.isBlank()
            val hasAutofilledData = idListing.isNotBlank() && (namaMe.isNotBlank() || lokasi.isNotBlank())
            
            val containerColor = if (hasAutofilledData) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            }
            val borderColor = if (hasAutofilledData) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = containerColor),
                border = BorderStroke(1.5.dp, borderColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (hasAutofilledData) Icons.Default.Stars else Icons.Default.FlashOn,
                                contentDescription = null,
                                tint = if (hasAutofilledData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasAutofilledData) "Autofill Cerdas Aktif ✨" else "Deteksi Autofill Otomatis ⚡",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (hasAutofilledData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (hasAutofilledData) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "TERISI OTOMATIS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Nama ME Field
                    OutlinedTextField(
                        value = namaMe,
                        onValueChange = { namaMe = it },
                        label = { Text(if (isListingIdEmpty) "Nama ME / Agen *" else "Nama ME / Agen (Autofilled)") },
                        placeholder = { Text("Contoh: Budi Susanto") },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.Person, 
                                contentDescription = null,
                                tint = if (namaMe.isNotBlank() && !isListingIdEmpty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_input_nama_me"),
                        isError = isListingIdEmpty && namaMe.isBlank(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (hasAutofilledData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = if (namaMe.isNotBlank() && !isListingIdEmpty) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                        )
                    )

                    // Lokasi Field
                    OutlinedTextField(
                        value = lokasi,
                        onValueChange = { lokasi = it },
                        label = { Text(if (isListingIdEmpty) "Lokasi Properti *" else "Lokasi Properti (Autofilled)") },
                        placeholder = { Text("Contoh: Kavling Hijau Cluster B, Serpong") },
                        leadingIcon = { 
                            Icon(
                                imageVector = Icons.Default.LocationOn, 
                                contentDescription = null,
                                tint = if (lokasi.isNotBlank() && !isListingIdEmpty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            ) 
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().testTag("edit_input_lokasi"),
                        isError = isListingIdEmpty && lokasi.isBlank(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (hasAutofilledData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            unfocusedBorderColor = if (lokasi.isNotBlank() && !isListingIdEmpty) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Section 2: Date & Time Picker
            Text(
                text = "Waktu Pemotretan (Wajib)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_pick_date_card")
                        .clickable { datePickerDialog.show() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Tanggal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tanggal,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                OutlinedCard(
                    modifier = Modifier
                        .weight(1f)
                        .testTag("edit_pick_time_card")
                        .clickable { timePickerDialog.show() },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Jam", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = jam,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(Icons.Default.AccessTime, contentDescription = "Pick Time", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Section 3: Staff Picker
            Text(
                text = "Staff Fotografer (Wajib)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            var dropdownExpanded by remember { mutableStateOf(false) }
            val currentStaffLower = staff.lowercase()
            val isDavidSelected = currentStaffLower.contains("david")
            val isRaffaSelected = currentStaffLower.contains("raffa")

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = staff,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Photographer / Videographer *") },
                    placeholder = { Text("Pilih (David / Raffa)") },
                    leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                    trailingIcon = {
                        Icon(
                            imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle Dropdown"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_input_staff"),
                    isError = staff.isBlank()
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { dropdownExpanded = !dropdownExpanded }
                )

                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = isDavidSelected,
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("David")
                            }
                        },
                        onClick = {
                            val isChecked = !isDavidSelected
                            val newList = mutableListOf<String>()
                            if (isChecked) newList.add("David")
                            if (isRaffaSelected) newList.add("Raffa")
                            staff = newList.joinToString(", ")
                        }
                    )

                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = isRaffaSelected,
                                    onCheckedChange = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Raffa")
                            }
                        },
                        onClick = {
                            val isChecked = !isRaffaSelected
                            val newList = mutableListOf<String>()
                            if (isDavidSelected) newList.add("David")
                            if (isChecked) newList.add("Raffa")
                            staff = newList.joinToString(", ")
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Section 4: Type (Dropdown) and Status (2 Opsi) fields
            Text(
                text = "Tipe Kegiatan & Status Jadwal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            var typeDropdownExpanded by remember { mutableStateOf(false) }
            val typeOptions = listOf(
                "Foto",
                "Foto + Video",
                "Foto + Drone"
            )

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = type,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tipe Kegiatan (Dropdown) *") },
                    placeholder = { Text("Pilih Tipe Kegiatan...") },
                    leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                    trailingIcon = {
                        Icon(
                            imageVector = if (typeDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                            contentDescription = "Toggle Dropdown"
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_input_type")
                )

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { typeDropdownExpanded = !typeDropdownExpanded }
                )

                DropdownMenu(
                    expanded = typeDropdownExpanded,
                    onDismissRequest = { typeDropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    typeOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                type = option
                                typeDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Status Jadwal (2 Opsi)",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        status = "DONE"
                        useManualStatus = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (status == "DONE") Color(0xFF22C55E) else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (status == "DONE") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (status == "DONE") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SET DONE", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        useManualStatus = true
                        if (status == "DONE") {
                            status = "Pending"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (useManualStatus) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (useManualStatus) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ketik Manual", fontWeight = FontWeight.Bold)
                }
            }

            if (useManualStatus) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = status,
                    onValueChange = { status = it },
                    label = { Text("Ketik Manual Status Jadwal *") },
                    placeholder = { Text("Contoh: Pending, Garis Tanah, Up Foto, dll.") },
                    leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("edit_input_status")
                )
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Status 'DONE' terpilih (Rata Tengah & Background Hijau di Google Sheets)",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF15803D),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val canSubmit = isEditFotoTask || (
            (idListing.isNotBlank() || (namaMe.isNotBlank() && lokasi.isNotBlank())) &&
            tanggal.isNotBlank() && jam.isNotBlank() && staff.isNotBlank()
        )

        Button(
            onClick = {
                isSaving = true
                if (isEditFotoTask) {
                    val formattedNotes = if (postingIg) "[Posting IG: Ya] ${cleanNotes.trim()}" else "[Posting IG: Tidak] ${cleanNotes.trim()}"
                    val updatedTask = com.example.data.EditFotoTask(
                        id = schedule.id,
                        no = schedule.no,
                        idListing = idListing.trim(),
                        namaMe = namaMe.trim(),
                        postingIg = postingIg,
                        jadwalPosting = tanggal.trim(),
                        editNotes = cleanNotes.trim(),
                        done = (status.trim().lowercase() == "done"),
                        judul = jam.trim(),
                        source = staff.trim(),
                        synced = false
                    )
                    viewModel.updateEditFotoTaskDirectly(updatedTask)
                } else {
                    viewModel.updateScheduleDirectly(
                        schedule = schedule.copy(
                            idListing = idListing.trim(),
                            namaMe = namaMe.trim(),
                            lokasi = lokasi.trim(),
                            tanggal = tanggal.trim(),
                            jam = jam.trim(),
                            staff = staff.trim(),
                            type = type.trim(),
                            status = status.trim(),
                            synced = false
                        ),
                        original = schedule
                    )
                }
                isSaving = false
                onSaveSuccess()
            },
            modifier = Modifier.fillMaxWidth().height(56.dp).testTag("save_edit_button"),
            enabled = canSubmit && !isSaving,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("Menyimpan & Mensinkronisasi...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Simpan Perubahan",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
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

// ============================================
// PAGE 1 : DETAIL HOME
// ============================================
@Composable
fun PageDetailHome(
    schedule: Schedule,
    listingImagesMap: Map<String, String>,
    listingImagesGalleryMap: Map<String, List<String>>,
    agentInfoMap: Map<String, AgentInfo>,
    listingPriceMap: Map<String, String>,
    listingDescMap: Map<String, String>,
    viewModel: com.example.ui.ScheduleViewModel,
    isSold: Boolean = false,
    isScraping: Boolean = false,
    onNavigateToEdit: () -> Unit,
    onNavigateToFollowUp: () -> Unit,
    onBack: () -> Unit = {}
) {
    val cleanId = schedule.idListing.trim()
    val galleryList = if (cleanId.isNotBlank()) listingImagesGalleryMap[cleanId] ?: emptyList() else emptyList()
    val fallbackImg = if (cleanId.isNotBlank()) listingImagesMap[cleanId] else null
    val imagesToDisplay = remember(galleryList, fallbackImg) {
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
    
    val agentInfo = if (cleanId.isNotBlank()) agentInfoMap[cleanId] else null
    
    val finalAgentName = agentInfo?.name?.ifBlank { schedule.namaMe } ?: schedule.namaMe
    val agentNamesList = remember(finalAgentName) {
        com.example.ui.parseMultipleAgentNames(finalAgentName)
    }

    // Retrieve scraped price & description
    val rawPrice = listingPriceMap[cleanId]
    val price = if (rawPrice.isNullOrBlank() && isScraping) {
        "Memuat Harga..."
    } else {
        rawPrice ?: "Rp. Hubungi Agent"
    }

    val rawDesc = listingDescMap[cleanId] ?: ""
    val description = if (rawDesc.isBlank() && isScraping) {
        "Sedang mengambil deskripsi properti lengkap dari website..."
    } else if (rawDesc.isBlank()) {
        "Deskripsi tidak tersedia di websiteListing. Silakan hubungi agent terkait."
    } else {
        rawDesc
            .replace("\r", "")
            .replace("(?m)^[ \t]*\r?\n".toRegex(), "\n")
            .replace(" {2,}".toRegex(), " ")
            .trim()
    }

    val isRent = price.lowercase().contains("sewa") || 
                 price.lowercase().contains("/th") || 
                 price.lowercase().contains("/bln") || 
                 description.lowercase().contains("disewakan") || 
                 description.lowercase().contains("sewa ") || 
                 description.lowercase().contains("rental") ||
                 schedule.type.lowercase().contains("sewa") ||
                 schedule.type.lowercase().contains("rent")
    
    val priceHeaderLabel = if (isRent) "For Rent" else "For Sale"

    val igListingsState by viewModel.weeklyMeetingIgListings.collectAsState()
    val meetingListingsState by viewModel.meetingListings.collectAsState()
    val editFotoTasksState by viewModel.allEditFotoTasks.collectAsState()
    val localYearlyIgHistory by viewModel.yearlyIgPostingHistory.collectAsState()
    val igHistory = remember(cleanId, igListingsState, editFotoTasksState, localYearlyIgHistory) {
        viewModel.getIgPostingHistory(cleanId)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IgPostingInfoCard(history = igHistory)

        if (isSold) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Red),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "SOLD",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "LISTING INI SUDAH SOLD (TERJUAL)",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        // 1. Sliding property photo gallery
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Galeri Foto Properti",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            
            if (imagesToDisplay.isEmpty()) {
                if (isScraping) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), shape = RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            Text(
                                text = "Mengambil foto properti dari website...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f), shape = RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak ada foto properti tersedia.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val carouselImages = imagesToDisplay

                    itemsIndexed(carouselImages) { index, img ->
                        Card(
                            modifier = Modifier
                                .width(180.dp)
                                .height(240.dp),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = img,
                                    contentDescription = "Foto Listing $cleanId - Slide ${index+1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                if (isSold) {
                                    SoldWatermark()
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color.Black.copy(alpha = 0.4f))
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "Slide ${index + 1} dari ${carouselImages.size}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 2. Marketing Executive (ME) Full Width Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (agentNamesList.size > 1) "Marketing Executives (Co-listing)" else "Marketing Executive (ME)",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                
                agentNamesList.forEachIndexed { index, namePart ->
                    if (index > 0) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    }
                    
                    val contact = com.example.ui.findContact(namePart)
                    val meDisplayName = contact?.let { capitalizeName(it.nameKey) } ?: capitalizeName(namePart)
                    
                    var mePhone = contact?.phone ?: com.example.ui.getAgentPhoneByName(namePart)
                    if (mePhone.isBlank()) {
                        mePhone = "085169671344" // default
                    }
                    val meEmail = contact?.email ?: com.example.ui.getAgentEmailByName(namePart)
                    val meIg = contact?.instagram ?: com.example.ui.getAgentInstagramByName(namePart)
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Agent Avatar / Initials
                        Card(
                            modifier = Modifier.size(64.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                val avatars = remember(agentInfo) {
                                    agentInfo?.avatarUrl?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
                                }
                                val meAvatarUrl = if (index < avatars.size) {
                                    avatars[index]
                                } else {
                                    ""
                                }
                                if (meAvatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = meAvatarUrl,
                                        contentDescription = "Foto Agent",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val initials = meDisplayName.split(" ")
                                            .filter { it.isNotBlank() }
                                            .take(2)
                                            .map { it.first().uppercaseChar() }
                                            .joinToString("")
                                        Text(
                                            text = initials.ifEmpty { "ME" },
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        // Agent Info Column
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = meDisplayName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // WhatsApp Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Call,
                                    contentDescription = "WhatsApp",
                                    tint = Color(0xFF25D366),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = mePhone,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Email Row
                            if (meEmail.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = "Email",
                                        tint = Color(0xFFEA4335),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = meEmail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Instagram Row
                            val igDisplay = if (meIg.isNotBlank() && !meIg.startsWith("@")) "@$meIg" else meIg
                            if (igDisplay.isNotBlank()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AlternateEmail,
                                        contentDescription = "Instagram",
                                        tint = Color(0xFFE1306C),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = igDisplay,
                                        style = MaterialTheme.typography.bodySmall,
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

        // 3. Pricing Card (Full Width)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = priceHeaderLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = price,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // 4. Deskripsi Card (Full Width - Text displays perfectly with wrap)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Deskripsi Properti",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Removed guidance slide card indicator as requested by user - bottom navigation is already fixed below.

        // 4. Details parameters info card
        if (schedule.type == "Meeting Result") {
            val meetingListing = remember(cleanId, igListingsState, meetingListingsState) {
                meetingListingsState.find { it.idListing.trim().equals(cleanId, ignoreCase = true) }
                    ?: igListingsState.find { it.idListing.trim().equals(cleanId, ignoreCase = true) }
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detail Hasil Meeting",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val isPosted = meetingListing?.postingIg?.lowercase()?.contains("done") == true || 
                                       meetingListing?.postingIg?.lowercase()?.contains("ya") == true || 
                                       meetingListing?.postingIg?.lowercase()?.contains("true") == true ||
                                       meetingListing?.postingIg == "1"
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isPosted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (isPosted) "Posting IG: Done" else "Posting IG: Pending",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isPosted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    val dispKeterangan = meetingListing?.keterangan?.ifBlank { schedule.lokasi } ?: schedule.lokasi
                    val dispCatatan = meetingListing?.catatan?.ifBlank { "-" } ?: "-"
                    val dispJadwal = meetingListing?.jadwalPosting?.ifBlank { "-" } ?: "-"
                    
                    InfoRow(label = "ID Listing", value = cleanId.ifBlank { "Unassigned" })
                    InfoRow(label = "Nama ME", value = meetingListing?.namaMe?.ifBlank { schedule.namaMe } ?: schedule.namaMe)
                    InfoRow(label = "Keterangan", value = dispKeterangan)
                    InfoRow(label = "Jadwal Posting IG", value = dispJadwal)
                    InfoRow(label = "Catatan Hasil Meeting", value = dispCatatan)
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Informasi Kegiatan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val isDone = schedule.status.lowercase().contains("done") || schedule.type.lowercase().contains("done")
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isDone) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    shape = RoundedCornerShape(100.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = schedule.status,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (isDone) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // Fields
                    if (schedule.type == "Edit Foto") {
                        val isDone = schedule.status == "Done"
                        val notesPart = schedule.lokasi.replace("[Posting IG: Ya] ", "").replace("[Posting IG: Tidak] ", "")
                        val isPostingIg = schedule.lokasi.contains("[Posting IG: Ya]")

                        InfoRow(label = "ID Listing", value = cleanId.ifBlank { "Unassigned" })
                        InfoRow(label = "Kategori / Tipe", value = "Edit Jadwal Posting IG RWC")
                        InfoRow(label = "Nama ME", value = schedule.namaMe)
                        InfoRow(label = "Keterangan Edit", value = notesPart.ifBlank { "-" })
                        InfoRow(label = "Jadwal Posting IG", value = schedule.tanggal)
                        InfoRow(label = "Judul Postingan IG", value = schedule.jam.ifBlank { "-" })
                        InfoRow(label = "Source", value = schedule.staff.ifBlank { "-" })
                        InfoRow(label = "Posting IG", value = if (isPostingIg) "Ceklis Done (Ya)" else "Belum Posting")
                        InfoRow(label = "Status Edit", value = if (isDone) "Ceklis Done (Selesai)" else "Pending")
                    } else {
                        InfoRow(label = "ID Listing", value = cleanId.ifBlank { "Unassigned" })
                        InfoRow(label = "Kategori / Tipe", value = schedule.type)
                        InfoRow(label = "Lokasi", value = schedule.lokasi)
                        InfoRow(label = "Tanggal Sesi", value = formatIndonesianDate(schedule.tanggal))
                        InfoRow(label = "Waktu Sesi", value = formatTwelveHourTime(schedule.jam))
                        InfoRow(label = "Runner (Staff)", value = schedule.staff)
                    }
                    InfoRow(label = "Sinkronisasi Sheets", value = if (schedule.synced) "Sudah Sinkron" else "Tertunda (Sinyal/Offline)")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Helper info row component
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.width(100.dp)
        )
        val isEmptyValue = value.isBlank() || value.trim() == "-"
        Text(
            text = if (isEmptyValue) "(belum di input)" else value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isEmptyValue) FontWeight.Normal else FontWeight.SemiBold
            ),
            color = if (isEmptyValue) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

// ============================================
// PAGE 2 : FOLLOW UP PANEL
// ============================================
@Composable
fun PageFollowUp(
    schedule: Schedule,
    agentInfoMap: Map<String, AgentInfo>,
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val cleanId = schedule.idListing.trim()
    val agentInfo = if (cleanId.isNotBlank()) agentInfoMap[cleanId] else null
    
    val finalAgentName = agentInfo?.name?.ifBlank { schedule.namaMe } ?: schedule.namaMe
    val agentNamesList = remember(finalAgentName) {
        com.example.ui.parseMultipleAgentNames(finalAgentName)
    }
    
    var selectedAgentIndex by remember { mutableStateOf(0) }
    
    val currentAgentName = if (agentNamesList.isNotEmpty() && selectedAgentIndex < agentNamesList.size) {
        agentNamesList[selectedAgentIndex]
    } else {
        finalAgentName
    }
    
    val currentContact = remember(currentAgentName) {
        com.example.ui.findContact(currentAgentName)
    }
    
    val currentAgentPhone = remember(currentAgentName, currentContact) {
        val phone = currentContact?.phone ?: com.example.ui.getAgentPhoneByName(currentAgentName)
        if (phone.isBlank()) "085169671344" else phone
    }
    
    var cleanPhone = currentAgentPhone.replace("[^\\d]".toRegex(), "")
    if (cleanPhone.startsWith("0")) {
        cleanPhone = "62" + cleanPhone.substring(1)
    }
    if (cleanPhone.isBlank()) {
        cleanPhone = "6285169671344" // Default Office line
    }

    val templateMessage = remember(schedule, currentAgentName) {
        WhatsAppFormatter.generateWhatsAppFollowUpMessage(context, schedule, currentAgentName)
    }
    
    var messageText by remember(templateMessage) { mutableStateOf(templateMessage) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (agentNamesList.size > 1) {
            Text(
                text = "Pilih Agent Co-listing Penerima:",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                agentNamesList.forEachIndexed { idx, name ->
                    val contactObj = com.example.ui.findContact(name)
                    val formattedName = contactObj?.let { capitalizeName(it.nameKey) } ?: capitalizeName(name)
                    val phoneStr = contactObj?.phone ?: com.example.ui.getAgentPhoneByName(name)
                    
                    val isSelected = selectedAgentIndex == idx
                    
                    Card(
                        onClick = { selectedAgentIndex = idx },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = isSelected,
                                onClick = { selectedAgentIndex = idx }
                            )
                            Column {
                                Text(
                                    text = formattedName,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold)
                                )
                                Text(
                                    text = "WhatsApp: ${phoneStr.ifBlank { "Tidak ada nomor" }}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        imageVector = Icons.Default.ContactPhone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Kontak Penerima",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                val contactObj = com.example.ui.findContact(currentAgentName)
                val currentAgentDisplayName = contactObj?.let { capitalizeName(it.nameKey) } ?: capitalizeName(currentAgentName)
                Text(
                    text = "Penerima: $currentAgentDisplayName",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                )
                Text(
                    text = "No. WhatsApp: $currentAgentPhone",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }

        // Message text editor
        OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            label = { Text("Pesan Follow Up (Siap Dikirim)") },
            modifier = Modifier.fillMaxWidth().height(220.dp),
            shape = RoundedCornerShape(16.dp),
            textStyle = MaterialTheme.typography.bodySmall
        )
        
        Text(
            text = "Pilih jenis aplikasi WhatsApp untuk membuka chat personal:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth()
        )

        // WhatsApp trigger handles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageText)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.whatsapp")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(fallbackIntent)
                        } catch (e2: Exception) {
                            Toast.makeText(context, "Gagal meluncurkan aplikasi WhatsApp", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = Color.White)
                    Text("WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = {
                    val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(messageText)}")
                    val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                        setPackage("com.whatsapp.w4b")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        try {
                            val fallbackIntent = Intent(Intent.ACTION_VIEW, uri)
                            context.startActivity(fallbackIntent)
                        } catch (e2: Exception) {
                            Toast.makeText(context, "Gagal meluncurkan aplikasi WhatsApp Business", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier.weight(1f).height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF075E54))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.SendToMobile, contentDescription = null, tint = Color.White)
                    Text("WA Business", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Helper conversions are loaded from DashboardScreen package namespace
