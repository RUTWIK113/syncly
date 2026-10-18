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

    @Test
    fun testMultiEventFourTypesAndDeadlineDefaultTime() {
        val text = """
            Company: Google
            PPT: 10th Oct (6:00 PM)
            OA: 12th Oct (7:00 PM - 8:30 PM)
            Resume Deadline: 8th Oct EOD
            Registration Deadline: 7th Oct
            Venue: CLT
        """.trimIndent()

        val results = extractor.extractAll(text, refDate)
        assertEquals(4, results.size)

        // 1. PPT
        val ppt = results.find { it.eventType == EventType.PRE_PLACEMENT_TALK.name }!!
        assertEquals("Google", ppt.company)
        assertEquals("2026-10-10", ppt.date)
        assertEquals("18:00", ppt.startTime)

        // 2. OA
        val oa = results.find { it.eventType == EventType.ONLINE_ASSESSMENT.name }!!
        assertEquals("Google", oa.company)
        assertEquals("2026-10-12", oa.date)
        assertEquals("19:00", oa.startTime)
        assertEquals("20:30", oa.endTime)

        // 3. Resume Deadline (EOD -> 23:00)
        val resume = results.find { it.eventType == EventType.RESUME_SUBMISSION.name }!!
        assertEquals("Google", resume.company)
        assertEquals("2026-10-08", resume.date)
        assertEquals("23:00", resume.startTime)

        // 4. Registration Deadline (no time provided -> 23:00)
        val reg = results.find { it.eventType == EventType.REGISTRATION.name }!!
        assertEquals("Google", reg.company)
        assertEquals("2026-10-07", reg.date)
        assertEquals("23:00", reg.startTime)
    }

    @Test
    fun testIterativeExtractionWithThreeEvents() {
        val text = """
            Company: Uber
            PPT: 25th Oct (6:00 PM)
            OA: 27th Oct (8:00 PM)
            Resume Deadline: 24th Oct EOD
        """.trimIndent()

        val results = extractor.extractAll(text, refDate)
        assertEquals(3, results.size)

        for (res in results) {
            assertEquals("Uber", res.company)
        }

        val ppt = results.find { it.eventType == EventType.PRE_PLACEMENT_TALK.name }!!
        assertEquals("2026-10-25", ppt.date)
        assertEquals("18:00", ppt.startTime)

        val oa = results.find { it.eventType == EventType.ONLINE_ASSESSMENT.name }!!
        assertEquals("2026-10-27", oa.date)
        assertEquals("20:00", oa.startTime)

        val resume = results.find { it.eventType == EventType.RESUME_SUBMISSION.name }!!
        assertEquals("2026-10-24", resume.date)
        assertEquals("23:00", resume.startTime)
    }

    @Test
    fun testFallbackDashesWhenCompanyNotFound() {
        val text = """
            PPT: 15th Oct (5:00 PM)
            OA: 18th Oct (7:00 PM)
            Resume Deadline: 14th Oct EOD
        """.trimIndent()

        val results = extractor.extractAll(text, refDate)
        assertEquals(3, results.size)

        for (res in results) {
            assertEquals("-------", res.company)
        }
    }

    @Test
    fun testProseMultiEventExtraction() {
        val text = "Placement notice: PPT is on 15th Oct at 5:00 PM in CLT. Coding test is on 18th Oct at 7:00 PM. Please register and submit resume before 14th Oct EOD. Firm: Microsoft"

        val results = extractor.extractAll(text, refDate)
        assertTrue(results.size >= 2)

        for (res in results) {
            assertEquals("Microsoft", res.company)
        }
    }
}
