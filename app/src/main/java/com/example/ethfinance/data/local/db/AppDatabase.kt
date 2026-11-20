package com.example.ethfinance.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.ethfinance.data.local.db.converters.Converters
import com.example.ethfinance.data.local.db.dao.AccountDao
import com.example.ethfinance.data.local.db.dao.BudgetDao
import com.example.ethfinance.data.local.db.dao.CategoryDao
import com.example.ethfinance.data.local.db.dao.SyncStateDao
import com.example.ethfinance.data.local.db.dao.TransactionDao
import com.example.ethfinance.data.local.db.dao.UserRuleDao
import com.example.ethfinance.data.local.db.entity.AccountEntity
import com.example.ethfinance.data.local.db.entity.BudgetEntity
import com.example.ethfinance.data.local.db.entity.CategoryEntity
import com.example.ethfinance.data.local.db.entity.SyncStateEntity
import com.example.ethfinance.data.local.db.entity.TransactionEntity
import com.example.ethfinance.data.local.db.entity.UserRuleEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        UserRuleEntity::class,
        BudgetEntity::class,
        SyncStateEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userRuleDao(): UserRuleDao
    abstract fun budgetDao(): BudgetDao
    abstract fun syncStateDao(): SyncStateDao

    companion object {
        const val DATABASE_NAME = "eth_finance.db"
    }
}
