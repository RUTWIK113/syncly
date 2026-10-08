package com.ppicalendar.app.domain.usecase

import com.ppicalendar.app.data.notification.NotificationHelper
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
    data class MultipleProcessed(val created: Int, val requiredConfirmation: Int, val missingInfo: Int) : NotificationProcessOutcome()
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
    private val companyRepository: CompanyRepository,
    private val notificationHelper: NotificationHelper
) {
    suspend operator fun invoke(
        notificationKey: String,
        sender: String,
        text: String,
        referenceDate: LocalDate = LocalDate.now(),
        forceParse: Boolean = false
    ): NotificationProcessOutcome {
        val settings = settingsRepository.getSettings()

        if (!settings.notificationProcessingEnabled) return NotificationProcessOutcome.Disabled
        if (processedNotificationRepository.isNotificationProcessed(notificationKey)) return NotificationProcessOutcome.AlreadyProcessed

        processedNotificationRepository.markNotificationProcessed(notificationKey, sender)

        val lowerText = text.lowercase()
        val lowerSender = sender.lowercase()

        val trustedSources = listOf("computer center", "computer centre", "computer centere", "ug mechanical", "mechanical 2026", "student announcements", "students announcements", "me ug placements", "rutwik")
        val isTrustedSource = forceParse || sender == "Manual Entry" || trustedSources.any { lowerSender.contains(it) || lowerText.contains(it) }

        if (!isTrustedSource) return NotificationProcessOutcome.NotPlacementEvent

        val placementKeywords = listOf("ppt", "pre-placement", "oa", "online assessment", "interview", "ppi", "test", "shortlist", "placement", "internship", "session", "talk", "coding", "hackerrank", "hackerearth", "mettl", "superset", "assessment", "round", "gd", "group discussion", "deadline", "slot", "schedule", "venue")
        val isManualEntry = forceParse || sender == "Manual Entry"

        val matchesKeyword = isManualEntry || settings.keywords.any { kw -> kw.isNotBlank() && (lowerText.contains(kw.lowercase()) || lowerSender.contains(kw.lowercase())) } || placementKeywords.any { kw -> lowerText.contains(kw) }

        if (!matchesKeyword) return NotificationProcessOutcome.NoKeywordMatch

        val isCasualOrQuestion = !isManualEntry && (lowerText.startsWith("can anyone") || lowerText.startsWith("does anyone") || lowerText.startsWith("is anyone") || lowerText.startsWith("where is") || lowerText.startsWith("when will") || lowerText.startsWith("why is") || lowerText.startsWith("how to") || lowerText.startsWith("thanks") || lowerText.startsWith("thank you") || lowerText.startsWith("congrats") || lowerText.startsWith("all the best") || lowerText == "ok" || lowerText == "k" || lowerText == "yes" || lowerText == "no")

        if (isCasualOrQuestion) return NotificationProcessOutcome.NotPlacementEvent

        val extractions = try {
            extractPlacementEventUseCase(text, referenceDate, if (settings.useAiExtraction) settings.geminiApiKey else "", isManualEntry)
        } catch (e: Exception) {
            return NotificationProcessOutcome.Error("Extraction failed: ${e.message}", e)
        }

        if (extractions.isEmpty()) return NotificationProcessOutcome.NotPlacementEvent

        var createdCount = 0
        var confirmationCount = 0
        var missingInfoCount = 0
        var firstOutcome: NotificationProcessOutcome? = null
        
        val validEvents = extractions.filter { it.isEvent || isManualEntry }
        if (validEvents.isEmpty()) return NotificationProcessOutcome.NotPlacementEvent

        val existingEventsList = placementEventRepository.getAllEventsImmediate()

        for ((index, originalExtraction) in validEvents.withIndex()) {
            var extraction = originalExtraction
            
            if (extraction.company.isBlank() && (isManualEntry || (isTrustedSource && (extraction.startTime.isNotBlank() || extraction.date.isNotBlank())))) {
                val fallbackCompany = when {
                    lowerSender.contains("computer center") || lowerSender.contains("computer centre") || lowerText.contains("computer centre") -> "Computer Centre IIT Madras"
                    lowerSender.contains("ug mechanical") || lowerText.contains("ug mechanical") -> "UG Mechanical Placements"
                    lowerSender.contains("students announcements") || lowerText.contains("students announcements") -> "IITM Students Announcements"
                    lowerSender.contains("rutwik") || lowerText.contains("rutwik") -> "Rutwik (Placement Notice)"
                    isManualEntry -> "Manual Entry"
                    sender.isNotBlank() -> sender.trim()
                    else -> "Placement Cell IITM"
                }
                extraction = extraction.copy(isEvent = true, company = fallbackCompany)
            }

            if (!isManualEntry && (!extraction.isEvent || extraction.company.isBlank() || (extraction.startTime.isBlank() && extraction.date.isBlank()))) {
                continue
            }

            val resolvedDate = resolveDateUseCase.resolve(extraction.date, referenceDate)
            val eventType = EventType.fromString(extraction.eventType)
            
            val isEssentialInfoPresent = resolvedDate.isNotBlank() && extraction.startTime.isNotBlank() && extraction.company.isNotBlank()
            
            var existingEvent = existingEventsList.find { it.company == extraction.company && it.eventType == eventType }
            
            var eventIdToUse = 0L
            if (existingEvent != null) {
                if (existingEvent.date == resolvedDate && existingEvent.startTime == extraction.startTime) continue
                eventIdToUse = existingEvent.id
            }

            val initialStatus = if (isEssentialInfoPresent && settings.automaticCalendarCreation && !settings.confirmationRequired) {
                EventStatus.CREATED_IN_CALENDAR
            } else {
                EventStatus.PENDING_CONFIRMATION
            }

            val suffix = if (index > 0) "-${index+1}" else ""
            var event = PlacementEvent(
                id = eventIdToUse,
                notificationKey = "$notificationKey$suffix",
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

            val savedId = if (eventIdToUse > 0) {
                placementEventRepository.updateEvent(event)
                eventIdToUse
            } else {
                placementEventRepository.insertEvent(event)
            }
            
            event = event.copy(id = savedId)
            
            // Schedule Alarm Reminder whenever event is saved/updated
            if (isEssentialInfoPresent) {
                notificationHelper.scheduleEventReminder(event)
            }

            try {
                val companyProfile = companyRepository.getOrCreateCompanyByName(event.company)
                if (!event.meetingUrl.isNullOrBlank()) {
                    val existingWeb = companyProfile.website ?: ""
                    if (!existingWeb.contains(event.meetingUrl)) {
                        val updatedWeb = if (existingWeb.isBlank()) event.meetingUrl else "$existingWeb\n${event.meetingUrl}"
                        companyRepository.insertOrUpdateCompany(companyProfile.copy(website = updatedWeb))
                    }
                }
            } catch (e: Exception) {}

            var outcome: NotificationProcessOutcome
            if (!isEssentialInfoPresent) {
                outcome = NotificationProcessOutcome.MissingEssentialInfo(event)
                missingInfoCount++
            } else if (settings.automaticCalendarCreation && !settings.confirmationRequired) {
                val result = createCalendarEventUseCase(event)
                outcome = if (result.isSuccess) {
                    createdCount++
                    NotificationProcessOutcome.CreatedAutomatically(event, result.getOrThrow())
                } else {
                    confirmationCount++
                    NotificationProcessOutcome.ConfirmationRequired(event)
                }
            } else {
                confirmationCount++
                outcome = NotificationProcessOutcome.ConfirmationRequired(event)
            }
            
            if (firstOutcome == null) firstOutcome = outcome
        }
        
        val totalProcessed = createdCount + confirmationCount + missingInfoCount
        if (totalProcessed == 0) return NotificationProcessOutcome.NotPlacementEvent
        if (totalProcessed == 1) return firstOutcome!!
        return NotificationProcessOutcome.MultipleProcessed(createdCount, confirmationCount, missingInfoCount)
    }
}
