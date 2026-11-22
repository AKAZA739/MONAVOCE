package com.myexpenses.app.domain.categorizer

import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.DefaultCategories
import com.myexpenses.app.domain.parser.TextNormalizer

data class CategoryMatch(
    val category: CategoryEntity,
    val score: Int,
    val matchedKeyword: String?
)

/**
 * 100% local, extensible rule-and-keyword scoring categorizer.
 * Automatically adapts to user-added categories and custom keywords stored in Room.
 */
object ExpenseCategorizer {

    fun categorize(
        text: String,
        categories: List<CategoryEntity>
    ): CategoryMatch {
        val fallback = categories.firstOrNull { it.id == DefaultCategories.OTHER }
            ?: categories.firstOrNull()
            ?: DefaultCategories.list.last()

        val cleaned = TextNormalizer.normalizeForMatching(text)
        if (cleaned.isBlank()) {
            return CategoryMatch(fallback, 0, null)
        }

        val words = cleaned.split(" ").filter { it.isNotBlank() }
        val expandedWords = words.flatMap { TextNormalizer.stripArabicPrefixes(it) }.toSet()

        var bestCategory: CategoryEntity = fallback
        var bestScore = 0
        var bestKeyword: String? = null

        for (category in categories) {
            if (category.id == DefaultCategories.OTHER) continue

            var score = 0
            var topHit: String? = null

            // Check category's own names + keywords
            val candidates = buildList {
                add(category.nameAr)
                add(category.nameEn)
                addAll(category.keywordList())
            }

            for (rawKeyword in candidates) {
                val kw = TextNormalizer.normalizeForMatching(rawKeyword)
                if (kw.isBlank()) continue

                if (kw.contains(" ")) {
                    // Multi-word phrase match (higher precision)
                    if (cleaned.contains(kw)) {
                        score += 12
                        if (topHit == null) topHit = rawKeyword
                    }
                } else {
                    val kwForms = TextNormalizer.stripArabicPrefixes(kw)
                    val exactMatch = kwForms.any { it in expandedWords }
                    if (exactMatch) {
                        score += 10
                        if (topHit == null) topHit = rawKeyword
                    } else if (kw.length >= 4 && expandedWords.any { w -> w.length >= 4 && (w.startsWith(kw) || kw.startsWith(w)) }) {
                        // Stem / plural prefix match (e.g. مطاعم <-> مطعم, مواصلات <-> مواصلة)
                        score += 5
                        if (topHit == null) topHit = rawKeyword
                    }
                }
            }

            // Custom user categories get a tiny tie-breaker preference
            if (!category.isDefault && score > 0) {
                score += 1
            }

            if (score > bestScore) {
                bestScore = score
                bestCategory = category
                bestKeyword = topHit
            }
        }

        return CategoryMatch(
            category = bestCategory,
            score = bestScore,
            matchedKeyword = bestKeyword
        )
    }
}
