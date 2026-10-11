package com.example.eventnest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiValidationTest {

    private val upiRegex = Regex("""^[\w.\-]{2,}@[a-zA-Z]{2,}$""")

    @Test
    fun testValidUpiIds() {
        val validIds = listOf(
            "user@upi",
            "john.doe@okhdfcbank",
            "alice_99@oksbi",
            "rahul-sharma@icici",
            "test.user@paytm",
            "demo123@ybl"
        )
        for (upi in validIds) {
            assertTrue("Expected '$upi' to be valid", upiRegex.matches(upi))
        }
    }

    @Test
    fun testInvalidUpiIds() {
        val invalidIds = listOf(
            "",
            "user",
            "user@",
            "@upi",
            "a@b", // too short before and after @
            "user@123", // bank handle has digits
            "u@upi", // only 1 char username
            "user@u", // only 1 char bank handle
            "user name@upi" // contains space
        )
        for (upi in invalidIds) {
            assertFalse("Expected '$upi' to be invalid", upiRegex.matches(upi))
        }
    }
}
