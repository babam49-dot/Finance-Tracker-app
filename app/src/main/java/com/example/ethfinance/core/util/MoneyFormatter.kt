package com.example.ethfinance.core.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

object MoneyFormatter {

    enum class CurrencySymbol(val label: String) {
        ETB("ETB"),
        BIRR("Birr"),
        AMHARIC_BIRR("ብር")
    }

    private val decimalFormat = DecimalFormat("#,##0.00", DecimalFormatSymbols(Locale.US))

    /**
     * Converts minor units (santim) to formatted currency string.
     * Example: 1083484L -> "10,834.84 ETB"
     */
    fun formatSantim(
        santim: Long,
        currencySymbol: CurrencySymbol = CurrencySymbol.ETB,
        includeSign: Boolean = false,
        masked: Boolean = false
    ): String {
        if (masked) return "••••"

        val isNegative = santim < 0
        val absSantim = Math.abs(santim)
        val majorUnits = absSantim / 100
        val minorUnits = absSantim % 100

        val formattedAmount = decimalFormat.format(majorUnits + (minorUnits / 100.0))

        val signPrefix = when {
            isNegative -> "-"
            includeSign && santim > 0 -> "+"
            else -> ""
        }

        return "$signPrefix$formattedAmount ${currencySymbol.label}"
    }

    /**
     * Converts a string like "1,250.50", "ETB 1,250.50", "Birr 200", or "200.00Birr" to santim.
     */
    fun parseToSantim(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        val cleaned = text
            .replace("ETB", "", ignoreCase = true)
            .replace("Birr", "", ignoreCase = true)
            .replace("ብር", "")
            .replace(",", "")
            .trim()

        return try {
            val doubleVal = cleaned.toDouble()
            Math.round(doubleVal * 100.0)
        } catch (e: Exception) {
            null
        }
    }
}
