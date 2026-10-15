package com.ppicalendar.app.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ppicalendar.app.domain.model.CalendarInfo
import com.ppicalendar.app.domain.model.EventStatus
import com.ppicalendar.app.domain.model.EventType
import com.ppicalendar.app.domain.model.PlacementEvent
import com.ppicalendar.app.presentation.MainViewModel
import com.ppicalendar.app.presentation.components.AccountSelectionDialog
import com.ppicalendar.app.presentation.components.EventCard
import com.ppicalendar.app.presentation.components.PermissionBanner
import com.ppicalendar.app.presentation.components.SynclyFooter
import com.ppicalendar.app.presentation.components.SynclyHeader
import com.ppicalendar.app.ui.theme.SynclyPrimaryAmber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class EventTypeFilter {
    ALL,
    PPT,
    OA,
    RESUME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val allEvents by viewModel.allEvents.collectAsState()
    val isPermissionGranted by viewModel.isNotificationListenerGranted.collectAsState()
    val liveSettings by viewModel.settings.collectAsState()
    val availableCalendars by viewModel.availableCalendars.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var eventTypeFilter by remember { mutableStateOf(EventTypeFilter.ALL) }
    var isArchiveExpanded by remember { mutableStateOf(false) }
    var eventToDelete by remember { mutableStateOf<PlacementEvent?>(null) }
    var isAccountDialogVisible by remember { mutableStateOf(false) }
    var selectedCalendarCandidate by remember { mutableStateOf<CalendarInfo?>(null) }
    var showRestrictedDialog by remember { mutableStateOf(false) }

    val connectedEmail = liveSettings.connectedEmail.ifBlank {
        availableCalendars.find { it.id == liveSettings.selectedCalendarId }?.accountName
            ?: availableCalendars.firstOrNull { it.isPrimary }?.accountName
            ?: ""
    }

    val currentCalendar = availableCalendars.find { it.id == liveSettings.selectedCalendarId }
        ?: availableCalendars.find { it.accountName.equals(connectedEmail, ignoreCase = true) }
        ?: availableCalendars.firstOrNull { it.isPrimary }
        ?: availableCalendars.firstOrNull()

    val currentDateTime = remember(allEvents) { LocalDateTime.now() }

    fun isEventElapsed(event: PlacementEvent): Boolean {
        if (event.date.isBlank()) return false
        return try {
            val timeToUse = when {
                !event.endTime.isNullOrBlank() -> event.endTime
                event.startTime.isNotBlank() -> event.startTime
                else -> "23:59"
            }
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            val eventEndDateTime = LocalDateTime.parse("${event.date} $timeToUse", formatter)
            eventEndDateTime.isBefore(currentDateTime)
        } catch (e: Exception) {
            false
        }
    }

    // Filter events by tab and status
    val baseEvents = remember(allEvents, selectedTab) {
        when (selectedTab) {
            0 -> allEvents.filter { it.status == EventStatus.CREATED_IN_CALENDAR }
            1 -> allEvents.filter { it.status != EventStatus.CREATED_IN_CALENDAR && it.status != EventStatus.DISMISSED }
            2 -> allEvents.filter { it.status != EventStatus.DISMISSED }
            else -> allEvents
        }
    }

    // Partition into Past Events and Active (Upcoming) Events per PRD Section 6.3
    val (pastEvents, upcomingEvents) = remember(baseEvents, currentDateTime) {
        baseEvents.partition { isEventElapsed(it) }
    }

    val createdCount = allEvents.count { it.status == EventStatus.CREATED_IN_CALENDAR && !isEventElapsed(it) }
    val pendingCount = allEvents.count { it.status != EventStatus.CREATED_IN_CALENDAR && it.status != EventStatus.DISMISSED && !isEventElapsed(it) }

    fun applyTypeFilter(list: List<PlacementEvent>): List<PlacementEvent> {
        return when (eventTypeFilter) {
            EventTypeFilter.ALL -> list
            EventTypeFilter.PPT -> list.filter { it.eventType == EventType.PRE_PLACEMENT_TALK }
            EventTypeFilter.OA -> list.filter { it.eventType == EventType.ONLINE_ASSESSMENT }
            EventTypeFilter.RESUME -> list.filter { it.eventType == EventType.RESUME_SUBMISSION || it.eventType == EventType.RESUME_DEADLINE }
        }
    }

    val filteredUpcoming = remember(upcomingEvents, eventTypeFilter) {
        applyTypeFilter(upcomingEvents)
    }

    val filteredPast = remember(pastEvents, eventTypeFilter) {
        applyTypeFilter(pastEvents)
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            SynclyHeader(
                title = "Syncly",
                isHighContrast = liveSettings.isDarkTheme,
                actions = {
                    val initial = liveSettings.connectedEmail.firstOrNull()?.uppercaseChar() ?: 'U'
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SynclyPrimaryAmber)
                            .border(1.dp, Color(0xFFE5D5A0), CircleShape)
                            .clickable {
                                viewModel.refreshCalendars()
                                selectedCalendarCandidate = currentCalendar
                                isAccountDialogVisible = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initial.toString(),
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF231B00),
                            fontSize = 18.sp
                        )
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
                // Permission Warning Banner
                if (!isPermissionGranted) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            PermissionBanner(onFixPermissions = {
                                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                                    showRestrictedDialog = true
                                } else {
                                    onNavigateToPermissions()
                                }
                            })
                        }
                    }
                }

                // Interactive KPI Metric Cards
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
                    }
                }

                // Event Type Filters
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EventTypeFilter.entries.forEach { filter ->
                            val isSelected = eventTypeFilter == filter
                            FilterChip(
                                selected = isSelected,
                                onClick = { eventTypeFilter = filter },
                                label = {
                                    Text(
                                        text = when (filter) {
                                            EventTypeFilter.ALL -> "All"
                                            EventTypeFilter.PPT -> "PPT"
                                            EventTypeFilter.OA -> "OA"
                                            EventTypeFilter.RESUME -> "Resume"
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

                // Section 6: Telegram-style Past Events Archive Entry Card
                if (filteredPast.isNotEmpty()) {
                    item(key = "archive_entry_card") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .clickable { isArchiveExpanded = !isArchiveExpanded },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Archive,
                                                contentDescription = "Past Events",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = "Past Events",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${filteredPast.size} completed events",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isArchiveExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Expanded Past Events List (muted gray appearance and reduced opacity per Section 6.2)
                    if (isArchiveExpanded) {
                        val groupedPast = filteredPast
                            .sortedWith(compareBy({ it.date }, { it.startTime }))
                            .groupBy { it.date }

                        groupedPast.forEach { (dateStr, events) ->
                            val displayDate = if (dateStr.isNotBlank()) {
                                com.ppicalendar.app.data.extractor.DateTimeParser.formatIndianDate(dateStr)
                            } else "Unknown Date"

                            item(key = "past_header_" + dateStr) {
                                Text(
                                    text = displayDate,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(start = 20.dp, top = 4.dp, end = 16.dp, bottom = 2.dp)
                                )
                            }

                            items(events, key = { "past_" + it.id }) { event ->
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp)
                                        .alpha(0.6f)
                                ) {
                                    EventCard(
                                        event = event,
                                        onConfirm = { viewModel.confirmEvent(event) },
                                        onEdit = { viewModel.openEditDialog(event) },
                                        onDismiss = { viewModel.deleteEvent(event.id) },
                                        onDelete = { eventToDelete = event },
                                        isPendingView = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Active / Upcoming Events
                if (filteredUpcoming.isEmpty() && (!isArchiveExpanded || filteredPast.isEmpty())) {
                    item {
                        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                                        text = if (eventTypeFilter != EventTypeFilter.ALL)
                                            "No events match the selected event type filter."
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
                    val groupedUpcoming = filteredUpcoming
                        .sortedWith(compareBy({ it.date }, { it.startTime }))
                        .groupBy { it.date }

                    groupedUpcoming.forEach { (dateStr, events) ->
                        val displayDate = if (dateStr.isNotBlank()) {
                            com.ppicalendar.app.data.extractor.DateTimeParser.formatIndianDate(dateStr)
                        } else {
                            "Unknown Date"
                        }

                        item(key = "header_" + dateStr) {
                            Text(
                                text = displayDate,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 2.dp)
                            )
                        }

                        items(events, key = { it.id }) { event ->
                            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                EventCard(
                                    event = event,
                                    onConfirm = { viewModel.confirmEvent(event) },
                                    onEdit = { viewModel.openEditDialog(event) },
                                    onDismiss = { viewModel.deleteEvent(event.id) },
                                    onDelete = { eventToDelete = event },
                                    isPendingView = selectedTab == 1 // PRD Section 5.1: Update button in Pending
                                )
                            }
                        }
                    }
                }

                // Footer
                item {
                    SynclyFooter()
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }

        // Delete Event Confirmation Popup Dialog
        eventToDelete?.let { event ->
            AlertDialog(
                onDismissRequest = { eventToDelete = null },
                title = { Text(text = "Delete Event?", fontWeight = FontWeight.Bold) },
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
                    TextButton(onClick = { eventToDelete = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Google Calendar Account Picker Dialog
        AccountSelectionDialog(
            isVisible = isAccountDialogVisible,
            onDismiss = { isAccountDialogVisible = false },
            availableCalendars = availableCalendars,
            selectedCalendarCandidate = selectedCalendarCandidate,
            currentCalendar = currentCalendar,
            onCalendarSelect = { selectedCalendarCandidate = it },
            onConfirm = {
                selectedCalendarCandidate?.let {
                    viewModel.confirmCalendarConnection(it)
                }
                isAccountDialogVisible = false
            }
        )

        // Floating Action Button for Event Creation (Two Methods: Parsing & Quick Form)
        FloatingActionButton(
            onClick = { viewModel.openSimulator() },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Event")
        }

        if (showRestrictedDialog) {
            com.ppicalendar.app.presentation.dialogs.RestrictedSettingsDialog(
                onDismiss = {
                    showRestrictedDialog = false
                    viewModel.checkPermissions()
                }
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
