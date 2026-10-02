package com.ppicalendar.app.domain.repository

import com.ppicalendar.app.domain.model.EventStatus
import com.ppicalendar.app.domain.model.PlacementEvent
import kotlinx.coroutines.flow.Flow

interface PlacementEventRepository {
    fun getAllEventsFlow(): Flow<List<PlacementEvent>>
    fun getPendingEventsFlow(): Flow<List<PlacementEvent>>
    suspend fun getEventById(id: Long): PlacementEvent?
    suspend fun getEventByNotificationKey(notificationKey: String): PlacementEvent?
    suspend fun insertEvent(event: PlacementEvent): Long
    suspend fun updateEvent(event: PlacementEvent)
    suspend fun updateEventStatus(id: Long, status: EventStatus, calendarEventId: Long? = null)
    suspend fun deleteEvent(id: Long)
    suspend fun clearDismissedEvents()
}
