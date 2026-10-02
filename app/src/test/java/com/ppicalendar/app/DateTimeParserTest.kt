package com.ppicalendar.app

import com.ppicalendar.app.data.extractor.DateTimeParser
import org.junit.Assert.assertEquals
import org.junit.Test

class DateTimeParserTest {

    @Test
    fun testTimeRangeExtraction() {
        val (start1, end1) = DateTimeParser.extractTimeRange("Pre-placement talk from 6:00 PM - 7:30 PM in CLT")
        assertEquals("18:00", start1)
        assertEquals("19:30", end1)

        val (start2, end2) = DateTimeParser.extractTimeRange("Test session 10:00 AM to 12:00 PM")
        assertEquals("10:00", start2)
        assertEquals("12:00", end2)

        val (start3, end3) = DateTimeParser.extractTimeRange("Session from 6 to 8 PM")
        assertEquals("18:00", start3)
        assertEquals("20:00", end3)
    }

    @Test
    fun testSingleTimeExtraction() {
        val (start1, end1) = DateTimeParser.extractTimeRange("Talk starts at 5:00 PM today")
        assertEquals("17:00", start1)
        assertEquals("", end1)

        val (start2, end2) = DateTimeParser.extractTimeRange("Online interview at 10:30 AM")
        assertEquals("10:30", start2)
        assertEquals("", end2)
    }
}
