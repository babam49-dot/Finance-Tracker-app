package com.example.ethfinance.data.sms.parser

import com.example.ethfinance.data.local.db.entity.TransactionType

data class ParsedSms(
    val direction: TransactionType,
    val amountMinor: Long,
    val feeMinor: Long = 0L,
    val balanceAfterMinor: Long? = null,
    val counterparty: String? = null,
    val reference: String? = null,
    val accountHint: String? = null,
    val occurredAt: Long,
    val confidence: Float,
    val rawType: String
)

interface BankParser {
    val id: String
    fun canParse(sender: String, body: String): Boolean
    fun parse(sender: String, body: String, receivedAt: Long): ParsedSms?
}
