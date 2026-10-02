package com.ppicalendar.app.data.telemetry

import android.content.Context
import android.os.Build
import android.util.Log
import com.ppicalendar.app.data.datastore.DataStoreManager
import com.ppicalendar.app.data.local.dao.CompanyDao
import com.ppicalendar.app.data.local.dao.PlacementEventDao
import com.ppicalendar.app.domain.model.EventStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@Serializable
data class UserTelemetryRecord(
    val email: String,
    val deviceName: String,
    val androidVersion: String,
    val appVersion: String = "1.0.0",
    val appOpenCount: Int,
    val vaultCompanyCount: Int,
    val syncedEventsCount: Int,
    val pendingEventsCount: Int,
    val lastActive: String,
    val timestamp: Long = System.currentTimeMillis()
)

class SynclyTelemetryManager(
    private val context: Context,
    private val dataStoreManager: DataStoreManager,
    private val companyDao: CompanyDao,
    private val placementEventDao: PlacementEventDao
) {
    companion object {
        private const val TAG = "SynclyTelemetry"
        // Connected Firebase Realtime Database endpoint for free live user telemetry
        private const val DEFAULT_TELEMETRY_ENDPOINT = "https://campulse-9f1c8-default-rtdb.asia-southeast1.firebasedatabase.app"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun syncUserTelemetry(customEndpoint: String? = null): UserTelemetryRecord? = withContext(Dispatchers.IO) {
        try {
            val settings = dataStoreManager.getSettings()
            val (openCount, _) = dataStoreManager.recordAppOpen()
            val companies = companyDao.getAllCompaniesFlow().first()
            val events = placementEventDao.getAllEvents().first()

            val syncedCount = events.count { it.status == EventStatus.CREATED_IN_CALENDAR.name }
            val pendingCount = events.count { it.status == EventStatus.PENDING_CONFIRMATION.name }

            val androidId = try {
                android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID)
            } catch (e: Exception) {
                Build.ID
            } ?: "device_${Build.MODEL.hashCode()}"

            val userEmail = if (settings.connectedEmail.isNotBlank()) {
                settings.connectedEmail
            } else {
                "Student (${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL})"
            }

            val sanitizedKey = if (settings.connectedEmail.isNotBlank()) {
                settings.connectedEmail.replace(".", "_").replace("@", "_at_")
            } else {
                "dev_$androidId"
            }

            val record = UserTelemetryRecord(
                email = userEmail,
                deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}",
                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                appVersion = "1.0.0",
                appOpenCount = openCount,
                vaultCompanyCount = companies.size,
                syncedEventsCount = syncedCount,
                pendingEventsCount = pendingCount,
                lastActive = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a")),
                timestamp = System.currentTimeMillis()
            )

            // If a Firebase URL or telemetry endpoint is provided, push update
            val targetEndpoint = customEndpoint?.ifBlank { null } ?: DEFAULT_TELEMETRY_ENDPOINT
            if (targetEndpoint.isNotBlank()) {
                val url = if (targetEndpoint.endsWith("/")) {
                    "${targetEndpoint}telemetry/users/$sanitizedKey.json"
                } else {
                    "$targetEndpoint/telemetry/users/$sanitizedKey.json"
                }

                val body = json.encodeToString(record).toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder().url(url).put(body).build()

                httpClient.newCall(request).execute().use { response ->
                    Log.d(TAG, "Telemetry dispatched for $userEmail, code: ${response.code}")
                }
            }

            record
        } catch (e: Exception) {
            Log.w(TAG, "Telemetry dispatch skipped: ${e.message}")
            null
        }
    }
}
