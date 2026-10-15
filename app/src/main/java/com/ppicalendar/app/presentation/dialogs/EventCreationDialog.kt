package com.ppicalendar.app.presentation.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ppicalendar.app.domain.model.EventType
import com.ppicalendar.app.domain.model.PlacementEvent
import com.ppicalendar.app.presentation.components.NativeDatePickerField
import com.ppicalendar.app.presentation.components.NativeTimePickerField
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class CreationMode {
    PASTE_MESSAGE,
    QUICK_FORM
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventCreationDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onExtractAndParse: (message: String) -> Unit,
    onSaveQuickEvent: (PlacementEvent) -> Unit
) {
    var selectedMode by remember { mutableStateOf(CreationMode.PASTE_MESSAGE) }

    // Method A: Paste Message
    var pastedMessage by remember { mutableStateOf("") }

    // Method B: Quick Form
    var companyName by remember { mutableStateOf("") }
    var selectedEventType by remember { mutableStateOf(EventType.PRE_PLACEMENT_TALK) }
    var eventTypeDropdownExpanded by remember { mutableStateOf(false) }

    val todayStr = remember { LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE) }
    var dateString by remember { mutableStateOf(todayStr) }
    var timeString by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }
    var hasDeadline by remember { mutableStateOf(false) }
    var deadlineDateString by remember { mutableStateOf("") }
    var deadlineTimeString by remember { mutableStateOf("23:00") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Title Header
                Text(
                    text = "Add Event",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))

                // Mode Tabs
                PrimaryTabRow(
                    selectedTabIndex = if (selectedMode == CreationMode.PASTE_MESSAGE) 0 else 1,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedMode == CreationMode.PASTE_MESSAGE,
                        onClick = { selectedMode = CreationMode.PASTE_MESSAGE },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Paste Message", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedMode == CreationMode.QUICK_FORM,
                        onClick = { selectedMode = CreationMode.QUICK_FORM },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Quick Form", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedMode == CreationMode.PASTE_MESSAGE) {
                        Text(
                            text = "Paste any placement announcement, WhatsApp message, or notice. Syncly will automatically extract the events and deadlines.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = pastedMessage,
                            onValueChange = { pastedMessage = it },
                            label = { Text("Message Text") },
                            placeholder = { Text("Paste message here (e.g. Company: Squarepoint Capital\nTest Date: 9th Oct 6:45 PM...)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 180.dp),
                            minLines = 7
                        )
                    } else {
                        // Method B: Quick Form
                        OutlinedTextField(
                            value = companyName,
                            onValueChange = { companyName = it },
                            label = { Text("Company Name (Optional)") },
                            placeholder = { Text("e.g. Google, McKinsey (or '-------')") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Event Type Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = selectedEventType.displayName,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Event Type") },
                                trailingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Event Type",
                                        modifier = Modifier.clickable { eventTypeDropdownExpanded = true }
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { eventTypeDropdownExpanded = true }
                            )
                            DropdownMenu(
                                expanded = eventTypeDropdownExpanded,
                                onDismissRequest = { eventTypeDropdownExpanded = false }
                            ) {
                                EventType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.displayName) },
                                        onClick = {
                                            selectedEventType = type
                                            eventTypeDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Native Date Picker
                        NativeDatePickerField(
                            date = dateString,
                            onDateSelected = { dateString = it },
                            label = "Event Date *"
                        )

                        // Native Time Picker
                        NativeTimePickerField(
                            time = timeString,
                            onTimeSelected = { timeString = it },
                            label = "Start Time (HH:mm) *"
                        )

                        OutlinedTextField(
                            value = venue,
                            onValueChange = { venue = it },
                            label = { Text("Venue (Optional)") },
                            placeholder = { Text("e.g. CLT, CRC, Online") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Optional Deadline Section
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { hasDeadline = !hasDeadline }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = hasDeadline,
                                onCheckedChange = { hasDeadline = it }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Has Registration/Application Deadline",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        if (hasDeadline) {
                            NativeDatePickerField(
                                date = deadlineDateString,
                                onDateSelected = { deadlineDateString = it },
                                label = "Deadline Date *"
                            )
                            NativeTimePickerField(
                                time = deadlineTimeString,
                                onTimeSelected = { deadlineTimeString = it },
                                label = "Deadline Time (Defaults to 23:00)"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))

                    if (selectedMode == CreationMode.PASTE_MESSAGE) {
                        Button(
                            onClick = {
                                onExtractAndParse(pastedMessage.trim())
                                onDismiss()
                            },
                            enabled = !isLoading && pastedMessage.isNotBlank()
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Extract Event")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                val deadlineDate = if (hasDeadline && deadlineDateString.isNotBlank()) deadlineDateString else null
                                val deadlineTime = PlacementEvent.normalizeDeadlineTime(deadlineDate, deadlineTimeString)

                                val event = PlacementEvent(
                                    notificationKey = UUID.randomUUID().toString(),
                                    company = companyName.trim().ifBlank { PlacementEvent.DEFAULT_COMPANY_FALLBACK },
                                    title = "",
                                    eventType = selectedEventType,
                                    date = dateString,
                                    startTime = timeString,
                                    deadlineDate = deadlineDate,
                                    deadlineTime = deadlineTime,
                                    venue = venue.trim().ifBlank { null },
                                    description = "Manually created event"
                                )
                                onSaveQuickEvent(event)
                                onDismiss()
                            },
                            enabled = dateString.isNotBlank() && timeString.isNotBlank()
                        ) {
                            Text("Save Event")
                        }
                    }
                }
            }
        }
    }
}
