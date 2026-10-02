package com.ppicalendar.app.data.extractor

import android.util.Log
import com.ppicalendar.app.domain.model.ExtractionResult
import com.ppicalendar.app.domain.repository.EventExtractor
import java.time.LocalDate

class HybridEventExtractor(
    private val ruleBasedExtractor: RuleBasedExtractor = RuleBasedExtractor(),
    private val geminiApiExtractor: GeminiApiExtractor = GeminiApiExtractor()
) : EventExtractor {

    companion object {
        private const val TAG = "HybridEventExtractor"
    }

    override suspend fun extractEvent(
        text: String,
        referenceDate: LocalDate,
        apiKey: String
    ): ExtractionResult {
        // If Gemini API Key is provided, attempt AI extraction
        if (apiKey.isNotBlank()) {
            try {
                Log.d(TAG, "Attempting extraction via Gemini AI...")
                val result = geminiApiExtractor.extract(text, referenceDate, apiKey)
                if (result.isEvent && result.company.isNotBlank()) {
                    Log.d(TAG, "Gemini AI extraction successful: ${result.company} - ${result.eventType}")
                    return result
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini AI extraction failed or rate limited, falling back to rule-based engine: ${e.message}")
            }
        }

        // Fallback to offline rule-based & regex heuristics engine
        Log.d(TAG, "Executing offline Rule-Based Extractor...")
        return ruleBasedExtractor.extract(text, referenceDate)
    }
}
