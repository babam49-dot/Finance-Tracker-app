package com.example.ethfinance.data.sms.parser

import com.example.ethfinance.core.util.MoneyFormatter
import com.example.ethfinance.data.local.db.entity.TransactionType
import java.util.regex.Pattern

class CbeParser : BankParser {

    override val id: String = "CBE"

    private val accountPattern = Pattern.compile("(?:Account|acc(?:ount)?\\s+no|ሂሳብ\\s+ቁጥር)\\s*[:.]?\\s*([\\w*]+)", Pattern.CASE_INSENSITIVE)
    
    // Debit patterns (English & Amharic)
    private val debitPattern = Pattern.compile("(?:debited\\s+with|debited|transferred|withdrawn|paid|ወጪ\\s+ተደርጓል)\\s+(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val altDebitPattern = Pattern.compile("(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:debited\\s+from|ወጪ\\s+ተደርጓል)", Pattern.CASE_INSENSITIVE)

    // Credit patterns (English & Amharic)
    private val creditPattern = Pattern.compile("(?:credited\\s+with|credited|deposited|received|ገቢ\\s+ተደርጓል|ገቢ\\s+ሆኗል)\\s+(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val altCreditPattern = Pattern.compile("(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:credited\\s+to|ገቢ\\s+ተደርጓል)", Pattern.CASE_INSENSITIVE)

    // Balance pattern
    private val balancePattern = Pattern.compile("(?:Current\\s+Balance|Balance|ቀሪ\\s+ሂሳብ(?:ዎ)?)(?:\\s+is)?\\s+(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val receiptPattern = Pattern.compile("(?:Receipt|Txn\\s*Id|Ref(?:erence)?|የግብይት\\s+ቁጥር):?\\s*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE)
    private val counterpartyPattern = Pattern.compile("(?:from|to|at|ወደ|ከ)\\s+([A-Za-z0-9\\s.&'-]+?)(?:\\.|,|on|\\n|Receipt|ቀሪ|$)", Pattern.CASE_INSENSITIVE)

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.trim().lowercase()
        return s == "cbe" || s == "cbebirr" || s == "cbe-birr" || s.contains("cbe")
    }

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedSms? {
        // Ignore OTP / security messages
        if (body.contains("OTP", ignoreCase = true) || body.contains("verification code", ignoreCase = true)) {
            return null
        }

        var isDebit = false
        var isCredit = false
        var amountMinor: Long? = null

        val debM = debitPattern.matcher(body)
        val altDebM = altDebitPattern.matcher(body)
        val credM = creditPattern.matcher(body)
        val altCredM = altCreditPattern.matcher(body)

        if (debM.find()) {
            isDebit = true
            amountMinor = MoneyFormatter.parseToSantim(debM.group(1))
        } else if (altDebM.find()) {
            isDebit = true
            amountMinor = MoneyFormatter.parseToSantim(altDebM.group(1))
        } else if (credM.find()) {
            isCredit = true
            amountMinor = MoneyFormatter.parseToSantim(credM.group(1))
        } else if (altCredM.find()) {
            isCredit = true
            amountMinor = MoneyFormatter.parseToSantim(altCredM.group(1))
        }

        var balanceMinor: Long? = null
        val balMatcher = balancePattern.matcher(body)
        if (balMatcher.find()) {
            balanceMinor = MoneyFormatter.parseToSantim(balMatcher.group(1))
        }

        if (amountMinor == null) {
            if (balanceMinor != null) {
                amountMinor = 0L
            } else {
                return null
            }
        }

        val direction = if (isDebit) TransactionType.EXPENSE else TransactionType.INCOME

        var accountHint: String? = null
        val accMatcher = accountPattern.matcher(body)
        if (accMatcher.find()) {
            accountHint = accMatcher.group(1)
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
