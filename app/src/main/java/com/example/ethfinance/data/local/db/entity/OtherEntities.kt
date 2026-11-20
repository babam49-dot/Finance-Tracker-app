package com.example.ethfinance.data.local.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RuleMatchType {
    COUNTERPARTY_CONTAINS,
    SMS_CONTAINS,
    AMOUNT_RANGE
}

@Entity(tableName = "user_rules")
data class UserRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val matchType: RuleMatchType,
    val pattern: String,
    val categoryId: Long,
    val createdAt: Long = System.currentTimeMillis(),
    val timesApplied: Int = 0
)

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val categoryId: Long?,
    val periodType: String = "MONTH",
    val limitMinor: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey
    val id: Int = 1,
    val lastInboxScanAt: Long? = null,
    val lastProcessedSmsId: Long? = null,
    val mode: String = "DEMO" // DEMO | LIVE
)
