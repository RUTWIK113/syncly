package com.ppicalendar.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtractionResponse(
    val events: List<ExtractionResult> = emptyList()
)

@Serializable
data class ExtractionResult(
    @SerialName("is_event")
    val isEvent: Boolean = false,

    @SerialName("company")
    val company: String = "",

    @SerialName("title")
    val title: String = PlacementEvent.DEFAULT_TITLE_FALLBACK,

    @SerialName("event_type")
    val eventType: String = "",

    @SerialName("date")
    val date: String = "",

    @SerialName("start_time")
    val startTime: String = "",

    @SerialName("end_time")
    val endTime: String = "",

    @SerialName("deadline_date")
    val deadlineDate: String = "",

    @SerialName("deadline_time")
    val deadlineTime: String = "",

    @SerialName("venue")
    val venue: String = "",

    @SerialName("meeting_url")
    val meetingUrl: String = "",

    @SerialName("description")
    val description: String = "",

    @SerialName("incentive_points")
    val incentivePoints: Int? = null,

    @SerialName("confidence")
    val confidence: Double = 0.0
) {
    fun isValidForAutoCreation(): Boolean {
        return isEvent &&
                company.isNotBlank() &&
                date.isNotBlank() &&
                startTime.isNotBlank()
    }

    fun hasEssentialInfo(): Boolean {
        return date.isNotBlank() && startTime.isNotBlank()
    }
}
