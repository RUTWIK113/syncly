import sys
import io
import os
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

path = r'C:\Users\HP\Downloads\Syncly\app\src\main\java\com\ppicalendar\app\presentation\screens\AppCalendarScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add TabRow import
if 'androidx.compose.material3.TabRow' not in content:
    content = content.replace('import androidx.compose.material3.*', 'import androidx.compose.material3.*\nimport androidx.compose.material3.TabRow\nimport androidx.compose.material3.Tab')


state_marker = 'var selectedDate by remember { mutableStateOf(LocalDate.now()) }'
if state_marker in content and 'var isMonthView' not in content:
    new_states = state_marker + '\n    var isMonthView by remember { mutableStateOf(true) }'
    content = content.replace(state_marker, new_states)

col_marker = 'Column(modifier = Modifier.weight(1f)) {'
if col_marker in content:
    idx = content.find(col_marker) + len(col_marker)
    
    tab_row = """
            TabRow(selectedTabIndex = if (isMonthView) 0 else 1) {
                Tab(selected = isMonthView, onClick = { isMonthView = true }, text = { Text("Month Grid") })
                Tab(selected = !isMonthView, onClick = { isMonthView = false }, text = { Text("3-Day Timeline") })
            }
            if (isMonthView) {
"""
    content = content[:idx] + tab_row + content[idx:]
    
    dialog_marker = 'com.ppicalendar.app.presentation.components.AccountSelectionDialog('
    if dialog_marker in content:
        # We find the closing brace of the Column(modifier = Modifier.weight(1f)) which is right before the dialog usually...
        # Wait, the structure is:
        # Column(modifier = modifier.fillMaxSize()) {
        #    SynclyHeader(...)
        #    Column(modifier = Modifier.weight(1f)) {
        #        ...
        #    }
        # }
        # AccountSelectionDialog(...)
        
        # We need to insert "} else { ... }" right before the closing brace of `Column(modifier = Modifier.weight(1f))`
        # Let's search for the end of the `Column` by looking backwards from AccountSelectionDialog
        
        idx_dialog = content.find(dialog_marker)
        # There are two closing braces between the end of LazyColumn/Empty state and AccountSelectionDialog
        
        timeline_code = """
            } else {
                // 3-Day Timeline View
                Column(modifier = Modifier.fillMaxSize()) {
                    // Date header navigator
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { selectedDate = selectedDate.minusDays(3) }) {
                            Text("<", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        val d1 = selectedDate.format(DateTimeFormatter.ofPattern("MMM dd"))
                        val d3 = selectedDate.plusDays(2).format(DateTimeFormatter.ofPattern("MMM dd"))
                        Text("$d1 - $d3", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        TextButton(onClick = { selectedDate = selectedDate.plusDays(3) }) {
                            Text(">", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    val timelineDates = (0..2).map { selectedDate.plusDays(it.toLong()) }
                    
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
"""
        # Find the closing brace of the Column(modifier = Modifier.weight(1f))
        # Wait, the structure is:
        #             }
        #         }
        #     }
        # } // End of Column(weight = 1f)
        # 
        # AccountSelectionDialog(...)
        
        # We need to replace the last brace before AccountSelectionDialog with timeline_code + '}'
        # Let's count braces to precisely find the end of Column(modifier = Modifier.weight(1f))
        
        braces = 0
        in_block = False
        target_close_idx = -1
        
        for i in range(idx, len(content)):
            if content[i] == '{':
                braces += 1
                in_block = True
            elif content[i] == '}':
                braces -= 1
            if in_block and braces == 0:
                target_close_idx = i
                break
                
        if target_close_idx != -1:
            content = content[:target_close_idx] + timeline_code + content[target_close_idx:]

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated AppCalendarScreen safely!")
