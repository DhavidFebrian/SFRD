package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.util.RwcListingImage
import com.example.util.RwcSocialMediaDownloader

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RwcDesign3DownloadDialog(
    listingId: String,
    initialCoverTitle: String,
    onDismiss: () -> Unit,
    onDownloadSuccess: (List<Uri>, String) -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var isLoading by remember { mutableStateOf(true) }
    var isDownloading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Menghubungkan ke portal Ray White...") }

    var images by remember { mutableStateOf<List<RwcListingImage>>(emptyList()) }
    var selectedCoverId by remember { mutableStateOf("") }
    val selectedDownloadIds = remember { mutableStateListOf<String>() }
    var coverTitle by remember { mutableStateOf(initialCoverTitle) }
    var previewFullscreenIndex by remember { mutableStateOf<Int?>(null) }

    // Fetch images on open
    LaunchedEffect(listingId) {
        RwcSocialMediaDownloader.loginAndFetchImages(
            context = context,
            listingId = listingId,
            callback = object : RwcSocialMediaDownloader.FetchImagesCallback {
                override fun onProgress(message: String) {
                    statusMessage = message
                }

                override fun onSuccess(fetchedImages: List<RwcListingImage>) {
                    images = fetchedImages
                    selectedDownloadIds.clear()
                    if (fetchedImages.isNotEmpty()) {
                        selectedCoverId = fetchedImages[0].id
                        selectedDownloadIds.clear()
                        selectedDownloadIds.add(fetchedImages[0].id) // Default centang foto depan (cover) saja
                    }
                    isLoading = false
                }

                override fun onFailure(error: String) {
                    isLoading = false
                    Toast.makeText(context, "✗ $error", Toast.LENGTH_LONG).show()
                    onDismiss()
                }
            }
        )
    }

    // Full screen Hold-to-Preview dialog
    previewFullscreenIndex?.let { index ->
        FullScreenImagePagerViewer(
            images = images.map { it.imageUrl },
            initialIndex = index,
            title = "Preview Foto Listing",
            subtitle = "ID #$listingId • Hold to Preview",
            onDismiss = { previewFullscreenIndex = null }
        )
    }

    // Auto-focus Title Input (Headline 1) and open keyboard when loaded
    LaunchedEffect(isLoading) {
        if (!isLoading) {
            delay(250)
            try {
                focusRequester.requestFocus()
                keyboardController?.show()
            } catch (e: Exception) {}
        }
    }

    Dialog(
        onDismissRequest = { if (!isDownloading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
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
                            text = "Download Foto Listing",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Portal RWC • Listing ID $listingId",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onDismiss, enabled = !isDownloading) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup")
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                if (isLoading || isDownloading) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // Title Input (Headline 1)
                        OutlinedTextField(
                            value = coverTitle,
                            onValueChange = { coverTitle = it },
                            label = { Text("Judul Cover (Headline 1)") },
                            placeholder = { Text("Masukkan Judul Cover...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Pilih Foto & Cover:",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Hold foto untuk preview besar • Cover bersifat opsional",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            val totalImages = images.size
                            val selectedCount = selectedDownloadIds.size
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "$selectedCount / $totalImages Dipilih",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Image Grid
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(images) { index, item ->
                                val isCover = selectedCoverId == item.id
                                val isSelected = selectedDownloadIds.contains(item.id)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(135.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (isCover) 3.dp else if (isSelected) 2.dp else 1.dp,
                                            color = if (isCover) Color(0xFFFF9800) else if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray.copy(alpha = 0.5f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .combinedClickable(
                                            onClick = {
                                                // Click preview image to toggle download selection
                                                if (isSelected) {
                                                    selectedDownloadIds.remove(item.id)
                                                    if (isCover) {
                                                        selectedCoverId = ""
                                                    }
                                                } else {
                                                    selectedDownloadIds.add(item.id)
                                                }
                                            },
                                            onLongClick = {
                                                previewFullscreenIndex = index
                                            }
                                        ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                        // Dim non-selected images slightly
                                        if (!isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black.copy(alpha = 0.35f))
                                            )
                                        }

                                        // Selection Indicator Badge (Bottom Left)
                                        if (isSelected) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary,
                                                shape = RoundedCornerShape(topEnd = 8.dp),
                                                modifier = Modifier.align(Alignment.BottomStart)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Text(
                                                        text = "Download",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }

                                        // Cover Checkbox & Label (Top End) - Optional Toggle
                                        Surface(
                                            color = if (isCover) Color(0xFFFF9800) else Color.Black.copy(alpha = 0.6f),
                                            shape = RoundedCornerShape(bottomStart = 8.dp),
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .clickable {
                                                        if (isCover) {
                                                            selectedCoverId = ""
                                                        } else {
                                                            selectedCoverId = item.id
                                                            if (!selectedDownloadIds.contains(item.id)) {
                                                                selectedDownloadIds.add(item.id)
                                                            }
                                                        }
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Checkbox(
                                                    checked = isCover,
                                                    onCheckedChange = { checked ->
                                                        if (checked) {
                                                            selectedCoverId = item.id
                                                            if (!selectedDownloadIds.contains(item.id)) {
                                                                selectedDownloadIds.add(item.id)
                                                            }
                                                        } else {
                                                            selectedCoverId = ""
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = Color.White,
                                                        checkmarkColor = Color(0xFFFF9800),
                                                        uncheckedColor = Color.White
                                                    ),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    text = if (isCover) "★ COVER" else "Set Cover",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = if (isCover) FontWeight.ExtraBold else FontWeight.Medium,
                                                        fontSize = 10.sp
                                                    ),
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Download Button
                    Button(
                        onClick = {
                            if (selectedDownloadIds.isEmpty()) {
                                Toast.makeText(context, "Pilih minimal 1 foto untuk di-download!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val finalCoverTitle = if (coverTitle.isBlank()) initialCoverTitle.ifBlank { "PROPERTI PILIHAN" } else coverTitle

                            isDownloading = true
                            RwcSocialMediaDownloader.downloadDesign3Photos(
                                context = context,
                                listingId = listingId,
                                coverImageId = selectedCoverId,
                                coverTitle = finalCoverTitle,
                                selectedImageIds = selectedDownloadIds.toList(),
                                callback = object : RwcSocialMediaDownloader.DownloadDesign3Callback {
                                    override fun onProgress(message: String) {
                                        statusMessage = message
                                    }

                                    override fun onSuccess(downloadedUris: List<Uri>, firstImageUri: Uri?) {
                                        isDownloading = false
                                        val coverInfo = if (selectedCoverId.isNotBlank()) " dengan Cover" else ""
                                        Toast.makeText(context, "✓ Berhasil mengunduh ${downloadedUris.size} foto$coverInfo!", Toast.LENGTH_SHORT).show()
                                        onDownloadSuccess(downloadedUris, finalCoverTitle)
                                    }

                                    override fun onFailure(error: String) {
                                        isDownloading = false
                                        Toast.makeText(context, "✗ $error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        },
                        enabled = !isDownloading,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedCoverId.isNotBlank()) "Download (${selectedDownloadIds.size} Foto + Cover)" else "Download (${selectedDownloadIds.size} Foto)",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}
