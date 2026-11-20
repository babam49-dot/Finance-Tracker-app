package com.example.ethfinance.data.local.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class AccountType {
    BANK_CBE,
    TELEBIRR,
    BANK_OTHER,
    CASH
}

@Entity(
    tableName = "accounts",
    indices = [
        Index(value = ["maskedNumber", "type"])
    ]
)
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: AccountType,
    val displayName: String,
    val institutionName: String,
    val maskedNumber: String,
    val senderIds: List<String>,
    val currentBalanceMinor: Long?,
    val balanceUpdatedAt: Long?,
    val isSample: Boolean = false,
    val isHidden: Boolean = false,
    val colorTag: Int? = null
)
