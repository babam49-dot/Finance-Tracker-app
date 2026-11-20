package com.example.ethfinance.core.util

/**
 * Astronomical converter between Gregorian and Ethiopian (Ge'ez) calendars.
 * Uses Julian Day Number (JDN) formulas.
 */
object EthiopianCalendar {

    data class EthiopicDate(
        val year: Int,
        val month: Int, // 1 = Meskerem, 13 = Pagume
        val day: Int
    ) {
        val monthNameEnglish: String
            get() = MONTH_NAMES_EN.getOrElse(month - 1) { "Unknown" }

        val monthNameAmharic: String
            get() = MONTH_NAMES_AM.getOrElse(month - 1) { "ያልታወቀ" }

        fun format(useAmharic: Boolean = false): String {
            val m = if (useAmharic) monthNameAmharic else monthNameEnglish
            return "$day $m $year"
        }
    }

    val MONTH_NAMES_EN = listOf(
        "Meskerem", "Tikimt", "Hidar", "Tahsas", "Tir", "Yekatit",
        "Megabit", "Miazia", "Ginbot", "Sene", "Hamle", "Nehase", "Pagume"
    )

    val MONTH_NAMES_AM = listOf(
        "መስከረም", "ጥቅምት", "ኅዳር", "ታኅሣሥ", "ጥር", "የካቲት",
        "መጋቢት", "ሚያዝያ", "ግንቦት", "ሰኔ", "ሐምሌ", "ነሐሴ", "ጳጉሜ"
    )

    /**
     * Converts Gregorian year, month (1..12), day (1..31) to Ethiopian date.
     */
    fun gregorianToEthiopian(year: Int, month: Int, day: Int): EthiopicDate {
        val jdn = gregorianToJdn(year, month, day)
        return jdnToEthiopian(jdn)
    }

    /**
     * Converts Ethiopian year, month (1..13), day (1..30 or 1..6) to Gregorian date (Triple of year, month, day).
     */
    fun ethiopianToGregorian(year: Int, month: Int, day: Int): Triple<Int, Int, Int> {
        val jdn = ethiopianToJdn(year, month, day)
        return jdnToGregorian(jdn)
    }

    fun isEthiopianLeapYear(year: Int): Boolean {
        return (year % 4) == 3
    }

    private fun gregorianToJdn(year: Int, month: Int, day: Int): Long {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + (153 * m + 2) / 5 + 365L * y + y / 4 - y / 100 + y / 400 - 32045
    }

    private fun jdnToGregorian(jdn: Long): Triple<Int, Int, Int> {
        val a = jdn + 32044
        val b = (4 * a + 3) / 146097
        val c = a - (146097 * b) / 4
        val d = (4 * c + 3) / 1461
        val e = c - (1461 * d) / 4
        val m = (5 * e + 2) / 153
        val day = (e - (153 * m + 2) / 5 + 1).toInt()
        val month = (m + 3 - 12 * (m / 10)).toInt()
        val year = (100 * b + d - 4800 + (m / 10)).toInt()
        return Triple(year, month, day)
    }

    private const val JD_EPOCH_OFFSET_AMETE_MIHRET = 1723856L

    private fun jdnToEthiopian(jdn: Long): EthiopicDate {
        val r = (jdn - JD_EPOCH_OFFSET_AMETE_MIHRET) % 1461
        val n = (r % 365) + 365 * (r / 1460)
        val year = 4 * ((jdn - JD_EPOCH_OFFSET_AMETE_MIHRET) / 1461) + (r / 365) - (r / 1460)
        val month = (n / 30) + 1
        val day = (n % 30) + 1
        return EthiopicDate(year.toInt(), month.toInt(), day.toInt())
    }

    private fun ethiopianToJdn(year: Int, month: Int, day: Int): Long {
        return (JD_EPOCH_OFFSET_AMETE_MIHRET + 365L * (year - 1) + (year / 4) + 30L * (month - 1) + day - 1)
    }
}
