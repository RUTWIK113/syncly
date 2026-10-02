package com.ppicalendar.app

import com.ppicalendar.app.data.extractor.RuleBasedExtractor
import com.ppicalendar.app.domain.model.EventType
import com.ppicalendar.app.domain.usecase.ResolveDateUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class RuleBasedExtractorTest {

    private val extractor = RuleBasedExtractor(ResolveDateUseCase())
    private val refDate = LocalDate.of(2026, 10, 24)

    @Test
    fun testJaneStreetExtraction() {
        val text = "Jane Street PPT & Coding OA: Jane Street is hosting a Pre-Placement Talk tomorrow at 6:00 PM - 7:30 PM in CLT. Followed by OA on HackerRank. Meeting link: https://meet.google.com/abc-defg-hij"
        val result = extractor.extract(text, refDate)

        assertTrue(result.isEvent)
        assertEquals("Jane Street", result.company)
        assertEquals(EventType.PRE_PLACEMENT_TALK.name, result.eventType)
        assertEquals("2026-10-25", result.date)
        assertEquals("18:00", result.startTime)
        assertEquals("19:30", result.endTime)
        assertEquals("CLT", result.venue)
        assertEquals("https://meet.google.com/abc-defg-hij", result.meetingUrl)
    }

    @Test
    fun testMicrosoftInterviewExtraction() {
        val text = "Microsoft Placement Update: Interview shortlisted candidates session is scheduled for Monday at 10:00 AM IST on MS Teams (https://teams.microsoft.com/l/meetup-join/xyz). Venue: Online."
        val result = extractor.extract(text, refDate)

        assertTrue(result.isEvent)
        assertEquals("Microsoft", result.company)
        assertEquals(EventType.INTERVIEW.name, result.eventType)
        assertEquals("2026-10-26", result.date)
        assertEquals("10:00", result.startTime)
        assertEquals("https://teams.microsoft.com/l/meetup-join/xyz", result.meetingUrl)
    }

    @Test
    fun testHondaExtraction() {
        val text = """
            *PPT Announcement* 

            Company Name : Honda R&D

            PPT time : 07/10/2026  7pm

            PPT venue : RMN 101

            Profiles :   
            1. AI Engineer
            2. AI & advanced mobility
            3. Semiconductor

            Profiles are open for all departments and all degrees.

            (Since placement portal is down, you can't see or apply for the profiles)

            Whatsapp group: https://chat.whatsapp.com/LIuelkgiauo5cwtyNbNVPg

            Join the group for further updates.
        """.trimIndent()
        val result = extractor.extract(text, refDate)

        assertTrue(result.isEvent)
        assertEquals("Honda R&D", result.company)
        assertEquals(EventType.PRE_PLACEMENT_TALK.name, result.eventType)
        assertEquals("2026-10-07", result.date)
        assertEquals("19:00", result.startTime)
        assertEquals("RMN 101", result.venue)
        assertEquals("https://chat.whatsapp.com/LIuelkgiauo5cwtyNbNVPg", result.meetingUrl)
    }

    @Test
    fun testNonPlacementMessage() {
        val text = "Hey guys, let's meet at Himalaya Mess for dinner at 8 PM tonight."
        val result = extractor.extract(text, refDate)

        assertTrue(!result.isEvent || result.company.isBlank())
    }
}
