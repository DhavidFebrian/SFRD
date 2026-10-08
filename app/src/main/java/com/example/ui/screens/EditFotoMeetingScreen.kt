package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
    val highlightedIds by viewModel.highlightedMeListingIds.collectAsState()
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

    LaunchedEffect(currentMonth) { viewModel.fetchWeeklyMeetingIgListings(currentMonth) }
    val pending = remember(listings, highlightedIds, search) {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        listings.filter { item ->
            val millis = runCatching { parser.parse(normalizeDate(item.date))?.time }.getOrNull()
            millis != null && millis >= weekBounds.first && millis < weekBounds.second &&
                item.keterangan.trim().equals("IG", true) && !item.meHighlighted &&
                !highlightedIds.contains(item.idListing.trim()) &&
                (search.isBlank() || item.idListing.contains(search, true) || item.namaMe.contains(search, true) || item.catatan.contains(search, true))
        }.sortedWith(compareBy<MeetingListing> { normalizeDate(it.date) }.thenBy { it.no })
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
                    itemsIndexed(
                        pending,
                        key = { index, listing ->
                            "${normalizeDate(listing.date)}_${listing.row}_${listing.colIndex}_${listing.no}_${listing.idListing}_$index"
                        }
                    ) { _, listing ->
                        LaunchedEffect(listing.idListing) {
                            if (listing.idListing.isNotBlank()) viewModel.fetchListingImageIfNeeded(listing.idListing, listing.namaMe)
                        }
                        EditFotoIgCard(
                            listing, images[listing.idListing.trim()], titles[listing.idListing.trim()],
                            onClick = { selectedListing = listing },
                            onDone = {
                                viewModel.updateWeeklyMeetingMeHighlight(
                                    viewModel.getWeeklyMeetingSheetNameForMonth(currentMonth), listing.date, listing.row,
                                    listing.colIndex, listing.idListing, true
                                ) { _, msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                            }
                        )
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
        shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1.08f).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (imageUrl != null) AsyncImage(imageUrl, "Foto listing ${listing.idListing}", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Default.Image, null, Modifier.size(38.dp), tint = MaterialTheme.colorScheme.outline) }
                Surface(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(bottomEnd = 12.dp)) {
                    Text("IG", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                }
            }
            Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(listing.namaMe.ifBlank { "Nama ME belum diisi" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("ID ${listing.idListing.ifBlank { "-" }}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(title?.takeIf { it.isNotBlank() } ?: listing.catatan.ifBlank { "Buka untuk melihat preview" }, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                FilledTonalButton(onClick = onDone, modifier = Modifier.fillMaxWidth().heightIn(min = 42.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                    Icon(Icons.Default.Done, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Sudah Edit", maxLines = 1)
                }
            }
        }
    }
}
