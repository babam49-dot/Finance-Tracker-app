package com.example.ethfinance.data.sms.parser

import com.example.ethfinance.core.util.MoneyFormatter
import com.example.ethfinance.data.local.db.entity.TransactionType
import java.util.regex.Pattern

class TelebirrParser : BankParser {

    override val id: String = "TELEBIRR"

    // English patterns
    private val transferredPattern = Pattern.compile("(?:transferred|transfer)\\s+(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)(?:\\s+to\\s+([A-Za-z0-9\\s()*-]+?))?(?:\\s+on|\\.|\\n|$)", Pattern.CASE_INSENSITIVE)
    private val receivedPattern = Pattern.compile("(?:received|credited)\\s+(?:with\\s+)?(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)(?:\\s+from\\s+([A-Za-z0-9\\s()*-]+?))?(?:\\.|\\n|$)", Pattern.CASE_INSENSITIVE)
    private val airtimePattern = Pattern.compile("(?:bought|purchased)\\s+airtime\\s+(?:of\\s+)?(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val paidPattern = Pattern.compile("(?:paid|debited)\\s+(?:with\\s+)?(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)(?:\\s+to\\s+([A-Za-z0-9\\s()*-]+?))?(?:\\.|\\n|$)", Pattern.CASE_INSENSITIVE)
    private val depositedPattern = Pattern.compile("(?:deposited)\\s+(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)

    // Amharic patterns
    private val amhTransferredPattern = Pattern.compile("(?:ብር|ETB)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ብር|ETB)?\\s*ወደ\\s+([^\\n.]+?)\\s*(?:አስተላልፈዋል|ተላልፏል)", Pattern.CASE_INSENSITIVE)
    private val amhReceivedPattern = Pattern.compile("(?:ከ\\s*([^\\n.]+?)\\s*)?(?:ብር|ETB)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ብር|ETB)?\\s*(?:ተቀብለዋል|ገቢ\\s+ሆኗል)", Pattern.CASE_INSENSITIVE)
    private val amhAirtimePattern = Pattern.compile("(?:የብር|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*የአየር\\s*ሰዓት\\s*ገዝተዋል", Pattern.CASE_INSENSITIVE)
    private val amhPaidPattern = Pattern.compile("(?:ብር|ETB)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ብር|ETB)?\\s*ለ\\s*([^\\n.]+?)\\s*(?:ከፍለዋል|ተከፍሏል)", Pattern.CASE_INSENSITIVE)

    // Balance, Ref & Fee patterns
    private val balancePattern = Pattern.compile("(?:(?:telebirr\\s+account\\s+)?balance\\s+is|your\\s+balance\\s+is|Current\\s+Balance|Balance|ቀሪ\\s+ሂሳብ(?:ዎ)?|የቴሌብር\\s+ሂሳብዎ)(?:\\s+is)?\\s+(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val txnNumberPattern = Pattern.compile("(?:Transaction\\s+(?:number|id|no)|Txn\\s*Id|የግብይት\\s+ቁጥር):?\\s*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE)
    private val serviceChargePattern = Pattern.compile("(?:Service\\s+charge|የአገልግሎት\\s+ክፍያ)\\s+(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.trim().lowercase()
        return s == "127" || s == "telebirr" || s.contains("telebirr") || s.contains("127")
    }

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedSms? {
        if (body.contains("OTP", ignoreCase = true) || body.contains("verification code", ignoreCase = true)) {
            return null
        }

        var direction = TransactionType.EXPENSE
        var amountMinor: Long? = null
        var counterparty: String? = null
        var rawType = "TELEBIRR_TRANSACTION"

        val trMatcher = transferredPattern.matcher(body)
        val rcMatcher = receivedPattern.matcher(body)
        val atMatcher = airtimePattern.matcher(body)
        val pdMatcher = paidPattern.matcher(body)
        val dpMatcher = depositedPattern.matcher(body)

        val amhTrMatcher = amhTransferredPattern.matcher(body)
        val amhRcMatcher = amhReceivedPattern.matcher(body)
        val amhAtMatcher = amhAirtimePattern.matcher(body)
        val amhPdMatcher = amhPaidPattern.matcher(body)

        when {
            trMatcher.find() -> {
                direction = TransactionType.EXPENSE
                amountMinor = MoneyFormatter.parseToSantim(trMatcher.group(1))
                counterparty = trMatcher.group(2)?.trim()
                rawType = "TELEBIRR_TRANSFER_OUT"
            }
            amhTrMatcher.find() -> {
                direction = TransactionType.EXPENSE
                amountMinor = MoneyFormatter.parseToSantim(amhTrMatcher.group(1))
                counterparty = amhTrMatcher.group(2)?.trim()
                rawType = "TELEBIRR_TRANSFER_OUT"
            }
            rcMatcher.find() -> {
                direction = TransactionType.INCOME
                amountMinor = MoneyFormatter.parseToSantim(rcMatcher.group(1))
                counterparty = rcMatcher.group(2)?.trim()
                rawType = "TELEBIRR_TRANSFER_IN"
            }
            amhRcMatcher.find() -> {
                direction = TransactionType.INCOME
                amountMinor = MoneyFormatter.parseToSantim(amhRcMatcher.group(2) ?: amhRcMatcher.group(1))
                counterparty = amhRcMatcher.group(1)?.trim()
                rawType = "TELEBIRR_TRANSFER_IN"
            }
            atMatcher.find() -> {
                direction = TransactionType.EXPENSE
                amountMinor = MoneyFormatter.parseToSantim(atMatcher.group(1))
                counterparty = "Ethio telecom Airtime"
                rawType = "TELEBIRR_AIRTIME"
            }
            amhAtMatcher.find() -> {
                direction = TransactionType.EXPENSE
                amountMinor = MoneyFormatter.parseToSantim(amhAtMatcher.group(1))
                counterparty = "Ethio telecom Airtime"
                rawType = "TELEBIRR_AIRTIME"
            }
            pdMatcher.find() -> {
                direction = TransactionType.EXPENSE
                amountMinor = MoneyFormatter.parseToSantim(pdMatcher.group(1))
                counterparty = pdMatcher.group(2)?.trim()
                rawType = "TELEBIRR_BILL"
            }
            amhPdMatcher.find() -> {
                direction = TransactionType.EXPENSE
                amountMinor = MoneyFormatter.parseToSantim(amhPdMatcher.group(1))
                counterparty = amhPdMatcher.group(2)?.trim()
                rawType = "TELEBIRR_BILL"
            }
            dpMatcher.find() -> {
                direction = TransactionType.INCOME
                amountMinor = MoneyFormatter.parseToSantim(dpMatcher.group(1))
                counterparty = "Telebirr Deposit"
                rawType = "TELEBIRR_DEPOSIT"
            }
        }

        var balanceMinor: Long? = null
        val balMatcher = balancePattern.matcher(body)
        if (balMatcher.find()) {
            balanceMinor = MoneyFormatter.parseToSantim(balMatcher.group(1))
        }

        // If amount was not explicitly captured by a pattern, but balance exists, check generic money
        if (amountMinor == null) {
            if (balanceMinor != null) {
                // If it's a balance inquiry or unclassified message with balance
                amountMinor = 0L
            } else {
                return null
            }
        }

        var feeMinor = 0L
        val feeMatcher = serviceChargePattern.matcher(body)
        if (feeMatcher.find()) {
            feeMinor = MoneyFormatter.parseToSantim(feeMatcher.group(1)) ?: 0L
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
