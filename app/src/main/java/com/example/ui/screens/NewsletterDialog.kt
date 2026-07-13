package com.example.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.network.MeetingListing
import com.example.util.NewsletterDownloader
import com.example.ui.ScheduleViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.lazy.items
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NewsletterDialog(
    dateStr: String,
    listings: List<MeetingListing>,
    viewModel: ScheduleViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val listingImagesMap by viewModel.listingImagesMap.collectAsState()
    val listingImagesGalleryMap by viewModel.listingImagesGalleryMap.collectAsState()

    var includeCover by remember { mutableStateOf(true) }
    var selectedCoverListing by remember { mutableStateOf<MeetingListing?>(listings.firstOrNull()) }
    var selectedCoverPhotoUrl by remember { mutableStateOf("") }

    LaunchedEffect(selectedCoverListing, listingImagesGalleryMap, listingImagesMap) {
        selectedCoverListing?.let { listing ->
            val cleanId = listing.idListing.trim()
            val gallery = listingImagesGalleryMap[cleanId] ?: emptyList()
            val main = listingImagesMap[cleanId]
            if (selectedCoverPhotoUrl.isBlank() || !gallery.contains(selectedCoverPhotoUrl) && selectedCoverPhotoUrl != main) {
                selectedCoverPhotoUrl = gallery.firstOrNull() ?: main ?: ""
            }
        }
    }

    val calculatedTitle = remember(dateStr) { calculateNewsletterTitle(dateStr) }
    var titleInput by remember { mutableStateOf(calculatedTitle) }

    var status by remember { mutableStateOf("idle") } // idle, running, success, error
    var progressMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    
    var pdfOutputUri by remember { mutableStateOf<Uri?>(null) }
    var jpgOutputUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    val listingIds = remember(listings) {
        listings.map { it.idListing.trim() }.filter { it.isNotEmpty() }.joinToString(",")
    }

    Dialog(
        onDismissRequest = { if (status != "running") onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureAsPdf,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Unduh Newsletter PDF & JPG",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                when (status) {
                    "idle" -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                Text(
                                    text = "Konfirmasi judul newsletter yang akan diunduh dari portal Ray White Cipete. Judul ini akan tertulis di dalam file PDF dan juga dijadikan nama file.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item {
                                OutlinedTextField(
                                    value = titleInput,
                                    onValueChange = { titleInput = it },
                                    label = { Text("Judul Newsletter") },
                                    placeholder = { Text("Contoh: HOT PROPERTY 22 - 28 JUNI 2026") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Menyaring total ${listings.size} ID Listing untuk dicari.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            item {
                                HorizontalDivider()
                            }

                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = includeCover,
                                        onCheckedChange = { includeCover = it }
                                    )
                                    Text(
                                        text = "Tambahkan Halaman Cover Depan",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                }
                            }

                            if (includeCover) {
                                item {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text(
                                            text = "Listing untuk Cover Foto:",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .padding(6.dp)
                                            ) {
                                                LazyColumn(
                                                    modifier = Modifier.fillMaxSize(),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    items(listings) { listing ->
                                                        val cleanId = listing.idListing.trim()
                                                        val imageUrl = listingImagesMap[cleanId]
                                                        val isSelected = selectedCoverListing == listing
                                                        
                                                        Card(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .clickable { 
                                                                    selectedCoverListing = listing
                                                                    // Reset selected photo URL to trigger default photo search
                                                                    selectedCoverPhotoUrl = ""
                                                                },
                                                            shape = RoundedCornerShape(8.dp),
                                                            border = BorderStroke(
                                                                width = if (isSelected) 2.dp else 1.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                            ),
                                                            colors = CardDefaults.cardColors(
                                                                containerColor = if (isSelected) {
                                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                                } else {
                                                                    MaterialTheme.colorScheme.surface
                                                                }
                                                            )
                                                        ) {
                                                            Row(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .padding(8.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                            ) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(48.dp)
                                                                        .background(MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(6.dp)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    if (imageUrl != null) {
                                                                        AsyncImage(
                                                                            model = imageUrl,
                                                                            contentDescription = null,
                                                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(6.dp)),
                                                                            contentScale = ContentScale.Crop
                                                                        )
                                                                    } else {
                                                                        Icon(
                                                                            imageVector = Icons.Default.Image,
                                                                            contentDescription = null,
                                                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    }
                                                                }
                                                                
                                                                Column(modifier = Modifier.weight(1f)) {
                                                                    Text(
                                                                        text = listing.idListing,
                                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                                        color = MaterialTheme.colorScheme.onSurface
                                                                    )
                                                                    Text(
                                                                        text = listing.namaMe,
                                                                        style = MaterialTheme.typography.bodySmall,
                                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                                    )
                                                                }
                                                                
                                                                if (isSelected) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.CheckCircle,
                                                                        contentDescription = "Selected",
                                                                        tint = MaterialTheme.colorScheme.primary,
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                selectedCoverListing?.let { listing ->
                                    val cleanId = listing.idListing.trim()
                                    val gallery = listingImagesGalleryMap[cleanId] ?: emptyList()
                                    val main = listingImagesMap[cleanId]
                                    val images = (gallery + listOfNotNull(main)).distinct()

                                    if (images.isNotEmpty()) {
                                        item {
                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                Text(
                                                    text = "Pilih Foto untuk Cover:",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                                Spacer(modifier = Modifier.height(6.dp))
                                                LazyRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    items(images) { imgUrl ->
                                                        val isSelected = selectedCoverPhotoUrl == imgUrl
                                                        val border = if (isSelected) BorderStroke(3.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                                        Card(
                                                            modifier = Modifier
                                                                .size(80.dp)
                                                                .clickable { selectedCoverPhotoUrl = imgUrl },
                                                            shape = RoundedCornerShape(8.dp),
                                                            border = border
                                                        ) {
                                                            AsyncImage(
                                                                model = imgUrl,
                                                                contentDescription = null,
                                                                modifier = Modifier.fillMaxSize(),
                                                                contentScale = ContentScale.Crop
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Batal")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (titleInput.isBlank()) {
                                        Toast.makeText(context, "Judul tidak boleh kosong!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    status = "running"
                                    NewsletterDownloader.downloadAndConvert(
                                        context = context,
                                        listingIds = listingIds,
                                        pdfTitle = titleInput,
                                        includeCover = includeCover,
                                        coverPhotoUrl = if (includeCover && selectedCoverPhotoUrl.isNotBlank()) selectedCoverPhotoUrl else null,
                                        callback = object : NewsletterDownloader.DownloadCallback {
                                            override fun onProgress(message: String) {
                                                progressMessage = message
                                            }

                                            override fun onSuccess(pdfUri: Uri, jpgUris: List<Uri>) {
                                                pdfOutputUri = pdfUri
                                                jpgOutputUris = jpgUris
                                                status = "success"
                                            }

                                            override fun onFailure(error: String) {
                                                errorMessage = error
                                                status = "error"
                                            }
                                        }
                                    )
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Mulai Unduh")
                            }
                        }
                    }
                    "running" -> {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = progressMessage,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    "success" -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Success",
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Unduhan Berhasil!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "File PDF dan JPG telah disimpan di perangkat Anda.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Outputs list
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Hasil File Output:",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "📂 [PDF] $titleInput.pdf (Folder Downloads)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "🖼️ [JPG] ${jpgOutputUris.size} Halaman Gambar (Folder Gallery / Pictures / RWNewsletter)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "📊 Total Listing yang diunduh: ${listings.size} listing",
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = onDismiss,
                                    modifier = Modifier.weight(0.9f),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Text("Tutup", fontSize = 11.sp, maxLines = 1)
                                }
                                Button(
                                    onClick = {
                                        blastJpgsToWhatsapp(context, jpgOutputUris)
                                    },
                                    modifier = Modifier.weight(1.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Blast JPG", fontSize = 11.sp, maxLines = 1)
                                }
                                Button(
                                    onClick = {
                                        pdfOutputUri?.let { pdfUri ->
                                            blastPdfToWhatsapp(context, pdfUri)
                                        }
                                    },
                                    modifier = Modifier.weight(1.1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Blast PDF", fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                    }
                    "error" -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Gagal Mengunduh",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                            
                            Spacer(modifier = Modifier.height(24.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(onClick = onDismiss) {
                                    Text("Tutup")
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        status = "idle"
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Coba Lagi")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun calculateNewsletterTitle(dateStr: String): String {
    try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = sdf.parse(dateStr) ?: return "HOT PROPERTY"
        
        val cal = Calendar.getInstance()
        cal.time = date
        
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        
        val daysToMonday = if (dayOfWeek >= Calendar.MONDAY) {
            dayOfWeek - Calendar.MONDAY + 7
        } else {
            dayOfWeek + 5
        }
        
        val mondayCal = cal.clone() as Calendar
        mondayCal.add(Calendar.DAY_OF_YEAR, -daysToMonday)
        
        val sundayCal = mondayCal.clone() as Calendar
        sundayCal.add(Calendar.DAY_OF_YEAR, 6)
        
        val startDay = mondayCal.get(Calendar.DAY_OF_MONTH)
        val endDay = sundayCal.get(Calendar.DAY_OF_MONTH)
        
        val startMonthName = SimpleDateFormat("MMMM", Locale("id", "ID")).format(mondayCal.time).uppercase()
        val endMonthName = SimpleDateFormat("MMMM", Locale("id", "ID")).format(sundayCal.time).uppercase()
        val yearStr = sundayCal.get(Calendar.YEAR)
        
        return if (startMonthName == endMonthName) {
            "HOT PROPERTY $startDay - $endDay $endMonthName $yearStr"
        } else {
            "HOT PROPERTY $startDay $startMonthName - $endDay $endMonthName $yearStr"
        }
    } catch (e: Exception) {
        return "HOT PROPERTY"
    }
}

private fun blastJpgsToWhatsapp(context: Context, jpgUris: List<Uri>) {
    if (jpgUris.isEmpty()) {
        Toast.makeText(context, "Tidak ada file JPG untuk dikirim.", Toast.LENGTH_SHORT).show()
        return
    }
    
    val uris = ArrayList<Uri>().apply {
        addAll(jpgUris)
    }
    
    val applyClipData: Intent.(ArrayList<Uri>) -> Unit = { list ->
        val clipData = ClipData.newRawUri("Newsletter JPGs", list.first())
        for (i in 1 until list.size) {
            clipData.addItem(ClipData.Item(list[i]))
        }
        setClipData(clipData)
    }
    
    val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
        type = "image/*"
        putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        setPackage("com.whatsapp")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        applyClipData(uris)
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val businessIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            setPackage("com.whatsapp.w4b")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            applyClipData(uris)
        }
        try {
            context.startActivity(businessIntent)
        } catch (ex: Exception) {
            val generalIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "image/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                applyClipData(uris)
            }
            try {
                context.startActivity(Intent.createChooser(generalIntent, "Kirim Gambar via..."))
            } catch (anyEx: Exception) {
                Toast.makeText(context, "Gagal membagikan gambar.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

private fun blastPdfToWhatsapp(context: Context, pdfUri: Uri) {
    val textMessage = "Terlampir bahan dan cover IG NewsLetter dan WA Story untuk bahan blast"
    
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, pdfUri)
        putExtra(Intent.EXTRA_TEXT, textMessage)
        setPackage("com.whatsapp")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        setClipData(ClipData.newRawUri("Newsletter PDF", pdfUri))
    }

    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        val businessIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            putExtra(Intent.EXTRA_TEXT, textMessage)
            setPackage("com.whatsapp.w4b")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setClipData(ClipData.newRawUri("Newsletter PDF", pdfUri))
        }
        try {
            context.startActivity(businessIntent)
        } catch (ex: Exception) {
            val generalIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, pdfUri)
                putExtra(Intent.EXTRA_TEXT, textMessage)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setClipData(ClipData.newRawUri("Newsletter PDF", pdfUri))
            }
            try {
                context.startActivity(Intent.createChooser(generalIntent, "Kirim PDF via..."))
            } catch (anyEx: Exception) {
                Toast.makeText(context, "Gagal membagikan PDF.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

