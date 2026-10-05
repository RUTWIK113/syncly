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

        // 3. Strict whitelisted placement sources filtering
        val lowerText = text.lowercase()
        val lowerSender = sender.lowercase()

        val trustedSources = listOf(
            "computer center",
            "computer centre",
            "computer centere",
            "ug mechanical",
            "mechanical 2026",
            "student announcements",
            "students announcements",
            "me ug placements",
            "rutwik"
        )

        val isTrustedSource = trustedSources.any {
            lowerSender.contains(it) || lowerText.contains(it)
        }

        if (!isTrustedSource) {
            return NotificationProcessOutcome.NotPlacementEvent
        }

        // 3b. Keyword Requirement: The message MUST contain placement-related keywords to be parsed
        val placementKeywords = listOf(
            "ppt", "pre-placement", "oa", "online assessment", "interview", "ppi",
            "test", "shortlist", "placement", "internship", "session", "talk",
            "coding", "hackerrank", "hackerearth", "mettl", "superset", "assessment",
            "round", "gd", "group discussion", "deadline", "slot", "schedule", "venue"
        )

        val isManualEntry = sender == "Manual Entry"

        val matchesKeyword = isManualEntry || settings.keywords.any { kw ->
            kw.isNotBlank() && (lowerText.contains(kw.lowercase()) || lowerSender.contains(kw.lowercase()))
        } || placementKeywords.any { kw ->
            lowerText.contains(kw)
        }

        if (!matchesKeyword) {
            return NotificationProcessOutcome.NoKeywordMatch
        }

        // 3c. Filter out pure casual chats, student queries, or greetings
        val isCasualOrQuestion = !isManualEntry && (lowerText.startsWith("can anyone") ||
                lowerText.startsWith("does anyone") ||
                lowerText.startsWith("is anyone") ||
                lowerText.startsWith("where is") ||
                lowerText.startsWith("when will") ||
                lowerText.startsWith("why is") ||
                lowerText.startsWith("how to") ||
                lowerText.startsWith("thanks") ||
                lowerText.startsWith("thank you") ||
                lowerText.startsWith("congrats") ||
                lowerText.startsWith("all the best") ||
                lowerText == "ok" || lowerText == "k" || lowerText == "yes" || lowerText == "no")

        if (isCasualOrQuestion) {
            return NotificationProcessOutcome.NotPlacementEvent
        }

        // 4. AI / Heuristic Extraction
        var extraction = try {
            extractPlacementEventUseCase(
                text = text,
                referenceDate = referenceDate,
                apiKey = if (settings.useAiExtraction) settings.geminiApiKey else "",
                forcePlacement = isManualEntry
            )
        } catch (e: Exception) {
            return NotificationProcessOutcome.Error("Extraction failed: ${e.message}", e)
        }

        // A genuine event MUST be classified as an event
        if (!extraction.isEvent && !isManualEntry) {
            return NotificationProcessOutcome.NotPlacementEvent
        }

        // If company is empty but message came from a trusted source with genuine timing/date
        if (extraction.company.isBlank() && (isManualEntry || (isTrustedSource && (extraction.startTime.isNotBlank() || extraction.date.isNotBlank())))) {
            val fallbackCompany = when {
                lowerSender.contains("computer center") || lowerSender.contains("computer centre") || lowerSender.contains("computer centere") || lowerText.contains("computer centre") || lowerText.contains("computer center") -> "Computer Centre IIT Madras"
                lowerSender.contains("ug mechanical") || lowerSender.contains("me ug placements") || lowerText.contains("ug mechanical") -> "UG Mechanical Placements"
                lowerSender.contains("students announcements") || lowerText.contains("students announcements") -> "IITM Students Announcements"
                lowerSender.contains("rutwik") || lowerText.contains("rutwik") -> "Rutwik (Placement Notice)"
                isManualEntry -> "Manual Entry"
                sender.isNotBlank() -> sender.trim()
                else -> "Placement Cell IITM"
            }
            extraction = extraction.copy(
                isEvent = true,
                company = fallbackCompany
            )
        }

        // Final guard: Must be an event, must have a company, and must have at least start time or date
        if (!isManualEntry && (!extraction.isEvent || extraction.company.isBlank() || (extraction.startTime.isBlank() && extraction.date.isBlank()))) {
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
