package com.myexpenses.app.data.backup

import android.content.Context
import android.net.Uri
import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.DefaultCategories
import com.myexpenses.app.data.local.entity.ExpenseEntity
import com.myexpenses.app.data.repository.ExpenseRepository
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RestoreSummary(
    val restoredCategories: Int,
    val restoredExpenses: Int
)

class BackupManager(
    private val context: Context,
    private val repository: ExpenseRepository
) {
    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

    suspend fun exportCsv(uri: Uri): Result<Int> = runCatching {
        val items = repository.getAllExpensesWithCategory()
        val sb = StringBuilder()
        // UTF-8 BOM so Microsoft Excel displays Arabic text cleanly
        sb.append('\uFEFF')
        sb.appendLine("ID,Date,Amount,Currency,Category_AR,Category_EN,Note,Raw_Transcript,Source")
        for (item in items) {
            val e = item.expense
            val dateStr = Instant.ofEpochMilli(e.timestamp)
                .atZone(ZoneId.systemDefault())
                .format(isoDateFormatter)
            sb.append(e.id).append(',')
            sb.append(csvEscape(dateStr)).append(',')
            sb.append(e.amount).append(',')
            sb.append(csvEscape(e.currencyCode)).append(',')
            sb.append(csvEscape(item.category?.nameAr ?: e.categoryId)).append(',')
            sb.append(csvEscape(item.category?.nameEn ?: e.categoryId)).append(',')
            sb.append(csvEscape(e.note)).append(',')
            sb.append(csvEscape(e.rawTranscript)).append(',')
            sb.appendLine(csvEscape(e.source))
        }
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(sb.toString().toByteArray(Charsets.UTF_8))
        } ?: error("Unable to open output stream")
        items.size
    }

    suspend fun exportJsonBackup(uri: Uri): Result<Int> = runCatching {
        val categories = repository.getCategories()
        val expenses = repository.getAllExpensesWithCategory().map { it.expense }

        val root = JSONObject().apply {
            put("app", "MyExpenses")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())

            val catArray = JSONArray()
            for (c in categories) {
                catArray.put(JSONObject().apply {
                    put("id", c.id)
                    put("nameAr", c.nameAr)
                    put("nameEn", c.nameEn)
                    put("iconName", c.iconName)
                    put("colorHex", c.colorHex)
                    put("keywords", c.keywords)
                    put("isDefault", c.isDefault)
                    put("sortOrder", c.sortOrder)
                })
            }
            put("categories", catArray)

            val expArray = JSONArray()
            for (e in expenses) {
                expArray.put(JSONObject().apply {
                    put("amount", e.amount)
                    put("currencyCode", e.currencyCode)
                    put("categoryId", e.categoryId)
                    put("note", e.note)
                    put("rawTranscript", e.rawTranscript)
                    put("timestamp", e.timestamp)
                    put("createdAt", e.createdAt)
                    put("source", e.source)
                })
            }
            put("expenses", expArray)
        }

        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(root.toString(2).toByteArray(Charsets.UTF_8))
        } ?: error("Unable to open output stream")
        expenses.size
    }

    suspend fun restoreJsonBackup(uri: Uri): Result<RestoreSummary> = runCatching {
        val jsonText = context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).readText()
        } ?: error("Unable to read backup file")

        val root = JSONObject(jsonText)
        val categories = mutableListOf<CategoryEntity>()
        val catArray = root.optJSONArray("categories")
        if (catArray != null) {
            for (i in 0 until catArray.length()) {
                val obj = catArray.getJSONObject(i)
                categories += CategoryEntity(
                    id = obj.getString("id"),
                    nameAr = obj.getString("nameAr"),
                    nameEn = obj.getString("nameEn"),
                    iconName = obj.optString("iconName", "category"),
                    colorHex = obj.optLong("colorHex", 0xFF607D8B),
                    keywords = obj.optString("keywords", ""),
                    isDefault = obj.optBoolean("isDefault", false),
                    sortOrder = obj.optInt("sortOrder", i + 1)
                )
            }
        }
        val effectiveCategories = if (categories.isEmpty()) DefaultCategories.list else categories
        val validCatIds = effectiveCategories.map { it.id }.toSet()

        val expenses = mutableListOf<ExpenseEntity>()
        val expArray = root.optJSONArray("expenses")
        if (expArray != null) {
            for (i in 0 until expArray.length()) {
                val obj = expArray.getJSONObject(i)
                val catId = obj.optString("categoryId", DefaultCategories.OTHER)
                expenses += ExpenseEntity(
                    amount = obj.getDouble("amount"),
                    currencyCode = obj.optString("currencyCode", "YER"),
                    categoryId = if (catId in validCatIds) catId else DefaultCategories.OTHER,
                    note = obj.optString("note", ""),
                    rawTranscript = obj.optString("rawTranscript", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    source = obj.optString("source", "VOICE")
                )
            }
        }

        repository.replaceAllFromBackup(effectiveCategories, expenses)
        RestoreSummary(effectiveCategories.size, expenses.size)
    }

    private fun csvEscape(value: String): String {
        val cleaned = value.replace("\r", " ").replace("\n", " ")
        return if (cleaned.contains(',') || cleaned.contains('"')) {
            "\"${cleaned.replace("\"", "\"\"")}\""
        } else {
            cleaned
        }
    }
}
