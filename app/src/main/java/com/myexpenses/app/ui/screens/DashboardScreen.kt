package com.myexpenses.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.myexpenses.app.data.local.entity.ExpenseEntity
import com.myexpenses.app.data.local.entity.ExpenseWithCategory
import com.myexpenses.app.data.preferences.CurrencyOption
import com.myexpenses.app.data.preferences.SupportedCurrencies
import com.myexpenses.app.domain.DateTimeUtils
import com.myexpenses.app.ui.components.CategoryBreakdownItem
import com.myexpenses.app.ui.components.CategoryIconMapper
import com.myexpenses.app.ui.components.ExpenseChartCard
import com.myexpenses.app.ui.theme.AccentPeriwinkle
import com.myexpenses.app.ui.theme.AccentPeriwinkleDeep
import com.myexpenses.app.ui.viewmodel.BarChartSlot
import com.myexpenses.app.ui.viewmodel.DashboardPeriod
import com.myexpenses.app.ui.viewmodel.DashboardUiState
import java.time.LocalDate

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    currency: CurrencyOption,
    isArabic: Boolean,
    onSelectPeriod: (DashboardPeriod) -> Unit,
    onSelectChartCurrency: (String) -> Unit,
    onStepDate: (Long) -> Unit,
    onSelectToday: () -> Unit,
    onOpenCategories: () -> Unit,
    onAddManual: () -> Unit,
    onEditExpense: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    var showInsightsExpanded by remember { mutableStateOf(false) }
    var topModeAll by remember { mutableStateOf(false) }
    var showCurrencyMenu by remember { mutableStateOf(false) }

    val cardBg = MaterialTheme.colorScheme.surface
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val primaryAccent = MaterialTheme.colorScheme.primary
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant

    val displayedExpenses = if (topModeAll) state.recentExpenses else state.activeExpenses
    val chartCurrency = state.chartCurrencyCode?.let(SupportedCurrencies::forExpense) ?: currency

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Segmented Pill Switcher (inspired by Personal | Business pill in reference)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp)
                ) {
                    TopSegmentPill(
                        label = if (isArabic) "مصاريفي" else "Personal",
                        selected = !topModeAll,
                        onClick = { topModeAll = false },
                        modifier = Modifier.weight(1f)
                    )
                    TopSegmentPill(
                        label = if (isArabic) "آخر العمليات" else "All Recent",
                        selected = topModeAll,
                        onClick = { topModeAll = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Section Title Row ("نظرة عامة / Overview")
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "نظرة عامة" else "Overview",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Day browser arrows when browsing specific day
                if (state.activePeriod == DashboardPeriod.TODAY || state.activePeriod == DashboardPeriod.CUSTOM_DAY) {
                    val isToday = state.selectedDate == LocalDate.now()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { onStepDate(-1) }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = if (isArabic) "اليوم السابق" else "Previous day",
                                tint = mutedText
                            )
                        }
                        Text(
                            text = DateTimeUtils.formatDayHeader(state.selectedDate, isArabic),
                            style = MaterialTheme.typography.labelLarge,
                            color = primaryAccent,
                            modifier = Modifier.clickable(enabled = !isToday) { onSelectToday() }
                        )
                        IconButton(
                            onClick = { onStepDate(1) },
                            enabled = !isToday,
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                contentDescription = if (isArabic) "اليوم التالي" else "Next day",
                                tint = if (isToday) mutedText.copy(alpha = 0.3f) else mutedText
                            )
                        }
                    }
                }
            }
        }

        // 3. Main Obsidian Overview Card (Period Tabs + Big Amount + Rounded Bar Chart + Top Categories + Spending Insights)
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Period Segmented Tabs: Today | Week | Month | Yesterday
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(3.dp)
                        ) {
                            val tabs = listOf(
                                DashboardPeriod.TODAY to (if (isArabic) "اليوم" else "Today"),
                                DashboardPeriod.WEEK to (if (isArabic) "الأسبوع" else "Week"),
                                DashboardPeriod.MONTH to (if (isArabic) "الشهر" else "Month"),
                                DashboardPeriod.CUSTOM_DAY to (if (isArabic) "أمس" else "Custom")
                            )
                            tabs.forEach { (period, label) ->
                                val selected = state.activePeriod == period
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (selected) MaterialTheme.colorScheme.primaryContainer
                                            else Color.Transparent
                                        )
                                        .clickable {
                                            if (period == DashboardPeriod.CUSTOM_DAY && state.selectedDate == LocalDate.now()) {
                                                onStepDate(-1)
                                            } else {
                                                onSelectPeriod(period)
                                            }
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) primaryAccent else mutedText,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Big Crisp Total Amount + Entry Count
                    Text(
                        text = DateTimeUtils.formatCurrencyTotals(state.activeTotals, currency, isArabic, "\n"),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isArabic) {
                            "عدد العمليات: ${state.activeExpenses.size}"
                        } else {
                            "Entries: ${state.activeExpenses.size}"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = mutedText
                    )
                    if (state.activePeriod != DashboardPeriod.MONTH) {
                        Text(
                            text = (if (isArabic) "إجمالي الشهر: " else "Month total: ") +
                                DateTimeUtils.formatCurrencyTotals(state.monthTotals, currency, isArabic),
                            style = MaterialTheme.typography.bodyMedium,
                            color = mutedText
                        )
                    }

                    if (state.chartCurrencyCodes.size > 1) {
                        Box {
                            TextButton(onClick = { showCurrencyMenu = true }) {
                                val name = if (isArabic) chartCurrency.nameAr else chartCurrency.nameEn
                                Text(if (isArabic) "عملة التحليلات: $name" else "Charts: $name")
                                Icon(Icons.Rounded.ExpandMore, contentDescription = null)
                            }
                            DropdownMenu(expanded = showCurrencyMenu, onDismissRequest = { showCurrencyMenu = false }) {
                                state.chartCurrencyCodes.forEach { code ->
                                    val option = SupportedCurrencies.forExpense(code)
                                    DropdownMenuItem(
                                        text = { Text(if (isArabic) option.nameAr else option.nameEn) },
                                        onClick = {
                                            showCurrencyMenu = false
                                            onSelectChartCurrency(code)
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Sleek Rounded Periwinkle Bar Chart (matching W1..W5 in reference!)
                    ObsidianBarChart(
                        slots = state.barSlots,
                        isArabic = isArabic,
                        barColor = primaryAccent
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // "أبرز الفئات / Home categories" + "+ تعديل / + Edit"
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "أبرز الفئات" else "Home categories",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isArabic) "تعديل" else "Edit",
                            style = MaterialTheme.typography.labelLarge,
                            color = primaryAccent,
                            modifier = Modifier.clickable(onClick = onOpenCategories)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3-Column Category Mini Cards inside subtle dark sub-panel
                    TopCategoriesStrip(
                        items = state.activeBreakdown,
                        currency = chartCurrency,
                        isArabic = isArabic,
                        accentColor = primaryAccent
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = borderColor.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom "Spending insights / تحليلات المصروفات" expandable row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showInsightsExpanded = !showInsightsExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(primaryAccent.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PieChart,
                                    contentDescription = null,
                                    tint = primaryAccent,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (isArabic) "تحليلات المصروفات" else "Spending insights",
                                style = MaterialTheme.typography.titleMedium,
                                color = primaryAccent
                            )
                        }
                        Icon(
                            imageVector = if (showInsightsExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = mutedText
                        )
                    }

                    AnimatedVisibility(visible = showInsightsExpanded) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            ExpenseChartCard(
                                title = if (isArabic) "التوزيع التفصيلي للفئات" else "Detailed Category Split",
                                items = state.activeBreakdown,
                                totalAmount = state.chartTotal,
                                currency = chartCurrency,
                                isArabic = isArabic
                            )
                        }
                    }
                }
            }
        }

        // 4. Recent Expenses Header (like "Folders" + "+ New" in reference!)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (topModeAll) {
                        if (isArabic) "أحدث العمليات" else "Recent Expenses"
                    } else {
                        if (isArabic) "العمليات المسجلة" else "Transactions"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = if (isArabic) "+ جديد" else "+ New",
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryAccent,
                    modifier = Modifier.clickable(onClick = onAddManual)
                )
            }
        }

        if (displayedExpenses.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = cardBg,
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (isArabic) "لا توجد مصروفات في هذه الفترة بعد" else "No expenses recorded yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = mutedText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) "اضغط زر الميكروفون المتوهج بالأسفل وسجّل بصوتك" else "Tap the glowing mic button below to speak an expense",
                            style = MaterialTheme.typography.bodyMedium,
                            color = mutedText.copy(alpha = 0.75f)
                        )
                    }
                }
            }
        } else {
            items(displayedExpenses, key = { it.expense.id }) { item ->
                ExpenseListItemCard(
                    item = item,
                    isArabic = isArabic,
                    onClick = { onEditExpense(item.expense) },
                    onDelete = { onDeleteExpense(item.expense) }
                )
            }
        }
    }
}

@Composable
private fun TopSegmentPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) MaterialTheme.colorScheme.surfaceVariant
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ObsidianBarChart(
    slots: List<BarChartSlot>,
    isArabic: Boolean,
    barColor: Color
) {
    val maxVal = slots.maxOfOrNull { it.amount }?.takeIf { it > 0.0 } ?: 1.0
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant
    val emptySlotColor = MaterialTheme.colorScheme.surfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        slots.forEach { slot ->
            val ratio = (slot.amount / maxVal).toFloat().coerceIn(0f, 1f)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val barFraction = if (slot.amount > 0.0) ratio.coerceAtLeast(0.12f) else 0.05f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(barFraction)
                            .clip(RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp, bottomStart = 4.dp, bottomEnd = 4.dp))
                            .background(
                                if (slot.amount > 0.0) {
                                    Brush.verticalGradient(
                                        colors = if (slot.isHighlighted) {
                                            listOf(AccentPeriwinkle, AccentPeriwinkleDeep)
                                        } else {
                                            listOf(barColor.copy(alpha = 0.85f), barColor.copy(alpha = 0.55f))
                                        }
                                    )
                                } else {
                                    Brush.verticalGradient(listOf(emptySlotColor, emptySlotColor))
                                }
                            )
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = if (isArabic) slot.labelAr else slot.labelEn,
                    fontSize = 11.sp,
                    color = if (slot.isHighlighted) MaterialTheme.colorScheme.onSurface else mutedText,
                    fontWeight = if (slot.isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun TopCategoriesStrip(
    items: List<CategoryBreakdownItem>,
    currency: CurrencyOption,
    isArabic: Boolean,
    accentColor: Color
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant
    val mutedText = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.65f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        if (items.isEmpty()) {
            Text(
                text = if (isArabic) "ستظهر أبرز فئات مصروفاتك هنا تلقائيًا" else "Your top spending categories will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = mutedText,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items.take(3).forEach { catItem ->
                    val catColor = Color(catItem.category.colorHex)
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(catColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = CategoryIconMapper.iconFor(catItem.category.iconName),
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                        Text(
                            text = catItem.category.displayName(isArabic),
                            style = MaterialTheme.typography.labelMedium,
                            color = mutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = DateTimeUtils.formatCurrency(catItem.total, currency, isArabic),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        LinearProgressIndicator(
                            progress = { catItem.ratio.coerceIn(0.08f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = accentColor,
                            trackColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseListItemCard(
    item: ExpenseWithCategory,
    isArabic: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val cat = item.category
    val catColor = Color(cat?.colorHex ?: 0xFF8B8BFF)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(catColor.copy(alpha = 0.16f))
                    .border(1.dp, catColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = CategoryIconMapper.iconFor(cat?.iconName ?: "category"),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.expense.note.ifBlank { cat?.displayName(isArabic) ?: "" },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = cat?.displayName(isArabic) ?: item.expense.categoryId,
                        style = MaterialTheme.typography.labelMedium,
                        color = catColor,
                        maxLines = 1
                    )
                    Text(
                        text = "• ${DateTimeUtils.formatTime(item.expense.timestamp, isArabic)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = DateTimeUtils.formatCurrency(
                        item.expense.amount, SupportedCurrencies.forExpense(item.expense.currencyCode), isArabic
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = if (isArabic) "حذف" else "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
