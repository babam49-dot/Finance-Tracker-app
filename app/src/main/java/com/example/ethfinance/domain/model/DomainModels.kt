package com.example.ethfinance.domain.model

import com.example.ethfinance.data.local.db.entity.AccountType
import com.example.ethfinance.data.local.db.entity.CategorySource
import com.example.ethfinance.data.local.db.entity.TransactionType

data class Account(
    val id: Long,
    val type: AccountType,
    val displayName: String,
    val institutionName: String,
    val maskedNumber: String,
    val senderIds: List<String>,
    val currentBalanceMinor: Long?,
    val balanceUpdatedAt: Long?,
    val isSample: Boolean,
    val isHidden: Boolean
)

data class Transaction(
    val id: Long,
    val accountId: Long,
    val accountName: String = "",
    val accountType: AccountType = AccountType.BANK_CBE,
    val type: TransactionType,
    val amountMinor: Long,
    val feeMinor: Long = 0,
    val balanceAfterMinor: Long? = null,
    val counterpartyName: String? = null,
    val counterpartyRef: String? = null,
    val reference: String? = null,
    val categoryId: Long? = null,
    val categoryName: String? = null,
    val categoryColorHex: String? = null,
    val categoryIcon: String? = null,
    val categorySource: CategorySource = CategorySource.NONE,
    val confidence: Float = 0f,
    val needsReview: Boolean = false,
    val note: String? = null,
    val occurredAt: Long,
    val rawSmsHash: String? = null,
    val rawSmsBody: String? = null,
    val sourceSender: String? = null,
    val isSample: Boolean = false,
    val isExcludedFromReports: Boolean = false
)

data class CashFlow(
    val incomeMinor: Long,
    val expenseMinor: Long,
    val netFlowMinor: Long
)

data class OverviewSummary(
    val totalTrackedMinor: Long,
    val digitalBalanceMinor: Long,
    val cashBalanceMinor: Long,
    val cashFlow: CashFlow,
    val pendingReviewCount: Int,
    val accounts: List<Account>,
    val isSampleMode: Boolean,
    val areAmountsHidden: Boolean
)
