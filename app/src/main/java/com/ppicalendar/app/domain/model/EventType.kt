package com.ppicalendar.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class EventType(val displayName: String) {
    PPI("PPI"),
    PRE_PLACEMENT_TALK("PPT"),
    ONLINE_ASSESSMENT("OA"),
    INTERVIEW("Interview"),
    COMPANY_SESSION("Company Session"),
    REGISTRATION_DEADLINE("Registration Deadline"),
    RESUME_DEADLINE("Resume Deadline"),
    OTHER("Other Placement Event");

    companion object {
        fun fromString(typeStr: String): EventType {
            val normalized = typeStr.trim().uppercase().replace(" ", "_").replace("-", "_")
            return entries.find { it.name == normalized } ?: when {
                normalized.contains("PPI") -> PPI
                normalized.contains("PPT") || normalized.contains("TALK") -> PRE_PLACEMENT_TALK
                normalized.contains("OA") || normalized.contains("ASSESSMENT") || normalized.contains("TEST") -> ONLINE_ASSESSMENT
                normalized.contains("INTERVIEW") -> INTERVIEW
                normalized.contains("SESSION") || normalized.contains("WORKSHOP") -> COMPANY_SESSION
                normalized.contains("REGISTRATION") -> REGISTRATION_DEADLINE
                normalized.contains("RESUME") || normalized.contains("SHORTLIST") -> RESUME_DEADLINE
                else -> OTHER
            }
        }
    }
}
