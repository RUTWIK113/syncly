package com.ppicalendar.app.presentation.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Outbox
import androidx.compose.material.icons.filled.Password
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ppicalendar.app.domain.model.CalendarInfo
import com.ppicalendar.app.presentation.MainViewModel
import com.ppicalendar.app.ui.theme.AccentGreen
import com.ppicalendar.app.ui.theme.AccentRedBadge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val liveSettings by viewModel.settings.collectAsState()
    val availableCalendars by viewModel.availableCalendars.collectAsState()
    val scope = rememberCoroutineScope()

    var newKeywordInput by remember { mutableStateOf("") }
    var isAccountDialogVisible by remember { mutableStateOf(false) }
    var isCustomEmailDialogVisible by remember { mutableStateOf(false) }
    var customEmailInput by remember { mutableStateOf("") }
    var selectedCalendarCandidate by remember { mutableStateOf<CalendarInfo?>(null) }
    var showApiKey by remember { mutableStateOf(false) }
    var showAutoSaveToast by remember { mutableStateOf(false) }

    var feedbackCategory by remember { mutableStateOf("Bug Report") }
    var feedbackMessage by remember { mutableStateOf("") }

    fun triggerAutoSaveIndicator() {
        scope.launch {
            showAutoSaveToast = true
            delay(2000)
            showAutoSaveToast = false
        }
    }

    // Determine the persistent email and calendar info
    val connectedEmail = liveSettings.connectedEmail.ifBlank {
        availableCalendars.find { it.id == liveSettings.selectedCalendarId }?.accountName
            ?: availableCalendars.firstOrNull { it.isPrimary }?.accountName
            ?: ""
    }

    val currentCalendar = availableCalendars.find { it.id == liveSettings.selectedCalendarId }
        ?: availableCalendars.find { it.accountName.equals(connectedEmail, ignoreCase = true) }
        ?: availableCalendars.firstOrNull { it.isPrimary }
        ?: availableCalendars.firstOrNull()

    val isAccountConnected = connectedEmail.isNotBlank() || currentCalendar != null

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Sticky Top Yellow/Themed Header Bar
            com.ppicalendar.app.presentation.components.SynclyHeader(
                title = "Preferences",
                subtitle = "Configure WhatsApp detection rules, Google account, and display theme."
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Section 1: Google Calendar & Permanent Email Connection Card
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsSectionCard(title = "Google Calendar Connection", icon = Icons.Default.Email) {
                        Text(
                            text = "Permanent Google/Campus Mail Account where events & 60-min advance reminders are automatically synced:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
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
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (liveSettings.isEmailVerified) Icons.Default.VerifiedUser else Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = if (connectedEmail.isNotBlank()) connectedEmail else "No Account Connected",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = currentCalendar?.displayName
                                                ?: if (liveSettings.isEmailVerified) "Linked Campus Calendar" else "Tap below to connect mail account",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isAccountConnected) AccentGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, if (isAccountConnected) AccentGreen else MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = if (isAccountConnected) "● Connected" else "Not Connected",
                                        color = if (isAccountConnected) AccentGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.refreshCalendars()
                                    selectedCalendarCandidate = currentCalendar
                                    isAccountDialogVisible = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAccountConnected) "Switch Account" else "Select Device Account",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    customEmailInput = connectedEmail
                                    isCustomEmailDialogVisible = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Custom Email", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }

                        if (isAccountConnected) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.disconnectAccount()
                                        triggerAutoSaveIndicator()
                                    }
                                ) {
                                    Icon(Icons.Default.LinkOff, contentDescription = null, tint = AccentRedBadge, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Disconnect Account", color = AccentRedBadge, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Automation Rules
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsSectionCard(title = "Automation Rules", icon = Icons.Default.Tune) {
                        SettingsToggleRow(
                            title = "Process WhatsApp Notifications",
                            subtitle = "Inspect WhatsApp notification previews to extract placement notices",
                            checked = liveSettings.notificationProcessingEnabled,
                            onCheckedChange = {
                                viewModel.setNotificationProcessing(it)
                                triggerAutoSaveIndicator()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SettingsToggleRow(
                            title = "Auto-Join WhatsApp Groups",
                            subtitle = "Automatically launch invite links when detected in messages",
                            checked = liveSettings.autoJoinWhatsAppGroups,
                            onCheckedChange = {
                                viewModel.setAutoJoinWhatsAppGroups(it)
                                triggerAutoSaveIndicator()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SettingsToggleRow(
                            title = "Automatic Calendar Creation",
                            subtitle = "Sync directly to Google Calendar without manual review",
                            checked = liveSettings.automaticCalendarCreation,
                            onCheckedChange = {
                                viewModel.setAutoCalendarCreation(it)
                                triggerAutoSaveIndicator()
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        SettingsToggleRow(
                            title = "Require Confirmation",
                            subtitle = "Show notification action buttons before adding to calendar",
                            checked = liveSettings.confirmationRequired,
                            onCheckedChange = {
                                viewModel.setConfirmationRequired(it)
                                triggerAutoSaveIndicator()
                            }
                        )
                    }
                }
            }

            // Section 3: Placement Filter Keywords
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsSectionCard(title = "Detection Keywords", icon = Icons.Default.NotificationsActive) {
                        Text(
                            text = "Notices containing any of these keywords will be parsed by Syncly:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newKeywordInput,
                                onValueChange = { newKeywordInput = it },
                                label = { Text("Add Keyword") },
                                placeholder = { Text("e.g. Assessment, PPT, GD") },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    if (newKeywordInput.isNotBlank()) {
                                        val word = newKeywordInput.trim()
                                        viewModel.addKeyword(word)
                                        newKeywordInput = ""
                                        triggerAutoSaveIndicator()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.height(56.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Add")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            liveSettings.keywords.forEach { keyword ->
                                InputChip(
                                    selected = true,
                                    onClick = {},
                                    label = { Text(keyword, fontWeight = FontWeight.Medium) },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = {
                                                viewModel.removeKeyword(keyword)
                                                triggerAutoSaveIndicator()
                                            },
                                            modifier = Modifier.size(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Remove $keyword",
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = InputChipDefaults.inputChipBorder(
                                        enabled = true,
                                        selected = true,
                                        borderColor = MaterialTheme.colorScheme.outline
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: AI Extraction Engine (Gemini)
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsSectionCard(title = "AI Extraction Engine", icon = Icons.Default.AutoAwesome) {
                        SettingsToggleRow(
                            title = "Enable Gemini AI Extractor",
                            subtitle = "Use Gemini Flash with offline heuristic fallback",
                            checked = liveSettings.useAiExtraction,
                            onCheckedChange = {
                                viewModel.setUseAiExtraction(it)
                                triggerAutoSaveIndicator()
                            }
                        )

                        if (liveSettings.useAiExtraction) {
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = liveSettings.geminiApiKey,
                                onValueChange = {
                                    viewModel.setGeminiApiKey(it)
                                    triggerAutoSaveIndicator()
                                },
                                label = { Text("Google Gemini API Key (Optional)") },
                                placeholder = { Text("AIzaSy...") },
                                visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                trailingIcon = {
                                    IconButton(onClick = { showApiKey = !showApiKey }) {
                                        Icon(Icons.Default.Key, contentDescription = "Toggle key")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Leave empty to use built-in offline Regex & Campus heuristics (100% offline & private).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Section 5: Report Flaws, Feedback & Development Suggestions
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    SettingsSectionCard(title = "Feedback & Bug Report", icon = Icons.Default.RateReview) {
                        Text(
                            text = "Found a flaw, missed notification, or have a feature idea? Share direct feedback to improve Syncly:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Category Selection Chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Bug Report", "Feature Idea", "General Review").forEach { category ->
                                FilterChip(
                                    selected = feedbackCategory == category,
                                    onClick = { feedbackCategory = category },
                                    label = { Text(category, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = feedbackMessage,
                            onValueChange = { feedbackMessage = it },
                            label = { Text("Your Feedback / Bug Details") },
                            placeholder = { Text("Describe the flaw, what went wrong, or suggestions for improvements...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            maxLines = 8,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (feedbackMessage.isNotBlank()) {
                                    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf("aadharamos113@gmail.com"))
                                        putExtra(Intent.EXTRA_SUBJECT, "[Syncly $feedbackCategory] Campus Feedback")
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            """
                                            Category: $feedbackCategory
                                            Feedback / Bug Report:
                                            ${feedbackMessage.trim()}

                                            ---
                                            Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})
                                            App Version: 1.0.0
                                            """.trimIndent()
                                        )
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(emailIntent, "Send Feedback via Email"))
                                        feedbackMessage = ""
                                    } catch (e: Exception) {
                                        // Ignore if no email app installed
                                    }
                                }
                            },
                            enabled = feedbackMessage.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Send Feedback to Developer", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // In-App Updates Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Syncly Version 1.0.5",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Build 6 • Direct Campus Release",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.checkForAppUpdates()
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Check Updates", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Trademark Footer: Crafted by Rutwik
            item {
                com.ppicalendar.app.presentation.components.SynclyFooter()
            }
        }
    }

    // Floating Auto-Save Notification Toast
        AnimatedVisibility(
            visible = showAutoSaveToast,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.onSurface,
                shadowElevation = 6.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Preferences auto-saved ✓",
                        color = MaterialTheme.colorScheme.surface,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // Custom Email Connection Modal / Dialog (No OTP needed)
    if (isCustomEmailDialogVisible) {
        var emailError by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { isCustomEmailDialogVisible = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Link Campus / Google Email", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Enter your primary campus or personal Google mail. All synced events and reminders will be associated with this account.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = customEmailInput,
                        onValueChange = {
                            customEmailInput = it
                            emailError = null
                        },
                        label = { Text("College or Google Email") },
                        placeholder = { Text("e.g. yourname@smail.iitm.ac.in") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (!emailError.isNullOrBlank()) {
                        Text(
                            text = emailError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = customEmailInput.trim()
                        if (clean.isBlank() || !clean.contains("@")) {
                            emailError = "Please enter a valid email address"
                        } else {
                            viewModel.verifyAndConnectEmail(clean)
                            triggerAutoSaveIndicator()
                            isCustomEmailDialogVisible = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Connect Account", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isCustomEmailDialogVisible = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    // Google Calendar & Mail Connection Selection Dialog (Device accounts)
    if (isAccountDialogVisible) {
        AlertDialog(
            onDismissRequest = { isAccountDialogVisible = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Device Calendar Account", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose which email/calendar account Syncly should add your placement schedules and 60-min advance reminders to:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    if (availableCalendars.isEmpty()) {
                        Text(
                            text = "No calendar accounts detected. Please ensure Calendar permission is granted in Access tab, or use OTP Email verification above.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        availableCalendars.forEach { cal ->
                            val isSelected = (selectedCalendarCandidate?.id == cal.id) ||
                                    (selectedCalendarCandidate == null && cal.id == currentCalendar?.id)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedCalendarCandidate = cal },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )

                                    Spacer(modifier = Modifier.width(10.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cal.accountName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = cal.displayName + if (cal.isPrimary) " (Primary)" else "",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        selectedCalendarCandidate?.let {
                            viewModel.confirmCalendarConnection(it)
                            triggerAutoSaveIndicator()
                        }
                        isAccountDialogVisible = false
                    },
                    enabled = selectedCalendarCandidate != null || availableCalendars.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Confirm & Sync to Account", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { isAccountDialogVisible = false }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            content()
        }
    }
}

@Composable
fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )
    }
}
