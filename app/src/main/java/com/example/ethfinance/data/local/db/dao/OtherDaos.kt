package com.example.ethfinance.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ethfinance.data.local.db.entity.BudgetEntity
import com.example.ethfinance.data.local.db.entity.CategoryEntity
import com.example.ethfinance.data.local.db.entity.SyncStateEntity
import com.example.ethfinance.data.local.db.entity.UserRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories WHERE isArchived = 0 ORDER BY sortOrder ASC")
    fun getAllCategoriesFlow(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE `key` = :key LIMIT 1")
    suspend fun getCategoryByKey(key: String): CategoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Update
    suspend fun updateCategory(category: CategoryEntity)
}

@Dao
interface UserRuleDao {

    @Query("SELECT * FROM user_rules ORDER BY timesApplied DESC, createdAt DESC")
    fun getAllRulesFlow(): Flow<List<UserRuleEntity>>

    @Query("SELECT * FROM user_rules")
    suspend fun getAllRules(): List<UserRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: UserRuleEntity): Long

    @Query("UPDATE user_rules SET timesApplied = timesApplied + 1 WHERE id = :id")
    suspend fun incrementTimesApplied(id: Long)

    @Query("DELETE FROM user_rules WHERE id = :id")
    suspend fun deleteRule(id: Long)
}

@Dao
interface BudgetDao {

    @Query("SELECT * FROM budgets")
    fun getAllBudgetsFlow(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity): Long
}

@Dao
interface SyncStateDao {

    @Query("SELECT * FROM sync_state WHERE id = 1")
    fun getSyncStateFlow(): Flow<SyncStateEntity?>

    @Query("SELECT * FROM sync_state WHERE id = 1")
    suspend fun getSyncState(): SyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun setSyncState(state: SyncStateEntity)

    @Query("UPDATE sync_state SET mode = :mode WHERE id = 1")
    suspend fun setMode(mode: String)
}
