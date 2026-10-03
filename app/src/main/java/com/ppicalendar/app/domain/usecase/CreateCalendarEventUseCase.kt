package com.ppicalendar.app.domain.usecase

import com.ppicalendar.app.domain.model.EventStatus
import com.ppicalendar.app.domain.model.PlacementEvent
import com.ppicalendar.app.domain.repository.CalendarRepository
import com.ppicalendar.app.domain.repository.PlacementEventRepository
import com.ppicalendar.app.domain.repository.SettingsRepository

class CreateCalendarEventUseCase(
    private val calendarRepository: CalendarRepository,
    private val placementEventRepository: PlacementEventRepository,
    private val settingsRepository: SettingsRepository
) {
    suspend operator fun invoke(event: PlacementEvent): Result<Long> {
        // 1. Validation: date and start time cannot be missing
        if (event.date.isBlank() || event.startTime.isBlank() || event.company.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Cannot create calendar event: Missing essential information (Date, Time, or Company).")
            )
        }

        val settings = settingsRepository.getSettings()
        val targetCalendarId = settings.selectedCalendarId

        // 2. Check duplicate in calendar
        val alreadyExists = calendarRepository.eventExists(event, targetCalendarId)
        if (alreadyExists) {
            placementEventRepository.updateEventStatus(event.id, EventStatus.CREATED_IN_CALENDAR)
            return Result.success(event.calendarEventId ?: 0L)
        }

        // 3. Create event in calendar
        val createResult = calendarRepository.createEvent(
            event = event,
            calendarId = targetCalendarId,
            accountEmail = settings.connectedEmail.ifBlank { null },
            reminderMinutes = settings.defaultReminderMinutes
        )

        return createResult.onSuccess { calendarEventId ->
            // 4. Update status in local database
            placementEventRepository.updateEventStatus(
                id = event.id,
                status = EventStatus.CREATED_IN_CALENDAR,
                calendarEventId = calendarEventId
            )
        }.onFailure { error ->
            placementEventRepository.updateEventStatus(
                id = event.id,
                status = EventStatus.FAILED
            )
        }
    }
}
