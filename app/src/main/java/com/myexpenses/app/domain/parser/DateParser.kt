package com.myexpenses.app.domain.parser

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

enum class QueryPeriod {
    TODAY,
    YESTERDAY,
    THIS_WEEK,
    THIS_MONTH
}

data class ParsedDateResult(
    val date: LocalDate,
    val timestampMillis: Long,
    val cleanedText: String,
    val wasExplicitDate: Boolean
)

object DateParser {

    fun parseDate(
        input: String,
        referenceDate: LocalDate = LocalDate.now(),
        referenceTime: LocalTime = LocalTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): ParsedDateResult {
        val norm = TextNormalizer.normalizeArabic(TextNormalizer.normalizeDigits(input))
        var resolvedDate = referenceDate
        var explicit = false
        var working = input

        // 1. Day before yesterday
        val dayBeforeYesterdayPatterns = listOf(
            "اول امس", "أول أمس", "اول امبارح", "أول امبارح",
            "قبل امس", "قبل أمس", "قبل البارحه", "قبل البارحة",
            "day before yesterday"
        )
        for (p in dayBeforeYesterdayPatterns) {
            if (norm.contains(TextNormalizer.normalizeArabic(p))) {
                resolvedDate = referenceDate.minusDays(2)
                explicit = true
                working = removePhrase(working, p)
                break
            }
        }

        // 2. "قبل يومين" / "قبل N أيام" / "N days ago"
        if (!explicit) {
            if (norm.contains("قبل يومين") || norm.contains("من يومين")) {
                resolvedDate = referenceDate.minusDays(2)
                explicit = true
                working = working.replace(Regex("(?:قبل|من)\\s+يومين"), " ")
            } else {
                val arDaysAgo = Regex("(?:قبل|من)\\s+(\\d+)\\s+(?:ايام|أيام|يوم)").find(norm)
                val enDaysAgo = Regex("(\\d+)\\s+days?\\s+ago").find(norm)
                val days = arDaysAgo?.groupValues?.get(1)?.toLongOrNull()
                    ?: enDaysAgo?.groupValues?.get(1)?.toLongOrNull()
                if (days != null && days in 1..60) {
                    resolvedDate = referenceDate.minusDays(days)
                    explicit = true
                    working = working
                        .replace(Regex("(?:قبل|من)\\s+\\d+\\s+(?:ايام|أيام|يوم)"), " ")
                        .replace(Regex("\\d+\\s+days?\\s+ago", RegexOption.IGNORE_CASE), " ")
                }
            }
        }

        // 3. Yesterday
        if (!explicit) {
            val yesterdayPatterns = listOf(
                "البارحة", "البارحه", "البارح", "امبارح", "إمبارح", "أمس", "امس", "yesterday"
            )
            for (p in yesterdayPatterns) {
                val regex = Regex("(?<![\\p{L}])${Regex.escape(p)}(?![\\p{L}])", RegexOption.IGNORE_CASE)
                if (regex.containsMatchIn(working) || norm.split(Regex("\\s+")).contains(TextNormalizer.normalizeArabic(p))) {
                    resolvedDate = referenceDate.minusDays(1)
                    explicit = true
                    working = removePhrase(working, p)
                    break
                }
            }
        }

        // 4. Today
        if (!explicit) {
            val todayPatterns = listOf("اليوم", "النهاردة", "النهارده", "today")
            for (p in todayPatterns) {
                if (norm.split(Regex("\\s+")).contains(TextNormalizer.normalizeArabic(p))) {
                    resolvedDate = referenceDate
                    explicit = true
                    working = removePhrase(working, p)
                    break
                }
            }
        }

        // 5. Weekdays ("يوم الأحد", "السبت", "Monday")
        if (!explicit) {
            val weekdayMap = mapOf(
                "السبت" to DayOfWeek.SATURDAY, "saturday" to DayOfWeek.SATURDAY,
                "الاحد" to DayOfWeek.SUNDAY, "الأحد" to DayOfWeek.SUNDAY, "sunday" to DayOfWeek.SUNDAY,
                "الاثنين" to DayOfWeek.MONDAY, "الإثنين" to DayOfWeek.MONDAY, "monday" to DayOfWeek.MONDAY,
                "الثلاثاء" to DayOfWeek.TUESDAY, "الثلاثا" to DayOfWeek.TUESDAY, "tuesday" to DayOfWeek.TUESDAY,
                "الاربعاء" to DayOfWeek.WEDNESDAY, "الأربعاء" to DayOfWeek.WEDNESDAY, "wednesday" to DayOfWeek.WEDNESDAY,
                "الخميس" to DayOfWeek.THURSDAY, "thursday" to DayOfWeek.THURSDAY,
                "الجمعة" to DayOfWeek.FRIDAY, "الجمعه" to DayOfWeek.FRIDAY, "friday" to DayOfWeek.FRIDAY
            )
            for ((token, dow) in weekdayMap) {
                if (norm.contains(TextNormalizer.normalizeArabic(token))) {
                    resolvedDate = mostRecentPastDayOfWeek(referenceDate, dow)
                    explicit = true
                    working = removePhrase(working, "يوم $token")
                    working = removePhrase(working, token)
                    break
                }
            }
        }

        // Keep current time of day if today, or noon for past dates so sorting is predictable
        val targetTime = if (resolvedDate == referenceDate) referenceTime else LocalTime.of(12, 0)
        val millis = resolvedDate.atTime(targetTime).atZone(zoneId).toInstant().toEpochMilli()

        return ParsedDateResult(
            date = resolvedDate,
            timestampMillis = millis,
            cleanedText = working.replace(Regex("\\s+"), " ").trim(),
            wasExplicitDate = explicit
        )
    }

    fun detectQueryPeriod(input: String): QueryPeriod {
        val norm = TextNormalizer.normalizeArabic(input)
        return when {
            norm.contains("هذا الشهر") || norm.contains("الشهر") || norm.contains("شهري") || norm.contains("this month") || norm.contains("month") ->
                QueryPeriod.THIS_MONTH
            norm.contains("هذا الاسبوع") || norm.contains("الاسبوع") || norm.contains("اسبوع") || norm.contains("this week") || norm.contains("week") ->
                QueryPeriod.THIS_WEEK
            norm.contains("امس") || norm.contains("البارح") || norm.contains("امبارح") || norm.contains("yesterday") ->
                QueryPeriod.YESTERDAY
            else -> QueryPeriod.TODAY
        }
    }

    private fun mostRecentPastDayOfWeek(ref: LocalDate, target: DayOfWeek): LocalDate {
        var d = ref
        for (step in 0..6) {
            if (d.dayOfWeek == target) return d
            d = d.minusDays(1)
        }
        return ref
    }

    private fun removePhrase(text: String, phrase: String): String {
        val normPhrase = TextNormalizer.normalizeArabic(phrase)
        val words = text.split(Regex("\\s+")).toMutableList()
        val phraseWords = normPhrase.split(Regex("\\s+"))
        if (phraseWords.size == 1) {
            words.removeAll { TextNormalizer.normalizeArabic(it) == normPhrase }
        } else {
            var idx = 0
            while (idx <= words.size - phraseWords.size) {
                val sliceMatch = phraseWords.indices.all { j ->
                    TextNormalizer.normalizeArabic(words[idx + j]) == phraseWords[j]
                }
                if (sliceMatch) {
                    repeat(phraseWords.size) { words.removeAt(idx) }
                } else {
                    idx++
                }
            }
        }
        return words.joinToString(" ")
    }
}
