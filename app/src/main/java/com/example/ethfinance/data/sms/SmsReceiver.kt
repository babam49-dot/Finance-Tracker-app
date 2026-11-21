package com.example.ethfinance.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.ethfinance.core.common.DispatcherProvider
import com.example.ethfinance.core.util.MoneyFormatter
import com.example.ethfinance.core.util.NotificationHelper
import com.example.ethfinance.data.local.db.dao.AccountDao
import com.example.ethfinance.data.local.db.dao.TransactionDao
import com.example.ethfinance.data.local.db.entity.AccountEntity
import com.example.ethfinance.data.local.db.entity.AccountType
import com.example.ethfinance.data.local.db.entity.TransactionEntity
import com.example.ethfinance.data.local.db.entity.TransactionType
import com.example.ethfinance.data.sms.parser.ParserEngine
import com.example.ethfinance.domain.categorize.Categorizer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var senderRegistry: SenderRegistry

    @Inject
    lateinit var parserEngine: ParserEngine

    @Inject
    lateinit var categorizer: Categorizer

    @Inject
    lateinit var accountDao: AccountDao

    @Inject
    lateinit var transactionDao: TransactionDao

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    private val scope = CoroutineScope(SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val sender = messages[0].displayOriginatingAddress ?: return
        if (!senderRegistry.isTrackedSender(sender)) return

        val fullBody = messages.joinToString("") { it.displayMessageBody ?: "" }
        val timestamp = messages[0].timestampMillis

        val pendingResult = goAsync()

        scope.launch(dispatcherProvider.io) {
            try {
                processIncomingSms(sender, fullBody, timestamp)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun processIncomingSms(sender: String, body: String, timestamp: Long) {
        val normalizedBody = body.trim().replace("\\s+".toRegex(), " ")
        val minuteTimestamp = timestamp / 60000L
        val hash = sha256("$sender:$normalizedBody:$minuteTimestamp")

        if (transactionDao.getTransactionByHash(hash) != null) {
            return
        }

        val parsed = parserEngine.parse(sender, body, timestamp) ?: return

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
            val name = "${accountType.name.replace("BANK_", "")} Account ${parsed.accountHint ?: ""}".trim()
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

        val insertedId = transactionDao.insertTransaction(transaction)

        // Post notification
        val amountFormatted = MoneyFormatter.formatSantim(parsed.amountMinor)
        val cp = txCounterparty ?: account?.displayName ?: "Bank"
        val notifTitle = if (isInternalTransfer) "Transfer: $amountFormatted" else "New Transaction: $amountFormatted"
        notificationHelper.notifyNewTransaction(
            title = notifTitle,
            message = "$cp • ${sender.uppercase()}",
            txId = insertedId
        )
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
