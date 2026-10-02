package com.ppicalendar.app.domain.usecase

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.regex.Pattern

class ResolveDateUseCase {

    /**
     * Resolves relative date phrases ("today", "tomorrow", "Monday", "next Friday", "24th Oct", etc.)
     * to standard ISO date YYYY-MM-DD using the provided reference date.
     */
    fun resolve(dateStr: String, referenceDate: LocalDate = LocalDate.now()): String {
        val raw = dateStr.trim().lowercase(Locale.ENGLISH)
        if (raw.isBlank()) return ""

        // If already in YYYY-MM-DD format
        if (raw.matches(Regex("""^\d{4}-\d{2}-\d{2}$"""))) {
            return raw
        }

        // Relative keywords
        when {
            raw.contains("today") || raw.contains("tonight") -> {
                return referenceDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            }
            raw.contains("day after tomorrow") -> {
                return referenceDate.plusDays(2).format(DateTimeFormatter.ISO_LOCAL_DATE)
            }
            raw.contains("tomorrow") -> {
                return referenceDate.plusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            }
            raw.contains("yesterday") -> {
                return referenceDate.minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
            }
        }

        // Days of the week (e.g., "Monday", "this Friday", "next Tuesday")
        val dayOfWeekMap = mapOf(
            "monday" to DayOfWeek.MONDAY,
            "mon" to DayOfWeek.MONDAY,
            "tuesday" to DayOfWeek.TUESDAY,
            "tue" to DayOfWeek.TUESDAY,
            "tues" to DayOfWeek.TUESDAY,
            "wednesday" to DayOfWeek.WEDNESDAY,
            "wed" to DayOfWeek.WEDNESDAY,
            "thursday" to DayOfWeek.THURSDAY,
            "thu" to DayOfWeek.THURSDAY,
            "thur" to DayOfWeek.THURSDAY,
            "thurs" to DayOfWeek.THURSDAY,
            "friday" to DayOfWeek.FRIDAY,
            "fri" to DayOfWeek.FRIDAY,
            "saturday" to DayOfWeek.SATURDAY,
            "sat" to DayOfWeek.SATURDAY,
            "sunday" to DayOfWeek.SUNDAY,
            "sun" to DayOfWeek.SUNDAY
        )

        for ((dayName, targetDow) in dayOfWeekMap) {
            val pattern = Regex("""\b(next\s+|this\s+|coming\s+)?$dayName\b""", RegexOption.IGNORE_CASE)
            val match = pattern.find(raw)
            if (match != null) {
                val prefix = match.groupValues[1].trim().lowercase(Locale.ENGLISH)
                val isNext = prefix == "next"
                var target = referenceDate.with(TemporalAdjusters.nextOrSame(targetDow))
                if (target == referenceDate && !raw.contains("today")) {
                    target = target.plusWeeks(1)
                } else if (isNext && target == referenceDate) {
                    target = target.plusWeeks(1)
                }
                return target.format(DateTimeFormatter.ISO_LOCAL_DATE)
            }
        }

        // Standard explicit dates like "24th Oct", "24 October", "Oct 24", "24/10/2026", "24-10-2026"
        val explicitDate = parseExplicitDate(raw, referenceDate)
        if (explicitDate != null) {
            return explicitDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        }

        return ""
    }

    private fun parseExplicitDate(text: String, referenceDate: LocalDate): LocalDate? {
        val currentYear = referenceDate.year

        val months = mapOf(
            "jan" to 1, "january" to 1,
            "feb" to 2, "february" to 2,
            "mar" to 3, "march" to 3,
            "apr" to 4, "april" to 4,
            "may" to 5,
            "jun" to 6, "june" to 6,
            "jul" to 7, "july" to 7,
            "aug" to 8, "august" to 8,
            "sep" to 9, "sept" to 9, "september" to 9,
            "oct" to 10, "october" to 10,
            "nov" to 11, "november" to 11,
            "dec" to 12, "december" to 12
        )

        // Pattern: 24th Oct / 24 Oct 2026 / 24th October
        val dayMonthRegex = Regex("""\b(\d{1,2})(?:st|nd|rd|th)?[\s\-_]+([a-zA-Z]{3,9})(?:[\s\-_]+(\d{4}))?\b""", RegexOption.IGNORE_CASE)
        val dmMatch = dayMonthRegex.find(text)
        if (dmMatch != null) {
            val day = dmMatch.groupValues[1].toIntOrNull()
            val monthStr = dmMatch.groupValues[2].lowercase(Locale.ENGLISH)
            val year = dmMatch.groupValues[3].toIntOrNull() ?: currentYear
            val month = months[monthStr]
            if (day != null && month != null && day in 1..31) {
                return try {
                    LocalDate.of(year, month, day)
                } catch (e: Exception) {
                    null
                }
            }
        }

        // Pattern: Oct 24th / October 24, 2026
        val monthDayRegex = Regex("""\b([a-zA-Z]{3,9})[\s\-_]+(\d{1,2})(?:st|nd|rd|th)?(?:[\s\-_,]+(\d{4}))?\b""", RegexOption.IGNORE_CASE)
        val mdMatch = monthDayRegex.find(text)
        if (mdMatch != null) {
            val monthStr = mdMatch.groupValues[1].lowercase(Locale.ENGLISH)
            val day = mdMatch.groupValues[2].toIntOrNull()
            val year = mdMatch.groupValues[3].toIntOrNull() ?: currentYear
            val month = months[monthStr]
            if (day != null && month != null && day in 1..31) {
                return try {
                    LocalDate.of(year, month, day)
                } catch (e: Exception) {
                    null
                }
            }
        }

        // Pattern: 24/10/2026 or 24-10-2026 or 24.10.2026 or 24/10
        val numericRegex = Regex("""\b(\d{1,2})[/\.-](\d{1,2})(?:[/\.-](\d{2,4}))?\b""")
        val numMatch = numericRegex.find(text)
        if (numMatch != null) {
            val day = numMatch.groupValues[1].toIntOrNull()
            val month = numMatch.groupValues[2].toIntOrNull()
            val rawYear = numMatch.groupValues[3]
            val year = when {
                rawYear.isNullOrBlank() -> currentYear
                rawYear.length == 2 -> 2000 + rawYear.toInt()
                else -> rawYear.toIntOrNull() ?: currentYear
            }
            if (day != null && month != null && month in 1..12 && day in 1..31) {
                return try {
                    LocalDate.of(year, month, day)
                } catch (e: Exception) {
                    null
                }
            }
        }

        return null
    }
}
