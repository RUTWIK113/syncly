package com.ppicalendar.app.data.local

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.ppicalendar.app.data.local.entity.CompanyAttachmentEntity
import com.ppicalendar.app.data.local.entity.CompanyEntity
import com.ppicalendar.app.data.local.entity.IncentivePointEntity
import com.ppicalendar.app.data.local.entity.PlacementEventEntity
import com.ppicalendar.app.data.notification.NotificationHelper
import com.ppicalendar.app.domain.model.EventStatus
import com.ppicalendar.app.domain.model.EventType
import com.ppicalendar.app.domain.model.PlacementEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

object SynclyBackupManager {

    private const val BACKUP_FILE_NAME = "syncly_backup_snapshot.json"

    suspend fun createBackupSnapshot(context: Context, database: AppDatabase): Boolean = withContext(Dispatchers.IO) {
        try {
            val events = database.placementEventDao().getAllEventsImmediate()
            val companies = database.companyDao().getAllCompaniesImmediate()
            val attachments = database.companyAttachmentDao().getAllAttachmentsImmediate()
            val points = database.incentivePointDao().getAllPointsImmediate()

            // Safety guard: if DB is completely empty, don't overwrite an existing populated backup
            if (events.isEmpty() && companies.isEmpty() && points.isEmpty()) {
                val existing = File(context.filesDir, BACKUP_FILE_NAME)
                if (existing.exists() && existing.length() > 20) {
                    return@withContext false
                }
            }

            val root = JSONObject().apply {
                put("version", 1)
                put("timestamp", System.currentTimeMillis())

                val eventsArray = JSONArray()
                events.forEach { e ->
                    eventsArray.put(JSONObject().apply {
                        put("id", e.id)
                        put("notificationKey", e.notificationKey)
                        put("company", e.company)
                        put("title", e.title)
                        put("eventType", e.eventType)
                        put("date", e.date)
                        put("startTime", e.startTime)
                        put("endTime", e.endTime ?: "")
                        put("deadlineDate", e.deadlineDate ?: "")
                        put("deadlineTime", e.deadlineTime ?: "")
                        put("venue", e.venue ?: "")
                        put("meetingUrl", e.meetingUrl ?: "")
                        put("description", e.description ?: "")
                        put("rawNotificationSnippet", e.rawNotificationSnippet ?: "")
                        put("incentivePoints", e.incentivePoints ?: 0)
                        put("confidence", e.confidence)
                        put("status", e.status)
                        put("calendarEventId", e.calendarEventId ?: -1L)
                        put("createdAt", e.createdAt)
                    })
                }
                put("events", eventsArray)

                val companiesArray = JSONArray()
                companies.forEach { c ->
                    companiesArray.put(JSONObject().apply {
                        put("id", c.id)
                        put("name", c.name)
                        put("notes", c.notes ?: "")
                        put("payScale", c.payScale ?: "")
                        put("roleNames", c.roleNames ?: "")
                        put("website", c.website ?: "")
                        put("applied", c.applied ?: false)
                        put("createdAt", c.createdAt)
                    })
                }
                put("companies", companiesArray)

                val attachmentsArray = JSONArray()
                attachments.forEach { a ->
                    attachmentsArray.put(JSONObject().apply {
                        put("id", a.id)
                        put("companyId", a.companyId)
                        put("title", a.title)
                        put("uriString", a.uriString ?: "")
                        put("textContent", a.textContent ?: "")
                        put("type", a.type)
                        put("fileSizeFormatted", a.fileSizeFormatted ?: "")
                        put("createdAt", a.createdAt)
                    })
                }
                put("attachments", attachmentsArray)

                val pointsArray = JSONArray()
                points.forEach { p ->
                    pointsArray.put(JSONObject().apply {
                        put("id", p.id)
                        put("title", p.title)
                        put("points", p.points)
                        put("date", p.date)
                        put("note", p.note ?: "")
                        put("createdAt", p.createdAt)
                    })
                }
                put("points", pointsArray)
            }

            val jsonString = root.toString(2)

            // 1. Write to internal filesDir (private app sandbox)
            val internalFile = File(context.filesDir, BACKUP_FILE_NAME)
            internalFile.writeText(jsonString, Charsets.UTF_8)

            // 2. Write to external app-specific filesDir (persists across app updates)
            try {
                val externalDir = context.getExternalFilesDir("backup")
                if (externalDir != null && (externalDir.exists() || externalDir.mkdirs())) {
                    val externalFile = File(externalDir, BACKUP_FILE_NAME)
                    externalFile.writeText(jsonString, Charsets.UTF_8)
                }
            } catch (e: Exception) {
                // Ignore external storage errors
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun restoreFromBackupIfEmpty(
        context: Context,
        database: AppDatabase,
        notificationHelper: NotificationHelper?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val eventCount = database.placementEventDao().getAllEventsImmediate().size

            // If events already exist, no need to auto-restore
            if (eventCount > 0) {
                return@withContext false
            }

            // Check internal files first, then external
            var backupFile = File(context.filesDir, BACKUP_FILE_NAME)
            if (!backupFile.exists() || backupFile.length() < 10) {
                val extFile = File(context.getExternalFilesDir("backup"), BACKUP_FILE_NAME)
                if (extFile.exists() && extFile.length() >= 10) {
                    backupFile = extFile
                }
            }

            var restored = false
            if (backupFile.exists() && backupFile.length() >= 10) {
                restored = performRestoreFromJson(backupFile.readText(Charsets.UTF_8), database, notificationHelper)
            }

            // If events are still empty, attempt Calendar Auto-Recovery
            val currentEventCount = database.placementEventDao().getAllEventsImmediate().size
            val recoveredFromCalendar = if (currentEventCount == 0) {
                recoverEventsFromCalendar(context, database, notificationHelper)
            } else false

            restored || recoveredFromCalendar
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun performRestoreFromJson(
        jsonString: String,
        database: AppDatabase,
        notificationHelper: NotificationHelper?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(jsonString)

            val companiesArray = root.optJSONArray("companies") ?: JSONArray()
            for (i in 0 until companiesArray.length()) {
                val obj = companiesArray.getJSONObject(i)
                val company = CompanyEntity(
                    id = obj.optLong("id", 0L),
                    name = obj.getString("name"),
                    notes = obj.optString("notes").ifBlank { null },
                    payScale = obj.optString("payScale").ifBlank { null },
                    roleNames = obj.optString("roleNames").ifBlank { null },
                    website = obj.optString("website").ifBlank { null },
                    applied = if (obj.has("applied")) obj.getBoolean("applied") else null,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.companyDao().insert(company)
            }

            val eventsArray = root.optJSONArray("events") ?: JSONArray()
            for (i in 0 until eventsArray.length()) {
                val obj = eventsArray.getJSONObject(i)
                val event = PlacementEventEntity(
                    id = obj.optLong("id", 0L),
                    notificationKey = obj.optString("notificationKey", UUID.randomUUID().toString()),
                    company = obj.getString("company"),
                    title = obj.optString("title", PlacementEvent.DEFAULT_TITLE_FALLBACK),
                    eventType = obj.optString("eventType", EventType.GENERAL_EVENT.name),
                    date = obj.getString("date"),
                    startTime = obj.getString("startTime"),
                    endTime = obj.optString("endTime").ifBlank { null },
                    deadlineDate = obj.optString("deadlineDate").ifBlank { null },
                    deadlineTime = obj.optString("deadlineTime").ifBlank { null },
                    venue = obj.optString("venue").ifBlank { null },
                    meetingUrl = obj.optString("meetingUrl").ifBlank { null },
                    description = obj.optString("description").ifBlank { null },
                    rawNotificationSnippet = obj.optString("rawNotificationSnippet").ifBlank { null },
                    incentivePoints = if (obj.has("incentivePoints")) obj.getInt("incentivePoints") else null,
                    confidence = obj.optDouble("confidence", 1.0),
                    status = obj.optString("status", EventStatus.CREATED_IN_CALENDAR.name),
                    calendarEventId = if (obj.has("calendarEventId") && obj.getLong("calendarEventId") > 0) obj.getLong("calendarEventId") else null,
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.placementEventDao().insert(event)

                if (notificationHelper != null && event.status != EventStatus.DISMISSED.name) {
                    notificationHelper.scheduleEventReminder(event.toDomain())
                }
            }

            val attachmentsArray = root.optJSONArray("attachments") ?: JSONArray()
            for (i in 0 until attachmentsArray.length()) {
                val obj = attachmentsArray.getJSONObject(i)
                val attachment = CompanyAttachmentEntity(
                    id = obj.optLong("id", 0L),
                    companyId = obj.getLong("companyId"),
                    title = obj.getString("title"),
                    uriString = obj.optString("uriString").ifBlank { null },
                    textContent = obj.optString("textContent").ifBlank { null },
                    type = obj.optString("type", "NOTE"),
                    fileSizeFormatted = obj.optString("fileSizeFormatted").ifBlank { null },
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.companyAttachmentDao().insert(attachment)
            }

            val pointsArray = root.optJSONArray("points") ?: JSONArray()
            for (i in 0 until pointsArray.length()) {
                val obj = pointsArray.getJSONObject(i)
                val point = IncentivePointEntity(
                    id = obj.optLong("id", 0L),
                    title = obj.getString("title"),
                    points = obj.getInt("points"),
                    date = obj.getString("date"),
                    note = obj.optString("note").ifBlank { null },
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                )
                database.incentivePointDao().insert(point)
            }

            eventsArray.length() > 0 || companiesArray.length() > 0 || pointsArray.length() > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun recoverEventsFromCalendar(
        context: Context,
        database: AppDatabase,
        notificationHelper: NotificationHelper?
    ): Boolean = withContext(Dispatchers.IO) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasPermission) return@withContext false

        var cursor: Cursor? = null
        var recoveredCount = 0

        try {
            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND
            )

            // Look for events created in the last 1 year or upcoming
            val oneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
            val selection = "${CalendarContract.Events.DTSTART} >= ?"
            val selectionArgs = arrayOf(oneYearAgo.toString())

            cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} DESC"
            )

            cursor?.let {
                val idCol = it.getColumnIndexOrThrow(CalendarContract.Events._ID)
                val titleCol = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                val descCol = it.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
                val locCol = it.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
                val startCol = it.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
                val endCol = it.getColumnIndexOrThrow(CalendarContract.Events.DTEND)

                val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

                while (it.moveToNext()) {
                    val calId = it.getLong(idCol)
                    val title = it.getString(titleCol) ?: ""
                    val description = it.getString(descCol) ?: ""
                    val location = it.getString(locCol) ?: ""
                    val dtStart = it.getLong(startCol)
                    val dtEnd = it.getLong(endCol)

                    // Check if this event was created by Syncly or is a placement notice
                    val isSynclyEvent = title.contains("•") ||
                            title.contains("|") ||
                            description.contains("🎯") ||
                            description.contains("Placement") ||
                            description.contains("Syncly")

                    if (isSynclyEvent && title.isNotBlank() && dtStart > 0) {
                        val parts = if (title.contains("•")) {
                            title.split("•").map { p -> p.trim() }
                        } else if (title.contains("|")) {
                            title.split("|").map { p -> p.trim() }
                        } else {
                            listOf(title)
                        }

                        val rawComp = parts.firstOrNull()?.replace("🎯", "")?.replace("[", "")?.replace("]", "")?.trim()
                        val company = PlacementEvent.normalizeCompany(rawComp)
                        val eventTypeStr = if (parts.size > 1) parts[1].replace("[", "").replace("]", "").trim() else "General Event"
                        val eventType = EventType.fromString(eventTypeStr)

                        val startDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dtStart), ZoneId.systemDefault())
                        val endDateTime = if (dtEnd > dtStart) LocalDateTime.ofInstant(Instant.ofEpochMilli(dtEnd), ZoneId.systemDefault()) else null

                        val eventDate = startDateTime.format(dateFormatter)
                        val startTime = startDateTime.format(timeFormatter)
                        val endTime = endDateTime?.format(timeFormatter)

                        val entity = PlacementEventEntity(
                            notificationKey = "recovered_cal_$calId",
                            company = company,
                            title = "",
                            eventType = eventType.name,
                            date = eventDate,
                            startTime = startTime,
                            endTime = endTime,
                            venue = location.ifBlank { null },
                            meetingUrl = null,
                            description = description.ifBlank { null },
                            rawNotificationSnippet = "Recovered from Google Calendar sync",
                            confidence = 1.0,
                            status = EventStatus.CREATED_IN_CALENDAR.name,
                            calendarEventId = calId,
                            createdAt = System.currentTimeMillis()
                        )

                        database.placementEventDao().insert(entity)
                        if (company != PlacementEvent.DEFAULT_COMPANY_FALLBACK) {
                            database.companyDao().insert(
                                CompanyEntity(
                                    name = company,
                                    createdAt = System.currentTimeMillis()
                                )
                            )
                        }

                        if (notificationHelper != null) {
                            notificationHelper.scheduleEventReminder(entity.toDomain())
                        }
                        recoveredCount++
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            cursor?.close()
        }

        recoveredCount > 0
    }
}
