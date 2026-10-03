package com.ppicalendar.app.domain.usecase

import com.ppicalendar.app.domain.model.EventStatus
import com.ppicalendar.app.domain.model.EventType
import com.ppicalendar.app.domain.model.PlacementEvent
import com.ppicalendar.app.domain.repository.CompanyRepository
import com.ppicalendar.app.domain.repository.PlacementEventRepository
import com.ppicalendar.app.domain.repository.ProcessedNotificationRepository
import com.ppicalendar.app.domain.repository.SettingsRepository
import java.time.LocalDate

sealed class NotificationProcessOutcome {
    object Disabled : NotificationProcessOutcome()
    object AlreadyProcessed : NotificationProcessOutcome()
    object NoKeywordMatch : NotificationProcessOutcome()
    object NotPlacementEvent : NotificationProcessOutcome()
    data class CreatedAutomatically(val event: PlacementEvent, val calendarEventId: Long) : NotificationProcessOutcome()
    data class ConfirmationRequired(val event: PlacementEvent) : NotificationProcessOutcome()
    data class MissingEssentialInfo(val event: PlacementEvent) : NotificationProcessOutcome()
    data class Error(val message: String, val throwable: Throwable? = null) : NotificationProcessOutcome()
}

class ProcessNotificationUseCase(
    private val settingsRepository: SettingsRepository,
    private val processedNotificationRepository: ProcessedNotificationRepository,
    private val placementEventRepository: PlacementEventRepository,
    private val extractPlacementEventUseCase: ExtractPlacementEventUseCase,
    private val resolveDateUseCase: ResolveDateUseCase,
    private val createCalendarEventUseCase: CreateCalendarEventUseCase,
    private val companyRepository: CompanyRepository
) {
    suspend operator fun invoke(
        notificationKey: String,
        sender: String,
        text: String,
        referenceDate: LocalDate = LocalDate.now()
    ): NotificationProcessOutcome {
        val settings = settingsRepository.getSettings()

        // 1. Check if processing is globally enabled
        if (!settings.notificationProcessingEnabled) {
            return NotificationProcessOutcome.Disabled
        }

        // 2. Check if this notification was already processed
        if (processedNotificationRepository.isNotificationProcessed(notificationKey)) {
            return NotificationProcessOutcome.AlreadyProcessed
        }

        // Mark as processed immediately to prevent duplicate concurrent runs
        processedNotificationRepository.markNotificationProcessed(notificationKey, sender)

        // 3. Keyword filtering
        val lowerText = text.lowercase()
        val hasKeywordMatch = settings.keywords.isEmpty() || settings.keywords.any { keyword ->
            lowerText.contains(keyword.lowercase().trim())
        }

        if (!hasKeywordMatch) {
            return NotificationProcessOutcome.NoKeywordMatch
        }

        // 4. AI / Heuristic Extraction
        val extraction = try {
            extractPlacementEventUseCase(
                text = text,
                referenceDate = referenceDate,
                apiKey = if (settings.useAiExtraction) settings.geminiApiKey else ""
            )
        } catch (e: Exception) {
            return NotificationProcessOutcome.Error("Extraction failed: ${e.message}", e)
        }

        if (!extraction.isEvent || extraction.company.isBlank()) {
            return NotificationProcessOutcome.NotPlacementEvent
        }

        // 5. Date resolution
        val resolvedDate = resolveDateUseCase.resolve(extraction.date, referenceDate)

        // 6. Build PlacementEvent model
        val eventType = EventType.fromString(extraction.eventType)
        val initialStatus = if (extraction.isValidForAutoCreation() && settings.automaticCalendarCreation && !settings.confirmationRequired) {
            EventStatus.CREATED_IN_CALENDAR
        } else {
            EventStatus.PENDING_CONFIRMATION
        }

        val event = PlacementEvent(
            notificationKey = notificationKey,
            company = extraction.company.trim(),
            eventType = eventType,
            date = resolvedDate,
            startTime = extraction.startTime.trim(),
            endTime = extraction.endTime.ifBlank { null }?.trim(),
            venue = extraction.venue.ifBlank { null }?.trim(),
            meetingUrl = extraction.meetingUrl.ifBlank { null }?.trim(),
            description = extraction.description.ifBlank { null }?.trim(),
            rawNotificationSnippet = text.take(300),
            incentivePoints = extraction.incentivePoints,
            confidence = extraction.confidence,
            status = initialStatus
        )

        // 7. Save to local repository
        val savedId = placementEventRepository.insertEvent(event)
        val savedEvent = event.copy(id = savedId)

        // Automatically ensure company exists in Placement Vault and attach portal/meeting links
        try {
            val companyProfile = companyRepository.getOrCreateCompanyByName(savedEvent.company)
            if (!savedEvent.meetingUrl.isNullOrBlank()) {
                val existingWeb = companyProfile.website ?: ""
                if (!existingWeb.contains(savedEvent.meetingUrl)) {
                    val updatedWeb = if (existingWeb.isBlank()) savedEvent.meetingUrl else "$existingWeb\n${savedEvent.meetingUrl}"
                    companyRepository.insertOrUpdateCompany(companyProfile.copy(website = updatedWeb))
                }
            }
        } catch (e: Exception) {
            // Non-fatal
        }

        // 8. Decide on auto-creation vs confirmation
        val isEssentialInfoPresent = savedEvent.date.isNotBlank() && savedEvent.startTime.isNotBlank() && savedEvent.company.isNotBlank()

        if (!isEssentialInfoPresent) {
            return NotificationProcessOutcome.MissingEssentialInfo(savedEvent)
        }

        if (settings.automaticCalendarCreation && !settings.confirmationRequired) {
            val result = createCalendarEventUseCase(savedEvent)
            return if (result.isSuccess) {
                NotificationProcessOutcome.CreatedAutomatically(savedEvent, result.getOrThrow())
            } else {
                NotificationProcessOutcome.ConfirmationRequired(savedEvent)
            }
        }

        return NotificationProcessOutcome.ConfirmationRequired(savedEvent)
    }
}
