package com.example.eventnest

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CardValidationTest {

    private fun createMockCalendar(year: Int, monthZeroIndexed: Int): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, monthZeroIndexed)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        return cal
    }

    @Test
    fun testCardNumberValidation() {
        val validCard = "4111 2222 3333 4444"
        val rawDigits = validCard.replace(" ", "")
        assertTrue(rawDigits.length == 16 && rawDigits.all { it.isDigit() })

        val shortCard = "4111 2222 3333"
        val shortDigits = shortCard.replace(" ", "")
        assertFalse(shortDigits.length == 16)

        val nonDigitCard = "4111 2222 3333 444A"
        val nonDigitRaw = nonDigitCard.replace(" ", "")
        assertFalse(nonDigitRaw.all { it.isDigit() })
    }

    @Test
    fun testExpiryValidation_FutureAndCurrentDates() {
        // Current date: September 2026
        val cal2026Sep = createMockCalendar(2026, Calendar.SEPTEMBER)

        // Future dates in 2026 or later
        assertTrue("12/28 should be valid", CardPaymentActivity.isValidExpiry("12/28", cal2026Sep))
        assertTrue("09/26 should be valid (current month)", CardPaymentActivity.isValidExpiry("09/26", cal2026Sep))
        assertTrue("10/26 should be valid (future month)", CardPaymentActivity.isValidExpiry("10/26", cal2026Sep))
        assertTrue("01/30 should be valid (future year)", CardPaymentActivity.isValidExpiry("01/30", cal2026Sep))
    }

    @Test
    fun testExpiryValidation_PastDates() {
        val cal2026Sep = createMockCalendar(2026, Calendar.SEPTEMBER)

        // Past dates in 2026 or earlier
        assertFalse("08/26 should be expired", CardPaymentActivity.isValidExpiry("08/26", cal2026Sep))
        assertFalse("12/25 should be expired", CardPaymentActivity.isValidExpiry("12/25", cal2026Sep))
        assertFalse("01/24 should be expired", CardPaymentActivity.isValidExpiry("01/24", cal2026Sep))
    }

    @Test
    fun testExpiryValidation_InvalidFormatAndMonths() {
        val cal2026Sep = createMockCalendar(2026, Calendar.SEPTEMBER)

        assertFalse("Empty string is invalid", CardPaymentActivity.isValidExpiry("", cal2026Sep))
        assertFalse("13/28 month > 12 is invalid", CardPaymentActivity.isValidExpiry("13/28", cal2026Sep))
        assertFalse("00/28 month 0 is invalid", CardPaymentActivity.isValidExpiry("00/28", cal2026Sep))
        assertFalse("1228 missing slash is invalid", CardPaymentActivity.isValidExpiry("1228", cal2026Sep))
        assertFalse("abc/28 non-digit is invalid", CardPaymentActivity.isValidExpiry("abc/28", cal2026Sep))
    }

    @Test
    fun testCvvValidation() {
        val validCvv = "123"
        assertTrue(validCvv.length == 3 && validCvv.all { it.isDigit() })

        val shortCvv = "12"
        assertFalse(shortCvv.length == 3)

        val longCvv = "1234"
        assertFalse(longCvv.length == 3)

        val nonNumericCvv = "12a"
        assertFalse(nonNumericCvv.all { it.isDigit() })
    }
}
