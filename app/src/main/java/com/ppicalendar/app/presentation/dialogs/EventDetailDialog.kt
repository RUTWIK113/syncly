package com.ppicalendar.app.presentation.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ppicalendar.app.domain.model.EventType
import com.ppicalendar.app.domain.model.PlacementEvent
import android.widget.TextView
import android.text.util.Linkify
import android.text.method.LinkMovementMethod
import android.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
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
    var isEditingNote by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top App Bar
                TopAppBar(
                    title = { Text("Event Details", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Event Card as Heading
                    com.ppicalendar.app.presentation.components.EventCard(
                        event = event.copy(company = company, eventType = eventType, date = date, startTime = startTime, endTime = endTime, venue = venue, meetingUrl = meetingUrl, description = description),
                        onConfirm = {},
                        onEdit = {},
                        onDismiss = {},
                        onDelete = {}
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

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
                            modifier = Modifier.fillMaxWidth().clickable { eventTypeMenuExpanded = !eventTypeMenuExpanded }
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

                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD) *") },
                        placeholder = { Text("2026-10-25") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

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

                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("Venue / Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = meetingUrl,
                        onValueChange = { meetingUrl = it },
                        label = { Text("Meeting / Assessment URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Description / Notes Field with Clickable Links Support
                    if (!isEditingNote) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                                .clickable { isEditingNote = true }
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Description / Notes (Tap to edit)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            AndroidView(
                                factory = { context ->
                                    TextView(context).apply {
                                        autoLinkMask = Linkify.WEB_URLS or Linkify.EMAIL_ADDRESSES or Linkify.PHONE_NUMBERS
                                        linksClickable = true
                                        movementMethod = LinkMovementMethod.getInstance()
                                        textSize = 15f
                                        setTextColor(Color.DKGRAY)
                                        setLineSpacing(0f, 1.2f)
                                    }
                                },
                                update = {
                                    it.text = description.ifBlank { "No notes attached. Tap to add." }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    } else {
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description / Notes") },
                            modifier = Modifier.fillMaxWidth().height(250.dp),
                            trailingIcon = {
                                IconButton(onClick = { isEditingNote = false }) {
                                    Icon(Icons.Default.Check, contentDescription = "Done Editing")
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Bottom Action Bar
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onDismiss, modifier = Modifier.padding(end = 8.dp)) {
                            Text("Cancel")
                        }
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
                            Text("Save & Update Calendar")
                        }
                    }
                }
            }
        }
    }
}
