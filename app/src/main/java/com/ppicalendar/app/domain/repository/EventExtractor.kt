package com.ppicalendar.app.domain.repository

import com.ppicalendar.app.domain.model.ExtractionResult
import java.time.LocalDate

interface EventExtractor {
    suspend fun extractEvent(
        text: String,
        referenceDate: LocalDate = LocalDate.now(),
        apiKey: String = ""
    ): ExtractionResult
}
