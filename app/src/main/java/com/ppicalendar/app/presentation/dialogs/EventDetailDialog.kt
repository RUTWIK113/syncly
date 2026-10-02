package com.ppicalendar.app.presentation.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ppicalendar.app.domain.model.EventType
import com.ppicalendar.app.domain.model.PlacementEvent

@Composable
fun EventDetailDialog(
    event: PlacementEvent,
    onDismiss: () -> Unit,
    onSave: (PlacementEvent, Boolean) -> Unit
) {
    var company by remember { mutableStateOf(event.company) }
    var eventType by remember { mutableStateOf(event.eventType) }
    var date by remember { mutableStateOf(event.date) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime ?: "") }
    var venue by remember { mutableStateOf(event.venue ?: "") }
    var meetingUrl by remember { mutableStateOf(event.meetingUrl ?: "") }
    var description by remember { mutableStateOf(event.description ?: "") }

    var eventTypeMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Placement Event",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Company Name
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Event Type Selector
                Column {
                    OutlinedTextField(
                        value = eventType.displayName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Event Type") },
                        trailingIcon = {
                            IconButton(onClick = { eventTypeMenuExpanded = !eventTypeMenuExpanded }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(
                        expanded = eventTypeMenuExpanded,
                        onDismissRequest = { eventTypeMenuExpanded = false }
                    ) {
                        EventType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    eventType = type
                                    eventTypeMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Date (YYYY-MM-DD)
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date (YYYY-MM-DD) *") },
                    placeholder = { Text("2026-10-25") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Start & End Times
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startTime,
                        onValueChange = { startTime = it },
                        label = { Text("Start (HH:mm) *") },
                        placeholder = { Text("18:00") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endTime,
                        onValueChange = { endTime = it },
                        label = { Text("End (HH:mm)") },
                        placeholder = { Text("19:30") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // Venue
                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue / Location") },
                    placeholder = { Text("CLT / ICSR Auditorium / Online") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Meeting URL
                OutlinedTextField(
                    value = meetingUrl,
                    onValueChange = { meetingUrl = it },
                    label = { Text("Meeting / Assessment URL") },
                    placeholder = { Text("https://meet.google.com/...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description / Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = event.copy(
                        company = company.trim(),
                        eventType = eventType,
                        date = date.trim(),
                        startTime = startTime.trim(),
                        endTime = endTime.trim().ifBlank { null },
                        venue = venue.trim().ifBlank { null },
                        meetingUrl = meetingUrl.trim().ifBlank { null },
                        description = description.trim().ifBlank { null }
                    )
                    onSave(updated, true)
                },
                enabled = company.isNotBlank() && date.isNotBlank() && startTime.isNotBlank()
            ) {
                Text("Save & Add to Calendar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
