package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.ScheduleViewModel
import com.example.ui.SyncState

/**
 * PublishScreen combines Upload IG + Scheduling into one cohesive screen.
 * Tab 0 = Upload IG (from TaskDashboardScreen with isUploadIgOnly=true)
 * Tab 1 = Scheduling (embedded SchedulingScreenContent — not a dialog)
 */
@Composable
fun PublishScreen(
    viewModel: ScheduleViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToChat: (() -> Unit)? = null,
    onNavigateToForm: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var publishMode by remember { mutableIntStateOf(0) } // 0 = Feeds, 1 = Reels, 2 = Scheduling
    val igSyncStatus by viewModel.weeklyMeetingIgSyncStatus.collectAsState()
    val reelsSyncStatus by viewModel.reelsSyncStatus.collectAsState()
    val unreadCount by viewModel.unreadChatCount.collectAsState()
    val publishSelectedMonth by viewModel.publishSelectedMonth.collectAsState()
    var isSearchExpanded by remember { mutableStateOf(false) }

    // NOTE: data fetching is handled by each tab (Upload IG / Scheduling) for its own selected month.
    // A separate forced fetch here used a different month and raced with the tabs, causing data to vanish.

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Unified Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onOpenDrawer) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Buka Menu Utama",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            ) {
                Text(
                    text = "Publish Desk",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (publishMode) {
                        1 -> "Reels · video Google Drive"
                        2 -> "Scheduling · $publishSelectedMonth"
                        else -> "Feeds · Instagram · $publishSelectedMonth"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Search / Filter Icon Button
            IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Cari / Filter",
                    tint = if (isSearchExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Top Refresh button
            IconButton(
                onClick = {
                    if (publishMode == 1) viewModel.fetchReels(true) else {
                        viewModel.refreshCurrentIgListings()
                        viewModel.syncDataSilently()
                    }
                },
                enabled = if (publishMode == 1) reelsSyncStatus !is SyncState.Loading else igSyncStatus !is SyncState.Loading
            ) {
                if ((publishMode == 1 && reelsSyncStatus is SyncState.Loading) ||
                    (publishMode == 0 && igSyncStatus is SyncState.Loading)) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chat button
            if (onNavigateToChat != null) {
                IconButton(onClick = onNavigateToChat) {
                    BadgedBox(
                        badge = {
                            if (unreadCount > 0) {
                                Badge { Text(unreadCount.toString()) }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = "Chat",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Primary content type selector. Kept separate from workflow tabs so the hierarchy stays clear.
        TabRow(
            selectedTabIndex = publishMode,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(44.dp),
            indicator = { tabPositions ->
                if (publishMode < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[publishMode]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            listOf(
                Triple(0, Icons.Default.GridView, "Feeds"),
                Triple(1, Icons.Default.SmartDisplay, "Reels"),
                Triple(2, Icons.Default.EditCalendar, "Scheduling")
            ).forEach { (index, icon, label) -> Tab(
                selected = publishMode == index,
                onClick = { publishMode = index },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (publishMode == index) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            ) }
        }

        when (publishMode) {
            0 -> TaskDashboardScreen(
                viewModel = viewModel,
                onOpenDrawer = onOpenDrawer,
                initialSubTab = 2,
                isUploadIgOnly = true,
                onNavigateToChat = onNavigateToChat,
                onNavigateToForm = onNavigateToForm,
                isExternalFilterExpanded = isSearchExpanded
            )
            1 -> ReelsPublishScreen(
                viewModel = viewModel,
                isSearchExpanded = isSearchExpanded,
                modifier = Modifier.fillMaxWidth().weight(1f)
            )
            else -> SchedulingScreenContent(viewModel = viewModel)
        }
    }
}
