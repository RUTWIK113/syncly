package com.ppicalendar.app.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import com.ppicalendar.app.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ppicalendar.app.domain.model.EventStatus
import com.ppicalendar.app.domain.model.PlacementEvent
import com.ppicalendar.app.presentation.MainViewModel
import com.ppicalendar.app.presentation.components.EventCard
import com.ppicalendar.app.presentation.components.PermissionBanner
import com.ppicalendar.app.ui.theme.DarkBorder
import com.ppicalendar.app.ui.theme.Gray300
import com.ppicalendar.app.ui.theme.Gray500
import com.ppicalendar.app.ui.theme.Gray700
import com.ppicalendar.app.ui.theme.Gray800
import com.ppicalendar.app.ui.theme.LightBorder
import com.ppicalendar.app.ui.theme.PureBlack
enum class PointsFilter {
    ALL,
    POINTS_YES,
    POINTS_NO
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allEvents by viewModel.allEvents.collectAsState()
    val pendingEvents by viewModel.pendingEvents.collectAsState()
    val isPermissionGranted by viewModel.isNotificationListenerGranted.collectAsState()
    val testNoticeCount by viewModel.testNoticeCount.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var pointsFilter by remember { mutableStateOf(PointsFilter.ALL) }
    var eventToDelete by remember { mutableStateOf<PlacementEvent?>(null) }

    val createdEvents = remember(allEvents) { allEvents.filter { it.status == EventStatus.CREATED_IN_CALENDAR } }
    val createdCount = createdEvents.size
    val pendingCount = pendingEvents.size

    val displayedEvents = when (selectedTab) {
        0 -> createdEvents
        1 -> pendingEvents
        else -> createdEvents
    }

    val filteredEvents = remember(displayedEvents, pointsFilter) {
        when (pointsFilter) {
            PointsFilter.ALL -> displayedEvents
            PointsFilter.POINTS_YES -> displayedEvents.filter { (it.incentivePoints ?: 0) > 0 }
            PointsFilter.POINTS_NO -> displayedEvents.filter { (it.incentivePoints ?: 0) == 0 }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sticky Top Header with Syncly S Logo (syncly_s)
            com.ppicalendar.app.presentation.components.SynclyHeader(
                title = "Syncly",
                subtitle = "Turn campus updates into calendar events.",
                logo = painterResource(id = R.drawable.syncly_s)
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Permission Warning Banner if missing
                if (!isPermissionGranted) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PermissionBanner(onFixPermissions = onNavigateToPermissions)
                        }
                    }
                }

                // Stat Cards Row - Synced, Pending, Total
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatMetricCard(
                            title = "Synced",
                            count = createdCount.toString(),
                            icon = Icons.Default.CheckCircleOutline,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "Pending",
                            count = pendingCount.toString(),
                            icon = Icons.Default.PendingActions,
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "Total",
                            count = allEvents.size.toString(),
                            icon = Icons.Default.NotificationsActive,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Tab Selection - Streamlined to Synced (Default) & Pending
                item {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        SecondaryTabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = {
                                    Text(
                                        text = "Synced ($createdCount)",
                                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = {
                                    Text(
                                        text = "Pending ($pendingCount)",
                                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            )
                        }
                    }
                }

                // Incentive Points Filter Chips (All, points=yes, points=no)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PointsFilter.entries.forEach { filter ->
                            val isSelected = pointsFilter == filter
                            val count = when (filter) {
                                PointsFilter.ALL -> displayedEvents.size
                                PointsFilter.POINTS_YES -> displayedEvents.count { (it.incentivePoints ?: 0) > 0 }
                                PointsFilter.POINTS_NO -> displayedEvents.count { (it.incentivePoints ?: 0) == 0 }
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { pointsFilter = filter },
                                label = {
                                    Text(
                                        text = when (filter) {
                                            PointsFilter.ALL -> "All ($count)"
                                            PointsFilter.POINTS_YES -> "🎯 Points = Yes ($count)"
                                            PointsFilter.POINTS_NO -> "Points = No ($count)"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                        }
                    }
                }

                // List of Events
                if (filteredEvents.isEmpty()) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(54.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.EventNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = if (selectedTab == 0) "No synced calendar events" else "No pending placement notices",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = if (pointsFilter != PointsFilter.ALL)
                                            "No events match the selected points filter."
                                        else if (selectedTab == 0)
                                            "Notices confirmed or auto-synced to your Google Calendar will appear here."
                                        else
                                            "Incoming WhatsApp placement notices will be extracted and shown here automatically.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(filteredEvents, key = { it.id }) { event ->
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            EventCard(
                                event = event,
                                onConfirm = { viewModel.confirmEvent(event) },
                                onEdit = { viewModel.openEditDialog(event) },
                                onDismiss = { viewModel.dismissEvent(event.id) },
                                onDelete = { eventToDelete = event }
                            )
                        }
                    }
                }

                // Trademark Footer: Crafted by Rutwik
                item {
                    com.ppicalendar.app.presentation.components.SynclyFooter()
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }

        // Delete Event Confirmation Popup Dialog
        eventToDelete?.let { event ->
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { eventToDelete = null },
                title = {
                    Text(
                        text = "Delete Event?",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete '${event.formattedTitle}'?\n\nThis will remove the event from Syncly and delete it from your Google Calendar. The company record in Company Vault will remain saved.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteEvent(event.id)
                            eventToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Delete", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { eventToDelete = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Floating Action Button - Removed automatically after used 3 times
        if (testNoticeCount < 3) {
            val remaining = 3 - testNoticeCount
            ExtendedFloatingActionButton(
                onClick = { viewModel.openSimulator() },
                icon = { Icon(Icons.Default.Science, contentDescription = null) },
                text = { Text("Test Notice ($remaining left)", fontWeight = FontWeight.Bold) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            )
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    count: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = count,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
