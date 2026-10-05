package com.ppicalendar.app.domain.repository

import com.ppicalendar.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    fun getSettingsFlow(): Flow<AppSettings>
    suspend fun getSettings(): AppSettings
    suspend fun updateNotificationProcessingEnabled(enabled: Boolean)
    suspend fun updateAutomaticCalendarCreation(enabled: Boolean)
    suspend fun updateConfirmationRequired(required: Boolean)
    suspend fun updateAutoJoinWhatsAppGroups(autoJoin: Boolean)
    suspend fun updateKeywords(keywords: List<String>)
    suspend fun addKeyword(keyword: String)
    suspend fun removeKeyword(keyword: String)
    suspend fun updateSelectedCalendarId(calendarId: Long?)
    suspend fun updateConnectedAccount(email: String, accountName: String, isVerified: Boolean, calendarId: Long?)
    suspend fun updateGeminiApiKey(apiKey: String)
    suspend fun updateUseAiExtraction(useAi: Boolean)
    suspend fun updateAccessibilityEnabled(enabled: Boolean)
}
