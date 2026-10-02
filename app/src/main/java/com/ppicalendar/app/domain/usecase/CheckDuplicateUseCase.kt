package com.ppicalendar.app.domain.usecase

import com.ppicalendar.app.domain.model.PlacementEvent
import com.ppicalendar.app.domain.repository.CalendarRepository
import com.ppicalendar.app.domain.repository.PlacementEventRepository
import com.ppicalendar.app.domain.repository.ProcessedNotificationRepository

class CheckDuplicateUseCase(
    private val processedNotificationRepository: ProcessedNotificationRepository,
    private val placementEventRepository: PlacementEventRepository,
    private val calendarRepository: CalendarRepository
) {
    suspend fun isNotificationAlreadyProcessed(notificationKey: String): Boolean {
        return processedNotificationRepository.isNotificationProcessed(notificationKey)
    }

    suspend fun isEventAlreadyInCalendar(event: PlacementEvent, calendarId: Long?): Boolean {
        return calendarRepository.eventExists(event, calendarId)
    }
}
