package com.example.ethfinance.data.sms.parser

import com.example.ethfinance.core.util.MoneyFormatter
import com.example.ethfinance.data.local.db.entity.TransactionType
import java.util.regex.Pattern

class AwashParser : BankParser {
    override val id: String = "AWASH"

    private val amountPattern = Pattern.compile("(?:debited|credited|withdrawn|paid)\\s+(?:ETB|Birr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val balancePattern = Pattern.compile("balance\\s+is\\s+(?:ETB|Birr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val refPattern = Pattern.compile("(?:Ref|Txn):?\\s*([A-Za-z0-9]+)", Pattern.CASE_INSENSITIVE)

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.trim().lowercase()
        return s.contains("awash")
    }

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedSms? {
        val isDebit = body.contains("debit", ignoreCase = true) || body.contains("withdrawn", ignoreCase = true)
        val isCredit = body.contains("credit", ignoreCase = true) || body.contains("deposit", ignoreCase = true)
        if (!isDebit && !isCredit) return null

        val amtMatcher = amountPattern.matcher(body)
        if (!amtMatcher.find()) return null
        val amountMinor = MoneyFormatter.parseToSantim(amtMatcher.group(1)) ?: return null

        var balMinor: Long? = null
        val balMatcher = balancePattern.matcher(body)
        if (balMatcher.find()) {
            balMinor = MoneyFormatter.parseToSantim(balMatcher.group(1))
        }

        var ref: String? = null
        val refMatcher = refPattern.matcher(body)
        if (refMatcher.find()) {
            ref = refMatcher.group(1)
        }

        return ParsedSms(
            direction = if (isDebit) TransactionType.EXPENSE else TransactionType.INCOME,
            amountMinor = amountMinor,
            balanceAfterMinor = balMinor,
            reference = ref,
            accountHint = "Awash Bank",
            occurredAt = receivedAt,
            confidence = 0.90f,
            rawType = "AWASH_TRANSACTION"
        )
    }
}

class DashenParser : BankParser {
    override val id: String = "DASHEN"

    private val amountPattern = Pattern.compile("(?:debited|credited|paid)\\s+(?:ETB|Birr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val balancePattern = Pattern.compile("balance\\s+(?:is)?\\s*(?:ETB|Birr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.trim().lowercase()
        return s.contains("dashen") || s.contains("amole")
    }

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedSms? {
        val isDebit = body.contains("debit", ignoreCase = true) || body.contains("paid", ignoreCase = true)
        val isCredit = body.contains("credit", ignoreCase = true) || body.contains("received", ignoreCase = true)
        if (!isDebit && !isCredit) return null

        val amtMatcher = amountPattern.matcher(body)
        if (!amtMatcher.find()) return null
        val amountMinor = MoneyFormatter.parseToSantim(amtMatcher.group(1)) ?: return null

        var balMinor: Long? = null
        val balMatcher = balancePattern.matcher(body)
        if (balMatcher.find()) {
            balMinor = MoneyFormatter.parseToSantim(balMatcher.group(1))
        }

        return ParsedSms(
            direction = if (isDebit) TransactionType.EXPENSE else TransactionType.INCOME,
            amountMinor = amountMinor,
            balanceAfterMinor = balMinor,
            accountHint = "Dashen Bank",
            occurredAt = receivedAt,
            confidence = 0.90f,
            rawType = "DASHEN_TRANSACTION"
        )
    }
}

class BoaParser : BankParser {
    override val id: String = "BOA"

    private val amountPattern = Pattern.compile("(?:debited|credited|transferred)\\s+(?:ETB|Birr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
    private val balancePattern = Pattern.compile("balance\\s+(?:is)?\\s*(?:ETB|Birr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)

    override fun canParse(sender: String, body: String): Boolean {
        val s = sender.trim().lowercase()
        return s.contains("boa") || s.contains("abyssinia")
    }

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedSms? {
        val isDebit = body.contains("debit", ignoreCase = true) || body.contains("transferred", ignoreCase = true)
        val isCredit = body.contains("credit", ignoreCase = true)
        if (!isDebit && !isCredit) return null

        val amtMatcher = amountPattern.matcher(body)
        if (!amtMatcher.find()) return null
        val amountMinor = MoneyFormatter.parseToSantim(amtMatcher.group(1)) ?: return null

        var balMinor: Long? = null
        val balMatcher = balancePattern.matcher(body)
        if (balMatcher.find()) {
            balMinor = MoneyFormatter.parseToSantim(balMatcher.group(1))
        }

        return ParsedSms(
            direction = if (isDebit) TransactionType.EXPENSE else TransactionType.INCOME,
            amountMinor = amountMinor,
            balanceAfterMinor = balMinor,
            accountHint = "Bank of Abyssinia",
            occurredAt = receivedAt,
            confidence = 0.90f,
            rawType = "BOA_TRANSACTION"
        )
    }
}

/**
 * Generic fallback parser that detects common financial keywords:
 * debited, credited, sent, received, paid, transferred, withdrawn, purchased.
 * Assigns confidence = 0.40f, which forces review.
 */
class GenericFallbackParser : BankParser {
    override val id: String = "GENERIC_FALLBACK"

    private val moneyPattern = Pattern.compile("(?:ETB|Birr|ብር)?\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:ETB|Birr|ብር)?", Pattern.CASE_INSENSITIVE)

    override fun canParse(sender: String, body: String): Boolean = true

    override fun parse(sender: String, body: String, receivedAt: Long): ParsedSms? {
        val lower = body.lowercase()
        val isDebit = lower.contains("debit") || lower.contains("sent") || lower.contains("paid") ||
                lower.contains("transferred") || lower.contains("withdrawn") || lower.contains("purchased") ||
                lower.contains("ወጪ")
        val isCredit = lower.contains("credit") || lower.contains("received") || lower.contains("deposited") ||
                lower.contains("ገቢ")

        if (!isDebit && !isCredit) return null

        val matcher = moneyPattern.matcher(body)
        if (!matcher.find()) return null

        val amountMinor = MoneyFormatter.parseToSantim(matcher.group(1)) ?: return null

        return ParsedSms(
            direction = if (isDebit) TransactionType.EXPENSE else TransactionType.INCOME,
            amountMinor = amountMinor,
            occurredAt = receivedAt,
            confidence = 0.40f, // Low confidence -> forces review
            rawType = "GENERIC_SMS"
        )
    }
}
