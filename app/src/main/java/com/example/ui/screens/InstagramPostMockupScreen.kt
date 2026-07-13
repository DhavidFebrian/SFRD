package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
                                    text = details["lokasi"]?.uppercase() ?: "CILANDAK",
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

                    // IG Icons Row directly below image
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Like Icon button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    isLiked = !isLiked
                                    if (isLiked) likeCount++ else likeCount--
                                }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Like",
                                tint = if (isLiked) Color.Red else Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$likeCount",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Comment Icon button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    isCommented = !isCommented
                                    if (isCommented) commentCount++ else commentCount--
                                }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ChatBubbleOutline,
                                contentDescription = "Komen",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$commentCount",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Repost / Retweet Icon button
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    isReposted = !isReposted
                                    if (isReposted) repostCount++ else repostCount--
                                }
                                .padding(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Autorenew,
                                contentDescription = "Repost",
                                tint = if (isReposted) Color(0xFF4CAF50) else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$repostCount",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Share / WA Icon button (Paper plane icon)
                        IconButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, captionText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Bagikan ke WhatsApp"))
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Share WA",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Bookmark Save Icon button
                        IconButton(
                            onClick = { isSaved = !isSaved },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Save",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Caption details section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$captionText less",
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 18.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Action buttons row (Copy text, Detail Listing)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Copy Description button
                        Button(
                            onClick = {
                                val copyableText = captionText.removePrefix("raywhitecipete ").trim()
                                clipboardManager.setText(AnnotatedString(copyableText))
                                Toast.makeText(context, "Deskripsi berhasil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF262626) // Soft IG gray-black button
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Salin Teks", color = Color.White, fontSize = 12.sp)
                        }

                        // Detail Listing button
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
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF262626)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Detail Listing", color = Color.White, fontSize = 12.sp)
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
    var lokasiVal = "CILANDAK"
    val locations = listOf("cilandak", "cipete", "kemang", "jagakarsa", "pondok indah", "ampera", "kebayoran", "senopati", "bintaro", "tebet", "pejaten", "cilodong")
    for (loc in locations) {
        if (descLower.contains(loc) || title.lowercase().contains(loc) || scrapedTitle.lowercase().contains(loc)) {
            lokasiVal = loc.uppercase()
            break
        }
    }
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

// Select a valid property title, ensuring it's never just numbers
private fun selectPropertyTitle(scrapedTitle: String, judulTask: String, cleanDesc: String, lokasi: String): String {
    var title = scrapedTitle.replace("<[^>]*>".toRegex(), "").trim()
    if (title.isNotBlank() && !isLineJustNumbersOrStats(title)) {
        return title
    }
    title = judulTask.replace("<[^>]*>".toRegex(), "").trim()
    if (title.isNotBlank() && !isLineJustNumbersOrStats(title)) {
        return title
    }
    val fallbackLines = cleanDesc.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    for (line in fallbackLines) {
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
    
    // Also remove trailing commas or semicolons
    while (cleanedLine.endsWith(",") || cleanedLine.endsWith(";")) {
        cleanedLine = cleanedLine.substring(0, cleanedLine.length - 1).trim()
    }
    
    val lower = cleanedLine.lowercase()
    
    // Check for combined LT/LB line (e.g. LT/LB 936/600)
    val combinedLtLbRegex = Regex("(?i)\\b(?:lt\\s*/\\s*lb|luas\\s*tanah\\s*/\\s*luas\\s*bangunan|lt\\s*-\\s*lb)\\s*[:\\s-]*(\\d+)\\s*[\\s/-]+\\s*(\\d+)")
    val combinedLbLtRegex = Regex("(?i)\\b(?:lb\\s*/\\s*lt|luas\\s*bangunan\\s*/\\s*luas\\s*tanah|lb\\s*-\\s*lt)\\s*[:\\s-]*(\\d+)\\s*[\\s/-]+\\s*(\\d+)")
    
    val combinedLtLbMatch = combinedLtLbRegex.find(lower)
    if (combinedLtLbMatch != null) {
        val ltVal = combinedLtLbMatch.groupValues[1]
        val lbVal = combinedLtLbMatch.groupValues[2]
        return "• LT $ltVal m2\n• LB $lbVal m2"
    }
    
    val combinedLbLtMatch = combinedLbLtRegex.find(lower)
    if (combinedLbLtMatch != null) {
        val lbVal = combinedLbLtMatch.groupValues[1]
        val ltVal = combinedLbLtMatch.groupValues[2]
        return "• LT $ltVal m2\n• LB $lbVal m2"
    }
    
    // 1. Luas Tanah (LT)
    if (lower.startsWith("luas tanah") || lower.startsWith("lt")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)luas\\s*tanah".toRegex(), "").replace("(?i)lt".toRegex(), "").trim()
        }
        val digits = value.replace("[^\\d\\s\\+\\.,mMyY2²]".toRegex(), "").trim()
        val displayVal = if (digits.isBlank()) "275" else digits
        var formattedVal = displayVal.replace("(?i)m2".toRegex(), "m2").replace("(?i)m²".toRegex(), "m2")
        if (!formattedVal.lowercase().endsWith("m2")) {
            formattedVal = "$formattedVal m2"
        }
        formattedVal = formattedVal.replace("\\s+".toRegex(), " ")
        return "• LT $formattedVal"
    }
    
    // 2. Luas Bangunan (LB)
    if (lower.startsWith("luas bangunan") || lower.startsWith("lb")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)luas\\s*bangunan".toRegex(), "").replace("(?i)lb".toRegex(), "").trim()
        }
        val digits = value.replace("[^\\d\\s\\+\\.,mMyY2²]".toRegex(), "").trim()
        val displayVal = if (digits.isBlank()) "360" else digits
        var formattedVal = displayVal.replace("(?i)m2".toRegex(), "m2").replace("(?i)m²".toRegex(), "m2")
        if (!formattedVal.lowercase().endsWith("m2")) {
            formattedVal = "$formattedVal m2"
        }
        formattedVal = formattedVal.replace("\\s+".toRegex(), " ")
        return "• LB $formattedVal"
    }
    
    // 3. Kamar Tidur (KT)
    if (lower.startsWith("kamar tidur") || lower.startsWith("kt")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)kamar\\s*tidur".toRegex(), "").replace("(?i)kt".toRegex(), "").trim()
        }
        val formatted = value.replace("\\s+".toRegex(), " ")
        return "• KT ${formatted.ifBlank { "4+1" }}"
    }
    
    // 4. Kamar Mandi (KM)
    if (lower.startsWith("kamar mandi") || lower.startsWith("km")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)kamar\\s*mandi".toRegex(), "").replace("(?i)km".toRegex(), "").trim()
        }
        val formatted = value.replace("\\s+".toRegex(), " ")
        return "• KM ${formatted.ifBlank { "3+1" }}"
    }
    
    // 5. Garasi
    if (lower.startsWith("garasi")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)garasi".toRegex(), "").trim()
        }
        val digits = value.replace("[^\\d\\s\\+]".toRegex(), "").trim()
        return "• Garasi ${digits.ifBlank { "1" }} Mobil"
    }
    
    // 6. Carport
    if (lower.startsWith("carport")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)carport".toRegex(), "").trim()
        }
        val digits = value.replace("[^\\d\\s\\+]".toRegex(), "").trim()
        return "• Carport ${digits.ifBlank { "2" }} Mobil"
    }
    
    // 7. Sertifikat
    if (lower.startsWith("sertifikat") || lower.startsWith("shm")) {
        var value = cleanedLine.substringAfter(":").trim()
        if (value.isBlank() || value == cleanedLine) {
            value = cleanedLine.replace("(?i)sertifikat".toRegex(), "").trim()
        }
        val upperVal = value.uppercase()
        if (upperVal.contains("SHM") || upperVal.contains("HAK MILIK") || upperVal.isBlank()) {
            return "• SHM & IMB Lengkap"
        }
        return "• Sertifikat $value"
    }
    
    // Fallback: capitalize words nicely
    val words = cleanedLine.split("\\s+".toRegex()).map { word ->
        if (word.lowercase() == "dan" || word.lowercase() == "di" || word.lowercase() == "area" || word.lowercase() == "ke") {
            word.lowercase()
        } else {
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
        }
    }.joinToString(" ")
    
    return "• $words"
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
    // 1. Clean HTML
    val clean = rawDesc
        .replace("(?i)<br\\s*/?>".toRegex(), "\n")
        .replace("(?i)</p>".toRegex(), "\n")
        .replace("<[^>]*>".toRegex(), "")
        .trim()

    // Find location for title generation if needed
    var lokasiVal = "CILANDAK"
    val locations = listOf("cilandak", "cipete", "kemang", "jagakarsa", "pondok indah", "ampera", "kebayoran", "senopati", "bintaro", "tebet", "pejaten", "cilodong")
    val descLower = clean.lowercase()
    for (loc in locations) {
        if (descLower.contains(loc) || judulTask.lowercase().contains(loc) || scrapedTitle.lowercase().contains(loc)) {
            lokasiVal = loc.uppercase()
            break
        }
    }

    // 2. Select beautiful title, avoiding pure stats or numbers
    val title = selectPropertyTitle(scrapedTitle, judulTask, clean, lokasiVal)

    // Contact info
    val contactsStr = getInstagramCaptionContacts(namaMe)

    // Check if "Deskripsi Lengkap:" exists or fallback to standard specs
    val deskripsiLengkapIndex = clean.indexOf("Deskripsi Lengkap:", ignoreCase = true)
    val withSpecIndex = clean.indexOf("Dengan Spek Sebagai Berikut", ignoreCase = true)
    
    val targetText = if (deskripsiLengkapIndex != -1) {
        clean.substring(deskripsiLengkapIndex + "Deskripsi Lengkap:".length).trim()
    } else if (withSpecIndex != -1) {
        val afterSpec = clean.substring(withSpecIndex + "Dengan Spek Sebagai Berikut".length).trim()
        if (afterSpec.startsWith(":")) {
            afterSpec.substring(1).trim()
        } else {
            afterSpec
        }
    } else {
        clean
    }

    val rawLines = targetText.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
    val processedLines = mutableListOf<String>()
    for (line in rawLines) {
        if (line.contains(",")) {
            val lowerLine = line.lowercase()
            val containsMultipleSpecs = (
                (lowerLine.contains("luas tanah") || lowerLine.contains("lt")) && (lowerLine.contains("luas bangunan") || lowerLine.contains("lb"))
            ) || (
                (lowerLine.contains("kt") || lowerLine.contains("kamar tidur")) && (lowerLine.contains("km") || lowerLine.contains("kamar mandi"))
            ) || (
                lowerLine.contains(Regex("\\bkt\\b|\\bkm\\b|\\blt\\b|\\blb\\b")) && lowerLine.count { it == ',' } >= 1
            )
            
            if (containsMultipleSpecs) {
                val parts = line.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                processedLines.addAll(parts)
            } else {
                processedLines.add(line)
            }
        } else {
            processedLines.add(line)
        }
    }

    val parsedBulletPoints = mutableListOf<String>()
    
    var stopParsing = false
    for (line in processedLines) {
        if (stopParsing) break
        
        val lower = line.lowercase()
        
        // Stop indicators: only links, copyright, or agent/broker promo sections
        if (lower.contains("http://") || lower.contains("https://") ||
            lower.contains("wa.me") || lower.contains("copyright") ||
            lower.contains("ray white") || lower.contains("hubungi kami") ||
            lower.contains("contact us")
        ) {
            stopParsing = true
            continue
        }
        
        // Skip headers, duplicates, etc.
        if (lower.startsWith("dengan spek") || 
            lower.startsWith("listing id:") || 
            lower.contains("deskripsi lengkap") ||
            lower.contains("for sale") ||
            lower.contains("hubungi") ||
            lower.contains("contact") ||
            lower.startsWith("harga") ||
            line.equals(title, ignoreCase = true) ||
            isLineJustNumbersOrStats(line) ||
            line.matches("^\\d+$".toRegex()) // ignores single ID number line
        ) {
            continue
        }
        
        val formatted = formatBulletPoint(line)
        if (formatted.isNotEmpty()) {
            parsedBulletPoints.add(formatted)
        }
    }

    // Fallback if parsing didn't find any bullet points (extremely safe)
    if (parsedBulletPoints.isEmpty()) {
        val details = parsePropertyDetails(rawDesc, idListing, rawPrice, judulTask, scrapedTitle)
        parsedBulletPoints.add("• LT ${details["lt"]} m2")
        parsedBulletPoints.add("• LB ${details["lb"]} m2")
        parsedBulletPoints.add("• KT ${details["kt"]}")
        parsedBulletPoints.add("• KM ${details["km"]}")
        parsedBulletPoints.add("• Garasi ${details["garasi"]}")
        parsedBulletPoints.add("• Carport ${details["carport"]}")
        parsedBulletPoints.add("• ${details["swimming_pool"]}")
        parsedBulletPoints.add("• ${details["security"]}")
        parsedBulletPoints.add("• ${details["sertifikat"]}")
    }

    // Build final caption
    val category = getListingCategory(rawPrice)
    return buildString {
        append("raywhitecipete $category ID $idListing\n\n")
        append("$title\n\n")
        
        parsedBulletPoints.forEach { append("$it\n") }
        append("\n")
        
        val displayPrice = formatPriceCompact(rawPrice.ifBlank { "Hubungi Agent" })
        append("$displayPrice Nego\n\n")
        
        append(contactsStr)
        append("DM US FOR MORE INFORMATION")
    }
}

private fun getInstagramCaptionContacts(namaMe: String): String {
    val names = namaMe.split(Regex("\\s*(?:&|\\bdan\\b|\\band\\b|/|,)\\s*", RegexOption.IGNORE_CASE)).map { it.trim() }.filter { it.isNotEmpty() }
    if (names.isEmpty()) {
        return "CONTACT\nILHAM: 08561103735/@ilhamsraywhite\n\n"
    }
    return buildString {
        append("CONTACT\n")
        names.forEach { name ->
            val contact = com.example.ui.findContact(name)
            if (contact != null) {
                val igHandle = if (contact.instagram.isNotBlank()) {
                    val cleanIg = contact.instagram.removePrefix("@")
                    "/@$cleanIg"
                } else {
                    ""
                }
                val contactName = contact.nameKey.uppercase()
                append("$contactName: ${contact.phone}$igHandle\n")
            } else {
                append("${name.uppercase()}: Hubungi Agent\n")
            }
        }
        append("\n")
    }
}

private fun parsePriceStringToValue(priceStr: String): Long {
    val lower = priceStr.lowercase().trim()
    
    // Extract only decimal numbers and suffixes
    val clean = lower.replace("rp\\.?".toRegex(), "").trim()
    
    // Find numeric part (e.g. "1.5", "150", "1,2")
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

private fun getListingCategory(rawPrice: String): String {
    if (rawPrice.isBlank()) return "[FOR SALE]"
    
    val priceParts = rawPrice.split(Regex("\\s*/\\s*|\\s+dan\\s+|\\s*&\\s*")).filter { it.isNotBlank() }
    
    if (priceParts.size > 1) {
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
        if (hasRent && hasSale) {
            return "[FOR SALE / RENT]"
        }
    }
    
    val value = parsePriceStringToValue(rawPrice)
    if (isRentPriceValue(value, rawPrice)) {
        return "[FOR RENT]"
    } else {
        return "[FOR SALE]"
    }
}

private fun formatPriceCompact(raw: String): String {
    if (raw.isBlank()) return "Harga Hubungi Agent"
    
    val rpRegex = """Rp\.?\s*[0-9\.,]+(?:\s*(?:Milyar|M|Juta|J|Tahun|Thn|Bulan|Bln))?""".toRegex(RegexOption.IGNORE_CASE)
    val matches = rpRegex.findAll(raw).map { it.value.trim() }.toList()
    
    if (matches.size > 1) {
        return matches.map { formatSinglePriceCompact(it) }.joinToString(" / ")
    } else {
        return formatSinglePriceCompact(raw)
    }
}

private fun formatSinglePriceCompact(raw: String): String {
    val clean = raw.replace("(?i)harga jual".toRegex(), "").replace("(?i)harga".toRegex(), "").replace(":", "").trim()
    val digitsOnly = clean.replace("[^\\d]".toRegex(), "")
    if (digitsOnly.length >= 7) {
        val longVal = digitsOnly.toLongOrNull()
        if (longVal != null) {
            val suffix = if (clean.contains("tahun", ignoreCase = true) || clean.contains("thn", ignoreCase = true)) {
                " / Thn"
            } else if (clean.contains("bulan", ignoreCase = true) || clean.contains("bln", ignoreCase = true)) {
                " / Bln"
            } else {
                ""
            }
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
        val cleanM = clean.uppercase().replace("MILAR", "M").replace("MILIAR", "M").replace("MILYAR", "M").replace("JUTA", "Jt")
        return "Harga ${if (!cleanM.startsWith("Rp")) "Rp " else ""}$cleanM"
    }
    return "Harga $clean"
}
