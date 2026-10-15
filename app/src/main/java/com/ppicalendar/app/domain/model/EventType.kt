package com.ppicalendar.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class EventType(val displayName: String) {
    PRE_PLACEMENT_TALK("Pre-placement talk"),
    ONLINE_ASSESSMENT("Online assessment"),
    RESUME_SUBMISSION("Resume submission"),
    RESUME_DEADLINE("Resume submission"),
    REGISTRATION("Registration"),
    REGISTRATION_DEADLINE("Registration"),
    INTERVIEW("Interview"),
    GROUP_DISCUSSION("Group discussion"),
    SHORTLIST_ANNOUNCEMENT("Shortlist announcement"),
    COMPANY_BRIEFING("Company briefing"),
    COMPANY_SESSION("Company briefing"),
    GENERAL_EVENT("General event"),
    PPI("PPI"),
    OTHER("Other");

    companion object {
        fun fromString(typeStr: String): EventType {
            val normalized = typeStr.trim().uppercase().replace(" ", "_").replace("-", "_")
            return entries.find { it.name == normalized } ?: when {
                normalized.contains("PPI") -> PPI
                normalized.contains("PPT") || normalized.contains("PRE_PLACEMENT") || normalized.contains("TALK") -> PRE_PLACEMENT_TALK
                normalized.contains("OA") || normalized.contains("ASSESSMENT") || normalized.contains("TEST") || normalized.contains("CODING") -> ONLINE_ASSESSMENT
                normalized.contains("RESUME") || normalized.contains("CV") -> RESUME_SUBMISSION
                normalized.contains("REGISTRATION") || normalized.contains("REGISTER") || normalized.contains("APPLY") -> REGISTRATION
                normalized.contains("GD") || normalized.contains("GROUP_DISCUSSION") -> GROUP_DISCUSSION
                normalized.contains("SHORTLIST") || normalized.contains("SHORTLISTED") || normalized.contains("SELECTED") -> SHORTLIST_ANNOUNCEMENT
                normalized.contains("BRIEFING") || normalized.contains("SESSION") || normalized.contains("WORKSHOP") -> COMPANY_BRIEFING
                normalized.contains("INTERVIEW") || normalized.contains("HR") || normalized.contains("SLOT") -> INTERVIEW
                normalized.contains("GENERAL") || normalized.contains("DRIVE") || normalized.contains("RECRUITMENT") -> GENERAL_EVENT
                else -> OTHER
            }
        }
    }
}
