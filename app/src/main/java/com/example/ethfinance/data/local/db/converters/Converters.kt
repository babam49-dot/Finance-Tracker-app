package com.example.ethfinance.data.local.db.converters

import androidx.room.TypeConverter
import com.example.ethfinance.data.local.db.entity.AccountType
import com.example.ethfinance.data.local.db.entity.CategorySource
import com.example.ethfinance.data.local.db.entity.RuleMatchType
import com.example.ethfinance.data.local.db.entity.TransactionType

class Converters {

    @TypeConverter
    fun fromStringList(list: List<String>?): String {
        return list?.joinToString(";;;") ?: ""
    }

    @TypeConverter
    fun toStringList(data: String?): List<String> {
        if (data.isNullOrBlank()) return emptyList()
        return data.split(";;;")
    }

    @TypeConverter
    fun fromAccountType(type: AccountType?): String? = type?.name

    @TypeConverter
    fun toAccountType(value: String?): AccountType? =
        value?.let { runCatching { AccountType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromTransactionType(type: TransactionType?): String? = type?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? =
        value?.let { runCatching { TransactionType.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromCategorySource(source: CategorySource?): String? = source?.name

    @TypeConverter
    fun toCategorySource(value: String?): CategorySource? =
        value?.let { runCatching { CategorySource.valueOf(it) }.getOrNull() }

    @TypeConverter
    fun fromRuleMatchType(type: RuleMatchType?): String? = type?.name

    @TypeConverter
    fun toRuleMatchType(value: String?): RuleMatchType? =
        value?.let { runCatching { RuleMatchType.valueOf(it) }.getOrNull() }
}
