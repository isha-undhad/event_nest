package com.example.eventnest

import com.example.eventnest.repositories.AdminRepository
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminStatsTest {

    @Test
    fun testIsUpcomingOrToday_WithFutureDates() {
        // Fixed reference date: 2026-09-07 10:00:00
        val baseCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 10, 0, 0)
        }

        // Distinct supported date formats for future dates
        assertTrue(AdminRepository.isUpcomingOrToday("08 Sep 2026", baseCalendar))
        assertTrue(AdminRepository.isUpcomingOrToday("24 Oct 2026", baseCalendar))
        assertTrue(AdminRepository.isUpcomingOrToday("2026-10-15", baseCalendar))
        assertTrue(AdminRepository.isUpcomingOrToday("15/10/2026", baseCalendar))
        assertTrue(AdminRepository.isUpcomingOrToday("15-10-2026", baseCalendar))
        assertTrue(AdminRepository.isUpcomingOrToday("01 Jan 2027", baseCalendar))
    }

    @Test
    fun testIsUpcomingOrToday_WithTodayDate() {
        val baseCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 18, 30, 0)
        }

        // Even if the current time is 18:30 in the evening, today's date should be considered upcoming/today
        assertTrue(AdminRepository.isUpcomingOrToday("07 Sep 2026", baseCalendar))
        assertTrue(AdminRepository.isUpcomingOrToday("2026-09-07", baseCalendar))
        assertTrue(AdminRepository.isUpcomingOrToday("07/09/2026", baseCalendar))
        assertTrue(AdminRepository.isUpcomingOrToday("07-09-2026", baseCalendar))
    }

    @Test
    fun testIsUpcomingOrToday_WithPastDates() {
        val baseCalendar = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 7, 10, 0, 0)
        }

        // Yesterday
        assertFalse(AdminRepository.isUpcomingOrToday("06 Sep 2026", baseCalendar))
        assertFalse(AdminRepository.isUpcomingOrToday("2026-09-06", baseCalendar))
        assertFalse(AdminRepository.isUpcomingOrToday("06/09/2026", baseCalendar))

        // Older past dates
        assertFalse(AdminRepository.isUpcomingOrToday("01 Jan 2025", baseCalendar))
        assertFalse(AdminRepository.isUpcomingOrToday("2024-12-31", baseCalendar))
        assertFalse(AdminRepository.isUpcomingOrToday("15/08/2023", baseCalendar))
    }

    @Test
    fun testIsUpcomingOrToday_WithInvalidAndBlankDates() {
        val baseCalendar = Calendar.getInstance()

        assertFalse(AdminRepository.isUpcomingOrToday("", baseCalendar))
        assertFalse(AdminRepository.isUpcomingOrToday("   ", baseCalendar))
        assertFalse(AdminRepository.isUpcomingOrToday("not-a-date", baseCalendar))
        assertFalse(AdminRepository.isUpcomingOrToday("TBD", baseCalendar))
    }
}
