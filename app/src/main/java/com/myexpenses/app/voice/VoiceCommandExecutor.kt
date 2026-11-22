package com.myexpenses.app.voice

import com.myexpenses.app.data.preferences.CurrencyOption
import com.myexpenses.app.data.preferences.SupportedCurrencies
import com.myexpenses.app.data.repository.ExpenseRepository
import com.myexpenses.app.domain.DateTimeUtils
import com.myexpenses.app.domain.parser.QueryPeriod
import com.myexpenses.app.domain.parser.VoiceCommand
import java.time.LocalDate

data class CommandExecutionResult(
    val messageAr: String,
    val messageEn: String,
    val navigateToDate: LocalDate? = null,
    val openHistory: Boolean = false
) {
    fun message(isArabic: Boolean): String = if (isArabic) messageAr else messageEn
}

class VoiceCommandExecutor(
    private val repository: ExpenseRepository
) {
    suspend fun execute(
        command: VoiceCommand,
        currency: CurrencyOption
    ): CommandExecutionResult {
        return when (command) {
            is VoiceCommand.QueryTotal -> {
                val range = when (command.period) {
                    QueryPeriod.TODAY -> DateTimeUtils.todayRange()
                    QueryPeriod.YESTERDAY -> DateTimeUtils.yesterdayRange()
                    QueryPeriod.THIS_WEEK -> DateTimeUtils.thisWeekRange()
                    QueryPeriod.THIS_MONTH -> DateTimeUtils.thisMonthRange()
                }
                val periodAr = when (command.period) {
                    QueryPeriod.TODAY -> "اليوم"
                    QueryPeriod.YESTERDAY -> "أمس"
                    QueryPeriod.THIS_WEEK -> "هذا الأسبوع"
                    QueryPeriod.THIS_MONTH -> "هذا الشهر"
                }
                val periodEn = when (command.period) {
                    QueryPeriod.TODAY -> "today"
                    QueryPeriod.YESTERDAY -> "yesterday"
                    QueryPeriod.THIS_WEEK -> "this week"
                    QueryPeriod.THIS_MONTH -> "this month"
                }

                if (command.categoryId != null) {
                    val totals = repository.getCurrencyTotals(
                        range.startMillis,
                        range.endMillis,
                        command.categoryId
                    )
                    val amtAr = DateTimeUtils.formatCurrencyTotals(totals, currency, isArabic = true)
                    val amtEn = DateTimeUtils.formatCurrencyTotals(totals, currency, isArabic = false)
                    CommandExecutionResult(
                        messageAr = "صرفت على ${command.categoryNameAr ?: ""} $periodAr: $amtAr",
                        messageEn = "You spent $amtEn on ${command.categoryNameEn ?: ""} $periodEn"
                    )
                } else {
                    val totals = repository.getCurrencyTotals(range.startMillis, range.endMillis)
                    val count = totals.sumOf { it.count }
                    val amtAr = DateTimeUtils.formatCurrencyTotals(totals, currency, isArabic = true)
                    val amtEn = DateTimeUtils.formatCurrencyTotals(totals, currency, isArabic = false)
                    CommandExecutionResult(
                        messageAr = "إجمالي مصروفات $periodAr: $amtAr ($count عمليات)",
                        messageEn = "Total expenses $periodEn: $amtEn ($count entries)"
                    )
                }
            }

            is VoiceCommand.ShowExpenses -> {
                val targetDate = when (command.period) {
                    QueryPeriod.YESTERDAY -> LocalDate.now().minusDays(1)
                    else -> LocalDate.now()
                }
                val range = when (command.period) {
                    QueryPeriod.TODAY -> DateTimeUtils.todayRange()
                    QueryPeriod.YESTERDAY -> DateTimeUtils.yesterdayRange()
                    QueryPeriod.THIS_WEEK -> DateTimeUtils.thisWeekRange()
                    QueryPeriod.THIS_MONTH -> DateTimeUtils.thisMonthRange()
                }
                val totals = repository.getCurrencyTotals(range.startMillis, range.endMillis)
                val count = totals.sumOf { it.count }
                val amtAr = DateTimeUtils.formatCurrencyTotals(totals, currency, isArabic = true)
                val amtEn = DateTimeUtils.formatCurrencyTotals(totals, currency, isArabic = false)
                val labelAr = when (command.period) {
                    QueryPeriod.YESTERDAY -> "أمس"
                    QueryPeriod.THIS_WEEK -> "هذا الأسبوع"
                    QueryPeriod.THIS_MONTH -> "هذا الشهر"
                    QueryPeriod.TODAY -> "اليوم"
                }
                val labelEn = when (command.period) {
                    QueryPeriod.YESTERDAY -> "yesterday"
                    QueryPeriod.THIS_WEEK -> "this week"
                    QueryPeriod.THIS_MONTH -> "this month"
                    QueryPeriod.TODAY -> "today"
                }
                CommandExecutionResult(
                    messageAr = "مصروفات $labelAr: $amtAr ($count عمليات)",
                    messageEn = "Expenses for $labelEn: $amtEn ($count items)",
                    navigateToDate = targetDate,
                    openHistory = command.period == QueryPeriod.THIS_WEEK || command.period == QueryPeriod.THIS_MONTH
                )
            }

            VoiceCommand.DeleteLastExpense -> {
                val last = repository.getLastCreatedExpense()
                if (last == null) {
                    CommandExecutionResult(
                        messageAr = "لا توجد مصروفات مسجلة لحذفها.",
                        messageEn = "There are no recorded expenses to delete."
                    )
                } else {
                    repository.deleteExpense(last.expense)
                    val expenseCurrency = SupportedCurrencies.forExpense(last.expense.currencyCode)
                    val amtAr = DateTimeUtils.formatCurrency(last.expense.amount, expenseCurrency, isArabic = true)
                    val amtEn = DateTimeUtils.formatCurrency(last.expense.amount, expenseCurrency, isArabic = false)
                    val catAr = last.category?.nameAr ?: ""
                    val catEn = last.category?.nameEn ?: ""
                    CommandExecutionResult(
                        messageAr = "تم حذف آخر مصروف ($amtAr - $catAr)",
                        messageEn = "Deleted last expense ($amtEn - $catEn)"
                    )
                }
            }

            is VoiceCommand.UpdateLastExpense -> {
                val last = repository.getLastCreatedExpense()
                if (last == null) {
                    CommandExecutionResult(
                        messageAr = "لا يوجد مصروف سابق لتعديله.",
                        messageEn = "No previous expense found to update."
                    )
                } else {
                    val updated = last.expense.copy(
                        amount = command.newAmount,
                        categoryId = command.newCategoryId ?: last.expense.categoryId
                    )
                    repository.updateExpense(updated)
                    val expenseCurrency = SupportedCurrencies.forExpense(last.expense.currencyCode)
                    val amtAr = DateTimeUtils.formatCurrency(command.newAmount, expenseCurrency, isArabic = true)
                    val amtEn = DateTimeUtils.formatCurrency(command.newAmount, expenseCurrency, isArabic = false)
                    CommandExecutionResult(
                        messageAr = "تم تعديل آخر مصروف إلى $amtAr",
                        messageEn = "Updated last expense to $amtEn"
                    )
                }
            }
        }
    }
}
