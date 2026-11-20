package com.example.ethfinance.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ethfinance.data.local.db.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

data class CashFlowSummary(
    val totalIncomeMinor: Long?,
    val totalExpenseMinor: Long?,
    val totalFeeMinor: Long?
)

data class CategorySpending(
    val categoryId: Long?,
    val totalMinor: Long,
    val count: Int
)

data class AccountSpending(
    val accountId: Long,
    val totalMinor: Long
)

data class MerchantSpend(
    val counterpartyName: String,
    val totalMinor: Long,
    val count: Int
)

@Dao
interface TransactionDao {

    @Query("""
        SELECT * FROM transactions 
        WHERE occurredAt BETWEEN :startEpoch AND :endEpoch
        AND (:isSample = isSample)
        AND (:needsReviewOnly = 0 OR needsReview = 1)
        ORDER BY occurredAt DESC
    """)
    fun getTransactionsInRange(
        startEpoch: Long,
        endEpoch: Long,
        isSample: Boolean,
        needsReviewOnly: Boolean = false
    ): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE rawSmsHash = :hash LIMIT 1")
    suspend fun getTransactionByHash(hash: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE accountId = :accountId AND reference = :reference LIMIT 1")
    suspend fun getTransactionByReference(accountId: Long, reference: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE needsReview = 1 AND isSample = :isSample ORDER BY occurredAt DESC")
    fun getReviewTransactionsFlow(isSample: Boolean): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE needsReview = 1 AND isSample = :isSample")
    fun getPendingReviewCountFlow(isSample: Boolean): Flow<Int>

    // Aggregations using SQL
    @Query("""
        SELECT 
            SUM(CASE WHEN type IN ('INCOME', 'CASH_IN') AND isExcludedFromReports = 0 THEN amountMinor ELSE 0 END) AS totalIncomeMinor,
            SUM(CASE WHEN type IN ('EXPENSE', 'CASH_OUT') AND isExcludedFromReports = 0 THEN amountMinor ELSE 0 END) AS totalExpenseMinor,
            SUM(CASE WHEN isExcludedFromReports = 0 THEN feeMinor ELSE 0 END) AS totalFeeMinor
        FROM transactions
        WHERE occurredAt BETWEEN :startEpoch AND :endEpoch
        AND isSample = :isSample
    """)
    fun getCashFlowSummary(startEpoch: Long, endEpoch: Long, isSample: Boolean): Flow<CashFlowSummary>

    @Query("""
        SELECT categoryId, SUM(amountMinor) AS totalMinor, COUNT(*) AS count
        FROM transactions
        WHERE occurredAt BETWEEN :startEpoch AND :endEpoch
        AND type IN ('EXPENSE', 'CASH_OUT', 'FEE')
        AND isExcludedFromReports = 0
        AND isSample = :isSample
        GROUP BY categoryId
        ORDER BY totalMinor DESC
    """)
    fun getSpendingByCategory(startEpoch: Long, endEpoch: Long, isSample: Boolean): Flow<List<CategorySpending>>

    @Query("""
        SELECT accountId, SUM(amountMinor) AS totalMinor
        FROM transactions
        WHERE occurredAt BETWEEN :startEpoch AND :endEpoch
        AND type IN ('EXPENSE', 'CASH_OUT', 'FEE')
        AND isExcludedFromReports = 0
        AND isSample = :isSample
        GROUP BY accountId
    """)
    fun getSpendingByAccount(startEpoch: Long, endEpoch: Long, isSample: Boolean): Flow<List<AccountSpending>>

    @Query("""
        SELECT counterpartyName, SUM(amountMinor) AS totalMinor, COUNT(*) AS count
        FROM transactions
        WHERE occurredAt BETWEEN :startEpoch AND :endEpoch
        AND type IN ('EXPENSE', 'CASH_OUT', 'FEE')
        AND isExcludedFromReports = 0
        AND counterpartyName IS NOT NULL
        AND counterpartyName != ''
        AND isSample = :isSample
        GROUP BY counterpartyName
        ORDER BY totalMinor DESC
        LIMIT :limit
    """)
    fun getTopMerchants(startEpoch: Long, endEpoch: Long, limit: Int, isSample: Boolean): Flow<List<MerchantSpend>>

    @Query("""
        SELECT * FROM transactions
        WHERE occurredAt BETWEEN :startEpoch AND :endEpoch
        AND type IN ('EXPENSE', 'CASH_OUT', 'FEE')
        AND isExcludedFromReports = 0
        AND isSample = :isSample
        ORDER BY amountMinor DESC
        LIMIT :limit
    """)
    fun getBiggestExpenses(startEpoch: Long, endEpoch: Long, limit: Int, isSample: Boolean): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    @Query("DELETE FROM transactions WHERE isSample = 1")
    suspend fun deleteSampleTransactions()

    @Query("DELETE FROM transactions")
    suspend fun deleteAll()
}
