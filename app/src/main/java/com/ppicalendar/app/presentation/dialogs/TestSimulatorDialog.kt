package com.ppicalendar.app.presentation.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
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

data class SampleNotification(
    val label: String,
    val sender: String,
    val message: String
)

val sampleNotifications = listOf(
    SampleNotification(
        label = "Google PPT (5 Incentive Points)",
        sender = "IITM Placement Notices 2026",
        message = """*Google Pre-Placement Talk*

Company Name: Google
PPT time: 10/10/2026 6:00 PM
PPT venue: CLT Auditorium

Incentive points: 5 points for attending!
Shortlist for OA requires active PPT participation.

Whatsapp group: https://chat.whatsapp.com/GoogleCampus2026"""
    ),
    SampleNotification(
        label = "Honda R&D PPT (With WA Group)",
        sender = "IITM Placement Notices 2026",
        message = """*PPT Announcement*

Company Name : Honda R&D

PPT time : 07/10/2026 7pm

PPT venue : RMN 101

Profiles :
1. AI Engineer
2. AI & advanced mobility
3. Semiconductor

Profiles are open for all departments and all degrees.

(Since placement portal is down, you can't see or apply for the profiles)

Whatsapp group: https://chat.whatsapp.com/LIuelkgiauo5cwtyNbNVPg

Join the group for further updates.

Kindly forward this message in the relevant groups."""
    ),
    SampleNotification(
        label = "Jane Street PPT",
        sender = "IITM Placement Notices 2026",
        message = "Jane Street PPT & Coding OA: Jane Street is hosting a Pre-Placement Talk tomorrow at 6:00 PM - 7:30 PM in CLT. Followed by OA on HackerRank. Meeting link: https://meet.google.com/abc-defg-hij"
    ),
    SampleNotification(
        label = "Microsoft Interview",
        sender = "CDC Updates Official",
        message = "Microsoft Placement Update: Interview shortlisted candidates session is scheduled for Monday at 10:00 AM IST on MS Teams (https://teams.microsoft.com/l/meetup-join/xyz). Venue: Online."
    ),
    SampleNotification(
        label = "Goldman Sachs PPI",
        sender = "IITM Placement Cell",
        message = "Goldman Sachs: PPI selection round starts on 28th October at 2:00 PM. Please be present at ICSR Auditorium 15 mins prior."
    ),
    SampleNotification(
        label = "Uber OA Deadline",
        sender = "Department Placement Rep",
        message = "Uber Online Assessment: OA test window is live on HackerEarth. Deadline to register is Friday at 11:59 PM. Link: https://assessment.hackerearth.com/uber-2026"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSimulatorDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSimulate: (sender: String, message: String) -> Unit
) {
    var sender by remember { mutableStateOf("IITM Placement Group") }
    var messageText by remember { mutableStateOf(sampleNotifications[0].message) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "WhatsApp Message Simulator",
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
                Text(
                    text = "Pick a sample IIT Madras placement announcement or paste your own to test real-time AI & Heuristic extraction:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Quick sample buttons
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    sampleNotifications.forEach { sample ->
                        FilterChip(
                            selected = messageText == sample.message,
                            onClick = {
                                sender = sample.sender
                                messageText = sample.message
                            },
                            label = { Text(sample.label) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("Sender / WhatsApp Group Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("WhatsApp Message Text") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 6
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSimulate(sender, messageText)
                    onDismiss()
                },
                enabled = !isLoading && messageText.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Simulate & Parse")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
