import sys

path = r'C:\Users\HP\Downloads\Syncly\app\src\main\java\com\ppicalendar\app\presentation\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Make sure imports exist
if 'androidx.compose.ui.draw.alpha' not in content:
    content = content.replace('import androidx.compose.ui.Modifier', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.draw.alpha')

if 'androidx.compose.material.icons.filled.KeyboardArrowUp' not in content:
    content = content.replace('import androidx.compose.material.icons.filled.Add', 'import androidx.compose.material.icons.filled.Add\nimport androidx.compose.material.icons.filled.KeyboardArrowUp\nimport androidx.compose.material.icons.filled.KeyboardArrowDown')

# Add showPastEvents state
state_old = 'var eventTypeFilter by remember { mutableStateOf(EventTypeFilter.ALL) }'
state_new = 'var eventTypeFilter by remember { mutableStateOf(EventTypeFilter.ALL) }\n    var showPastEvents by remember { mutableStateOf(false) }'
if state_old in content and state_new not in content:
    content = content.replace(state_old, state_new)

# Replace list logic
old_block = '''                } else {
                    // Group and sort by Date and Time
                    val groupedEvents = filteredEvents
                        .sortedWith(compareBy({ it.date }, { it.startTime }))
                        .groupBy { it.date }

                    groupedEvents.forEach { (dateStr, events) ->
                        val displayDate = if (dateStr.isNotBlank()) {
                            com.ppicalendar.app.data.extractor.DateTimeParser.formatIndianDate(dateStr)
                        } else {
                            "Unknown Date"
                        }
                        
                        item(key = "header_$" + dateStr) {
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
                                    onDismiss = { viewModel.dismissEvent(event.id) },
                                    onDelete = { eventToDelete = event }
                                )
                            }
                        }
                    }
                }'''

new_block = '''                } else {
                    val currentDateTime = java.time.LocalDateTime.now()
                    
                    val (pastEvents, upcomingEvents) = filteredEvents.partition { event ->
                        if (event.date.isNotBlank() && event.startTime.isNotBlank()) {
                            try {
                                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                                val eventDateTime = java.time.LocalDateTime.parse(" ", formatter)
                                eventDateTime.isBefore(currentDateTime)
                            } catch (e: Exception) {
                                false
                            }
                        } else {
                            false
                        }
                    }
                    
                    // 1. Past Events Bar
                    if (pastEvents.isNotEmpty()) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .clickable { showPastEvents = !showPastEvents },
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Past Events ()", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Icon(
                                        imageVector = if (showPastEvents) androidx.compose.material.icons.filled.KeyboardArrowUp else androidx.compose.material.icons.filled.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (showPastEvents) {
                            val groupedPast = pastEvents.sortedWith(compareBy({ it.date }, { it.startTime })).groupBy { it.date }
                            groupedPast.forEach { (dateStr, events) ->
                                val displayDate = if (dateStr.isNotBlank()) {
                                    com.ppicalendar.app.data.extractor.DateTimeParser.formatIndianDate(dateStr)
                                } else "Unknown Date"
                                
                                item(key = "past_header_" + dateStr) {
                                    Text(
                                        text = displayDate,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 2.dp)
                                    )
                                }
                                
                                items(events, key = { it.id }) { event ->
                                    Box(modifier = Modifier.padding(horizontal = 16.dp).alpha(0.5f)) {
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
                        }
                    }

                    // 2. Upcoming Events
                    val groupedUpcoming = upcomingEvents.sortedWith(compareBy({ it.date }, { it.startTime })).groupBy { it.date }
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
                                    onDismiss = { viewModel.dismissEvent(event.id) },
                                    onDelete = { eventToDelete = event }
                                )
                            }
                        }
                    }
                }'''

content = content.replace(old_block, new_block)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("HomeScreen updated for Past Events")
