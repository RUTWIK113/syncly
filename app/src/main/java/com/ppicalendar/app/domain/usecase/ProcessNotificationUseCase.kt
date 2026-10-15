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
    object DuplicateEvent : NotificationProcessOutcome()
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

        val isManualEntry = forceParse || sender == "Manual Entry"

        val placementKeywords = listOf(
            "ppt", "pre-placement", "oa", "online assessment", "interview", "ppi", "test",
            "shortlist", "placement", "internship", "session", "talk", "coding", "hackerrank",
            "hackerearth", "mettl", "superset", "assessment", "round", "gd", "group discussion",
            "deadline", "slot", "schedule", "venue", "register", "registration", "resume",
            "briefing", "recruitment", "presentation"
        )

        // Source-independent keyword checking: eligible based on content or sender keywords
        val matchesKeyword = isManualEntry ||
                settings.keywords.any { kw -> kw.isNotBlank() && (lowerText.contains(kw.lowercase()) || lowerSender.contains(kw.lowercase())) } ||
                placementKeywords.any { kw -> lowerText.contains(kw) }

        if (!matchesKeyword) return NotificationProcessOutcome.NoKeywordMatch

        val isCasualOrQuestion = !isManualEntry && (
                lowerText.startsWith("can anyone") || lowerText.startsWith("does anyone") ||
                lowerText.startsWith("is anyone") || lowerText.startsWith("where is") ||
                lowerText.startsWith("when will") || lowerText.startsWith("why is") ||
                lowerText.startsWith("how to") || lowerText.startsWith("thanks") ||
                lowerText.startsWith("thank you") || lowerText.startsWith("congrats") ||
                lowerText.startsWith("all the best") || lowerText == "ok" || lowerText == "k" ||
                lowerText == "yes" || lowerText == "no"
        )

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
        var duplicateCount = 0
        var firstOutcome: NotificationProcessOutcome? = null
        
        val validEvents = extractions.filter { it.isEvent || isManualEntry }
        if (validEvents.isEmpty()) return NotificationProcessOutcome.NotPlacementEvent

        val existingEventsList = placementEventRepository.getAllEventsImmediate()

        for ((index, originalExtraction) in validEvents.withIndex()) {
            var extraction = originalExtraction
            
            if (extraction.company.isBlank()) {
                val fallbackCompany = when {
                    isManualEntry -> "Manual Entry"
                    sender.isNotBlank() && !sender.equals("whatsapp", ignoreCase = true) -> sender.trim()
                    else -> "Placement Cell"
                }
                extraction = extraction.copy(isEvent = true, company = fallbackCompany)
            }

            if (!isManualEntry && (!extraction.isEvent || extraction.company.isBlank() || (extraction.startTime.isBlank() && extraction.date.isBlank()))) {
                continue
            }

            val resolvedDate = resolveDateUseCase.resolve(extraction.date, referenceDate)
            val eventType = EventType.fromString(extraction.eventType)
            
            val finalCompany = PlacementEvent.normalizeCompany(extraction.company)
            val isEssentialInfoPresent = resolvedDate.isNotBlank() && extraction.startTime.isNotBlank()
            
            val existingEvent = existingEventsList.find { it.company == finalCompany && it.eventType == eventType }
            
            var eventIdToUse = 0L
            if (existingEvent != null) {
                if (existingEvent.date == resolvedDate && existingEvent.startTime == extraction.startTime) {
                    duplicateCount++
                    continue
                }
                eventIdToUse = existingEvent.id
            }

            val initialStatus = if (isEssentialInfoPresent && settings.automaticCalendarCreation && !settings.confirmationRequired) {
                EventStatus.CREATED_IN_CALENDAR
            } else {
                EventStatus.PENDING_CONFIRMATION
            }

            val deadlineDate = extraction.deadlineDate.ifBlank { null }?.trim()
            val deadlineTime = PlacementEvent.normalizeDeadlineTime(deadlineDate, extraction.deadlineTime)

            val suffix = if (index > 0) "-${index+1}" else ""
            var event = PlacementEvent(
                id = eventIdToUse,
                notificationKey = "$notificationKey$suffix",
                company = finalCompany,
                title = "",
                eventType = eventType,
                date = resolvedDate,
                startTime = extraction.startTime.trim(),
                endTime = extraction.endTime.ifBlank { null }?.trim(),
                deadlineDate = deadlineDate,
                deadlineTime = deadlineTime,
                venue = extraction.venue.ifBlank { null }?.trim(),
                meetingUrl = extraction.meetingUrl.ifBlank { null }?.trim(),
                description = extraction.description.ifBlank { null }?.trim(),
                rawNotificationSnippet = text,
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
                if (finalCompany.isNotBlank() && finalCompany != PlacementEvent.DEFAULT_COMPANY_FALLBACK) {
                    val companyProfile = companyRepository.getOrCreateCompanyByName(event.company)
                    if (!event.meetingUrl.isNullOrBlank()) {
                        val existingWeb = companyProfile.website ?: ""
                        if (!existingWeb.contains(event.meetingUrl)) {
                            val newWeb = if (existingWeb.isBlank()) event.meetingUrl else "$existingWeb\n${event.meetingUrl}"
                            companyRepository.insertOrUpdateCompany(companyProfile.copy(website = newWeb))
                        }
                    }
                }
            } catch (e: Exception) {
                // Non-fatal
            }

            val currentOutcome = when {
                !isEssentialInfoPresent -> {
                    missingInfoCount++
                    NotificationProcessOutcome.MissingEssentialInfo(event)
                }
                initialStatus == EventStatus.CREATED_IN_CALENDAR -> {
                    createdCount++
                    val calendarResult = createCalendarEventUseCase(event)
                    val calendarId = calendarResult.getOrNull() ?: -1L
                    if (calendarId > 0) {
                        placementEventRepository.updateEventStatus(event.id, EventStatus.CREATED_IN_CALENDAR, calendarId)
                        notificationHelper.showEventCreatedNotification(event.copy(id = savedId))
                    }
                    NotificationProcessOutcome.CreatedAutomatically(event, calendarId)
                }
                else -> {
                    confirmationCount++
                    notificationHelper.showEventConfirmationNotification(event.copy(id = savedId))
                    NotificationProcessOutcome.ConfirmationRequired(event)
                }
            }

            if (firstOutcome == null) {
                firstOutcome = currentOutcome
            }
        }

        val totalProcessed = createdCount + confirmationCount + missingInfoCount
        return when {
            totalProcessed == 0 && duplicateCount > 0 -> NotificationProcessOutcome.DuplicateEvent
            totalProcessed == 0 -> NotificationProcessOutcome.NotPlacementEvent
            totalProcessed == 1 -> firstOutcome ?: NotificationProcessOutcome.NotPlacementEvent
            else -> NotificationProcessOutcome.MultipleProcessed(createdCount, confirmationCount, missingInfoCount)
        }
    }
}
