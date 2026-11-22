package com.myexpenses.app.domain.parser

import java.util.Locale

/**
 * Converts spoken Arabic (Modern Standard + Yemeni/Gulf/Egyptian/Levantine dialects)
 * and English number words, plus Arabic-Indic / formatted digits, into normalized numeric tokens.
 */
object NumberWordParser {

    private val arabicUnits = mapOf(
        "صفر" to 0.0,
        "واحد" to 1.0, "واحده" to 1.0, "احد" to 1.0, "احدي" to 1.0,
        "اثنين" to 2.0, "اثنان" to 2.0, "اتنين" to 2.0, "تنين" to 2.0, "ثنتين" to 2.0, "اثنتين" to 2.0,
        "ثلاثه" to 3.0, "ثلاث" to 3.0, "ثلاثا" to 3.0, "تلاته" to 3.0, "تلات" to 3.0, "تلاتا" to 3.0,
        "اربعه" to 4.0, "اربع" to 4.0, "اربعا" to 4.0,
        "خمسه" to 5.0, "خمس" to 5.0, "خمسا" to 5.0,
        "سته" to 6.0, "ست" to 6.0, "ستا" to 6.0,
        "سبعه" to 7.0, "سبع" to 7.0, "سبعا" to 7.0,
        "ثمانيه" to 8.0, "ثمان" to 8.0, "ثمانا" to 8.0, "ثمانيا" to 8.0, "تمانيه" to 8.0, "تمان" to 8.0, "ثمن" to 8.0,
        "تسعه" to 9.0, "تسع" to 9.0, "تسعا" to 9.0,
        "عشره" to 10.0, "عشر" to 10.0, "عشرا" to 10.0,
        "حداشر" to 11.0, "احداشر" to 11.0, "احدعشر" to 11.0,
        "اثناشر" to 12.0, "اتناشر" to 12.0, "اطناشر" to 12.0, "اثناعشر" to 12.0, "اثنعشر" to 12.0,
        "ثلاثتاشر" to 13.0, "تلتاشر" to 13.0, "ثلاثطعشر" to 13.0, "ثلاثعشر" to 13.0,
        "اربعتاشر" to 14.0, "اربعطعشر" to 14.0, "اربعطعش" to 14.0,
        "خمستاشر" to 15.0, "خمسطعشر" to 15.0, "خمسطعش" to 15.0,
        "ستاشر" to 16.0, "ستطعشر" to 16.0, "ستطعش" to 16.0,
        "سبعتاشر" to 17.0, "سبعطعشر" to 17.0, "سبعطعش" to 17.0,
        "تمنتاشر" to 18.0, "ثمنتاشر" to 18.0, "ثمانطعشر" to 18.0,
        "تسعتاشر" to 19.0, "تسعطعشر" to 19.0, "تسعطعش" to 19.0
    )

    private val arabicTens = mapOf(
        "عشرين" to 20.0, "عشرون" to 20.0,
        "ثلاثين" to 30.0, "ثلاثون" to 30.0, "تلاتين" to 30.0,
        "اربعين" to 40.0, "اربعون" to 40.0,
        "خمسين" to 50.0, "خمسون" to 50.0,
        "ستين" to 60.0, "ستون" to 60.0,
        "سبعين" to 70.0, "سبعون" to 70.0,
        "ثمانين" to 80.0, "ثمانون" to 80.0, "تمانين" to 80.0,
        "تسعين" to 90.0, "تسعون" to 90.0
    )

    private val arabicHundreds = mapOf(
        "ميه" to 100.0, "مايه" to 100.0, "مئه" to 100.0, "مائه" to 100.0,
        "ميتين" to 200.0, "مايتين" to 200.0, "مئتين" to 200.0, "مائتين" to 200.0, "مئتان" to 200.0,
        "ثلاثميه" to 300.0, "ثلاثمائه" to 300.0, "ثلاثمئه" to 300.0, "تلاتميه" to 300.0, "ثلثميه" to 300.0, "تلتميه" to 300.0,
        "اربعميه" to 400.0, "اربعمائه" to 400.0, "اربعمئه" to 400.0,
        "خمسميه" to 500.0, "خمسمائه" to 500.0, "خمسمئه" to 500.0,
        "ستميه" to 600.0, "ستمائه" to 600.0, "ستمئه" to 600.0,
        "سبعميه" to 700.0, "سبعمائه" to 700.0, "سبعمئه" to 700.0,
        "ثمانميه" to 800.0, "ثمانمائه" to 800.0, "ثمنميه" to 800.0, "تمنميه" to 800.0, "تمانميه" to 800.0,
        "تسعميه" to 900.0, "تسعمائه" to 900.0, "تسعمئه" to 900.0
    )

    private val arabicDualScales = mapOf(
        "الفين" to 2000.0, "الفان" to 2000.0,
        "مليونين" to 2_000_000.0, "مليونان" to 2_000_000.0
    )

    private val arabicMultipliers = mapOf(
        "الف" to 1000.0, "الاف" to 1000.0,
        "مليون" to 1_000_000.0, "ملايين" to 1_000_000.0
    )

    private val arabicFractions = mapOf(
        "نص" to 0.5, "نصف" to 0.5,
        "ربع" to 0.25,
        "ثلث" to 0.3333
    )

    private val englishUnits = mapOf(
        "zero" to 0.0, "one" to 1.0, "a" to 1.0, "an" to 1.0,
        "two" to 2.0, "three" to 3.0, "four" to 4.0, "five" to 5.0,
        "six" to 6.0, "seven" to 7.0, "eight" to 8.0, "nine" to 9.0,
        "ten" to 10.0, "eleven" to 11.0, "twelve" to 12.0, "thirteen" to 13.0,
        "fourteen" to 14.0, "fifteen" to 15.0, "sixteen" to 16.0, "seventeen" to 17.0,
        "eighteen" to 18.0, "nineteen" to 19.0
    )

    private val englishTens = mapOf(
        "twenty" to 20.0, "thirty" to 30.0, "forty" to 40.0, "fifty" to 50.0,
        "sixty" to 60.0, "seventy" to 70.0, "eighty" to 80.0, "ninety" to 90.0
    )

    private val englishMultipliers = mapOf(
        "hundred" to 100.0,
        "thousand" to 1000.0,
        "million" to 1_000_000.0
    )

    private val digitRegex = Regex("""^(\d+(?:,\d{3})*(?:\.\d+)?|\d+(?:\.\d+)?)([kK])?$""")

    data class Token(
        val original: String,
        val normalized: String,
        val hadLeadingWaw: Boolean = false,
        val hadLeadingBa: Boolean = false
    )

    /**
     * Replaces all numeric expressions (spoken Arabic/English + digits) with clean western
     * numeric strings while preserving clause separators (like leading "و" before a new expense).
     */
    fun normalizeNumbersInText(rawInput: String): String {
        val digitNormalized = TextNormalizer.normalizeDigits(rawInput)
            .replace("،", " , ")
            .replace(",", ",")
            .replace(Regex("(?<=\\d),(?=\\d{3}\\b)"), "") // 10,000 -> 10000
            .replace(",", " , ")
            .replace("؟", " ")
            .replace("?", " ")
            .replace("!", " ")
            .replace("؛", " , ")
            .replace(";", " , ")

        val rawWords = digitNormalized.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (rawWords.isEmpty()) return ""

        val tokens = rawWords.map { tokenizeWord(it) }
        val result = mutableListOf<String>()
        var i = 0

        while (i < tokens.size) {
            val tok = tokens[i]

            // Standalone "a" / "an" only starts a number if followed by hundred/thousand/million
            if ((tok.normalized == "a" || tok.normalized == "an") &&
                (i + 1 >= tokens.size || tokens[i + 1].normalized !in englishMultipliers)
            ) {
                result += tok.original
                i++
                continue
            }

            if (isNumberStart(tok)) {
                val (value, consumed) = consumeNumberPhrase(tokens, i)
                if (value != null && consumed > 0) {
                    // Preserve leading "و" if this number started a new clause with attached waw (e.g. "و1500", "وثلاثة آلاف")
                    if (tok.hadLeadingWaw) {
                        result += "و"
                    }
                    result += formatNumber(value)
                    i += consumed
                    continue
                }
            }

            result += tok.original
            i++
        }

        return result.joinToString(" ").replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Extracts the first numeric amount found in a text string, if any.
     */
    fun extractFirstNumber(rawInput: String): Double? {
        val normalized = normalizeNumbersInText(rawInput)
        val match = Regex("""\b\d+(?:\.\d+)?\b""").find(normalized)
        return match?.value?.toDoubleOrNull()
    }

    private fun tokenizeWord(raw: String): Token {
        val cleaned = raw.trim().trimEnd('.', ':')
        val norm = TextNormalizer.normalizeArabic(cleaned)

        // Direct numeric token like 500 or 2.5k
        if (digitRegex.matches(norm)) {
            return Token(cleaned, norm)
        }

        // Attached Arabic prefix + digits: و1500, ب500, بـ500, وب500
        val wawBaDigit = Regex("^وبـ?(\\d+(?:\\.\\d+)?[kK]?)$").matchEntire(norm)
        if (wawBaDigit != null) {
            return Token(cleaned, wawBaDigit.groupValues[1], hadLeadingWaw = true, hadLeadingBa = true)
        }
        val wawDigit = Regex("^و(\\d+(?:\\.\\d+)?[kK]?)$").matchEntire(norm)
        if (wawDigit != null) {
            return Token(cleaned, wawDigit.groupValues[1], hadLeadingWaw = true)
        }
        val baDigit = Regex("^بـ?(\\d+(?:\\.\\d+)?[kK]?)$").matchEntire(norm)
        if (baDigit != null) {
            return Token(cleaned, baDigit.groupValues[1], hadLeadingBa = true)
        }

        // Direct Arabic/English number word
        if (isCoreNumberWord(norm)) {
            return Token(cleaned, norm)
        }

        // Attached "وب" + Arabic number word (e.g. "وبألفين", "وبعشرة")
        if (norm.startsWith("وب") && norm.length > 3) {
            val stripped = norm.removePrefix("وب").removePrefix("ـ")
            if (isCoreNumberWord(stripped)) {
                return Token(cleaned, stripped, hadLeadingWaw = true, hadLeadingBa = true)
            }
        }

        // Attached "و" + Arabic number word (e.g. "وعشرين", "وألفين", "وخمسة", "ونص")
        if (norm.startsWith("و") && norm.length > 2) {
            val stripped = norm.removePrefix("و")
            if (isCoreNumberWord(stripped)) {
                return Token(cleaned, stripped, hadLeadingWaw = true)
            }
        }

        // Attached "ب" + Arabic number word (e.g. "بعشرة", "بألفين", "بخمسمية", "بـألف")
        if (norm.startsWith("ب") && norm.length > 2) {
            val stripped = norm.removePrefix("ب").removePrefix("ـ")
            if (isCoreNumberWord(stripped)) {
                return Token(cleaned, stripped, hadLeadingBa = true)
            }
        }

        return Token(cleaned, norm)
    }

    private fun isCoreNumberWord(w: String): Boolean {
        return digitRegex.matches(w) ||
            w in arabicUnits ||
            w in arabicTens ||
            w in arabicHundreds ||
            w in arabicDualScales ||
            w in arabicMultipliers ||
            w in arabicFractions ||
            w in englishUnits ||
            w in englishTens ||
            w in englishMultipliers ||
            w == "half" || w == "quarter"
    }

    private fun isNumberStart(tok: Token): Boolean {
        val w = tok.normalized
        return digitRegex.matches(w) ||
            w in arabicUnits ||
            w in arabicTens ||
            w in arabicHundreds ||
            w in arabicDualScales ||
            w in arabicMultipliers ||
            w in arabicFractions ||
            w in englishUnits ||
            w in englishTens ||
            w in englishMultipliers
    }

    private fun consumeNumberPhrase(tokens: List<Token>, start: Int): Pair<Double?, Int> {
        var total = 0.0
        var current = 0.0
        var lastScale = 0.0
        var i = start
        var consumedAny = false
        var lastValidIndex = start

        while (i < tokens.size) {
            val tok = tokens[i]
            val w = tok.normalized

            // Connector words INSIDE a number phrase ("و" / "and")
            if (i > start && (w == "و" || w == "and")) {
                // Only consume connector if the next token continues the number
                if (i + 1 < tokens.size && isCoreNumberWord(tokens[i + 1].normalized)) {
                    i++
                    continue
                } else {
                    break
                }
            }

            // "a" in "two and a half thousand"
            if (i > start && (w == "a" || w == "an")) {
                if (i + 1 < tokens.size && (tokens[i + 1].normalized == "half" || tokens[i + 1].normalized == "quarter")) {
                    i++
                    continue
                }
            }

            // If we already saw a standalone number and next token has attached "ب" (e.g. "بـ2000"), don't merge!
            if (i > start && tok.hadLeadingBa) {
                break
            }

            // Digits (e.g. 500, 10000, 2.5k)
            val digitMatch = digitRegex.matchEntire(w)
            if (digitMatch != null) {
                val base = digitMatch.groupValues[1].toDoubleOrNull() ?: break
                val isK = digitMatch.groupValues[2].equals("k", ignoreCase = true)
                val num = if (isK) base * 1000.0 else base

                // Avoid merging two unrelated numbers like "500 200" unless preceded by "و" or a scale
                if (consumedAny && current > 0.0 && lastScale == 0.0 && !tok.hadLeadingWaw) {
                    break
                }
                current += num
                consumedAny = true
                lastValidIndex = i + 1
                i++
                continue
            }

            // Arabic dual scales: ألفين (2000), مليونين (2000000)
            val dual = arabicDualScales[w]
            if (dual != null) {
                total += current + dual
                current = 0.0
                lastScale = dual / 2.0
                consumedAny = true
                lastValidIndex = i + 1
                i++
                continue
            }

            // Arabic multipliers: ألف, آلاف, مليون
            val arMult = arabicMultipliers[w]
            if (arMult != null) {
                val factor = if (current > 0.0) current else 1.0
                total += factor * arMult
                current = 0.0
                lastScale = arMult
                consumedAny = true
                lastValidIndex = i + 1
                i++
                continue
            }

            // English multipliers: hundred, thousand, million
            val enMult = englishMultipliers[w]
            if (enMult != null) {
                if (enMult == 100.0) {
                    val factor = if (current > 0.0) current else 1.0
                    current = factor * 100.0
                    lastScale = 100.0
                } else {
                    val factor = if (current > 0.0) current else 1.0
                    total += factor * enMult
                    current = 0.0
                    lastScale = enMult
                }
                consumedAny = true
                lastValidIndex = i + 1
                i++
                continue
            }

            // Arabic hundreds: مية, ميتين, خمسمية ...
            val hun = arabicHundreds[w]
            if (hun != null) {
                // Check "ثلاث مية" where current was 3 and w is مية (100)
                if (hun == 100.0 && current in 2.0..9.0) {
                    current *= 100.0
                } else {
                    current += hun
                }
                lastScale = 100.0
                consumedAny = true
                lastValidIndex = i + 1
                i++
                continue
            }

            // Fractions: نص / نصف / ربع / half / quarter
            val frac = arabicFractions[w] ?: when (w) {
                "half" -> 0.5
                "quarter" -> 0.25
                else -> null
            }
            if (frac != null) {
                // Case 1: "نص ألف" (fraction BEFORE multiplier)
                val nextNorm = tokens.getOrNull(i + 1)?.normalized
                if (nextNorm != null && (nextNorm in arabicMultipliers || nextNorm in englishMultipliers)) {
                    current += frac
                    consumedAny = true
                    lastValidIndex = i + 1
                    i++
                    continue
                }
                // Case 2: "ألف ونص" / "ألفين ونص" / "مية ونص" (fraction AFTER scale)
                if (lastScale >= 100.0) {
                    total += frac * lastScale
                    lastScale = 0.0
                    consumedAny = true
                    lastValidIndex = i + 1
                    i++
                    continue
                }
                // Case 3: "two and a half thousand"
                if (current > 0.0) {
                    current += frac
                    consumedAny = true
                    lastValidIndex = i + 1
                    i++
                    continue
                }
                // Standalone "نص" without number context is not an expense amount
                break
            }

            // Arabic / English tens
            val ten = arabicTens[w] ?: englishTens[w]
            if (ten != null) {
                current += ten
                consumedAny = true
                lastValidIndex = i + 1
                i++
                continue
            }

            // Arabic / English units (also handle "أحد عشر" / "ثلاثة عشر")
            val unit = arabicUnits[w] ?: englishUnits[w]
            if (unit != null) {
                // Handle "ثلاثة عشر" -> 13 if next is "عشر"
                val nextNorm = tokens.getOrNull(i + 1)?.normalized
                if (unit in 1.0..9.0 && nextNorm == "عشر") {
                    current += (unit + 10.0)
                    consumedAny = true
                    lastValidIndex = i + 2
                    i += 2
                    continue
                }
                current += unit
                consumedAny = true
                lastValidIndex = i + 1
                i++
                continue
            }

            // Non-number token reached: stop number phrase
            break
        }

        if (!consumedAny) return null to 0
        val finalValue = total + current
        if (finalValue <= 0.0) return null to 0
        return finalValue to (lastValidIndex - start)
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
        }
    }
}
