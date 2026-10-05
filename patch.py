import sys

path = r'C:\Users\HP\Downloads\Syncly\app\src\main\java\com\ppicalendar\app\presentation\screens\HomeScreen.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

old_block = '''                } else {
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
                }'''

new_block = '''                } else {
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
                                modifier = Modifier.padding(horizontal = 16.dp, top = 8.dp, bottom = 2.dp)
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

if old_block in content:
    content = content.replace(old_block, new_block)
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('Replaced')
else:
    print('Failed to find block')
