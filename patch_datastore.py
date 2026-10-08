import sys
import io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
path = r'C:\Users\HP\Downloads\Syncly\app\src\main\java\com\ppicalendar\app\data\datastore\DataStoreManager.kt'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

content = content.replace(
'            accessibilityEnabled = prefs[PreferencesKeys.ACCESSIBILITY_ENABLED] ?: false',
'            accessibilityEnabled = prefs[PreferencesKeys.ACCESSIBILITY_ENABLED] ?: false,\n            googleCalendarIntegrationEnabled = prefs[PreferencesKeys.GOOGLE_CALENDAR_INTEGRATION] ?: false,\n            googleCalendarSyncMode = prefs[PreferencesKeys.GOOGLE_CALENDAR_SYNC_MODE] ?: "Manual"'
)

content = content.replace(
'    suspend fun setAccessibilityEnabled(enabled: Boolean) {\n        context.dataStore.edit { it[PreferencesKeys.ACCESSIBILITY_ENABLED] = enabled }\n    }',
'    suspend fun setAccessibilityEnabled(enabled: Boolean) {\n        context.dataStore.edit { it[PreferencesKeys.ACCESSIBILITY_ENABLED] = enabled }\n    }\n\n    suspend fun setGoogleCalendarIntegrationEnabled(enabled: Boolean) {\n        context.dataStore.edit { it[PreferencesKeys.GOOGLE_CALENDAR_INTEGRATION] = enabled }\n    }\n\n    suspend fun setGoogleCalendarSyncMode(mode: String) {\n        context.dataStore.edit { it[PreferencesKeys.GOOGLE_CALENDAR_SYNC_MODE] = mode }\n    }'
)

with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
