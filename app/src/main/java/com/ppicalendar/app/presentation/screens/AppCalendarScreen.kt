package com.ppicalendar.app.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.ppicalendar.app.ui.theme.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.ppicalendar.app.domain.model.PlacementEvent
import com.ppicalendar.app.presentation.MainViewModel
import com.ppicalendar.app.presentation.components.EventCard
import com.ppicalendar.app.presentation.components.SynclyHeader
import com.ppicalendar.app.ui.theme.SynclyPrimaryAmber
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AppCalendarScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val allEvents by viewModel.allEvents.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val availableCalendars by viewModel.availableCalendars.collectAsState()

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var isAccountDialogVisible by remember { mutableStateOf(false) }
    var selectedCalendarCandidate by remember { mutableStateOf<com.ppicalendar.app.domain.model.CalendarInfo?>(null) }
    val currentCalendar = availableCalendars.find { it.id == settings.selectedCalendarId }
    
    // Toggle state: true = month grid, false = day timeline strip
    var isMonthView by remember { mutableStateOf(false) } 

    val eventsByDate = remember(allEvents) {
        allEvents.groupBy { it.date }.filterKeys { it.isNotBlank() }
    }

    val selectedDateStr = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val eventsForSelectedDate = eventsByDate[selectedDateStr] ?: emptyList()

    val connectedEmail = settings.connectedEmail.ifBlank {
        availableCalendars.find { it.id == settings.selectedCalendarId }?.accountName
            ?: availableCalendars.firstOrNull { it.isPrimary }?.accountName
            ?: "User"
    }
    val initial = connectedEmail.firstOrNull()?.uppercaseChar() ?: 'U'

    Column(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        SynclyHeader(
            title = "Calendar",
            isHighContrast = settings.isDarkTheme,
            actions = {
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

        // Main Content Area
        Column(modifier = Modifier.weight(1f)) {
            
            // Image-like Header: Month Year + Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val monthName = currentMonth.month.name.lowercase(Locale.ROOT)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                Text(
                    text = "$monthName ${currentMonth.year}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                // Segmented Toggle
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isMonthView) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { isMonthView = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarViewMonth,
                            contentDescription = "Month View",
                            tint = if (isMonthView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (!isMonthView) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { isMonthView = false }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ViewAgenda,
                            contentDescription = "Day View",
                            tint = if (!isMonthView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            if (isMonthView) {
                // WHOLE MONTH CALENDAR
                Column(modifier = Modifier.fillMaxSize()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Days of week
                            // Days of week header with light gray background
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Gray200) // subtle gray background
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                                Text(
                                    text = day,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
    
                            Spacer(modifier = Modifier.height(12.dp))
    
                            // Calendar Grid
                            val firstDayOfMonth = currentMonth.atDay(1)
                            val daysInMonth = currentMonth.lengthOfMonth()
                            val startOffset = firstDayOfMonth.dayOfWeek.value % 7 
    
                            val totalCells = startOffset + daysInMonth
                            val rows = (totalCells + 6) / 7
    
                            var currentDay = 1
                            for (row in 0 until rows) {
                                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    for (col in 0..6) {
                                        if (row == 0 && col < startOffset || currentDay > daysInMonth) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        } else {
                                            val date = currentMonth.atDay(currentDay)
                                            val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                            val eventCount = eventsByDate[dateStr]?.size ?: 0
                                            val isSelected = date == selectedDate
    
                                            Box(
                                                modifier = Modifier.weight(1f),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Date Circle
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) SynclyPrimaryAmber else Color.Transparent)
                                                        .clickable { 
                                                            selectedDate = date 
                                                            // Removed redirection to day view as requested
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = currentDay.toString(),
                                                        color = if (isSelected) Color(0xFF231B00) else MaterialTheme.colorScheme.onSurface,
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                    )
                                                }
                                                // Notification Badge
                                                if (eventCount > 0) {
                                                    Box(
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .offset(x = 2.dp, y = (-2).dp)
                                                            .size(16.dp)
                                                            .zIndex(1f)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF4CAF50))
                                                            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = eventCount.toString(),
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                            currentDay++
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Events list directly below the month calendar
                    Text(
                        text = "Events on ${selectedDate.format(DateTimeFormatter.ofPattern("MMM dd"))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )

                    if (eventsForSelectedDate.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No events scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(eventsForSelectedDate, key = { it.id }) { event ->
                                EventCard(
                                    event = event,
                                    onConfirm = { viewModel.confirmEvent(event) },
                                    onEdit = { viewModel.openEditDialog(event) },
                                    onDismiss = { viewModel.deleteEvent(event.id) },
                                    onDelete = { viewModel.deleteEvent(event.id) }
                                )
                            }
                        }
                    }
                }
            } else {
                // THIS DAY CALENDAR (HORIZONTAL STRIP + TIMELINE)
                
                // Horizontal Strip - Fixed Anchor to prevent jumping
                val anchorDate = remember { LocalDate.now() }
                val startDate = remember(anchorDate) { anchorDate.minusDays(180) }
                val selectedIndex = java.time.temporal.ChronoUnit.DAYS.between(startDate, selectedDate).toInt()
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = maxOf(0, selectedIndex - 2))
                
                LazyRow(
                    state = listState,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // tighter spacing for squircles
                ) {
                    items(365) { offset ->
                        val date = startDate.plusDays(offset.toLong())
                        val isSelected = date == selectedDate
                        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        val eventCount = eventsByDate[dateStr]?.size ?: 0
                        
                        Box(contentAlignment = Alignment.Center) {
                            Column(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(16.dp)) // App Icon Shape (Squircle)
                                    .background(if (isSelected) SynclyPrimaryAmber else Gray100)
                                    .border(1.dp, Gray300, RoundedCornerShape(16.dp))
                                    .clickable { 
                                        selectedDate = date 
                                        currentMonth = YearMonth.from(date)
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = date.dayOfWeek.name.take(3).lowercase(Locale.ROOT).replaceFirstChar { it.titlecase(Locale.ROOT) },
                                    fontSize = 11.sp,
                                    color = if (isSelected) PureBlack else MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = date.dayOfMonth.toString(),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) PureBlack else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            
                            // App-Icon style Badge
                            if (eventCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-4).dp)
                                        .size(20.dp)
                                        .zIndex(1f)
                                        .clip(CircleShape)
                                        .background(AccentGreen)
                                        .border(1.5.dp, MaterialTheme.colorScheme.background, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = eventCount.toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(20.dp))

                // Timeline container with white rounded top
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 24.dp)) {
                        
                        val dayName = selectedDate.dayOfWeek.name.lowercase(Locale.ROOT)
                            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
                        Text(
                            text = "$dayName ${selectedDate.dayOfMonth}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))

                        if (eventsForSelectedDate.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No events scheduled", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(eventsForSelectedDate.sortedBy { it.startTime }) { event ->
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        // Time Column
                                        Column(
                                            modifier = Modifier.width(60.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = event.startTime,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        // Card Column
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(SynclyPrimaryAmber.copy(alpha = 0.15f))
                                                .clickable { viewModel.openEditDialog(event) }
                                                .padding(16.dp)
                                        ) {
                                            Column {
                                                Text(
                                                    text = event.company,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontSize = 16.sp
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.Circle,
                                                        contentDescription = null,
                                                        tint = SynclyPrimaryAmber,
                                                        modifier = Modifier.size(8.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = event.eventType.displayName + (if (event.venue != null) " • ${event.venue}" else ""),
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 12.sp
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

        com.ppicalendar.app.presentation.components.AccountSelectionDialog(
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
    }
}
