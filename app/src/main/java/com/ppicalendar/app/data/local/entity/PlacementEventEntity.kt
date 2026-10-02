package com.ppicalendar.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ppicalendar.app.domain.model.EventStatus
import com.ppicalendar.app.domain.model.EventType
import com.ppicalendar.app.domain.model.PlacementEvent

@Entity(tableName = "placement_events")
data class PlacementEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val notificationKey: String,
    val company: String,
    val eventType: String,
    val date: String,
    val startTime: String,
    val endTime: String?,
    val venue: String?,
    val meetingUrl: String?,
    val description: String?,
    val rawNotificationSnippet: String?,
    val incentivePoints: Int? = null,
    val confidence: Double,
    val status: String,
    val calendarEventId: Long?,
    val createdAt: Long
) {
    fun toDomain(): PlacementEvent {
        return PlacementEvent(
            id = id,
            notificationKey = notificationKey,
            company = company,
            eventType = EventType.fromString(eventType),
            date = date,
            startTime = startTime,
            endTime = endTime,
            venue = venue,
            meetingUrl = meetingUrl,
            description = description,
            rawNotificationSnippet = rawNotificationSnippet,
            incentivePoints = incentivePoints,
            confidence = confidence,
            status = try { EventStatus.valueOf(status) } catch (e: Exception) { EventStatus.PENDING_CONFIRMATION },
            calendarEventId = calendarEventId,
            createdAt = createdAt
        )
    }

    companion object {
        fun fromDomain(event: PlacementEvent): PlacementEventEntity {
            return PlacementEventEntity(
                id = event.id,
                notificationKey = event.notificationKey,
                company = event.company,
                eventType = event.eventType.name,
                date = event.date,
                startTime = event.startTime,
                endTime = event.endTime,
                venue = event.venue,
                meetingUrl = event.meetingUrl,
                description = event.description,
                rawNotificationSnippet = event.rawNotificationSnippet,
                incentivePoints = event.incentivePoints,
                confidence = event.confidence,
                status = event.status.name,
                calendarEventId = event.calendarEventId,
                createdAt = event.createdAt
            )
        }
    }
}
