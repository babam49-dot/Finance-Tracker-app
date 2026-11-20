package com.example.ethfinance.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.ethfinance.core.common.DispatcherProvider
import com.example.ethfinance.data.local.db.dao.AccountDao
import com.example.ethfinance.data.local.db.dao.TransactionDao
import com.example.ethfinance.data.local.db.entity.AccountEntity
import com.example.ethfinance.data.local.db.entity.AccountType
import com.example.ethfinance.data.local.db.entity.TransactionEntity
import com.example.ethfinance.data.sms.parser.ParserEngine
import com.example.ethfinance.domain.categorize.Categorizer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

interface MessageSource {
    suspend fun startListening(onMessageReceived: (source: String, body: String, timestamp: Long) -> Unit)
}

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
        // 1. Dedupe check: SHA-256 of sender + normalized body + timestamp rounded to minute
        val normalizedBody = body.trim().replace("\\s+".toRegex(), " ")
        val minuteTimestamp = timestamp / 60000L
        val hash = sha256("$sender:$normalizedBody:$minuteTimestamp")

        if (transactionDao.getTransactionByHash(hash) != null) {
            return // Already processed
        }

        // 2. Parse SMS
        val parsed = parserEngine.parse(sender, body, timestamp) ?: return

        // 3. Resolve Account
        val accountType = when {
            sender.contains("cbe", ignoreCase = true) -> AccountType.BANK_CBE
            sender.contains("127") || sender.contains("telebirr", ignoreCase = true) -> AccountType.TELEBIRR
            else -> AccountType.BANK_OTHER
        }

        var account = if (!parsed.accountHint.isNullOrBlank()) {
            accountDao.getAccountByTypeAndMask(accountType, parsed.accountHint)
        } else {
            accountDao.getFirstAccountByType(accountType)
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
            // Update balance if timestamp is newer
            val lastUpdated = account.balanceUpdatedAt ?: 0L
            if (timestamp >= lastUpdated) {
                accountDao.updateBalance(account.id, parsed.balanceAfterMinor, timestamp)
            }
        }

        val accountId = account?.id ?: 1L

        // 4. Categorize
        val catResult = categorizer.categorize(
            counterparty = parsed.counterparty,
            smsBody = body,
            amountMinor = parsed.amountMinor
        )

        // 5. Insert Transaction
        val transaction = TransactionEntity(
            accountId = accountId,
            type = parsed.direction,
            amountMinor = parsed.amountMinor,
            feeMinor = parsed.feeMinor,
            balanceAfterMinor = parsed.balanceAfterMinor,
            counterpartyName = parsed.counterparty,
            reference = parsed.reference,
            categoryId = catResult.categoryId,
            categorySource = catResult.source,
            confidence = catResult.confidence,
            needsReview = catResult.needsReview || parsed.confidence < 0.5f,
            occurredAt = parsed.occurredAt,
            rawSmsHash = hash,
            rawSmsBody = null, // Raw text encrypted/stored only if setting enabled
            sourceSender = sender,
            isSample = false
        )

        transactionDao.insertTransaction(transaction)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
