package com.myexpenses.app

import com.myexpenses.app.data.backup.csvEscape
import com.myexpenses.app.data.backup.parseBackup
import com.myexpenses.app.data.local.entity.CurrencyTotal
import com.myexpenses.app.data.local.entity.DefaultCategories
import com.myexpenses.app.data.local.entity.ExpenseEntity
import com.myexpenses.app.data.local.entity.ExpenseWithCategory
import com.myexpenses.app.data.local.entity.totalsByCurrency
import com.myexpenses.app.data.preferences.SupportedCurrencies
import com.myexpenses.app.domain.DateTimeUtils
import com.myexpenses.app.voice.SystemRecognitionMode
import com.myexpenses.app.voice.selectSystemRecognitionMode
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ReleaseRegressionTest {
    @Test
    fun missingExpenseArrayIsRejectedBeforeRestore() {
        assertThrows(JSONException::class.java) {
            parseBackup(JSONObject("""{"app":"MyExpenses","version":1,"categories":[]}"""))
        }
    }

    @Test
    fun backupFromOtherAppOrVersionIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            parseBackup(JSONObject("""{"app":"OtherApp","version":1,"categories":[],"expenses":[]}"""))
        }
        assertThrows(IllegalArgumentException::class.java) {
            parseBackup(JSONObject("""{"app":"MyExpenses","version":2,"categories":[],"expenses":[]}"""))
        }
    }

    @Test
    fun backupPreservesCurrencyAndRepairsMissingCategory() {
        val backup = parseBackup(JSONObject("""{
            "app":"MyExpenses", "version":1, "categories":[],
            "expenses":[{"amount":10.25,"currencyCode":"usd","categoryId":"old-custom"}]
        }"""))

        assertEquals(1, backup.summary.restoredExpenses)
        val expense = backup.expenses.single()
        assertEquals("USD", expense.currencyCode)
        assertEquals(DefaultCategories.OTHER, expense.categoryId)
        assertEquals(10.25, expense.amount, 0.0)
    }

    @Test
    fun backupRejectsInvalidAmountsBeforeRestore() {
        assertThrows(IllegalArgumentException::class.java) {
            parseBackup(JSONObject("""{
                "app":"MyExpenses", "version":1, "categories":[], "expenses":[{"amount":-2}]
            }"""))
        }
    }

    @Test
    fun explicitEmptyExpenseArrayIsAValidBackup() {
        val backup = parseBackup(JSONObject("""{"app":"MyExpenses","version":1,"categories":[],"expenses":[]}"""))
        assertEquals(0, backup.summary.restoredExpenses)
    }

    @Test
    fun csvKeepsTextCellsLiteralAndQuotesDelimiters() {
        assertEquals("'  + 12", csvEscape("  + 12"))
        assertEquals("\"قهوة, \"\"حليب\"\"\"", csvEscape("قهوة, \"حليب\""))
    }

    @Test
    fun expenseTotalsGroupByStoredCurrency() {
        val expenses = listOf(
            ExpenseWithCategory(ExpenseEntity(amount = 100.0, currencyCode = "SAR"), null),
            ExpenseWithCategory(ExpenseEntity(amount = 10.0, currencyCode = "USD"), null),
            ExpenseWithCategory(ExpenseEntity(amount = 5.0, currencyCode = "sar"), null)
        )
        assertEquals(listOf(CurrencyTotal("SAR", 105.0, 2), CurrencyTotal("USD", 10.0, 1)), expenses.totalsByCurrency())
    }

    @Test
    fun amountsUseTheStoredCurrencyIncludingThreeDecimalCurrencies() {
        val totals = listOf(CurrencyTotal("KWD", 1.234, 1), CurrencyTotal("GBP", 10.0, 1))
        val text = DateTimeUtils.formatCurrencyTotals(totals, SupportedCurrencies.findByCode("SAR"), isArabic = false)
        assertEquals("1.234\u00A0KWD, 10\u00A0GBP", text)
    }

    @Test
    fun standardRecognitionRequiresConsent() {
        for (languageInstalled in listOf(null, false, true)) {
            assertEquals(SystemRecognitionMode.UNAVAILABLE, selectSystemRecognitionMode(false, languageInstalled, true, false))
            assertEquals(SystemRecognitionMode.ON_DEVICE, selectSystemRecognitionMode(true, languageInstalled, true, false))
            assertEquals(SystemRecognitionMode.STANDARD, selectSystemRecognitionMode(false, languageInstalled, true, true))
        }
    }

    @Test
    fun optInStillPrefersAnAvailableLocalLanguage() {
        assertEquals(SystemRecognitionMode.ON_DEVICE, selectSystemRecognitionMode(true, true, true, true))
        assertEquals(SystemRecognitionMode.STANDARD, selectSystemRecognitionMode(true, false, true, true))
    }
}
