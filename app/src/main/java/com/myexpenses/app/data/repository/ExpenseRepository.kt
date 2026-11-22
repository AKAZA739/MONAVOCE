package com.myexpenses.app.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.myexpenses.app.data.local.ExpenseDatabase
import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.CurrencyTotal
import com.myexpenses.app.data.local.entity.DefaultCategories
import com.myexpenses.app.data.local.entity.ExpenseEntity
import com.myexpenses.app.data.local.entity.ExpenseWithCategory
import com.myexpenses.app.quicklaunch.ExpenseWidgetProvider
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val context: Context,
    private val database: ExpenseDatabase
) {
    private val expenseDao = database.expenseDao()
    private val categoryDao = database.categoryDao()

    suspend fun ensureDefaultCategories() = database.withTransaction {
        if (categoryDao.count() == 0) {
            categoryDao.insertAll(DefaultCategories.list)
        }
    }

    fun observeCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()
    suspend fun getCategories(): List<CategoryEntity> {
        ensureDefaultCategories()
        return categoryDao.getAll()
    }

    suspend fun getCategoryById(id: String): CategoryEntity? = categoryDao.getById(id)

    suspend fun saveCategory(category: CategoryEntity) {
        categoryDao.upsert(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        if (category.id == DefaultCategories.OTHER) return
        database.withTransaction {
            expenseDao.reassignCategory(category.id, DefaultCategories.OTHER)
            categoryDao.delete(category)
        }
        refreshWidgets()
    }

    fun observeAllExpenses(): Flow<List<ExpenseWithCategory>> = expenseDao.observeAllWithCategory()

    fun observeExpensesInRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>> =
        expenseDao.observeInRange(startMillis, endMillis)

    suspend fun getExpensesInRange(startMillis: Long, endMillis: Long): List<ExpenseWithCategory> =
        expenseDao.getInRange(startMillis, endMillis)

    suspend fun getCurrencyTotals(startMillis: Long, endMillis: Long, categoryId: String? = null): List<CurrencyTotal> =
        expenseDao.getCurrencyTotals(startMillis, endMillis, categoryId)

    suspend fun getLastCreatedExpense(): ExpenseWithCategory? = expenseDao.getLastCreated()

    suspend fun getAllExpensesWithCategory(): List<ExpenseWithCategory> = expenseDao.getAllWithCategory()

    suspend fun getBackupSnapshot(): Pair<List<CategoryEntity>, List<ExpenseEntity>> = database.withTransaction {
        getCategories() to expenseDao.getAllRaw()
    }

    suspend fun insertExpense(expense: ExpenseEntity): Long {
        ensureDefaultCategories()
        val id = expenseDao.insert(expense)
        refreshWidgets()
        return id
    }

    suspend fun insertExpenses(expenses: List<ExpenseEntity>): List<Long> {
        ensureDefaultCategories()
        val ids = expenseDao.insertAll(expenses)
        refreshWidgets()
        return ids
    }

    suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.update(expense)
        refreshWidgets()
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.delete(expense)
        refreshWidgets()
    }

    suspend fun deleteExpensesByIds(ids: List<Long>) {
        if (ids.isEmpty()) return
        expenseDao.deleteByIds(ids)
        refreshWidgets()
    }

    suspend fun replaceAllFromBackup(
        categories: List<CategoryEntity>,
        expenses: List<ExpenseEntity>
    ) {
        database.withTransaction {
            expenseDao.deleteAll()
            categoryDao.deleteAll()
            categoryDao.insertAll(categories)
            expenseDao.insertAll(expenses)
        }
        refreshWidgets()
    }

    private fun refreshWidgets() {
        runCatching { ExpenseWidgetProvider.requestUpdate(context) }
    }
}
