package com.example.eventnest

import com.example.eventnest.utils.formatCurrency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyUtilsTest {

    @Test
    fun testCurrencyFormatting_ZeroAmount() {
        val result = formatCurrency(0.0)
        assertEquals("₹0", result)
    }

    @Test
    fun testCurrencyFormatting_StandardAmounts() {
        assertEquals("₹499", formatCurrency(499.0))
        assertEquals("₹1,250", formatCurrency(1250.0))
        assertEquals("₹25,000", formatCurrency(25000.0))
        assertEquals("₹100,000", formatCurrency(100000.0))
    }

    @Test
    fun testCurrencyFormatting_DecimalAmounts() {
        assertEquals("₹499.50", formatCurrency(499.5))
        assertEquals("₹1,250.75", formatCurrency(1250.75))
    }

    @Test
    fun testCurrencyFormatting_IntAndLongOverloads() {
        assertEquals("₹499", formatCurrency(499))
        assertEquals("₹1,250", formatCurrency(1250L))
    }

    @Test
    fun testCurrencyFormatting_NoDollarSignEver() {
        val testValues = listOf(0.0, 10.0, 499.0, 1250.0, 99999.0)
        for (value in testValues) {
            val formatted = formatCurrency(value)
            assertFalse("Formatted currency must never contain a dollar sign ($)", formatted.contains("$"))
            assertTrue("Formatted currency must start with Rupee symbol (₹)", formatted.startsWith("₹"))
        }
    }
}
