package com.example.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import kotlinx.coroutines.launch

/**
 * PublishScreen combines Upload IG + Scheduling into one cohesive screen.
 * Tab 0 = Upload IG (from TaskDashboardScreen with isUploadIgOnly=true)
 * Tab 1 = Scheduling (embedded SchedulingScreenContent — not a dialog)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PublishScreen(
    viewModel: ScheduleViewModel,
    onOpenDrawer: () -> Unit,
    onNavigateToChat: (() -> Unit)? = null,
    onNavigateToForm: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val igSyncStatus by viewModel.weeklyMeetingIgSyncStatus.collectAsState()
    val unreadCount by viewModel.unreadChatCount.collectAsState()
    val selectedMonth by viewModel.selectedMonth.collectAsState()
    var isSearchExpanded by remember { mutableStateOf(false) }

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
                    text = if (pagerState.currentPage == 0) "Upload Instagram" else "Instagram Scheduling",
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
                onClick = { viewModel.fetchWeeklyMeetingIgListings(selectedMonth, forceRefresh = true) },
                enabled = igSyncStatus !is SyncState.Loading
            ) {
                if (igSyncStatus is SyncState.Loading) {
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

        // TabRow with sliding indicator
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(46.dp),
            indicator = { tabPositions ->
                if (pagerState.currentPage < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        ) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(0)
                    }
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Upload IG",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (pagerState.currentPage == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(1)
                    }
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.EditCalendar,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scheduling",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (pagerState.currentPage == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        )
                    }
                }
            )
        }

        // HorizontalPager hosting Upload IG and Scheduling views
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> TaskDashboardScreen(
                    viewModel = viewModel,
                    onOpenDrawer = onOpenDrawer,
                    initialSubTab = 2,
                    isUploadIgOnly = true,
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToForm = onNavigateToForm,
                    isExternalFilterExpanded = isSearchExpanded
                )
                1 -> SchedulingScreenContent(
                    viewModel = viewModel
                )
            }
        }
    }
}
