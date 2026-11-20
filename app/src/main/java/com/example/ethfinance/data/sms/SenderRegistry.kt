package com.example.ethfinance.data.sms

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SenderRegistry @Inject constructor() {

    private val defaultSenders = setOf(
        "CBE", "CBEBIRR", "CBE-BIRR", "CBE BIRR",
        "127", "TELEBIRR",
        "AWASHBANK", "AWASH",
        "DASHENBANK", "DASHEN",
        "BOA", "ABYSSINIABANK", "BANKOFABYSSINIA",
        "ZEMEN", "WEGAGEN", "NIB", "HIBRET", "COOPBANK", "ENAT", "AHADU", "AMHARA BANK", "M-PESA"
    )

    fun isTrackedSender(sender: String): Boolean {
        val s = sender.trim().uppercase()
        return defaultSenders.any { s.contains(it) || it.contains(s) }
    }

    fun getAllTrackedSenders(): Set<String> = defaultSenders
}
