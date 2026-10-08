package com.ppicalendar.app.data.extractor

import com.ppicalendar.app.domain.model.ExtractionResult
import com.ppicalendar.app.domain.model.ExtractionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
            throw IllegalStateException("Gemini API error (HTTP $response.code): $responseBody")
        }

        parseGeminiResponse(responseBody)
    }

    private fun buildPrompt(text: String, referenceDate: LocalDate): String {
        return """
        You are a high-precision placement message analyzer for IIT Madras campus placement notifications.
        
        Reference current date: ${referenceDate} (Day of week: ${referenceDate.dayOfWeek}).
        
        CRITICAL RULES:
        1. Extract EVERY distinct event from the message as a SEPARATE object. A single message often contains MULTIPLE events.
        2. Common multi-event patterns you MUST split into separate events:
           - "PPT: 6PM, 9th Oct" + "Test Date: 6:45PM, 9th Oct" = TWO events (PRE_PLACEMENT_TALK + ONLINE_ASSESSMENT)
           - "GForm Deadline: 7th Oct EOD" = ONE event (REGISTRATION_DEADLINE, start_time = "23:00")
           - "Registration link" with a deadline = ONE event (REGISTRATION_DEADLINE) with the link as meeting_url
           - "PPT time: 7pm, 07/10/2026" = ONE event (PRE_PLACEMENT_TALK)
        3. ALWAYS set is_event = true for any event that has a company name and relates to placements/recruitment.
        4. Extract the company name from patterns like "Company Name : X", "Company: X", or "*Company: X*".
        5. Resolve dates: "07/10/2026" = "2026-10-07", "7th October" = "${referenceDate.year}-10-07", "EOD" = start_time "23:00".
        6. Normalize times to 24-hour HH:mm: "7pm" = "19:00", "6:45PM" = "18:45", "6PM - 6:45PM" = start "18:00" end "18:45".
        7. For venues, use campus abbreviations (CRC, RJN, RMN, MSB, ESB, CLT, SAC) if mentioned. Include room numbers (e.g. "RMN 101"). Default to "Online" if no physical venue.
        8. Attach links intelligently: registration/gform links go to REGISTRATION_DEADLINE events, WhatsApp group links go to the main event, test platform links go to OA events.
        9. Never invent information. If date/time is missing, leave as empty string "".
        10. For date format DD/MM/YYYY (Indian format): "07/10/2026" means day=07, month=10(October), year=2026 -> "2026-10-07".
        
        Supported event types: PPI, PRE_PLACEMENT_TALK, ONLINE_ASSESSMENT, INTERVIEW, COMPANY_SESSION, REGISTRATION_DEADLINE, RESUME_DEADLINE, OTHER
        
        Return STRICT JSON:
        {
          "events": [
            {
              "is_event": true,
              "company": "Company Name",
              "event_type": "PRE_PLACEMENT_TALK",
              "date": "YYYY-MM-DD",
              "start_time": "HH:mm",
              "end_time": "HH:mm",
              "venue": "Venue or Online",
              "meeting_url": "URL if present",
              "description": "Short summary",
              "incentive_points": null,
              "confidence": 0.95
            }
          ]
        }
        
        WhatsApp Notification Text:
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
            response.events
        } catch (e: Exception) {
            try {
                // Try old format (single object)
                val result = json.decodeFromString<ExtractionResult>(cleanJson)
                listOf(result)
            } catch (e2: Exception) {
                // Manual fallback parsing if strict JSON deserialization fails
                val jsonObj = JSONObject(cleanJson)
                if (jsonObj.has("events")) {
                    val eventsArray = jsonObj.optJSONArray("events")
                    val list = mutableListOf<ExtractionResult>()
                    if (eventsArray != null) {
                        for (i in 0 until eventsArray.length()) {
                            val eventObj = eventsArray.optJSONObject(i) ?: continue
                            list.add(parseSingleEvent(eventObj))
                        }
                    }
                    list
                } else {
                    listOf(parseSingleEvent(jsonObj))
                }
            }
        }
    }
    
    private fun parseSingleEvent(jsonObj: JSONObject): ExtractionResult {
        val points = if (jsonObj.has("incentive_points") && !jsonObj.isNull("incentive_points")) {
            jsonObj.optInt("incentive_points")
        } else null

        return ExtractionResult(
            isEvent = jsonObj.optBoolean("is_event", false),
            company = jsonObj.optString("company", ""),
            eventType = jsonObj.optString("event_type", "OTHER"),
            date = jsonObj.optString("date", ""),
            startTime = jsonObj.optString("start_time", ""),
            endTime = jsonObj.optString("end_time", ""),
            venue = jsonObj.optString("venue", ""),
            meetingUrl = jsonObj.optString("meeting_url", ""),
            description = jsonObj.optString("description", ""),
            incentivePoints = points,
            confidence = jsonObj.optDouble("confidence", 0.5)
        )
    }
}
