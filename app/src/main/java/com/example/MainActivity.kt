package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import coil.Coil
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ScheduleViewModel
import com.example.ui.ScheduleCategory
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.InputFormScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.AnalyticScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape

enum class TabItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
) {
    DASHBOARD("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "tab_dashboard"),
    TASK("Task", Icons.Filled.TaskAlt, Icons.Outlined.TaskAlt, "tab_task"),
    UPLOAD_IG("Upload IG", Icons.Filled.CloudUpload, Icons.Outlined.CloudUpload, "tab_upload_ig"),
    WEEKLY_MEETING("Meeting", Icons.Filled.Groups, Icons.Outlined.Groups, "tab_weekly_meeting"),
    TAMBAH("Tambah", Icons.Filled.AddCircle, Icons.Outlined.AddCircle, "tab_tambah"),
    ANALYTIC("Analytic", Icons.Filled.PieChart, Icons.Outlined.PieChart, "tab_analytic"),
    PENGATURAN("Setting", Icons.Filled.Settings, Icons.Outlined.Settings, "tab_settings"),
    ABSEN_WEEKLY_MEETING("Absen Weekly Meeting", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle, "tab_absen_weekly_meeting")
}

class MainActivity : ComponentActivity() {
    private val navigateToChatTrigger = mutableStateOf(false)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("navigate_to_chat", false)) {
            navigateToChatTrigger.value = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize optimized Coil Image Loader for responsive image loading
        // Updated and verified from Antigravity Workspace sync.
        val imageLoader = ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(this.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(512 * 1024 * 1024) // 512 MB
                    .build()
            }
            .respectCacheHeaders(false) // Force cache local images regardless of server headers
            .crossfade(true) // Smooth image fade-in transition
            .build()
        Coil.setImageLoader(imageLoader)
        
        if (intent.getBooleanExtra("navigate_to_chat", false)) {
            navigateToChatTrigger.value = true
        }
        
        // Request Post Notifications permission dynamically for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val permissionCheck = checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
            if (permissionCheck != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Start Background Sync Service
        try {
            val serviceIntent = Intent(this, com.example.service.BackgroundSyncService::class.java)
            startService(serviceIntent)
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Failed to start BackgroundSyncService", e)
        }

        setContent {
            val viewmodel: ScheduleViewModel = viewModel()
            val isDarkTheme by viewmodel.isDarkTheme.collectAsState()
            val themeStyle by viewmodel.selectedThemeStyle.collectAsState()
            
            MyApplicationTheme(darkTheme = isDarkTheme, themeStyle = themeStyle) {
                var currentTab by remember { mutableStateOf(TabItem.DASHBOARD) }
                var isMonthMenuExpanded by remember { mutableStateOf(false) }
                var isInChatScreen by remember { mutableStateOf(false) }
                var isWeeklyMeetingExpanded by remember { mutableStateOf(false) }
                var isWeeklyJuneExpanded by remember { mutableStateOf(false) }
                var isWeeklyJulyExpanded by remember { mutableStateOf(false) }
                
                // App update download states
                val context = androidx.compose.ui.platform.LocalContext.current
                var downloadProgress by remember { mutableStateOf<Float?>(null) }
                var downloadedFile by remember { mutableStateOf<java.io.File?>(null) }
                var updateErrorMessage by remember { mutableStateOf<String?>(null) }
                var showPermissionRequiredDialog by remember { mutableStateOf(false) }
                var isUpdateDialogDismissed by remember { mutableStateOf(false) }

                LaunchedEffect(isInChatScreen) {
                    viewmodel.isInChatScreen.value = isInChatScreen
                }
                
                LaunchedEffect(navigateToChatTrigger.value) {
                    if (navigateToChatTrigger.value) {
                        isInChatScreen = true
                        navigateToChatTrigger.value = false
                    }
                }
                
                val selectedChatSchedule by viewmodel.selectedScheduleFromChat.collectAsState()
                val listingImagesMap by viewmodel.listingImagesMap.collectAsState()
                val listingImagesGalleryMap by viewmodel.listingImagesGalleryMap.collectAsState()
                val agentInfoMap by viewmodel.agentInfoMap.collectAsState()
                
                val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                val scope = rememberCoroutineScope()

                androidx.activity.compose.BackHandler(
                    enabled = drawerState.isOpen || isInChatScreen || currentTab != TabItem.DASHBOARD
                ) {
                    if (drawerState.isOpen) {
                        scope.launch { drawerState.close() }
                    } else if (isInChatScreen) {
                        isInChatScreen = false
                    } else {
                        currentTab = TabItem.DASHBOARD
                    }
                }
                
                if (selectedChatSchedule != null) {
                    com.example.ui.screens.DetailPagerScreen(
                        schedule = selectedChatSchedule!!,
                        listingImagesMap = listingImagesMap,
                        listingImagesGalleryMap = listingImagesGalleryMap,
                        agentInfoMap = agentInfoMap,
                        viewModel = viewmodel,
                        onDismiss = { viewmodel.selectedScheduleFromChat.value = null },
                        onNavigateToChat = {
                            viewmodel.selectedScheduleFromChat.value = null
                            isInChatScreen = true
                        }
                    )
                } else if (isInChatScreen) {
                    com.example.ui.screens.ChatScreen(
                        viewModel = viewmodel,
                        onNavigateBack = { isInChatScreen = false }
                    )
                } else {
                    ModalNavigationDrawer(
                        drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "RW Cipete Scheduling",
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 20.dp)
                            )
                             HorizontalDivider()
                             Spacer(Modifier.height(12.dp))

                             // Analytic Drawer Item (Moved from Dashboard to left navigation drawer)
                             NavigationDrawerItem(
                                 icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                                 label = { Text("Analytic Dashboard") },
                                 selected = currentTab == TabItem.ANALYTIC,
                                 onClick = {
                                     currentTab = TabItem.ANALYTIC
                                     scope.launch { drawerState.close() }
                                 },
                                 modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                             )

                             HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                             // Navigation Drawer Items as beautiful list
                             val selectedCat by viewmodel.selectedCategory.collectAsState()
                            
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                                label = { Text("Dashboard Utama") },
                                selected = currentTab == TabItem.DASHBOARD && selectedCat == ScheduleCategory.SEMUA,
                                onClick = {
                                    currentTab = TabItem.DASHBOARD
                                    viewmodel.selectedCategory.value = ScheduleCategory.SEMUA
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.ElectricBolt, contentDescription = null) },
                                label = { Text("Jadwal Aktif") },
                                selected = currentTab == TabItem.DASHBOARD && selectedCat == ScheduleCategory.AKTIF,
                                onClick = {
                                    currentTab = TabItem.DASHBOARD
                                    viewmodel.selectedCategory.value = ScheduleCategory.AKTIF
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                             NavigationDrawerItem(
                                icon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                label = { Text("Jadwal Selesai") },
                                selected = currentTab == TabItem.DASHBOARD && selectedCat == ScheduleCategory.SELESAI,
                                onClick = {
                                    currentTab = TabItem.DASHBOARD
                                    viewmodel.selectedCategory.value = ScheduleCategory.SELESAI
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.PauseCircle, contentDescription = null) },
                                label = { Text("Jadwal Non-Aktif") },
                                selected = currentTab == TabItem.DASHBOARD && selectedCat == ScheduleCategory.NON_AKTIF,
                                onClick = {
                                    currentTab = TabItem.DASHBOARD
                                    viewmodel.selectedCategory.value = ScheduleCategory.NON_AKTIF
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )

                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Task, contentDescription = null) },
                                label = { Text("Dashboard Task (V5.8)") },
                                selected = currentTab == TabItem.TASK,
                                onClick = {
                                    currentTab = TabItem.TASK
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                            // Sheet (Month) Selector Section
                            val activeMonth by viewmodel.selectedMonth.collectAsState()
                            
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                                Surface(
                                    onClick = { isMonthMenuExpanded = !isMonthMenuExpanded },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isMonthMenuExpanded) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f) else Color.Transparent,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.DateRange, 
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    "Pilih Data Sheet (Bulan)",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                                )
                                                Text(
                                                    "Aktif: $activeMonth",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Icon(
                                            if (isMonthMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = null
                                        )
                                    }
                                }
                                
                                AnimatedVisibility(visible = isMonthMenuExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp)
                                            .fillMaxWidth()
                                    ) {
                                        val months = listOf("Juni 2026", "Juli 2026")
                                        months.forEach { month ->
                                            val isSelected = activeMonth == month
                                            Surface(
                                                onClick = {
                                                    viewmodel.selectMonth(month)
                                                    scope.launch { drawerState.close() }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 2.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                        contentDescription = null,
                                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(Modifier.width(12.dp))
                                                    Text(
                                                        text = month,
                                                        style = MaterialTheme.typography.bodyMedium.copy(
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                        )
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                            
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.AddCircle, contentDescription = null) },
                                label = { Text("Tambah Jadwal Baru") },
                                selected = currentTab == TabItem.TAMBAH,
                                onClick = {
                                    currentTab = TabItem.TAMBAH
                                    viewmodel.resetForm()
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            NavigationDrawerItem(
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                label = { Text("Setting API Sheets") },
                                selected = currentTab == TabItem.PENGATURAN,
                                onClick = {
                                    currentTab = TabItem.PENGATURAN
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                            )
                            
                            Spacer(Modifier.weight(1f))
                            Text(
                                "Versi V5.8",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                            )
                        }
                    }
                ) {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        bottomBar = {
                            NavigationBar(
                                modifier = Modifier.testTag("bottom_nav_bar"),
                                windowInsets = WindowInsets.navigationBars
                            ) {
                                val selectedCat by viewmodel.selectedCategory.collectAsState()
                                TabItem.values().filter { it != TabItem.PENGATURAN && it != TabItem.ANALYTIC && it != TabItem.ABSEN_WEEKLY_MEETING }.forEach { tab ->
                                    val isSelected = currentTab == tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { 
                                            currentTab = tab
                                            if (tab == TabItem.TAMBAH) {
                                                viewmodel.resetForm()
                                            }
                                        },
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.title
                                            )
                                        },
                                        label = { Text(tab.title) },
                                        modifier = Modifier.testTag(tab.tag)
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            // Central Update Dialog Overlay
                            val updateState by viewmodel.appUpdateState.collectAsState()
                            if (updateState != null && !isUpdateDialogDismissed) {
                                val u = updateState!!
                                AlertDialog(
                                    onDismissRequest = {}, // Force explicit interaction (either Update or Skip)
                                    icon = {
                                        Icon(
                                            imageVector = Icons.Default.SystemUpdate,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    },
                                    title = {
                                        Text(
                                            text = "Update Tersedia!",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleLarge,
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    text = {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = "Versi baru SFRD V${u.version} telah tersedia di GitHub.",
                                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (u.changelog.isNotBlank()) {
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Text(
                                                            text = "Catatan Rilis:",
                                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                        Spacer(Modifier.height(4.dp))
                                                        Text(
                                                            text = u.changelog,
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            } else {
                                                Text(
                                                    text = "Silakan unduh untuk memperbarui fitur dan performa aplikasi.",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    },
                                    confirmButton = {
                                        Button(
                                            onClick = {
                                                if (downloadedFile != null && downloadedFile!!.exists()) {
                                                    val success = com.example.util.AppUpdateChecker.installApk(context, downloadedFile!!)
                                                    if (!success) {
                                                        showPermissionRequiredDialog = true
                                                    }
                                                } else {
                                                    downloadProgress = 0f
                                                    updateErrorMessage = null
                                                    com.example.util.AppUpdateChecker.downloadApk(
                                                        context = context,
                                                        url = u.downloadUrl,
                                                        onProgress = { progress ->
                                                            downloadProgress = progress
                                                        },
                                                        onSuccess = { file ->
                                                            downloadProgress = null
                                                            downloadedFile = file
                                                            val success = com.example.util.AppUpdateChecker.installApk(context, file)
                                                            if (!success) {
                                                                showPermissionRequiredDialog = true
                                                            }
                                                        },
                                                        onFailure = { error ->
                                                            downloadProgress = null
                                                            updateErrorMessage = "Gagal mengunduh update: ${error.localizedMessage}"
                                                        }
                                                    )
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text(if (downloadedFile != null && downloadedFile!!.exists()) "Instal" else "Update Sekarang")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(
                                            onClick = { isUpdateDialogDismissed = true }
                                        ) {
                                            Text("Nanti / Skip", color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                )
                            }

                            // Render app update status dialogs inline
                            if (downloadProgress != null) {
                                AlertDialog(
                                    onDismissRequest = {},
                                    title = { Text("Mengunduh Pembaruan") },
                                    text = {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            LinearProgressIndicator(
                                                progress = { downloadProgress!! },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                text = "Mengunduh file APK: ${(downloadProgress!! * 100).toInt()}% selesai",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    },
                                    confirmButton = {}
                                )
                            }

                            if (updateErrorMessage != null) {
                                AlertDialog(
                                    onDismissRequest = { updateErrorMessage = null },
                                    title = { Text("Pembaruan Gagal") },
                                    text = { Text(updateErrorMessage!!) },
                                    confirmButton = {
                                        TextButton(onClick = { updateErrorMessage = null }) {
                                            Text("Tutup")
                                        }
                                    }
                                )
                            }

                            if (showPermissionRequiredDialog) {
                                AlertDialog(
                                    onDismissRequest = { showPermissionRequiredDialog = false },
                                    title = { Text("Izin Instalasi Diperlukan") },
                                    text = {
                                        Text("Aplikasi membutuhkan izin untuk menginstal aplikasi dari sumber tidak dikenal agar dapat melakukan pembaruan otomatis. Silakan aktifkan di Pengaturan.")
                                    },
                                    confirmButton = {
                                        TextButton(
                                            onClick = {
                                                showPermissionRequiredDialog = false
                                                if (downloadedFile != null) {
                                                    com.example.util.AppUpdateChecker.installApk(context, downloadedFile!!)
                                                }
                                            }
                                        ) {
                                            Text("Buka Pengaturan")
                                        }
                                    },
                                    dismissButton = {
                                        TextButton(onClick = { showPermissionRequiredDialog = false }) {
                                            Text("Batal")
                                        }
                                    }
                                )
                            }

                            
                            Box(modifier = Modifier.weight(1f)) {
                                when (currentTab) {
                                    TabItem.WEEKLY_MEETING -> com.example.ui.screens.WeeklyMeetingCombinedScreen(
                                        viewModel = viewmodel,
                                        onOpenDrawer = {
                                            scope.launch { drawerState.open() }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    TabItem.ABSEN_WEEKLY_MEETING -> com.example.ui.screens.WeeklyMeetingCombinedScreen(
                                        viewModel = viewmodel,
                                        onOpenDrawer = {
                                            scope.launch { drawerState.open() }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    TabItem.DASHBOARD -> DashboardScreen(
                                        viewModel = viewmodel,
                                        onNavigateToForm = { currentTab = TabItem.TAMBAH },
                                        onNavigateToChat = { isInChatScreen = true },
                                        onOpenDrawer = {
                                            scope.launch { drawerState.open() }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    TabItem.TASK -> com.example.ui.screens.TaskDashboardScreen(
                                        viewModel = viewmodel,
                                        onOpenDrawer = {
                                            scope.launch { drawerState.open() }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        initialSubTab = 0,
                                        isUploadIgOnly = false,
                                        onNavigateToChat = { isInChatScreen = true },
                                        onNavigateToForm = { currentTab = TabItem.TAMBAH }
                                    )
                                    TabItem.UPLOAD_IG -> com.example.ui.screens.TaskDashboardScreen(
                                        viewModel = viewmodel,
                                        onOpenDrawer = {
                                            scope.launch { drawerState.open() }
                                        },
                                        modifier = Modifier.fillMaxSize(),
                                        initialSubTab = 2,
                                        isUploadIgOnly = true,
                                        onNavigateToChat = { isInChatScreen = true },
                                        onNavigateToForm = { currentTab = TabItem.TAMBAH }
                                    )
                                    TabItem.ANALYTIC -> AnalyticScreen(
                                        viewModel = viewmodel,
                                        onNavigateBack = { currentTab = TabItem.DASHBOARD },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    TabItem.TAMBAH -> InputFormScreen(
                                        viewModel = viewmodel,
                                        onNavigateBack = { currentTab = TabItem.DASHBOARD },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    TabItem.PENGATURAN -> SettingsScreen(
                                        viewModel = viewmodel,
                                        onNavigateBack = { currentTab = TabItem.DASHBOARD },
                                        modifier = Modifier.fillMaxSize()
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
