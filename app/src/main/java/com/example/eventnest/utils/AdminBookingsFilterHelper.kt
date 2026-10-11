package com.example.eventnest.utils

import com.example.eventnest.models.Booking

object AdminBookingsFilterHelper {
    const val FILTER_ALL = "all"
    const val FILTER_CONFIRMED = "confirmed"
    const val FILTER_PENDING = "pending"
    const val FILTER_CANCELLED = "cancelled"

    /**
     * Filters a list of bookings based on the status filter string.
     */
    fun filterBookings(bookings: List<Booking>, filter: String): List<Booking> {
        return when (filter.lowercase()) {
            FILTER_CONFIRMED -> bookings.filter { it.status.equals("confirmed", ignoreCase = true) }
            FILTER_PENDING -> bookings.filter { it.status.equals("pending", ignoreCase = true) }
            FILTER_CANCELLED -> bookings.filter { it.status.equals("cancelled", ignoreCase = true) }
            else -> bookings
        }
    }

    /**
     * Formats customer label for display in booking card.
     */
    fun formatCustomerLabel(name: String?, email: String?, fallbackUserId: String): String {
        val cleanName = name?.trim().orEmpty()
        val cleanEmail = email?.trim().orEmpty()

        return when {
            cleanName.isNotBlank() && cleanEmail.isNotBlank() -> "$cleanName ($cleanEmail)"
            cleanName.isNotBlank() -> cleanName
            cleanEmail.isNotBlank() -> cleanEmail
            fallbackUserId.isNotBlank() -> "User: ${fallbackUserId.take(10)}..."
            else -> "Customer: Unknown"
        }
    }
}
