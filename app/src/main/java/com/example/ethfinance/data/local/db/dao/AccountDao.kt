package com.example.ethfinance.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ethfinance.data.local.db.entity.AccountEntity
import com.example.ethfinance.data.local.db.entity.AccountType
import kotlinx.coroutines.flow.Flow

@Dao
interface AccountDao {

    @Query("SELECT * FROM accounts WHERE (:includeHidden = 1 OR isHidden = 0) ORDER BY id ASC")
    fun getAccountsFlow(includeHidden: Boolean = false): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id")
    suspend fun getAccountById(id: Long): AccountEntity?

    @Query("SELECT * FROM accounts WHERE type = :type AND maskedNumber = :maskedNumber LIMIT 1")
    suspend fun getAccountByTypeAndMask(type: AccountType, maskedNumber: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE type = :type LIMIT 1")
    suspend fun getFirstAccountByType(type: AccountType): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>): List<Long>

    @Update
    suspend fun updateAccount(account: AccountEntity)

    @Query("UPDATE accounts SET currentBalanceMinor = :balanceMinor, balanceUpdatedAt = :updatedAt WHERE id = :id")
    suspend fun updateBalance(id: Long, balanceMinor: Long, updatedAt: Long)

    @Query("DELETE FROM accounts WHERE isSample = 1")
    suspend fun deleteSampleAccounts()

    @Query("DELETE FROM accounts")
    suspend fun deleteAll()
}
