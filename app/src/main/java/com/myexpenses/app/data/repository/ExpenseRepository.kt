package com.myexpenses.app.data.repository

import android.content.Context
import com.myexpenses.app.data.local.ExpenseDatabase
import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.CategoryTotalRow
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

    suspend fun ensureDefaultCategories() {
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
        categoryDao.insert(category)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        if (category.id == DefaultCategories.OTHER) return
        expenseDao.reassignCategory(category.id, DefaultCategories.OTHER)
        categoryDao.delete(category)
        refreshWidgets()
    }

    fun observeAllExpenses(): Flow<List<ExpenseWithCategory>> = expenseDao.observeAllWithCategory()

    fun observeExpensesInRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>> =
        expenseDao.observeInRange(startMillis, endMillis)

    suspend fun getExpensesInRange(startMillis: Long, endMillis: Long): List<ExpenseWithCategory> =
        expenseDao.getInRange(startMillis, endMillis)

    fun observeTotalInRange(startMillis: Long, endMillis: Long): Flow<Double> =
        expenseDao.observeTotalInRange(startMillis, endMillis)

    suspend fun getTotalInRange(startMillis: Long, endMillis: Long): Double =
        expenseDao.getTotalInRange(startMillis, endMillis)

    suspend fun getTotalForCategoryInRange(
        categoryId: String,
        startMillis: Long,
        endMillis: Long
    ): Double = expenseDao.getTotalForCategoryInRange(categoryId, startMillis, endMillis)

    fun observeCountInRange(startMillis: Long, endMillis: Long): Flow<Int> =
        expenseDao.observeCountInRange(startMillis, endMillis)

    suspend fun getCountInRange(startMillis: Long, endMillis: Long): Int =
        expenseDao.getCountInRange(startMillis, endMillis)

    fun observeCategoryTotalsInRange(startMillis: Long, endMillis: Long): Flow<List<CategoryTotalRow>> =
        expenseDao.observeCategoryTotalsInRange(startMillis, endMillis)

    suspend fun getLastCreatedExpense(): ExpenseWithCategory? = expenseDao.getLastCreated()

    suspend fun getAllExpensesWithCategory(): List<ExpenseWithCategory> = expenseDao.getAllWithCategory()

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
        if (categories.isNotEmpty()) {
            categoryDao.insertAll(categories)
        } else {
            ensureDefaultCategories()
        }
        expenseDao.deleteAll()
        if (expenses.isNotEmpty()) {
            expenseDao.insertAll(expenses)
        }
        refreshWidgets()
    }

    private fun refreshWidgets() {
        runCatching { ExpenseWidgetProvider.requestUpdate(context) }
    }
}
