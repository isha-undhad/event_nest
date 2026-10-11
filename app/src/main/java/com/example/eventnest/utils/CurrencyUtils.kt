package com.example.eventnest.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object CurrencyUtils {

    /**
     * Formats a monetary amount using the Indian Rupee symbol (₹).
     *
     * Examples:
     * - 0.0 -> "₹0"
     * - 499.0 -> "₹499"
     * - 1250.0 -> "₹1,250"
     * - 25000.0 -> "₹25,000"
     * - 499.50 -> "₹499.50"
     */
    fun formatCurrency(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }

        val pattern = if (amount % 1.0 == 0.0) {
            "₹#,##0"
        } else {
            "₹#,##0.00"
        }

        val decimalFormat = DecimalFormat(pattern, symbols)
        return decimalFormat.format(amount)
    }

    fun formatCurrency(amount: Int): String = formatCurrency(amount.toDouble())

    fun formatCurrency(amount: Long): String = formatCurrency(amount.toDouble())
}

/**
 * Global helper function to format currency anywhere in the application.
 */
fun formatCurrency(amount: Double): String = CurrencyUtils.formatCurrency(amount)
fun formatCurrency(amount: Int): String = CurrencyUtils.formatCurrency(amount)
fun formatCurrency(amount: Long): String = CurrencyUtils.formatCurrency(amount)
