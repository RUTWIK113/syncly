package com.ppicalendar.app.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.ppicalendar.app.domain.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "syncly_settings")

class DataStoreManager(private val context: Context) {

    private object PreferencesKeys {
        val NOTIFICATION_PROCESSING = booleanPreferencesKey("notification_processing_enabled")
        val AUTO_CALENDAR = booleanPreferencesKey("auto_calendar_creation")
        val CONFIRMATION_REQUIRED = booleanPreferencesKey("confirmation_required")
        val AUTO_JOIN_WA_GROUPS = booleanPreferencesKey("auto_join_wa_groups")
        val KEYWORDS_JSON = stringPreferencesKey("keywords_json")
        val SELECTED_CALENDAR_ID = longPreferencesKey("selected_calendar_id")
        val CONNECTED_EMAIL = stringPreferencesKey("connected_email")
        val CONNECTED_ACCOUNT_NAME = stringPreferencesKey("connected_account_name")
        val IS_EMAIL_VERIFIED = booleanPreferencesKey("is_email_verified")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val USE_AI_EXTRACTION = booleanPreferencesKey("use_ai_extraction")
        val TEST_NOTICE_COUNT = intPreferencesKey("test_notice_count")
        val DARK_THEME = booleanPreferencesKey("dark_theme_enabled")
        val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
        val LAST_RATED_APP_OPEN_COUNT = intPreferencesKey("last_rated_app_open_count")
    }

    val appOpenCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.APP_OPEN_COUNT] ?: 0
    }

    val testNoticeCountFlow: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[PreferencesKeys.TEST_NOTICE_COUNT] ?: 0
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data.map { prefs ->
        val keywordsJson = prefs[PreferencesKeys.KEYWORDS_JSON]
        val keywordsList = if (!keywordsJson.isNullOrBlank()) {
            try {
                Json.decodeFromString<List<String>>(keywordsJson)
            } catch (e: Exception) {
                AppSettings.defaultKeywords
            }
        } else {
            AppSettings.defaultKeywords
        }

        AppSettings(
            notificationProcessingEnabled = prefs[PreferencesKeys.NOTIFICATION_PROCESSING] ?: true,
            automaticCalendarCreation = prefs[PreferencesKeys.AUTO_CALENDAR] ?: true,
            confirmationRequired = prefs[PreferencesKeys.CONFIRMATION_REQUIRED] ?: false,
            autoJoinWhatsAppGroups = prefs[PreferencesKeys.AUTO_JOIN_WA_GROUPS] ?: false,
            keywords = keywordsList,
            selectedCalendarId = prefs[PreferencesKeys.SELECTED_CALENDAR_ID],
            connectedEmail = prefs[PreferencesKeys.CONNECTED_EMAIL] ?: "",
            connectedAccountName = prefs[PreferencesKeys.CONNECTED_ACCOUNT_NAME] ?: "",
            isEmailVerified = prefs[PreferencesKeys.IS_EMAIL_VERIFIED] ?: false,
            geminiApiKey = prefs[PreferencesKeys.GEMINI_API_KEY] ?: "",
            useAiExtraction = prefs[PreferencesKeys.USE_AI_EXTRACTION] ?: true,
            isDarkTheme = prefs[PreferencesKeys.DARK_THEME] ?: false
        )
    }

    suspend fun getSettings(): AppSettings = settingsFlow.first()

    suspend fun setNotificationProcessing(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.NOTIFICATION_PROCESSING] = enabled }
    }

    suspend fun setAutoCalendarCreation(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_CALENDAR] = enabled }
    }

    suspend fun setConfirmationRequired(required: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.CONFIRMATION_REQUIRED] = required }
    }

    suspend fun setAutoJoinWhatsAppGroups(autoJoin: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.AUTO_JOIN_WA_GROUPS] = autoJoin }
    }

    suspend fun setKeywords(keywords: List<String>) {
        val json = Json.encodeToString(keywords)
        context.dataStore.edit { it[PreferencesKeys.KEYWORDS_JSON] = json }
    }

    suspend fun setSelectedCalendarId(calendarId: Long?) {
        context.dataStore.edit {
            if (calendarId != null) {
                it[PreferencesKeys.SELECTED_CALENDAR_ID] = calendarId
            } else {
                it.remove(PreferencesKeys.SELECTED_CALENDAR_ID)
            }
        }
    }

    suspend fun setGeminiApiKey(apiKey: String) {
        context.dataStore.edit { it[PreferencesKeys.GEMINI_API_KEY] = apiKey }
    }

    suspend fun setUseAiExtraction(useAi: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.USE_AI_EXTRACTION] = useAi }
    }

    suspend fun setConnectedAccount(
        email: String,
        accountName: String,
        isVerified: Boolean,
        calendarId: Long?
    ) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.CONNECTED_EMAIL] = email
            prefs[PreferencesKeys.CONNECTED_ACCOUNT_NAME] = accountName
            prefs[PreferencesKeys.IS_EMAIL_VERIFIED] = isVerified
            if (calendarId != null) {
                prefs[PreferencesKeys.SELECTED_CALENDAR_ID] = calendarId
            } else {
                prefs.remove(PreferencesKeys.SELECTED_CALENDAR_ID)
            }
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.DARK_THEME] = isDark }
    }

    suspend fun incrementTestNoticeCount() {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.TEST_NOTICE_COUNT] ?: 0
            prefs[PreferencesKeys.TEST_NOTICE_COUNT] = current + 1
        }
    }

    suspend fun recordAppOpen(): Pair<Int, Int> {
        var openCount = 1
        var lastRated = 0
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.APP_OPEN_COUNT] ?: 0
            openCount = current + 1
            prefs[PreferencesKeys.APP_OPEN_COUNT] = openCount
            lastRated = prefs[PreferencesKeys.LAST_RATED_APP_OPEN_COUNT] ?: 0
        }
        return Pair(openCount, lastRated)
    }

    suspend fun setLastRatedAppOpenCount(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.LAST_RATED_APP_OPEN_COUNT] = count
        }
    }

    suspend fun saveAppSettings(settings: AppSettings) {
        val keywordsJson = Json.encodeToString(settings.keywords)
        context.dataStore.edit { prefs ->
            prefs[PreferencesKeys.NOTIFICATION_PROCESSING] = settings.notificationProcessingEnabled
            prefs[PreferencesKeys.AUTO_CALENDAR] = settings.automaticCalendarCreation
            prefs[PreferencesKeys.CONFIRMATION_REQUIRED] = settings.confirmationRequired
            prefs[PreferencesKeys.AUTO_JOIN_WA_GROUPS] = settings.autoJoinWhatsAppGroups
            prefs[PreferencesKeys.KEYWORDS_JSON] = keywordsJson
            if (settings.selectedCalendarId != null) {
                prefs[PreferencesKeys.SELECTED_CALENDAR_ID] = settings.selectedCalendarId
            } else {
                prefs.remove(PreferencesKeys.SELECTED_CALENDAR_ID)
            }
            prefs[PreferencesKeys.CONNECTED_EMAIL] = settings.connectedEmail
            prefs[PreferencesKeys.CONNECTED_ACCOUNT_NAME] = settings.connectedAccountName
            prefs[PreferencesKeys.IS_EMAIL_VERIFIED] = settings.isEmailVerified
            prefs[PreferencesKeys.GEMINI_API_KEY] = settings.geminiApiKey
            prefs[PreferencesKeys.USE_AI_EXTRACTION] = settings.useAiExtraction
            prefs[PreferencesKeys.DARK_THEME] = settings.isDarkTheme
        }
    }
}
