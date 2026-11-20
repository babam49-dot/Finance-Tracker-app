package com.example.ethfinance.data.repository

import com.example.ethfinance.data.local.db.dao.AccountDao
import com.example.ethfinance.data.local.db.dao.CategoryDao
import com.example.ethfinance.data.local.db.dao.TransactionDao
import com.example.ethfinance.data.local.db.dao.UserRuleDao
import com.example.ethfinance.data.local.db.entity.AccountEntity
import com.example.ethfinance.data.local.db.entity.AccountType
import com.example.ethfinance.data.local.db.entity.CategoryEntity
import com.example.ethfinance.data.local.db.entity.CategorySource
import com.example.ethfinance.data.local.db.entity.RuleMatchType
import com.example.ethfinance.data.local.db.entity.TransactionEntity
import com.example.ethfinance.data.local.db.entity.UserRuleEntity
import com.example.ethfinance.domain.model.Transaction
import com.example.ethfinance.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val userRuleDao: UserRuleDao
) : TransactionRepository {

    override fun getTransactionsInRange(
        startEpoch: Long,
        endEpoch: Long,
        isSample: Boolean,
        needsReviewOnly: Boolean
    ): Flow<List<Transaction>> {
        return combine(
            transactionDao.getTransactionsInRange(startEpoch, endEpoch, isSample, needsReviewOnly),
            accountDao.getAccountsFlow(includeHidden = true),
            categoryDao.getAllCategoriesFlow()
        ) { txEntities, accounts, categories ->
            val accountMap = accounts.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }

            txEntities.map { entity ->
                val account = accountMap[entity.accountId]
                val category = categoryMap[entity.categoryId]
                entity.toDomain(account, category)
            }
        }
    }

    override fun getReviewTransactions(isSample: Boolean): Flow<List<Transaction>> {
        return combine(
            transactionDao.getReviewTransactionsFlow(isSample),
            accountDao.getAccountsFlow(includeHidden = true),
            categoryDao.getAllCategoriesFlow()
        ) { txEntities, accounts, categories ->
            val accountMap = accounts.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }

            txEntities.map { entity ->
                val account = accountMap[entity.accountId]
                val category = categoryMap[entity.categoryId]
                entity.toDomain(account, category)
            }
        }
    }

    override fun getPendingReviewCount(isSample: Boolean): Flow<Int> {
        return transactionDao.getPendingReviewCountFlow(isSample)
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        val entity = transactionDao.getTransactionById(id) ?: return null
        val account = accountDao.getAccountById(entity.accountId)
        val category = entity.categoryId?.let { categoryDao.getCategoryById(it) }
        return entity.toDomain(account, category)
    }

    override suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insertTransaction(transaction)
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransaction(id)
    }

    override suspend fun updateCategory(transactionId: Long, categoryId: Long, createRule: Boolean) {
        val entity = transactionDao.getTransactionById(transactionId) ?: return
        val updated = entity.copy(
            categoryId = categoryId,
            categorySource = CategorySource.USER,
            confidence = 1.0f,
            needsReview = false,
            updatedAt = System.currentTimeMillis()
        )
        transactionDao.updateTransaction(updated)

        if (createRule && !entity.counterpartyName.isNullOrBlank()) {
            userRuleDao.insertRule(
                UserRuleEntity(
                    matchType = RuleMatchType.COUNTERPARTY_CONTAINS,
                    pattern = entity.counterpartyName,
                    categoryId = categoryId
                )
            )
        }
    }

    private fun TransactionEntity.toDomain(
        account: AccountEntity?,
        category: CategoryEntity?
    ): Transaction {
        return Transaction(
            id = id,
            accountId = accountId,
            accountName = account?.displayName ?: "Account",
            accountType = account?.type ?: AccountType.BANK_CBE,
            type = type,
            amountMinor = amountMinor,
            feeMinor = feeMinor,
            balanceAfterMinor = balanceAfterMinor,
            counterpartyName = counterpartyName,
            counterpartyRef = counterpartyRef,
            reference = reference,
            categoryId = categoryId,
            categoryName = category?.nameResKey ?: "Uncategorized",
            categoryColorHex = category?.colorHex ?: "#9CA3AF",
            categoryIcon = category?.iconName ?: "help_outline",
            categorySource = categorySource,
            confidence = confidence,
            needsReview = needsReview,
            note = note,
            occurredAt = occurredAt,
            rawSmsHash = rawSmsHash,
            rawSmsBody = rawSmsBody,
            sourceSender = sourceSender,
            isSample = isSample,
            isExcludedFromReports = isExcludedFromReports
        )
    }
}
