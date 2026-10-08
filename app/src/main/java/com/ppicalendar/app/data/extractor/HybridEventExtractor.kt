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
        apiKey: String,
        forcePlacement: Boolean
    ): List<ExtractionResult> {
        // If Gemini API Key is provided, attempt AI extraction
        if (apiKey.isNotBlank()) {
            try {
                Log.d(TAG, "Attempting extraction via Gemini AI...")
                val results = geminiApiExtractor.extract(text, referenceDate, apiKey)
                
                val finalResults = results.map { result -> 
                    if (forcePlacement && result.company.isNotBlank()) {
                        result.copy(isEvent = true)
                    } else {
                        result
                    }
                }
                
                if (finalResults.any { it.isEvent && it.company.isNotBlank() }) {
                    Log.d(TAG, "Gemini AI extraction successful")
                    return finalResults
                }
            } catch (e: Exception) {
                Log.w(TAG, "Gemini AI extraction failed or rate limited, falling back to rule-based engine: ${e.message}")
            }
        }

        // Fallback to offline rule-based & regex heuristics engine
        Log.d(TAG, "Executing offline Rule-Based Extractor...")
        val fallbackResult = ruleBasedExtractor.extract(text, referenceDate, forcePlacement)
        return listOf(fallbackResult)
    }
}
