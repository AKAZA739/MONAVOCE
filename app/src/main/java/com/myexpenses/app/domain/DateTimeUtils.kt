package com.myexpenses.app.domain

import com.myexpenses.app.data.preferences.CurrencyOption
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

data class TimeRange(val startMillis: Long, val endMillis: Long)

object DateTimeUtils {

    fun dayRange(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): TimeRange {
        val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return TimeRange(start, end)
    }

    fun todayRange(zoneId: ZoneId = ZoneId.systemDefault()): TimeRange =
        dayRange(LocalDate.now(zoneId), zoneId)

    fun yesterdayRange(zoneId: ZoneId = ZoneId.systemDefault()): TimeRange =
        dayRange(LocalDate.now(zoneId).minusDays(1), zoneId)

    fun thisWeekRange(zoneId: ZoneId = ZoneId.systemDefault()): TimeRange {
        val today = LocalDate.now(zoneId)
        // Week starts on Saturday (standard in Yemen & MENA)
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
        val start = startOfWeek.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = startOfWeek.plusDays(7).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return TimeRange(start, end)
    }

    fun thisMonthRange(zoneId: ZoneId = ZoneId.systemDefault()): TimeRange {
        val today = LocalDate.now(zoneId)
        val firstOfMonth = today.withDayOfMonth(1)
        val start = firstOfMonth.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = firstOfMonth.plusMonths(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return TimeRange(start, end)
    }

    fun formatAmount(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        return if (amount % 1.0 == 0.0) {
            DecimalFormat("#,##0", symbols).format(amount)
        } else {
            DecimalFormat("#,##0.##", symbols).format(amount)
        }
    }

    fun formatCurrency(amount: Double, currency: CurrencyOption, isArabic: Boolean): String {
        val num = formatAmount(amount)
        return if (isArabic) "$num ${currency.symbolAr}" else "$num ${currency.symbolEn}"
    }

    fun formatDayHeader(date: LocalDate, isArabic: Boolean): String {
        val today = LocalDate.now()
        return when (date) {
            today -> if (isArabic) "اليوم" else "Today"
            today.minusDays(1) -> if (isArabic) "أمس" else "Yesterday"
            else -> {
                val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
                date.format(DateTimeFormatter.ofPattern("EEE، d MMM yyyy", locale))
            }
        }
    }

    fun formatTime(timestampMillis: Long, isArabic: Boolean): String {
        val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("h:mm a", locale))
    }

    fun toLocalDate(timestampMillis: Long): LocalDate =
        Instant.ofEpochMilli(timestampMillis).atZone(ZoneId.systemDefault()).toLocalDate()
}
