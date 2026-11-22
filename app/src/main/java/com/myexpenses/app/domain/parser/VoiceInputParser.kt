package com.myexpenses.app.domain.parser

import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.DefaultCategories
import com.myexpenses.app.domain.categorizer.ExpenseCategorizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

data class ParsedExpense(
    val amount: Double,
    val categoryId: String,
    val categoryNameAr: String,
    val categoryNameEn: String,
    val note: String,
    val timestampMillis: Long,
    val date: LocalDate,
    val confidence: Float,
    val isCategoryMatched: Boolean
)

sealed class VoiceCommand {
    data class QueryTotal(
        val period: QueryPeriod,
        val categoryId: String? = null,
        val categoryNameAr: String? = null,
        val categoryNameEn: String? = null
    ) : VoiceCommand()

    data class ShowExpenses(val period: QueryPeriod) : VoiceCommand()

    data object DeleteLastExpense : VoiceCommand()

    data class UpdateLastExpense(
        val newAmount: Double,
        val newCategoryId: String? = null
    ) : VoiceCommand()
}

sealed class VoiceParseResult {
    data class Command(val command: VoiceCommand, val rawTranscript: String) : VoiceParseResult()

    data class Expenses(
        val items: List<ParsedExpense>,
        val rawTranscript: String,
        val needsConfirmation: Boolean,
        val ambiguityReasonAr: String? = null,
        val ambiguityReasonEn: String? = null
    ) : VoiceParseResult()

    data class Unclear(
        val rawTranscript: String,
        val reasonAr: String,
        val reasonEn: String
    ) : VoiceParseResult()
}

/**
 * Core offline NLP engine for My Expenses | مصاريفي.
 * Handles:
 *  - Voice commands (query totals, show period, delete/edit last expense)
 *  - Single & multi-expense extraction from one utterance
 *  - Arabic (MSA + dialects), English, and code-switched Arabic+English
 *  - Spoken numbers, currency words, dates ("أمس", "اليوم", "yesterday"), and auto-categorization
 */
object VoiceInputParser {

    private val fillerWords = setOf(
        // Arabic verbs & prepositions
        "لقد", "قد", "انا", "اني", "اللي",
        "صرفت", "دفعت", "اشتريت", "شريت", "سجل", "سجلي", "اضف", "أضف", "اضيف", "ضيف",
        "مصروف", "مصاريف", "مبلغ", "بقيمه", "بقيمة", "قيمه", "قيمة", "حساب", "تكلفته", "تكلفة",
        "علي", "على", "في", "من", "الي", "إلى", "ل", "عن", "مع", "عشان", "علشان", "حق", "تبع",
        "اليوم", "امس", "أمس", "البارحة", "البارحه",
        // Currencies
        "ريال", "ريالات", "يمني", "سعودي", "دولار", "دولارات", "درهم", "دراهم", "جنيه", "دينار", "يورو",
        "yer", "sar", "usd", "aed", "egp", "eur", "riyal", "riyals", "dollar", "dollars", "bucks",
        // English verbs & prepositions
        "i", "spent", "paid", "bought", "buy", "spend", "pay", "add", "log", "record", "expense",
        "on", "for", "at", "in", "of", "the", "a", "an", "to", "my", "today", "yesterday", "just"
    ).map { TextNormalizer.normalizeArabic(it) }.toSet()

    private val numberTokenRegex = Regex("""^\d+(?:\.\d+)?$""")

    fun parse(
        rawTranscript: String,
        categories: List<CategoryEntity>,
        referenceDate: LocalDate = LocalDate.now(),
        referenceTime: LocalTime = LocalTime.now(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): VoiceParseResult {
        val trimmed = rawTranscript.trim()
        if (trimmed.isBlank()) {
            return VoiceParseResult.Unclear(
                rawTranscript = trimmed,
                reasonAr = "لم يتم التقاط أي كلام.",
                reasonEn = "No speech was detected."
            )
        }

        // 1. Try voice command classification first
        detectCommand(trimmed, categories)?.let { cmd ->
            return VoiceParseResult.Command(cmd, trimmed)
        }

        // 2. Parse shared date context from the full utterance
        val globalDate = DateParser.parseDate(trimmed, referenceDate, referenceTime, zoneId)

        // 3. Normalize all spoken numbers (Arabic + English) BEFORE splitting clauses
        //    so compound numbers like "خمسة وعشرين ألف" become "25000" first!
        val numbersNormalized = NumberWordParser.normalizeNumbersInText(globalDate.cleanedText)

        // 4. Split utterance into expense clauses (supporting multi-expense utterances)
        val clauses = splitIntoExpenseClauses(numbersNormalized)

        val parsedExpenses = mutableListOf<ParsedExpense>()
        for (clause in clauses) {
            val clauseDate = DateParser.parseDate(clause, globalDate.date, referenceTime, zoneId)
            val parsed = parseSingleClause(
                clause = clauseDate.cleanedText,
                timestampMillis = if (clauseDate.wasExplicitDate) clauseDate.timestampMillis else globalDate.timestampMillis,
                date = if (clauseDate.wasExplicitDate) clauseDate.date else globalDate.date,
                categories = categories
            )
            if (parsed != null) {
                parsedExpenses += parsed
            }
        }

        if (parsedExpenses.isEmpty()) {
            return VoiceParseResult.Unclear(
                rawTranscript = trimmed,
                reasonAr = "لم أتمكن من فهم المبلغ. مثال: \"صرفت 500 ريال على الفطور\"",
                reasonEn = "Could not detect an amount. Try: \"I spent 500 on groceries\""
            )
        }

        val hasLowConfidence = parsedExpenses.any { it.confidence < 0.65f }
        val hasZeroOrHugeAmount = parsedExpenses.any { it.amount <= 0.0 || it.amount > 100_000_000.0 }
        val needsConfirm = hasLowConfidence || hasZeroOrHugeAmount

        return VoiceParseResult.Expenses(
            items = parsedExpenses,
            rawTranscript = trimmed,
            needsConfirmation = needsConfirm,
            ambiguityReasonAr = if (needsConfirm) "يرجى مراجعة الفئة أو التفاصيل قبل الحفظ" else null,
            ambiguityReasonEn = if (needsConfirm) "Please review the category or details before saving" else null
        )
    }

    private fun detectCommand(
        rawTranscript: String,
        categories: List<CategoryEntity>
    ): VoiceCommand? {
        val norm = TextNormalizer.normalizeArabic(TextNormalizer.normalizeDigits(rawTranscript))

        // Delete last expense
        val isDeleteLast = (
            (norm.contains("احذف") || norm.contains("امسح") || norm.contains("الغ") || norm.contains("تراجع") ||
                norm.contains("delete") || norm.contains("remove") || norm.contains("undo")) &&
                (norm.contains("اخر") || norm.contains("الاخير") || norm.contains("last") || norm.contains("previous") || norm.contains("مصروف"))
            )
        if (isDeleteLast && !norm.contains("عدل") && !norm.contains("غير") && !norm.contains("update") && !norm.contains("edit")) {
            return VoiceCommand.DeleteLastExpense
        }

        // Update last expense ("عدل آخر مصروف إلى 2000 ريال" / "update last expense to 2000")
        val isUpdateLast = (
            (norm.contains("عدل") || norm.contains("غير") || norm.contains("تعديل") ||
                norm.contains("update") || norm.contains("edit") || norm.contains("change")) &&
                (norm.contains("اخر") || norm.contains("الاخير") || norm.contains("last"))
            )
        if (isUpdateLast) {
            val amount = NumberWordParser.extractFirstNumber(rawTranscript)
            if (amount != null && amount > 0.0) {
                val catMatch = ExpenseCategorizer.categorize(rawTranscript, categories)
                return VoiceCommand.UpdateLastExpense(
                    newAmount = amount,
                    newCategoryId = if (catMatch.score >= 10) catMatch.category.id else null
                )
            }
        }

        // Query total ("كم صرفت اليوم؟", "كم صرفت على الأكل هذا الشهر؟", "how much did I spend today?")
        val isQueryTotal = norm.startsWith("كم") ||
            norm.contains("كم صرف") || norm.contains("كم دفع") || norm.contains("كم مجموع") ||
            norm.contains("اجمالي") || norm.contains("مجموع مصروف") ||
            norm.contains("how much") || norm.contains("total spent") || norm.contains("total expenses")
        if (isQueryTotal) {
            val period = DateParser.detectQueryPeriod(norm)
            val catMatch = ExpenseCategorizer.categorize(rawTranscript, categories)
            val matchedCat = if (catMatch.score >= 10) catMatch.category else null
            return VoiceCommand.QueryTotal(
                period = period,
                categoryId = matchedCat?.id,
                categoryNameAr = matchedCat?.nameAr,
                categoryNameEn = matchedCat?.nameEn
            )
        }

        // Show expenses ("اعرض مصروفات أمس", "وريني مصروفات اليوم", "show yesterday's expenses")
        val isShowExpenses = (
            norm.contains("اعرض") || norm.contains("عرض") || norm.contains("وريني") ||
                norm.contains("ارني") || norm.contains("افتح") || norm.contains("show") || norm.contains("list")
            ) && (norm.contains("مصروف") || norm.contains("مصاريف") || norm.contains("expense"))
        if (isShowExpenses) {
            val period = DateParser.detectQueryPeriod(norm)
            return VoiceCommand.ShowExpenses(period)
        }

        return null
    }

    /**
     * Splits a number-normalized utterance into individual expense clauses.
     * Example: "صرفت 500 بقالة و 1500 مواصلات و 3000 غداء"
     *   -> ["صرفت 500 بقالة", "1500 مواصلات", "3000 غداء"]
     * Example: "500 groceries and 1500 lunch"
     *   -> ["500 groceries", "1500 lunch"]
     */
    private fun splitIntoExpenseClauses(normalizedText: String): List<String> {
        val tokens = normalizedText.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (tokens.isEmpty()) return emptyList()

        // First count how many distinct numbers exist. If <= 1, no need to split!
        val numberIndices = tokens.indices.filter { numberTokenRegex.matches(tokens[it]) }
        if (numberIndices.size <= 1) {
            return listOf(normalizedText)
        }

        val clauses = mutableListOf<String>()
        val current = mutableListOf<String>()
        var currentHasNumber = false

        val splitWords = setOf("و", "ثم", "بعدين", "وكمان", "كمان", "وايضا", "ايضا", "and", "then", "plus", ",")

        for (idx in tokens.indices) {
            val tok = tokens[idx]
            val normTok = TextNormalizer.normalizeArabic(tok)
            val isSplitWord = normTok in splitWords
            val isAttachedWaw = tok.length > 2 && normTok.startsWith("و") && !isSplitWord
            val isNumber = numberTokenRegex.matches(tok)

            if (isSplitWord && currentHasNumber) {
                // Check if there is another number ahead before splitting
                val hasNumberAhead = (idx + 1 until tokens.size).any { numberTokenRegex.matches(tokens[it]) }
                if (hasNumberAhead) {
                    clauses += current.joinToString(" ").trim()
                    current.clear()
                    currentHasNumber = false
                    continue
                }
            }

            // Handle Arabic attached "و" directly on a description before a number: e.g. "بقالة 500 وغداء 2000"
            if (isAttachedWaw && currentHasNumber) {
                val hasNumberAhead = (idx + 1 until tokens.size).any { numberTokenRegex.matches(tokens[it]) }
                if (hasNumberAhead) {
                    clauses += current.joinToString(" ").trim()
                    current.clear()
                    currentHasNumber = false
                    current += tok.substring(1) // drop leading و
                    continue
                }
            }

            // If we already have a number + at least one non-filler word, and another number appears directly:
            // e.g. "500 بقالة 1500 مواصلات" (without "و")
            if (isNumber && currentHasNumber) {
                val hasDescriptiveWord = current.any { !numberTokenRegex.matches(it) && TextNormalizer.normalizeArabic(it) !in fillerWords }
                if (hasDescriptiveWord) {
                    clauses += current.joinToString(" ").trim()
                    current.clear()
                    currentHasNumber = false
                }
            }

            current += tok
            if (isNumber) currentHasNumber = true
        }

        if (current.isNotEmpty()) {
            clauses += current.joinToString(" ").trim()
        }

        return clauses.filter { it.isNotBlank() }
    }

    private fun parseSingleClause(
        clause: String,
        timestampMillis: Long,
        date: LocalDate,
        categories: List<CategoryEntity>
    ): ParsedExpense? {
        val tokens = clause.split(Regex("\\s+")).filter { it.isNotBlank() }
        val amountToken = tokens.firstOrNull { numberTokenRegex.matches(it) } ?: return null
        val amount = amountToken.toDoubleOrNull() ?: return null
        if (amount <= 0.0) return null

        val descTokens = tokens.filterNot { it == amountToken }.filter { rawTok ->
            val norm = TextNormalizer.normalizeArabic(rawTok).trim(',', '.')
            norm.isNotBlank() && norm !in fillerWords
        }

        val cleanNote = descTokens.joinToString(" ")
            .replace(Regex("^(?:على|علي|في|لـ|ل|بـ|for|on|at)\\s+"), "")
            .trim()

        val catMatch = ExpenseCategorizer.categorize(
            text = if (cleanNote.isNotBlank()) cleanNote else clause,
            categories = categories
        )

        val isCategoryMatched = catMatch.score >= 5 && catMatch.category.id != DefaultCategories.OTHER
        val confidence = when {
            isCategoryMatched && catMatch.score >= 10 -> 0.95f
            isCategoryMatched -> 0.80f
            cleanNote.isNotBlank() -> 0.65f
            else -> 0.45f // Amount spoken with no description at all -> ask confirmation!
        }

        val finalNote = cleanNote.ifBlank {
            if (TextNormalizer.containsArabic(clause)) catMatch.category.nameAr else catMatch.category.nameEn
        }

        return ParsedExpense(
            amount = amount,
            categoryId = catMatch.category.id,
            categoryNameAr = catMatch.category.nameAr,
            categoryNameEn = catMatch.category.nameEn,
            note = finalNote,
            timestampMillis = timestampMillis,
            date = date,
            confidence = confidence,
            isCategoryMatched = isCategoryMatched
        )
    }
}
