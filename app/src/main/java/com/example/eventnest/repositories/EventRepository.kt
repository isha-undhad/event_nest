package com.example.eventnest.repositories

import android.util.Log
import com.example.eventnest.models.Event
import com.example.eventnest.utils.awaitTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EventRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private val eventsCollection = firestore.collection(COLLECTION_EVENTS)

    suspend fun getAllEvents(): Result<List<Event>> = withContext(Dispatchers.IO) {
        try {
            val snapshot = eventsCollection.get().awaitTask()
            val events = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Event::class.java)?.apply {
                    if (eventId.isBlank()) {
                        eventId = doc.id
                    }
                }
            }
            Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching events: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getEventsByCategory(categoryId: String): List<Event> = withContext(Dispatchers.IO) {
        try {
            val query = if (categoryId.isBlank() || categoryId.equals("all", ignoreCase = true)) {
                eventsCollection.whereEqualTo("status", "active")
            } else {
                eventsCollection
                    .whereEqualTo("categoryId", categoryId)
                    .whereEqualTo("status", "active")
            }

            // Attempt ordered query by date
            val snapshot = try {
                query.orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING).get().awaitTask()
            } catch (e: Exception) {
                Log.w(TAG, "Indexed order query failed, sorting in memory: ${e.message}")
                query.get().awaitTask()
            }

            val events = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Event::class.java)?.apply {
                    if (eventId.isBlank()) {
                        eventId = doc.id
                    }
                }
            }
            events.sortedBy { it.date }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching events for category $categoryId: ${e.message}", e)
            emptyList()
        }
    }

    suspend fun searchEvents(query: String, categoryId: String? = null): Result<List<Event>> = withContext(Dispatchers.IO) {
        try {
            val events = if (categoryId.isNullOrBlank() || categoryId.equals("all", ignoreCase = true)) {
                getAllEvents().getOrDefault(emptyList())
            } else {
                getEventsByCategory(categoryId)
            }

            val filtered = if (query.isBlank()) {
                events
            } else {
                val cleanQuery = query.trim().lowercase()
                events.filter { event ->
                    event.eventName.lowercase().contains(cleanQuery) ||
                    event.venue.lowercase().contains(cleanQuery) ||
                    event.description.lowercase().contains(cleanQuery)
                }
            }
            Result.success(filtered)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching events: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getEventById(eventId: String): Result<Event?> = withContext(Dispatchers.IO) {
        try {
            val snapshot = eventsCollection.document(eventId).get().awaitTask()
            val event = snapshot.toObject(Event::class.java)?.apply {
                if (this.eventId.isBlank()) {
                    this.eventId = snapshot.id
                }
            }
            Result.success(event)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching event $eventId: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createEvent(event: Event): Result<String> = withContext(Dispatchers.IO) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            return@withContext Result.failure(Exception("You must be signed in to perform this action"))
        }
        try {
            val docRef = if (event.eventId.isNotBlank()) {
                eventsCollection.document(event.eventId)
            } else {
                eventsCollection.document()
            }
            event.eventId = docRef.id
            if (event.status.isBlank()) {
                event.status = "active"
            }
            docRef.set(event, SetOptions.merge()).awaitTask()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating event: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updateEvent(event: Event): Result<Unit> = withContext(Dispatchers.IO) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            return@withContext Result.failure(Exception("You must be signed in to perform this action"))
        }
        try {
            require(event.eventId.isNotBlank()) { "Event ID cannot be blank for update" }
            eventsCollection.document(event.eventId).set(event, SetOptions.merge()).awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event ${event.eventId}: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            return@withContext Result.failure(Exception("You must be signed in to perform this action"))
        }
        try {
            eventsCollection.document(eventId).delete().awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting event $eventId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Soft deletes an event by updating its status to "cancelled" without deleting the Firestore document.
     * Preserves existing bookings for administrative tracking.
     */
    suspend fun softDeleteEvent(eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (FirebaseAuth.getInstance().currentUser == null) {
            return@withContext Result.failure(Exception("You must be signed in to perform this action"))
        }
        try {
            require(eventId.isNotBlank()) { "Event ID cannot be blank for soft delete" }
            eventsCollection.document(eventId).update("status", "cancelled").awaitTask()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error soft-deleting event $eventId: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Attaches a real-time Firestore snapshot listener to the events collection.
     * Emits the complete, live list of events whenever documents are created, updated, or cancelled.
     */
    fun listenToAllEvents(
        onUpdate: (List<Event>) -> Unit,
        onError: ((Exception) -> Unit)? = null
    ): ListenerRegistration {
        return eventsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error listening to all events: ${error.message}", error)
                onError?.invoke(error)
                return@addSnapshotListener
            }

            val events = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Event::class.java)?.apply {
                    if (eventId.isBlank()) {
                        eventId = doc.id
                    }
                }
            }.orEmpty()

            onUpdate(events)
        }
    }

    suspend fun seedSampleEventsIfEmpty(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = eventsCollection.limit(1).get().awaitTask()
            if (snapshot.isEmpty) {
                Log.d(TAG, "Events collection is empty. Seeding sample events...")
                val sampleEvents = listOf(
                    Event(
                        eventId = "evt_music_fest_2026",
                        eventName = "Indie Beats Music Festival",
                        categoryId = "cat_music",
                        description = "An unforgettable evening of live acoustic, indie rock, and electronic music featuring renowned national and independent artists.",
                        venue = "Jio World Garden, BKC, Mumbai",
                        date = "24 Oct 2026",
                        time = "06:30 PM",
                        tiers = listOf(
                            com.example.eventnest.models.TicketTier("tier_general", "General", 499.0, 300, 50),
                            com.example.eventnest.models.TicketTier("tier_vip", "VIP", 999.0, 150, 20),
                            com.example.eventnest.models.TicketTier("tier_vvip", "VVIP", 1999.0, 50, 8)
                        ),
                        price = 499.0,
                        totalSeats = 500,
                        availableSeats = 78,
                        bannerImageUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=800&auto=format&fit=crop",
                        status = "active"
                    ),
                    Event(
                        eventId = "evt_arts_gala_2026",
                        eventName = "Bangalore Contemporary Art & Theater Gala",
                        categoryId = "cat_arts",
                        description = "Experience immersive live drama, classic storytelling, and an exclusive showcase of modern abstract paintings by Indian artists.",
                        venue = "Chowdiah Memorial Hall, Bengaluru",
                        date = "28 Oct 2026",
                        time = "07:00 PM",
                        tiers = listOf(
                            com.example.eventnest.models.TicketTier("tier_general", "General", 650.0, 100, 3),
                            com.example.eventnest.models.TicketTier("tier_vip", "VIP", 1200.0, 35, 2),
                            com.example.eventnest.models.TicketTier("tier_vvip", "VVIP", 2200.0, 15, 0)
                        ),
                        price = 650.0,
                        totalSeats = 150,
                        availableSeats = 5,
                        bannerImageUrl = "https://images.unsplash.com/photo-1460661419201-fd4cecdf8a8b?w=800&auto=format&fit=crop",
                        status = "active"
                    ),
                    Event(
                        eventId = "evt_holiday_getaway_2026",
                        eventName = "Goa Sunsplash Holiday Getaway",
                        categoryId = "cat_holidays",
                        description = "A celebratory beach holiday festival featuring bonfire music, sunset cruises, and reggae vibes by the Arabian Sea.",
                        venue = "Anjuna Beach, North Goa",
                        date = "31 Dec 2026",
                        time = "05:00 PM",
                        tiers = listOf(
                            com.example.eventnest.models.TicketTier("tier_general", "General", 2499.0, 120, 0),
                            com.example.eventnest.models.TicketTier("tier_vip", "VIP", 4499.0, 60, 0),
                            com.example.eventnest.models.TicketTier("tier_vvip", "VVIP", 7999.0, 20, 0)
                        ),
                        price = 2499.0,
                        totalSeats = 200,
                        availableSeats = 0,
                        bannerImageUrl = "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=800&auto=format&fit=crop",
                        status = "active"
                    ),
                    Event(
                        eventId = "evt_coffee_pottery_workshop",
                        eventName = "Artisanal Coffee Brewing & Pottery Workshop",
                        categoryId = "cat_hobbies",
                        description = "Hands-on pottery wheel masterclass combined with specialty coffee pour-over brewing techniques guided by master roasters.",
                        venue = "The Bohemian Studio, Indiranagar, Bengaluru",
                        date = "08 Nov 2026",
                        time = "11:00 AM",
                        tiers = listOf(
                            com.example.eventnest.models.TicketTier("tier_general", "General", 350.0, 20, 12),
                            com.example.eventnest.models.TicketTier("tier_vip", "VIP Workshop Kit", 650.0, 10, 6)
                        ),
                        price = 350.0,
                        totalSeats = 30,
                        availableSeats = 18,
                        bannerImageUrl = "https://images.unsplash.com/photo-1517256064527-09c73fc73e38?w=800&auto=format&fit=crop",
                        status = "active"
                    ),
                    Event(
                        eventId = "evt_mumbai_food_fiesta",
                        eventName = "The Great Mumbai Street Food Fiesta",
                        categoryId = "cat_food",
                        description = "Over 60 signature food stalls celebrating iconic chaat, kebabs, coastal seafood, and handcrafted artisan desserts.",
                        venue = "Mahalaxmi Racecourse, Mumbai",
                        date = "15 Nov 2026",
                        time = "12:00 PM",
                        tiers = listOf(
                            com.example.eventnest.models.TicketTier("tier_general", "General Tasting", 299.0, 250, 30),
                            com.example.eventnest.models.TicketTier("tier_vip", "VIP FastPass", 599.0, 100, 10),
                            com.example.eventnest.models.TicketTier("tier_vvip", "VVIP Chef Lounge", 1199.0, 50, 5)
                        ),
                        price = 299.0,
                        totalSeats = 400,
                        availableSeats = 45,
                        bannerImageUrl = "https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=800&auto=format&fit=crop",
                        status = "active"
                    )
                )

                for (event in sampleEvents) {
                    eventsCollection.document(event.eventId).set(event).awaitTask()
                }
                Log.d(TAG, "Sample events seeded successfully.")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Could not seed events: ${e.message}")
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "EventRepository"
        const val COLLECTION_EVENTS = "events"
    }
}
