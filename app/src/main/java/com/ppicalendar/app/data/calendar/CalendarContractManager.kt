package com.ppicalendar.app.data.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import android.util.Log
import androidx.core.content.ContextCompat
import com.ppicalendar.app.domain.model.CalendarInfo
import com.ppicalendar.app.domain.model.EventType
import com.ppicalendar.app.domain.model.PlacementEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

class CalendarContractManager(private val context: Context) {

    companion object {
        private const val TAG = "CalendarContractManager"
    }

    private fun hasCalendarPermission(): Boolean {
        val readPerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR)
        val writePerm = ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_CALENDAR)
        return readPerm == PackageManager.PERMISSION_GRANTED && writePerm == PackageManager.PERMISSION_GRANTED
    }

    suspend fun getAvailableCalendars(): List<CalendarInfo> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            Log.w(TAG, "Calendar permissions not granted")
            return@withContext emptyList()
        }

        val rawCalendars = mutableListOf<CalendarInfo>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.IS_PRIMARY
        )

        val uri: Uri = CalendarContract.Calendars.CONTENT_URI
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(uri, projection, null, null, null)
            cursor?.let {
                val idCol = it.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameCol = it.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accountCol = it.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val primaryCol = it.getColumnIndex(CalendarContract.Calendars.IS_PRIMARY)

                while (it.moveToNext()) {
                    val id = it.getLong(idCol)
                    val displayName = it.getString(nameCol) ?: ""
                    val accountName = it.getString(accountCol) ?: ""
                    val isPrimary = if (primaryCol != -1) it.getInt(primaryCol) == 1 else false

                    val lowerName = displayName.lowercase()
                    // Filter out secondary calendars like Holidays, Birthdays, Tasks, Contacts
                    val isSecondary = lowerName.contains("holiday") ||
                            lowerName.contains("birthday") ||
                            lowerName.contains("task") ||
                            lowerName.contains("contact") ||
                            lowerName.contains("weather")

                    if (!isSecondary && accountName.isNotBlank()) {
                        rawCalendars.add(
                            CalendarInfo(
                                id = id,
                                displayName = accountName.trim(), // Use clean account email address directly
                                accountName = accountName.trim(),
                                isPrimary = isPrimary
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying calendars: ${e.message}", e)
        } finally {
            cursor?.close()
        }

        // Group by account email so each Google account appears exactly once with its primary calendar
        val uniqueCalendars = rawCalendars
            .groupBy { it.accountName.lowercase() }
            .map { (_, list) ->
                list.find { it.isPrimary } ?: list.first()
            }

        uniqueCalendars
    }

    suspend fun getDefaultCalendarId(): Long? = withContext(Dispatchers.IO) {
        val calendars = getAvailableCalendars()
        val primary = calendars.find { it.isPrimary }
        primary?.id ?: calendars.firstOrNull()?.id
    }

    suspend fun eventExists(event: PlacementEvent, calendarId: Long?): Boolean = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission() || event.date.isBlank() || event.startTime.isBlank()) {
            return@withContext false
        }

        val targetStartMillis = calculateEpochMillis(event.date, event.startTime) ?: return@withContext false
        val windowStart = targetStartMillis - (2 * 3600 * 1000) // -2 hours window
        val windowEnd = targetStartMillis + (2 * 3600 * 1000)   // +2 hours window

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART
        )

        val selection = StringBuilder("(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?)")
        val selectionArgs = mutableListOf(windowStart.toString(), windowEnd.toString())

        if (calendarId != null) {
            selection.append(" AND (${CalendarContract.Events.CALENDAR_ID} = ?)")
            selectionArgs.add(calendarId.toString())
        }

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection.toString(),
                selectionArgs.toTypedArray(),
                null
            )
            cursor?.let {
                val titleCol = it.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
                while (it.moveToNext()) {
                    val title = it.getString(titleCol) ?: ""
                    // Check if title contains the company name
                    if (title.contains(event.company, ignoreCase = true)) {
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking duplicate event: ${e.message}", e)
        } finally {
            cursor?.close()
        }

        false
    }

    suspend fun getCalendarIdForAccount(preferredEmail: String? = null, preferredId: Long? = null): Long? = withContext(Dispatchers.IO) {
        val calendars = getAvailableCalendars()
        if (preferredId != null) {
            val exact = calendars.find { it.id == preferredId }
            if (exact != null) return@withContext exact.id
        }
        if (!preferredEmail.isNullOrBlank()) {
            val byEmail = calendars.find { it.accountName.equals(preferredEmail, ignoreCase = true) }
            if (byEmail != null) return@withContext byEmail.id
        }
        val primary = calendars.find { it.isPrimary }
        primary?.id ?: calendars.firstOrNull()?.id
    }

    suspend fun createEvent(
        event: PlacementEvent,
        calendarId: Long?,
        accountEmail: String? = null,
        reminderMinutes: Int = 60
    ): Result<Long> = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) {
            return@withContext Result.failure(SecurityException("Calendar permissions not granted"))
        }

        if (event.date.isBlank() || event.startTime.isBlank() || event.company.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Missing essential event information"))
        }

        val targetCalendarId = getCalendarIdForAccount(accountEmail, calendarId)
            ?: return@withContext Result.failure(IllegalStateException("No available Android calendar found on device"))

        val startMillis = calculateEpochMillis(event.date, event.startTime)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid date/time format: ${event.date} ${event.startTime}"))

        val endMillis = if (!event.endTime.isNullOrBlank()) {
            calculateEpochMillis(event.date, event.endTime) ?: (startMillis + (60 * 60 * 1000))
        } else {
            startMillis + (60 * 60 * 1000) // Default duration: 1 hour
        }

        val adjustedEndMillis = if (endMillis <= startMillis) startMillis + (60 * 60 * 1000) else endMillis

        // Build rich, structured key points description
        val descriptionBuilder = StringBuilder()
        descriptionBuilder.append("🎯 ${event.company} • ${event.eventType.displayName}\n\n")
        descriptionBuilder.append("📅 Date: ${event.date}\n")
        descriptionBuilder.append("⏰ Time: ${event.startTime}${if (!event.endTime.isNullOrBlank()) " - ${event.endTime}" else ""}\n")

        if (!event.venue.isNullOrBlank()) {
            descriptionBuilder.append("📍 Venue: ${event.venue}\n")
        }

        if (!event.meetingUrl.isNullOrBlank()) {
            val linkTitle = if (event.meetingUrl.contains("chat.whatsapp.com")) "WhatsApp Group" else "Meeting / Portal Link"
            descriptionBuilder.append("🔗 $linkTitle: ${event.meetingUrl}\n")
        }

        if (!event.description.isNullOrBlank()) {
            descriptionBuilder.append("\n📋 Details & Key Points:\n${event.description}\n")
        }

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, targetCalendarId)
            put(CalendarContract.Events.TITLE, event.formattedTitle)
            put(CalendarContract.Events.DESCRIPTION, descriptionBuilder.toString())
            put(CalendarContract.Events.EVENT_LOCATION, event.venue ?: if (!event.meetingUrl.isNullOrBlank()) event.meetingUrl else "")
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, adjustedEndMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
            put(CalendarContract.Events.STATUS, CalendarContract.Events.STATUS_CONFIRMED)
            put(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }

        try {
            val eventUri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                ?: return@withContext Result.failure(IllegalStateException("Failed to insert event into calendar"))

            val eventId = ContentUris.parseId(eventUri)

            // Add reminder 60 minutes before
            val reminderValues = ContentValues().apply {
                put(CalendarContract.Reminders.EVENT_ID, eventId)
                put(CalendarContract.Reminders.MINUTES, reminderMinutes)
                put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
            }
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)

            Log.d(TAG, "Successfully created calendar event with ID: $eventId for ${event.company}")
            triggerAccountSync(targetCalendarId)
            Result.success(eventId)
        } catch (e: Exception) {
            Log.e(TAG, "Error inserting event into Calendar: ${e.message}", e)
            Result.failure(e)
        }
    }

    private fun triggerAccountSync(calendarId: Long) {
        try {
            val projection = arrayOf(
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE
            )
            val uri = ContentUris.withAppendedId(CalendarContract.Calendars.CONTENT_URI, calendarId)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME))
                    val type = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_TYPE))
                    if (!name.isNullOrBlank() && !type.isNullOrBlank()) {
                        val account = android.accounts.Account(name, type)

                        // 1. Permanently enable background auto-sync on device for this account so user never has to manual sync in settings
                        try {
                            android.content.ContentResolver.setMasterSyncAutomatically(true)
                            android.content.ContentResolver.setIsSyncable(account, CalendarContract.AUTHORITY, 1)
                            android.content.ContentResolver.setSyncAutomatically(account, CalendarContract.AUTHORITY, true)
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not set sync automatically: ${e.message}")
                        }

                        // 2. Request immediate expedited sync without backoff restrictions
                        val extras = android.os.Bundle().apply {
                            putBoolean(android.content.ContentResolver.SYNC_EXTRAS_MANUAL, true)
                            putBoolean(android.content.ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                            putBoolean(android.content.ContentResolver.SYNC_EXTRAS_IGNORE_BACKOFF, true)
                            putBoolean(android.content.ContentResolver.SYNC_EXTRAS_IGNORE_SETTINGS, true)
                            putBoolean("force", true)
                        }
                        android.content.ContentResolver.requestSync(account, CalendarContract.AUTHORITY, extras)
                        try {
                            android.content.ContentResolver.requestSync(account, "com.google.android.calendar", extras)
                        } catch (e: Exception) { }
                        Log.d(TAG, "Triggered expedited calendar sync for account: $name ($type)")
                    }
                }
            }
            context.contentResolver.notifyChange(CalendarContract.Events.CONTENT_URI, null)
            context.contentResolver.notifyChange(CalendarContract.CONTENT_URI, null)
        } catch (e: Exception) {
            Log.w(TAG, "Could not trigger account sync: ${e.message}")
        }
    }

    suspend fun deleteEvent(calendarEventId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!hasCalendarPermission()) return@withContext false
        try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, calendarEventId)
            val rows = context.contentResolver.delete(deleteUri, null, null)
            rows > 0
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting calendar event: ${e.message}", e)
            false
        }
    }

    private fun calculateEpochMillis(dateStr: String, timeStr: String): Long? {
        return try {
            val localDate = LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
            val timeParts = timeStr.split(":")
            val hour = timeParts[0].toInt()
            val minute = if (timeParts.size > 1) timeParts[1].toInt() else 0
            val localTime = LocalTime.of(hour, minute)
            val localDateTime = LocalDateTime.of(localDate, localTime)
            localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse date/time: $dateStr $timeStr", e)
            null
        }
    }
}
