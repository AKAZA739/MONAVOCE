package com.myexpenses.app.data.local.entity

data class CurrencyTotal(val currencyCode: String, val total: Double, val count: Int)

fun Iterable<ExpenseWithCategory>.totalsByCurrency(): List<CurrencyTotal> =
    groupBy { it.expense.currencyCode.uppercase() }
        .toSortedMap()
        .map { (code, items) -> CurrencyTotal(code, items.sumOf { it.expense.amount }, items.size) }
