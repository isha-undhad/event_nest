package com.example.eventnest

import com.example.eventnest.models.Event
import com.example.eventnest.utils.ManageEventsFilterHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageEventsFilterTest {

    private val sampleEvents = listOf(
        Event(eventId = "1", eventName = "Rock Fest", status = "active"),
        Event(eventId = "2", eventName = "Jazz Night", status = "cancelled"),
        Event(eventId = "3", eventName = "Art Expo", status = "active"),
        Event(eventId = "4", eventName = "Food Fair", status = "cancelled"),
        Event(eventId = "5", eventName = "Poetry Slam", status = "active")
    )

    @Test
    fun testFilterEvents_AllReturnsCompleteList() {
        val result = ManageEventsFilterHelper.filterEvents(sampleEvents, ManageEventsFilterHelper.FILTER_ALL)
        assertEquals(5, result.size)
        assertEquals(listOf("1", "2", "3", "4", "5"), result.map { it.eventId })
    }

    @Test
    fun testFilterEvents_ActiveReturnsOnlyActive() {
        val result = ManageEventsFilterHelper.filterEvents(sampleEvents, ManageEventsFilterHelper.FILTER_ACTIVE)
        assertEquals(3, result.size)
        assertTrue(result.all { it.status.equals("active", ignoreCase = true) })
        assertEquals(listOf("1", "3", "5"), result.map { it.eventId })
    }

    @Test
    fun testFilterEvents_CancelledReturnsOnlyCancelled() {
        val result = ManageEventsFilterHelper.filterEvents(sampleEvents, ManageEventsFilterHelper.FILTER_CANCELLED)
        assertEquals(2, result.size)
        assertTrue(result.all { it.status.equals("cancelled", ignoreCase = true) })
        assertEquals(listOf("2", "4"), result.map { it.eventId })
    }

    @Test
    fun testFilterEvents_CaseInsensitiveFilter() {
        val activeUpper = ManageEventsFilterHelper.filterEvents(sampleEvents, "ACTIVE")
        assertEquals(3, activeUpper.size)

        val cancelledMixed = ManageEventsFilterHelper.filterEvents(sampleEvents, "Cancelled")
        assertEquals(2, cancelledMixed.size)
    }

    @Test
    fun testFilterEvents_EmptyListReturnsEmpty() {
        val result = ManageEventsFilterHelper.filterEvents(emptyList(), ManageEventsFilterHelper.FILTER_ACTIVE)
        assertTrue(result.isEmpty())
    }
}
