package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaBlastPostDialog(
    initialCaption: String,
    downloadedUris: List<Uri>,
    listingId: String = "",
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val captionFocusRequester = remember { FocusRequester() }

    // Remove "raywhitecipete " prefix from editor content
    val cleanInitialCaption = remember(initialCaption) {
        initialCaption.removePrefix("raywhitecipete ").trim()
    }
    var captionText by remember { mutableStateOf(cleanInitialCaption) }

    val pagerState = rememberPagerState(initialPage = 0) { downloadedUris.size.coerceAtLeast(1) }
    val cleanId = remember(listingId) { listingId.replace("[^0-9]".toRegex(), "") }

    // Auto-focus description field when dialog opens
    LaunchedEffect(Unit) {
        delay(300)
        try {
            captionFocusRequester.requestFocus()
            keyboardController?.show()
        } catch (e: Exception) {}
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.94f)
                .padding(8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Foto Ready • Blast to WA",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Edit deskripsi & kirim foto langsung ke WhatsApp",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Scrollable main content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Slidable Image Carousel with Original Aspect Ratio (Full-Bleed 3:4)
                    if (downloadedUris.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(3f / 4f)
                                .clip(RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    AsyncImage(
                                        model = downloadedUris[page],
                                        contentDescription = "Preview Foto ${page + 1}",
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                // Carousel Badge (e.g. Foto 1 dari 4)
                                Surface(
                                    color = Color.Black.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        text = "${pagerState.currentPage + 1} / ${downloadedUris.size}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }

                                // Status Badge
                                Surface(
                                    color = Color(0xFF4CAF50),
                                    shape = RoundedCornerShape(bottomEnd = 10.dp),
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Text(
                                        text = "✓ ${downloadedUris.size} Foto Terunduh",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Description Text Editor Title
                    Text(
                        text = "Deskripsi Instagram Editor",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Warning Alert Banner
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                            Text(
                                text = "Warning! Please Double Check The Descriptions before Blasting.",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            )
                        }
                    }

                    // Expanded Text Editor Field (no nested scroll box)
                    OutlinedTextField(
                        value = captionText,
                        onValueChange = { captionText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp)
                            .focusRequester(captionFocusRequester),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons Row: Blast to WA + Detail Listing
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Blast to WA / Share Button
                    Button(
                        onClick = {
                            sendToWhatsAppChooser(context, captionText, downloadedUris)
                        },
                        modifier = Modifier
                            .weight(1.3f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Blast to WA / Share",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }

                    // 2. Detail Listing Button
                    Button(
                        onClick = {
                            if (cleanId.isNotBlank()) {
                                try {
                                    val url = "https://raywhitecipete.net/ListingView/Detail/$cleanId"
                                    val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(webIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Gagal membuka web: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "ID Listing tidak tersedia", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262626))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Detail Listing",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                    }
                }
            }
        }
    }
}

private fun sendToWhatsAppChooser(
    context: Context,
    text: String,
    imageUris: List<Uri>
) {
    try {
        val sendIntent = Intent().apply {
            action = if (imageUris.size > 1) Intent.ACTION_SEND_MULTIPLE else Intent.ACTION_SEND
            type = if (imageUris.isNotEmpty()) "image/*" else "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (imageUris.isNotEmpty()) {
                if (imageUris.size > 1) {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(imageUris))
                } else {
                    putExtra(Intent.EXTRA_STREAM, imageUris[0])
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        val chooserIntent = Intent.createChooser(sendIntent, "Pilih Aplikasi WhatsApp")
        context.startActivity(chooserIntent)
    } catch (e: Exception) {
        Toast.makeText(context, "Gagal menguji intent WhatsApp: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}
