package com.ppicalendar.app.data.extractor

import com.ppicalendar.app.domain.model.EventType
import com.ppicalendar.app.domain.model.ExtractionResult
import com.ppicalendar.app.domain.model.PlacementEvent
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
        "Google", "Microsoft", "Squarepoint Capital", "Squarepoint", "Jane Street", "Apple", "Amazon", "Uber", "Goldman Sachs",
        "Morgan Stanley", "DE Shaw", "D.E. Shaw", "Tower Research", "Graviton", "Optiver",
        "Citadel", "Jump Trading", "Qualcomm", "Texas Instruments", "TI", "Intel", "Nvidia",
        "Samsung", "Adobe", "Oracle", "Cisco", "Salesforce", "Sprinklr", "Stripe", "McKinsey",
        "Boston Consulting Group", "BCG", "Bain & Company", "Bain", "Kearney", "ITC",
        "Hindustan Unilever", "HUL", "P&G", "Procter & Gamble", "Schlumberger", "SLB", "Shell",
        "ExxonMobil", "Airbus", "Boeing", "ISRO", "Tata Steel", "Tata Motors", "L&T",
        "Reliance", "Flipkart", "Swiggy", "Zomato", "Meesho", "CRED", "Razorpay", "PhonePe",
        "Paytm", "Groww", "Zerodha", "Rubrik", "Cohesity", "Databricks", "Snowflake", "Palantir",
        "Honda R&D", "Honda", "Hyundai", "Mercedes-Benz", "BMW", "Toyota", "Bajaj", "TVS", "Barclays"
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
        return extractAll(text, referenceDate, forcePlacement).firstOrNull() ?: ExtractionResult(isEvent = false)
    }

    fun extractAll(
        text: String,
        referenceDate: LocalDate = LocalDate.now(),
        forcePlacement: Boolean = false
    ): List<ExtractionResult> {
        if (text.isBlank()) {
            return emptyList()
        }

        // 1. One company per message
        val companyRegex = Regex(
            """(?m)^\*?(?:Company(?:\s+Name)?|Firm|Org)\s*[:\-]\s*\*?(.*?)\*?$""",
            RegexOption.IGNORE_CASE
        )
        val matchedCompany = companyRegex.find(text)?.groupValues?.get(1)?.replace("*", "")?.trim()
        val detectedComp = if (!matchedCompany.isNullOrBlank() && !isGenericWord(matchedCompany)) {
            matchedCompany
        } else {
            val d = detectCompany(text)
            if (d.isNotBlank()) d else PlacementEvent.DEFAULT_COMPANY_FALLBACK
        }

        val normalizedCompany = PlacementEvent.normalizeCompany(detectedComp)
        val globalVenue = detectVenue(text)
        val globalMeetingUrl = detectMeetingUrl(text)
        val incentivePoints = detectIncentivePoints(text)

        val events = mutableListOf<ExtractionResult>()
        val processedEventKeys = mutableSetOf<String>()

        var workingText = text

        // 2. Parse the message up to 4 times (for safe side)
        // In every iteration, when the first date/time (event) is found:
        // parse and remove this date, time, and keyword from workingText!
        // Repeat on remaining text. When nothing is found, just pass/break.
        for (iter in 1..4) {
            val candidates = findCandidates(workingText, referenceDate, globalVenue, globalMeetingUrl)
            val candidate = candidates.firstOrNull { cand ->
                val key = "${cand.eventType.name}_${cand.date}"
                !processedEventKeys.contains(key)
            }

            if (candidate == null) {
                // When nothing found, just pass / break
                break
            }

            val eventKey = "${candidate.eventType.name}_${candidate.date}"
            processedEventKeys.add(eventKey)

            events.add(
                ExtractionResult(
                    isEvent = true,
                    company = normalizedCompany,
                    title = "",
                    eventType = candidate.eventType.name,
                    date = candidate.date,
                    startTime = candidate.startTime,
                    endTime = candidate.endTime,
                    deadlineDate = if (candidate.isDeadline) candidate.date else "",
                    deadlineTime = if (candidate.isDeadline) candidate.startTime else "",
                    venue = candidate.venue,
                    meetingUrl = candidate.meetingUrl,
                    description = candidate.description,
                    incentivePoints = incentivePoints,
                    confidence = 0.95
                )
            )

            // Remove this date, time, and keyword from workingText
            workingText = removeEventFromText(workingText, candidate)
        }

        // 3. Fallback: If no structured line events matched, extract single event
        if (events.isEmpty()) {
            val single = extractSingle(text, referenceDate, forcePlacement, normalizedCompany)
            if (single.isEvent) {
                events.add(single)
            }
        }

        return events
    }

    private data class EventCandidate(
        val eventType: EventType,
        val isDeadline: Boolean,
        val date: String,
        val startTime: String,
        val endTime: String,
        val venue: String,
        val meetingUrl: String,
        val description: String,
        val startIndex: Int,
        val range: IntRange,
        val matchedKeyword: String,
        val matchedDateStr: String,
        val matchedTimeStr: String
    )

    private fun findCandidates(
        workingText: String,
        referenceDate: LocalDate,
        globalVenue: String,
        globalMeetingUrl: String
    ): List<EventCandidate> {
        val candidates = mutableListOf<EventCandidate>()
        val coveredRanges = mutableListOf<IntRange>()

        // 1. Line-based pattern: e.g. "PPT : 10th Oct (6:00 PM)" or "Resume Deadline : 8th Oct EOD"
        val linePattern = Regex("""(?m)^[*\s\-•]*([A-Za-z0-9\s/&.\-_()]+?)\s*[:\-]\s*(.*?)$""")
        for (match in linePattern.findAll(workingText)) {
            val rawLabel = match.groupValues[1].replace("*", "").trim()
            val rest = match.groupValues[2].replace("*", "").trim()
            if (rest.isBlank()) continue

            val eventType = matchEventTypeFromLabel(rawLabel, rest) ?: continue

            val isDeadline = eventType == EventType.RESUME_SUBMISSION ||
                    eventType == EventType.REGISTRATION ||
                    rawLabel.contains("deadline", ignoreCase = true) ||
                    rest.contains("deadline", ignoreCase = true) ||
                    rest.contains("EOD", ignoreCase = true) ||
                    rest.contains("end of day", ignoreCase = true) ||
                    rawLabel.contains("last date", ignoreCase = true) ||
                    rest.contains("last date", ignoreCase = true)

            val (startTimeRaw, endTimeRaw) = DateTimeParser.extractTimeRange(rest)
            val startTime = if (startTimeRaw.isNotBlank()) {
                startTimeRaw
            } else if (isDeadline) {
                PlacementEvent.DEFAULT_DEADLINE_TIME
            } else {
                ""
            }

            val rawDate = detectDateString(rest).ifBlank { detectDateString(workingText) }
            val resolvedDate = if (rawDate.isNotBlank()) resolveDateUseCase.resolve(rawDate, referenceDate) else ""

            if (resolvedDate.isBlank() && startTime.isBlank()) continue

            candidates.add(
                EventCandidate(
                    eventType = eventType,
                    isDeadline = isDeadline,
                    date = resolvedDate,
                    startTime = startTime,
                    endTime = endTimeRaw,
                    venue = detectVenue(rest).ifBlank { globalVenue },
                    meetingUrl = detectMeetingUrl(rest).ifBlank { globalMeetingUrl },
                    description = rest,
                    startIndex = match.range.first,
                    range = match.range,
                    matchedKeyword = rawLabel,
                    matchedDateStr = rawDate,
                    matchedTimeStr = startTimeRaw
                )
            )
            coveredRanges.add(match.range)
        }

        // 2. Phrase-based / Free-form pattern: e.g. "Pre-Placement Talk tomorrow at 6:00 PM"
        val phraseKeywords = listOf(
            Pair(Regex("""\b(pre[\s\-]?placement\s+(?:talk|session)|ppt|presentation)\b""", RegexOption.IGNORE_CASE), EventType.PRE_PLACEMENT_TALK),
            Pair(Regex("""\b(online\s+assessment|coding\s+(?:test|round)|technical\s+assessment|oa)\b""", RegexOption.IGNORE_CASE), EventType.ONLINE_ASSESSMENT),
            Pair(Regex("""\b(resume[\s\-]?deadline|cv[\s\-]?deadline|resume\s+submission|cv\s+submission|submit\s+resume|submit\s+cv|upload\s+resume)\b""", RegexOption.IGNORE_CASE), EventType.RESUME_SUBMISSION),
            Pair(Regex("""\b(registration[\s\-]?deadline|reg[\s\-]?deadline|gform[\s\-]?deadline|register\s+by|registration\s+closes|last\s+date\s+to\s+register|last\s+date\s+to\s+apply)\b""", RegexOption.IGNORE_CASE), EventType.REGISTRATION),
            Pair(Regex("""\b(interview|tech[\s\-]?interview|hr[\s\-]?interview)\b""", RegexOption.IGNORE_CASE), EventType.INTERVIEW),
            Pair(Regex("""\b(gd|group[\s\-]?discussion)\b""", RegexOption.IGNORE_CASE), EventType.GROUP_DISCUSSION),
            Pair(Regex("""\b(deadline|last\s+date)\b""", RegexOption.IGNORE_CASE), EventType.REGISTRATION)
        )

        for ((regex, type) in phraseKeywords) {
            for (match in regex.findAll(workingText)) {
                // If this match is already covered by a line-based candidate, skip
                if (coveredRanges.any { it.first <= match.range.first && match.range.last <= it.last }) continue

                // Find enclosing clause/sentence (bounded by newlines or sentence-ending punctuation)
                var lineStart = 0
                val prevBoundRegex = Regex("""(?:\.|\;|\n)\s+""")
                for (bm in prevBoundRegex.findAll(workingText.substring(0, match.range.first))) {
                    lineStart = bm.range.last + 1
                }

                val nextBoundRegex = Regex("""(?:\.|\;|\n)(?=\s+[A-Z]|\s*$)""")
                val nextMatch = nextBoundRegex.find(workingText, match.range.last)
                val lineEnd = nextMatch?.range?.first ?: workingText.length
                val clause = workingText.substring(lineStart, lineEnd).trim()

                val lowerClause = clause.lowercase(Locale.ENGLISH)
                if (lowerClause.startsWith("company") || lowerClause.contains("whatsapp group")) continue

                val isDeadline = type == EventType.RESUME_SUBMISSION ||
                        type == EventType.REGISTRATION ||
                        clause.contains("deadline", ignoreCase = true) ||
                        clause.contains("EOD", ignoreCase = true) ||
                        clause.contains("end of day", ignoreCase = true) ||
                        clause.contains("last date", ignoreCase = true)

                val (startTimeRaw, endTimeRaw) = DateTimeParser.extractTimeRange(clause)
                val startTime = if (startTimeRaw.isNotBlank()) {
                    startTimeRaw
                } else if (isDeadline) {
                    PlacementEvent.DEFAULT_DEADLINE_TIME
                } else {
                    ""
                }

                val rawDate = detectDateString(clause)
                // A phrase candidate must either contain its own date, its own time, or be an explicit deadline
                if (rawDate.isBlank() && startTimeRaw.isBlank() && !isDeadline) continue

                val resolvedDate = if (rawDate.isNotBlank()) {
                    resolveDateUseCase.resolve(rawDate, referenceDate)
                } else if (startTimeRaw.isNotBlank() || isDeadline) {
                    val fallbackDate = detectDateString(workingText)
                    if (fallbackDate.isNotBlank()) resolveDateUseCase.resolve(fallbackDate, referenceDate) else ""
                } else {
                    ""
                }

                if (resolvedDate.isBlank() && startTime.isBlank()) continue

                val candidateRange = lineStart until lineEnd
                candidates.add(
                    EventCandidate(
                        eventType = type,
                        isDeadline = isDeadline,
                        date = resolvedDate,
                        startTime = startTime,
                        endTime = endTimeRaw,
                        venue = detectVenue(clause).ifBlank { globalVenue },
                        meetingUrl = detectMeetingUrl(clause).ifBlank { globalMeetingUrl },
                        description = clause,
                        startIndex = match.range.first,
                        range = candidateRange,
                        matchedKeyword = match.value,
                        matchedDateStr = rawDate,
                        matchedTimeStr = startTimeRaw
                    )
                )
                coveredRanges.add(candidateRange)
            }
        }

        return candidates.sortedBy { it.startIndex }
    }

    private fun removeEventFromText(text: String, candidate: EventCandidate): String {
        val chars = text.toCharArray()

        val start = candidate.range.first.coerceIn(0, chars.size)
        val end = minOf(candidate.range.last + 1, chars.size)
        for (i in start until end) {
            if (chars[i] != '\n' && chars[i] != '\r') {
                chars[i] = ' '
            }
        }

        var result = String(chars)

        if (candidate.matchedKeyword.isNotBlank() && candidate.matchedKeyword.length > 1) {
            val kwPattern = Regex("""\b${Pattern.quote(candidate.matchedKeyword)}\b""", RegexOption.IGNORE_CASE)
            result = result.replace(kwPattern, " ".repeat(candidate.matchedKeyword.length))
        }

        if (candidate.matchedDateStr.isNotBlank()) {
            val datePattern = Regex("""\b${Pattern.quote(candidate.matchedDateStr)}\b""", RegexOption.IGNORE_CASE)
            result = result.replace(datePattern, " ".repeat(candidate.matchedDateStr.length))
        }

        if (candidate.matchedTimeStr.isNotBlank()) {
            val timePattern = Regex("""\b${Pattern.quote(candidate.matchedTimeStr)}\b""", RegexOption.IGNORE_CASE)
            result = result.replace(timePattern, " ".repeat(candidate.matchedTimeStr.length))
        }

        return result
    }

    private fun extractSingle(
        text: String,
        referenceDate: LocalDate,
        forcePlacement: Boolean,
        detectedCompany: String
    ): ExtractionResult {
        val eventType = detectEventType(text)
        val company = if (detectedCompany != PlacementEvent.DEFAULT_COMPANY_FALLBACK && detectedCompany.isNotBlank()) {
            detectedCompany
        } else {
            detectCompany(text)
        }
        val (startTime, endTime) = DateTimeParser.extractTimeRange(text)

        val rawDate = detectDateString(text)
        val resolvedDate = resolveDateUseCase.resolve(rawDate, referenceDate)
        val venue = detectVenue(text)
        val meetingUrl = detectMeetingUrl(text)
        val description = extractDescription(text, company, eventType.displayName)
        val incentivePoints = detectIncentivePoints(text)

        // Deadline extraction
        val (rawDeadlineDate, deadlineTimeExplicit) = detectDeadline(text)
        val deadlineDate = if (rawDeadlineDate.isNotBlank()) resolveDateUseCase.resolve(rawDeadlineDate, referenceDate) else ""
        val deadlineTime = if (deadlineDate.isNotBlank()) {
            if (deadlineTimeExplicit.isNotBlank()) deadlineTimeExplicit else PlacementEvent.DEFAULT_DEADLINE_TIME
        } else ""

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
            if (company.isNotBlank() && company != PlacementEvent.DEFAULT_COMPANY_FALLBACK) confidenceScore += 0.2
            if (resolvedDate.isNotBlank()) confidenceScore += 0.25
            if (startTime.isNotBlank()) confidenceScore += 0.25
        }

        return ExtractionResult(
            isEvent = isPlacementEvent,
            company = PlacementEvent.normalizeCompany(company),
            title = "",
            eventType = eventType.name,
            date = resolvedDate,
            startTime = startTime,
            endTime = endTime,
            deadlineDate = deadlineDate,
            deadlineTime = deadlineTime,
            venue = venue,
            meetingUrl = meetingUrl,
            description = description,
            incentivePoints = incentivePoints,
            confidence = (confidenceScore * 100).toInt() / 100.0
        )
    }

    private fun matchEventTypeFromLabel(label: String, rest: String): EventType? {
        val clean = label.trim().lowercase(Locale.ENGLISH)
        if (clean.contains("venue") || clean.contains("location") || clean.contains("place") ||
            clean.contains("profile") || clean.contains("role") || clean.contains("eligib") ||
            clean.contains("ctc") || clean.contains("stipend") || clean.contains("salary") ||
            clean.contains("whatsapp") || clean.contains("link") || clean.contains("note") ||
            clean.contains("info") || clean.startsWith("company") || clean.startsWith("firm") ||
            clean.startsWith("org")
        ) {
            return null
        }

        return when {
            clean.contains("resume") || clean.contains("cv") -> EventType.RESUME_SUBMISSION
            clean.contains("gform") || clean.contains("registration") || clean.contains("register") ||
                    clean.contains("reg deadline") || clean.contains("apply") -> EventType.REGISTRATION
            clean.contains("deadline") || clean.contains("last date") -> {
                if (rest.contains("resume", ignoreCase = true) || rest.contains("cv", ignoreCase = true)) {
                    EventType.RESUME_SUBMISSION
                } else {
                    EventType.REGISTRATION
                }
            }
            clean.contains("ppt") || clean.contains("pre-placement") || clean.contains("pre placement") ||
                    clean.contains("talk") || clean == "presentation" || clean == "session" -> EventType.PRE_PLACEMENT_TALK
            clean == "oa" || clean.contains("online assessment") || clean.contains("coding") ||
                    clean.contains("assessment") || clean == "test" || clean.contains("test") -> EventType.ONLINE_ASSESSMENT
            clean.contains("interview") -> EventType.INTERVIEW
            clean == "gd" || clean.contains("group discussion") -> EventType.GROUP_DISCUSSION
            clean == "ppi" -> EventType.PPI
            else -> null
        }
    }

    private fun detectDeadline(text: String): Pair<String, String> {
        val deadlinePattern = Regex("""(?:Deadline|Last\s+date|Register\s+by|Registration\s+closes?|GForm\s+Deadline)\s*[:\-]?\s*([A-Za-z0-9\s,/\.\-]{3,30})""", RegexOption.IGNORE_CASE)
        val match = deadlinePattern.find(text) ?: return Pair("", "")
        val rawSnippet = match.groupValues[1]

        val rawDate = detectDateString(rawSnippet)
        val (startTime, _) = DateTimeParser.extractTimeRange(rawSnippet)
        val deadlineTime = when {
            startTime.isNotBlank() -> startTime
            else -> PlacementEvent.DEFAULT_DEADLINE_TIME // "23:00"
        }
        return Pair(rawDate, deadlineTime)
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

            Regex("""\b(interview|tech[\s\-]?interview|hr[\s\-]?interview|interview\s+slot)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.INTERVIEW

            Regex("""\b(gd|group[\s\-]?discussion)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.GROUP_DISCUSSION

            Regex("""\b(shortlist|shortlisted|selected\s+candidates|eligible\s+candidates)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.SHORTLIST_ANNOUNCEMENT

            Regex("""\b(company[\s\-]?briefing|job[\s\-]?description|role[\s\-]?details|compensation[\s\-]?details)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.COMPANY_BRIEFING

            Regex("""\b(registration[\s\-]?deadline|form[\s\-]?deadline|last[\s\-]?date[\s\-]?to[\s\-]?register|portal[\s\-]?closes?|register|registration)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.REGISTRATION

            Regex("""\b(resume[\s\-]?deadline|cv[\s\-]?submission|upload\s+resume|submit\s+cv)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.RESUME_SUBMISSION

            Regex("""\b(placement\s+drive|recruitment|career\s+event|assessment)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text) ->
                EventType.GENERAL_EVENT

            else -> EventType.OTHER
        }
    }

    private fun detectCompany(text: String): String {
        // Strip URLs to avoid false positive matching against company domains in links
        val textWithoutUrls = text.replace(Regex("""https?://[^\s<>"'{}|\\^`]+"""), "")

        // 1. Look for Header style: "Company Name : XYZ" or "Company: XYZ" or "*Company: XYZ*"
        val explicitCompanyPattern = Regex("""(?:Company(?:\s+Name)?|Org|Firm)\s*[:\-]\s*\*?([A-Za-z0-9\s&.\-_/]{2,35})\*?""", RegexOption.IGNORE_CASE)
        val explicitMatch = explicitCompanyPattern.find(textWithoutUrls)
        if (explicitMatch != null) {
            val candidate = explicitMatch.groupValues[1].lines().first().replace("*", "").trim()
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
                val candidate = match.groupValues[1].replace("*", "").trim()
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
        return text.replace("*", "").trim()
    }
}
