package com.example.ethfinance.data.local.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    indices = [
        Index(value = ["key"], unique = true)
    ]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val key: String,
    val nameResKey: String,
    val iconName: String,
    val colorHex: String,
    val isIncomeCategory: Boolean,
    val isSystem: Boolean = true,
    val sortOrder: Int = 0,
    val isArchived: Boolean = false
)
