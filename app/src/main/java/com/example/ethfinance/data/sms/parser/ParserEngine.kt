package com.example.ethfinance.data.sms.parser

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParserEngine @Inject constructor() {

    private val parsers: List<BankParser> = listOf(
        CbeParser(),
        TelebirrParser(),
        AwashParser(),
        DashenParser(),
        BoaParser(),
        GenericFallbackParser()
    )

    fun parse(sender: String, body: String, receivedAt: Long): ParsedSms? {
        // Try specific bank parsers first
        for (parser in parsers) {
            if (parser !is GenericFallbackParser && parser.canParse(sender, body)) {
                try {
                    val result = parser.parse(sender, body, receivedAt)
                    if (result != null) return result
                } catch (e: Exception) {
                    // Fail-safe: do not crash on unexpected SMS structure
                }
            }
        }

        // Try Generic fallback parser
        val fallback = parsers.last() as GenericFallbackParser
        return try {
            fallback.parse(sender, body, receivedAt)
        } catch (e: Exception) {
            null
        }
    }
}
