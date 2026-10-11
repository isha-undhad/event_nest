package com.example.eventnest.repositories

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AdminRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Real-time listener for confirmed bookings to calculate Total Revenue and Tickets Sold.
     */
    fun listenToBookingStats(
        onUpdate: (totalRevenue: Double, ticketsSold: Int) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_BOOKINGS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to bookings: ${error.message}", error)
                    return@addSnapshotListener
                }

                var revenue = 0.0
                var tickets = 0

                if (snapshot != null) {
                    for (doc in snapshot.documents) {
                        val status = doc.getString("status").orEmpty()
                        if (status.equals("confirmed", ignoreCase = true)) {
                            val amount = doc.getDouble("totalAmount") ?: 0.0
                            val ticketCount = doc.getLong("numberOfTickets")?.toInt() ?: 0
                            revenue += amount
                            tickets += ticketCount
                        }
                    }
                }

                onUpdate(revenue, tickets)
            }
    }

    /**
     * Real-time listener for active events to calculate Active Events and Upcoming Events count.
     */
    fun listenToEventStats(
        onUpdate: (activeEventsCount: Int, upcomingEventsCount: Int) -> Unit
    ): ListenerRegistration {
        return firestore.collection(COLLECTION_EVENTS)
            .whereEqualTo("status", "active")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to active events: ${error.message}", error)
                    return@addSnapshotListener
                }

                var activeCount = 0
                var upcomingCount = 0

                if (snapshot != null) {
                    activeCount = snapshot.size()
                    val now = Calendar.getInstance()

                    for (doc in snapshot.documents) {
                        val dateStr = doc.getString("date").orEmpty()
                        if (isUpcomingOrToday(dateStr, now)) {
                            upcomingCount++
                        }
                    }
                }

                onUpdate(activeCount, upcomingCount)
            }
    }

    companion object {
        private const val TAG = "AdminRepository"
        const val COLLECTION_BOOKINGS = "bookings"
        const val COLLECTION_EVENTS = "events"

        /**
         * Checks if the date string represents today or a future date.
         */
        fun isUpcomingOrToday(dateStr: String, nowCalendar: Calendar = Calendar.getInstance()): Boolean {
            val cleanDate = dateStr.trim()
            if (cleanDate.isBlank()) return false

            val supportedPatterns = listOf(
                "dd MMM yyyy",
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "d MMM yyyy"
            )

            val startOfToday = (nowCalendar.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            for (pattern in supportedPatterns) {
                try {
                    val formatter = SimpleDateFormat(pattern, Locale.ENGLISH).apply {
                        isLenient = false
                    }
                    val pos = java.text.ParsePosition(0)
                    val parsedDate = formatter.parse(cleanDate, pos)
                    if (parsedDate != null && pos.index == cleanDate.length) {
                        return parsedDate.time >= startOfToday
                    }
                } catch (e: Exception) {
                    // Try next format
                }
            }

            return false
        }
    }
}
