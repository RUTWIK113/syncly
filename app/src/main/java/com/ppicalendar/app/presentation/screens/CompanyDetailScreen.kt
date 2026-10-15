package com.ppicalendar.app.presentation.screens

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
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
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showAddEventDialog by remember { mutableStateOf(false) }

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

                            // Responsive Attachment Action Buttons Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { pdfPickerLauncher.launch("application/pdf") },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PictureAsPdf,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "PDF",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { imagePickerLauncher.launch("image/*") },
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Image,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Photo",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
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
                                val rows = company.attachments.chunked(2)
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    rows.forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            rowItems.forEach { attachment ->
                                                Box(modifier = Modifier.weight(1f)) {
                                                    SquareAttachmentCard(
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
                                            if (rowItems.size == 1) {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }
                                        }
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
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
                                        text = "Scheduled Events (${companyEvents.size})",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                TextButton(onClick = { showAddEventDialog = true }) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Event")
                                }
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

            // Applied Status Buttons
            if (company.id != 0L) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.saveCompanyProfile(company.copy(applied = true))
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (company.applied == true) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                contentColor = if (company.applied == true) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Applied", fontWeight = FontWeight.Bold)
                        }
                        
                        OutlinedButton(
                            onClick = {
                                viewModel.saveCompanyProfile(company.copy(applied = false))
                            },
                            modifier = Modifier.weight(1f).height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (company.applied == false) AccentRedBadge.copy(alpha = 0.1f) else Color.Transparent,
                                contentColor = if (company.applied == false) AccentRedBadge else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text("Not Applied", fontWeight = FontWeight.Bold)
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

    if (showAddEventDialog) {
        AddEventDialog(
            companyName = company.name,
            onDismiss = { showAddEventDialog = false },
            onAdd = { event ->
                viewModel.addQuickEvent(event)
                showAddEventDialog = false
            }
        )
    }
}

@Composable
fun SquareAttachmentCard(
    attachment: CompanyAttachment,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val previewBitmap = remember(attachment.uriString) {
        attachment.uriString?.let { uriStr ->
            try {
                val uri = Uri.parse(uriStr)
                if (attachment.type == AttachmentType.IMAGE) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                        BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
                    }
                } else if (attachment.type == AttachmentType.PDF && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        val renderer = android.graphics.pdf.PdfRenderer(pfd)
                        if (renderer.pageCount > 0) {
                            val page = renderer.openPage(0)
                            val bmp = android.graphics.Bitmap.createBitmap(320, 320, android.graphics.Bitmap.Config.ARGB_8888)
                            bmp.eraseColor(android.graphics.Color.WHITE)
                            page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            renderer.close()
                            bmp.asImageBitmap()
                        } else {
                            renderer.close()
                            null
                        }
                    }
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = attachment.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC000000))
                            )
                        )
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val icon = when (attachment.type) {
                        AttachmentType.PDF -> Icons.Default.PictureAsPdf
                        AttachmentType.IMAGE -> Icons.Default.Image
                        else -> Icons.Default.Description
                    }
                    val iconTint = when (attachment.type) {
                        AttachmentType.PDF -> AccentRedBadge
                        else -> MaterialTheme.colorScheme.primary
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = attachment.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    if (!attachment.fileSizeFormatted.isNullOrBlank()) {
                        Text(
                            text = attachment.fileSizeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Top-left Type Tag
            Surface(
                shape = RoundedCornerShape(bottomEnd = 8.dp),
                color = when (attachment.type) {
                    AttachmentType.PDF -> AccentRedBadge.copy(alpha = 0.9f)
                    AttachmentType.IMAGE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                    else -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f)
                },
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = when (attachment.type) {
                        AttachmentType.PDF -> "PDF"
                        AttachmentType.IMAGE -> "IMG"
                        else -> "DOC"
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Top-right Delete Button
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(26.dp)
                    .clickable(onClick = onDelete)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete Attachment",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Bottom label when thumbnail is active
            if (previewBitmap != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = attachment.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )
                    if (!attachment.fileSizeFormatted.isNullOrBlank()) {
                        Text(
                            text = attachment.fileSizeFormatted,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFDDDDDD),
                            fontSize = 9.sp
                        )
                    }
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
