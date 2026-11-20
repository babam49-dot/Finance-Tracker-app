package com.example.ethfinance.data.sms.parser

import com.example.ethfinance.core.util.MoneyFormatter
import com.example.ethfinance.data.local.db.entity.TransactionType
import java.util.regex.Pattern

class TelebirrParser : BankParser {

    override val id: String = "TELEBIRR"

    private val transferredPattern = Pattern.compile("transferred\\s+(?:ETB|Birr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s+to\\s+([A-Za-z0-9\\s()*-]+?)(?:\\s+on|\\.|\\n|$)", Pattern.CASE_INSENSITIVE)
    private val receivedPattern = Pattern.compile("received\\s+(?:ETB|Birr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s+from\\s+([A-Za-z0-9\\s()*-]+?)(?:\\.|\\n|$)", Pattern.CASE_INSENSITIVE)
    private val airtimePattern = Pattern.compile("bought\\s+airtime\\s+of\\s+(?:ETB|Birr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val paidPattern = Pattern.compile("paid\\s+(?:ETB|Birr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s+to\\s+([A-Za-z0-9\\s()*-]+?)(?:\\.|\\n|$)", Pattern.CASE_INSENSITIVE)
    private val balancePattern = Pattern.compile("(?:balance\\s+is|telebirr\\s+account\\s+balance\\s+is|Balance)\\s+(?:ETB|Birr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val txnNumberPattern = Pattern.compile("Transaction\\s+(?:number|id):?\\s*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE)
    private val serviceChargePattern = Pattern.compile("Service\\s+charge\\s+(?:ETB|Birr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.trim().lowercase()
        return s == "127" || s == "telebirr" || s.contains("telebirr")
    }

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedSms? {
        if (body.contains("OTP", ignoreCase = true) || body.contains("verification code", ignoreCase = true)) {
            return null
        }

        var direction = TransactionType.EXPENSE
        var amountMinor: Long? = null
        var counterparty: String? = null
        var rawType = "TELEBIRR_PAYMENT"

        val trMatcher = transferredPattern.matcher(body)
        val rcMatcher = receivedPattern.matcher(body)
        val atMatcher = airtimePattern.matcher(body)
        val pdMatcher = paidPattern.matcher(body)

        when {
            trMatcher.find() -> {
                direction = TransactionType.EXPENSE
                amountMinor = MoneyFormatter.parseToSantim(trMatcher.group(1))
                counterparty = trMatcher.group(2)?.trim()
                rawType = "TELEBIRR_TRANSFER_OUT"
            }
            rcMatcher.find() -> {
                direction = TransactionType.INCOME
                amountMinor = MoneyFormatter.parseToSantim(rcMatcher.group(1))
                counterparty = rcMatcher.group(2)?.trim()
                rawType = "TELEBIRR_TRANSFER_IN"
            }
            atMatcher.find() -> {
                direction = TransactionType.EXPENSE
                amountMinor = MoneyFormatter.parseToSantim(atMatcher.group(1))
                counterparty = "Ethio telecom Airtime"
                rawType = "TELEBIRR_AIRTIME"
            }
            pdMatcher.find() -> {
                direction = TransactionType.EXPENSE
                amountMinor = MoneyFormatter.parseToSantim(pdMatcher.group(1))
                counterparty = pdMatcher.group(2)?.trim()
                rawType = "TELEBIRR_BILL"
            }
        }

        if (amountMinor == null) return null

        var feeMinor = 0L
        val feeMatcher = serviceChargePattern.matcher(body)
        if (feeMatcher.find()) {
            feeMinor = MoneyFormatter.parseToSantim(feeMatcher.group(1)) ?: 0L
        }

        var balanceMinor: Long? = null
        val balMatcher = balancePattern.matcher(body)
        if (balMatcher.find()) {
            balanceMinor = MoneyFormatter.parseToSantim(balMatcher.group(1))
        }

        var reference: String? = null
        val txnMatcher = txnNumberPattern.matcher(body)
        if (txnMatcher.find()) {
            reference = txnMatcher.group(1)
        }

        return ParsedSms(
            direction = direction,
            amountMinor = amountMinor,
            feeMinor = feeMinor,
            balanceAfterMinor = balanceMinor,
            counterparty = counterparty,
            reference = reference,
            accountHint = "Telebirr",
            occurredAt = receivedAt,
            confidence = 0.95f,
            rawType = rawType
        )
    }
}
