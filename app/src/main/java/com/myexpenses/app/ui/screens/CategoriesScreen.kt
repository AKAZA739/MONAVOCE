package com.myexpenses.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.DefaultCategories
import com.myexpenses.app.ui.components.CategoryIconMapper

@Composable
fun CategoriesScreen(
    categories: List<CategoryEntity>,
    isArabic: Boolean,
    onSaveCategory: (existingId: String?, nameAr: String, nameEn: String, iconName: String, colorHex: Long, keywords: String) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit
) {
    var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "فئات التصنيف الذكي" else "Smart Categories",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = if (isArabic) "أضف كلماتك الخاصة لتحسين التصنيف الصوتي المحلي"
                        else "Customize keywords to train local offline categorization",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "جديدة" else "New")
                }
            }
        }

        items(categories, key = { it.id }) { cat ->
            val catColor = Color(cat.colorHex)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { editingCategory = cat },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(catColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = CategoryIconMapper.iconFor(cat.iconName),
                            contentDescription = null,
                            tint = catColor
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${cat.nameAr} • ${cat.nameEn}",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = cat.keywordList().take(6).joinToString("، "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { editingCategory = cat }) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                    }
                    if (!cat.isDefault && cat.id != DefaultCategories.OTHER) {
                        IconButton(onClick = { onDeleteCategory(cat) }) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CategoryEditorDialog(
            initial = null,
            isArabic = isArabic,
            onDismiss = { showCreateDialog = false },
            onSave = { ar, en, icon, color, keywords ->
                onSaveCategory(null, ar, en, icon, color, keywords)
                showCreateDialog = false
            }
        )
    }

    editingCategory?.let { cat ->
        CategoryEditorDialog(
            initial = cat,
            isArabic = isArabic,
            onDismiss = { editingCategory = null },
            onSave = { ar, en, icon, color, keywords ->
                onSaveCategory(cat.id, ar, en, icon, color, keywords)
                editingCategory = null
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    initial: CategoryEntity?,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (nameAr: String, nameEn: String, iconName: String, colorHex: Long, keywords: String) -> Unit
) {
    var nameAr by remember(initial) { mutableStateOf(initial?.nameAr ?: "") }
    var nameEn by remember(initial) { mutableStateOf(initial?.nameEn ?: "") }
    var keywords by remember(initial) { mutableStateOf(initial?.keywords ?: "") }
    var iconName by remember(initial) { mutableStateOf(initial?.iconName ?: "category") }
    var colorHex by remember(initial) { mutableLongStateOf(initial?.colorHex ?: 0xFF16A085) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    initial != null && isArabic -> "تعديل الفئة"
                    initial != null -> "Edit Category"
                    isArabic -> "إضافة فئة جديدة"
                    else -> "New Category"
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = nameAr,
                    onValueChange = {
                        nameAr = it
                        nameError = false
                    },
                    label = { Text(if (isArabic) "الاسم بالعربية" else "Arabic Name") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text(if (isArabic) "الاسم بالإنجليزية" else "English Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text(if (isArabic) "كلمات مفتاحية للتعرف الصوتي (افصل بفاصلة)" else "Voice keywords (comma-separated)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = if (isArabic) "الأيقونة" else "Icon",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIconMapper.availableIcons.forEach { (key, icon) ->
                        val selected = iconName == key
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (selected) Color(colorHex).copy(alpha = 0.22f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) Color(colorHex) else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { iconName = key },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) Color(colorHex) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = if (isArabic) "اللون" else "Color",
                    style = MaterialTheme.typography.labelLarge
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryIconMapper.availableColors.forEach { hex ->
                        val selected = colorHex == hex
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (selected) 3.dp else 0.dp,
                                    color = if (selected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { colorHex = hex }
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إلغاء" else "Cancel")
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameAr.isBlank() && nameEn.isBlank()) {
                        nameError = true
                        return@Button
                    }
                    onSave(
                        nameAr.ifBlank { nameEn },
                        nameEn.ifBlank { nameAr },
                        iconName,
                        colorHex,
                        keywords
                    )
                }
            ) {
                Text(if (isArabic) "حفظ" else "Save")
            }
        }
    )
}
