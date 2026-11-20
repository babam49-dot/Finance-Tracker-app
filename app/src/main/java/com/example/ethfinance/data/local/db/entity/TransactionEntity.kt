package com.example.ethfinance.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER_IN,
    TRANSFER_OUT,
    FEE,
    CASH_IN,
    CASH_OUT
}

enum class CategorySource {
    USER,
    RULE,
    AUTO,
    NONE
}

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["rawSmsHash"], unique = true),
        Index(value = ["occurredAt"]),
        Index(value = ["accountId"]),
        Index(value = ["categoryId"]),
        Index(value = ["needsReview"]),
        Index(value = ["accountId", "reference"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val accountId: Long,
    val type: TransactionType,
    val amountMinor: Long,
    val feeMinor: Long = 0,
    val balanceAfterMinor: Long? = null,
    val counterpartyName: String? = null,
    val counterpartyRef: String? = null,
    val reference: String? = null,
    val categoryId: Long? = null,
    val categorySource: CategorySource = CategorySource.NONE,
    val confidence: Float = 0f,
    val needsReview: Boolean = false,
    val note: String? = null,
    val occurredAt: Long,
    val rawSmsHash: String? = null,
    val rawSmsBody: String? = null,
    val sourceSender: String? = null,
    val isSample: Boolean = false,
    val isExcludedFromReports: Boolean = false,
    val transferGroupId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
