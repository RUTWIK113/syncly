package com.ppicalendar.app.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.ppicalendar.app.presentation.components.NativeDatePickerField
import com.ppicalendar.app.presentation.components.NativeTimePickerField
import com.ppicalendar.app.ui.theme.PureBlack
import com.ppicalendar.app.ui.theme.SynclyPrimaryAmber
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    companyName: String,
    onDismiss: () -> Unit,
    onAdd: (PlacementEvent) -> Unit
) {
    var selectedEventType by remember { mutableStateOf(EventType.INTERVIEW) }
    var expanded by remember { mutableStateOf(false) }

    var dateString by remember { mutableStateOf("") }
    var timeString by remember { mutableStateOf("") }
    var venue by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Add Event to Calendar", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = companyName,
                    onValueChange = {},
                    label = { Text("Company Name") },
                    readOnly = true,
                    leadingIcon = {
                        Icon(Icons.Default.Business, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedEventType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Event Type *") },
                            leadingIcon = {
                                Icon(Icons.Default.Category, contentDescription = null)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expanded = !expanded }
                        )
                    }
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        EventType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName) },
                                onClick = {
                                    selectedEventType = type
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                NativeDatePickerField(
                    label = "Event Date *",
                    date = dateString,
                    onDateSelected = { dateString = it }
                )

                NativeTimePickerField(
                    label = "Start Time *",
                    time = timeString,
                    onTimeSelected = { timeString = it }
                )

                OutlinedTextField(
                    value = venue,
                    onValueChange = { venue = it },
                    label = { Text("Venue (Optional)") },
                    placeholder = { Text("e.g. CRC 101, Online, MS Teams") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (dateString.isNotBlank() && timeString.isNotBlank()) {
                        val event = PlacementEvent(
                            notificationKey = UUID.randomUUID().toString(),
                            company = companyName.trim().ifBlank { PlacementEvent.DEFAULT_COMPANY_FALLBACK },
                            title = "",
                            eventType = selectedEventType,
                            date = dateString.trim(),
                            startTime = timeString.trim(),
                            venue = venue.trim().ifBlank { null },
                            description = "Added from Placement Vault dossier"
                        )
                        onAdd(event)
                    }
                },
                enabled = dateString.isNotBlank() && timeString.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SynclyPrimaryAmber,
                    contentColor = PureBlack
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Add to Calendar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
