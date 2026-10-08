package com.ppicalendar.app.data.extractor

import com.ppicalendar.app.domain.model.EventType
import com.ppicalendar.app.domain.model.ExtractionResult
import com.ppicalendar.app.domain.usecase.ResolveDateUseCase
import java.time.LocalDate
import java.util.Locale
import java.util.regex.Pattern

class RuleBasedExtractor(
    private val resolveDateUseCase: ResolveDateUseCase = ResolveDateUseCase()
) {

    private val commonCompanies = listOf(
        "Computer Centre IIT Madras", "Computer Centre", "Computer Center",
        "UG Mechanical Placements", "ME UG Placements", "Placement Cell IITM", "Placement Cell",
        "Google", "Microsoft", "Jane Street", "Apple", "Amazon", "Uber", "Goldman Sachs",
        "Morgan Stanley", "DE Shaw", "D.E. Shaw", "Tower Research", "Graviton", "Optiver",
        "Citadel", "Jump Trading", "Qualcomm", "Texas Instruments", "TI", "Intel", "Nvidia",
        "Samsung", "Adobe", "Oracle", "Cisco", "Salesforce", "Sprinklr", "Stripe", "McKinsey",
        "Boston Consulting Group", "BCG", "Bain & Company", "Bain", "Kearney", "ITC",
        "Hindustan Unilever", "HUL", "P&G", "Procter & Gamble", "Schlumberger", "SLB", "Shell",
        "ExxonMobil", "Airbus", "Boeing", "ISRO", "Tata Steel", "Tata Motors", "L&T",
        "Reliance", "Flipkart", "Swiggy", "Zomato", "Meesho", "CRED", "Razorpay", "PhonePe",
        "Paytm", "Groww", "Zerodha", "Rubrik", "Cohesity", "Databricks", "Snowflake", "Palantir",
        "Honda R&D", "Honda", "Hyundai", "Mercedes-Benz", "BMW", "Toyota", "Bajaj", "TVS"
    )

    private val venues = listOf(
        "CLT", "Central Lecture Theatre",
        "ICSR", "IC&SR", "ICSR Auditorium", "ICSR Hall",
        "SAC", "Students Activity Centre",
        "CRC", "Central Research Facility",
        "NAC", "New Academic Complex",
        "RMN", "RMN 101", "RMN 102", "RMN Hall",
        "MSB", "MSB 101", "MSB 201",
        "ED102", "ED 102", "ED Hall",
        "CS34", "CS 34", "CS Auditorium",
        "BT Hall", "Biotech Auditorium",
        "DoMS", "Management Studies",
        "OAT", "Open Air Theatre",
        "Auditorium", "Seminar Hall", "Placement Cell",
        "Virtual", "Online", "MS Teams", "Zoom", "Google Meet"
    )

    fun extract(text: String, referenceDate: LocalDate = LocalDate.now(), forcePlacement: Boolean = false): ExtractionResult {
        if (text.isBlank()) {
            return ExtractionResult(isEvent = false)
        }

        val eventType = detectEventType(text)
        val company = detectCompany(text)
        var (startTime, endTime) = DateTimeParser.extractTimeRange(text)
        
        if (startTime.isBlank() && eventType == EventType.REGISTRATION_DEADLINE) {
            startTime = "23:00"
        }

        val rawDate = detectDateString(text)
        val resolvedDate = resolveDateUseCase.resolve(rawDate, referenceDate)
        val venue = detectVenue(text)
        val meetingUrl = detectMeetingUrl(text)
        val description = extractDescription(text, company, eventType.displayName)
        val incentivePoints = detectIncentivePoints(text)

        val isPlacementEvent = company.isNotBlank() && (
                forcePlacement ||
                eventType != EventType.OTHER ||
                        text.contains("placement", ignoreCase = true) ||
                        text.contains("talk", ignoreCase = true) ||
                        text.contains("ppt", ignoreCase = true) ||
                        text.contains("session", ignoreCase = true) ||
                        text.contains("test", ignoreCase = true) ||
                        text.contains("oa", ignoreCase = true) ||
                        text.contains("interview", ignoreCase = true) ||
                        text.contains("profile", ignoreCase = true)
                )

        var confidenceScore = 0.0
        if (isPlacementEvent) {
            confidenceScore += 0.3
            if (company.isNotBlank()) confidenceScore += 0.2
            if (resolvedDate.isNotBlank()) confidenceScore += 0.25
            if (startTime.isNotBlank()) confidenceScore += 0.25
        }

        return ExtractionResult(
            isEvent = isPlacementEvent,
            company = company,
            eventType = eventType.name,
            date = resolvedDate,
            startTime = startTime,
            endTime = endTime,
            venue = venue,
            meetingUrl = meetingUrl,
            description = description,
            incentivePoints = incentivePoints,
            confidence = (confidenceScore * 100).toInt() / 100.0
        )
    }

    private fun detectIncentivePoints(text: String): Int? {
        val patterns = listOf(
            Regex("""(?i)(?:incentive(?:\s+points?)?|attendance\s+points?|points?)\s*[:=\-]\s*(\d{1,3})\s*(?:points?|pts)?"""),
            Regex("""(?i)\b(\d{1,3})\s*(?:incentive|attendance|bonus)\s*(?:points?|pts)\b"""),
            Regex("""(?i)\b(?:award(?:s|ed)?|give(?:s)?|get|carry|carries)\s+(\d{1,3})\s*(?:points?|pts)\b"""),
            Regex("""(?i)\b(\d{1,3})\s*(?:points?|pts)\s+(?:for\s+(?:attending|attendance|joining|ppt|talk|session))\b""")
        )

        for (pattern in patterns) {
            val match = pattern.find(text)
            if (match != null) {
                val numStr = match.groupValues[1]
                numStr.toIntOrNull()?.let { points ->
                    if (points in 1..100) return points
                }
            }
        }
        return null
    }

    private fun detectEventType(text: String): EventType {
        return when {
            Regex("""\b(ppi|pre[\s\-]?placement[\s\-]?interview)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.PPI

            Regex("""\b(ppt|pre[\s\-]?placement[\s\-]?talk|pre[\s\-]?placement[\s\-]?session|presentation)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.PRE_PLACEMENT_TALK

            Regex("""\b(oa|online[\s\-]?assessment|coding[\s\-]?round|coding[\s\-]?test|technical[\s\-]?assessment|hackerearth|hackerrank|mettl)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.ONLINE_ASSESSMENT

            Regex("""\b(interview|gd|group[\s\-]?discussion|personal[\s\-]?interview|tech[\s\-]?interview|hr[\s\-]?interview)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.INTERVIEW

            Regex("""\b(company[\s\-]?session|info[\s\-]?session|webinar|workshop|interactive[\s\-]?session)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.COMPANY_SESSION

            Regex("""\b(registration[\s\-]?deadline|form[\s\-]?deadline|last[\s\-]?date[\s\-]?to[\s\-]?register|portal[\s\-]?closes?)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.REGISTRATION_DEADLINE

            Regex("""\b(resume[\s\-]?deadline|cv[\s\-]?submission|shortlist|shortlisted)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.RESUME_DEADLINE

            else -> EventType.OTHER
        }
    }

    private fun detectCompany(text: String): String {
        // Strip URLs to avoid false positive matching against company domains in links (e.g. meet.google.com, teams.microsoft.com)
        val textWithoutUrls = text.replace(Regex("""https?://[^\s<>"'{}|\\^`]+"""), "")

        // 1. Look for Header style: "Company Name : XYZ" or "Company: XYZ"
        val explicitCompanyPattern = Regex("""(?:Company(?:\s+Name)?|Org|Firm)\s*[:\-]\s*([A-Za-z0-9\s&.\-_/]{2,35})""", RegexOption.IGNORE_CASE)
        val explicitMatch = explicitCompanyPattern.find(textWithoutUrls)
        if (explicitMatch != null) {
            val candidate = explicitMatch.groupValues[1].lines().first().trim()
            if (candidate.length in 2..35 && !isGenericWord(candidate)) {
                return candidate
            }
        }

        // 2. Direct match against known campus companies (choose the one appearing earliest in text)
        var earliestCompany: String? = null
        var earliestIndex = Int.MAX_VALUE

        for (company in commonCompanies) {
            val pattern = Regex("""\b${Pattern.quote(company)}\b""", RegexOption.IGNORE_CASE)
            val match = pattern.find(textWithoutUrls)
            if (match != null && match.range.first < earliestIndex) {
                earliestIndex = match.range.first
                earliestCompany = company
            }
        }

        if (earliestCompany != null) {
            return earliestCompany
        }

        // 3. Fallback header patterns
        val headerPatterns = listOf(
            Regex("""^\[([A-Za-z0-9\s&.]{2,25})\]"""),
            Regex("""\b([A-Z][A-Za-z0-9&.\s]{1,20})\s+(?:PPT|OA|Interview|Placement|Talk|Session)\b"""),
            Regex("""\b([A-Z][A-Za-z0-9&.\s]{1,20})\s*[-:]\s*(?:PPT|OA|Interview|Talk|Test)\b""")
        )

        for (pattern in headerPatterns) {
            val match = pattern.find(textWithoutUrls)
            if (match != null) {
                val candidate = match.groupValues[1].trim()
                if (candidate.length in 2..30 && !isGenericWord(candidate)) {
                    return candidate
                }
            }
        }

        return ""
    }

    private fun isGenericWord(word: String): Boolean {
        val generic = listOf("dear", "all", "students", "placement", "internship", "campus", "team", "urgent", "update", "notice", "alert", "reminder", "training", "cdc")
        return generic.contains(word.lowercase(Locale.ENGLISH))
    }

    private fun detectDateString(text: String): String {
        val relativePatterns = listOf(
            Regex("""\b(today|tonight|tomorrow|day after tomorrow|yesterday)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b((?:next\s+|this\s+|coming\s+)?(?:monday|tuesday|wednesday|thursday|friday|saturday|sunday|mon|tue|wed|thu|fri|sat|sun))\b""", RegexOption.IGNORE_CASE),
            Regex("""\b(\d{1,2}(?:st|nd|rd|th)?\s+(?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t|tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)(?:\s+\d{4})?)\b""", RegexOption.IGNORE_CASE),
            Regex("""\b((?:jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|jun(?:e)?|jul(?:y)?|aug(?:ust)?|sep(?:t|tember)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?)\s+\d{1,2}(?:st|nd|rd|th)?(?:,?\s+\d{4})?)\b""", RegexOption.IGNORE_CASE),
            Regex("""(\d{1,2}[/\.-]\d{1,2}(?:[/\.-]\d{2,4})?)"""),
            Regex("""(\d{4}[/\.-]\d{1,2}[/\.-]\d{1,2})""")
        )

        for (pattern in relativePatterns) {
            val match = pattern.find(text)
            if (match != null) {
                val captured = if (match.groupValues.size > 1) match.groupValues[1] else match.groupValues[0]
                return captured.trim()
            }
        }

        return ""
    }

    private fun detectVenue(text: String): String {
        // Direct venue prefix pattern: "PPT venue : RMN 101" or "Venue: CLT"
        val venuePattern = Regex("""(?:PPT\s+venue|Venue|Location|Place|Room)\s*[:\-]\s*([A-Za-z0-9\s,\-\(\)/]{2,35})""", RegexOption.IGNORE_CASE)
        val match = venuePattern.find(text)
        if (match != null) {
            val candidate = match.groupValues[1].lines().first().trim()
            if (candidate.isNotBlank()) return candidate
        }

        for (v in venues) {
            val pattern = Regex("""\b${Pattern.quote(v)}\b""", RegexOption.IGNORE_CASE)
            if (pattern.containsMatchIn(text)) {
                return v
            }
        }

        return ""
    }

    private fun detectMeetingUrl(text: String): String {
        val urlPattern = Regex("""(https?://[^\s<>"'{}|\\^`]+)""")
        val matches = urlPattern.findAll(text)
        for (match in matches) {
            val rawUrl = match.groupValues[1]
            val url = rawUrl.replace(Regex("""[),.;:]+$"""), "")
            if (url.contains("chat.whatsapp.com", ignoreCase = true) ||
                url.contains("meet.google", ignoreCase = true) ||
                url.contains("zoom.us", ignoreCase = true) ||
                url.contains("teams.microsoft", ignoreCase = true) ||
                url.contains("webex", ignoreCase = true) ||
                url.contains("hackerearth", ignoreCase = true) ||
                url.contains("hackerrank", ignoreCase = true) ||
                url.contains("mettl", ignoreCase = true) ||
                url.contains("unstop", ignoreCase = true) ||
                url.contains("superset", ignoreCase = true)
            ) {
                return url
            }
        }
        return matches.firstOrNull()?.groupValues?.get(1)?.replace(Regex("""[),.;:]+$"""), "") ?: ""
    }

    private fun extractDescription(text: String, company: String, eventType: String): String {
        // Clean and structure key placement notes
        val cleanText = text.replace("*", "").trim()
        return cleanText
    }
}
