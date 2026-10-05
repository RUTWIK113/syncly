package com.ppicalendar.app.data.extractor

import com.ppicalendar.app.domain.model.ExtractionResult
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
    ): ExtractionResult = withContext(Dispatchers.IO) {
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
        You are a high-precision placement message analyzer for Indian Institute of Technology Madras (IIT Madras) campus placement notifications.
        
        Analyze the following WhatsApp notification message.
        Reference current date: ${referenceDate} (Day of week: ${referenceDate.dayOfWeek}).
        
        Classify whether the message is a PLACEMENT_EVENT or NOT_PLACEMENT_EVENT.
        
        Supported event types:
        - PPI (Pre-Placement Interview / Offer)
        - PRE_PLACEMENT_TALK (PPT, presentation)
        - ONLINE_ASSESSMENT (OA, coding test, hackerrank, mettl, hackerearth)
        - INTERVIEW (Technical / HR / GD round)
        - COMPANY_SESSION (Webinar, tech talk, workshop)
        - REGISTRATION_DEADLINE (Portal closing, form deadline)
        - RESUME_DEADLINE (Resume submission, shortlist verification)
        - OTHER (Other placement activity)
        
        Rules:
        1. Resolve relative dates like 'tomorrow', 'Monday', 'this Friday', 'today' to ISO YYYY-MM-DD using the reference date: ${referenceDate}.
        2. Normalize start_time and end_time to 24-hour HH:mm format (e.g., '6:00 PM' -> '18:00', '10:30 AM' -> '10:30').
        3. Never invent missing information. If date or time is missing, keep the field as an empty string "".
        4. If essential information is missing, confidence should be lower and fields left empty.
        5. Extract meeting URLs (Google Meet, Zoom, Teams, HackerRank, HackerEarth, etc.) and venue (e.g. CLT, ICSR, SAC, CRC, Virtual, Online).
        6. IMPORTANT: If the event is a REGISTRATION_DEADLINE or RESUME_DEADLINE, the 'date' and 'start_time' MUST be the actual deadline date and time, NOT the date the message was sent.
        7. Classify the event primarily as PRE_PLACEMENT_TALK or ONLINE_ASSESSMENT if the keywords suggest so. Only use the other event types if it strictly does not fit PPT or OA.
        
        Return STRICT JSON matching this exact schema:
        {
          "is_event": true or false,
          "company": "Company Name",
          "event_type": "PPI | PRE_PLACEMENT_TALK | ONLINE_ASSESSMENT | INTERVIEW | COMPANY_SESSION | REGISTRATION_DEADLINE | RESUME_DEADLINE | OTHER",
          "date": "YYYY-MM-DD",
          "start_time": "HH:mm",
          "end_time": "HH:mm",
          "venue": "Venue name or Online",
          "meeting_url": "URL if present",
          "description": "Short summary or notes",
          "incentive_points": integer points if mentioned (e.g. 5, 10) or null,
          "confidence": 0.0 to 1.0
        }
        
        WhatsApp Notification Text:
        ---
        $text
        ---
        """.trimIndent()
    }

    private fun parseGeminiResponse(rawResponseBody: String): ExtractionResult {
        val root = JSONObject(rawResponseBody)
        val candidates = root.optJSONArray("candidates") ?: return ExtractionResult(isEvent = false)
        val firstCandidate = candidates.optJSONObject(0) ?: return ExtractionResult(isEvent = false)
        val content = firstCandidate.optJSONObject("content") ?: return ExtractionResult(isEvent = false)
        val parts = content.optJSONArray("parts") ?: return ExtractionResult(isEvent = false)
        val textPart = parts.optJSONObject(0)?.optString("text") ?: return ExtractionResult(isEvent = false)

        var cleanJson = textPart.trim()
        if (cleanJson.startsWith("```json")) {
            cleanJson = cleanJson.removePrefix("```json").removeSuffix("```").trim()
        } else if (cleanJson.startsWith("```")) {
            cleanJson = cleanJson.removePrefix("```").removeSuffix("```").trim()
        }

        return try {
            json.decodeFromString<ExtractionResult>(cleanJson)
        } catch (e: Exception) {
            // Manual fallback parsing if strict JSON deserialization fails
            val jsonObj = JSONObject(cleanJson)
            val points = if (jsonObj.has("incentive_points") && !jsonObj.isNull("incentive_points")) {
                jsonObj.optInt("incentive_points")
            } else null

            ExtractionResult(
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
}
