package com.example.eventnest

import com.example.eventnest.models.Booking
import com.example.eventnest.utils.AdminBookingsFilterHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminBookingsFilterTest {

    private val sampleBookings = listOf(
        Booking(bookingId = "b1", eventName = "Music Fest", status = "CONFIRMED", totalAmount = 998.0),
        Booking(bookingId = "b2", eventName = "Art Gala", status = "confirmed", totalAmount = 650.0),
        Booking(bookingId = "b3", eventName = "Holiday Getaway", status = "pending", totalAmount = 2499.0),
        Booking(bookingId = "b4", eventName = "Coffee Workshop", status = "cancelled", totalAmount = 350.0),
        Booking(bookingId = "b5", eventName = "Food Fiesta", status = "CONFIRMED", totalAmount = 598.0)
    )

    @Test
    fun testFilterBookings_AllReturnsCompleteList() {
        val result = AdminBookingsFilterHelper.filterBookings(sampleBookings, AdminBookingsFilterHelper.FILTER_ALL)
        assertEquals(5, result.size)
    }

    @Test
    fun testFilterBookings_ConfirmedMatchesBothCasings() {
        val result = AdminBookingsFilterHelper.filterBookings(sampleBookings, AdminBookingsFilterHelper.FILTER_CONFIRMED)
        assertEquals(3, result.size)
        assertEquals(listOf("b1", "b2", "b5"), result.map { it.bookingId })
    }

    @Test
    fun testFilterBookings_PendingReturnsPendingOnly() {
        val result = AdminBookingsFilterHelper.filterBookings(sampleBookings, AdminBookingsFilterHelper.FILTER_PENDING)
        assertEquals(1, result.size)
        assertEquals("b3", result.first().bookingId)
    }

    @Test
    fun testFilterBookings_CancelledReturnsCancelledOnly() {
        val result = AdminBookingsFilterHelper.filterBookings(sampleBookings, AdminBookingsFilterHelper.FILTER_CANCELLED)
        assertEquals(1, result.size)
        assertEquals("b4", result.first().bookingId)
    }

    @Test
    fun testFormatCustomerLabel() {
        // Name + Email
        val fullLabel = AdminBookingsFilterHelper.formatCustomerLabel("John Doe", "john@example.com", "uid123456789")
        assertEquals("John Doe (john@example.com)", fullLabel)

        // Only Name
        val nameOnly = AdminBookingsFilterHelper.formatCustomerLabel("John Doe", "", "uid123456789")
        assertEquals("John Doe", nameOnly)

        // Only Email
        val emailOnly = AdminBookingsFilterHelper.formatCustomerLabel("", "john@example.com", "uid123456789")
        assertEquals("john@example.com", emailOnly)

        // Neither (fallback to UID)
        val fallback = AdminBookingsFilterHelper.formatCustomerLabel("", "", "usr_9876543210")
        assertTrue(fallback.startsWith("User: usr_987654"))
    }
}
