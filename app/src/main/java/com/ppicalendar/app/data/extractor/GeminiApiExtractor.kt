package com.ppicalendar.app.data.extractor

import com.ppicalendar.app.domain.model.ExtractionResult
import com.ppicalendar.app.domain.model.ExtractionResponse
import com.ppicalendar.app.domain.model.PlacementEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class GeminiApiExtractor(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true }
) {

    suspend fun extract(
        text: String,
        referenceDate: LocalDate,
        apiKey: String
    ): List<ExtractionResult> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("Gemini API key is empty")
        }

        val prompt = buildPrompt(text, referenceDate)
        val requestBodyJson = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply { put("text", prompt) })
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)

            val generationConfig = JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.1)
            }
            put("generationConfig", generationConfig)
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
            .post(requestBodyJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IllegalStateException("Empty response from Gemini API")

        if (!response.isSuccessful) {
            throw IllegalStateException("Gemini API error (HTTP ${response.code}): $responseBody")
        }

        parseGeminiResponse(responseBody)
    }

    private fun buildPrompt(text: String, referenceDate: LocalDate): String {
        return """
        You are a high-precision placement and career message analyzer.
        
        Reference current date: ${referenceDate} (Day of week: ${referenceDate.dayOfWeek}).
        
        CRITICAL RULES:
        1. Extract EVERY distinct event from the message as a SEPARATE object. A single message often contains MULTIPLE events (e.g. PPT, OA, Interview, Deadline).
        2. Common multi-event patterns you MUST split:
           - "PPT: 6PM, 9th Oct" + "Test Date: 6:45PM, 9th Oct" = TWO events (PRE_PLACEMENT_TALK + ONLINE_ASSESSMENT)
           - "GForm Deadline: 7th Oct EOD" = ONE event (REGISTRATION with deadline_date="YYYY-10-07", deadline_time="23:00")
        3. ALWAYS set is_event = true for any event that relates to campus placements, recruitment, tests, talks, or deadlines.
        4. Extract the company name (e.g. "Squarepoint Capital", "Google", "Barclays"). If no company is mentioned, use sender or empty string.
        5. Extract the event title (e.g. role title "Software Developer", "Desk Quant Analyst", or session topic). If no specific title is identifiable, set title to exactly "-------" (seven hyphens). NEVER save empty title.
        6. Distinguish event date/time from registration/resume deadline:
           - Event start time: HH:mm (24-hour format). If missing, leave as empty string "". NEVER assign 23:00 to event start times!
           - If a registration or submission deadline is explicitly specified, put it in deadline_date (YYYY-MM-DD). If deadline date is present but deadline time is omitted, default deadline_time to "23:00".
        7. Normalize times to 24-hour HH:mm: "7pm" = "19:00", "6:45PM" = "18:45", "6PM - 6:45PM" = start "18:00" end "18:45".
        8. For venues, capture campus lecture halls (CRC, RJN, RMN, MSB, ESB, CLT, SAC, etc.) or "Online".
        9. Attach links intelligently: registration form links go to REGISTRATION, test links go to ONLINE_ASSESSMENT, WhatsApp group links to meeting_url.
        10. Date format DD/MM/YYYY (Indian format): "07/10/2026" means day=07, month=10 (October), year=2026 -> "2026-10-07".
        
        Supported event types: PRE_PLACEMENT_TALK, ONLINE_ASSESSMENT, RESUME_SUBMISSION, REGISTRATION, INTERVIEW, GROUP_DISCUSSION, SHORTLIST_ANNOUNCEMENT, COMPANY_BRIEFING, GENERAL_EVENT, PPI, OTHER
        
        Return STRICT JSON:
        {
          "events": [
            {
              "is_event": true,
              "company": "Company Name",
              "title": "Role / Event Title or -------",
              "event_type": "PRE_PLACEMENT_TALK",
              "date": "YYYY-MM-DD",
              "start_time": "HH:mm",
              "end_time": "HH:mm",
              "deadline_date": "YYYY-MM-DD",
              "deadline_time": "HH:mm",
              "venue": "Venue or Online",
              "meeting_url": "URL if present",
              "description": "Short summary",
              "incentive_points": null,
              "confidence": 0.95
            }
          ]
        }
        
        Notification / Message Text:
        ---
        $text
        ---
        """.trimIndent()
    }

    private fun parseGeminiResponse(rawResponseBody: String): List<ExtractionResult> {
        val root = JSONObject(rawResponseBody)
        val candidates = root.optJSONArray("candidates") ?: return emptyList()
        val firstCandidate = candidates.optJSONObject(0) ?: return emptyList()
        val content = firstCandidate.optJSONObject("content") ?: return emptyList()
        val parts = content.optJSONArray("parts") ?: return emptyList()
        val textPart = parts.optJSONObject(0)?.optString("text") ?: return emptyList()

        var cleanJson = textPart.trim()
        val jsonPattern = Regex("(?s)```json\\s*(.*?)\\s*```")
        val matchResult = jsonPattern.find(cleanJson)
        if (matchResult != null) {
            cleanJson = matchResult.groupValues[1]
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.removePrefix("```").removeSuffix("```").trim()
        }

        return try {
            val response = json.decodeFromString<ExtractionResponse>(cleanJson)
            response.events.map { normalizeResult(it) }
        } catch (e: Exception) {
            try {
                // Try single object format
                val result = json.decodeFromString<ExtractionResult>(cleanJson)
                listOf(normalizeResult(result))
            } catch (e2: Exception) {
                // Manual fallback parsing if strict JSON deserialization fails
                val jsonObj = JSONObject(cleanJson)
                if (jsonObj.has("events")) {
                    val eventsArray = jsonObj.optJSONArray("events")
                    val list = mutableListOf<ExtractionResult>()
                    if (eventsArray != null) {
                        for (i in 0 until eventsArray.length()) {
                            val eventObj = eventsArray.optJSONObject(i) ?: continue
                            list.add(normalizeResult(parseSingleEvent(eventObj)))
                        }
                    }
                    list
                } else {
                    listOf(normalizeResult(parseSingleEvent(jsonObj)))
                }
            }
        }
    }
    
    private fun parseSingleEvent(jsonObj: JSONObject): ExtractionResult {
        val points = if (jsonObj.has("incentive_points") && !jsonObj.isNull("incentive_points")) {
            jsonObj.optInt("incentive_points")
        } else null

        val rawTitle = jsonObj.optString("title", "").trim()
        val title = if (rawTitle.isBlank()) PlacementEvent.DEFAULT_TITLE_FALLBACK else rawTitle

        val deadlineDate = jsonObj.optString("deadline_date", "").trim()
        val rawDeadlineTime = jsonObj.optString("deadline_time", "").trim()
        val deadlineTime = if (deadlineDate.isNotBlank() && rawDeadlineTime.isBlank()) {
            PlacementEvent.DEFAULT_DEADLINE_TIME
        } else {
            rawDeadlineTime
        }

        return ExtractionResult(
            isEvent = jsonObj.optBoolean("is_event", false),
            company = jsonObj.optString("company", ""),
            title = title,
            eventType = jsonObj.optString("event_type", "OTHER"),
            date = jsonObj.optString("date", ""),
            startTime = jsonObj.optString("start_time", ""),
            endTime = jsonObj.optString("end_time", ""),
            deadlineDate = deadlineDate,
            deadlineTime = deadlineTime,
            venue = jsonObj.optString("venue", ""),
            meetingUrl = jsonObj.optString("meeting_url", ""),
            description = jsonObj.optString("description", ""),
            incentivePoints = points,
            confidence = jsonObj.optDouble("confidence", 0.5)
        )
    }

    private fun normalizeResult(res: ExtractionResult): ExtractionResult {
        val title = if (res.title.isBlank()) PlacementEvent.DEFAULT_TITLE_FALLBACK else res.title.trim()
        val deadlineTime = if (res.deadlineDate.isNotBlank() && res.deadlineTime.isBlank()) {
            PlacementEvent.DEFAULT_DEADLINE_TIME
        } else {
            res.deadlineTime
        }
        return res.copy(title = title, deadlineTime = deadlineTime)
    }
}
