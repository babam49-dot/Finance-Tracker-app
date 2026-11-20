package com.example.ethfinance.domain.usecase

import com.example.ethfinance.core.util.DateUtils
import com.example.ethfinance.data.local.db.entity.AccountType
import com.example.ethfinance.data.local.db.entity.CategorySource
import com.example.ethfinance.data.local.db.entity.TransactionEntity
import com.example.ethfinance.data.local.db.entity.TransactionType
import com.example.ethfinance.domain.model.Account
import com.example.ethfinance.domain.model.CashFlow
import com.example.ethfinance.domain.model.OverviewSummary
import com.example.ethfinance.domain.model.Transaction
import com.example.ethfinance.domain.repository.AccountRepository
import com.example.ethfinance.domain.repository.ReportRepository
import com.example.ethfinance.domain.repository.SettingsRepository
import com.example.ethfinance.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class GetOverviewUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val reportRepository: ReportRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<OverviewSummary> {
        val now = System.currentTimeMillis()
        val monthRange = DateUtils.getPeriodRange(DateUtils.PeriodType.THIS_MONTH, now)

        return combine(
            accountRepository.getAccounts(includeHidden = false),
            settingsRepository.modeFlow,
            settingsRepository.hideAmountsFlow
        ) { accounts, mode, hideAmounts ->
            Triple(accounts, mode == "DEMO", hideAmounts)
        }.combine(transactionRepository.getPendingReviewCount(isSample = true)) { (accounts, isSample, hideAmounts), reviewCount ->
            // Calculate digital balance and cash balance
            var digitalMinor = 0L
            var cashMinor = 0L

            for (acc in accounts) {
                val bal = acc.currentBalanceMinor ?: 0L
                if (acc.type == AccountType.CASH) {
                    cashMinor += bal
                } else {
                    digitalMinor += bal
                }
            }

            val totalTrackedMinor = digitalMinor + cashMinor

            // For cash flow, in demo mode we reproduce: +4,000.00 income, -676.26 expense, +3,323.74 net flow
            val cashFlow = if (isSample) {
                CashFlow(
                    incomeMinor = 400000L,
                    expenseMinor = 67626L,
                    netFlowMinor = 332374L
                )
            } else {
                CashFlow(0L, 0L, 0L)
            }

            OverviewSummary(
                totalTrackedMinor = totalTrackedMinor,
                digitalBalanceMinor = digitalMinor,
                cashBalanceMinor = cashMinor,
                cashFlow = cashFlow,
                pendingReviewCount = reviewCount,
                accounts = accounts,
                isSampleMode = isSample,
                areAmountsHidden = hideAmounts
            )
        }
    }
}

class LogCashUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(
        amountMinor: Long,
        type: TransactionType,
        categoryId: Long?,
        note: String?,
        occurredAt: Long
    ) {
        val accounts = mutableListOf<Account>()
        accountRepository.getAccounts(includeHidden = true).collect {
            accounts.clear()
            accounts.addAll(it)
        }
        val cashAccount = accounts.firstOrNull { it.type == AccountType.CASH }

        val cashAccountId = cashAccount?.id ?: 3L

        val tx = TransactionEntity(
            accountId = cashAccountId,
            type = type,
            amountMinor = amountMinor,
            categoryId = categoryId,
            categorySource = CategorySource.USER,
            confidence = 1.0f,
            needsReview = false,
            note = note,
            occurredAt = occurredAt,
            isSample = cashAccount?.isSample ?: false
        )
        transactionRepository.insertTransaction(tx)

        // Update cash account balance
        val currentBal = cashAccount?.currentBalanceMinor ?: 0L
        val newBal = when (type) {
            TransactionType.CASH_IN, TransactionType.INCOME -> currentBal + amountMinor
            TransactionType.CASH_OUT, TransactionType.EXPENSE -> currentBal - amountMinor
            else -> currentBal
        }
        accountRepository.updateBalance(cashAccountId, newBal, occurredAt)
    }
}

class ToggleModeUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    suspend fun toLiveMode() {
        settingsRepository.switchToLiveMode()
    }

    suspend fun toDemoMode() {
        settingsRepository.switchToDemoMode()
    }
}
