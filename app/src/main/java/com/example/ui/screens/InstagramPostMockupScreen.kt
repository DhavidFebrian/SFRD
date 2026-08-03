package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.EditFotoTask

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun InstagramPostMockupScreen(
    task: EditFotoTask,
    listingImagesMap: Map<String, String>,
    listingImagesGalleryMap: Map<String, List<String>>,
    listingDescMap: Map<String, String>,
    listingPriceMap: Map<String, String>,
    listingTitleMap: Map<String, String> = emptyMap(),
    onDismiss: () -> Unit,
    onViewDetails: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val cleanId = task.idListing.trim()
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
    
    val pagerState = rememberPagerState(initialPage = 0) { imagesToDisplay.size }
    val rawDesc = if (cleanId.isNotBlank()) listingDescMap[cleanId] ?: "" else ""
    val rawPrice = if (cleanId.isNotBlank()) listingPriceMap[cleanId] ?: "" else ""
    val scrapedTitle = if (cleanId.isNotBlank()) listingTitleMap[cleanId] ?: "" else ""
    
    // Parse dynamic details
    val details = remember(rawDesc, cleanId, rawPrice, task.judul, scrapedTitle) {
        parsePropertyDetails(rawDesc, cleanId, rawPrice, task.judul, scrapedTitle)
    }

    // Interaction states
    var likeCount by remember { mutableStateOf((10..150).random()) }
    var isLiked by remember { mutableStateOf(false) }
    
    var commentCount by remember { mutableStateOf((5..35).random()) }
    var isCommented by remember { mutableStateOf(false) }

    var repostCount by remember { mutableStateOf((1..15).random()) }
    var isReposted by remember { mutableStateOf(false) }

    var isSaved by remember { mutableStateOf(false) }
    var showWaChooserDialog by remember { mutableStateOf(false) }

    var showRwcDownloadDialog by remember { mutableStateOf(false) }
    var showWaBlastDialog by remember { mutableStateOf(false) }
    var downloadedUris by remember { mutableStateOf<List<android.net.Uri>>(emptyList()) }

    // Build the dynamic Instagram caption/description text
    val captionText = remember(rawDesc, cleanId, rawPrice, task.namaMe, task.judul, scrapedTitle) {
        buildInstagramCaption(rawDesc, cleanId, rawPrice, task.namaMe, task.judul, scrapedTitle)
    }

    // Full screen-ish dialog matching IG black style
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF000000) // Pure Black Instagram style
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header of screen
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Posts",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color(0xFF000000)
                    ),
                    modifier = Modifier.height(56.dp)
                )

                // Scrollable feed content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    // IG User Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Profile picture (Stylized Ray White yellow corporate logo)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFDF00)), // Yellow theme
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Text(
                                    "Ray",
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF333333),
                                    fontSize = 7.sp,
                                    letterSpacing = (-0.3).sp
                                )
                                Text(
                                    "White",
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF333333),
                                    fontSize = 7.sp,
                                    letterSpacing = (-0.3).sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Username + Verified badge
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "raywhitecipete",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                // Verified Badge
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = Color(0xFF0095F6), // Instagram Blue
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            val displayTitleSub = details["title"]?.lowercase()?.split(" ")?.joinToString(" ") { 
                                if (it == "di" || it == "dan" || it == "area" || it == "ke") it else it.replaceFirstChar { char -> char.uppercase() }
                            } ?: "Rumah 2 Lantai di Lokasi Strategis"
                            Text(
                                text = displayTitleSub,
                                color = Color.Gray,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = Color.White
                            )
                        }
                    }

                    // Feed Image Container with overlaid spec card and sliding pager
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f) // Square image standard
                            .background(Color(0xFF121212))
                    ) {
                        if (imagesToDisplay.isNotEmpty()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { pageIndex ->
                                AsyncImage(
                                    model = imagesToDisplay[pageIndex],
                                    contentDescription = "Property Listing Image ${pageIndex + 1}",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Instagram-style Page Indicator on Top Right (e.g., "1/5")
                            if (imagesToDisplay.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1}/${imagesToDisplay.size}",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        } else {
                            // High quality placeholder
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF222222)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Image,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Memuat Foto Listing...", color = Color.Gray)
                                }
                            }
                        }

                        // Bottom gradient overlay to make text highly readable
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .align(Alignment.BottomCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                    )
                                )
                        )

                        // Overlaid Property spec card exactly like user screenshot
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomStart)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            // Location Pin + Name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = details["lokasi"]?.uppercase() ?: "JAKARTA SELATAN",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    letterSpacing = 1.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Icons Row: LT, LB, KT, KM
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.AspectRatio, contentDescription = "LT", tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text("LT ${details["lt"]}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Home, contentDescription = "LB", tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text("LB ${details["lb"]}", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Bed, contentDescription = "KT", tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text(details["kt"] ?: "4", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Icon(Icons.Default.Bathtub, contentDescription = "KM", tint = Color.White, modifier = Modifier.size(13.dp))
                                    Text(details["km"] ?: "3", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Price (yellow gold text, large)
                            Text(
                                text = details["harga"] ?: "Rp. 6,50 M",
                                color = Color(0xFFFFD700), // Gold
                                fontWeight = FontWeight.Black,
                                fontSize = 22.sp
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // "HOT SALE" Red badge
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFFE53935), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    "HOT SALE",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Footer details
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "PROUDLY RAY WHITE",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    "ID ${details["id"]}",
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Slide Indicator Dots directly below image container
                    if (imagesToDisplay.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(imagesToDisplay.size) { index ->
                                val isSelected = pagerState.currentPage == index
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 2.dp)
                                        .size(if (isSelected) 6.dp else 4.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF0095F6) else Color.White.copy(alpha = 0.3f))
                                )
                            }
                        }
                    }

                    // Sleek Interactive Post Details Container
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF161616)),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Toolbar Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Like
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            isLiked = !isLiked
                                            if (isLiked) likeCount++ else likeCount--
                                        }
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (isLiked) Color.Red else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$likeCount",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Comment
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            isCommented = !isCommented
                                            if (isCommented) commentCount++ else commentCount--
                                        }
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = "Komen",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$commentCount",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Repost
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clickable {
                                            isReposted = !isReposted
                                            if (isReposted) repostCount++ else repostCount--
                                        }
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Autorenew,
                                        contentDescription = "Repost",
                                        tint = if (isReposted) Color(0xFF4CAF50) else Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "$repostCount",
                                        color = Color.White,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Share
                                Box(
                                    modifier = Modifier
                                        .clickable {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, captionText)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Bagikan ke WhatsApp"))
                                        }
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Share WA",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                // Save
                                Box(
                                    modifier = Modifier
                                        .clickable { isSaved = !isSaved }
                                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = "Save",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                            // Caption Text
                            Text(
                                text = "Pratinjau Caption Posting:",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFFFDF00),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = captionText,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action buttons row (Copy text, Download Foto, Detail Listing)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // 1. Copy Description button
                        Button(
                            onClick = {
                                val copyableText = captionText.removePrefix("raywhitecipete ").trim()
                                clipboardManager.setText(AnnotatedString(copyableText))
                                Toast.makeText(context, "Deskripsi berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF262626) // Soft IG gray-black button
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Salin Teks", color = Color.White, fontSize = 11.sp)
                        }

                        // 2. Download Foto button (Middle button for RWC portal Design 3 cover & images)
                        Button(
                            onClick = {
                                if (cleanId.isNotBlank()) {
                                    showRwcDownloadDialog = true
                                } else {
                                    Toast.makeText(context, "ID Listing tidak tersedia", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1.1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF25D366) // WA Green theme matching Blast ke WA Group
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Download Foto", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // 3. Detail Listing button
                        Button(
                            onClick = {
                                if (cleanId.isNotBlank()) {
                                    try {
                                        val url = "https://raywhitecipete.net/ListingView/Detail/$cleanId"
                                        val webIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(url))
                                        context.startActivity(webIntent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Gagal membuka web: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(context, "ID Listing tidak tersedia", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF262626)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Detail Listing", color = Color.White, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Blast to WA Group Big Button
                    Button(
                        onClick = {
                            try {
                                // 1. Copy description to clipboard first as the definitive content to be posted
                                clipboardManager.setText(AnnotatedString(captionText))
                                Toast.makeText(
                                    context, 
                                    "Deskripsi disalin! Membuka WhatsApp, pilih grup Anda untuk menempelkan pesan secara otomatis.", 
                                    Toast.LENGTH_LONG
                                ).show()

                                // 2. Launch WhatsApp with pre-filled text parameter via whatsapp://send
                                val encodedText = android.net.Uri.encode(captionText)
                                val uri = android.net.Uri.parse("whatsapp://send?text=$encodedText")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val encodedText = android.net.Uri.encode(captionText)
                                    val uri = android.net.Uri.parse("https://api.whatsapp.com/send?text=$encodedText")
                                    val intent = Intent(Intent.ACTION_VIEW, uri)
                                    context.startActivity(intent)
                                } catch (ex: Exception) {
                                    try {
                                        val encodedText = android.net.Uri.encode(captionText)
                                        val uri = android.net.Uri.parse("https://wa.me/?text=$encodedText")
                                        val intent = Intent(Intent.ACTION_VIEW, uri)
                                        context.startActivity(intent)
                                    } catch (ex2: Exception) {
                                        Toast.makeText(context, "Gagal membuka WhatsApp: ${ex2.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp)
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF25D366) // WA Green call-to-action button
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "WhatsApp Icon",
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Blast ke WA Group",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(40.dp))
                }
            }
        }
    }

    if (showRwcDownloadDialog) {
        val initialHeadline = remember(task.judul, details) {
            val loc = details["lokasi"] ?: ""
            if (loc.isNotBlank() && !loc.equals("Kebayoran", ignoreCase = true) && !loc.equals("Unknown", ignoreCase = true)) {
                loc
            } else {
                task.judul.ifBlank { details["title"] ?: "" }
            }
        }
        RwcDesign3DownloadDialog(
            listingId = cleanId,
            initialCoverTitle = initialHeadline,
            onDismiss = { showRwcDownloadDialog = false },
            onDownloadSuccess = { uris, title ->
                showRwcDownloadDialog = false
                downloadedUris = uris
                showWaBlastDialog = true
            }
        )
    }

    if (showWaBlastDialog) {
        WaBlastPostDialog(
            initialCaption = captionText,
            downloadedUris = downloadedUris,
            listingId = cleanId,
            onDismiss = { showWaBlastDialog = false }
        )
    }
}

// Parser helper function to extract specs from Ray White description dynamically
private fun parsePropertyDetails(rawDesc: String, idListing: String, rawPrice: String, title: String, scrapedTitle: String = ""): Map<String, String> {
    val details = mutableMapOf<String, String>()
    
    // Strip HTML tags
    val cleanDesc = rawDesc.replace("<[^>]*>".toRegex(), "")
    val descLower = cleanDesc.lowercase()

    // 1. ID
    details["id"] = idListing.ifBlank { "8255" }

    // 10. Lokasi (Location) - computed first for title fallback
    val lokasiVal = extractPropertyLocation(descLower, title.lowercase(), scrapedTitle.lowercase())
    details["lokasi"] = lokasiVal

    // 2. Title - Use task.judul if available, otherwise capitalize first line of description or fallback
    val displayTitle = selectPropertyTitle(scrapedTitle, title, cleanDesc, lokasiVal)
    details["title"] = displayTitle.uppercase()

    // 3. Price
    val displayPrice = if (rawPrice.isNotBlank() && !rawPrice.contains("Hubungi", ignoreCase = true)) {
        rawPrice
    } else {
        // Try parsing price from description if not available
        // e.g., "6,5 M" or "6.5M" or "Rp 6.500.000.000" or similar
        val priceRegex = Regex("(?:harga|rp|idr)[:\\s-]*([\\d\\.,]+(?:\\s*(?:milyar|miliar|juta|m|jt|b|t))?)")
        val match = priceRegex.find(descLower)
        if (match != null) {
            val pVal = match.groupValues[1].uppercase().trim()
            if (pVal.endsWith("M") || pVal.contains("MILYAR") || pVal.contains("MILIAR")) {
                "Rp. ${pVal.replace("MILYAR", "M").replace("MILIAR", "M").trim()}"
            } else if (pVal.endsWith("JT") || pVal.contains("JUTA")) {
                "Rp. ${pVal.replace("JUTA", "Jt").trim()}"
            } else {
                "Rp. $pVal"
            }
        } else {
            "Rp. Hubungi Agent"
        }
    }
    details["harga"] = displayPrice
        .replace("(?i)\\bper\\s*meter\\b".toRegex(), "/ m2")
        .replace("(?i)\\bper\\s*m2\\b".toRegex(), "/ m2")
        .replace("(?i)\\bper\\s*m²\\b".toRegex(), "/ m2")
        .replace("(?i)/\\s*meter\\b".toRegex(), "/ m2")

    // Support combined LT/LB parsing (e.g. LT/LB 936/600 or LT/LB 936 / 600 or LT/LB : 936/600)
    val combinedLtLbRegex = Regex("(?i)\\b(?:lt\\s*/\\s*lb|luas\\s*tanah\\s*/\\s*luas\\s*bangunan|lt\\s*-\\s*lb)\\s*[:\\s-]*(\\d+)\\s*[\\s/-]+\\s*(\\d+)")
    val combinedLbLtRegex = Regex("(?i)\\b(?:lb\\s*/\\s*lt|luas\\s*bangunan\\s*/\\s*luas\\s*tanah|lb\\s*-\\s*lt)\\s*[:\\s-]*(\\d+)\\s*[\\s/-]+\\s*(\\d+)")
    
    val combinedLtLbMatch = combinedLtLbRegex.find(descLower)
    val combinedLbLtMatch = combinedLbLtRegex.find(descLower)
    
    var parsedLt: String? = null
    var parsedLb: String? = null
    
    if (combinedLtLbMatch != null) {
        parsedLt = combinedLtLbMatch.groupValues[1]
        parsedLb = combinedLtLbMatch.groupValues[2]
    } else if (combinedLbLtMatch != null) {
        parsedLb = combinedLbLtMatch.groupValues[1]
        parsedLt = combinedLbLtMatch.groupValues[2]
    }

    // 4. LT (Luas Tanah)
    if (parsedLt != null) {
        details["lt"] = parsedLt
    } else {
        val ltRegex = Regex("(?:lt|luas\\s*tanah)[:\\s\\.]*(\\d+)")
        val ltMatch = ltRegex.find(descLower)
        details["lt"] = ltMatch?.groupValues?.get(1) ?: "323" // Fallback
    }

    // 5. LB (Luas Bangunan)
    if (parsedLb != null) {
        details["lb"] = parsedLb
    } else {
        val lbRegex = Regex("(?:lb|luas\\s*bangunan)[:\\s\\.]*(\\d+)")
        val lbMatch = lbRegex.find(descLower)
        details["lb"] = lbMatch?.groupValues?.get(1) ?: "300" // Fallback
    }

    // 6. KT (Kamar Tidur)
    val ktRegex = Regex("(?:kt|kamar\\s*tidur|k\\.?\\s*tidur)[:\\s\\.]*(\\d+(?:\\s*\\+\\s*\\d+)?)")
    val ktMatch = ktRegex.find(descLower)
    details["kt"] = ktMatch?.groupValues?.get(1) ?: "4+1"

    // 7. KM (Kamar Mandi)
    val kmRegex = Regex("(?:km|kamar\\s*mandi|k\\.?\\s*mandi)[:\\s\\.]*(\\d+(?:\\s*\\+\\s*\\d+)?)")
    val kmMatch = kmRegex.find(descLower)
    details["km"] = kmMatch?.groupValues?.get(1) ?: "3+1"

    // 8. Garasi
    val garasiRegex = Regex("(?:garasi)[:\\s\\.]*(\\d+)")
    val garasiMatch = garasiRegex.find(descLower)
    details["garasi"] = if (garasiMatch != null) "${garasiMatch.groupValues[1]} Mobil" else "1 Mobil"

    // 9. Carport
    val carportRegex = Regex("(?:carport)[:\\s\\.]*(\\d+)")
    val carportMatch = carportRegex.find(descLower)
    details["carport"] = if (carportMatch != null) "${carportMatch.groupValues[1]} Mobil" else "2 Mobil"

    // 11. Additional features (e.g. Swimming Pool, Garden, Security, etc.)
    details["swimming_pool"] = if (descLower.contains("kolam renang") || descLower.contains("swimming pool") || descLower.contains("pool")) "Bisa Untuk Swimming Pool" else "Halaman Belakang Luas"
    details["security"] = if (descLower.contains("security") || descLower.contains("satpam") || descLower.contains("one gate")) "One Gate System, 24-Hour Security" else "Lingkungan Aman & Tenang"
    details["sertifikat"] = if (descLower.contains("shm") || descLower.contains("sertifikat hak milik")) "SHM & IMB Lengkap" else "Sertifikat Hak Milik (SHM)"

    return details
}

// Check if a line is just numbers or stats (e.g. "6 5 2" or "6/5/2" or starts with numbers without letters)
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

// Select a valid property title, preferring rich headlines written in Deskripsi Lengkap over generic web titles
private fun selectPropertyTitle(scrapedTitle: String, judulTask: String, cleanDesc: String, lokasi: String): String {
    // 1. PRIORITY 1: Prefer rich title line inside Deskripsi Lengkap
    if (cleanDesc.isNotBlank()) {
        val lines = cleanDesc.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            val lower = line.lowercase()
            val isSpecKey = lower.startsWith("luas tanah") || lower.startsWith("lt") ||
                    lower.startsWith("luas bangunan") || lower.startsWith("lb") ||
                    lower.startsWith("kamar tidur") || lower.startsWith("kt") ||
                    lower.startsWith("kamar mandi") || lower.startsWith("km") ||
                    lower.startsWith("sertifikat") || lower.startsWith("shm") ||
                    lower.startsWith("garasi") || lower.startsWith("carport") ||
                    lower.startsWith("dimensi") || lower.startsWith("menghadap") ||
                    lower.startsWith("harga") || lower.startsWith("listing id") ||
                    lower.startsWith("dengan spek") || lower.startsWith("dengan spesifikasi") ||
                    lower.contains("deskripsi lengkap") || lower.contains("ray white") ||
                    lower.contains("hubungi kami") || lower.contains("contact us")

            if (!isSpecKey && !isLineJustNumbersOrStats(line) && !isStatusTagText(line) && line.length >= 5) {
                return line
            }
        }
    }

    // 2. PRIORITY 2: Fallback to scraped web title if no rich title in description
    var title = scrapedTitle.replace("<[^>]*>".toRegex(), "").trim()
    if (title.isNotBlank() && !isLineJustNumbersOrStats(title) && !isStatusTagText(title)) {
        return title
    }

    // 3. PRIORITY 3: Fallback to task title
    title = judulTask.replace("<[^>]*>".toRegex(), "").trim()
    if (title.isNotBlank() && !isLineJustNumbersOrStats(title) && !isStatusTagText(title)) {
        return title
    }

    // 4. Default fallback
    val cleanLokasi = lokasi.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    return "Rumah 2 Lantai di Lokasi Strategis Area $cleanLokasi"
}

// Beautifully format individual bullet points to keep them clean and professional
private fun formatBulletPoint(line: String): String {
    var cleanedLine = line.trim()
    while (cleanedLine.isNotEmpty() && (cleanedLine.startsWith("•") || cleanedLine.startsWith("-") || cleanedLine.startsWith("*") || cleanedLine.startsWith("+") || cleanedLine.startsWith("o") || cleanedLine.startsWith("~"))) {
        cleanedLine = cleanedLine.substring(1).trim()
    }
    cleanedLine = cleanedLine.replace("^\\d+[\\.\\)]\\s*".toRegex(), "").trim()
    
    // Remove trailing commas or semicolons
    while (cleanedLine.endsWith(",") || cleanedLine.endsWith(";")) {
        cleanedLine = cleanedLine.substring(0, cleanedLine.length - 1).trim()
    }
    
    val lower = cleanedLine.lowercase()
    
    // Check dimension pattern first (e.g. "Luas tanah 15x21" or "15 x 21" or "15x21 m")
    val dimRegex = Regex("(?i)(?:luas\\s*tanah\\s*|dimensi\\s*)?(\\d+)\\s*[x×\\*]\\s*(\\d+)")
    val dimMatch = dimRegex.find(cleanedLine)
    if (dimMatch != null && (lower.contains("x") || lower.contains("×") || lower.contains("*") || lower.contains("dimensi"))) {
        val w = dimMatch.groupValues[1]
        val h = dimMatch.groupValues[2]
        return "• Dimensi $w × $h meter"
    }

    // Check for combined LT/LB line (e.g. LT/LB 936/600)
    val combinedLtLbRegex = Regex("(?i)\\b(?:lt\\s*/\\s*lb|luas\\s*tanah\\s*/\\s*luas\\s*bangunan|lt\\s*-\\s*lb)\\s*[:\\s-]*(\\d+)\\s*[\\s/-]+\\s*(\\d+)")
    val combinedLbLtRegex = Regex("(?i)\\b(?:lb\\s*/\\s*lt|luas\\s*bangunan\\s*/\\s*luas\\s*tanah|lb\\s*-\\s*lt)\\s*[:\\s-]*(\\d+)\\s*[\\s/-]+\\s*(\\d+)")
    
    val combinedLtLbMatch = combinedLtLbRegex.find(lower)
    if (combinedLtLbMatch != null) {
        val ltVal = combinedLtLbMatch.groupValues[1]
        val lbVal = combinedLtLbMatch.groupValues[2]
        return "• Luas Tanah : $ltVal m2\n• Luas Bangunan : $lbVal m2"
    }
    
    val combinedLbLtMatch = combinedLbLtRegex.find(lower)
    if (combinedLbLtMatch != null) {
        val lbVal = combinedLbLtMatch.groupValues[1]
        val ltVal = combinedLbLtMatch.groupValues[2]
        return "• Luas Tanah : $ltVal m2\n• Luas Bangunan : $lbVal m2"
    }
    
    // 1. Luas Tanah (LT)
    if (lower.startsWith("luas tanah") || lower.startsWith("lt")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)luas\\s*tanah".toRegex(), "").replace("(?i)\\blt\\b".toRegex(), "").trim()
        }
        val cleanValue = value.replace("(?i)m2".toRegex(), "")
                             .replace("(?i)m²".toRegex(), "")
                             .replace("(?i)meter".toRegex(), "")
                             .trim()

        val numberMatches = Regex("\\d+(?:[\\.,]\\d+)?").findAll(cleanValue).map { it.value }.toList()
        val formattedVal = when {
            numberMatches.size >= 2 -> "${numberMatches[0]} - ${numberMatches[1]} m2"
            numberMatches.size == 1 -> "${numberMatches[0]} m2"
            else -> "315 m2"
        }
        return "• Luas Tanah : $formattedVal"
    }
    
    // 2. Luas Bangunan (LB)
    if (lower.startsWith("luas bangunan") || lower.startsWith("lb")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)luas\\s*bangunan".toRegex(), "").replace("(?i)\\blb\\b".toRegex(), "").trim()
        }
        val cleanValue = value.replace("(?i)m2".toRegex(), "")
                             .replace("(?i)m²".toRegex(), "")
                             .replace("(?i)meter".toRegex(), "")
                             .trim()

        val numberMatches = Regex("\\d+(?:[\\.,]\\d+)?").findAll(cleanValue).map { it.value }.toList()
        val formattedVal = when {
            numberMatches.size >= 2 -> "${numberMatches[0]} - ${numberMatches[1]} m2"
            numberMatches.size == 1 -> "${numberMatches[0]} m2"
            else -> "190 m2"
        }
        return "• Luas Bangunan : $formattedVal"
    }

    // 3. Menghadap
    if (lower.startsWith("menghadap")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)menghadap(?:\\s*ke\\s*arah)?".toRegex(), "").trim()
        }
        val direction = value.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        return "• Menghadap ke Arah : ${direction.ifBlank { "Barat" }}"
    }
    
    // 4. Kamar Tidur (KT)
    if (lower.startsWith("kamar tidur") || lower.startsWith("kt")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)kamar\\s*tidur".toRegex(), "").replace("(?i)kt".toRegex(), "").trim()
        }
        val formatted = value.replace("\\s+".toRegex(), " ")
        return "• Kamar Tidur : ${formatted.ifBlank { "4" }}"
    }
    
    // 5. Kamar Mandi (KM)
    if (lower.startsWith("kamar mandi") || lower.startsWith("km")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)kamar\\s*mandi".toRegex(), "").replace("(?i)km".toRegex(), "").trim()
        }
        val formatted = value.replace("\\s+".toRegex(), " ")
        return "• Kamar Mandi : ${formatted.ifBlank { "3" }}"
    }
    
    // 6. Garasi
    if (lower.startsWith("garasi")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)garasi".toRegex(), "").trim()
        }
        val digits = value.replace("[^\\d\\s\\+]".toRegex(), "").trim()
        return "• Garasi : ${digits.ifBlank { "2" }}"
    }
    
    // 7. Carport
    if (lower.startsWith("carport")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)carport".toRegex(), "").trim()
        }
        val digits = value.replace("[^\\d\\s\\+]".toRegex(), "").trim()
        return "• Carport : ${digits.ifBlank { "2" }}"
    }
    
    // 8. Sertifikat
    if (lower.startsWith("sertifikat") || lower.startsWith("shm")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)sertifikat".toRegex(), "").trim()
        }
        val upperVal = value.uppercase()
        if (upperVal.contains("SHM") || upperVal.contains("HAK MILIK") || upperVal.isBlank()) {
            return "• Sertifikat : SHM"
        }
        return "• Sertifikat : $value"
    }
    
    // Fallback: format line nicely
    val words = cleanedLine.split("\\s+".toRegex()).map { word ->
        if (word.lowercase() == "dan" || word.lowercase() == "di" || word.lowercase() == "area" || word.lowercase() == "ke") {
            word.lowercase()
        } else {
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        }
    }.joinToString(" ")
    
    return "• $words"
}

private fun getCanonicalSpecKey(key: String): String {
    val k = key.lowercase().trim()
    return when {
        k == "lt" || k == "luas tanah" -> "luas tanah"
        k == "lb" || k == "luas bangunan" -> "luas bangunan"
        k == "kt" || k == "kamar tidur" -> "kamar tidur"
        k == "km" || k == "kamar mandi" -> "kamar mandi"
        k == "shm" || k == "sertifikat" -> "sertifikat"
        k == "dimensi" -> "dimensi"
        k == "garasi" -> "garasi"
        k == "carport" -> "carport"
        k.contains("menghadap") -> "menghadap"
        else -> k
    }
}

private fun sortBulletPoints(bulletPoints: List<String>): List<String> {
    fun getSortPriority(bLine: String): Int {
        val clean = bLine.removePrefix("•").trim()
        val rawKey = if (clean.contains(":")) clean.substringBefore(":").trim() else clean
        val canonical = getCanonicalSpecKey(rawKey)
        return when (canonical) {
            "luas tanah" -> 1
            "luas bangunan" -> 2
            "dimensi" -> 3
            "kamar tidur" -> 4
            "kamar mandi" -> 5
            "garasi" -> 6
            "carport" -> 7
            "sertifikat" -> 8
            "menghadap" -> 9
            else -> 100
        }
    }
    return bulletPoints.sortedWith(compareBy { getSortPriority(it) })
}

private fun isTitleDuplicateOrHeadline(
    line: String, 
    title: String, 
    scrapedTitle: String, 
    judulTask: String
): Boolean {
    val lower = line.lowercase().trim()
    if (lower.isBlank()) return true

    // 1. Direct exact or normalized title match
    val lineNorm = line.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
    val titleNorm = title.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
    val scrapedTitleNorm = scrapedTitle.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()
    val judulTaskNorm = judulTask.replace("[^a-zA-Z0-9]".toRegex(), "").lowercase()

    if (lineNorm.isNotBlank()) {
        if (lineNorm == titleNorm || 
            lineNorm == scrapedTitleNorm || 
            lineNorm == judulTaskNorm ||
            (titleNorm.isNotBlank() && (lineNorm.contains(titleNorm) || titleNorm.contains(lineNorm)))
        ) {
            return true
        }
    }

    // 2. Headline prefix check
    val isHeadlinePrefix = lower.startsWith("rumah dijual") || 
        lower.startsWith("rumah disewakan") || 
        lower.startsWith("dijual rumah") || 
        lower.startsWith("disewakan rumah") ||
        lower.startsWith("ruko dijual") || 
        lower.startsWith("ruko disewakan") || 
        lower.startsWith("tanah dijual") || 
        lower.startsWith("tanah disewakan") || 
        lower.startsWith("apartemen dijual") ||
        lower.startsWith("villa dijual") ||
        lower.startsWith("rumah modern") ||
        lower.startsWith("rumah mewah") ||
        lower.startsWith("rumah cantik") ||
        lower.startsWith("rumah baru")

    if (isHeadlinePrefix) return true

    // 3. Word-set overlap check with title candidates
    val stopWords = setOf("dan", "di", "ke", "yang", "dengan", "untuk", "pada", "ini", "itu", "atau", "dari", "serta")
    fun extractWordSet(text: String): Set<String> {
        return text.lowercase()
            .replace("[^a-z0-9\\s]".toRegex(), " ")
            .split("\\s+".toRegex())
            .filter { it.length >= 2 && it !in stopWords }
            .toSet()
    }

    val lineWords = extractWordSet(line)
    if (lineWords.isEmpty()) return false

    val titleCandidates = listOf(title, scrapedTitle, judulTask)
    for (cand in titleCandidates) {
        if (cand.isBlank()) continue
        val candWords = extractWordSet(cand)
        if (candWords.isEmpty()) continue

        val overlap = lineWords.intersect(candWords)
        if (overlap.size >= 2) {
            val ratioLine = overlap.size.toDouble() / lineWords.size
            val ratioCand = overlap.size.toDouble() / candWords.size
            if (ratioLine >= 0.50 || ratioCand >= 0.50 || overlap.size >= minOf(lineWords.size, candWords.size)) {
                return true
            }
        }
    }

    return false
}

// Dynamic Instagram caption generator
private fun buildInstagramCaption(
    rawDesc: String,
    idListing: String,
    rawPrice: String,
    namaMe: String,
    judulTask: String,
    scrapedTitle: String = ""
): String {
    // 1. Clean HTML and strip any leading "dengan spek" section completely
    val clean = com.example.ui.cleanListingDescription(
        rawDesc
            .replace("(?i)<br\\s*/?>".toRegex(), "\n")
            .replace("(?i)</p>".toRegex(), "\n")
            .replace("<[^>]*>".toRegex(), "")
    ).trim()

    val descLower = clean.lowercase()
    val lokasiVal = extractPropertyLocation(descLower, judulTask.lowercase(), scrapedTitle.lowercase())
    val title = selectPropertyTitle(scrapedTitle, judulTask, clean, lokasiVal)

    // Multi-contact resolver
    val contactsStr = getInstagramCaptionContacts(namaMe, rawDesc, scrapedTitle, judulTask, idListing)

    // Extract specs ONLY from "Deskripsi Lengkap" or main cleaned description body (NEVER from "Dengan Spek")
    val parsedBulletPoints = mutableListOf<String>()
    val linesToProcess = mutableListOf<String>()

    val deskripsiLengkapIndex = clean.indexOf("Deskripsi Lengkap:", ignoreCase = true)
    val deskripsiLengkapAltIndex = if (deskripsiLengkapIndex == -1) clean.indexOf("Deskripsi Lengkap", ignoreCase = true) else deskripsiLengkapIndex

    if (deskripsiLengkapAltIndex != -1) {
        val headerLength = if (deskripsiLengkapIndex != -1) "Deskripsi Lengkap:".length else "Deskripsi Lengkap".length
        val deskripsiText = clean.substring(deskripsiLengkapAltIndex + headerLength)
        deskripsiText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.forEach { linesToProcess.add(it) }
    } else {
        clean.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.forEach { linesToProcess.add(it) }
    }

    var stopParsing = false

    for (line in linesToProcess) {
        if (stopParsing) break
        val lower = line.lowercase()

        if (lower.contains("http://") || lower.contains("https://") ||
            lower.contains("wa.me") || lower.contains("copyright") ||
            lower.contains("ray white") || lower.contains("hubungi kami") ||
            lower.contains("contact us")
        ) {
            stopParsing = true
            continue
        }

        if (isTitleDuplicateOrHeadline(line, title, scrapedTitle, judulTask) ||
            lower.startsWith("dengan spek") || 
            lower.startsWith("dengan spesifikasi") ||
            lower.startsWith("listing id:") || 
            lower.contains("deskripsi lengkap") ||
            lower.contains("for sale") ||
            lower.contains("hubungi") ||
            lower.contains("contact") ||
            lower.startsWith("harga") ||
            isLineJustNumbersOrStats(line) ||
            line.matches("^\\d+$".toRegex())
        ) {
            continue
        }

        val formatted = formatBulletPoint(line)
        if (formatted.isNotEmpty()) {
            val bulletLines = formatted.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
            for (bLine in bulletLines) {
                val cleanFormatted = bLine.removePrefix("•").trim()
                val rawKey = if (cleanFormatted.contains(":")) cleanFormatted.substringBefore(":").trim() else cleanFormatted
                val canonicalKey = getCanonicalSpecKey(rawKey)
                
                val isKnownSpecKey = canonicalKey in setOf(
                    "luas tanah", "luas bangunan", "kamar tidur", "kamar mandi", 
                    "garasi", "carport", "sertifikat", "menghadap", "dimensi"
                )
                
                val existingCanonicalKeys = parsedBulletPoints.map { 
                    val c = it.removePrefix("•").trim()
                    val rKey = if (c.contains(":")) c.substringBefore(":").trim() else c
                    getCanonicalSpecKey(rKey)
                }
                
                val isDuplicateKey = isKnownSpecKey && existingCanonicalKeys.contains(canonicalKey)
                val isDuplicateText = parsedBulletPoints.any { 
                    it.equals(bLine, ignoreCase = true) || 
                    it.replace("[^a-zA-Z0-9]".toRegex(), "").equals(bLine.replace("[^a-zA-Z0-9]".toRegex(), ""), ignoreCase = true)
                }
                
                if (!isDuplicateKey && !isDuplicateText) {
                    parsedBulletPoints.add(bLine)
                }
            }
        }
    }

    if (parsedBulletPoints.isEmpty()) {
        val details = parsePropertyDetails(rawDesc, idListing, rawPrice, judulTask, scrapedTitle)
        parsedBulletPoints.add("• Luas Tanah : ${details["lt"]} m2")
        parsedBulletPoints.add("• Luas Bangunan : ${details["lb"]} m2")
        parsedBulletPoints.add("• Kamar Tidur : ${details["kt"]}")
        parsedBulletPoints.add("• Kamar Mandi : ${details["km"]}")
        parsedBulletPoints.add("• Garasi : ${details["garasi"]}")
        parsedBulletPoints.add("• Carport : ${details["carport"]}")
        parsedBulletPoints.add("• ${details["sertifikat"]}")
    }

    val sortedBulletPoints = sortBulletPoints(parsedBulletPoints)

    // Build final caption without "Nego"
    val category = getListingCategory(rawPrice, clean)
    return buildString {
        append("raywhitecipete $category ID $idListing\n\n")
        append("$title\n\n")
        
        sortedBulletPoints.forEach { append("$it\n") }
        append("\n")
        
        val displayPrice = formatPropertyPriceFull(rawPrice, clean)
        append("$displayPrice\n\n")
        
        append(contactsStr)
        append("DM US FOR MORE INFORMATION")
    }
}

private fun resolveMarketingName(namaMe: String, rawDesc: String, scrapedTitle: String, judulTask: String): String {
    val cleanInput = namaMe.trim()
    val combinedText = "$rawDesc $scrapedTitle $judulTask".lowercase()

    // 1. Direct check if Mari or Mari Hariadi is explicitly mentioned
    if (combinedText.contains("\\bmari\\b".toRegex()) || combinedText.contains("mari.raywhite") || combinedText.contains("08118087908")) {
        return "Mari"
    }

    // 2. Check cleanInput if it's a valid agent name and not Kebayoran/Hubungi Agent/Unknown
    if (cleanInput.isNotBlank() && 
        !cleanInput.equals("Hubungi Agent", ignoreCase = true) && 
        !cleanInput.equals("Unknown", ignoreCase = true) &&
        !cleanInput.contains("Kebayoran", ignoreCase = true)
    ) {
        val directContact = com.example.ui.findContact(cleanInput)
        if (directContact != null && directContact.nameKey != "bayu") {
            return directContact.nameKey.replaceFirstChar { it.uppercase() }
        }
        if (cleanInput.equals("bayu", ignoreCase = true)) {
            // Confirm bayu is actually intended and not a location mismatch
            if (combinedText.contains("\\bbayu\\b".toRegex())) {
                return "Bayu"
            }
        } else {
            val matchedAgent = com.example.ui.AGENT_CONTACT_LIST.find { contact ->
                cleanInput.lowercase().contains(contact.nameKey)
            }
            if (matchedAgent != null) {
                return matchedAgent.nameKey.replaceFirstChar { it.uppercase() }
            }
            return cleanInput
        }
    }

    // 3. Scan combinedText for any agent from AGENT_CONTACT_LIST using strict word boundary (\b)
    val matchedAgent = com.example.ui.AGENT_CONTACT_LIST.find { contact ->
        Regex("\\b" + Regex.escape(contact.nameKey) + "\\b", RegexOption.IGNORE_CASE).containsMatchIn(combinedText)
    }
    if (matchedAgent != null) {
        return matchedAgent.nameKey.replaceFirstChar { it.uppercase() }
    }

    return "Mari"
}

private fun getInstagramCaptionContacts(
    namaMe: String, 
    rawDesc: String = "", 
    scrapedTitle: String = "", 
    judulTask: String = "",
    idListing: String = ""
): String {
    val cleanId = idListing.replace("[^0-9]".toRegex(), "").trim()
    val matchedContacts = mutableListOf<com.example.ui.AgentContact>()

    // Explicit ID Overrides for 100% accuracy
    if (cleanId == "11366") {
        com.example.ui.findContact("yayan")?.let { matchedContacts.add(it) }
    } else if (cleanId == "11091") {
        com.example.ui.findContact("hilda")?.let { matchedContacts.add(it) }
        com.example.ui.findContact("remmy")?.let { matchedContacts.add(it) }
    }

    if (matchedContacts.isNotEmpty()) {
        return buildString {
            append("CONTACT\n")
            matchedContacts.distinctBy { it.nameKey.lowercase() }.forEach { contact ->
                val igHandle = if (contact.instagram.isNotBlank()) {
                    val cleanIg = contact.instagram.removePrefix("@")
                    "/@$cleanIg"
                } else ""
                val contactName = contact.nameKey.uppercase()
                append("$contactName: ${contact.phone}$igHandle\n")
            }
            append("\n")
        }
    }

    // 1. Primary Source: namaMe input string (e.g. "Hilda / Remmy", "Hilda & Remmy", "Yayan", "Dian")
    val cleanNamaMe = namaMe.trim()
    if (cleanNamaMe.isNotBlank() && 
        !cleanNamaMe.equals("Hubungi Agent", ignoreCase = true) && 
        !cleanNamaMe.equals("Unknown", ignoreCase = true) &&
        !cleanNamaMe.contains("Kebayoran", ignoreCase = true)
    ) {
        val nameTokens = cleanNamaMe.split(Regex("(?i)\\s*[/&+,]\\s*|\\s+dan\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        for (token in nameTokens) {
            val contact = com.example.ui.findContact(token)
            if (contact != null) {
                matchedContacts.add(contact)
            } else {
                val agent = com.example.ui.AGENT_CONTACT_LIST.find { 
                    token.lowercase().contains(it.nameKey.lowercase()) || it.nameKey.lowercase().contains(token.lowercase())
                }
                if (agent != null) {
                    matchedContacts.add(agent)
                }
            }
        }
    }

    // 2. Secondary Source: Check explicit contact mentions or phone/IG handles in text
    val combinedText = "$cleanNamaMe $rawDesc $scrapedTitle $judulTask".lowercase()
    com.example.ui.AGENT_CONTACT_LIST.forEach { contact ->
        val key = contact.nameKey.lowercase()
        val phoneClean = contact.phone.replace("[^0-9]".toRegex(), "")
        val igClean = contact.instagram.removePrefix("@").lowercase()
        
        val isPhoneMentioned = phoneClean.length >= 8 && combinedText.contains(phoneClean)
        val isIgMentioned = igClean.isNotBlank() && combinedText.contains(igClean)
        val isExplicitlyContacted = combinedText.contains("hubungi.*\\b${Regex.escape(key)}\\b".toRegex()) ||
                                     combinedText.contains("contact.*\\b${Regex.escape(key)}\\b".toRegex()) ||
                                     combinedText.contains("agent.*\\b${Regex.escape(key)}\\b".toRegex()) ||
                                     combinedText.contains("\\b${Regex.escape(key)}\\.raywhite".toRegex()) ||
                                     combinedText.contains("raywhite_${Regex.escape(key)}".toRegex())

        if (isPhoneMentioned || isIgMentioned || isExplicitlyContacted) {
            matchedContacts.add(contact)
        }
    }

    // 3. Fallback: If still empty, scan safe keys or default to Mari
    if (matchedContacts.isEmpty()) {
        com.example.ui.AGENT_CONTACT_LIST.forEach { contact ->
            val key = contact.nameKey.lowercase()
            val isSafeKey = key != "resmi" && key != "indah" && key != "ilham" && key != "duta" && key != "dutta" && key != "bayu"
            if (isSafeKey && combinedText.contains("\\b${Regex.escape(key)}\\b".toRegex())) {
                matchedContacts.add(contact)
            }
        }
    }

    if (matchedContacts.isEmpty()) {
        com.example.ui.findContact("Mari")?.let { matchedContacts.add(it) }
    }

    return buildString {
        append("CONTACT\n")
        matchedContacts.distinctBy { it.nameKey.lowercase() }.forEach { contact ->
            val igHandle = if (contact.instagram.isNotBlank()) {
                val cleanIg = contact.instagram.removePrefix("@")
                "/@$cleanIg"
            } else ""
            val contactName = contact.nameKey.uppercase()
            append("$contactName: ${contact.phone}$igHandle\n")
        }
        append("\n")
    }
}

private fun parsePriceStringToValue(priceStr: String): Long {
    val lower = priceStr.lowercase().trim()
    val clean = lower.replace("rp\\.?".toRegex(), "").trim()
    val numRegex = """[0-9\.,]+""".toRegex()
    val numMatch = numRegex.find(clean)?.value ?: return 0L
    
    val hasSuffix = clean.contains("m") || clean.contains("miliar") || clean.contains("milyar") || clean.contains("juta") || clean.contains("jt") || clean.contains("j")
    val cleanNumStr = if (hasSuffix) {
        numMatch.replace(",", ".")
    } else {
        numMatch.replace(".", "").replace(",", "")
    }
    
    val baseVal = cleanNumStr.toDoubleOrNull() ?: return 0L
    return when {
        clean.contains("milyar") || clean.contains("miliar") || clean.contains("m") -> {
            (baseVal * 1_000_000_000L).toLong()
        }
        clean.contains("juta") || clean.contains("jt") || clean.contains("j") -> {
            (baseVal * 1_000_000L).toLong()
        }
        else -> {
            baseVal.toLong()
        }
    }
}

private fun isRentPriceValue(priceValue: Long, originalStr: String): Boolean {
    val lower = originalStr.lowercase()
    if (lower.contains("tahun") || lower.contains("thn") || lower.contains("bulan") || lower.contains("bln") || lower.contains("sewa") || lower.contains("rent") || lower.contains("kontrak") || lower.contains("/th") || lower.contains("/bln")) {
        return true
    }
    if (priceValue in 1..999_999_999L) {
        return true
    }
    return false
}

private fun getListingCategory(rawPrice: String, rawDesc: String = ""): String {
    val combined = "$rawPrice $rawDesc".lowercase()
    val hasRentKey = combined.contains("sewa") || combined.contains("rent") || combined.contains("kontrak") || combined.contains("/th") || combined.contains("/bln")
    val hasSaleKey = combined.contains("jual") || combined.contains("sale") || parsePriceStringToValue(rawPrice) >= 1_000_000_000L
    
    val priceParts = rawPrice.split(Regex("\\s*/\\s*|\\s+dan\\s+|\\s*&\\s*")).filter { it.isNotBlank() }
    
    if (priceParts.size > 1 || (hasRentKey && hasSaleKey && combined.contains("rp"))) {
        var hasRent = false
        var hasSale = false
        for (part in priceParts) {
            val value = parsePriceStringToValue(part)
            if (isRentPriceValue(value, part)) {
                hasRent = true
            } else {
                hasSale = true
            }
        }
        if ((hasRent && hasSale) || (hasRentKey && hasSaleKey)) {
            return "[FOR SALE / RENT]"
        }
    }
    
    val value = parsePriceStringToValue(rawPrice)
    if (isRentPriceValue(value, rawPrice) || (hasRentKey && !hasSaleKey)) {
        return "[FOR RENT]"
    } else {
        return "[FOR SALE]"
    }
}

private fun formatPropertyPriceFull(rawPrice: String, rawDesc: String): String {
    val combined = "$rawPrice\n$rawDesc"
    val combinedLower = combined.lowercase()
    
    val isLandProperty = combinedLower.contains("tanah") || combinedLower.contains("kavling") || combinedLower.contains("kav") || combinedLower.contains("land")
    val isPerMeterPrice = combinedLower.contains("/ m2") || combinedLower.contains("/m2") || combinedLower.contains("/ m²") || combinedLower.contains("/m²") || combinedLower.contains("per m2") || combinedLower.contains("per m²") || combinedLower.contains("per meter") || combinedLower.contains("/ meter")

    // 1. Search for explicit Jual & Sewa patterns
    val jualRegex = Regex("(?i)(?:harga\\s*)?jual[:\\s-]*((?:rp\\.?\\s*)?[0-9\\.,]+\\s*(?:milyar|miliar|m|juta|jt|b|t)?(?:\\s*(?:/|per)\\s*(?:m2|m²|meter|thn|tahun|bln|bulan))?)")
    val sewaRegex = Regex("(?i)(?:harga\\s*)?sewa[:\\s-]*((?:rp\\.?\\s*)?[0-9\\.,]+\\s*(?:milyar|miliar|m|juta|jt|b|t)?(?:\\s*(?:/|per)\\s*(?:thn|tahun|bln|bulan))?)")

    val jualMatch = jualRegex.find(combined)
    val sewaMatch = sewaRegex.find(combined)

    var salePriceFormatted: String? = null
    var rentPriceFormatted: String? = null

    if (jualMatch != null) {
        val rawSale = jualMatch.groupValues[1].trim()
        if (rawSale.isNotBlank()) {
            salePriceFormatted = formatSinglePriceCompact(rawSale).replace("Harga ", "")
        }
    }
    
    if (sewaMatch != null) {
        val rawRent = sewaMatch.groupValues[1].trim()
        if (rawRent.isNotBlank()) {
            rentPriceFormatted = formatSinglePriceCompact(rawRent).replace("Harga ", "")
        }
    }

    // 2. If explicit Jual & Sewa matched
    if (salePriceFormatted != null && rentPriceFormatted != null) {
        return "Harga : $salePriceFormatted\nHarga Sewa : $rentPriceFormatted"
    }
    if (salePriceFormatted != null && rentPriceFormatted == null && !rawPrice.contains("sewa", ignoreCase = true) && !rawDesc.contains("sewa", ignoreCase = true)) {
        return "Harga : $salePriceFormatted"
    }
    if (rentPriceFormatted != null && salePriceFormatted == null) {
        return "Harga Sewa : $rentPriceFormatted"
    }

    // 3. Fallback: Parse multiple price values from rawPrice or combined text
    val rpRegex = """(?:Rp\.?\s*)?[0-9\.,]+\s*(?:Milyar|Miliar|M|Juta|Jt)(?:\s*(?:/|per)\s*(?:Thn|Tahun|Bln|Bulan|m2|m²|meter))?""".toRegex(RegexOption.IGNORE_CASE)
    val matches = rpRegex.findAll(rawPrice.ifBlank { rawDesc }).map { it.value.trim() }.distinct().toList()

    if (matches.size >= 2) {
        var saleStr: String? = null
        var rentStr: String? = null
        for (m in matches) {
            val valNum = parsePriceStringToValue(m)
            if (isRentPriceValue(valNum, m)) {
                rentStr = formatSinglePriceCompact(m).replace("Harga ", "").replace("Harga", "").trim()
            } else {
                saleStr = formatSinglePriceCompact(m).replace("Harga ", "").replace("Harga", "").trim()
            }
        }
        if (saleStr != null && rentStr != null) {
            return "Harga : $saleStr\nHarga Sewa : $rentStr"
        }
        if (matches.size > 1) {
            return matches.map { formatSinglePriceCompact(it) }.joinToString(" / ")
        }
    }

    if (matches.size == 1) {
        var single = formatSinglePriceCompact(matches.first())
        if ((isLandProperty || isPerMeterPrice) && !single.contains("/ m2") && !single.contains("/m2")) {
            single += " / m2"
        }
        val valNum = parsePriceStringToValue(matches.first())
        return if (isRentPriceValue(valNum, matches.first()) || rawPrice.contains("sewa", ignoreCase = true) || rawDesc.contains("sewa", ignoreCase = true)) {
            val cleanSingle = single.replace("Harga ", "").replace("Harga", "").trim()
            "Harga Sewa : $cleanSingle"
        } else {
            val cleanSingle = single.replace("Harga ", "").replace("Harga", "").trim()
            "Harga : $cleanSingle"
        }
    }

    var fallback = formatPriceCompact(rawPrice.ifBlank { "Hubungi Agent" })
    if ((isLandProperty || isPerMeterPrice) && !fallback.contains("/ m2") && !fallback.contains("/m2") && !fallback.contains("Hubungi")) {
        fallback += " / m2"
    }
    return if (fallback.startsWith("Harga ")) fallback.replace("Harga ", "Harga : ") else "Harga : $fallback"
}

private fun formatPriceCompact(raw: String): String {
    if (raw.isBlank()) return "Harga Hubungi Agent"
    
    val rpRegex = """Rp\.?\s*[0-9\.,]+(?:\s*(?:Milyar|M|Juta|J|Tahun|Thn|Bulan|Bln|m2|m²|meter))?(?:\s*(?:/|per)\s*(?:Thn|Tahun|Bln|Bulan|m2|m²|meter))?""".toRegex(RegexOption.IGNORE_CASE)
    val matches = rpRegex.findAll(raw).map { it.value.trim() }.toList()
    
    if (matches.size > 1) {
        return matches.map { formatSinglePriceCompact(it) }.joinToString(" / ")
    } else {
        return formatSinglePriceCompact(raw)
    }
}

private fun formatSinglePriceCompact(raw: String): String {
    val clean = raw.replace("(?i)harga jual".toRegex(), "").replace("(?i)harga".toRegex(), "").replace(":", "").trim()
    val lower = raw.lowercase()

    val isPerMeter = lower.contains("/ m2") || lower.contains("/m2") || 
                    lower.contains("/ m²") || lower.contains("/m²") || 
                    lower.contains("per m2") || lower.contains("per m²") || 
                    lower.contains("per meter") || lower.contains("/ meter")

    val suffix = when {
        lower.contains("tahun") || lower.contains("thn") || lower.contains("/th") -> " / Thn"
        lower.contains("bulan") || lower.contains("bln") || lower.contains("/bln") -> " / Bln"
        isPerMeter -> " / m2"
        else -> ""
    }

    val digitsOnly = clean.replace("[^\\d]".toRegex(), "")
    if (digitsOnly.length >= 7) {
        val longVal = digitsOnly.toLongOrNull()
        if (longVal != null) {
            if (longVal >= 1_000_000_000L) {
                val mVal = longVal.toDouble() / 1_000_000_000.0
                val formatted = if (mVal % 1.0 == 0.0) mVal.toInt().toString() else String.format(java.util.Locale.US, "%.2f", mVal).trimEnd('0').trimEnd('.')
                return "Harga Rp $formatted M$suffix"
            } else if (longVal >= 1_000_000L) {
                val jtVal = longVal.toDouble() / 1_000_000.0
                val formatted = if (jtVal % 1.0 == 0.0) jtVal.toInt().toString() else String.format(java.util.Locale.US, "%.2f", jtVal).trimEnd('0').trimEnd('.')
                return "Harga Rp $formatted Jt$suffix"
            }
        }
    }

    if (clean.contains("M", ignoreCase = true) || clean.contains("Jt", ignoreCase = true) || clean.contains("Miliar", ignoreCase = true)) {
        var cleanM = clean.uppercase()
            .replace("MILAR", "M").replace("MILIAR", "M").replace("MILYAR", "M").replace("JUTA", "Jt")
            .replace("PER METER", "").replace("PER M2", "").replace("PER M²", "")
            .replace("/ M2", "").replace("/M2", "").replace("/ M²", "").replace("/M²", "")
            .replace("/ METER", "").replace("/METER", "").trim()
            
        if (!cleanM.startsWith("Rp", ignoreCase = true)) {
            cleanM = "Rp $cleanM"
        }
        cleanM = cleanM.replace("JT", "Jt")
        val hasSuffix = cleanM.contains("/ THN") || cleanM.contains("/ BLN") || cleanM.contains("/ M2")
        val finalSuffix = if (hasSuffix) "" else suffix
        return "Harga $cleanM$finalSuffix"
    }

    val hasSuffix = clean.lowercase().contains("/ m2") || clean.lowercase().contains("/m2") || clean.lowercase().contains("/ thn") || clean.lowercase().contains("/ bln")
    val finalSuffix = if (hasSuffix) "" else suffix
    return "Harga $clean$finalSuffix"
        .replace("(?i)\\bper\\s*meter\\b".toRegex(), "/ m2")
        .replace("(?i)\\bper\\s*m2\\b".toRegex(), "/ m2")
        .replace("(?i)\\bper\\s*m²\\b".toRegex(), "/ m2")
        .replace("(?i)/\\s*meter\\b".toRegex(), "/ m2")
}

fun isStatusTagText(text: String): Boolean {
    val clean = text.trim().uppercase()
    return clean == "IG" || clean == "HOT PROPERTY" || clean == "FOTO ULANG" ||
           clean == "IG & HOT PROPERTY" || clean == "DONE" || clean == "PENDING" ||
           clean == "UP FOTO" || clean == "EDIT VIDEO" || clean == "GARIS TANAH" ||
           clean.startsWith("IG ") || clean.endsWith(" IG") || clean == "AKSI"
}

val LOCATION_BLACKLIST = setOf(
    "PEMBANTU", "UTAMA", "KOSONG", "LANTAI", "RUMAH", "TANAH", "HARGA", "DEKAT", "LOKASI",
    "FOTO", "JUAL", "SEWA", "READY", "LENGKAP", "BEBAS", "BANJIR", "STRATEGIS", "MINIMALIS",
    "HOEK", "HOOK", "DAPUR", "KAMAR", "MANDI", "TIDUR", "GARASI", "CARPORT", "TAMAN",
    "KAMPUS", "STASIUN", "AKSES", "TOL", "JALAN", "GANG", "BLOK", "NOMOR", "NO", "MILIK",
    "SHM", "IMB", "LISTRIK", "AIR", "PAM", "JETPUMP", "POOL", "KOLAM", "RENANG", "BARU"
)

fun extractPropertyLocation(descLower: String, titleLower: String, scrapedTitleLower: String, sheetLokasi: String = ""): String {
    val cleanSheet = sheetLokasi.trim().uppercase()
    if (cleanSheet.isNotBlank() && !isStatusTagText(cleanSheet) && cleanSheet != "UNKNOWN" && cleanSheet !in LOCATION_BLACKLIST) {
        return cleanSheet
    }

    for (loc in SORTED_LOCATIONS) {
        if (titleLower.contains(loc) || scrapedTitleLower.contains(loc)) {
            return loc.uppercase()
        }
    }

    for (loc in SORTED_LOCATIONS) {
        if (descLower.contains(loc)) {
            return loc.uppercase()
        }
    }

    // Regex match for "di [Kota]", "daerah [Kota]", "lokasi [Kota]", "area [Kota]"
    val diPattern = """(?:di|daerah|lokasi|kawasan|area|kota|kab\.?)\s+([A-Za-z]+(?:\s+[A-Za-z]+)?)""".toRegex(RegexOption.IGNORE_CASE)
    val diMatch = diPattern.find("$titleLower $scrapedTitleLower $descLower")
    if (diMatch != null) {
        val candidate = diMatch.groupValues[1].trim().uppercase()
        val isBlacklisted = LOCATION_BLACKLIST.any { candidate.contains(it) }
        if (candidate.length > 2 && !isStatusTagText(candidate) && !isBlacklisted) {
            return candidate
        }
    }

    if (cleanSheet.isNotBlank() && cleanSheet !in LOCATION_BLACKLIST) return cleanSheet
    return "INDONESIA"
}

private val SORTED_LOCATIONS = listOf(
    // Bandung & West Java
    "bandung kota", "bandung barat", "bandung selatan", "bandung timur", "bandung utara", "bandung",
    "lembang", "dago pakar", "dago", "pasteur", "cimahi", "buahnaga", "cibiru", "buahbatu", "setiabudi bandung",
    "sukajadi", "coblong", "sumur bandung", "cibeunying", "kiaracondong", "arcamanik", "gedebage",
    "sumedang", "garut", "tasikmalaya", "cirebon", "sukabumi", "cianjur", "purwakarta", "subang", "indramayu", "kuningan", "majalengka",
    // South Jakarta (Jakarta Selatan)
    "tb simatupang", "simatupang", "kebagusan", "cilandak", "cipete", "kemang", "jagakarsa", "pondok indah", "ampera", "kebayoran baru", "kebayoran lama", "kebayoran", "senopati", "bintaro", "tebet", "pejaten", "cilodong", "pasar minggu", "gandaria", "mampang prapatan", "mampang", "pancoran", "setiabudi", "kalibata", "ciganjur", "lenteng agung", "ragunan", "tanjung barat", "pesanggrahan", "cipulir", "pondok pinang", "lebak bulus", "fatmawati", "blok m", "radio dalam", "dharmawangsa", "darmawangsa", "panglima polim", "permata hijau", "senayan", "sudirman", "kuningan", "menteng", "prapanca", "wijaya", "cipete dalam", "cipete utara", "cipete selatan", "gandaria utara", "gandaria selatan", "pondok labu", "petukangan", "ulujami", "kebon baru", "manggarai", "pasar manggis", "karet semanggi", "karet pedurenan", "karet tengsin", "karet", "gatot subroto", "gatsu", "rasuna said", "mega kuningan", "scbd", "tebet barat", "tebet timur", "menteng dalam", "pengadegan", "pejaten barat", "pejaten timur", "jatipadang", "buncit", "warung buncit", "duren tiga", "bangka", "tendean", "kapten tendean", "petogogan", "melawai", "pulo", "cipulo", "kebayoran lama utara", "kebayoran lama selatan", "cilandak barat", "cilandak timur", "tanah kusir",
    // East Jakarta (Jakarta Timur)
    "cipinang melayu", "cipinang indah", "cipinang elok", "cipinang muara", "cipinang cempedak", "cipinang besar", "cipinang", "rawamangun", "duren sawit", "pulomas", "ciracas", "kramat jati", "makasar", "matraman", "pasar rebo", "cakung", "cipayung", "jatinegara", "kayu putih", "pondok kelapa", "pondok bambu", "klender", "condet", "halim", "cililitan",
    // Depok & Bogor
    "cinere", "depok", "sawangan", "margonda", "cimanggis", "limo", "beji", "pancoran mas", "sentul", "bogor", "cibubur", "bedahan", "beji timur", "gandul", "pangkalan jati", "krukut", "meruyung", "grogol", "mampang depok", "depok jaya", "sukmajaya", "tapos", "harjamukti", "bojonggede", "citayam", "sentul city", "tanah sareal", "bogor utara", "bogor selatan", "bogor timur", "bogor barat",
    // Tangerang / South Tangerang (Tangerang Selatan)
    "bsd city", "bsd", "serpong", "alam sutera", "gading serpong", "karawaci", "ciputat", "pamulang", "bintaro jaya", "ciledug", "tangerang", "serpong utara", "bintaro sektor 1", "bintaro sektor 2", "bintaro sektor 3", "bintaro sektor 4", "bintaro sektor 5", "bintaro sektor 6", "bintaro sektor 7", "bintaro sektor 8", "bintaro sektor 9", "graha raya", "pondok cabe", "cirendeu", "rempoa", "jombang", "sawah baru", "serua", "setu", "cisauk", "pagedangan", "legok", "curug", "cikokol", "tangerang kota", "larangan", "pondok aren",
    // West Jakarta (Jakarta Barat)
    "puri indah", "kembangan", "kebon jeruk", "meruya", "tanjung duren", "tomang", "grogol", "slipi", "palmerah", "kalideres", "cengkareng", "meruya utara", "meruya selatan", "kembangan utara", "kembangan selatan", "permata buana", "taman aries", "intercon", "semesta", "kemanggisan", "jelambar", "kapuk",
    // Central Jakarta (Jakarta Pusat)
    "salemba", "tanah abang", "gambir", "kemayoran", "cempaka putih", "sawah besar", "cikini", "gondangdia", "senen", "benhil", "bendungan hilir", "petamburan",
    // North Jakarta (Jakarta Utara)
    "pantai indah kapuk", "pik", "kelapa gading", "pluit", "sunter", "ancol", "cilincing", "koja", "pademangan", "penjaringan", "pik 2", "muara karang",
    // Bekasi
    "jatiasih", "tambun", "cikarang", "harapan indah", "summarecon bekasi", "bekasi", "grand wisata", "galaxy", "taman galaxy", "kemang pratama", "jatibening", "pondok gede",
    // Central Java & Yogyakarta
    "yogyakarta", "jogja", "semarang", "solo", "surakarta", "magelang", "purwokerto", "kudus", "pati", "tegal", "pekalongan", "klaten", "boyolali", "wonogiri",
    // East Java
    "surabaya", "malang", "batu", "sidoarjo", "gresik", "jember", "banyuwangi", "kediri", "blitar", "mojokerto", "madiun", "tuban", "lamongan", "pasuruan", "probolinggo",
    // Bali & Nusa Tenggara
    "bali", "denpasar", "badung", "seminyak", "canggu", "ubud", "sanur", "kuta", "nusa dua", "jimbaran", "uluwatu", "gianyar", "tabanan", "lombok", "mataram",
    // Sumatra
    "medan", "palembang", "pekanbaru", "batam", "padang", "bandar lampung", "jambi", "bengkulu", "aceh",
    // Sulawesi, Kalimantan & Eastern Indonesia
    "makassar", "manado", "palu", "kendari", "samarinda", "balikpapan", "pontianak", "banjarmasin", "palangkaraya", "jayapura"
).sortedByDescending { it.length }
