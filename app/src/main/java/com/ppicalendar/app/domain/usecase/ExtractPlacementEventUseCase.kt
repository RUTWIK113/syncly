package com.ppicalendar.app.domain.usecase

import com.ppicalendar.app.domain.model.ExtractionResult
import com.ppicalendar.app.domain.repository.EventExtractor
import java.time.LocalDate

class ExtractPlacementEventUseCase(
    private val eventExtractor: EventExtractor
) {
    suspend operator fun invoke(
        text: String,
        referenceDate: LocalDate = LocalDate.now(),
        apiKey: String = "",
        forcePlacement: Boolean = false
    ): ExtractionResult {
        return eventExtractor.extractEvent(text, referenceDate, apiKey, forcePlacement)
    }
}
