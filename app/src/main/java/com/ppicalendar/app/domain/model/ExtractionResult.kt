package com.ppicalendar.app.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExtractionResult(
    @SerialName("is_event")
    val isEvent: Boolean = false,

    @SerialName("company")
    val company: String = "",

    @SerialName("event_type")
    val eventType: String = "",

    @SerialName("date")
    val date: String = "",

    @SerialName("start_time")
    val startTime: String = "",

    @SerialName("end_time")
    val endTime: String = "",

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
