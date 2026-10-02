package com.ppicalendar.app.data.repository

import com.ppicalendar.app.data.datastore.DataStoreManager
import com.ppicalendar.app.domain.model.AppSettings
import com.ppicalendar.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class SettingsRepositoryImpl(
    private val dataStoreManager: DataStoreManager
) : SettingsRepository {

    override fun getSettingsFlow(): Flow<AppSettings> {
        return dataStoreManager.settingsFlow
    }

    override suspend fun getSettings(): AppSettings {
        return dataStoreManager.getSettings()
    }

    override suspend fun updateNotificationProcessingEnabled(enabled: Boolean) {
        dataStoreManager.setNotificationProcessing(enabled)
    }

    override suspend fun updateAutomaticCalendarCreation(enabled: Boolean) {
        dataStoreManager.setAutoCalendarCreation(enabled)
    }

    override suspend fun updateConfirmationRequired(required: Boolean) {
        dataStoreManager.setConfirmationRequired(required)
    }

    override suspend fun updateAutoJoinWhatsAppGroups(autoJoin: Boolean) {
        dataStoreManager.setAutoJoinWhatsAppGroups(autoJoin)
    }

    override suspend fun updateKeywords(keywords: List<String>) {
        dataStoreManager.setKeywords(keywords)
    }

    override suspend fun addKeyword(keyword: String) {
        val clean = keyword.trim()
        if (clean.isBlank()) return
        val current = dataStoreManager.getSettings().keywords
        if (!current.any { it.equals(clean, ignoreCase = true) }) {
            dataStoreManager.setKeywords(current + clean)
        }
    }

    override suspend fun removeKeyword(keyword: String) {
        val current = dataStoreManager.getSettings().keywords
        dataStoreManager.setKeywords(current.filterNot { it.equals(keyword, ignoreCase = true) })
    }

    override suspend fun updateSelectedCalendarId(calendarId: Long?) {
        dataStoreManager.setSelectedCalendarId(calendarId)
    }

    override suspend fun updateConnectedAccount(email: String, accountName: String, isVerified: Boolean, calendarId: Long?) {
        dataStoreManager.setConnectedAccount(email, accountName, isVerified, calendarId)
    }

    override suspend fun updateGeminiApiKey(apiKey: String) {
        dataStoreManager.setGeminiApiKey(apiKey.trim())
    }

    override suspend fun updateUseAiExtraction(useAi: Boolean) {
        dataStoreManager.setUseAiExtraction(useAi)
    }
}
