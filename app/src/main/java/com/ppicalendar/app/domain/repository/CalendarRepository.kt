package com.ppicalendar.app.domain.repository

import com.ppicalendar.app.domain.model.CalendarInfo
import com.ppicalendar.app.domain.model.PlacementEvent

interface CalendarRepository {
    suspend fun getAvailableCalendars(): List<CalendarInfo>
    suspend fun getDefaultCalendarId(): Long?
    suspend fun eventExists(event: PlacementEvent, calendarId: Long?): Boolean
    suspend fun createEvent(
        event: PlacementEvent,
        calendarId: Long?,
        accountEmail: String? = null,
        reminderMinutes: Int = 60
    ): Result<Long>
    suspend fun deleteEvent(calendarEventId: Long): Boolean
}
