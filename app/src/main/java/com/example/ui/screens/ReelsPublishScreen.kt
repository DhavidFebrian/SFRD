package com.example.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.graphics.Bitmap
import android.widget.Toast
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.ReelItem
import com.example.ui.ScheduleViewModel
import com.example.ui.SyncState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReelsPublishScreen(
    viewModel: ScheduleViewModel,
    isSearchExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    val reels by viewModel.reelsListings.collectAsState()
    val syncState by viewModel.reelsSyncStatus.collectAsState()
    val savedCaptions by viewModel.savedCaptionsMap.collectAsState()
    val listingDescriptions by viewModel.listingDescMap.collectAsState()
    val listingPrices by viewModel.listingPriceMap.collectAsState()
    val listingTitles by viewModel.listingTitleMap.collectAsState()
    var search by remember { mutableStateOf("") }
    var selectedReel by remember { mutableStateOf<ReelItem?>(null) }
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { viewModel.fetchReels() }
    LaunchedEffect(reels) {
        reels.forEach { reel ->
            viewModel.fetchListingImageIfNeeded(reel.idListing, reel.namaMe)
        }
    }

    val filtered = remember(reels, search) {
        reels.filter {
            search.isBlank() || it.idListing.contains(search, true) ||
                it.namaMe.contains(search, true) || it.catatan.contains(search, true)
        }
    }
    val unpublished = remember(filtered) {
        filtered.filter { !it.postingIg }.sortedWith(compareBy<ReelItem> {
            parsePublishJadwalPostingDateToEpoch(it.jadwalPosting)
        }.thenBy { it.no })
    }
    val published = remember(filtered) {
        filtered.filter { it.postingIg }.sortedWith(compareBy<ReelItem> {
            parsePublishJadwalPostingDateToEpoch(it.jadwalPosting)
        }.thenBy { it.no })
    }

    Column(modifier.fillMaxSize()) {
        AnimatedVisibility(isSearchExpanded) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                placeholder = { Text("Cari ID listing, Nama ME, atau catatan") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (search.isNotBlank()) IconButton(onClick = { search = "" }) {
                        Icon(Icons.Default.Close, "Hapus pencarian")
                    }
                },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        if (syncState is SyncState.Error) {
            Surface(color = MaterialTheme.colorScheme.errorContainer) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text((syncState as SyncState.Error).message, Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    TextButton(onClick = { viewModel.fetchReels(true) }) { Text("Coba lagi") }
                }
            }
        }

        PrimaryTabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.height(42.dp)) {
            listOf("Unpublished (${unpublished.size})", "Published (${published.size})").forEachIndexed { index, label ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text = { Text(label, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1) }
                )
            }
        }

        HorizontalPager(pagerState, Modifier.fillMaxWidth().weight(1f)) { page ->
            val items = if (page == 0) unpublished else published
            if (items.isEmpty() && syncState !is SyncState.Loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.VideoLibrary, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                        Text(if (page == 0) "Tidak ada Reels unpublished" else "Belum ada Reels published", fontWeight = FontWeight.Bold)
                    }
                }
            } else if (syncState is SyncState.Loading && items.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(items, key = { "reel_${it.row}_${it.idListing}" }) { reel ->
                        val generatedCaption = buildReelCaption(reel, listingDescriptions, listingPrices, listingTitles)
                        ReelCard(
                            reel = reel,
                            caption = savedCaptions[reel.idListing].takeUnless { it.isNullOrBlank() } ?: generatedCaption,
                            onClick = { selectedReel = reel }
                        )
                    }
                }
            }
        }
    }

    selectedReel?.let { reel ->
        ReelPreviewDialog(reel, viewModel, onDismiss = { selectedReel = null })
    }
}

@Composable
private fun ReelCard(reel: ReelItem, caption: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(4f / 5f).background(MaterialTheme.colorScheme.surfaceVariant)) {
                AsyncImage(
                    model = reel.thumbnailUrl,
                    contentDescription = "Thumbnail Reels ${reel.idListing}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Surface(
                    modifier = Modifier.align(Alignment.Center),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = .64f)
                ) { Icon(Icons.Default.PlayArrow, "Putar video", Modifier.padding(10.dp).size(24.dp), tint = Color.White) }
                Surface(
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = if (reel.postingIg) Color(0xFF16803A) else Color(0xFFE65100)
                ) {
                    Text(
                        if (reel.postingIg) "Published" else "Unpublished",
                        Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.ExtraBold
                    )
                }
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    shape = RoundedCornerShape(6.dp), color = Color(0xFFE1306C)
                ) { Text("REELS", Modifier.padding(horizontal = 6.dp, vertical = 3.dp), color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold) }
            }
            Column(Modifier.padding(9.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("#${reel.no} · ID ${reel.idListing}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CalendarToday, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        reel.jadwalPosting.takeIf { it.isNotBlank() }?.let(::formatPublishJadwalPostingDate) ?: "Belum dijadwalkan",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, Modifier.size(12.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(Modifier.width(4.dp))
                    Text(reel.namaMe.ifBlank { "Nama ME belum diisi" }, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(
                    caption.ifBlank { "Caption listing sedang dimuat…" },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 4, overflow = TextOverflow.Ellipsis
                )
                if (reel.catatan.isNotBlank()) Text(
                    reel.catatan,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReelPreviewDialog(reel: ReelItem, viewModel: ScheduleViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val captions by viewModel.savedCaptionsMap.collectAsState()
    val listingDescriptions by viewModel.listingDescMap.collectAsState()
    val listingPrices by viewModel.listingPriceMap.collectAsState()
    val listingTitles by viewModel.listingTitleMap.collectAsState()
    val generatedCaption = remember(reel, listingDescriptions, listingPrices, listingTitles) {
        buildReelCaption(reel, listingDescriptions, listingPrices, listingTitles)
    }
    var caption by remember(reel.idListing) { mutableStateOf("") }
    var captionEdited by remember(reel.idListing) { mutableStateOf(false) }
    var previewLoading by remember(reel.idListing) { mutableStateOf(true) }
    var playbackFailed by remember(reel.idListing) { mutableStateOf(false) }
    var retryKey by remember(reel.idListing) { mutableIntStateOf(0) }
    var webView by remember(reel.idListing) { mutableStateOf<WebView?>(null) }
    DisposableEffect(reel.idListing) {
        onDispose {
            webView?.run { stopLoading(); loadUrl("about:blank"); destroy() }
            webView = null
        }
    }
    LaunchedEffect(reel.idListing, captions, generatedCaption) {
        val resolved = captions[reel.idListing].takeUnless { it.isNullOrBlank() } ?: generatedCaption
        if (!captionEdited && resolved.isNotBlank()) caption = resolved
    }
    LaunchedEffect(reel.idListing) {
        viewModel.fetchListingImageIfNeeded(reel.idListing, reel.namaMe)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Reels · ${reel.idListing}", fontWeight = FontWeight.Bold)
                            Text(reel.namaMe, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } },
                    actions = {
                        IconButton(onClick = { openDriveLink(context, reel.driveLink) }) { Icon(Icons.Default.OpenInNew, "Buka Google Drive") }
                        IconButton(onClick = { enqueueReelDownload(context, reel) }) { Icon(Icons.Default.Download, "Download video") }
                    }
                )

                Box(
                    Modifier.fillMaxWidth().weight(1f).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (reel.previewUrl.isNotBlank()) {
                        key(retryKey) {
                            AndroidView(
                                factory = {
                                    WebView(context).apply {
                                        webView = this
                                        setBackgroundColor(android.graphics.Color.BLACK)
                                        settings.javaScriptEnabled = true
                                        settings.domStorageEnabled = true
                                        settings.mediaPlaybackRequiresUserGesture = false
                                        settings.loadsImagesAutomatically = true
                                        webChromeClient = WebChromeClient()
                                        webViewClient = object : WebViewClient() {
                                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                                previewLoading = true
                                                playbackFailed = false
                                            }

                                            override fun onPageFinished(view: WebView?, url: String?) {
                                                previewLoading = false
                                                playbackFailed = false
                                            }

                                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                                if (request?.isForMainFrame == true) {
                                                    previewLoading = false
                                                    playbackFailed = true
                                                }
                                            }
                                        }
                                        loadUrl(reel.previewUrl)
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    if (previewLoading || playbackFailed) {
                        AsyncImage(
                            model = reel.thumbnailUrl,
                            contentDescription = "Preview Reels ${reel.idListing}",
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (playbackFailed) {
                                FilledIconButton(
                                    onClick = { playbackFailed = false; previewLoading = true; retryKey++ },
                                    modifier = Modifier.size(64.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White.copy(alpha = .92f))
                                ) { Icon(Icons.Default.Refresh, "Coba lagi", Modifier.size(36.dp), tint = Color.Black) }
                            }
                            if (previewLoading && !playbackFailed) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = .68f)) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = Color.White)
                                        Spacer(Modifier.width(7.dp))
                                        Text("Memuat player video…", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            } else if (playbackFailed) {
                                Text("Preview gagal dimuat · ketuk untuk mencoba lagi", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("ID Listing ${reel.idListing}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            ReelInfoRow("Marketing (ME)", reel.namaMe.ifBlank { "N/A" })
                            ReelInfoRow("Status Posting IG", if (reel.postingIg) "Sudah Diposting" else "Belum Diposting")
                            ReelInfoRow("Jadwal Posting", formatPublishJadwalPostingDate(reel.jadwalPosting))
                            if (reel.catatan.isNotBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text("Catatan", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(reel.catatan, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Video Reels", Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        FilledTonalButton(onClick = { enqueueReelDownload(context, reel) }) {
                            Icon(Icons.Default.Download, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Download")
                        }
                    }
                    OutlinedTextField(
                        value = caption,
                        onValueChange = { caption = it; captionEdited = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp, max = 150.dp),
                        label = { Text("Caption Instagram") },
                        placeholder = { Text("Tulis caption Reels...") }
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { clipboard.setText(AnnotatedString(caption)); Toast.makeText(context, "Caption disalin", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.weight(1f), enabled = caption.isNotBlank()
                        ) { Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Salin") }
                        Button(
                            onClick = {
                                viewModel.saveCaptionToDatabase(reel.idListing, caption) { _, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                }
                            }, modifier = Modifier.weight(1f)
                        ) { Icon(Icons.Default.Save, null, Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)); Text("Simpan") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReelInfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

private fun buildReelCaption(
    reel: ReelItem,
    descriptions: Map<String, String>,
    prices: Map<String, String>,
    titles: Map<String, String>
): String {
    val description = descriptions[reel.idListing].orEmpty()
    val price = prices[reel.idListing].orEmpty()
    val title = titles[reel.idListing].orEmpty()
    if (description.isBlank() && price.isBlank() && title.isBlank()) return ""
    return buildInstagramCaption(
        rawDesc = description,
        idListing = reel.idListing,
        rawPrice = price,
        namaMe = reel.namaMe,
        judulTask = title,
        scrapedTitle = title
    )
}

private fun enqueueReelDownload(context: Context, reel: ReelItem) {
    if (reel.videoUrl.isBlank()) {
        Toast.makeText(context, "Link video tidak valid", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val request = DownloadManager.Request(Uri.parse(reel.videoUrl))
            .setTitle("Reels ${reel.idListing}")
            .setDescription("Mengunduh video ${reel.namaMe}")
            .setMimeType("video/mp4")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
        val fileName = "Reels-${reel.idListing.ifBlank { reel.no.toString() }}.mp4"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        } else {
            request.setDestinationInExternalFilesDir(context, Environment.DIRECTORY_MOVIES, fileName)
        }
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        Toast.makeText(context, "Download dimulai", Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, "Gagal download: ${it.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

private fun openDriveLink(context: Context, link: String) {
    if (link.isBlank()) return
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link))) }
}
