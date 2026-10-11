package com.example.eventnest.models

import com.google.firebase.firestore.Exclude
import java.io.Serializable

data class Event(
    var eventId: String = "",
    var eventName: String = "",
    var categoryId: String = "",
    var description: String = "",
    var venue: String = "",
    var date: String = "",
    var time: String = "",
    var tiers: List<TicketTier> = emptyList(),
    var bannerImageUrl: String = "",
    var status: String = "",
    // Legacy fields retained for backward compatibility with existing Firestore documents
    var price: Double = 0.0,
    var totalSeats: Int = 0,
    var availableSeats: Int = 0
) : Serializable {

    /**
     * Returns the active ticket tiers. If `tiers` is empty, falls back to a single "General"
     * tier constructed from the legacy `price`, `totalSeats`, and `availableSeats`.
     * If legacy values are also unpopulated, generates standard default tiers.
     */
    @get:Exclude
    val effectiveTiers: List<TicketTier>
        get() {
            if (tiers.isNotEmpty()) return tiers
            if (totalSeats > 0 || price > 0.0 || availableSeats > 0) {
                return listOf(
                    TicketTier(
                        tierId = "tier_general",
                        tierName = "General",
                        price = price,
                        totalSeats = totalSeats,
                        availableSeats = availableSeats
                    )
                )
            }
            return defaultTiers()
        }

    /**
     * Starting/lowest price across all effective tiers.
     */
    @get:Exclude
    val minPrice: Double
        get() = effectiveTiers.minOfOrNull { it.price } ?: price

    /**
     * Highest price across all effective tiers.
     */
    @get:Exclude
    val maxPrice: Double
        get() = effectiveTiers.maxOfOrNull { it.price } ?: price

    /**
     * Sum of available seats across all effective tiers.
     */
    @get:Exclude
    val totalAvailableSeats: Int
        get() = if (tiers.isNotEmpty()) tiers.sumOf { it.availableSeats } else availableSeats

    /**
     * Sum of total seats across all effective tiers.
     */
    @get:Exclude
    val totalCapacitySeats: Int
        get() = if (tiers.isNotEmpty()) tiers.sumOf { it.totalSeats } else totalSeats

    companion object {
        fun defaultTiers(basePrice: Double = 499.0, baseCapacity: Int = 100): List<TicketTier> {
            val generalSeats = (baseCapacity * 0.6).toInt().coerceAtLeast(1)
            val vipSeats = (baseCapacity * 0.3).toInt().coerceAtLeast(1)
            val vvipSeats = (baseCapacity - generalSeats - vipSeats).coerceAtLeast(1)

            return listOf(
                TicketTier(
                    tierId = "tier_general",
                    tierName = "General",
                    price = basePrice,
                    totalSeats = generalSeats,
                    availableSeats = generalSeats
                ),
                TicketTier(
                    tierId = "tier_vip",
                    tierName = "VIP",
                    price = basePrice * 2.0,
                    totalSeats = vipSeats,
                    availableSeats = vipSeats
                ),
                TicketTier(
                    tierId = "tier_vvip",
                    tierName = "VVIP",
                    price = basePrice * 3.5,
                    totalSeats = vvipSeats,
                    availableSeats = vvipSeats
                )
            )
        }
    }
}
