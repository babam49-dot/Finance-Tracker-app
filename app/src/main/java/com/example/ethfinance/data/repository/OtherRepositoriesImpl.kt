package com.example.ethfinance.data.repository

import com.example.ethfinance.data.local.db.dao.AccountDao
import com.example.ethfinance.data.local.db.dao.AccountSpending
import com.example.ethfinance.data.local.db.dao.CategoryDao
import com.example.ethfinance.data.local.db.dao.CategorySpending
import com.example.ethfinance.data.local.db.dao.CashFlowSummary
import com.example.ethfinance.data.local.db.dao.MerchantSpend
import com.example.ethfinance.data.local.db.dao.TransactionDao
import com.example.ethfinance.data.local.db.entity.AccountEntity
import com.example.ethfinance.data.local.db.entity.AccountType
import com.example.ethfinance.data.local.prefs.SettingsDataStore
import com.example.ethfinance.domain.model.Account
import com.example.ethfinance.domain.model.Transaction
import com.example.ethfinance.domain.repository.AccountRepository
import com.example.ethfinance.domain.repository.ReportRepository
import com.example.ethfinance.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAccounts(includeHidden: Boolean): Flow<List<Account>> {
        return accountDao.getAccountsFlow(includeHidden).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAccountById(id: Long): Account? {
        return accountDao.getAccountById(id)?.toDomain()
    }

    override suspend fun updateBalance(id: Long, balanceMinor: Long, updatedAt: Long) {
        accountDao.updateBalance(id, balanceMinor, updatedAt)
    }

    override suspend fun insertAccount(account: AccountEntity): Long {
        return accountDao.insertAccount(account)
    }

    override suspend fun updateAccount(account: AccountEntity) {
        accountDao.updateAccount(account)
    }

    private fun AccountEntity.toDomain() = Account(
        id = id,
        type = type,
        displayName = displayName,
        institutionName = institutionName,
        maskedNumber = maskedNumber,
        senderIds = senderIds,
        currentBalanceMinor = currentBalanceMinor,
        balanceUpdatedAt = balanceUpdatedAt,
        isSample = isSample,
        isHidden = isHidden
    )
}

@Singleton
class ReportRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao
) : ReportRepository {

    override fun getCashFlowSummary(
        startEpoch: Long,
        endEpoch: Long,
        isSample: Boolean
    ): Flow<CashFlowSummary> {
        return transactionDao.getCashFlowSummary(startEpoch, endEpoch, isSample)
    }

    override fun getSpendingByCategory(
        startEpoch: Long,
        endEpoch: Long,
        isSample: Boolean
    ): Flow<List<CategorySpending>> {
        return transactionDao.getSpendingByCategory(startEpoch, endEpoch, isSample)
    }

    override fun getSpendingByAccount(
        startEpoch: Long,
        endEpoch: Long,
        isSample: Boolean
    ): Flow<List<AccountSpending>> {
        return transactionDao.getSpendingByAccount(startEpoch, endEpoch, isSample)
    }

    override fun getTopMerchants(
        startEpoch: Long,
        endEpoch: Long,
        limit: Int,
        isSample: Boolean
    ): Flow<List<MerchantSpend>> {
        return transactionDao.getTopMerchants(startEpoch, endEpoch, limit, isSample)
    }

    override fun getBiggestExpenses(
        startEpoch: Long,
        endEpoch: Long,
        limit: Int,
        isSample: Boolean
    ): Flow<List<Transaction>> {
        return combine(
            transactionDao.getBiggestExpenses(startEpoch, endEpoch, limit, isSample),
            accountDao.getAccountsFlow(includeHidden = true),
            categoryDao.getAllCategoriesFlow()
        ) { txEntities, accounts, categories ->
            val accountMap = accounts.associateBy { it.id }
            val categoryMap = categories.associateBy { it.id }

            txEntities.map { entity ->
                val account = accountMap[entity.accountId]
                val category = categoryMap[entity.categoryId]
                Transaction(
                    id = entity.id,
                    accountId = entity.accountId,
                    accountName = account?.displayName ?: "Account",
                    accountType = account?.type ?: AccountType.BANK_CBE,
                    type = entity.type,
                    amountMinor = entity.amountMinor,
                    feeMinor = entity.feeMinor,
                    balanceAfterMinor = entity.balanceAfterMinor,
                    counterpartyName = entity.counterpartyName,
                    counterpartyRef = entity.counterpartyRef,
                    reference = entity.reference,
                    categoryId = entity.categoryId,
                    categoryName = category?.nameResKey ?: "Uncategorized",
                    categoryColorHex = category?.colorHex ?: "#9CA3AF",
                    categoryIcon = category?.iconName ?: "help_outline",
                    categorySource = entity.categorySource,
                    confidence = entity.confidence,
                    needsReview = entity.needsReview,
                    note = entity.note,
                    occurredAt = entity.occurredAt,
                    rawSmsHash = entity.rawSmsHash,
                    rawSmsBody = entity.rawSmsBody,
                    sourceSender = entity.sourceSender,
                    isSample = entity.isSample,
                    isExcludedFromReports = entity.isExcludedFromReports
                )
            }
        }
    }
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao
) : SettingsRepository {

    override val modeFlow = settingsDataStore.modeFlow
    override val themeFlow = settingsDataStore.themeFlow
    override val fontScaleFlow = settingsDataStore.fontScaleFlow
    override val boldNumbersFlow = settingsDataStore.boldNumbersFlow
    override val languageFlow = settingsDataStore.languageFlow
    override val useEthiopianCalendarFlow = settingsDataStore.useEthiopianCalendarFlow
    override val currencyDisplayFlow = settingsDataStore.currencyDisplayFlow
    override val hideAmountsFlow = settingsDataStore.hideAmountsFlow

    override suspend fun setMode(mode: String) = settingsDataStore.setMode(mode)
    override suspend fun setTheme(theme: String) = settingsDataStore.setTheme(theme)
    override suspend fun setFontScale(scale: Float) = settingsDataStore.setFontScale(scale)
    override suspend fun setBoldNumbers(bold: Boolean) = settingsDataStore.setBoldNumbers(bold)
    override suspend fun setLanguage(lang: String) = settingsDataStore.setLanguage(lang)
    override suspend fun setEthiopianCalendar(enabled: Boolean) = settingsDataStore.setEthiopianCalendar(enabled)
    override suspend fun setCurrencyDisplay(currency: String) = settingsDataStore.setCurrencyDisplay(currency)
    override suspend fun toggleHideAmounts() = settingsDataStore.toggleHideAmounts()

    override suspend fun switchToLiveMode() {
        // Clear demo sample data only; real data is never cleared
        accountDao.deleteSampleAccounts()
        transactionDao.deleteSampleTransactions()
        settingsDataStore.setMode("LIVE")
    }

    override suspend fun switchToDemoMode() {
        // Switching to Demo hides real data from active view without deleting it
        settingsDataStore.setMode("DEMO")
    }

    override suspend fun deleteAllUserData() {
        accountDao.deleteAll()
        transactionDao.deleteAll()
        settingsDataStore.setMode("DEMO")
    }
}
