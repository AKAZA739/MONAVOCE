package com.myexpenses.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.DefaultCategories
import com.myexpenses.app.data.local.entity.ExpenseEntity
import com.myexpenses.app.data.preferences.CurrencyOption
import com.myexpenses.app.domain.DateTimeUtils
import com.myexpenses.app.domain.parser.TextNormalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExpenseEditorDialog(
    initialExpense: ExpenseEntity?,
    categories: List<CategoryEntity>,
    currency: CurrencyOption,
    isArabic: Boolean,
    defaultDate: LocalDate = LocalDate.now(),
    onDismiss: () -> Unit,
    onSave: (amount: Double, categoryId: String, note: String, timestamp: Long) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var amountText by remember(initialExpense) {
        mutableStateOf(
            if (initialExpense != null) {
                if (initialExpense.amount % 1.0 == 0.0) initialExpense.amount.toLong().toString()
                else initialExpense.amount.toString()
            } else ""
        )
    }
    var selectedCategoryId by remember(initialExpense) {
        mutableStateOf(initialExpense?.categoryId ?: categories.firstOrNull()?.id ?: DefaultCategories.OTHER)
    }
    var note by remember(initialExpense) {
        mutableStateOf(initialExpense?.note ?: "")
    }
    var selectedDate by remember(initialExpense) {
        mutableStateOf(
            if (initialExpense != null) DateTimeUtils.toLocalDate(initialExpense.timestamp)
            else defaultDate
        )
    }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when {
                    initialExpense != null && isArabic -> "تعديل المصروف"
                    initialExpense != null -> "Edit Expense"
                    isArabic -> "إضافة مصروف يدويًا"
                    else -> "Add Expense Manually"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        amountError = false
                    },
                    label = {
                        val sym = if (isArabic) currency.symbolAr else currency.symbolEn
                        Text(if (isArabic) "المبلغ ($sym)" else "Amount ($sym)")
                    },
                    isError = amountError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text(if (isArabic) "الوصف / الملاحظة" else "Description / Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Date Selector (Today / Yesterday / 2 days ago)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val today = LocalDate.now()
                    val options = listOf(
                        today to (if (isArabic) "اليوم" else "Today"),
                        today.minusDays(1) to (if (isArabic) "أمس" else "Yesterday"),
                        today.minusDays(2) to (if (isArabic) "أول أمس" else "2d ago")
                    )
                    options.forEach { (date, label) ->
                        FilterChip(
                            selected = selectedDate == date,
                            onClick = { selectedDate = date },
                            label = { Text(label) }
                        )
                    }
                }

                Text(
                    text = if (isArabic) "الفئة" else "Category",
                    style = MaterialTheme.typography.labelLarge
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { cat ->
                        val selected = selectedCategoryId == cat.id
                        val catColor = Color(cat.colorHex)
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategoryId = cat.id },
                            label = { Text(cat.displayName(isArabic)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = CategoryIconMapper.iconFor(cat.iconName),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (selected) catColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.16f)
                            )
                        )
                    }
                }

                if (initialExpense != null && initialExpense.rawTranscript.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = (if (isArabic) "النص الصوتي الأصلي: " else "Voice transcript: ") +
                            "\"${initialExpense.rawTranscript}\"",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (isArabic) "حذف" else "Delete",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val normalized = TextNormalizer.normalizeDigits(amountText).replace(",", "").trim()
                    val parsed = normalized.toDoubleOrNull()
                    if (parsed == null || parsed <= 0.0) {
                        amountError = true
                        return@Button
                    }
                    val targetTime = if (selectedDate == LocalDate.now()) LocalTime.now() else LocalTime.of(12, 0)
                    val ts = if (initialExpense != null && DateTimeUtils.toLocalDate(initialExpense.timestamp) == selectedDate) {
                        initialExpense.timestamp
                    } else {
                        selectedDate.atTime(targetTime).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }
                    val cat = categories.firstOrNull { it.id == selectedCategoryId }
                    val finalNote = note.trim().ifBlank { cat?.displayName(isArabic) ?: "" }
                    onSave(parsed, selectedCategoryId, finalNote, ts)
                }
            ) {
                Text(if (isArabic) "حفظ" else "Save")
            }
        }
    )
}
