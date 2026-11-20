package com.example.ethfinance.core.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {

    val ADDIS_TIME_ZONE: TimeZone = TimeZone.getTimeZone("Africa/Addis_Ababa")

    fun formatRelativeTime(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val diffMillis = Math.max(0L, nowMillis - epochMillis)
        val minutes = diffMillis / (60 * 1000)
        val hours = minutes / 60
        val days = hours / 24

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "$days days ago"
            else -> formatDateShort(epochMillis)
        }
    }

    fun formatDateGroup(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val calThen = Calendar.getInstance(ADDIS_TIME_ZONE).apply { timeInMillis = epochMillis }
        val calNow = Calendar.getInstance(ADDIS_TIME_ZONE).apply { timeInMillis = nowMillis }

        val thenYear = calThen.get(Calendar.YEAR)
        val thenDayOfYear = calThen.get(Calendar.DAY_OF_YEAR)

        val nowYear = calNow.get(Calendar.YEAR)
        val nowDayOfYear = calNow.get(Calendar.DAY_OF_YEAR)

        return when {
            thenYear == nowYear && thenDayOfYear == nowDayOfYear -> "Today"
            thenYear == nowYear && thenDayOfYear == nowDayOfYear - 1 -> "Yesterday"
            else -> {
                val sdf = SimpleDateFormat("EEE, dd MMM", Locale.US).apply {
                    timeZone = ADDIS_TIME_ZONE
                }
                sdf.format(Date(epochMillis))
            }
        }
    }

    fun formatDateShort(epochMillis: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.US).apply {
            timeZone = ADDIS_TIME_ZONE
        }
        return sdf.format(Date(epochMillis))
    }

    fun formatTime(epochMillis: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.US).apply {
            timeZone = ADDIS_TIME_ZONE
        }
        return sdf.format(Date(epochMillis))
    }

    data class DateRange(val startEpochMillis: Long, val endEpochMillis: Long)

    fun getPeriodRange(period: PeriodType, referenceMillis: Long = System.currentTimeMillis()): DateRange {
        val cal = Calendar.getInstance(ADDIS_TIME_ZONE).apply {
            timeInMillis = referenceMillis
        }

        return when (period) {
            PeriodType.DAYS_7 -> {
                val end = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -7)
                DateRange(cal.timeInMillis, end)
            }
            PeriodType.DAYS_30 -> {
                val end = cal.timeInMillis
                cal.add(Calendar.DAY_OF_YEAR, -30)
                DateRange(cal.timeInMillis, end)
            }
            PeriodType.THIS_MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis - 1
                DateRange(start, end)
            }
            PeriodType.LAST_MONTH -> {
                cal.add(Calendar.MONTH, -1)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                val start = cal.timeInMillis
                cal.add(Calendar.MONTH, 1)
                val end = cal.timeInMillis - 1
                DateRange(start, end)
            }
            PeriodType.CUSTOM -> {
                DateRange(0, Long.MAX_VALUE)
            }
        }
    }

    enum class PeriodType {
        DAYS_7, DAYS_30, THIS_MONTH, LAST_MONTH, CUSTOM
    }
}
