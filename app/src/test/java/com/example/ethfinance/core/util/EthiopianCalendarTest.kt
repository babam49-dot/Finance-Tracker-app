package com.example.ethfinance.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EthiopianCalendarTest {

    @Test
    fun gregorianToEthiopian_convertsCorrectly() {
        // Enkutatash (Ethiopian New Year): Sep 11, 2024 Gregorian is Meskerem 1, 2017 Ethiopian
        val ethDate = EthiopianCalendar.gregorianToEthiopian(2024, 9, 11)
        assertEquals(2017, ethDate.year)
        assertEquals(1, ethDate.month) // Meskerem
        assertEquals(1, ethDate.day)
    }

    @Test
    fun ethiopianLeapYear_pagume6Calculated() {
        // Ethiopian year 2015 % 4 == 3 -> leap year
        assertTrue(EthiopianCalendar.isEthiopianLeapYear(2015))
    }

    @Test
    fun ethiopicDate_formatNames() {
        val date = EthiopianCalendar.EthiopicDate(2017, 1, 1)
        assertEquals("1 Meskerem 2017", date.format(useAmharic = false))
        assertEquals("1 መስከረም 2017", date.format(useAmharic = true))
    }
}
