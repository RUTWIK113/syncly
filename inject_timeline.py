import sys
import io
import os
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
path = r'C:\Users\HP\Downloads\Syncly\app\src\main\java\com\ppicalendar\app\presentation\screens\AppCalendarScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add timelineStartDate
if 'var timelineStartDate' not in content:
    content = content.replace('var selectedDate by remember { mutableStateOf(LocalDate.now()) }', 
                              'var selectedDate by remember { mutableStateOf(LocalDate.now()) }\n    var timelineStartDate by remember { mutableStateOf(LocalDate.now()) }')

# 2. Update TabRow
content = content.replace('onClick = { isMonthView = false }', 'onClick = { isMonthView = false; timelineStartDate = LocalDate.now() }')

# 3. Find the exact cut point:
# We know the end of the file looks like:
#                 }
#             }
#         }
#     }
# }
# com.ppicalendar.app.presentation.components.AccountSelectionDialog(...)
# }

marker = '                        )\n                    }\n                }\n            }'
idx = content.find(marker)
if idx != -1:
    cut_idx = idx + len(marker)
    # cut_idx is right after the } that closes 'else'
    
    timeline_code = '''
            } else {
                // 3-Day Timeline View
                Column(modifier = Modifier.fillMaxSize()) {
                    // Date header navigator
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { timelineStartDate = timelineStartDate.minusDays(3) }) {
                            Text("<", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        val d1 = timelineStartDate.format(DateTimeFormatter.ofPattern("MMM dd"))
                        val d3 = timelineStartDate.plusDays(2).format(DateTimeFormatter.ofPattern("MMM dd"))
                        Text("$d1 - $d3", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { timelineStartDate = timelineStartDate.plusDays(3) }) {
                            Text(">", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    val timelineDates = (0..2).map { timelineStartDate.plusDays(it.toLong()) }
                    
                    // Column Headers
                    Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Spacer(modifier = Modifier.width(48.dp))
                        timelineDates.forEach { date ->
                            val isToday = date == LocalDate.now()
                            Text(
                                text = date.format(DateTimeFormatter.ofPattern("EEE\\nMMM dd")),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                    
                    HorizontalDivider()
                    
                    // Scrollable hours
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items((8..22).toList()) { hour ->
                            Row(modifier = Modifier.fillMaxWidth().height(64.dp)) {
                                // Time label
                                Text(
                                    text = "$hour:00",
                                    modifier = Modifier.width(48.dp).padding(end = 8.dp, top = 4.dp),
                                    textAlign = TextAlign.End,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                // 3 Columns
                                timelineDates.forEach { date ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .border(0.5.dp, MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        val dateStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                        val eventsForHour = eventsByDate[dateStr]?.filter { 
                                            val eHour = it.startTime.split(":").firstOrNull()?.toIntOrNull() ?: 0
                                            eHour == hour 
                                        } ?: emptyList()
                                        
                                        Column(modifier = Modifier.fillMaxSize().padding(1.dp)) {
                                            eventsForHour.forEach { ev ->
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        .clickable { viewModel.openEditDialog(ev) }
                                                ) {
                                                    Column {
                                                        Text(ev.company, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                                        Text(ev.eventType.displayName, fontSize = 9.sp, maxLines = 1, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } // closes Column(weight=1f)

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
    } // closes Column(fillMaxSize)
} // closes AppCalendarScreen
'''
    content = content[:cut_idx] + timeline_code
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('Injected successfully!')
else:
    print('Marker not found!')
