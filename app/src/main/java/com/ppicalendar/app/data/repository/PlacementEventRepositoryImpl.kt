package com.ppicalendar.app.data.repository

import com.ppicalendar.app.data.local.dao.PlacementEventDao
import com.ppicalendar.app.data.local.entity.PlacementEventEntity
import com.ppicalendar.app.domain.model.EventStatus
import com.ppicalendar.app.domain.model.PlacementEvent
import com.ppicalendar.app.domain.repository.PlacementEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlacementEventRepositoryImpl(
    private val dao: PlacementEventDao
) : PlacementEventRepository {

    override fun getAllEventsFlow(): Flow<List<PlacementEvent>> {
        return dao.getAllEvents().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getPendingEventsFlow(): Flow<List<PlacementEvent>> {
        return dao.getPendingEvents().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getEventById(id: Long): PlacementEvent? {
        return dao.getEventById(id)?.toDomain()
    }

    override suspend fun getEventByNotificationKey(notificationKey: String): PlacementEvent? {
        return dao.getEventByNotificationKey(notificationKey)?.toDomain()
    }

    override suspend fun insertEvent(event: PlacementEvent): Long {
        return dao.insert(PlacementEventEntity.fromDomain(event))
    }

    override suspend fun updateEvent(event: PlacementEvent) {
        dao.update(PlacementEventEntity.fromDomain(event))
    }

    override suspend fun updateEventStatus(id: Long, status: EventStatus, calendarEventId: Long?) {
        dao.updateStatus(id, status.name, calendarEventId)
    }

    override suspend fun deleteEvent(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun clearDismissedEvents() {
        dao.clearDismissed()
    }
}
