package com.ppicalendar.app.domain.model

data class CalendarInfo(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val isPrimary: Boolean = false
)

data class AppSettings(
    val notificationProcessingEnabled: Boolean = true,
    val automaticCalendarCreation: Boolean = true,
    val confirmationRequired: Boolean = false,
    val autoJoinWhatsAppGroups: Boolean = false,
    val keywords: List<String> = defaultKeywords,
    val selectedCalendarId: Long? = null,
    val connectedEmail: String = "",
    val connectedAccountName: String = "",
    val isEmailVerified: Boolean = false,
    val geminiApiKey: String = "",
    val useAiExtraction: Boolean = true,
    val isDarkTheme: Boolean = false,
    val defaultReminderMinutes: Int = 60
) {
    companion object {
        val defaultKeywords = listOf(
            "PPT Announcement",
            "Pre-placement talk",
            "PPT",
            "OA",
            "Online assessment",
            "Interview",
            "PPI",
            "Registration",
            "Test",
            "Shortlist",
            "Placement",
            "Company session",
            "Coding round",
            "Hackerrank",
            "Hackerearth",
            "Mettl",
            "Superset"
        )
    }
}
