package com.example.eventnest.repositories

import android.util.Log
import com.example.eventnest.models.Booking
import com.example.eventnest.models.Event
import com.example.eventnest.utils.awaitTask
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BookingRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val bookingsCollection = firestore.collection(COLLECTION_BOOKINGS)
    private val eventsCollection = firestore.collection(EventRepository.COLLECTION_EVENTS)

    /**
     * Atomically books tickets using a Firestore transaction to prevent overbooking.
     */
    suspend fun createBooking(
        userId: String,
        eventId: String,
        numberOfTickets: Int,
        tierId: String = "",
        tierName: String = ""
    ): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            require(userId.isNotBlank()) { "User ID cannot be blank" }
            require(eventId.isNotBlank()) { "Event ID cannot be blank" }
            require(numberOfTickets > 0) { "Ticket count must be at least 1" }

            val bookingDocRef = bookingsCollection.document()
            val eventDocRef = eventsCollection.document(eventId)

            val currentDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

            val createdBooking = firestore.runTransaction { transaction ->
                val eventSnapshot = transaction.get(eventDocRef)
                if (!eventSnapshot.exists()) {
                    throw IllegalStateException("Event with ID $eventId does not exist.")
                }

                val event = eventSnapshot.toObject(Event::class.java)
                    ?: throw IllegalStateException("Failed to parse event data.")

                val tiers = event.effectiveTiers
                val matchedTier = if (tierId.isNotBlank()) {
                    tiers.firstOrNull { it.tierId == tierId }
                } else {
                    tiers.firstOrNull { it.availableSeats >= numberOfTickets } ?: tiers.firstOrNull()
                }

                val finalTierId: String
                val finalTierName: String
                val calculatedTotal: Double

                if (event.tiers.isNotEmpty() && matchedTier != null) {
                    if (matchedTier.availableSeats < numberOfTickets) {
                        throw IllegalStateException("Not enough seats available in ${matchedTier.tierName}. Remaining: ${matchedTier.availableSeats}")
                    }
                    finalTierId = matchedTier.tierId
                    finalTierName = matchedTier.tierName
                    calculatedTotal = numberOfTickets * matchedTier.price

                    val updatedTiers = event.tiers.map { t ->
                        if (t.tierId == matchedTier.tierId) {
                            t.copy(availableSeats = t.availableSeats - numberOfTickets)
                        } else {
                            t
                        }
                    }
                    val newTotalAvailable = updatedTiers.sumOf { it.availableSeats }

                    transaction.update(eventDocRef, "tiers", updatedTiers)
                    transaction.update(eventDocRef, "availableSeats", newTotalAvailable)
                } else {
                    // Legacy fallback
                    if (event.availableSeats < numberOfTickets) {
                        throw IllegalStateException("Not enough seats available. Remaining: ${event.availableSeats}")
                    }
                    finalTierId = tierId.ifBlank { "tier_general" }
                    finalTierName = tierName.ifBlank { "General" }
                    calculatedTotal = numberOfTickets * event.price
                    val newAvailableSeats = event.availableSeats - numberOfTickets
                    transaction.update(eventDocRef, "availableSeats", newAvailableSeats)
                }

                // Create Booking record
                val booking = Booking(
                    bookingId = bookingDocRef.id,
                    userId = userId,
                    eventId = eventId,
                    eventName = event.eventName,
                    bookingDate = currentDateStr,
                    numberOfTickets = numberOfTickets,
                    totalAmount = calculatedTotal,
                    status = "CONFIRMED",
                    tierId = finalTierId,
                    tierName = finalTierName
                )

                transaction.set(bookingDocRef, booking)
                booking
            }.awaitTask()

            Result.success(createdBooking)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating booking for event $eventId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Creates a pending booking document with status "pending".
     */
    suspend fun createPendingBooking(
        userId: String,
        eventId: String,
        eventName: String,
        numberOfTickets: Int,
        totalAmount: Double,
        tierId: String = "",
        tierName: String = ""
    ): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            require(userId.isNotBlank()) { "User ID cannot be blank" }
            require(eventId.isNotBlank()) { "Event ID cannot be blank" }
            require(numberOfTickets > 0) { "Ticket count must be at least 1" }

            val bookingDocRef = bookingsCollection.document()
            val currentDateStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())

            val booking = Booking(
                bookingId = bookingDocRef.id,
                userId = userId,
                eventId = eventId,
                eventName = eventName,
                bookingDate = currentDateStr,
                numberOfTickets = numberOfTickets,
                totalAmount = totalAmount,
                status = "pending",
                tierId = tierId,
                tierName = tierName
            )

            bookingDocRef.set(booking).awaitTask()
            Result.success(booking)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating pending booking for event $eventId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Atomically validates available seats, decrements seats by quantity,
     * and updates the booking status to "confirmed".
     * If check fails, rolls back transaction and sets booking status to "cancelled".
     */
    suspend fun confirmBookingWithSeatDeduction(
        bookingId: String,
        eventId: String,
        quantity: Int,
        tierId: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val eventDocRef = eventsCollection.document(eventId)
        val bookingDocRef = bookingsCollection.document(bookingId)

        try {
            firestore.runTransaction { transaction ->
                val bookingSnapshot = transaction.get(bookingDocRef)
                val eventSnapshot = transaction.get(eventDocRef)

                if (!eventSnapshot.exists()) {
                    throw IllegalStateException("Event with ID $eventId does not exist.")
                }

                val event = eventSnapshot.toObject(Event::class.java)
                    ?: throw IllegalStateException("Failed to parse event data.")

                // Determine target tierId from parameter or from the booking document
                val effectiveTierId = tierId.ifBlank {
                    bookingSnapshot.getString("tierId").orEmpty()
                }

                if (event.tiers.isNotEmpty() && effectiveTierId.isNotBlank()) {
                    val matchedTier = event.tiers.firstOrNull { it.tierId == effectiveTierId }
                        ?: throw IllegalStateException("Ticket tier not found on event: $effectiveTierId")

                    if (matchedTier.availableSeats < quantity) {
                        throw IllegalStateException("Not enough seats available in ${matchedTier.tierName}. Remaining: ${matchedTier.availableSeats}, Requested: $quantity")
                    }

                    val updatedTiers = event.tiers.map { t ->
                        if (t.tierId == effectiveTierId) {
                            t.copy(availableSeats = t.availableSeats - quantity)
                        } else {
                            t
                        }
                    }
                    val newTotalAvailable = updatedTiers.sumOf { it.availableSeats }

                    transaction.update(eventDocRef, "tiers", updatedTiers)
                    transaction.update(eventDocRef, "availableSeats", newTotalAvailable)
                } else if (event.tiers.isNotEmpty()) {
                    // Fallback to first tier with enough seats
                    val matchedTier = event.tiers.firstOrNull { it.availableSeats >= quantity }
                        ?: throw IllegalStateException("Not enough seats available in any tier.")

                    val updatedTiers = event.tiers.map { t ->
                        if (t.tierId == matchedTier.tierId) {
                            t.copy(availableSeats = t.availableSeats - quantity)
                        } else {
                            t
                        }
                    }
                    val newTotalAvailable = updatedTiers.sumOf { it.availableSeats }

                    transaction.update(eventDocRef, "tiers", updatedTiers)
                    transaction.update(eventDocRef, "availableSeats", newTotalAvailable)
                    transaction.update(bookingDocRef, "tierId", matchedTier.tierId)
                    transaction.update(bookingDocRef, "tierName", matchedTier.tierName)
                } else {
                    // Legacy event without tiers
                    val currentSeats = eventSnapshot.getLong("availableSeats")?.toInt()
                        ?: throw IllegalStateException("Failed to read available seats.")

                    if (currentSeats < quantity) {
                        throw IllegalStateException("Not enough seats available. Remaining: $currentSeats, Requested: $quantity")
                    }

                    val newAvailableSeats = currentSeats - quantity
                    transaction.update(eventDocRef, "availableSeats", newAvailableSeats)
                }

                transaction.update(bookingDocRef, "status", "confirmed")
            }.awaitTask()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "confirmBookingWithSeatDeduction failed for booking $bookingId: ${e.message}", e)
            try {
                bookingDocRef.update("status", "cancelled").awaitTask()
            } catch (cancelEx: Exception) {
                Log.e(TAG, "Failed to update booking status to cancelled: ${cancelEx.message}", cancelEx)
            }
            Result.failure(e)
        }
    }

    suspend fun getUserBookings(userId: String): List<Booking> = withContext(Dispatchers.IO) {
        try {
            val snapshot = bookingsCollection.whereEqualTo("userId", userId).get().awaitTask()
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val bookings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.apply {
                    if (bookingId.isBlank()) {
                        bookingId = doc.id
                    }
                }
            }
            // Sort by bookingDate descending
            bookings.sortedByDescending { booking ->
                try {
                    dateFormat.parse(booking.bookingDate)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching bookings for user $userId: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Fetches all bookings across the entire platform, ordered by bookingDate descending.
     * System-wide read-only access for administrators.
     */
    suspend fun getAllBookings(): List<Booking> = withContext(Dispatchers.IO) {
        try {
            val snapshot = bookingsCollection.get().awaitTask()
            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val bookings = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.apply {
                    if (bookingId.isBlank()) {
                        bookingId = doc.id
                    }
                }
            }
            bookings.sortedByDescending { booking ->
                try {
                    dateFormat.parse(booking.bookingDate)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching all bookings: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Real-time listener for all bookings across the platform, ordered by bookingDate descending.
     */
    fun listenToAllBookings(
        onUpdate: (List<Booking>) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ): ListenerRegistration {
        return bookingsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to all bookings: ${error.message}", error)
                onError?.invoke(error)
                return@addSnapshotListener
            }

            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val bookings = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Booking::class.java)?.apply {
                    if (bookingId.isBlank()) {
                        bookingId = doc.id
                    }
                }
            }.orEmpty()

            val sortedBookings = bookings.sortedByDescending { booking ->
                try {
                    dateFormat.parse(booking.bookingDate)?.time ?: 0L
                } catch (e: Exception) {
                    0L
                }
            }

            onUpdate(sortedBookings)
        }
    }

    suspend fun getBookingById(bookingId: String): Result<Booking?> = withContext(Dispatchers.IO) {
        try {
            val snapshot = bookingsCollection.document(bookingId).get().awaitTask()
            val booking = snapshot.toObject(Booking::class.java)?.apply {
                if (this.bookingId.isBlank()) {
                    this.bookingId = snapshot.id
                }
            }
            Result.success(booking)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching booking $bookingId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Atomically cancels the booking and restores seats to the event document.
     */
    suspend fun cancelBooking(
        bookingId: String,
        eventId: String,
        quantity: Int,
        tierId: String = ""
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bookingDocRef = bookingsCollection.document(bookingId)
            val eventDocRef = eventsCollection.document(eventId)

            firestore.runTransaction { transaction ->
                val bookingSnapshot = transaction.get(bookingDocRef)
                if (!bookingSnapshot.exists()) {
                    throw IllegalStateException("Booking $bookingId not found.")
                }

                val booking = bookingSnapshot.toObject(Booking::class.java)
                    ?: throw IllegalStateException("Failed to parse booking.")

                if (booking.status.equals("cancelled", ignoreCase = true)) {
                    return@runTransaction
                }

                val effectiveTierId = tierId.ifBlank { booking.tierId }

                // Restore seats to the event
                val eventSnapshot = transaction.get(eventDocRef)
                if (eventSnapshot.exists()) {
                    val event = eventSnapshot.toObject(Event::class.java)
                    if (event != null && event.tiers.isNotEmpty() && effectiveTierId.isNotBlank()) {
                        val updatedTiers = event.tiers.map { t ->
                            if (t.tierId == effectiveTierId) {
                                t.copy(availableSeats = (t.availableSeats + quantity).coerceAtMost(t.totalSeats))
                            } else {
                                t
                            }
                        }
                        val newTotalAvailable = updatedTiers.sumOf { it.availableSeats }
                        transaction.update(eventDocRef, "tiers", updatedTiers)
                        transaction.update(eventDocRef, "availableSeats", newTotalAvailable)
                    } else {
                        val currentAvailable = eventSnapshot.getLong("availableSeats")?.toInt() ?: 0
                        transaction.update(eventDocRef, "availableSeats", currentAvailable + quantity)
                    }
                }

                // Mark booking as cancelled
                transaction.update(bookingDocRef, "status", "cancelled")
            }.awaitTask()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling booking $bookingId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun cancelBooking(bookingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bookingSnapshot = bookingsCollection.document(bookingId).get().awaitTask()
            val booking = bookingSnapshot.toObject(Booking::class.java)
                ?: throw IllegalStateException("Booking $bookingId not found")
            cancelBooking(bookingId, booking.eventId, booking.numberOfTickets, booking.tierId)
        } catch (e: Exception) {
            Log.e(TAG, "Error in cancelBooking($bookingId): ${e.message}", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "BookingRepository"
        const val COLLECTION_BOOKINGS = "bookings"
    }
}
