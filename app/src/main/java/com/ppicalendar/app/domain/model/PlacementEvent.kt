package com.ppicalendar.app.domain.model

enum class EventStatus {
    PENDING_CONFIRMATION,
    CONFIRMED,
    CREATED_IN_CALENDAR,
    DISMISSED,
    FAILED
}

data class PlacementEvent(
    val id: Long = 0,
    val notificationKey: String,
    val company: String,
    val eventType: EventType,
    val date: String, // ISO format: YYYY-MM-DD
    val startTime: String, // 24-hr format: HH:mm
    val endTime: String? = null, // 24-hr format: HH:mm
    val venue: String? = null,
    val meetingUrl: String? = null,
    val description: String? = null,
    val rawNotificationSnippet: String? = null,
    val incentivePoints: Int? = null,
    val confidence: Double = 1.0,
    val status: EventStatus = EventStatus.PENDING_CONFIRMATION,
    val calendarEventId: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedTitle: String
        get() = "[$company] | [${eventType.displayName}]"

    val isMissingEssentialInfo: Boolean
        get() = date.isBlank() || startTime.isBlank() || company.isBlank()
}
