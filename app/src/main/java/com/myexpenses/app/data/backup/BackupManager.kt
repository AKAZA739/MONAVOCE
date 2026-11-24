package com.myexpenses.app.data.backup

import android.content.Context
import android.net.Uri
import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.DefaultCategories
import com.myexpenses.app.data.local.entity.ExpenseEntity
import com.myexpenses.app.data.repository.ExpenseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class RestoreSummary(
    val restoredCategories: Int,
    val restoredExpenses: Int
)

class ValidatedBackup internal constructor(
    internal val categories: List<CategoryEntity>,
    internal val expenses: List<ExpenseEntity>
) {
    val summary get() = RestoreSummary(categories.size, expenses.size)
}

class BackupManager(
    private val context: Context,
    private val repository: ExpenseRepository
) {
    private val isoDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US)

    suspend fun exportCsv(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
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
    }

    suspend fun exportJsonBackup(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val (categories, expenses) = repository.getBackupSnapshot()

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

            val bytes = root.toString().toByteArray(Charsets.UTF_8)
            require(bytes.size <= MAX_BACKUP_BYTES) { "Backup exceeds the 32 MiB limit" }
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(bytes)
            } ?: error("Unable to open output stream")
            expenses.size
        }
    }

    suspend fun readJsonBackup(uri: Uri): Result<ValidatedBackup> = withContext(Dispatchers.IO) {
        runCatching {
            val jsonText = context.contentResolver.openInputStream(uri)?.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                val output = ByteArrayOutputStream()
                var read = input.read(buffer)
                while (read != -1) {
                    require(output.size() + read <= MAX_BACKUP_BYTES) { "Backup exceeds the 32 MiB limit" }
                    output.write(buffer, 0, read)
                    read = input.read(buffer)
                }
                output.toString(Charsets.UTF_8.name()).removePrefix("\uFEFF")
            } ?: error("Unable to read backup file")
            parseBackup(JSONObject(jsonText))
        }
    }

    suspend fun restoreBackup(backup: ValidatedBackup): Result<RestoreSummary> =
        withContext(Dispatchers.IO) {
            runCatching {
                repository.replaceAllFromBackup(backup.categories, backup.expenses)
                backup.summary
            }
        }
}

private const val MAX_BACKUP_BYTES = 32 * 1024 * 1024

internal fun parseBackup(root: JSONObject): ValidatedBackup {
    require(root.get("app") == "MyExpenses" && (root.get("version") as? Number)?.toDouble() == 1.0) {
        "Unsupported backup format"
    }

    val catArray = root.getJSONArray("categories")
    val categories = buildList {
        for (i in 0 until catArray.length()) {
            val obj = catArray.getJSONObject(i)
            add(CategoryEntity(
                id = obj.getString("id"),
                nameAr = obj.getString("nameAr"),
                nameEn = obj.getString("nameEn"),
                iconName = obj.optString("iconName", "category"),
                colorHex = obj.optLong("colorHex", 0xFF607D8B),
                keywords = obj.optString("keywords", ""),
                isDefault = obj.optBoolean("isDefault", false),
                sortOrder = obj.optInt("sortOrder", i + 1)
            ))
        }
    }
    val categoryIds = categories.map { it.id }.toSet()
    require(categoryIds.size == categories.size && categoryIds.none { it.isBlank() }) { "Invalid category IDs" }
    val effectiveCategories = when {
        categories.isEmpty() -> DefaultCategories.list
        DefaultCategories.OTHER !in categoryIds -> categories + DefaultCategories.list.first { it.id == DefaultCategories.OTHER }
        else -> categories
    }
    val validCatIds = effectiveCategories.map { it.id }.toSet()

    val expArray = root.getJSONArray("expenses")
    val expenses = buildList {
        for (i in 0 until expArray.length()) {
            val obj = expArray.getJSONObject(i)
            val catId = obj.optString("categoryId", DefaultCategories.OTHER)
            val amount = obj.getDouble("amount")
            require(amount.isFinite() && amount > 0) { "Invalid expense amount" }
            val currencyCode = obj.optString("currencyCode", "YER").trim().uppercase()
            require(currencyCode.length == 3 && currencyCode.all { it in 'A'..'Z' }) { "Invalid currency code" }
            add(ExpenseEntity(
                amount = amount,
                currencyCode = currencyCode,
                categoryId = if (catId in validCatIds) catId else DefaultCategories.OTHER,
                note = obj.optString("note", ""),
                rawTranscript = obj.optString("rawTranscript", ""),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                source = obj.optString("source", "VOICE")
            ))
        }
    }
    return ValidatedBackup(effectiveCategories, expenses)
}

internal fun csvEscape(value: String): String {
    val cleaned = value.replace('\r', ' ').replace('\n', ' ')
    val firstVisible = cleaned.trimStart { it.isWhitespace() || it.isISOControl() || it.category == CharCategory.FORMAT }
        .firstOrNull()
    val safe = if (firstVisible != null && firstVisible in "=+-@＝＋－＠") "'$cleaned" else cleaned
    return if (safe.contains(',') || safe.contains('"')) {
        "\"${safe.replace("\"", "\"\"")}\""
    } else {
        safe
    }
}
