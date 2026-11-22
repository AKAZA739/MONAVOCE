package com.myexpenses.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.myexpenses.app.AppContainer
import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.CurrencyTotal
import com.myexpenses.app.data.local.entity.DefaultCategories
import com.myexpenses.app.data.local.entity.ExpenseEntity
import com.myexpenses.app.data.local.entity.ExpenseWithCategory
import com.myexpenses.app.data.local.entity.totalsByCurrency
import com.myexpenses.app.data.preferences.UserPreferences
import com.myexpenses.app.domain.DateTimeUtils
import com.myexpenses.app.ui.components.CategoryBreakdownItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

enum class HistoryDateFilter {
    ALL, TODAY, YESTERDAY, THIS_WEEK, THIS_MONTH
}

enum class DashboardPeriod {
    TODAY, WEEK, MONTH, CUSTOM_DAY
}

data class BarChartSlot(
    val labelAr: String,
    val labelEn: String,
    val amount: Double,
    val isHighlighted: Boolean = false
)

data class DashboardUiState(
    val activePeriod: DashboardPeriod = DashboardPeriod.MONTH,
    val selectedDate: LocalDate = LocalDate.now(),
    val activeTotals: List<CurrencyTotal> = emptyList(),
    val monthTotals: List<CurrencyTotal> = emptyList(),
    val chartCurrencyCode: String? = null,
    val chartCurrencyCodes: List<String> = emptyList(),
    val chartTotal: Double = 0.0,
    val barSlots: List<BarChartSlot> = emptyList(),
    val activeBreakdown: List<CategoryBreakdownItem> = emptyList(),
    val activeExpenses: List<ExpenseWithCategory> = emptyList(),
    val recentExpenses: List<ExpenseWithCategory> = emptyList()
)

class MainViewModel(private val container: AppContainer) : ViewModel() {

    private val repo = container.repository
    val prefsRepo = container.preferencesRepository

    val preferences: StateFlow<UserPreferences> = prefsRepo.preferences

    val categories: StateFlow<List<CategoryEntity>> = repo.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DefaultCategories.list)

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _activePeriod = MutableStateFlow(DashboardPeriod.TODAY)
    val activePeriod: StateFlow<DashboardPeriod> = _activePeriod.asStateFlow()

    private val chartCurrency = MutableStateFlow<String?>(null)

    // History filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _historyDateFilter = MutableStateFlow(HistoryDateFilter.ALL)
    val historyDateFilter: StateFlow<HistoryDateFilter> = _historyDateFilter.asStateFlow()

    private val _lastDeletedExpense = MutableStateFlow<ExpenseEntity?>(null)
    val lastDeletedExpense: StateFlow<ExpenseEntity?> = _lastDeletedExpense.asStateFlow()

    val dashboardState: StateFlow<DashboardUiState> = combine(
        repo.observeAllExpenses(),
        categories,
        _activePeriod,
        _selectedDate,
        chartCurrency
    ) { allExpenses, allCats, period, selectedDay, selectedCurrency ->
        val today = LocalDate.now()
        val catMap = allCats.associateBy { it.id }

        val todayRange = DateTimeUtils.dayRange(today)
        val weekRange = DateTimeUtils.thisWeekRange()
        val monthRange = DateTimeUtils.thisMonthRange()
        val selectedDayRange = DateTimeUtils.dayRange(selectedDay)

        val monthTotals = allExpenses.filter { it.expense.timestamp in monthRange.startMillis until monthRange.endMillis }
            .totalsByCurrency()

        val activeRange = when (period) {
            DashboardPeriod.TODAY -> todayRange
            DashboardPeriod.WEEK -> weekRange
            DashboardPeriod.MONTH -> monthRange
            DashboardPeriod.CUSTOM_DAY -> selectedDayRange
        }

        val activeExpenses = allExpenses.filter {
            it.expense.timestamp >= activeRange.startMillis && it.expense.timestamp < activeRange.endMillis
        }
        val activeTotals = activeExpenses.totalsByCurrency()
        val currencyCodes = allExpenses.map { it.expense.currencyCode.uppercase() }.distinct().sorted()
        val chartCode = selectedCurrency?.takeIf { it in currencyCodes }
            ?: activeTotals.firstOrNull()?.currencyCode
            ?: currencyCodes.firstOrNull()
        val chartExpenses = allExpenses.filter { it.expense.currencyCode.equals(chartCode, ignoreCase = true) }
        val chartTotal = activeTotals.firstOrNull { it.currencyCode == chartCode }?.total ?: 0.0

        // Category breakdown for the active period
        val breakdown = activeExpenses
            .filter { it.expense.currencyCode.equals(chartCode, ignoreCase = true) }
            .groupBy { it.expense.categoryId }
            .mapNotNull { (catId, list) ->
                val cat = catMap[catId] ?: return@mapNotNull null
                val sum = list.sumOf { it.expense.amount }
                val ratio = if (chartTotal > 0.0) (sum / chartTotal).toFloat() else 0f
                CategoryBreakdownItem(cat, sum, list.size, ratio)
            }
            .sortedByDescending { it.total }

        // Build clean bar chart slots matching the active period
        val barSlots = when (period) {
            DashboardPeriod.MONTH -> {
                val firstOfMonth = today.withDayOfMonth(1)
                val currentWeekIndex = ((today.dayOfMonth - 1) / 7).coerceIn(0, 4)
                (0..4).map { weekIdx ->
                    val startDay = firstOfMonth.plusDays((weekIdx * 7).toLong())
                    val endDay = if (weekIdx == 4) firstOfMonth.plusMonths(1) else firstOfMonth.plusDays(((weekIdx + 1) * 7).toLong())
                    val startMs = DateTimeUtils.dayRange(startDay).startMillis
                    val endMs = DateTimeUtils.dayRange(endDay).startMillis
                    val sum = chartExpenses.filter { it.expense.timestamp in startMs until endMs }.sumOf { it.expense.amount }
                    BarChartSlot(
                        labelAr = "أ${weekIdx + 1}",
                        labelEn = "W${weekIdx + 1}",
                        amount = sum,
                        isHighlighted = weekIdx == currentWeekIndex
                    )
                }
            }

            DashboardPeriod.WEEK -> {
                val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY))
                val labelsAr = listOf("سبت", "أحد", "إثن", "ثلا", "أرب", "خمي", "جمع")
                val labelsEn = listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
                (0..6).map { offset ->
                    val d = startOfWeek.plusDays(offset.toLong())
                    val r = DateTimeUtils.dayRange(d)
                    val sum = chartExpenses.filter { it.expense.timestamp in r.startMillis until r.endMillis }.sumOf { it.expense.amount }
                    BarChartSlot(
                        labelAr = labelsAr[offset],
                        labelEn = labelsEn[offset],
                        amount = sum,
                        isHighlighted = d == today
                    )
                }
            }

            DashboardPeriod.TODAY, DashboardPeriod.CUSTOM_DAY -> {
                val anchor = if (period == DashboardPeriod.CUSTOM_DAY) selectedDay else today
                (4 downTo 0).map { daysBack ->
                    val d = anchor.minusDays(daysBack.toLong())
                    val r = DateTimeUtils.dayRange(d)
                    val sum = chartExpenses.filter { it.expense.timestamp in r.startMillis until r.endMillis }.sumOf { it.expense.amount }
                    val labelAr = when (d) {
                        today -> "اليوم"
                        today.minusDays(1) -> "أمس"
                        else -> "${d.dayOfMonth}/${d.monthValue}"
                    }
                    val labelEn = when (d) {
                        today -> "Today"
                        today.minusDays(1) -> "Yest"
                        else -> "${d.dayOfMonth}/${d.monthValue}"
                    }
                    BarChartSlot(
                        labelAr = labelAr,
                        labelEn = labelEn,
                        amount = sum,
                        isHighlighted = d == anchor
                    )
                }
            }
        }

        DashboardUiState(
            activePeriod = period,
            selectedDate = selectedDay,
            activeTotals = activeTotals,
            monthTotals = monthTotals,
            chartCurrencyCode = chartCode,
            chartCurrencyCodes = currencyCodes,
            chartTotal = chartTotal,
            barSlots = barSlots,
            activeBreakdown = breakdown,
            activeExpenses = activeExpenses,
            recentExpenses = allExpenses.take(15)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    val filteredHistory: StateFlow<List<ExpenseWithCategory>> = combine(
        repo.observeAllExpenses(),
        _searchQuery,
        _selectedCategoryFilter,
        _historyDateFilter
    ) { all, query, catId, dateFilter ->
        val range = when (dateFilter) {
            HistoryDateFilter.ALL -> null
            HistoryDateFilter.TODAY -> DateTimeUtils.todayRange()
            HistoryDateFilter.YESTERDAY -> DateTimeUtils.yesterdayRange()
            HistoryDateFilter.THIS_WEEK -> DateTimeUtils.thisWeekRange()
            HistoryDateFilter.THIS_MONTH -> DateTimeUtils.thisMonthRange()
        }
        val q = query.trim().lowercase()
        all.filter { item ->
            val matchesDate = range == null ||
                (item.expense.timestamp >= range.startMillis && item.expense.timestamp < range.endMillis)
            val matchesCat = catId == null || item.expense.categoryId == catId
            val matchesQuery = q.isEmpty() ||
                item.expense.note.lowercase().contains(q) ||
                item.expense.rawTranscript.lowercase().contains(q) ||
                (item.category?.nameAr?.lowercase()?.contains(q) == true) ||
                (item.category?.nameEn?.lowercase()?.contains(q) == true) ||
                item.expense.amount.toString().contains(q)
            matchesDate && matchesCat && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectPeriod(period: DashboardPeriod) {
        _activePeriod.value = period
        if (period == DashboardPeriod.TODAY) {
            _selectedDate.value = LocalDate.now()
        }
    }

    fun selectChartCurrency(currencyCode: String) {
        chartCurrency.value = currencyCode
    }

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        _activePeriod.value = if (date == LocalDate.now()) DashboardPeriod.TODAY else DashboardPeriod.CUSTOM_DAY
    }

    fun stepDate(days: Long) {
        val next = _selectedDate.value.plusDays(days)
        if (!next.isAfter(LocalDate.now())) {
            _selectedDate.value = next
            _activePeriod.value = if (next == LocalDate.now()) DashboardPeriod.TODAY else DashboardPeriod.CUSTOM_DAY
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategoryFilter(categoryId: String?) {
        _selectedCategoryFilter.value = categoryId
    }

    fun updateHistoryDateFilter(filter: HistoryDateFilter) {
        _historyDateFilter.value = filter
    }

    fun addManualExpense(amount: Double, categoryId: String, note: String, timestamp: Long) {
        viewModelScope.launch {
            repo.insertExpense(
                ExpenseEntity(
                    amount = amount,
                    currencyCode = preferences.value.currencyCode,
                    categoryId = categoryId,
                    note = note,
                    timestamp = timestamp,
                    source = "MANUAL"
                )
            )
        }
    }

    fun updateExpense(existing: ExpenseEntity, amount: Double, categoryId: String, note: String, timestamp: Long) {
        viewModelScope.launch {
            repo.updateExpense(
                existing.copy(
                    amount = amount,
                    categoryId = categoryId,
                    note = note,
                    timestamp = timestamp
                )
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            _lastDeletedExpense.value = expense
            repo.deleteExpense(expense)
        }
    }

    fun undoDeleteExpense() {
        val toRestore = _lastDeletedExpense.value ?: return
        _lastDeletedExpense.value = null
        viewModelScope.launch {
            repo.insertExpense(toRestore.copy(id = 0))
        }
    }

    fun saveCategory(
        existingId: String?,
        nameAr: String,
        nameEn: String,
        iconName: String,
        colorHex: Long,
        keywords: String
    ) {
        viewModelScope.launch {
            val currentList = categories.value
            val existing = existingId?.let { id -> currentList.firstOrNull { it.id == id } }
            val entity = CategoryEntity(
                id = existing?.id ?: "custom_${UUID.randomUUID().toString().take(8)}",
                nameAr = nameAr.trim(),
                nameEn = nameEn.trim().ifBlank { nameAr.trim() },
                iconName = iconName,
                colorHex = colorHex,
                keywords = keywords.trim(),
                isDefault = existing?.isDefault ?: false,
                sortOrder = existing?.sortOrder ?: (currentList.size + 1)
            )
            repo.saveCategory(entity)
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repo.deleteCategory(category)
        }
    }

    class Factory(private val container: AppContainer) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MainViewModel(container) as T
        }
    }
}
