package com.example.ethfinance.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatterTest {

    @Test
    fun formatSantim_regularAmount_formatsCorrectly() {
        val result = MoneyFormatter.formatSantim(1083484L)
        assertEquals("10,834.84 ETB", result)
    }

    @Test
    fun formatSantim_negativeAmount_formatsWithMinus() {
        val result = MoneyFormatter.formatSantim(-67626L)
        assertEquals("-676.26 ETB", result)
    }

    @Test
    fun formatSantim_withPositiveSign_formatsWithPlus() {
        val result = MoneyFormatter.formatSantim(400000L, includeSign = true)
        assertEquals("+4,000.00 ETB", result)
    }

    @Test
    fun formatSantim_zero_formatsZero() {
        val result = MoneyFormatter.formatSantim(0L)
        assertEquals("0.00 ETB", result)
    }

    @Test
    fun formatSantim_masked_returnsMaskedDots() {
        val result = MoneyFormatter.formatSantim(1083484L, masked = true)
        assertEquals("••••", result)
    }

    @Test
    fun parseToSantim_variousFormats_parsedCorrectly() {
        assertEquals(125050L, MoneyFormatter.parseToSantim("1,250.50 ETB"))
        assertEquals(125050L, MoneyFormatter.parseToSantim("ETB 1,250.50"))
        assertEquals(20000L, MoneyFormatter.parseToSantim("Birr 200"))
        assertEquals(20000L, MoneyFormatter.parseToSantim("200.00Birr"))
        assertEquals(14076L, MoneyFormatter.parseToSantim("140.76"))
        assertNull(MoneyFormatter.parseToSantim("invalid text"))
        assertNull(MoneyFormatter.parseToSantim(null))
    }
}
