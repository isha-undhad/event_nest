package com.example.eventnest.debug

import android.util.Log
import com.example.eventnest.models.Category
import com.example.eventnest.models.Event
import com.example.eventnest.models.TicketTier
import com.example.eventnest.repositories.CategoryRepository
import com.example.eventnest.repositories.EventRepository
import com.example.eventnest.utils.awaitTask
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * One-time, debug-only seeder to populate 20 curated events into Firestore.
 * Matches existing schema and AddEditEventActivity field standards.
 */
object DebugEventSeeder {

    const val BANNER_PLACEHOLDER = "https://example.com/replace-me.jpg"
    private const val TAG = "DebugEventSeeder"

    sealed class SeederResult {
        data class Success(val writtenCount: Int, val eventIds: List<String>) : SeederResult()
        data class MissingCategories(val missing: List<String>, val availableInFirestore: List<String>) : SeederResult()
        data class Error(val message: String, val failures: Map<String, String> = emptyMap()) : SeederResult()
    }

    data class RequiredCategory(
        val categoryId: String,
        val categoryName: String,
        val iconUrl: String = ""
    )

    /**
     * Fixed category IDs and the exact names the seeder searches for.
     */
    val REQUIRED_CATEGORIES: List<RequiredCategory> = listOf(
        RequiredCategory("cat_arts", "Performing & Visual Arts"),
        RequiredCategory("cat_holidays", "Holiday"),
        RequiredCategory("cat_hobbies", "Hobbies"),
        RequiredCategory("cat_music", "Music"),
        RequiredCategory("cat_food", "Food & Drink")
    )

    data class RawTierSpec(
        val tierId: String,
        val tierName: String,
        val price: Double,
        val totalSeats: Int
    )

    data class RawEventSpec(
        val eventId: String,
        val eventName: String,
        val categoryName: String,
        val venue: String,
        val date: String,
        val time: String,
        val description: String,
        val tiers: List<RawTierSpec>
    )

    val EVENT_SPECS: List<RawEventSpec> = listOf(
        // 1
        RawEventSpec(
            eventId = "riff-2026",
            eventName = "Rajasthan International Folk Festival (RIFF)",
            categoryName = "Performing & Visual Arts",
            venue = "Mehrangarh Fort, Jodhpur",
            date = "2026-10-22",
            time = "18:00",
            description = "A celebration of folk, classical, jazz and world music with traditional and contemporary performances. 22-26 Oct 2026.",
            tiers = listOf(
                RawTierSpec("general", "General", 999.0, 300),
                RawTierSpec("vip", "VIP", 2499.0, 150),
                RawTierSpec("vvip", "VVIP", 4999.0, 50)
            )
        ),
        // 2
        RawEventSpec(
            eventId = "rann-utsav-2026",
            eventName = "Rann Utsav",
            categoryName = "Holiday",
            venue = "Tent City, Dhordo, Kutch",
            date = "2026-10-23",
            time = "17:00",
            description = "Experience the White Rann, Gujarati folk performances, handicrafts, regional cuisine and desert culture. 23 Oct 2026 to 4 Mar 2027.",
            tiers = listOf(
                RawTierSpec("standard", "Standard", 1499.0, 180),
                RawTierSpec("premium", "Premium", 3499.0, 90),
                RawTierSpec("vip", "VIP", 5999.0, 30)
            )
        ),
        // 3
        RawEventSpec(
            eventId = "tawang-festival-2026",
            eventName = "Tawang Festival",
            categoryName = "Performing & Visual Arts",
            venue = "Tawang, Arunachal Pradesh",
            date = "2026-10-27",
            time = "10:00",
            description = "Traditional dances, music, local crafts and regional food of Arunachal Pradesh. 27-31 Oct 2026.",
            tiers = listOf(
                RawTierSpec("general", "General", 200.0, 250),
                RawTierSpec("premium", "Premium", 500.0, 120),
                RawTierSpec("vip", "VIP", 1000.0, 30)
            )
        ),
        // 4
        RawEventSpec(
            eventId = "himalayan-100-2026",
            eventName = "Himalayan 100 Mile Stage Race",
            categoryName = "Hobbies",
            venue = "Singalila region, Eastern Himalayas",
            date = "2026-10-27",
            time = "06:00",
            description = "A multi-stage mountain running and trekking challenge through Himalayan landscapes. 27 Oct to 3 Nov 2026.",
            tiers = listOf(
                RawTierSpec("participant", "Participant", 5000.0, 60),
                RawTierSpec("advanced", "Advanced", 8000.0, 30),
                RawTierSpec("elite", "Elite", 12000.0, 10)
            )
        ),
        // 5
        RawEventSpec(
            eventId = "rashtriya-ekta-divas-2026",
            eventName = "Rashtriya Ekta Divas",
            categoryName = "Holiday",
            venue = "Statue of Unity, Ekta Nagar, Gujarat",
            date = "2026-10-31",
            time = "09:00",
            description = "A national celebration honoring Sardar Vallabhbhai Patel and promoting unity and national integration.",
            tiers = listOf(
                RawTierSpec("general", "General", 0.0, 700),
                RawTierSpec("reserved", "Reserved", 100.0, 250),
                RawTierSpec("premium", "Premium", 300.0, 50)
            )
        ),
        // 6
        RawEventSpec(
            eventId = "kalidas-samaroh-2026",
            eventName = "Akhil Bhartiya Kalidas Samaroh",
            categoryName = "Performing & Visual Arts",
            venue = "Kalidas Sanskrit Academy, Ujjain",
            date = "2026-11-25",
            time = "10:00",
            description = "A cultural gathering celebrating classical Indian literature, drama, music and the legacy of Kalidasa. Runs Nov-Dec 2026 (demo start date).",
            tiers = listOf(
                RawTierSpec("general", "General", 100.0, 250),
                RawTierSpec("premium", "Premium", 300.0, 120),
                RawTierSpec("vip", "VIP", 700.0, 30)
            )
        ),
        // 7
        RawEventSpec(
            eventId = "shamlaji-fair-2026",
            eventName = "Shamlaji Fair",
            categoryName = "Holiday",
            venue = "Shamlaji Temple, Aravalli, Gujarat",
            date = "2026-11-20",
            time = "08:00",
            description = "A traditional fair of pilgrims, local communities, handicrafts and regional festivities. 20-24 Nov 2026.",
            tiers = listOf(
                RawTierSpec("general", "General", 0.0, 400),
                RawTierSpec("special-access", "Special Access", 100.0, 150),
                RawTierSpec("premium", "Premium", 250.0, 50)
            )
        ),
        // 8
        RawEventSpec(
            eventId = "vautha-mela-2026",
            eventName = "Vautha Mela",
            categoryName = "Holiday",
            venue = "Vautha village fairgrounds, Gujarat",
            date = "2026-11-24",
            time = "08:00",
            description = "A traditional rural fair with cultural heritage, community gatherings, livestock trading and local crafts (demo date).",
            tiers = listOf(
                RawTierSpec("general", "General", 0.0, 350),
                RawTierSpec("reserved", "Reserved", 100.0, 120),
                RawTierSpec("premium", "Premium", 300.0, 30)
            )
        ),
        // 9
        RawEventSpec(
            eventId = "somnath-kartik-poonam-2026",
            eventName = "Somnath Kartik Poonam Fair",
            categoryName = "Holiday",
            venue = "Somnath Temple area, Gir Somnath",
            date = "2026-11-24",
            time = "17:00",
            description = "A religious and cultural gathering with devotional activities, temple traditions and festive celebrations (demo date).",
            tiers = listOf(
                RawTierSpec("general", "General", 0.0, 350),
                RawTierSpec("reserved", "Reserved", 100.0, 120),
                RawTierSpec("premium", "Premium", 250.0, 30)
            )
        ),
        // 10
        RawEventSpec(
            eventId = "konark-festival-2026",
            eventName = "Konark Festival",
            categoryName = "Performing & Visual Arts",
            venue = "Open-air auditorium near Konark Sun Temple, Odisha",
            date = "2026-12-01",
            time = "18:00",
            description = "A showcase of Indian classical dance and music. 1-5 Dec 2026.",
            tiers = listOf(
                RawTierSpec("general", "General", 300.0, 300),
                RawTierSpec("premium", "Premium", 800.0, 150),
                RawTierSpec("vip", "VIP", 1500.0, 50)
            )
        ),
        // 11
        RawEventSpec(
            eventId = "banaras-culture-biennale-2026",
            eventName = "CII Banaras Culture Biennale",
            categoryName = "Performing & Visual Arts",
            venue = "Varanasi, Uttar Pradesh",
            date = "2026-11-27",
            time = "17:00",
            description = "A cultural program celebrating India's living heritage through art, traditions and contemporary exchange. 27 Nov to 31 Dec 2026.",
            tiers = listOf(
                RawTierSpec("general", "General", 250.0, 250),
                RawTierSpec("premium", "Premium", 750.0, 120),
                RawTierSpec("vip", "VIP", 1500.0, 30)
            )
        ),
        // 12
        RawEventSpec(
            eventId = "diwali-pahat-2026",
            eventName = "Diwali Pahat",
            categoryName = "Performing & Visual Arts",
            venue = "Cultural auditorium, Madhya Pradesh",
            date = "2026-11-06",
            time = "06:00",
            description = "An early-morning musical celebration of classical music, devotional songs and traditional performances. 6-7 Nov 2026.",
            tiers = listOf(
                RawTierSpec("general", "General", 200.0, 200),
                RawTierSpec("premium", "Premium", 500.0, 80),
                RawTierSpec("vip", "VIP", 1000.0, 20)
            )
        ),
        // 13
        RawEventSpec(
            eventId = "chandigarh-carnival-2026",
            eventName = "Chandigarh Carnival",
            categoryName = "Performing & Visual Arts",
            venue = "Festival grounds, Chandigarh",
            date = "2026-11-27",
            time = "10:00",
            description = "A city carnival with cultural displays, performances, creative exhibits and community activities. 27-30 Nov 2026.",
            tiers = listOf(
                RawTierSpec("general", "General", 0.0, 400),
                RawTierSpec("premium", "Premium", 150.0, 150),
                RawTierSpec("vip", "VIP", 500.0, 50)
            )
        ),
        // 14
        RawEventSpec(
            eventId = "echoes-of-earth-2026",
            eventName = "Echoes of Earth",
            categoryName = "Music",
            venue = "Bengaluru",
            date = "2026-12-05",
            time = "14:00",
            description = "A music festival combining live performances, creative installations and environmental awareness.",
            tiers = listOf(
                RawTierSpec("general", "General", 1500.0, 300),
                RawTierSpec("premium", "Premium", 300.0, 150),
                RawTierSpec("vip", "VIP", 6000.0, 50)
            )
        ),
        // 15
        RawEventSpec(
            eventId = "sunburn-2026",
            eventName = "Sunburn Festival",
            categoryName = "Music",
            venue = "Goa",
            date = "2026-12-27",
            time = "16:00",
            description = "An electronic dance music festival with DJs, large-scale stage production and live music (demo listing).",
            tiers = listOf(
                RawTierSpec("general", "General", 2000.0, 300),
                RawTierSpec("premium", "Premium", 5000.0, 150),
                RawTierSpec("vip", "VIP", 10000.0, 50)
            )
        ),
        // 16
        RawEventSpec(
            eventId = "lollapalooza-india-2026",
            eventName = "Lollapalooza India",
            categoryName = "Music",
            venue = "Mumbai",
            date = "2026-12-12",
            time = "14:00",
            description = "A multi-artist music festival with international and Indian performers across multiple stages (demo listing).",
            tiers = listOf(
                RawTierSpec("general", "General", 2500.0, 300),
                RawTierSpec("premium", "Premium", 6000.0, 150),
                RawTierSpec("vip", "VIP", 12000.0, 50)
            )
        ),
        // 17
        RawEventSpec(
            eventId = "mysuru-dasara-showcase-2026",
            eventName = "Mysuru Dasara Cultural Showcase",
            categoryName = "Performing & Visual Arts",
            venue = "Mysuru Palace grounds, Mysuru",
            date = "2026-11-15",
            time = "18:00",
            description = "A showcase inspired by Mysuru's royal heritage, cultural performances, music and traditional arts (demo listing).",
            tiers = listOf(
                RawTierSpec("general", "General", 200.0, 300),
                RawTierSpec("premium", "Premium", 500.0, 150),
                RawTierSpec("vip", "VIP", 1000.0, 50)
            )
        ),
        // 18
        RawEventSpec(
            eventId = "street-food-festival-2026",
            eventName = "Indian Street Food Festival",
            categoryName = "Food & Drink",
            venue = "Exhibition grounds, Ahmedabad",
            date = "2026-12-20",
            time = "11:00",
            description = "Regional Indian street food, tasting stalls, culinary demos and local vendors (demo listing).",
            tiers = listOf(
                RawTierSpec("general", "General", 199.0, 300),
                RawTierSpec("food-pass", "Food Pass", 599.0, 150),
                RawTierSpec("vip", "VIP", 999.0, 50)
            )
        ),
        // 19
        RawEventSpec(
            eventId = "art-craft-workshop-2026",
            eventName = "Art & Craft Workshop",
            categoryName = "Hobbies",
            venue = "Community arts studio, Surat",
            date = "2026-11-22",
            time = "10:00",
            description = "A hands-on creative workshop with guided painting, pottery and craft activities for beginners (demo listing).",
            tiers = listOf(
                RawTierSpec("basic", "Basic", 299.0, 30),
                RawTierSpec("materials-included", "Materials Included", 599.0, 15),
                RawTierSpec("premium", "Premium", 999.0, 5)
            )
        ),
        // 20
        RawEventSpec(
            eventId = "new-year-celebration-2026",
            eventName = "New Year Cultural Celebration",
            categoryName = "Holiday",
            venue = "Surat city event venue",
            date = "2026-12-31",
            time = "19:00",
            description = "A year-end gathering with music, family activities, cultural performances and a countdown (demo listing).",
            tiers = listOf(
                RawTierSpec("general", "General", 499.0, 300),
                RawTierSpec("premium", "Premium", 1499.0, 150),
                RawTierSpec("vip", "VIP", 2999.0, 50)
            )
        )
    )

    /**
     * Converts a raw YYYY-MM-DD date to "dd MMM yyyy", matching AddEditEventActivity format.
     */
    fun formatDate(rawDate: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
            val parsed = inputFormat.parse(rawDate)
            if (parsed != null) outputFormat.format(parsed) else rawDate
        } catch (e: Exception) {
            rawDate
        }
    }

    /**
     * Converts a raw HH:mm 24h time to "hh:mm a", matching AddEditEventActivity format.
     */
    fun formatTime(rawTime: String): String {
        return try {
            val inputFormat = SimpleDateFormat("HH:mm", Locale.ENGLISH)
            val outputFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
            val parsed = inputFormat.parse(rawTime)
            if (parsed != null) outputFormat.format(parsed).uppercase() else rawTime
        } catch (e: Exception) {
            rawTime
        }
    }

    /**
     * Executes the one-time seeding of the 20 events.
     * Overwrites existing documents if re-run.
     */
    suspend fun seed20Events(
        firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    ): SeederResult = withContext(Dispatchers.IO) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            val msg = "User must be authenticated (Admin) to execute seeder."
            Log.e(TAG, msg)
            return@withContext SeederResult.Error(msg)
        }

        try {
            val categoriesCollection = firestore.collection(CategoryRepository.COLLECTION_CATEGORIES)

            // STEP 1: Query current categories in Firestore
            val initialCategorySnapshot = categoriesCollection.get().awaitTask()
            val existingCategories = initialCategorySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.apply {
                    if (categoryId.isBlank()) {
                        categoryId = doc.id
                    }
                }
            }

            Log.d(TAG, "Initial categories in Firestore: ${existingCategories.map { "${it.categoryId}:${it.categoryName}" }}")

            // STEP 2: Create any missing required categories with fixed IDs and exact names
            for (required in REQUIRED_CATEGORIES) {
                val exists = existingCategories.any {
                    it.categoryId.equals(required.categoryId, ignoreCase = true) ||
                    it.categoryName.trim().equals(required.categoryName.trim(), ignoreCase = true) ||
                    (required.categoryId == "cat_holidays" && it.categoryName.trim().equals("Holidays", ignoreCase = true))
                }

                if (!exists) {
                    val newCategory = Category(
                        categoryId = required.categoryId,
                        categoryName = required.categoryName,
                        iconUrl = required.iconUrl
                    )
                    Log.d(TAG, "Creating missing category: ${required.categoryId} -> '${required.categoryName}'")
                    // Wait for write to finish
                    categoriesCollection.document(required.categoryId).set(newCategory).awaitTask()
                    Log.d(TAG, "Created category document '${required.categoryId}'")
                }
            }

            // STEP 3: Re-query categories after creating any missing ones
            val refreshedCategorySnapshot = categoriesCollection.get().awaitTask()
            val finalCategories = refreshedCategorySnapshot.documents.mapNotNull { doc ->
                doc.toObject(Category::class.java)?.apply {
                    if (categoryId.isBlank()) {
                        categoryId = doc.id
                    }
                }
            }

            val categoryIdMap = mutableMapOf<String, String>()
            val missingCategories = mutableListOf<String>()

            for (required in REQUIRED_CATEGORIES) {
                val matched = finalCategories.firstOrNull {
                    it.categoryId.equals(required.categoryId, ignoreCase = true) ||
                    it.categoryName.trim().equals(required.categoryName.trim(), ignoreCase = true) ||
                    (required.categoryId == "cat_holidays" && it.categoryName.trim().equals("Holidays", ignoreCase = true))
                }

                if (matched != null && matched.categoryId.isNotBlank()) {
                    categoryIdMap[required.categoryName] = matched.categoryId
                    if (required.categoryName == "Holiday") {
                        categoryIdMap["Holidays"] = matched.categoryId
                    }
                    Log.d(TAG, "Resolved category '${required.categoryName}' -> '${matched.categoryId}'")
                } else {
                    missingCategories.add(required.categoryName)
                }
            }

            if (missingCategories.isNotEmpty()) {
                val errorMsg = "Seeder halted! Could not resolve categories: $missingCategories"
                Log.e(TAG, errorMsg)
                return@withContext SeederResult.MissingCategories(
                    missing = missingCategories,
                    availableInFirestore = finalCategories.map { it.categoryName }
                )
            }

            // STEP 4: Seed the 20 events
            val eventsCollection = firestore.collection(EventRepository.COLLECTION_EVENTS)
            var writtenCount = 0
            val writtenIds = mutableListOf<String>()
            val failures = mutableMapOf<String, String>()

            for (spec in EVENT_SPECS) {
                val categoryId = categoryIdMap[spec.categoryName]
                if (categoryId.isNullOrBlank()) {
                    val err = "Category '${spec.categoryName}' missing in mapping"
                    failures[spec.eventId] = err
                    Log.e(TAG, "Skipping event ${spec.eventId}: $err")
                    continue
                }

                val tiers = spec.tiers.map { tierSpec ->
                    TicketTier(
                        tierId = tierSpec.tierId,
                        tierName = tierSpec.tierName,
                        price = tierSpec.price,
                        totalSeats = tierSpec.totalSeats,
                        availableSeats = tierSpec.totalSeats // availableSeats = totalSeats for every tier
                    )
                }

                val lowestPrice = tiers.minOfOrNull { it.price } ?: 0.0
                val sumTotalSeats = tiers.sumOf { it.totalSeats }
                val sumAvailableSeats = sumTotalSeats

                val event = Event(
                    eventId = spec.eventId,
                    eventName = spec.eventName,
                    categoryId = categoryId,
                    description = spec.description,
                    venue = spec.venue,
                    date = formatDate(spec.date),
                    time = formatTime(spec.time),
                    tiers = tiers,
                    bannerImageUrl = BANNER_PLACEHOLDER,
                    status = "active",
                    price = lowestPrice,
                    totalSeats = sumTotalSeats,
                    availableSeats = sumAvailableSeats
                )

                try {
                    // Write with collection("events").document(eventId).set(...) so re-running overwrites
                    eventsCollection.document(spec.eventId).set(event).awaitTask()
                    writtenCount++
                    writtenIds.add(spec.eventId)
                    Log.d(TAG, "Wrote event ${spec.eventId} (${spec.eventName}) under category $categoryId")
                } catch (e: Exception) {
                    val msg = e.message ?: "Firestore set() error"
                    failures[spec.eventId] = msg
                    Log.e(TAG, "Failed writing event ${spec.eventId}: $msg", e)
                }
            }

            Log.i(TAG, "Completed seeding. Written: $writtenCount / ${EVENT_SPECS.size}. Failures: ${failures.size}")

            if (failures.isNotEmpty() && writtenCount == 0) {
                SeederResult.Error("Failed to write any events", failures)
            } else {
                SeederResult.Success(writtenCount, writtenIds)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error executing seeder: ${e.message}", e)
            SeederResult.Error(e.message ?: "Unknown error")
        }
    }
}
