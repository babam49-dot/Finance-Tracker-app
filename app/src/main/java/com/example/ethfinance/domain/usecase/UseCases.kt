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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import javax.inject.Inject

class GetOverviewUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val reportRepository: ReportRepository,
    private val settingsRepository: SettingsRepository
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<OverviewSummary> {
        val now = System.currentTimeMillis()
        val monthRange = DateUtils.getPeriodRange(DateUtils.PeriodType.THIS_MONTH, now)

        return combine(
            accountRepository.getAccounts(includeHidden = false),
            settingsRepository.modeFlow,
            settingsRepository.hideAmountsFlow
        ) { accounts, mode, hideAmounts ->
            Triple(accounts, mode == "DEMO", hideAmounts)
        }.flatMapLatest { (accounts, isSample, hideAmounts) ->
            combine(
                transactionRepository.getPendingReviewCount(isSample = isSample),
                reportRepository.getCashFlowSummary(monthRange.startEpochMillis, monthRange.endEpochMillis, isSample = isSample)
            ) { reviewCount, cashFlowSummary ->
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

                val cashFlow = if (isSample) {
                    CashFlow(
                        incomeMinor = 400000L,
                        expenseMinor = 67626L,
                        netFlowMinor = 332374L
                    )
                } else {
                    val inc = cashFlowSummary.totalIncomeMinor ?: 0L
                    val exp = cashFlowSummary.totalExpenseMinor ?: 0L
                    CashFlow(
                        incomeMinor = inc,
                        expenseMinor = exp,
                        netFlowMinor = inc - exp
                    )
                }

                val displayAccounts = if (!isSample && accounts.isEmpty()) {
                    listOf(
                        Account(
                            id = 101L,
                            type = AccountType.BANK_CBE,
                            displayName = "CBE Account",
                            institutionName = "Commercial Bank of Ethiopia",
                            maskedNumber = "Commercial Bank of Ethiopia",
                            senderIds = listOf("CBE"),
                            currentBalanceMinor = null,
                            balanceUpdatedAt = null,
                            isSample = false,
                            isHidden = false
                        ),
                        Account(
                            id = 102L,
                            type = AccountType.TELEBIRR,
                            displayName = "Telebirr Wallet",
                            institutionName = "Telebirr",
                            maskedNumber = "Telebirr (127)",
                            senderIds = listOf("127"),
                            currentBalanceMinor = null,
                            balanceUpdatedAt = null,
                            isSample = false,
                            isHidden = false
                        ),
                        Account(
                            id = 103L,
                            type = AccountType.CASH,
                            displayName = "Cash Wallet",
                            institutionName = "Physical Cash",
                            maskedNumber = "Physical Cash",
                            senderIds = emptyList(),
                            currentBalanceMinor = null,
                            balanceUpdatedAt = null,
                            isSample = false,
                            isHidden = false
                        )
                    )
                } else {
                    accounts
                }

                OverviewSummary(
                    totalTrackedMinor = totalTrackedMinor,
                    digitalBalanceMinor = digitalMinor,
                    cashBalanceMinor = cashMinor,
                    cashFlow = cashFlow,
                    pendingReviewCount = if (isSample) 2 else reviewCount,
                    accounts = displayAccounts,
                    isSampleMode = isSample,
                    areAmountsHidden = hideAmounts
                )
            }
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
            feeMinor = 0L,
            balanceAfterMinor = null,
            counterpartyName = "Manual Cash",
            reference = null,
            categoryId = categoryId,
            categorySource = CategorySource.USER,
            confidence = 1.0f,
            needsReview = false,
            occurredAt = occurredAt,
            rawSmsHash = null,
            rawSmsBody = null,
            sourceSender = null,
            note = note,
            isSample = false
        )

        transactionRepository.insertTransaction(tx)

        // Update cash account balance
        val currentBalance = cashAccount?.currentBalanceMinor ?: 0L
        val newBalance = if (type == TransactionType.INCOME) {
            currentBalance + amountMinor
        } else {
            currentBalance - amountMinor
        }
        accountRepository.updateBalance(cashAccountId, newBalance, occurredAt)
    }
}
