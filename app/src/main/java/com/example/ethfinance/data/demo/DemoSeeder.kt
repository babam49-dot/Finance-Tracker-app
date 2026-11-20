package com.example.ethfinance.data.demo

import com.example.ethfinance.data.local.db.CategorySeed
import com.example.ethfinance.data.local.db.dao.AccountDao
import com.example.ethfinance.data.local.db.dao.CategoryDao
import com.example.ethfinance.data.local.db.dao.SyncStateDao
import com.example.ethfinance.data.local.db.dao.TransactionDao
import com.example.ethfinance.data.local.db.entity.AccountEntity
import com.example.ethfinance.data.local.db.entity.AccountType
import com.example.ethfinance.data.local.db.entity.CategorySource
import com.example.ethfinance.data.local.db.entity.SyncStateEntity
import com.example.ethfinance.data.local.db.entity.TransactionEntity
import com.example.ethfinance.data.local.db.entity.TransactionType
import java.util.Calendar
import java.util.Random
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DemoSeeder @Inject constructor(
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val syncStateDao: SyncStateDao
) {

    suspend fun seedIfNeeded() {
        val syncState = syncStateDao.getSyncState()
        if (syncState != null) return // Already seeded

        // 1. Seed categories
        categoryDao.insertCategories(CategorySeed.getDefaultCategories())

        val now = System.currentTimeMillis()

        // 2. Seed Demo Accounts
        // CBE Main Account "1********8018": 6,194.34 ETB, updated 18 min ago
        val cbeAccount = AccountEntity(
            id = 1,
            type = AccountType.BANK_CBE,
            displayName = "CBE Main Account",
            institutionName = "Commercial Bank of Ethiopia",
            maskedNumber = "1********8018",
            senderIds = listOf("CBE", "CBEBirr"),
            currentBalanceMinor = 619434L, // 6,194.34 ETB
            balanceUpdatedAt = now - (18 * 60 * 1000L),
            isSample = true,
            isHidden = false
        )

        // Telebirr Wallet "091****890": 1,840.50 ETB, updated 3 h ago
        val telebirrAccount = AccountEntity(
            id = 2,
            type = AccountType.TELEBIRR,
            displayName = "Telebirr Wallet",
            institutionName = "Ethio telecom",
            maskedNumber = "091****890",
            senderIds = listOf("127", "telebirr"),
            currentBalanceMinor = 184050L, // 1,840.50 ETB
            balanceUpdatedAt = now - (3 * 3600 * 1000L),
            isSample = true,
            isHidden = false
        )

        // Cash Wallet: 2,800.00 ETB
        val cashAccount = AccountEntity(
            id = 3,
            type = AccountType.CASH,
            displayName = "Cash Wallet",
            institutionName = "Physical Cash",
            maskedNumber = "Cash",
            senderIds = emptyList(),
            currentBalanceMinor = 280000L, // 2,800.00 ETB
            balanceUpdatedAt = now - (30 * 60 * 1000L),
            isSample = true,
            isHidden = false
        )

        accountDao.insertAccounts(listOf(cbeAccount, telebirrAccount, cashAccount))

        // 3. Seed deterministic Transactions for this month & last 60 days
        // Requirements:
        // This month's cash flow must reproduce:
        // Income = +4,000.00 ETB (400000L santim)
        // Expenses = -676.26 ETB (67626L santim)
        // Net flow = +3,323.74 ETB (332374L santim)
        // Exactly 2 transactions have needsReview = true

        val txList = mutableListOf<TransactionEntity>()
        var txId = 1L

        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 9)
        }
        val thisMonthStart = cal.timeInMillis

        // Transaction 1: Salary Income +4,000.00 ETB
        txList.add(
            TransactionEntity(
                id = txId++,
                accountId = 1,
                type = TransactionType.INCOME,
                amountMinor = 400000L, // 4,000.00 ETB
                feeMinor = 0,
                balanceAfterMinor = 1019434L,
                counterpartyName = "EMPLOYER PLC",
                reference = "FT26301SAL",
                categoryId = 16, // Salary
                categorySource = CategorySource.AUTO,
                confidence = 0.95f,
                needsReview = false,
                occurredAt = thisMonthStart + (2 * 3600 * 1000L),
                rawSmsHash = "sample_hash_salary",
                sourceSender = "CBE",
                isSample = true
            )
        )

        // Expenses for THIS MONTH totaling exactly 676.26 ETB (67626 santim)
        // Item 1: Ride Taxi (Transport) 120.00 ETB
        txList.add(
            TransactionEntity(
                id = txId++,
                accountId = 1,
                type = TransactionType.EXPENSE,
                amountMinor = 12000L,
                feeMinor = 0,
                balanceAfterMinor = 1007434L,
                counterpartyName = "Ride Taxi",
                reference = "FT26316XYZ",
                categoryId = 1, // Transport
                categorySource = CategorySource.AUTO,
                confidence = 0.9f,
                needsReview = false,
                occurredAt = now - (2 * 3600 * 1000L),
                rawSmsHash = "sample_hash_ride",
                sourceSender = "CBE",
                isSample = true
            )
        )

        // Item 2: Lunch at Cafe (Food) 180.50 ETB
        txList.add(
            TransactionEntity(
                id = txId++,
                accountId = 2,
                type = TransactionType.EXPENSE,
                amountMinor = 18050L,
                feeMinor = 57,
                balanceAfterMinor = 184050L,
                counterpartyName = "Tomoca Coffee & Cafe",
                reference = "ABC123XYZ",
                categoryId = 2, // Food
                categorySource = CategorySource.AUTO,
                confidence = 0.88f,
                needsReview = false,
                occurredAt = now - (5 * 3600 * 1000L),
                rawSmsHash = "sample_hash_lunch",
                sourceSender = "127",
                isSample = true
            )
        )

        // Item 3: Ethio telecom Airtime (Airtime) 50.00 ETB
        txList.add(
            TransactionEntity(
                id = txId++,
                accountId = 2,
                type = TransactionType.EXPENSE,
                amountMinor = 5000L,
                feeMinor = 0,
                balanceAfterMinor = 202107L,
                counterpartyName = "Ethio telecom",
                reference = "DEF456AIR",
                categoryId = 4, // Airtime
                categorySource = CategorySource.AUTO,
                confidence = 0.95f,
                needsReview = false,
                occurredAt = now - (24 * 3600 * 1000L),
                rawSmsHash = "sample_hash_airtime",
                sourceSender = "127",
                isSample = true
            )
        )

        // Item 4: EEU Electricity Bill (Bills) 140.76 ETB
        txList.add(
            TransactionEntity(
                id = txId++,
                accountId = 1,
                type = TransactionType.EXPENSE,
                amountMinor = 14076L,
                feeMinor = 0,
                balanceAfterMinor = 619434L,
                counterpartyName = "EEU Utility",
                reference = "EEU884920",
                categoryId = 5, // Bills
                categorySource = CategorySource.AUTO,
                confidence = 0.92f,
                needsReview = false,
                occurredAt = now - (3 * 24 * 3600 * 1000L),
                rawSmsHash = "sample_hash_eeu",
                sourceSender = "CBE",
                isSample = true
            )
        )

        // Item 5 (Needs Review 1): Supermarket Groceries 110.00 ETB
        // (120 + 180.50 + 50 + 140.76 + 110 = 601.26; remaining = 75.00)
        txList.add(
            TransactionEntity(
                id = txId++,
                accountId = 1,
                type = TransactionType.EXPENSE,
                amountMinor = 11000L,
                feeMinor = 0,
                counterpartyName = "Shoa Supermarket Bole",
                reference = "SH992144",
                categoryId = 3, // Suggested Groceries
                categorySource = CategorySource.AUTO,
                confidence = 0.65f, // Suggestion threshold
                needsReview = true, // Review Item #1
                note = "Suggested: Groceries (65% confidence)",
                occurredAt = now - (4 * 24 * 3600 * 1000L),
                rawSmsHash = "sample_hash_shoa",
                sourceSender = "CBE",
                isSample = true
            )
        )

        // Item 6 (Needs Review 2): Bajaj transport 75.00 ETB
        // Total expenses = 120.00 + 180.50 + 50.00 + 140.76 + 110.00 + 75.00 = 676.26 ETB! Exactly matches screenshot!
        txList.add(
            TransactionEntity(
                id = txId++,
                accountId = 2,
                type = TransactionType.EXPENSE,
                amountMinor = 7500L,
                feeMinor = 0,
                counterpartyName = "Abebe Bajaj Transport",
                reference = "BJ771029",
                categoryId = null, // Unknown category
                categorySource = CategorySource.NONE,
                confidence = 0.35f,
                needsReview = true, // Review Item #2
                note = "Unknown merchant",
                occurredAt = now - (6 * 24 * 3600 * 1000L),
                rawSmsHash = "sample_hash_bajaj",
                sourceSender = "127",
                isSample = true
            )
        )

        // Item 7: Internal transfer (excluded from income/expenses)
        txList.add(
            TransactionEntity(
                id = txId++,
                accountId = 1,
                type = TransactionType.TRANSFER_OUT,
                amountMinor = 50000L, // 500.00 ETB
                counterpartyName = "Telebirr Wallet (Self)",
                reference = "TRF99210",
                categoryId = 21,
                isExcludedFromReports = true,
                occurredAt = now - (10 * 24 * 3600 * 1000L),
                rawSmsHash = "sample_hash_transfer",
                sourceSender = "CBE",
                isSample = true
            )
        )

        // Seed additional realistic historical transactions from previous month
        val random = Random(42L) // Fixed deterministic seed
        for (i in 1..100) {
            val daysAgo = 10 + random.nextInt(50)
            val amount = (random.nextInt(350) + 15) * 100L
            val isIncome = random.nextInt(10) == 0
            val catId = if (isIncome) 16L else (random.nextInt(10) + 1).toLong()

            txList.add(
                TransactionEntity(
                    id = txId++,
                    accountId = if (random.nextBoolean()) 1L else 2L,
                    type = if (isIncome) TransactionType.INCOME else TransactionType.EXPENSE,
                    amountMinor = amount,
                    feeMinor = 0,
                    counterpartyName = "Merchant #$i",
                    reference = "TXN_HIST_$i",
                    categoryId = catId,
                    categorySource = CategorySource.AUTO,
                    confidence = 0.9f,
                    needsReview = false,
                    occurredAt = now - (daysAgo * 24 * 3600 * 1000L) + random.nextInt(3600000),
                    rawSmsHash = "sample_hist_$i",
                    sourceSender = "CBE",
                    isSample = true
                )
            )
        }

        transactionDao.insertTransactions(txList)

        // Mark seeded in SyncState
        syncStateDao.setSyncState(
            SyncStateEntity(
                id = 1,
                lastInboxScanAt = now,
                mode = "DEMO"
            )
        )
    }
}
