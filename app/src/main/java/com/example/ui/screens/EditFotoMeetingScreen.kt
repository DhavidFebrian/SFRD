package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.EditFotoTask
import com.example.data.normalizeDate
import com.example.network.MeetingListing
import com.example.ui.ScheduleViewModel
import com.example.ui.SyncState
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditFotoMeetingScreen(
    viewModel: ScheduleViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToChat: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    showTopBar: Boolean = true
) {
    val listings by viewModel.weeklyMeetingIgListings.collectAsState()
    val syncState by viewModel.weeklyMeetingIgSyncStatus.collectAsState()
    val images by viewModel.listingImagesMap.collectAsState()
    val gallery by viewModel.listingImagesGalleryMap.collectAsState()
    val descriptions by viewModel.listingDescMap.collectAsState()
    val prices by viewModel.listingPriceMap.collectAsState()
    val titles by viewModel.listingTitleMap.collectAsState()
    val context = LocalContext.current
    var search by rememberSaveable { mutableStateOf("") }
    var selectedListing by remember { mutableStateOf<MeetingListing?>(null) }
    val now = remember { Calendar.getInstance() }
    val currentMonth = remember {
        val names = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        "${names[now.get(Calendar.MONTH)]} ${now.get(Calendar.YEAR)}"
    }
    val weekBounds = remember {
        val start = now.clone() as Calendar
        start.firstDayOfWeek = Calendar.MONDAY
        start.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
        val end = start.clone() as Calendar
        end.add(Calendar.DAY_OF_MONTH, 7)
        start.timeInMillis to end.timeInMillis
    }

    LaunchedEffect(currentMonth) {
        // Warna sel spreadsheet adalah sumber utama status edit, jadi selalu ambil
        // format terbaru ketika halaman dibuka dan jangan mengandalkan cache lokal.
        viewModel.fetchWeeklyMeetingIgListings(currentMonth, forceRefresh = true)
    }
    val pending = remember(listings, search) {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        listings.filter { item ->
            val millis = runCatching { parser.parse(normalizeDate(item.date))?.time }.getOrNull()
            millis != null && millis >= weekBounds.first && millis < weekBounds.second &&
                item.keterangan.trim().equals("IG", true) && !item.meHighlighted &&
                (search.isBlank() || item.idListing.contains(search, true) || item.namaMe.contains(search, true) || item.catatan.contains(search, true))
        }.sortedWith(compareBy<MeetingListing> { normalizeDate(it.date) }.thenBy { it.no })
    }
    val postingGroups = remember(pending) {
        pending.groupBy { listing ->
            normalizeDate(listing.jadwalPosting).ifBlank {
                listing.jadwalPosting.trim().ifBlank { "BELUM_DIJADWALKAN" }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showTopBar) TopAppBar(
                title = { Text("Edit Foto", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Default.Menu, "Buka menu") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search, onValueChange = { search = it }, modifier = Modifier.weight(1f),
                    placeholder = { Text("Cari Nama ME atau ID...") }, leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true, shape = RoundedCornerShape(14.dp)
                )
                IconButton(onClick = { viewModel.fetchWeeklyMeetingIgListings(currentMonth, true) }) {
                    if (syncState is SyncState.Loading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.Refresh, "Refresh")
                }
            }
            Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = .45f)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FilterAlt, null, Modifier.size(17.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(7.dp))
                    Text("${pending.size} antrean · Meeting minggu ini · IG · Belum diedit", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            if (pending.isEmpty() && syncState !is SyncState.Loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.TaskAlt, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                        Text("Antrean Edit Foto sudah bersih", fontWeight = FontWeight.Bold)
                        Text("Data biru/teks putih tidak ditampilkan.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2), contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    postingGroups.forEach { (dateKey, dateItems) ->
                        item(
                            key = "edit_date_header_$dateKey",
                            span = { GridItemSpan(maxLineSpan) }
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(
                                                com.example.ui.components.GlowNavSelectedBlue.copy(alpha = .82f),
                                                com.example.ui.components.GlowNavSelectedViolet.copy(alpha = .76f)
                                            )
                                        ),
                                        RoundedCornerShape(9.dp)
                                    )
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CalendarToday, null, Modifier.size(14.dp), tint = Color.White)
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (dateKey == "BELUM_DIJADWALKAN") {
                                            "Belum dijadwalkan"
                                        } else {
                                            formatPublishJadwalPostingDate(dateKey).ifBlank { "Belum dijadwalkan" }
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Surface(color = Color.White.copy(alpha = .18f), shape = RoundedCornerShape(50)) {
                                        Text(
                                            "${dateItems.size} postingan",
                                            Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        items(
                            dateItems,
                            key = { listing ->
                                "${normalizeDate(listing.date)}_${listing.row}_${listing.colIndex}_${listing.no}_${listing.idListing}"
                            }
                        ) { listing ->
                            LaunchedEffect(listing.idListing) {
                                if (listing.idListing.isNotBlank()) viewModel.fetchListingImageIfNeeded(listing.idListing, listing.namaMe)
                            }
                            EditFotoIgCard(
                                listing, images[listing.idListing.trim()], titles[listing.idListing.trim()],
                                onClick = { selectedListing = listing },
                                onDone = {
                                    viewModel.updateWeeklyMeetingMeHighlight(
                                        viewModel.getWeeklyMeetingSheetNameForMonth(currentMonth), listing.date,
                                        listing.row.takeIf { it > 0 } ?: listing.no,
                                        listing.colIndex, listing.idListing, true
                                    ) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    selectedListing?.let { listing ->
        val task = remember(listing, titles) {
            EditFotoTask(
                id = listing.no, no = listing.no, idListing = listing.idListing.trim(), namaMe = listing.namaMe.trim(),
                postingIg = false, jadwalPosting = listing.jadwalPosting, editNotes = listing.catatan, done = false,
                judul = titles[listing.idListing.trim()] ?: listing.catatan,
                source = "${listing.date}|||${listing.colIndex}"
            )
        }
        InstagramPostMockupScreen(
            task, images, gallery, descriptions, prices, titles, listings, viewModel,
            onDismiss = { selectedListing = null }, onViewDetails = { selectedListing = null }
        )
    }
}

@Composable
private fun EditFotoIgCard(
    listing: MeetingListing,
    imageUrl: String?,
    title: String?,
    onClick: () -> Unit,
    onDone: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = .25f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().height(120.dp).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .5f))) {
                if (imageUrl != null) AsyncImage(imageUrl, "Foto listing ${listing.idListing}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.outline) }
                Surface(
                    color = Color(0xFF2563EB).copy(alpha = .94f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp)
                ) {
                    Row(Modifier.padding(horizontal = 6.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoFixHigh, null, Modifier.size(10.dp), tint = Color.White)
                        Spacer(Modifier.width(3.dp))
                        Text("Belum Edit", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Surface(
                    color = Color(0xFFE1306C).copy(alpha = .92f),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                ) {
                    Text("IG", Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                }
                Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(3.dp).background(Color(0xFF2563EB)))
            }
            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${listing.row.takeIf { it > 0 } ?: listing.no}", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, modifier = Modifier.background(Color(0xFF2563EB), RoundedCornerShape(3.dp)).padding(horizontal = 4.dp, vertical = 1.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(listing.idListing.ifBlank { "Manual" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(title?.takeIf { it.isNotBlank() } ?: listing.catatan.ifBlank { "Buka untuk melihat preview" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, Modifier.size(10.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(3.dp))
                    Text(listing.namaMe.ifBlank { "Nama ME belum diisi" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(10.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(3.dp))
                    Text(formatPublishJadwalPostingDate(listing.jadwalPosting).ifBlank { "Belum dijadwalkan" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
                FilledTonalButton(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Default.Done, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Sudah Edit", maxLines = 1)
                }
            }
        }
    }
}
