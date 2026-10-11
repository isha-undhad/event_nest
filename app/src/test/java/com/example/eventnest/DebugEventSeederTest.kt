package com.example.eventnest

import com.example.eventnest.debug.DebugEventSeeder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DebugEventSeederTest {

    @Test
    fun testEventCountIsExactlyTwenty() {
        assertEquals("There should be exactly 20 events defined in the seeder", 20, DebugEventSeeder.EVENT_SPECS.size)
    }

    @Test
    fun testEventSlugsAreUnique() {
        val slugs = DebugEventSeeder.EVENT_SPECS.map { it.eventId }
        val uniqueSlugs = slugs.distinct()
        assertEquals("All 20 event slugs must be unique", slugs.size, uniqueSlugs.size)
    }

    @Test
    fun testDateFormatting() {
        assertEquals("22 Oct 2026", DebugEventSeeder.formatDate("2026-10-22"))
        assertEquals("23 Oct 2026", DebugEventSeeder.formatDate("2026-10-23"))
        assertEquals("01 Dec 2026", DebugEventSeeder.formatDate("2026-12-01"))
        assertEquals("31 Dec 2026", DebugEventSeeder.formatDate("2026-12-31"))
    }

    @Test
    fun testTimeFormatting() {
        assertEquals("06:00 PM", DebugEventSeeder.formatTime("18:00"))
        assertEquals("05:00 PM", DebugEventSeeder.formatTime("17:00"))
        assertEquals("10:00 AM", DebugEventSeeder.formatTime("10:00"))
        assertEquals("06:00 AM", DebugEventSeeder.formatTime("06:00"))
        assertEquals("09:00 AM", DebugEventSeeder.formatTime("09:00"))
        assertEquals("07:00 PM", DebugEventSeeder.formatTime("19:00"))
    }

    @Test
    fun testCategoryNamesBelongToAllowedSet() {
        val allowedCategories = setOf(
            "Performing & Visual Arts",
            "Holiday",
            "Hobbies",
            "Music",
            "Food & Drink"
        )

        for (spec in DebugEventSeeder.EVENT_SPECS) {
            assertTrue(
                "Event ${spec.eventId} has unexpected category '${spec.categoryName}'",
                allowedCategories.contains(spec.categoryName)
            )
        }
    }

    @Test
    fun testTiersAndLegacyCalculations() {
        for (spec in DebugEventSeeder.EVENT_SPECS) {
            assertTrue("Event ${spec.eventId} must have at least 1 tier", spec.tiers.isNotEmpty())

            val tierIds = spec.tiers.map { it.tierId }
            assertEquals("Tier IDs for event ${spec.eventId} must be unique", tierIds.size, tierIds.distinct().size)

            val lowestPrice = spec.tiers.minOf { it.price }
            val totalSeats = spec.tiers.sumOf { it.totalSeats }

            assertTrue("Event ${spec.eventId} total seats must be > 0", totalSeats > 0)
            assertTrue("Event ${spec.eventId} lowest price must be >= 0", lowestPrice >= 0.0)

            for (tier in spec.tiers) {
                assertTrue("Tier ID cannot be blank", tier.tierId.isNotBlank())
                assertTrue("Tier name cannot be blank", tier.tierName.isNotBlank())
                assertTrue("Tier price must be >= 0", tier.price >= 0.0)
                assertTrue("Tier seats must be > 0", tier.totalSeats > 0)
            }
        }
    }

    @Test
    fun testRequiredCategoriesHaveFixedIds() {
        val expected = mapOf(
            "cat_arts" to "Performing & Visual Arts",
            "cat_holidays" to "Holiday",
            "cat_hobbies" to "Hobbies",
            "cat_music" to "Music",
            "cat_food" to "Food & Drink"
        )

        assertEquals(expected.size, DebugEventSeeder.REQUIRED_CATEGORIES.size)
        for (req in DebugEventSeeder.REQUIRED_CATEGORIES) {
            assertTrue("Expected fixed ID ${req.categoryId}", expected.containsKey(req.categoryId))
            assertEquals(expected[req.categoryId], req.categoryName)
        }
    }
}
