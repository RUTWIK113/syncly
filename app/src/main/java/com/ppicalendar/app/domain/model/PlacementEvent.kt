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
    val title: String = DEFAULT_TITLE_FALLBACK,
    val eventType: EventType,
    val date: String, // ISO format: YYYY-MM-DD
    val startTime: String, // 24-hr format: HH:mm
    val endTime: String? = null, // 24-hr format: HH:mm
    val deadlineDate: String? = null, // ISO format: YYYY-MM-DD
    val deadlineTime: String? = null, // 24-hr format: HH:mm
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
    companion object {
        const val DEFAULT_TITLE_FALLBACK = "-------"
        const val DEFAULT_DEADLINE_TIME = "23:00"

        fun normalizeTitle(input: String?): String {
            return if (input.isNullOrBlank()) DEFAULT_TITLE_FALLBACK else input.trim()
        }

        fun normalizeDeadlineTime(deadlineDate: String?, deadlineTime: String?): String? {
            if (deadlineDate.isNullOrBlank()) return null
            return if (deadlineTime.isNullOrBlank()) DEFAULT_DEADLINE_TIME else deadlineTime.trim()
        }
    }

    val displayTitle: String
        get() = if (title.isNotBlank()) title else DEFAULT_TITLE_FALLBACK

    val formattedTitle: String
        get() = when {
            company.isNotBlank() && displayTitle != DEFAULT_TITLE_FALLBACK -> "[$company] | $displayTitle"
            company.isNotBlank() -> "[$company] | [${eventType.displayName}]"
            else -> displayTitle
        }

    val isMissingEssentialInfo: Boolean
        get() = date.isBlank() || startTime.isBlank() || company.isBlank()
}
