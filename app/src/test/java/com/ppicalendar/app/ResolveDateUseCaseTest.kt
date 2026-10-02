package com.ppicalendar.app

import com.ppicalendar.app.domain.usecase.ResolveDateUseCase
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ResolveDateUseCaseTest {

    private val resolveDateUseCase = ResolveDateUseCase()
    private val fixedReferenceDate = LocalDate.of(2026, 10, 24) // Saturday, Oct 24, 2026

    @Test
    fun testRelativeDates_TodayTomorrow() {
        val today = resolveDateUseCase.resolve("today", fixedReferenceDate)
        assertEquals("2026-10-24", today)

        val tomorrow = resolveDateUseCase.resolve("tomorrow", fixedReferenceDate)
        assertEquals("2026-10-25", tomorrow)

        val dayAfterTomorrow = resolveDateUseCase.resolve("day after tomorrow", fixedReferenceDate)
        assertEquals("2026-10-26", dayAfterTomorrow)
    }

    @Test
    fun testDaysOfWeek() {
        // Monday after Saturday Oct 24 is Oct 26
        val monday = resolveDateUseCase.resolve("Monday", fixedReferenceDate)
        assertEquals("2026-10-26", monday)

        // Tuesday is Oct 27
        val tuesday = resolveDateUseCase.resolve("this Tuesday", fixedReferenceDate)
        assertEquals("2026-10-27", tuesday)

        // Friday is Oct 30
        val friday = resolveDateUseCase.resolve("Friday", fixedReferenceDate)
        assertEquals("2026-10-30", friday)
    }

    @Test
    fun testExplicitDates() {
        val date1 = resolveDateUseCase.resolve("28th October", fixedReferenceDate)
        assertEquals("2026-10-28", date1)

        val date2 = resolveDateUseCase.resolve("Nov 5, 2026", fixedReferenceDate)
        assertEquals("2026-11-05", date2)

        val date3 = resolveDateUseCase.resolve("15/11/2026", fixedReferenceDate)
        assertEquals("2026-11-15", date3)
    }
}
