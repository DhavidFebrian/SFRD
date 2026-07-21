package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.ScheduleViewModel
import com.example.ui.SubmitState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun InputFormScreen(
    viewModel: ScheduleViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    // ViewModel Flows
    val listingId by viewModel.formListingId.collectAsStateWithLifecycle()
    val namaMe by viewModel.formNamaMe.collectAsStateWithLifecycle()
    val lokasi by viewModel.formLokasi.collectAsStateWithLifecycle()
    val tanggal by viewModel.formTanggal.collectAsStateWithLifecycle()
    val jam by viewModel.formJam.collectAsStateWithLifecycle()
    val staff by viewModel.formStaff.collectAsStateWithLifecycle()
    val formType by viewModel.formType.collectAsStateWithLifecycle()
    val formStatus by viewModel.formStatus.collectAsStateWithLifecycle()
    val submitStatus by viewModel.submitStatus.collectAsStateWithLifecycle()
    val editingSchedule by viewModel.editingSchedule.collectAsStateWithLifecycle()
    val isEditMode = editingSchedule != null

    // DateTime picker initialization helpers
    val calendar = Calendar.getInstance()
    
    // Parse existing date or use today
    val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val parsedDate = try {
        dateFormatter.parse(tanggal) ?: Date()
    } catch (e: Exception) {
        Date()
    }
    val dateCalendar = Calendar.getInstance().apply { time = parsedDate }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance().apply { set(year, month, dayOfMonth) }
            viewModel.formTanggal.value = dateFormatter.format(cal.time)
        },
        dateCalendar.get(Calendar.YEAR),
        dateCalendar.get(Calendar.MONTH),
        dateCalendar.get(Calendar.DAY_OF_MONTH)
    )

    // Parse existing time or use now
    val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    val parsedTime = try {
        timeFormatter.parse(jam) ?: Date()
    } catch (e: Exception) {
        Date()
    }
    val timeCalendar = Calendar.getInstance().apply { time = parsedTime }

    val timePickerDialog = TimePickerDialog(
        context,
        { _, hourOfDay, minute ->
            viewModel.formJam.value = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
        },
        timeCalendar.get(Calendar.HOUR_OF_DAY),
        timeCalendar.get(Calendar.MINUTE),
        true
    )

    // Success navigation trigger
    LaunchedEffect(submitStatus) {
        if (submitStatus is SubmitState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "SCHEDULE FOTO RWC",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (isEditMode) "Edit Jadwal" else "Tambah Jadwal",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Submit Status Overlay Box
                AnimatedVisibility(
                    visible = submitStatus is SubmitState.Error,
                    enter = slideInVertically() + fadeIn(),
                    exit = slideOutVertically() + fadeOut()
                ) {
                    if (submitStatus is SubmitState.Error) {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = (submitStatus as SubmitState.Error).message,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { viewModel.dismissSubmitStatus() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }

                // Callout Tip Box explaining conditional logic
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Panduan Penginputan:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "1. Jika menginput ID Listing, info ME dan Lokasi akan di-autofill secara otomatis dan real-time.\n" +
                                       "2. Jika ID Listing dikosongkan, Anda WAJIB mengisi Nama ME dan Lokasi secara manual.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // Section 1: Listing ID (Optional/Autocomplete Trigger)
                Text(
                    text = "ID Listing & Informasi Utama",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = listingId,
                    onValueChange = { viewModel.formListingId.value = it },
                    label = { Text("ID Listing (Opsional)") },
                    placeholder = { Text("Contoh: L-2384") },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_id_listing")
                )

                // Conditional validation flags for ME and Location
                val isListingIdEmpty = listingId.isBlank()
                val hasAutofilledData = listingId.isNotBlank() && (namaMe.isNotBlank() || lokasi.isNotBlank())
                
                // Highlight container with beautiful colors and border to showcase the advanced autofill feature
                val containerColor = if (hasAutofilledData) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f)
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                }
                val borderColor = if (hasAutofilledData) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outlineVariant
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = containerColor),
                    border = BorderStroke(1.5.dp, borderColor),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (hasAutofilledData) Icons.Default.Stars else Icons.Default.FlashOn,
                                    contentDescription = null,
                                    tint = if (hasAutofilledData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasAutofilledData) "Autofill Cerdas Aktif ✨" else "Deteksi Autofill Otomatis ⚡",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (hasAutofilledData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            if (hasAutofilledData) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "TERISI OTOMATIS",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Nama ME Field
                        OutlinedTextField(
                            value = namaMe,
                            onValueChange = { viewModel.formNamaMe.value = it },
                            label = { Text(if (isListingIdEmpty) "Nama ME / Agen *" else "Nama ME / Agen (Autofilled)") },
                            placeholder = { Text("Contoh: Budi Susanto") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Person, 
                                    contentDescription = null,
                                    tint = if (namaMe.isNotBlank() && !isListingIdEmpty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_nama_me"),
                            isError = isListingIdEmpty && namaMe.isBlank(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (hasAutofilledData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                unfocusedBorderColor = if (namaMe.isNotBlank() && !isListingIdEmpty) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                            )
                        )

                        // Lokasi Field
                        OutlinedTextField(
                            value = lokasi,
                            onValueChange = { viewModel.formLokasi.value = it },
                            label = { Text(if (isListingIdEmpty) "Lokasi Properti *" else "Lokasi Properti (Autofilled)") },
                            placeholder = { Text("Contoh: Kavling Hijau Cluster B, Serpong") },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.LocationOn, 
                                    contentDescription = null,
                                    tint = if (lokasi.isNotBlank() && !isListingIdEmpty) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                ) 
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_lokasi"),
                            isError = isListingIdEmpty && lokasi.isBlank(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (hasAutofilledData) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                unfocusedBorderColor = if (lokasi.isNotBlank() && !isListingIdEmpty) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section 2: Date & Time Picker
                Text(
                    text = "Waktu Pemotretan (Wajib)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Date picker component field
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_date_card")
                            .clickable { datePickerDialog.show() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Tanggal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = tanggal,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    // Time picker component field
                    OutlinedCard(
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_time_card")
                            .clickable { timePickerDialog.show() },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Jam", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = jam,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Default.AccessTime, contentDescription = "Pick Time", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section 3: Staff Picker
                Text(
                    text = "Staff Fotografer (Wajib)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Custom Multi-Select Dropdown for Staff Selection
                var dropdownExpanded by remember { mutableStateOf(false) }
                val currentStaffLower = staff.lowercase()
                val isDavidSelected = currentStaffLower.contains("david")
                val isRaffaSelected = currentStaffLower.contains("raffa")

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = staff,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Photographer / Videographer *") },
                        placeholder = { Text("Pilih (David / Raffa)") },
                        leadingIcon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                        trailingIcon = {
                            Icon(
                                imageVector = if (dropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Toggle Dropdown"
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_staff"),
                        isError = staff.isBlank()
                    )

                    // Overlay transparent click interceptor
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { dropdownExpanded = !dropdownExpanded }
                    )

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = isDavidSelected,
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("David")
                                }
                            },
                            onClick = {
                                val isChecked = !isDavidSelected
                                val newList = mutableListOf<String>()
                                if (isChecked) newList.add("David")
                                if (isRaffaSelected) newList.add("Raffa")
                                viewModel.formStaff.value = newList.joinToString(", ")
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Checkbox(
                                        checked = isRaffaSelected,
                                        onCheckedChange = null
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Raffa")
                                }
                            },
                            onClick = {
                                val isChecked = !isRaffaSelected
                                val newList = mutableListOf<String>()
                                if (isDavidSelected) newList.add("David")
                                if (isChecked) newList.add("Raffa")
                                viewModel.formStaff.value = newList.joinToString(", ")
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Section 4: Type & Status fields
                Text(
                    text = "Tipe Kegiatan & Status Jadwal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Type field (Dropdown selection as requested)
                var typeDropdownExpanded by remember { mutableStateOf(false) }
                val typeOptions = listOf(
                    "Foto",
                    "Foto + Video",
                    "Foto + Drone",
                    "Done Foto",
                    "Done Foto + Video",
                    "Done Foto + Drone"
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = formType,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipe Kegiatan (Dropdown) *") },
                        placeholder = { Text("Pilih Tipe Kegiatan...") },
                        leadingIcon = { Icon(Icons.Default.Category, contentDescription = null) },
                        trailingIcon = {
                            Icon(
                                imageVector = if (typeDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Toggle Dropdown"
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_type")
                    )

                    // Overlay transparent click interceptor
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { typeDropdownExpanded = !typeDropdownExpanded }
                    )

                    DropdownMenu(
                        expanded = typeDropdownExpanded,
                        onDismissRequest = { typeDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        typeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    viewModel.formType.value = option
                                    typeDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Checkbox Done Progress
                val isDoneProgress = formType.startsWith("Done ")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val checked = !isDoneProgress
                            val currentType = viewModel.formType.value
                            if (checked) {
                                if (currentType == "Foto") viewModel.formType.value = "Done Foto"
                                else if (currentType == "Foto + Video" || currentType == "Foto & Video" || currentType == "Video") viewModel.formType.value = "Done Foto + Video"
                                else if (currentType == "Foto + Drone" || currentType == "Drone") viewModel.formType.value = "Done Foto + Drone"
                                else if (!currentType.startsWith("Done ")) viewModel.formType.value = "Done $currentType"
                            } else {
                                if (currentType == "Done Foto") viewModel.formType.value = "Foto"
                                else if (currentType == "Done Foto + Video") viewModel.formType.value = "Foto + Video"
                                else if (currentType == "Done Foto + Drone") viewModel.formType.value = "Foto + Drone"
                                else if (currentType.startsWith("Done ")) viewModel.formType.value = currentType.removePrefix("Done ").trim()
                            }
                        }
                        .padding(vertical = 4.dp, horizontal = 4.dp)
                ) {
                    Checkbox(
                        checked = isDoneProgress,
                        onCheckedChange = { checked ->
                            val currentType = viewModel.formType.value
                            if (checked) {
                                if (currentType == "Foto") viewModel.formType.value = "Done Foto"
                                else if (currentType == "Foto + Video" || currentType == "Foto & Video" || currentType == "Video") viewModel.formType.value = "Done Foto + Video"
                                else if (currentType == "Foto + Drone" || currentType == "Drone") viewModel.formType.value = "Done Foto + Drone"
                                else if (!currentType.startsWith("Done ")) viewModel.formType.value = "Done $currentType"
                            } else {
                                if (currentType == "Done Foto") viewModel.formType.value = "Foto"
                                else if (currentType == "Done Foto + Video") viewModel.formType.value = "Foto + Video"
                                else if (currentType == "Done Foto + Drone") viewModel.formType.value = "Foto + Drone"
                                else if (currentType.startsWith("Done ")) viewModel.formType.value = currentType.removePrefix("Done ").trim()
                            }
                        },
                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Done Progress",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Status field with 2 options
                Text(
                    text = "Status Jadwal (2 Opsi)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                var useManualStatus by remember { mutableStateOf(formStatus != "DONE") }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Option 1 Button: Set DONE (Green styled button)
                    Button(
                        onClick = {
                            viewModel.formStatus.value = "DONE"
                            useManualStatus = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (formStatus == "DONE") Color(0xFF22C55E) else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (formStatus == "DONE") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (formStatus == "DONE") Color.Black else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SET DONE", fontWeight = FontWeight.Bold)
                    }

                    // Option 2 Button: Ketik Manual
                    Button(
                        onClick = {
                            useManualStatus = true
                            if (formStatus == "DONE") {
                                viewModel.formStatus.value = "Pending"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (useManualStatus) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (useManualStatus) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ketik Manual", fontWeight = FontWeight.Bold)
                    }
                }

                if (useManualStatus) {
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = formStatus,
                        onValueChange = { viewModel.formStatus.value = it },
                        label = { Text("Ketik Manual Status Jadwal *") },
                        placeholder = { Text("Contoh: Pending, Garis Tanah, Up Foto, dll.") },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_status")
                    )
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF15803D))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Status 'DONE' terpilih (Rata Tengah & Background Hijau di Google Sheets)",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF15803D),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save button
                val canSubmit = (listingId.isNotBlank() || (namaMe.isNotBlank() && lokasi.isNotBlank())) &&
                        tanggal.isNotBlank() && jam.isNotBlank() && staff.isNotBlank()

                Button(
                    onClick = { viewModel.submitSchedule() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("submit_schedule_button"),
                    enabled = canSubmit && submitStatus !is SubmitState.Loading,
                    shape = RoundedCornerShape(14.dp)
                ) {
                    if (submitStatus is SubmitState.Loading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Menyimpan & Mensinkronisasi...", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isEditMode) "Simpan Perubahan" else "Simpan Jadwal Foto",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
