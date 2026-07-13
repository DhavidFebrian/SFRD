package com.example.ui.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.data.Schedule
import com.example.data.EditFotoTask
import com.example.ui.ScheduleViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    viewModel: ScheduleViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val messages by viewModel.visibleChatMessages.collectAsState()
    val userLastReadMap by viewModel.userLastReadTimestamps.collectAsState()
    val chatSyncing by viewModel.chatSyncing.collectAsState()
    val isChatSheetMissing by viewModel.isChatSheetMissing.collectAsState()
    val allSchedules by viewModel.allSchedules.collectAsState()
    val allEditFotoTasks by viewModel.allEditFotoTasks.collectAsState()
    
    // Auto-detect or overridden role
    var currentRole by remember { mutableStateOf(viewModel.getSenderName()) }
    
    var inputText by remember { mutableStateOf("") }
    var editingMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var replyingToMessage by remember { mutableStateOf<ChatMessage?>(null) }
    
    var messageToManage by remember { mutableStateOf<ChatMessage?>(null) }
    var showActionMenu by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<ChatMessage?>(null) }
    var showDeleteAllConfirmDialog by remember { mutableStateOf(false) }
    
    var showPinDialog by remember { mutableStateOf(false) }
    var pinText by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Start periodic sync when ChatScreen is opened
    LaunchedEffect(Unit) {
        viewModel.syncChats() // Sync immediately
        while (true) {
            kotlinx.coroutines.delay(4000)
            viewModel.syncChats()
        }
    }

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Ruang Obrolan Tim",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Identitas: $currentRole",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    // Quick identity switch with PIN verification
                    IconButton(onClick = {
                        showPinDialog = true
                    }) {
                        Icon(
                            imageVector = Icons.Default.Cached,
                            contentDescription = "Ganti Identitas",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    // Editing Bar indicator
                    AnimatedVisibility(
                        visible = editingMessage != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = "Mengedit pesan",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = editingMessage?.message ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    editingMessage = null
                                    inputText = ""
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Batal",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Replying Bar indicator
                    AnimatedVisibility(
                        visible = replyingToMessage != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val replyMsg = replyingToMessage
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Reply,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column {
                                    Text(
                                        text = "Membalas ke ${replyMsg?.senderName ?: ""}",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    val cleanReplyText = if (replyMsg != null) getCleanMessageText(replyMsg.message) else ""
                                    val finalReplyDisplay = if (cleanReplyText.startsWith("[ATTACH:")) {
                                        val idx = cleanReplyText.indexOf("]")
                                        if (idx != -1) {
                                            "[Lampiran] " + cleanReplyText.substring(idx + 1).trim()
                                        } else {
                                            "[Lampiran]"
                                        }
                                    } else {
                                        cleanReplyText
                                    }
                                    Text(
                                        text = finalReplyDisplay,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    replyingToMessage = null
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Batal",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Chat input text field row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            placeholder = { Text("Ketik pesan di sini...") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_input_text_field"),
                            shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            maxLines = 4
                        )

                        FloatingActionButton(
                            onClick = {
                                if (inputText.isNotBlank()) {
                                    val currentEdit = editingMessage
                                    val currentReply = replyingToMessage
                                    if (currentEdit != null) {
                                        // Keep original reply prefix if it exists
                                        val prefix = if (currentEdit.message.startsWith("[REPLY:") && currentEdit.message.contains("]")) {
                                            currentEdit.message.substring(0, currentEdit.message.indexOf("]") + 1) + " "
                                        } else {
                                            ""
                                        }
                                        viewModel.editChatMessage(currentEdit, prefix + inputText.trim())
                                        editingMessage = null
                                    } else if (currentReply != null) {
                                        try {
                                            val cleanReplyText = getCleanMessageText(currentReply.message)
                                            val encodedSender = java.net.URLEncoder.encode(currentReply.senderName, "UTF-8")
                                            val encodedMsg = java.net.URLEncoder.encode(cleanReplyText, "UTF-8")
                                            val finalMsg = "[REPLY:sender=$encodedSender&msg=$encodedMsg] ${inputText.trim()}"
                                            viewModel.sendChatMessage(finalMsg)
                                        } catch (e: Exception) {
                                            viewModel.sendChatMessage(inputText.trim())
                                        }
                                        replyingToMessage = null
                                    } else {
                                        viewModel.sendChatMessage(inputText.trim())
                                    }
                                    inputText = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_send_button"),
                            shape = RoundedCornerShape(24.dp),
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                imageVector = if (editingMessage != null) Icons.Default.Check else Icons.Default.Send,
                                contentDescription = if (editingMessage != null) "Selesai Edit" else "Kirim",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        modifier = Modifier.imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Missing Sheet Warning Banner
            AnimatedVisibility(
                visible = isChatSheetMissing,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Tab Sheet 'Chat' Belum Dibuat!",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = "Aplikasi mendeteksi tab sheet khusus obrolan bernama 'Chat' belum ada di Google Spreadsheet Anda, sehingga chat sementara disinkronkan di database lokal.\n\nSilakan tambahkan tab/sheet baru bernama 'Chat' di Google Spreadsheet Anda agar obrolan dapat tersinkronisasi secara real-time antar perangkat!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            if (messages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = "Belum Ada Obrolan",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Ketik sesuatu di bawah untuk memulai percakapan internal.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        val isMe = message.senderName == currentRole
                        
                        // Row to hold bubble and alignments
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                        ) {
                            Column(
                                modifier = Modifier.widthIn(max = 280.dp),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                // Sender Name text
                                Text(
                                    text = message.senderName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 2.dp)
                                )

                                // Message Bubble
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isMe) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        }
                                    ),
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isMe) 16.dp else 4.dp,
                                        bottomEnd = if (isMe) 4.dp else 16.dp
                                    ),
                                    modifier = Modifier
                                        .combinedClickable(
                                            onClick = {
                                                messageToManage = message
                                                showActionMenu = true
                                            },
                                            onLongClick = {
                                                messageToManage = message
                                                showActionMenu = true
                                            }
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        var currentText = message.message
                                        val hasReply = currentText.startsWith("[REPLY:") && currentText.contains("]")
                                        var replySender = ""
                                        var replyMsg = ""

                                        if (hasReply) {
                                            try {
                                                val closeBracketIndex = currentText.indexOf("]")
                                                val replyPart = currentText.substring(7, closeBracketIndex)
                                                currentText = currentText.substring(closeBracketIndex + 1).trim()
                                                val params = replyPart.split("&")
                                                for (param in params) {
                                                    val kv = param.split("=")
                                                    if (kv.size == 2) {
                                                        val key = kv[0]
                                                        val value = kv[1]
                                                        when (key) {
                                                            "sender" -> replySender = java.net.URLDecoder.decode(value, "UTF-8")
                                                            "msg" -> replyMsg = java.net.URLDecoder.decode(value, "UTF-8")
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }

                                        val hasAttach = currentText.startsWith("[ATTACH:") && currentText.contains("]")
                                        var attachIdListing = ""
                                        var attachNo = 0
                                        var attachType = ""
                                        var attachNamaMe = ""
                                        var displayText = currentText

                                        if (hasAttach) {
                                            try {
                                                val closeBracketIndex = currentText.indexOf("]")
                                                val attachPart = currentText.substring(8, closeBracketIndex)
                                                displayText = currentText.substring(closeBracketIndex + 1).trim()
                                                val params = attachPart.split("&")
                                                for (param in params) {
                                                    val kv = param.split("=")
                                                    if (kv.size == 2) {
                                                        val key = kv[0]
                                                        val value = kv[1]
                                                        when (key) {
                                                            "idListing" -> attachIdListing = value
                                                            "no" -> attachNo = value.toIntOrNull() ?: 0
                                                            "type" -> attachType = value
                                                            "namaMe" -> attachNamaMe = value
                                                        }
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }

                                        if (hasReply && replySender.isNotEmpty()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 6.dp)
                                            ) {
                                                val barColor = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                                                Row(
                                                    modifier = Modifier
                                                        .drawBehind {
                                                            val strokeWidth = 4.dp.toPx()
                                                            drawLine(
                                                                color = barColor,
                                                                start = androidx.compose.ui.geometry.Offset(strokeWidth / 2, 0f),
                                                                end = androidx.compose.ui.geometry.Offset(strokeWidth / 2, size.height),
                                                                strokeWidth = strokeWidth
                                                            )
                                                        }
                                                        .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 8.dp)
                                                ) {
                                                    Column {
                                                        Text(
                                                            text = replySender,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = barColor
                                                        )
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        val cleanReplyMsg = if (replyMsg.startsWith("[ATTACH:")) {
                                                            val idx = replyMsg.indexOf("]")
                                                            if (idx != -1) {
                                                                "[Lampiran] " + replyMsg.substring(idx + 1).trim()
                                                            } else {
                                                                "[Lampiran]"
                                                            }
                                                        } else {
                                                            replyMsg
                                                        }
                                                        Text(
                                                            text = cleanReplyMsg,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                                            maxLines = 2,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        if (hasAttach) {
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(bottom = 8.dp),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Attachment,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Text(
                                                            text = "Lampiran Jadwal / Task",
                                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "ID Listing: $attachIdListing",
                                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                                    )
                                                    Text(
                                                        text = "Tipe: $attachType",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Text(
                                                        text = "ME: $attachNamaMe",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                    Button(
                                                        onClick = {
                                                            val found = if (attachType.lowercase().contains("edit foto") || attachType.lowercase().contains("upload ig")) {
                                                                allEditFotoTasks.find { it.no == attachNo || (attachIdListing.isNotBlank() && it.idListing == attachIdListing) }?.let { task ->
                                                                    Schedule(
                                                                        id = task.id,
                                                                        no = task.no,
                                                                        idListing = task.idListing,
                                                                        namaMe = task.namaMe,
                                                                        lokasi = "${if (task.postingIg) "[Posting IG: Ya] " else "[Posting IG: Tidak] "}${task.editNotes}",
                                                                        tanggal = task.jadwalPosting,
                                                                        jam = task.judul,
                                                                        staff = task.source,
                                                                        type = "Edit Foto",
                                                                        status = if (task.done) "Done" else "Pending",
                                                                        synced = task.synced
                                                                    )
                                                                }
                                                            } else {
                                                                allSchedules.find { it.no == attachNo || (attachIdListing.isNotBlank() && it.idListing == attachIdListing) }
                                                            }
                                                            if (found != null) {
                                                                viewModel.selectedScheduleFromChat.value = found
                                                                onNavigateBack()
                                                            } else {
                                                                android.widget.Toast.makeText(context, "Listing tidak ditemukan di HP.", android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        modifier = Modifier.fillMaxWidth().height(36.dp),
                                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(6.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = MaterialTheme.colorScheme.primary,
                                                            contentColor = Color.White
                                                        )
                                                    ) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.OpenInNew,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                            Text("Lihat Listing", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (displayText.isNotEmpty()) {
                                            Text(
                                                text = displayText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Time details (HH:mm:ss) and sync status
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.End,
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            val timeStr = remember(message.timestamp) {
                                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(message.timestamp))
                                            }
                                            Text(
                                                text = timeStr,
                                                fontSize = 10.sp,
                                                color = if (isMe) {
                                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                                } else {
                                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                }
                                            )
                                            if (isMe) {
                                                val isRead = remember(message.timestamp, userLastReadMap, currentRole) {
                                                    userLastReadMap.any { (user, ts) -> user != currentRole && ts >= message.timestamp }
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = if (message.synced) Icons.Default.DoneAll else Icons.Default.Schedule,
                                                    contentDescription = if (message.synced) (if (isRead) "Dibaca" else "Tersinkronisasi") else "Tertunda",
                                                    tint = if (message.synced) {
                                                        if (isRead) Color(0xFF0288D1) else MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                                    } else {
                                                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.4f)
                                                    },
                                                    modifier = Modifier.size(12.dp)
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
        }
    }

    // Action dialog/sheet for selected message
    if (showActionMenu && messageToManage != null) {
        val selectedMsg = messageToManage!!
        AlertDialog(
            onDismissRequest = {
                showActionMenu = false
                messageToManage = null
            },
            title = {
                Text(
                    text = "Kelola Pesan",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "\"${selectedMsg.message}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 2
                    )
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Balas (Reply) button
                    Button(
                        onClick = {
                            replyingToMessage = selectedMsg
                            editingMessage = null // cancel edit if replying
                            showActionMenu = false
                            messageToManage = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(18.dp))
                            Text("Balas (Reply)")
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Edit button
                        Button(
                            onClick = {
                                editingMessage = selectedMsg
                                inputText = getCleanMessageText(selectedMsg.message)
                                replyingToMessage = null // cancel reply if editing
                                showActionMenu = false
                                messageToManage = null
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Edit")
                            }
                        }

                        // Delete button
                        Button(
                            onClick = {
                                showActionMenu = false
                                showDeleteConfirmDialog = selectedMsg
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Hapus")
                            }
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showActionMenu = false
                        messageToManage = null
                    }
                ) {
                    Text("Tutup")
                }
            }
        )
    }

    // Single message delete confirmation
    if (showDeleteConfirmDialog != null) {
        val msg = showDeleteConfirmDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = null },
            title = { Text("Hapus Pesan?", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus pesan ini secara permanen?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteChatMessage(msg.id)
                        showDeleteConfirmDialog = null
                        messageToManage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = null }) {
                    Text("Batal")
                }
            }
        )
    }

    // PIN Verification Dialog for David Staff
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pinText = ""
                pinError = false
            },
            title = {
                Text(
                    text = "Verifikasi PIN",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Masukkan PIN",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = pinText,
                        onValueChange = {
                            pinText = it
                            pinError = false
                        },
                        label = { Text("PIN") },
                        isError = pinError,
                        supportingText = {
                            if (pinError) {
                                Text(
                                    text = "PIN salah, silakan coba lagi.",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        },
                        singleLine = true,
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (pinText == "2332") {
                            viewModel.setSenderRoleOverride("David Staff")
                            currentRole = viewModel.getSenderName()
                            showPinDialog = false
                            pinText = ""
                            pinError = false
                            android.widget.Toast.makeText(context, "Berhasil masuk sebagai David Staff", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            pinError = true
                        }
                    }
                ) {
                    Text("Masuk")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            viewModel.setSenderRoleOverride(null)
                            currentRole = viewModel.getSenderName()
                            showPinDialog = false
                            pinText = ""
                            pinError = false
                            android.widget.Toast.makeText(context, "Kembali ke mode default (Raffa)", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Reset")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            showPinDialog = false
                            pinText = ""
                            pinError = false
                        }
                    ) {
                        Text("Batal")
                    }
                }
            }
        )
    }
}

private fun getCleanMessageText(message: String): String {
    if (message.startsWith("[REPLY:") && message.contains("]")) {
        val closeBracketIndex = message.indexOf("]")
        return message.substring(closeBracketIndex + 1).trim()
    }
    return message
}
