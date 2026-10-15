package com.ppicalendar.app.presentation.dialogs

import android.graphics.Color
import android.text.method.LinkMovementMethod
import android.text.util.Linkify
import android.widget.TextView
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
import com.ppicalendar.app.presentation.components.NativeDatePickerField
import com.ppicalendar.app.presentation.components.NativeTimePickerField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailDialog(
    event: PlacementEvent,
    onDismiss: () -> Unit,
    onSave: (PlacementEvent, Boolean) -> Unit
) {
    var company by remember { mutableStateOf(event.company) }
    var title by remember { mutableStateOf(if (event.title == PlacementEvent.DEFAULT_TITLE_FALLBACK) "" else event.title) }
    var eventType by remember { mutableStateOf(event.eventType) }
    var date by remember { mutableStateOf(event.date) }
    var startTime by remember { mutableStateOf(event.startTime) }
    var endTime by remember { mutableStateOf(event.endTime ?: "") }
    var hasDeadline by remember { mutableStateOf(!event.deadlineDate.isNullOrBlank()) }
    var deadlineDate by remember { mutableStateOf(event.deadlineDate ?: "") }
    var deadlineTime by remember { mutableStateOf(event.deadlineTime ?: PlacementEvent.DEFAULT_DEADLINE_TIME) }
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                TopAppBar(
                    title = { Text("Update Event", fontWeight = FontWeight.Bold) },
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

                    // Company Name
                    OutlinedTextField(
                        value = company,
                        onValueChange = { company = it },
                        label = { Text("Company Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Event Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Event Title") },
                        placeholder = { Text("------- (Defaults if empty)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Event Type Selector (clickable anywhere on the box or arrow)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = eventType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Event Type") },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select Event Type",
                                    modifier = Modifier.clickable { eventTypeMenuExpanded = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { eventTypeMenuExpanded = true }
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

                    // Native Date Picker
                    NativeDatePickerField(
                        date = date,
                        onDateSelected = { date = it },
                        label = "Date *"
                    )

                    // Native Time Pickers for Start and End Time
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NativeTimePickerField(
                            time = startTime,
                            onTimeSelected = { startTime = it },
                            label = "Start Time *",
                            modifier = Modifier.weight(1f)
                        )
                        NativeTimePickerField(
                            time = endTime,
                            onTimeSelected = { endTime = it },
                            label = "End Time",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Venue
                    OutlinedTextField(
                        value = venue,
                        onValueChange = { venue = it },
                        label = { Text("Venue / Location") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Meeting / URL
                    OutlinedTextField(
                        value = meetingUrl,
                        onValueChange = { meetingUrl = it },
                        label = { Text("Meeting / Assessment URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    // Deadline Section
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
                            text = "Include Registration / Application Deadline",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (hasDeadline) {
                        NativeDatePickerField(
                            date = deadlineDate,
                            onDateSelected = { deadlineDate = it },
                            label = "Deadline Date *"
                        )
                        NativeTimePickerField(
                            time = deadlineTime,
                            onTimeSelected = { deadlineTime = it },
                            label = "Deadline Time (Defaults to 23:00)"
                        )
                    }

                    // Notes Section
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
                                factory = { ctx ->
                                    TextView(ctx).apply {
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp),
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
                                val finalTitle = if (title.isBlank()) PlacementEvent.DEFAULT_TITLE_FALLBACK else title.trim()
                                val finalDeadlineDate = if (hasDeadline && deadlineDate.isNotBlank()) deadlineDate else null
                                val finalDeadlineTime = PlacementEvent.normalizeDeadlineTime(finalDeadlineDate, deadlineTime)

                                val updated = event.copy(
                                    company = company.trim(),
                                    title = finalTitle,
                                    eventType = eventType,
                                    date = date.trim(),
                                    startTime = startTime.trim(),
                                    endTime = endTime.trim().ifBlank { null },
                                    deadlineDate = finalDeadlineDate,
                                    deadlineTime = finalDeadlineTime,
                                    venue = venue.trim().ifBlank { null },
                                    meetingUrl = meetingUrl.trim().ifBlank { null },
                                    description = description.trim().ifBlank { null }
                                )
                                onSave(updated, true)
                            },
                            enabled = company.isNotBlank() && date.isNotBlank() && startTime.isNotBlank()
                        ) {
                            Text("Save Update")
                        }
                    }
                }
            }
        }
    }
}
