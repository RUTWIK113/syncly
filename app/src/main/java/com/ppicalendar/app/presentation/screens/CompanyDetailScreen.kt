package com.ppicalendar.app.presentation.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ppicalendar.app.domain.model.AttachmentType
import com.ppicalendar.app.domain.model.CompanyAttachment
import com.ppicalendar.app.domain.model.CompanyProfile
import com.ppicalendar.app.presentation.MainViewModel
import com.ppicalendar.app.presentation.components.SynclyFooter
import com.ppicalendar.app.presentation.components.SynclyHeader
import com.ppicalendar.app.ui.theme.AccentAmberBadge
import com.ppicalendar.app.ui.theme.AccentRedBadge

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CompanyDetailScreen(
    company: CompanyProfile,
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allEvents by viewModel.allEvents.collectAsState()
    val companyEvents = allEvents.filter { it.company.equals(company.name, ignoreCase = true) }

    var name by remember(company) { mutableStateOf(company.name) }
    var payScale by remember(company) { mutableStateOf(company.payScale ?: "") }
    var notes by remember(company) { mutableStateOf(company.notes ?: "") }
    var linkInput by remember { mutableStateOf("") }
    val linksList = remember(company) {
        val initial = company.website?.split("\n")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
        mutableStateListOf<String>().apply { addAll(initial) }
    }

    fun getFinalWebsiteString(): String? {
        val list = linksList.toMutableList()
        if (linkInput.isNotBlank()) {
            val raw = linkInput.trim()
            val formatted = if (!raw.startsWith("http://", ignoreCase = true) && !raw.startsWith("https://", ignoreCase = true)) {
                "https://$raw"
            } else {
                raw
            }
            if (!list.contains(formatted)) {
                list.add(formatted)
            }
        }
        return if (list.isNotEmpty()) list.joinToString("\n") else null
    }

    var isAddingTextNote by remember { mutableStateOf(false) }
    var noteTitleInput by remember { mutableStateOf("") }
    var noteContentInput by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // PDF Picker Launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (savedUri, fileSize) = copyUriToSynclyStorage(context, it, "Job_Description.pdf")
            val fileName = getFileName(context, it) ?: "Job_Description.pdf"
            viewModel.addCompanyAttachment(company.id, fileName, savedUri, null, AttachmentType.PDF, fileSize)
        }
    }

    // Image Picker Launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val (savedUri, fileSize) = copyUriToSynclyStorage(context, it, "JD_Screenshot.png")
            val fileName = getFileName(context, it) ?: "JD_Screenshot.png"
            viewModel.addCompanyAttachment(company.id, fileName, savedUri, null, AttachmentType.IMAGE, fileSize)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Full Screen Top Header Bar
        SynclyHeader(
            title = if (company.id == 0L) "New Company" else (company.name.ifBlank { "Company Dossier" }),
            subtitle = if (company.id != 0L) "Vault & Placement Dossier" else "Add company to vault",
            onBackClick = onBack,
            actions = {
                IconButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.saveCompanyProfile(
                                company.copy(
                                    name = name.trim(),
                                    payScale = payScale.trim().ifBlank { null },
                                    roleNames = null,
                                    website = getFinalWebsiteString(),
                                    notes = notes.trim().ifBlank { null }
                                )
                            )
                            onBack()
                        }
                    },
                    enabled = name.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Save Company",
                        tint = if (MaterialTheme.colorScheme.background == com.ppicalendar.app.ui.theme.SynclyDarkBg) Color(0xFFFFE082) else Color(0xFF231B00)
                    )
                }
            }
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card 1: Essential Company Information
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Company Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Company Name *") },
                            placeholder = { Text("e.g. Honda R&D, Google, Jane Street") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = payScale,
                            onValueChange = { payScale = it },
                            label = { Text("Pay Scale / CTC Package") },
                            placeholder = { Text("e.g. 24 LPA CTC / 1.5 Lakhs Stipend") },
                            leadingIcon = { Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        // Links input with trailing checkmark (✓) button to add as clickable pills
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = linkInput,
                                onValueChange = { linkInput = it },
                                label = { Text("Links") },
                                placeholder = { Text("Paste link (e.g. https://portal.college.edu) and tap ✓") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Link,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                trailingIcon = {
                                    if (linkInput.isNotBlank()) {
                                        IconButton(
                                            onClick = {
                                                val raw = linkInput.trim()
                                                if (raw.isNotBlank()) {
                                                    val formatted = if (!raw.startsWith("http://", ignoreCase = true) && !raw.startsWith("https://", ignoreCase = true)) {
                                                        "https://$raw"
                                                    } else {
                                                        raw
                                                    }
                                                    if (!linksList.contains(formatted)) {
                                                        linksList.add(formatted)
                                                    }
                                                    linkInput = ""
                                                }
                                            }
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Add Link",
                                                        tint = MaterialTheme.colorScheme.onPrimary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            // Google Docs-style clickable pills
                            if (linksList.isNotEmpty()) {
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    linksList.forEach { url ->
                                        val displayUrl = url
                                            .removePrefix("https://")
                                            .removePrefix("http://")
                                            .removePrefix("www.")
                                        val cleanDisplay = if (displayUrl.length > 28) displayUrl.take(25) + "..." else displayUrl

                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                            modifier = Modifier.clickable {
                                                try {
                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                    }
                                                    context.startActivity(intent)
                                                } catch (e: Exception) { }
                                            }
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Link,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = cleanDisplay,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                IconButton(
                                                    onClick = { linksList.remove(url) },
                                                    modifier = Modifier.size(20.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Close,
                                                        contentDescription = "Remove",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Preparation Notes & Questions") },
                            placeholder = { Text("Add key points, previous round questions, or interview experiences...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            // Card 2: Attached Dossiers, PDFs, Images & Notes
            if (company.id != 0L) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AttachFile,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Job Description & Files",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "${company.attachments.size} Files",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Spacious Attachment Action Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { imagePickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Image", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { isAddingTextNote = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.NoteAdd, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Note", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // List of files
                            if (company.attachments.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier.padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No files attached yet. Attach JD PDFs, compensation breakdown images, or interview notes above.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    company.attachments.forEach { attachment ->
                                        SpaciousAttachmentCard(
                                            attachment = attachment,
                                            onOpen = {
                                                if (!attachment.uriString.isNullOrBlank()) {
                                                    try {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachment.uriString)).apply {
                                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) { }
                                                }
                                            },
                                            onDelete = {
                                                viewModel.deleteCompanyAttachment(company.id, attachment.id)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Card 3: Events History for this company
            if (companyEvents.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scheduled Placement Events (${companyEvents.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            companyEvents.forEach { event ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            val formattedEventDate = com.ppicalendar.app.data.extractor.DateTimeParser.formatIndianDate(event.date)
                                            val formattedTime = com.ppicalendar.app.data.extractor.DateTimeParser.formatIndianTimeRange(event.startTime, event.endTime)
                                            Text(
                                                text = "${event.eventType.displayName} • $formattedEventDate",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            if (event.startTime.isNotBlank()) {
                                                Text(
                                                    text = "Time: $formattedTime",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (!event.venue.isNullOrBlank()) {
                                                Text(
                                                    text = "Venue: ${event.venue}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Delete Company Button (Bottom Most)
            if (company.id != 0L) {
                item {
                    OutlinedButton(
                        onClick = { showDeleteConfirm = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = AccentRedBadge
                        ),
                        border = BorderStroke(1.dp, AccentRedBadge.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = AccentRedBadge, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Company from Vault",
                            color = AccentRedBadge,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Trademark footer
            item {
                SynclyFooter()
            }
        }
    }

    // Sub-dialog to add text note
    if (isAddingTextNote) {
        AlertDialog(
            onDismissRequest = { isAddingTextNote = false },
            title = { Text("Add Text Note", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = noteTitleInput,
                        onValueChange = { noteTitleInput = it },
                        label = { Text("Note Title") },
                        placeholder = { Text("e.g. Round 1 Questions, CTC Breakdown") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = noteContentInput,
                        onValueChange = { noteContentInput = it },
                        label = { Text("Content") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (noteTitleInput.isNotBlank() || noteContentInput.isNotBlank()) {
                            viewModel.addCompanyAttachment(
                                company.id,
                                noteTitleInput.ifBlank { "Note" },
                                null,
                                noteContentInput,
                                AttachmentType.NOTE,
                                "Text Note"
                            )
                            noteTitleInput = ""
                            noteContentInput = ""
                            isAddingTextNote = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Add Note", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingTextNote = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Company Dossier?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete ${company.name} and all associated files from your vault?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCompany(company.id)
                        showDeleteConfirm = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRedBadge, contentColor = androidx.compose.ui.graphics.Color.White)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SpaciousAttachmentCard(
    attachment: CompanyAttachment,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val icon = when (attachment.type) {
                            AttachmentType.PDF -> Icons.Default.PictureAsPdf
                            AttachmentType.IMAGE -> Icons.Default.Image
                            AttachmentType.NOTE -> Icons.Default.Description
                            else -> Icons.Default.AttachFile
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = attachment.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (!attachment.textContent.isNullOrBlank()) {
                        Text(
                            text = attachment.textContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    } else if (!attachment.fileSizeFormatted.isNullOrBlank()) {
                        Text(
                            text = attachment.fileSizeFormatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (!attachment.uriString.isNullOrBlank()) {
                    IconButton(onClick = onOpen) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = "Open file",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = AccentRedBadge,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val index = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    name = it.getString(index)
                }
            }
        }
    }
    if (name == null) {
        name = uri.path?.let {
            val cut = it.lastIndexOf('/')
            if (cut != -1) it.substring(cut + 1) else it
        }
    }
    return name
}

private fun copyUriToSynclyStorage(context: Context, uri: Uri, fallbackName: String): Pair<String, String> {
    return try {
        val originalName = getFileName(context, uri) ?: fallbackName
        val cleanName = originalName.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
        val synclyDir = java.io.File(context.filesDir, "Syncly").apply { if (!exists()) mkdirs() }
        val targetFile = java.io.File(synclyDir, "${System.currentTimeMillis()}_$cleanName")

        context.contentResolver.openInputStream(uri)?.use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        val bytes = targetFile.length()
        val formattedSize = if (bytes >= 1024 * 1024) {
            "%.1f MB".format(bytes / (1024.0 * 1024.0))
        } else {
            "${bytes / 1024} KB"
        }

        val localUri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            targetFile
        )
        Pair(localUri.toString(), formattedSize)
    } catch (e: Exception) {
        Pair(uri.toString(), "Local Storage")
    }
}
