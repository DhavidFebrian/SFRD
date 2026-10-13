package com.example.ui.screens

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import coil.compose.AsyncImage
import com.example.data.ReelItem
import com.example.ui.ScheduleViewModel
import com.example.ui.SyncState
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.io.IOException

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

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
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
    var playRequested by remember(reel.idListing) { mutableStateOf(false) }
    var previewLoading by remember(reel.idListing) { mutableStateOf(true) }
    var playbackFailed by remember(reel.idListing) { mutableStateOf(false) }
    var playbackErrorMessage by remember(reel.idListing) { mutableStateOf<String?>(null) }
    var useDrivePreview by remember(reel.idListing) { mutableStateOf(false) }
    var driveWebView by remember(reel.idListing) { mutableStateOf<WebView?>(null) }
    var showFullPreview by remember(reel.idListing) { mutableStateOf(false) }
    DisposableEffect(reel.idListing) {
        onDispose {
            driveWebView?.apply { stopLoading(); loadUrl("about:blank"); destroy() }
            driveWebView = null
        }
    }
    val mediaSource = remember(reel.videoUrl) {
        val client = OkHttpClient.Builder().addNetworkInterceptor { chain ->
            val original = chain.request()
            val request = if (original.header("Range") == null) {
                original.newBuilder().header("Range", "bytes=0-").build()
            } else original
            chain.proceed(request).also { response ->
                if (response.header("Content-Type")?.startsWith("text/html") == true) {
                    val html = response.peekBody(8_192).string()
                    if (html.contains("Quota exceeded", ignoreCase = true) || html.contains("Too many users", ignoreCase = true)) {
                        response.close()
                        throw IOException("DRIVE_QUOTA_EXCEEDED")
                    }
                }
            }
        }.build()
        val httpFactory = OkHttpDataSource.Factory(client)
            .setUserAgent("Mozilla/5.0 (Linux; Android ${Build.VERSION.RELEASE}) AppleWebKit/537.36 Chrome/140 Mobile Safari/537.36")
            .setDefaultRequestProperties(mapOf(
                "Accept" to "video/mp4,video/*;q=0.9,*/*;q=0.8",
                "Referer" to "https://drive.google.com/"
            ))
        val item = MediaItem.Builder().setUri(reel.videoUrl).setMimeType(MimeTypes.VIDEO_MP4).build()
        ProgressiveMediaSource.Factory(DefaultDataSource.Factory(context, httpFactory)).createMediaSource(item)
    }
    val player = remember(reel.videoUrl) {
        val loadControl = DefaultLoadControl.Builder().setBufferDurationsMs(2_000, 15_000, 500, 1_000).build()
        ExoPlayer.Builder(context).setLoadControl(loadControl).build().apply {
            setMediaSource(mediaSource)
            prepare()
        }
    }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                previewLoading = state == Player.STATE_BUFFERING || state == Player.STATE_IDLE
                if (state == Player.STATE_READY) playbackFailed = false
            }
            override fun onPlayerError(error: PlaybackException) {
                previewLoading = false
                playbackFailed = true
                if (reel.previewUrl.isNotBlank()) {
                    useDrivePreview = true
                    player.stop()
                }
                playbackErrorMessage = if (generateSequence<Throwable>(error) { it.cause }
                        .any { it.message?.contains("DRIVE_QUOTA_EXCEEDED") == true }) {
                    "Kuota video Google Drive sedang penuh · coba lagi nanti atau buka Drive"
                } else {
                    "Preview gagal dimuat · ketuk untuk mencoba lagi"
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener); player.release() }
    }
    LaunchedEffect(reel.idListing, captions, generatedCaption) {
        val resolved = captions[reel.idListing].takeUnless { it.isNullOrBlank() } ?: generatedCaption
        if (!captionEdited && resolved.isNotBlank()) caption = resolved
    }
    LaunchedEffect(reel.idListing) {
        viewModel.fetchListingImageIfNeeded(reel.idListing, reel.namaMe)
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Color.Black) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                TopAppBar(
                    title = {
                        Column {
                            Text("Posts · Reels ${reel.idListing}", fontWeight = FontWeight.Bold, color = Color.White)
                            Text(reel.namaMe, style = MaterialTheme.typography.labelSmall, color = Color(0xFF58A6FF))
                        }
                    },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } },
                    actions = {
                        IconButton(onClick = { openDriveLink(context, reel.driveLink) }) { Icon(Icons.Default.OpenInNew, "Buka Google Drive", tint = Color.White) }
                        IconButton(onClick = { enqueueReelDownload(context, reel) }) { Icon(Icons.Default.Download, "Download video", tint = Color.White) }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
                )

                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (useDrivePreview) {
                        AndroidView(
                            factory = {
                                WebView(context).apply {
                                    driveWebView = this
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.mediaPlaybackRequiresUserGesture = false
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    // Drive's mobile touch controls cover the frame with a dark scrim.
                                    // Its desktop controls use a compact bar and leave the video unobscured.
                                    settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
                                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                    webChromeClient = WebChromeClient()
                                    webViewClient = WebViewClient()
                                    if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                                        WebViewCompat.addDocumentStartJavaScript(
                                            this, REEL_PLAYER_CONTROLS_SCRIPT,
                                            setOf("https://drive.google.com")
                                        )
                                    }
                                    loadUrl(reel.previewUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (reel.videoUrl.isNotBlank()) {
                        AndroidView(
                            factory = {
                                PlayerView(context).apply {
                                    this.player = player
                                    useController = true
                                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                                    setBackgroundColor(android.graphics.Color.BLACK)
                                }
                            },
                            update = { it.player = player },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    if (!useDrivePreview && (!playRequested || playbackFailed)) {
                        AsyncImage(
                            model = reel.thumbnailUrl,
                            contentDescription = "Preview Reels ${reel.idListing}",
                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilledIconButton(
                                onClick = {
                                    playRequested = true
                                    playbackFailed = false
                                    playbackErrorMessage = null
                                    previewLoading = true
                                    if (player.playerError != null) {
                                        player.setMediaSource(mediaSource)
                                        player.prepare()
                                    }
                                    player.play()
                                },
                                modifier = Modifier.size(64.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White.copy(alpha = .92f))
                            ) {
                                Icon(if (playbackFailed) Icons.Default.Refresh else Icons.Default.PlayArrow, if (playbackFailed) "Coba lagi" else "Putar", Modifier.size(36.dp), tint = Color.Black)
                            }
                            if (previewLoading && !playbackFailed) {
                                Surface(shape = RoundedCornerShape(12.dp), color = Color.Black.copy(alpha = .68f)) {
                                    Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = Color.White)
                                        Spacer(Modifier.width(7.dp))
                                        Text("Menyiapkan video…", color = Color.White, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            } else if (playbackFailed) {
                                Text(playbackErrorMessage ?: "Preview gagal dimuat · ketuk untuk mencoba lagi", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = {
                        player.pause()
                        showFullPreview = true
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                ) {
                    Icon(Icons.Default.Fullscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Full Preview")
                }

                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF161B22),
                        border = BorderStroke(1.dp, Color(0xFF30363D))
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text("INFORMASI TASK & KONTEN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFFA657))
                            HorizontalDivider(color = Color(0xFF30363D))
                            ReelInfoRow("ID Listing", reel.idListing, darkTheme = true)
                            ReelInfoRow("Marketing (ME)", reel.namaMe.ifBlank { "N/A" }, darkTheme = true)
                            ReelInfoRow("Status Posting IG", if (reel.postingIg) "Sudah Diposting" else "Belum Diposting", darkTheme = true)
                            ReelInfoRow("Jadwal Posting", formatPublishJadwalPostingDate(reel.jadwalPosting), darkTheme = true)
                            if (reel.catatan.isNotBlank()) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                                Text("Catatan", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = .55f))
                                Text(reel.catatan, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Video Reels", Modifier.weight(1f), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge, color = Color.White)
                        FilledTonalButton(onClick = { enqueueReelDownload(context, reel) }) {
                            Icon(Icons.Default.Download, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Download")
                        }
                    }
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF161616),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = Color(0xFFFFDF00), modifier = Modifier.size(18.dp))
                                Text(
                                    "Pratinjau Caption Posting (Bisa Di-edit):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFDF00)
                                )
                            }
                            OutlinedTextField(
                                value = caption,
                                onValueChange = { caption = it; captionEdited = true },
                                modifier = Modifier.fillMaxWidth().heightIn(min = 140.dp),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontSize = 13.sp, lineHeight = 18.sp),
                                placeholder = { Text("Tulis caption postingan...", color = Color.Gray) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF0D1117),
                                    unfocusedContainerColor = Color(0xFF0D1117),
                                    focusedBorderColor = Color(0xFFFFDF00).copy(alpha = .8f),
                                    unfocusedBorderColor = Color(0xFF30363D),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
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

    if (showFullPreview) {
        FullReelPreviewDialog(reel = reel, onDismiss = { showFullPreview = false })
    }
}

@Composable
private fun ReelInfoRow(label: String, value: String, darkTheme: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = if (darkTheme) Color.White.copy(alpha = .55f) else MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(value, style = MaterialTheme.typography.labelSmall, color = if (darkTheme) Color.White else Color.Unspecified, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
private fun FullReelPreviewDialog(reel: ReelItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var webView by remember(reel.idListing) { mutableStateOf<WebView?>(null) }
    DisposableEffect(reel.idListing) {
        onDispose {
            webView?.apply { stopLoading(); loadUrl("about:blank"); destroy() }
            webView = null
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (reel.previewUrl.isNotBlank()) {
                    AndroidView(
                        factory = {
                            WebView(context).apply {
                                webView = this
                                setBackgroundColor(android.graphics.Color.BLACK)
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.mediaPlaybackRequiresUserGesture = false
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                settings.userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36"
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                                webChromeClient = WebChromeClient()
                                webViewClient = WebViewClient()
                                if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
                                    WebViewCompat.addDocumentStartJavaScript(
                                        this,
                                        REEL_PLAYER_CONTROLS_SCRIPT,
                                        setOf("https://drive.google.com")
                                    )
                                }
                                loadUrl(reel.previewUrl)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .statusBarsPadding()
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = .68f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Tutup full preview", tint = Color.White)
                }
            }
        }
    }
}

// Drive's control section paints a 90%-opaque backdrop across the entire video.
// Remove only that backdrop; retain Drive's playback, audio and seek controls.
private val REEL_PLAYER_CONTROLS_SCRIPT = """
    (function() {
      document.addEventListener('DOMContentLoaded',function() {
        var style = document.createElement('style');
        style.textContent = 'section.VUN2V{background:transparent!important;background-image:none!important}';
        document.head.appendChild(style);
      });
    })();
""".trimIndent()

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
        scrapedTitle = title,
        editNotes = reel.catatan
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
