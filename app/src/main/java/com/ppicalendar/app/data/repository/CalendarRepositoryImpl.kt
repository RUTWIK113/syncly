package com.ppicalendar.app.data.repository

import com.ppicalendar.app.data.calendar.CalendarContractManager
import com.ppicalendar.app.domain.model.CalendarInfo
import com.ppicalendar.app.domain.model.PlacementEvent
import com.ppicalendar.app.domain.repository.CalendarRepository

class CalendarRepositoryImpl(
    private val calendarContractManager: CalendarContractManager
) : CalendarRepository {

    override suspend fun getAvailableCalendars(): List<CalendarInfo> {
        return calendarContractManager.getAvailableCalendars()
    }

    override suspend fun getDefaultCalendarId(): Long? {
        return calendarContractManager.getDefaultCalendarId()
    }

    override suspend fun eventExists(event: PlacementEvent, calendarId: Long?): Boolean {
        return calendarContractManager.eventExists(event, calendarId)
    }

    override suspend fun createEvent(
        event: PlacementEvent,
        calendarId: Long?,
        accountEmail: String?,
        reminderMinutes: Int
    ): Result<Long> {
        return calendarContractManager.createEvent(event, calendarId, accountEmail, reminderMinutes)
    }

    override suspend fun deleteEvent(calendarEventId: Long): Boolean {
        return calendarContractManager.deleteEvent(calendarEventId)
    }
}
