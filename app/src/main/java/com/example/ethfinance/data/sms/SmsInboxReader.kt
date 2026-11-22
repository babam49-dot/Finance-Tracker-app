package com.example.ethfinance.data.sms

import android.content.Context
import android.net.Uri
import com.example.ethfinance.core.util.NotificationHelper
import com.example.ethfinance.data.local.db.dao.AccountDao
import com.example.ethfinance.data.local.db.dao.TransactionDao
import com.example.ethfinance.data.local.db.entity.AccountEntity
import com.example.ethfinance.data.local.db.entity.AccountType
import com.example.ethfinance.data.local.db.entity.TransactionEntity
import com.example.ethfinance.data.local.db.entity.TransactionType
import com.example.ethfinance.data.sms.parser.ParserEngine
import com.example.ethfinance.domain.categorize.Categorizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsInboxReader @Inject constructor(
    @ApplicationContext private val context: Context,
    private val senderRegistry: SenderRegistry,
    private val parserEngine: ParserEngine,
    private val categorizer: Categorizer,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val notificationHelper: NotificationHelper
) {

    suspend fun importInbox(): Int = withContext(Dispatchers.IO) {
        // Clean up any remaining demo sample accounts so only real accounts exist
        accountDao.deleteSampleAccounts()
        transactionDao.deleteSampleTransactions()

        var importedCount = 0
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("_id", "address", "body", "date")

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                null,
                null,
                "date DESC"
            )

            cursor?.use {
                val addressIdx = it.getColumnIndex("address")
                val bodyIdx = it.getColumnIndex("body")
                val dateIdx = it.getColumnIndex("date")

                while (it.moveToNext()) {
                    val sender = it.getString(addressIdx) ?: continue
                    val body = it.getString(bodyIdx) ?: continue
                    if (!senderRegistry.isTrackedSender(sender, body)) continue
                    val timestamp = it.getLong(dateIdx)

                    val processed = processSingleSms(sender, body, timestamp)
                    if (processed) {
                        importedCount++
                    }
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Post sync notification
        notificationHelper.notifySyncCompleted(importedCount)

        importedCount
    }

    private suspend fun processSingleSms(sender: String, body: String, timestamp: Long): Boolean {
        val normalizedBody = body.trim().replace("\\s+".toRegex(), " ")
        val minuteTimestamp = timestamp / 60000L
        val hash = sha256("$sender:$normalizedBody:$minuteTimestamp")

        if (transactionDao.getTransactionByHash(hash) != null) {
            return false
        }

        val parsed = parserEngine.parse(sender, body, timestamp) ?: return false

        val accountType = when {
            sender.contains("cbe", ignoreCase = true) -> AccountType.BANK_CBE
            sender.contains("127") || sender.contains("telebirr", ignoreCase = true) -> AccountType.TELEBIRR
            else -> AccountType.BANK_OTHER
        }

        var account = if (!parsed.accountHint.isNullOrBlank()) {
            accountDao.getAccountByTypeAndMask(accountType, parsed.accountHint)
        } else {
            accountDao.getFirstRealAccountByType(accountType)
        }

        if (account == null) {
            val name = when (accountType) {
                AccountType.BANK_CBE -> "CBE Account ${parsed.accountHint ?: ""}".trim()
                AccountType.TELEBIRR -> "Telebirr Wallet"
                else -> "${accountType.name.replace("BANK_", "")} Account"
            }
            val newAccountId = accountDao.insertAccount(
                AccountEntity(
                    type = accountType,
                    displayName = name,
                    institutionName = name,
                    maskedNumber = parsed.accountHint ?: "****",
                    senderIds = listOf(sender),
                    currentBalanceMinor = parsed.balanceAfterMinor,
                    balanceUpdatedAt = timestamp,
                    isSample = false
                )
            )
            account = accountDao.getAccountById(newAccountId)
        } else if (parsed.balanceAfterMinor != null) {
            val lastUpdated = account.balanceUpdatedAt ?: 0L
            if (timestamp >= lastUpdated) {
                accountDao.updateBalance(account.id, parsed.balanceAfterMinor, timestamp)
            }
        } else if (account.currentBalanceMinor != null) {
            val lastUpdated = account.balanceUpdatedAt ?: 0L
            if (timestamp >= lastUpdated) {
                val newBal = if (parsed.direction == TransactionType.EXPENSE) {
                    account.currentBalanceMinor - parsed.amountMinor
                } else {
                    account.currentBalanceMinor + parsed.amountMinor
                }
                accountDao.updateBalance(account.id, newBal, timestamp)
            }
        }

        val accountId = account?.id ?: 1L

        val fullText = "${parsed.counterparty ?: ""} $body".lowercase()
        val isTelebirrSender = sender.contains("127") || sender.contains("telebirr", ignoreCase = true)
        val isCbeSender = sender.contains("cbe", ignoreCase = true)

        val isTransferToBank = isTelebirrSender && (
            fullText.contains("bank") || fullText.contains("cbe") || fullText.contains("commercial") ||
            fullText.contains("ንግድ ባንክ") || fullText.contains("ባንክ") || fullText.contains("cbebirr") ||
            fullText.contains("dashen") || fullText.contains("awash") || fullText.contains("boa")
        )

        val isTransferToTelebirr = isCbeSender && (
            fullText.contains("telebirr") || fullText.contains("ቴሌብር") || fullText.contains("127") || fullText.contains("cbebirr")
        )

        val isInternalTransfer = isTransferToBank || isTransferToTelebirr

        // Update destination account balance if available
        if (isTransferToBank) {
            val bankAccount = accountDao.getFirstRealAccountByType(AccountType.BANK_CBE)
            if (bankAccount != null && bankAccount.currentBalanceMinor != null && (bankAccount.balanceUpdatedAt ?: 0L) < timestamp) {
                accountDao.updateBalance(bankAccount.id, bankAccount.currentBalanceMinor + parsed.amountMinor, timestamp)
            }
        } else if (isTransferToTelebirr) {
            val telebirrAccount = accountDao.getFirstRealAccountByType(AccountType.TELEBIRR)
            if (telebirrAccount != null && telebirrAccount.currentBalanceMinor != null && (telebirrAccount.balanceUpdatedAt ?: 0L) < timestamp) {
                accountDao.updateBalance(telebirrAccount.id, telebirrAccount.currentBalanceMinor + parsed.amountMinor, timestamp)
            }
        }

        val txType = when {
            isInternalTransfer && parsed.direction == TransactionType.EXPENSE -> TransactionType.TRANSFER_OUT
            isInternalTransfer && parsed.direction == TransactionType.INCOME -> TransactionType.TRANSFER_IN
            else -> parsed.direction
        }

        val txCounterparty = when {
            isTransferToBank -> "Transfer: Telebirr → Bank"
            isTransferToTelebirr -> "Transfer: Bank → Telebirr"
            else -> parsed.counterparty
        }

        val catResult = categorizer.categorize(
            counterparty = txCounterparty,
            smsBody = body,
            amountMinor = parsed.amountMinor
        )

        val transaction = TransactionEntity(
            accountId = accountId,
            type = txType,
            amountMinor = parsed.amountMinor,
            feeMinor = parsed.feeMinor,
            balanceAfterMinor = parsed.balanceAfterMinor,
            counterpartyName = txCounterparty,
            reference = parsed.reference,
            categoryId = catResult.categoryId,
            categorySource = catResult.source,
            confidence = catResult.confidence,
            needsReview = if (isInternalTransfer) false else (catResult.needsReview || parsed.confidence < 0.5f),
            occurredAt = parsed.occurredAt,
            rawSmsHash = hash,
            rawSmsBody = null,
            sourceSender = sender,
            isSample = false,
            isExcludedFromReports = isInternalTransfer
        )

        transactionDao.insertTransaction(transaction)
        return true
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
