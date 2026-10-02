package com.ppicalendar.app.data.extractor

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern

object DateTimeParser {

    private val TIME_FORMAT_24 = DateTimeFormatter.ofPattern("HH:mm")

    /**
     * Extracts start and end times from text, normalizing to 24-hour HH:mm.
     */
    fun extractTimeRange(text: String): Pair<String, String> {
        val normalized = text.replace("IST", "", ignoreCase = true)
            .replace("hrs", "", ignoreCase = true)
            .replace("hours", "", ignoreCase = true)

        // Range pattern: "6:00 PM - 7:30 PM", "6 PM to 8 PM", "10:00 AM - 12:00 PM", "18:00 - 19:30"
        val rangeRegex = Regex(
            """\b(\d{1,2}(?::\d{2})?\s*(?:AM|PM|am|pm)?)\s*(?:-|–|—|to|till)\s*(\d{1,2}(?::\d{2})?\s*(?:AM|PM|am|pm))\b""",
            RegexOption.IGNORE_CASE
        )
        val rangeMatch = rangeRegex.find(normalized)
        if (rangeMatch != null) {
            var startRaw = rangeMatch.groupValues[1].trim()
            val endRaw = rangeMatch.groupValues[2].trim()

            val endAmPm = if (endRaw.contains("PM", ignoreCase = true)) "PM" else if (endRaw.contains("AM", ignoreCase = true)) "AM" else ""
            if (!startRaw.contains("AM", ignoreCase = true) && !startRaw.contains("PM", ignoreCase = true) && endAmPm.isNotEmpty()) {
                startRaw = "$startRaw $endAmPm"
            }

            val startTime = parseSingleTime(startRaw)
            val endTime = parseSingleTime(endRaw)
            return Pair(startTime, endTime)
        }

        // Single time pattern with prefix: "at 6:00 PM", "time : 7pm", "timing : 5:30 PM"
        val prefixedTimeRegex = Regex(
            """\b(?:at|time|starts?|timing|from)?\s*[:\s]?\s*(\d{1,2}(?::\d{2})?\s*(?:AM|PM|am|pm))\b""",
            RegexOption.IGNORE_CASE
        )
        val prefixMatch = prefixedTimeRegex.find(normalized)
        if (prefixMatch != null) {
            val startRaw = prefixMatch.groupValues[1].trim()
            val startTime = parseSingleTime(startRaw)
            if (startTime.isNotBlank()) {
                return Pair(startTime, "")
            }
        }

        // Direct AM/PM pattern: "7pm", "6:30 PM"
        val directAmPmRegex = Regex("""\b(\d{1,2}(?::\d{2})?\s*(?:AM|PM|am|pm))\b""", RegexOption.IGNORE_CASE)
        val directMatch = directAmPmRegex.find(normalized)
        if (directMatch != null) {
            val startRaw = directMatch.groupValues[1].trim()
            val startTime = parseSingleTime(startRaw)
            if (startTime.isNotBlank()) {
                return Pair(startTime, "")
            }
        }

        // Fallback 24-hour pattern: "18:30" or "09:00"
        val militaryRegex = Regex("""\b([01]?\d|2[0-3]):([0-5]\d)\b""")
        val militaryMatch = militaryRegex.find(normalized)
        if (militaryMatch != null) {
            val h = militaryMatch.groupValues[1].toInt()
            val m = militaryMatch.groupValues[2].toInt()
            return Pair(String.format(Locale.US, "%02d:%02d", h, m), "")
        }

        return Pair("", "")
    }

    fun parseSingleTime(rawTime: String): String {
        val clean = rawTime.trim().uppercase(Locale.ENGLISH)
        if (clean.isBlank()) return ""

        val hasPm = clean.contains("PM")
        val hasAm = clean.contains("AM")
        val digitsPart = clean.replace("AM", "").replace("PM", "").trim()

        val parts = digitsPart.split(":")
        val hours = parts.getOrNull(0)?.toIntOrNull() ?: return ""
        val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0

        var normalizedHour = hours
        if (hasPm && hours < 12) {
            normalizedHour = hours + 12
        } else if (hasAm && hours == 12) {
            normalizedHour = 0
        } else if (!hasPm && !hasAm && hours in 1..7) {
            normalizedHour = hours + 12
        }

        if (normalizedHour in 0..23 && minutes in 0..59) {
            return String.format(Locale.US, "%02d:%02d", normalizedHour, minutes)
        }

        return ""
    }

    /**
     * Formats an ISO date string (YYYY-MM-DD) or raw date into Indian standard format (DD/MM/YYYY).
     */
    fun formatIndianDate(rawDate: String): String {
        if (rawDate.isBlank()) return ""
        val trimmed = rawDate.trim()
        
        // Try ISO LocalDate: 2026-10-07 -> 07/10/2026
        try {
            if (trimmed.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
                val parts = trimmed.split("-")
                return "${parts[2]}/${parts[1]}/${parts[0]}"
            }
        } catch (e: Exception) {
            // Ignore
        }
        
        return trimmed
    }

    /**
     * Formats 24-hour time (HH:mm) into Indian 12-hour AM/PM format (e.g. 07:00 PM).
     */
    fun formatIndianTime(rawTime: String): String {
        if (rawTime.isBlank()) return ""
        val trimmed = rawTime.trim()
        
        // If it's in HH:mm format
        if (trimmed.matches(Regex("""\d{1,2}:\d{2}"""))) {
            val parts = trimmed.split(":")
            val h = parts[0].toIntOrNull() ?: return trimmed
            val m = parts[1].toIntOrNull() ?: 0
            val amPm = if (h >= 12) "PM" else "AM"
            val displayHour = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            return String.format(Locale.US, "%02d:%02d %s", displayHour, m, amPm)
        }
        
        // If already has AM/PM
        if (trimmed.contains("AM", ignoreCase = true) || trimmed.contains("PM", ignoreCase = true)) {
            return trimmed.uppercase(Locale.ENGLISH)
        }
        
        return trimmed
    }

    /**
     * Formats start and end times into Indian 12-hour AM/PM format range.
     */
    fun formatIndianTimeRange(startTime: String, endTime: String?): String {
        val startFormatted = formatIndianTime(startTime)
        val endFormatted = if (!endTime.isNullOrBlank()) formatIndianTime(endTime) else ""
        
        return when {
            startFormatted.isNotBlank() && endFormatted.isNotBlank() -> "$startFormatted - $endFormatted"
            startFormatted.isNotBlank() -> startFormatted
            else -> "Time missing"
        }
    }
}
