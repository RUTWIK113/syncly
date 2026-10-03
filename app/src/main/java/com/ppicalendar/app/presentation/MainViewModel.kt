package com.ppicalendar.app.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ppicalendar.app.PPICalendarApplication
import com.ppicalendar.app.data.notification.WhatsAppNotificationListenerService
import com.ppicalendar.app.domain.model.AppSettings
import com.ppicalendar.app.domain.model.AttachmentType
import com.ppicalendar.app.domain.model.CalendarInfo
import com.ppicalendar.app.domain.model.CompanyAttachment
import com.ppicalendar.app.domain.model.CompanyProfile
import com.ppicalendar.app.domain.model.EventStatus
import com.ppicalendar.app.domain.model.PlacementEvent
import com.ppicalendar.app.domain.usecase.NotificationProcessOutcome
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

data class UiNotification(
    val message: String,
    val isError: Boolean = false
)

data class RatingPromptData(
    val promptType: com.ppicalendar.app.presentation.dialogs.RatingPromptType,
    val appOpenCount: Int
)

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val container = (application as PPICalendarApplication).container

    val allEvents: StateFlow<List<PlacementEvent>> = container.placementEventRepository
        .getAllEventsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingEvents: StateFlow<List<PlacementEvent>> = container.placementEventRepository
        .getPendingEventsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val companies: StateFlow<List<CompanyProfile>> = container.companyRepository
        .getAllCompaniesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPoints: StateFlow<List<com.ppicalendar.app.domain.model.IncentivePoint>> = container.incentivePointRepository
        .getAllPointsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = container.settingsRepository
        .getSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val testNoticeCount: StateFlow<Int> = container.dataStoreManager.testNoticeCountFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    private val _availableCalendars = MutableStateFlow<List<CalendarInfo>>(emptyList())
    val availableCalendars: StateFlow<List<CalendarInfo>> = _availableCalendars.asStateFlow()

    private val _uiEvents = MutableSharedFlow<UiNotification>()
    val uiEvents: SharedFlow<UiNotification> = _uiEvents.asSharedFlow()

    private val _isNotificationListenerGranted = MutableStateFlow(false)
    val isNotificationListenerGranted: StateFlow<Boolean> = _isNotificationListenerGranted.asStateFlow()

    private val _selectedEventForEdit = MutableStateFlow<PlacementEvent?>(null)
    val selectedEventForEdit: StateFlow<PlacementEvent?> = _selectedEventForEdit.asStateFlow()

    private val _selectedCompanyForView = MutableStateFlow<CompanyProfile?>(null)
    val selectedCompanyForView: StateFlow<CompanyProfile?> = _selectedCompanyForView.asStateFlow()

    private val _isSimulatorOpen = MutableStateFlow(false)
    val isSimulatorOpen: StateFlow<Boolean> = _isSimulatorOpen.asStateFlow()

    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()

    private val _ratingPromptState = MutableStateFlow<RatingPromptData?>(null)
    val ratingPromptState: StateFlow<RatingPromptData?> = _ratingPromptState.asStateFlow()

    init {
        checkPermissions()
        refreshCalendars()
        checkAppOpenRating()
        viewModelScope.launch {
            try {
                container.telemetryManager.syncUserTelemetry()
            } catch (e: Exception) {
                // Ignore
            }
        }
        // Auto-ensure all companies from events are present in Placement Vault
        viewModelScope.launch {
            allEvents.collect { events ->
                events.forEach { event ->
                    if (event.company.isNotBlank()) {
                        try {
                            val comp = container.companyRepository.getOrCreateCompanyByName(event.company)
                            if (!event.meetingUrl.isNullOrBlank()) {
                                val curWeb = comp.website ?: ""
                                if (!curWeb.contains(event.meetingUrl)) {
                                    val newWeb = if (curWeb.isBlank()) event.meetingUrl else "$curWeb\n${event.meetingUrl}"
                                    container.companyRepository.insertOrUpdateCompany(comp.copy(website = newWeb))
                                }
                            }
                        } catch (e: Exception) {
                            // Non-fatal
                        }
                    }
                }
            }
        }
    }

    fun checkAppOpenRating() {
        viewModelScope.launch {
            val (openCount, lastRated) = container.dataStoreManager.recordAppOpen()
            if (openCount == 5 && lastRated < 5) {
                _ratingPromptState.value = RatingPromptData(
                    promptType = com.ppicalendar.app.presentation.dialogs.RatingPromptType.FIVE_STAR,
                    appOpenCount = openCount
                )
            } else if (openCount >= 50 && openCount % 50 == 0 && lastRated < openCount) {
                _ratingPromptState.value = RatingPromptData(
                    promptType = com.ppicalendar.app.presentation.dialogs.RatingPromptType.TEN_STAR,
                    appOpenCount = openCount
                )
            }
        }
    }

    fun dismissRatingDialog() {
        _ratingPromptState.value = null
    }

    fun submitRating(rating: Int, maxStars: Int, reviewText: String) {
        val currentPrompt = _ratingPromptState.value
        val openCount = currentPrompt?.appOpenCount ?: 5
        viewModelScope.launch {
            container.dataStoreManager.setLastRatedAppOpenCount(openCount)
            _ratingPromptState.value = null
        }
    }

    fun checkPermissions() {
        _isNotificationListenerGranted.value = WhatsAppNotificationListenerService.isPermissionGranted(getApplication())
    }

    fun refreshCalendars() {
        viewModelScope.launch {
            val calendars = container.calendarRepository.getAvailableCalendars()
            _availableCalendars.value = calendars

            val savedSettings = container.settingsRepository.getSettings()
            if (savedSettings.connectedEmail.isNotBlank()) {
                val matching = calendars.find { it.accountName.equals(savedSettings.connectedEmail, ignoreCase = true) }
                if (matching != null && savedSettings.selectedCalendarId != matching.id) {
                    container.settingsRepository.updateSelectedCalendarId(matching.id)
                }
            } else if (savedSettings.selectedCalendarId != null) {
                val matching = calendars.find { it.id == savedSettings.selectedCalendarId }
                if (matching != null && savedSettings.connectedEmail.isBlank()) {
                    container.settingsRepository.updateConnectedAccount(
                        email = matching.accountName,
                        accountName = matching.displayName,
                        isVerified = true,
                        calendarId = matching.id
                    )
                }
            }
        }
    }

    fun confirmEvent(event: PlacementEvent) {
        viewModelScope.launch {
            val result = container.createCalendarEventUseCase(event)
            result.onSuccess {
                // Automatically save company to company vault
                container.companyRepository.getOrCreateCompanyByName(event.company)

                _uiEvents.emit(UiNotification("✅ Added '${event.formattedTitle}' to Calendar!"))
            }.onFailure { error ->
                _uiEvents.emit(UiNotification("❌ ${error.message ?: "Failed to create calendar event"}", isError = true))
            }
        }
    }

    fun dismissEvent(eventId: Long) {
        viewModelScope.launch {
            container.placementEventRepository.updateEventStatus(eventId, EventStatus.DISMISSED)
            _uiEvents.emit(UiNotification("Event dismissed"))
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            val event = allEvents.value.find { it.id == eventId }
            if (event?.calendarEventId != null) {
                try {
                    container.calendarRepository.deleteEvent(event.calendarEventId)
                } catch (e: Exception) {
                    // Ignore error if calendar event was already removed
                }
            }
            container.placementEventRepository.deleteEvent(eventId)
            _uiEvents.emit(UiNotification("Event deleted from calendar. Company preserved in Vault."))
        }
    }

    fun saveEditedEvent(event: PlacementEvent, createImmediately: Boolean = false) {
        viewModelScope.launch {
            container.placementEventRepository.updateEvent(event)
            _selectedEventForEdit.value = null

            // Auto-store company into vault
            container.companyRepository.getOrCreateCompanyByName(event.company)

            if (createImmediately) {
                confirmEvent(event)
            } else {
                _uiEvents.emit(UiNotification("Event details updated"))
            }
        }
    }

    // Company Vault Operations
    fun selectCompany(company: CompanyProfile) {
        viewModelScope.launch {
            val fullProfile = container.companyRepository.getCompanyById(company.id) ?: company
            _selectedCompanyForView.value = fullProfile
        }
    }

    fun clearSelectedCompany() {
        _selectedCompanyForView.value = null
    }

    fun saveCompanyProfile(company: CompanyProfile, keepOpen: Boolean = false) {
        viewModelScope.launch {
            val id = container.companyRepository.insertOrUpdateCompany(company)
            if (keepOpen) {
                val updated = container.companyRepository.getCompanyById(id)
                _selectedCompanyForView.value = updated
            } else {
                _selectedCompanyForView.value = null
            }
            _uiEvents.emit(UiNotification("Company profile saved"))
        }
    }

    fun deleteCompany(companyId: Long) {
        viewModelScope.launch {
            container.companyRepository.deleteCompany(companyId)
            _selectedCompanyForView.value = null
            _uiEvents.emit(UiNotification("Company deleted"))
        }
    }

    fun addCompanyAttachment(
        companyId: Long,
        title: String,
        uriString: String? = null,
        textContent: String? = null,
        type: AttachmentType = AttachmentType.NOTE,
        fileSizeFormatted: String? = null
    ) {
        viewModelScope.launch {
            container.companyRepository.addAttachment(
                CompanyAttachment(
                    companyId = companyId,
                    title = title.ifBlank { "Untitled ${type.name}" },
                    uriString = uriString,
                    textContent = textContent,
                    type = type,
                    fileSizeFormatted = fileSizeFormatted
                )
            )
            val updated = container.companyRepository.getCompanyById(companyId)
            _selectedCompanyForView.value = updated
            _uiEvents.emit(UiNotification("Added ${type.name.lowercase()} attachment"))
        }
    }

    fun deleteCompanyAttachment(companyId: Long, attachmentId: Long) {
        viewModelScope.launch {
            container.companyRepository.deleteAttachment(attachmentId)
            val updated = container.companyRepository.getCompanyById(companyId)
            _selectedCompanyForView.value = updated
            _uiEvents.emit(UiNotification("Attachment removed"))
        }
    }

    fun openEditDialog(event: PlacementEvent) {
        _selectedEventForEdit.value = event
    }

    fun closeEditDialog() {
        _selectedEventForEdit.value = null
    }

    fun openSimulator() {
        _isSimulatorOpen.value = true
    }

    fun closeSimulator() {
        _isSimulatorOpen.value = false
    }

    fun simulateWhatsAppMessage(sender: String, message: String) {
        viewModelScope.launch {
            _isSimulating.value = true
            container.dataStoreManager.incrementTestNoticeCount()
            val testKey = "SIM_${System.currentTimeMillis()}"
            val outcome = container.processNotificationUseCase(
                notificationKey = testKey,
                sender = sender,
                text = message,
                referenceDate = LocalDate.now()
            )

            _isSimulating.value = false
            when (outcome) {
                is NotificationProcessOutcome.CreatedAutomatically -> {
                    container.companyRepository.getOrCreateCompanyByName(outcome.event.company)
                    _uiEvents.emit(UiNotification("🎯 Event detected & created: ${outcome.event.formattedTitle}"))
                }
                is NotificationProcessOutcome.ConfirmationRequired -> {
                    container.companyRepository.getOrCreateCompanyByName(outcome.event.company)
                    _uiEvents.emit(UiNotification("📋 Event extracted! Confirmation required for ${outcome.event.company}."))
                }
                is NotificationProcessOutcome.MissingEssentialInfo -> {
                    container.companyRepository.getOrCreateCompanyByName(outcome.event.company)
                    _uiEvents.emit(UiNotification("⚠️ Event detected for ${outcome.event.company}, but date/time is missing. Please edit."))
                }
                is NotificationProcessOutcome.NoKeywordMatch -> {
                    _uiEvents.emit(UiNotification("ℹ️ Message ignored: Did not match placement keywords.", isError = true))
                }
                is NotificationProcessOutcome.NotPlacementEvent -> {
                    _uiEvents.emit(UiNotification("ℹ️ Message classified as NOT a placement event.", isError = true))
                }
                is NotificationProcessOutcome.Disabled -> {
                    _uiEvents.emit(UiNotification("⚠️ Notification processing is disabled in settings.", isError = true))
                }
                is NotificationProcessOutcome.AlreadyProcessed -> {
                    _uiEvents.emit(UiNotification("⚠️ Duplicate notification skipped."))
                }
                is NotificationProcessOutcome.Error -> {
                    _uiEvents.emit(UiNotification("❌ Error: ${outcome.message}", isError = true))
                }
            }
        }
    }

    // Save All Settings in Batch
    fun saveAllSettings(updatedSettings: AppSettings, onSaved: () -> Unit) = viewModelScope.launch {
        container.dataStoreManager.saveAppSettings(updatedSettings)
        _uiEvents.emit(UiNotification("✓ Preferences saved successfully!"))
        onSaved()
    }

    // Calendar Account Confirmation
    fun confirmCalendarConnection(calendar: CalendarInfo) = viewModelScope.launch {
        container.settingsRepository.updateConnectedAccount(
            email = calendar.accountName,
            accountName = calendar.displayName,
            isVerified = true,
            calendarId = calendar.id
        )
        _uiEvents.emit(UiNotification("✓ Connected & synced with ${calendar.accountName}"))
    }

    // Verify Email with OTP and set as Primary Calendar account
    fun verifyAndConnectEmail(email: String, calendarId: Long? = null) = viewModelScope.launch {
        val cleanEmail = email.trim()
        val matchingCalendar = availableCalendars.value.find { it.accountName.equals(cleanEmail, ignoreCase = true) }
        val targetCalId = matchingCalendar?.id ?: calendarId ?: availableCalendars.value.firstOrNull { it.isPrimary }?.id ?: availableCalendars.value.firstOrNull()?.id

        container.settingsRepository.updateConnectedAccount(
            email = cleanEmail,
            accountName = matchingCalendar?.displayName ?: "Primary Campus Account",
            isVerified = true,
            calendarId = targetCalId
        )
        _uiEvents.emit(UiNotification("✅ Verified & Linked: $cleanEmail"))
    }

    fun disconnectAccount() = viewModelScope.launch {
        container.settingsRepository.updateConnectedAccount(
            email = "",
            accountName = "",
            isVerified = false,
            calendarId = null
        )
        _uiEvents.emit(UiNotification("Account disconnected"))
    }

    // Auto-Save Setting actions
    fun setNotificationProcessing(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.updateNotificationProcessingEnabled(enabled)
    }

    fun setAutoCalendarCreation(enabled: Boolean) = viewModelScope.launch {
        container.settingsRepository.updateAutomaticCalendarCreation(enabled)
    }

    fun setConfirmationRequired(required: Boolean) = viewModelScope.launch {
        container.settingsRepository.updateConfirmationRequired(required)
    }

    fun setAutoJoinWhatsAppGroups(autoJoin: Boolean) = viewModelScope.launch {
        container.settingsRepository.updateAutoJoinWhatsAppGroups(autoJoin)
    }

    fun addKeyword(keyword: String) = viewModelScope.launch {
        container.settingsRepository.addKeyword(keyword)
    }

    fun removeKeyword(keyword: String) = viewModelScope.launch {
        container.settingsRepository.removeKeyword(keyword)
    }

    fun setSelectedCalendarId(calendarId: Long?) = viewModelScope.launch {
        container.settingsRepository.updateSelectedCalendarId(calendarId)
    }

    fun setGeminiApiKey(apiKey: String) = viewModelScope.launch {
        container.settingsRepository.updateGeminiApiKey(apiKey)
    }

    fun setUseAiExtraction(useAi: Boolean) = viewModelScope.launch {
        container.settingsRepository.updateUseAiExtraction(useAi)
    }

    fun setDarkTheme(isDark: Boolean) = viewModelScope.launch {
        container.dataStoreManager.setDarkTheme(isDark)
    }

    fun addIncentivePoint(title: String, points: Int, date: String = "", note: String? = null) = viewModelScope.launch {
        val cleanDate = if (date.isNotBlank()) date else java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        container.incentivePointRepository.addPoint(
            com.ppicalendar.app.domain.model.IncentivePoint(
                title = title.trim(),
                points = points,
                date = cleanDate,
                note = note?.trim()?.ifBlank { null }
            )
        )
        _uiEvents.emit(UiNotification("Added +$points Points for $title!"))
    }

    fun deleteIncentivePoint(id: Long) = viewModelScope.launch {
        container.incentivePointRepository.deletePoint(id)
        _uiEvents.emit(UiNotification("Points entry deleted"))
    }
}
