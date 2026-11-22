package com.myexpenses.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.myexpenses.app.data.local.entity.CategoryTotalRow
import com.myexpenses.app.data.local.entity.ExpenseEntity
import com.myexpenses.app.data.local.entity.ExpenseWithCategory
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC, id DESC")
    fun observeAllWithCategory(): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE timestamp >= :startMillis AND timestamp < :endMillis ORDER BY timestamp DESC, id DESC")
    fun observeInRange(startMillis: Long, endMillis: Long): Flow<List<ExpenseWithCategory>>

    @Transaction
    @Query("SELECT * FROM expenses WHERE timestamp >= :startMillis AND timestamp < :endMillis ORDER BY timestamp DESC, id DESC")
    suspend fun getInRange(startMillis: Long, endMillis: Long): List<ExpenseWithCategory>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE timestamp >= :startMillis AND timestamp < :endMillis")
    fun observeTotalInRange(startMillis: Long, endMillis: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE timestamp >= :startMillis AND timestamp < :endMillis")
    suspend fun getTotalInRange(startMillis: Long, endMillis: Long): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE categoryId = :categoryId AND timestamp >= :startMillis AND timestamp < :endMillis")
    suspend fun getTotalForCategoryInRange(categoryId: String, startMillis: Long, endMillis: Long): Double

    @Query("SELECT COUNT(*) FROM expenses WHERE timestamp >= :startMillis AND timestamp < :endMillis")
    fun observeCountInRange(startMillis: Long, endMillis: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM expenses WHERE timestamp >= :startMillis AND timestamp < :endMillis")
    suspend fun getCountInRange(startMillis: Long, endMillis: Long): Int

    @Query("SELECT categoryId, COALESCE(SUM(amount), 0.0) AS total, COUNT(*) AS count FROM expenses WHERE timestamp >= :startMillis AND timestamp < :endMillis GROUP BY categoryId ORDER BY total DESC")
    fun observeCategoryTotalsInRange(startMillis: Long, endMillis: Long): Flow<List<CategoryTotalRow>>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY createdAt DESC, id DESC LIMIT 1")
    suspend fun getLastCreated(): ExpenseWithCategory?

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC, id DESC")
    suspend fun getAllRaw(): List<ExpenseEntity>

    @Transaction
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC, id DESC")
    suspend fun getAllWithCategory(): List<ExpenseWithCategory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expenses: List<ExpenseEntity>): List<Long>

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Delete
    suspend fun delete(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE expenses SET categoryId = :fallbackCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignCategory(oldCategoryId: String, fallbackCategoryId: String)

    @Query("DELETE FROM expenses")
    suspend fun deleteAll()
}
