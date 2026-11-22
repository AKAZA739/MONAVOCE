package com.myexpenses.app.domain

import com.myexpenses.app.data.local.entity.CurrencyTotal
import com.myexpenses.app.data.preferences.CurrencyOption
import com.myexpenses.app.data.preferences.SupportedCurrencies
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

    fun formatAmount(amount: Double, currency: CurrencyOption): String {
        val symbols = DecimalFormatSymbols(Locale.US)
        val pattern = if (currency.code in setOf("KWD", "JOD")) "#,##0.###" else "#,##0.##"
        return DecimalFormat(pattern, symbols).format(amount)
    }

    fun formatCurrency(amount: Double, currency: CurrencyOption, isArabic: Boolean): String {
        val num = formatAmount(amount, currency)
        return when {
            isArabic -> "$num\u00A0${currency.symbolAr}"
            currency.code == "USD" || currency.code == "EUR" -> "${currency.symbolEn}$num"
            else -> "$num\u00A0${currency.symbolEn}"
        }
    }

    fun formatCurrencyTotals(
        totals: List<CurrencyTotal>,
        defaultCurrency: CurrencyOption,
        isArabic: Boolean,
        separator: String = if (isArabic) "، " else ", "
    ): String = if (totals.isEmpty()) {
        formatCurrency(0.0, defaultCurrency, isArabic)
    } else {
        totals.joinToString(separator) {
            formatCurrency(it.total, SupportedCurrencies.forExpense(it.currencyCode), isArabic)
        }
    }

    fun formatDayHeader(date: LocalDate, isArabic: Boolean): String {
        val today = LocalDate.now()
        return when (date) {
            today -> if (isArabic) "اليوم" else "Today"
            today.minusDays(1) -> if (isArabic) "أمس" else "Yesterday"
            else -> {
                val locale = if (isArabic) Locale("ar") else Locale.ENGLISH
                val pattern = if (isArabic) "EEE، d MMM yyyy" else "EEE, d MMM yyyy"
                date.format(DateTimeFormatter.ofPattern(pattern, locale))
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
