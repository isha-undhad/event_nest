package com.example.eventnest

import com.example.eventnest.utils.EventSeatHelper
import com.example.eventnest.utils.SeatValidationResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EventSeatsCalculationTest {

    @Test
    fun testCalculateBookedSeats() {
        assertEquals(60, EventSeatHelper.calculateBookedSeats(totalSeats = 100, availableSeats = 40))
        assertEquals(0, EventSeatHelper.calculateBookedSeats(totalSeats = 500, availableSeats = 500))
        assertEquals(50, EventSeatHelper.calculateBookedSeats(totalSeats = 50, availableSeats = 0))
    }

    @Test
    fun testCalculateNewAvailableSeats_IncreaseTotalSeats() {
        val originalTotal = 100
        val originalAvailable = 40
        val booked = EventSeatHelper.calculateBookedSeats(originalTotal, originalAvailable) // 60

        // Admin increases total seats by 20 (100 -> 120)
        val newTotal = 120
        val newAvailable = EventSeatHelper.calculateNewAvailableSeats(newTotal, booked)

        // Available seats should increase by the same delta (+20): 40 + 20 = 60
        assertEquals(60, newAvailable)
        assertEquals(originalAvailable + (newTotal - originalTotal), newAvailable)
    }

    @Test
    fun testCalculateNewAvailableSeats_DecreaseTotalSeatsSafely() {
        val originalTotal = 100
        val originalAvailable = 40
        val booked = EventSeatHelper.calculateBookedSeats(originalTotal, originalAvailable) // 60

        // Admin decreases total seats by 20 (100 -> 80), which is still >= 60 booked
        val newTotal = 80
        val newAvailable = EventSeatHelper.calculateNewAvailableSeats(newTotal, booked)

        // Available seats should decrease by 20: 40 - 20 = 20
        assertEquals(20, newAvailable)
    }

    @Test
    fun testValidateTotalSeats_ValidIncreasesAndSafeDecreases() {
        val booked = 60

        // Increasing total seats
        val resIncrease = EventSeatHelper.validateTotalSeats(150, booked)
        assertTrue(resIncrease is SeatValidationResult.Valid)
        assertEquals(90, (resIncrease as SeatValidationResult.Valid).newAvailableSeats)

        // Decreasing total seats safely
        val resSafeDecrease = EventSeatHelper.validateTotalSeats(75, booked)
        assertTrue(resSafeDecrease is SeatValidationResult.Valid)
        assertEquals(15, (resSafeDecrease as SeatValidationResult.Valid).newAvailableSeats)

        // Decreasing total seats to exactly booked count (becomes sold out)
        val resSoldOut = EventSeatHelper.validateTotalSeats(60, booked)
        assertTrue(resSoldOut is SeatValidationResult.Valid)
        assertEquals(0, (resSoldOut as SeatValidationResult.Valid).newAvailableSeats)
    }

    @Test
    fun testValidateTotalSeats_InvalidWhenBelowBookedTickets() {
        val booked = 60

        // Trying to reduce total seats to 50 when 60 tickets are already booked
        val resBelowBooked = EventSeatHelper.validateTotalSeats(50, booked)
        assertTrue(resBelowBooked is SeatValidationResult.Invalid)
        val errMsg = (resBelowBooked as SeatValidationResult.Invalid).errorMessage
        assertTrue(errMsg.contains("cannot be less than already booked tickets"))
        assertTrue(errMsg.contains("60"))
    }

    @Test
    fun testValidateTotalSeats_InvalidWhenZeroOrNegative() {
        val booked = 0

        val resZero = EventSeatHelper.validateTotalSeats(0, booked)
        assertTrue(resZero is SeatValidationResult.Invalid)

        val resNegative = EventSeatHelper.validateTotalSeats(-10, booked)
        assertTrue(resNegative is SeatValidationResult.Invalid)
    }

    @Test
    fun testEventEffectiveTiers_LegacyFallback() {
        // Event created with legacy single price/seat fields and empty tiers
        val legacyEvent = com.example.eventnest.models.Event(
            eventId = "evt_legacy",
            eventName = "Classic Rock Night",
            price = 499.0,
            totalSeats = 100,
            availableSeats = 45
        )

        val tiers = legacyEvent.effectiveTiers
        assertEquals(1, tiers.size)
        assertEquals("tier_general", tiers[0].tierId)
        assertEquals("General", tiers[0].tierName)
        assertEquals(499.0, tiers[0].price, 0.001)
        assertEquals(100, tiers[0].totalSeats)
        assertEquals(45, tiers[0].availableSeats)
        assertEquals(499.0, legacyEvent.minPrice, 0.001)
        assertEquals(499.0, legacyEvent.maxPrice, 0.001)
        assertEquals(45, legacyEvent.totalAvailableSeats)
        assertEquals(100, legacyEvent.totalCapacitySeats)
    }

    @Test
    fun testEventEffectiveTiers_MultiTierValues() {
        val multiTierEvent = com.example.eventnest.models.Event(
            eventId = "evt_multi",
            eventName = "Mega Fest",
            tiers = listOf(
                com.example.eventnest.models.TicketTier("tier_gen", "General", 500.0, 100, 40),
                com.example.eventnest.models.TicketTier("tier_vip", "VIP", 1200.0, 50, 15),
                com.example.eventnest.models.TicketTier("tier_vvip", "VVIP", 2500.0, 20, 5)
            )
        )
        assertEquals(3, multiTierEvent.effectiveTiers.size)
        assertEquals(500.0, multiTierEvent.minPrice, 0.001)
        assertEquals(2500.0, multiTierEvent.maxPrice, 0.001)
        assertEquals(60, multiTierEvent.totalAvailableSeats) // 40 + 15 + 5
        assertEquals(170, multiTierEvent.totalCapacitySeats) // 100 + 50 + 20
    }

    @Test
    fun testEventEffectiveTiers_SingleCustomTier() {
        val singleTierEvent = com.example.eventnest.models.Event(
            eventId = "evt_single",
            eventName = "Workshop",
            tiers = listOf(
                com.example.eventnest.models.TicketTier("tier_custom", "Early Bird", 299.0, 50, 30)
            )
        )

        assertEquals(1, singleTierEvent.effectiveTiers.size)
        assertEquals("Early Bird", singleTierEvent.effectiveTiers[0].tierName)
        assertEquals(299.0, singleTierEvent.minPrice, 0.001)
        assertEquals(299.0, singleTierEvent.maxPrice, 0.001)
        assertEquals(30, singleTierEvent.totalAvailableSeats)
        assertEquals(50, singleTierEvent.totalCapacitySeats)
    }

    @Test
    fun testEventEffectiveTiers_TwoCustomTiers() {
        val twoTierEvent = com.example.eventnest.models.Event(
            eventId = "evt_two",
            eventName = "Comedy Night",
            tiers = listOf(
                com.example.eventnest.models.TicketTier("tier_1", "Regular", 350.0, 80, 50),
                com.example.eventnest.models.TicketTier("tier_2", "Front Row", 750.0, 20, 10)
            )
        )

        assertEquals(2, twoTierEvent.effectiveTiers.size)
        assertEquals(350.0, twoTierEvent.minPrice, 0.001)
        assertEquals(750.0, twoTierEvent.maxPrice, 0.001)
        assertEquals(60, twoTierEvent.totalAvailableSeats) // 50 + 10
        assertEquals(100, twoTierEvent.totalCapacitySeats) // 80 + 20
    }
}

