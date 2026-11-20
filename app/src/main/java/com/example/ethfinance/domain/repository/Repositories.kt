package com.example.ethfinance.domain.repository

import com.example.ethfinance.data.local.db.dao.AccountSpending
import com.example.ethfinance.data.local.db.dao.CashFlowSummary
import com.example.ethfinance.data.local.db.dao.CategorySpending
import com.example.ethfinance.data.local.db.dao.MerchantSpend
import com.example.ethfinance.data.local.db.entity.AccountEntity
import com.example.ethfinance.data.local.db.entity.CategoryEntity
import com.example.ethfinance.data.local.db.entity.TransactionEntity
import com.example.ethfinance.data.local.db.entity.UserRuleEntity
import com.example.ethfinance.domain.model.Account
import com.example.ethfinance.domain.model.OverviewSummary
import com.example.ethfinance.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getTransactionsInRange(
        startEpoch: Long,
        endEpoch: Long,
        isSample: Boolean,
        needsReviewOnly: Boolean = false
    ): Flow<List<Transaction>>

    fun getReviewTransactions(isSample: Boolean): Flow<List<Transaction>>
    fun getPendingReviewCount(isSample: Boolean): Flow<Int>
    suspend fun getTransactionById(id: Long): Transaction?
    suspend fun insertTransaction(transaction: TransactionEntity): Long
    suspend fun updateTransaction(transaction: TransactionEntity)
    suspend fun deleteTransaction(id: Long)
    suspend fun updateCategory(transactionId: Long, categoryId: Long, createRule: Boolean)
}

interface AccountRepository {
    fun getAccounts(includeHidden: Boolean = false): Flow<List<Account>>
    suspend fun getAccountById(id: Long): Account?
    suspend fun updateBalance(id: Long, balanceMinor: Long, updatedAt: Long)
    suspend fun insertAccount(account: AccountEntity): Long
    suspend fun updateAccount(account: AccountEntity)
}

interface ReportRepository {
    fun getCashFlowSummary(startEpoch: Long, endEpoch: Long, isSample: Boolean): Flow<CashFlowSummary>
    fun getSpendingByCategory(startEpoch: Long, endEpoch: Long, isSample: Boolean): Flow<List<CategorySpending>>
    fun getSpendingByAccount(startEpoch: Long, endEpoch: Long, isSample: Boolean): Flow<List<AccountSpending>>
    fun getTopMerchants(startEpoch: Long, endEpoch: Long, limit: Int, isSample: Boolean): Flow<List<MerchantSpend>>
    fun getBiggestExpenses(startEpoch: Long, endEpoch: Long, limit: Int, isSample: Boolean): Flow<List<Transaction>>
}

interface SettingsRepository {
    val modeFlow: Flow<String>
    val themeFlow: Flow<String>
    val fontScaleFlow: Flow<Float>
    val boldNumbersFlow: Flow<Boolean>
    val languageFlow: Flow<String>
    val useEthiopianCalendarFlow: Flow<Boolean>
    val currencyDisplayFlow: Flow<String>
    val hideAmountsFlow: Flow<Boolean>

    suspend fun setMode(mode: String)
    suspend fun setTheme(theme: String)
    suspend fun setFontScale(scale: Float)
    suspend fun setBoldNumbers(bold: Boolean)
    suspend fun setLanguage(lang: String)
    suspend fun setEthiopianCalendar(enabled: Boolean)
    suspend fun setCurrencyDisplay(currency: String)
    suspend fun toggleHideAmounts()
    suspend fun switchToLiveMode()
    suspend fun switchToDemoMode()
    suspend fun deleteAllUserData()
}
