package com.myexpenses.app.domain.parser

object TextNormalizer {
    private val arabicDiacritics = Regex("[\u064B-\u065F\u0670\u0640]")

    fun normalizeDigits(input: String): String {
        val sb = StringBuilder(input.length)
        for (ch in input) {
            when (ch) {
                in '٠'..'٩' -> sb.append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> sb.append(('0'.code + (ch.code - '۰'.code)).toChar())
                '٬' -> sb.append(',')
                '٫' -> sb.append('.')
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    fun normalizeArabic(input: String): String {
        val noDiacritics = arabicDiacritics.replace(input, "")
        val sb = StringBuilder(noDiacritics.length)
        for (ch in noDiacritics) {
            when (ch) {
                'أ', 'إ', 'آ', 'ٱ' -> sb.append('ا')
                'ة' -> sb.append('ه')
                'ى' -> sb.append('ي')
                'ؤ' -> sb.append('و')
                'ئ' -> sb.append('ي')
                else -> sb.append(ch)
            }
        }
        return sb.toString().lowercase()
    }

    fun normalizeForMatching(input: String): String {
        return normalizeArabic(normalizeDigits(input))
            .replace(Regex("[^\\p{L}\\p{N}\\s.,]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun stripArabicPrefixes(word: String): List<String> {
        val norm = normalizeArabic(word).trim()
        if (norm.length <= 2) return listOf(norm)
        val forms = linkedSetOf(norm)
        val prefixes = listOf("وبال", "فال", "كال", "بال", "وال", "لل", "ال", "وب", "ب", "و", "ل", "ف")
        for (prefix in prefixes) {
            if (norm.startsWith(prefix) && norm.length - prefix.length >= 2) {
                forms += norm.removePrefix(prefix)
            }
        }
        return forms.toList()
    }

    fun containsArabic(text: String): Boolean =
        text.any { it in '\u0600'..'\u06FF' }
}
