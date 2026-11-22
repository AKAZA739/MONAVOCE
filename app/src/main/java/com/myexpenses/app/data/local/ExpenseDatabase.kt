package com.myexpenses.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.myexpenses.app.data.local.dao.CategoryDao
import com.myexpenses.app.data.local.dao.ExpenseDao
import com.myexpenses.app.data.local.entity.CategoryEntity
import com.myexpenses.app.data.local.entity.DefaultCategories
import com.myexpenses.app.data.local.entity.ExpenseEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@Database(
    entities = [ExpenseEntity::class, CategoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ExpenseDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseDatabase? = null

        fun getInstance(context: Context): ExpenseDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): ExpenseDatabase {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            return Room.databaseBuilder(
                context,
                ExpenseDatabase::class.java,
                "my_expenses_db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        scope.launch {
                            INSTANCE?.categoryDao()?.insertAll(DefaultCategories.list)
                        }
                    }
                })
                .build()
        }
    }
}
