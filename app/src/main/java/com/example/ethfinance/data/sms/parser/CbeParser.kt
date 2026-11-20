package com.example.ethfinance.data.sms.parser

import com.example.ethfinance.core.util.MoneyFormatter
import com.example.ethfinance.data.local.db.entity.TransactionType
import java.util.regex.Pattern

class CbeParser : BankParser {

    override val id: String = "CBE"

    private val accountPattern = Pattern.compile("Account\\s+([\\w*]+)|ሂሳብ\\s+ቁጥር\\s+([\\w*]+)", Pattern.CASE_INSENSITIVE)
    private val debitPattern = Pattern.compile("(debited\\s+with|ወጪ\\s+ተደርጓል)\\s+(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val creditPattern = Pattern.compile("(credited\\s+with|ገቢ\\s+ተደርጓል)\\s+(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val balancePattern = Pattern.compile("(?:Current\\s+Balance|Balance|ቀሪ\\s+ሂሳብ)(?:\\s+is)?\\s+(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val receiptPattern = Pattern.compile("(?:Receipt|Txn\\s*Id|Ref(?:erence)?):?\\s*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE)
    private val counterpartyPattern = Pattern.compile("(?:from|to|at)\\s+([A-Za-z0-9\\s.&'-]+?)(?:\\.|,|on|\\n|Receipt|$)", Pattern.CASE_INSENSITIVE)

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.trim().lowercase()
        return s == "cbe" || s == "cbebirr" || s == "cbe-birr" || s.contains("cbe")
    }

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedSms? {
        // Ignore OTP / security messages
        if (body.contains("OTP", ignoreCase = true) || body.contains("verification code", ignoreCase = true)) {
            return null
        }

        val isDebit = debitPattern.matcher(body).find()
        val isCredit = creditPattern.matcher(body).find()

        if (!isDebit && !isCredit) return null

        val direction = if (isDebit) TransactionType.EXPENSE else TransactionType.INCOME
        val matcher = if (isDebit) debitPattern.matcher(body) else creditPattern.matcher(body)
        matcher.find()

        val amountStr = matcher.group(2)
        val amountMinor = MoneyFormatter.parseToSantim(amountStr) ?: return null

        var balanceMinor: Long? = null
        val balMatcher = balancePattern.matcher(body)
        if (balMatcher.find()) {
            balanceMinor = MoneyFormatter.parseToSantim(balMatcher.group(1))
        }

        var accountHint: String? = null
        val accMatcher = accountPattern.matcher(body)
        if (accMatcher.find()) {
            accountHint = accMatcher.group(1) ?: accMatcher.group(2)
        }

        var reference: String? = null
        val recMatcher = receiptPattern.matcher(body)
        if (recMatcher.find()) {
            reference = recMatcher.group(1)
        }

        var counterparty: String? = null
        val cpMatcher = counterpartyPattern.matcher(body)
        if (cpMatcher.find()) {
            val cp = cpMatcher.group(1)?.trim()
            if (!cp.isNullOrBlank() && !cp.equals("Your", ignoreCase = true) && !cp.equals("Current", ignoreCase = true)) {
                counterparty = cp
            }
        }

        return ParsedSms(
            direction = direction,
            amountMinor = amountMinor,
            feeMinor = 0L,
            balanceAfterMinor = balanceMinor,
            counterparty = counterparty,
            reference = reference,
            accountHint = accountHint,
            occurredAt = receivedAt,
            confidence = 0.95f,
            rawType = if (isDebit) "CBE_DEBIT" else "CBE_CREDIT"
        )
    }
}
