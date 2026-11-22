package com.myexpenses.app.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.ExpenseEntity
import com.myexpenses.app.data.local.entity.ExpenseWithCategory
import com.myexpenses.app.data.preferences.CurrencyOption
import com.myexpenses.app.domain.DateTimeUtils
import com.myexpenses.app.ui.viewmodel.HistoryDateFilter

@Composable
fun HistoryScreen(
    expenses: List<ExpenseWithCategory>,
    categories: List<CategoryEntity>,
    searchQuery: String,
    selectedCategoryId: String?,
    dateFilter: HistoryDateFilter,
    currency: CurrencyOption,
    isArabic: Boolean,
    onSearchChange: (String) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onDateFilterChange: (HistoryDateFilter) -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    val grouped = remember(expenses) {
        expenses.groupBy { DateTimeUtils.toLocalDate(it.expense.timestamp) }
            .entries
            .sortedByDescending { it.key }
    }
    val filteredTotal = remember(expenses) { expenses.sumOf { it.expense.amount } }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = {
                    Text(if (isArabic) "ابحث في المصروفات أو الفئات..." else "Search expenses or categories...")
                },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Date Filter Chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val dateOptions = listOf(
                    HistoryDateFilter.ALL to (if (isArabic) "الكل" else "All"),
                    HistoryDateFilter.TODAY to (if (isArabic) "اليوم" else "Today"),
                    HistoryDateFilter.YESTERDAY to (if (isArabic) "أمس" else "Yesterday"),
                    HistoryDateFilter.THIS_WEEK to (if (isArabic) "الأسبوع" else "Week"),
                    HistoryDateFilter.THIS_MONTH to (if (isArabic) "الشهر" else "Month")
                )
                dateOptions.forEach { (filter, label) ->
                    FilterChip(
                        selected = dateFilter == filter,
                        onClick = { onDateFilterChange(filter) },
                        label = { Text(label) }
                    )
                }
            }

            // Category Filter Chips
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategoryId == null,
                    onClick = { onCategoryFilterChange(null) },
                    label = { Text(if (isArabic) "كل الفئات" else "All Categories") }
                )
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = {
                            onCategoryFilterChange(if (selectedCategoryId == cat.id) null else cat.id)
                        },
                        label = { Text(cat.displayName(isArabic)) }
                    )
                }
            }

            // Summary strip
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "النتائج: ${expenses.size} عملية" else "Results: ${expenses.size} items",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = DateTimeUtils.formatCurrency(filteredTotal, currency, isArabic),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        if (grouped.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isArabic) "لا توجد مصروفات مطابقة للبحث" else "No matching expenses found",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                grouped.forEach { (date, dayItems) ->
                    val daySum = dayItems.sumOf { it.expense.amount }
                    item(key = "header_$date") {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = DateTimeUtils.formatDayHeader(date, isArabic),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = DateTimeUtils.formatCurrency(daySum, currency, isArabic),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    items(dayItems, key = { it.expense.id }) { item ->
                        ExpenseListItemCard(
                            item = item,
                            currency = currency,
                            isArabic = isArabic,
                            onClick = { onEditExpense(item.expense) },
                            onDelete = { onDeleteExpense(item.expense) }
                        )
                    }
                }
            }
        }
    }
}
