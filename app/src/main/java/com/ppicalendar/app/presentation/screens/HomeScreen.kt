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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import com.ppicalendar.app.domain.model.CalendarInfo
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
    val liveSettings by viewModel.settings.collectAsState()
    val availableCalendars by viewModel.availableCalendars.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var pointsFilter by remember { mutableStateOf(PointsFilter.ALL) }
    var eventToDelete by remember { mutableStateOf<PlacementEvent?>(null) }
    var isAccountDialogVisible by remember { mutableStateOf(false) }
    var selectedCalendarCandidate by remember { mutableStateOf<CalendarInfo?>(null) }

    val connectedEmail = liveSettings.connectedEmail.ifBlank {
        availableCalendars.find { it.id == liveSettings.selectedCalendarId }?.accountName
            ?: availableCalendars.firstOrNull { it.isPrimary }?.accountName
            ?: ""
    }

    val currentCalendar = availableCalendars.find { it.id == liveSettings.selectedCalendarId }
        ?: availableCalendars.find { it.accountName.equals(connectedEmail, ignoreCase = true) }
        ?: availableCalendars.firstOrNull { it.isPrimary }
        ?: availableCalendars.firstOrNull()

    fun formatMaskedEmail(email: String): String {
        if (email.isBlank()) return "Connect Cal"
        val parts = email.split("@")
        val user = parts[0]
        val domain = if (parts.size > 1) "@" + parts[1] else ""
        val prefix = if (user.length > 3) user.take(3) else user
        val fullMasked = "$prefix...xxxx$domain"
        return if (fullMasked.length > 20) fullMasked.take(17) + "..." else fullMasked
    }

    val createdEvents = remember(allEvents) { allEvents.filter { it.status == EventStatus.CREATED_IN_CALENDAR } }
    val createdCount = createdEvents.size
    val pendingCount = pendingEvents.size

    val displayedEvents = when (selectedTab) {
        0 -> createdEvents
        1 -> pendingEvents
        2 -> allEvents
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
            // Sticky Top Header with Google Calendar Account Chip
            com.ppicalendar.app.presentation.components.SynclyHeader(
                title = "Syncly",
                actions = {
                    androidx.compose.material3.Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFF0C2),
                        border = BorderStroke(1.dp, Color(0xFFE5D5A0)),
                        modifier = Modifier.clickable {
                            viewModel.refreshCalendars()
                            selectedCalendarCandidate = currentCalendar
                            isAccountDialogVisible = true
                        }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Google Calendar Account",
                                tint = Color(0xFF524000),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            androidx.compose.material3.Text(
                                text = if (connectedEmail.isNotBlank()) formatMaskedEmail(connectedEmail) else "Connect Cal",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B2E00),
                                maxLines = 1
                            )
                        }
                    }
                }
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

                // Interactive KPI Metric Cards acting as Tabs
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
                            isSelected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "Pending",
                            count = pendingCount.toString(),
                            icon = Icons.Default.PendingActions,
                            isSelected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            modifier = Modifier.weight(1f)
                        )
                        StatMetricCard(
                            title = "All",
                            count = allEvents.size.toString(),
                            icon = Icons.Default.NotificationsActive,
                            isSelected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            modifier = Modifier.weight(1f)
                        )
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

        // Google Calendar Account Picker Dialog
        if (isAccountDialogVisible) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { isAccountDialogVisible = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Google Calendar Account", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Choose Google account for automatically syncing placement schedules & reminders:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (availableCalendars.isEmpty()) {
                            Text(
                                text = "No calendar accounts detected. Ensure Calendar permission is granted in Access tab.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            availableCalendars.forEach { cal ->
                                val isSelected = (selectedCalendarCandidate?.id == cal.id) ||
                                        (selectedCalendarCandidate == null && cal.id == currentCalendar?.id)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedCalendarCandidate = cal },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )

                                        Spacer(modifier = Modifier.width(10.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cal.accountName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = cal.displayName + if (cal.isPrimary) " (Primary)" else "",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            selectedCalendarCandidate?.let {
                                viewModel.confirmCalendarConnection(it)
                            }
                            isAccountDialogVisible = false
                        },
                        enabled = selectedCalendarCandidate != null || availableCalendars.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Confirm & Sync", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { isAccountDialogVisible = false }) {
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
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
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
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = count,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}
