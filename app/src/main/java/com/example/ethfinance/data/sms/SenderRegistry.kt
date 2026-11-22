package com.example.ethfinance.data.sms

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SenderRegistry @Inject constructor() {

    private val defaultSenders = setOf(
        "CBE", "CBEBIRR", "CBE-BIRR", "CBE BIRR", "808", "809",
        "127", "TELEBIRR",
        "AWASHBANK", "AWASH",
        "DASHENBANK", "DASHEN",
        "BOA", "ABYSSINIABANK", "BANKOFABYSSINIA",
        "ZEMEN", "WEGAGEN", "NIB", "HIBRET", "COOPBANK", "ENAT", "AHADU", "AMHARA BANK", "M-PESA"
    )

    fun isTrackedSender(sender: String, body: String? = null): Boolean {
        val s = sender.trim().uppercase()
        if (defaultSenders.any { s.contains(it) || it.contains(s) }) return true
        if (body != null) {
            val b = body.lowercase()
            if (b.contains("cbe") || b.contains("telebirr") || b.contains("ቴሌብር") || b.contains("commercial bank") || b.contains("የኢትዮጵያ ንግድ ባንክ")) {
                return true
            }
        }
        return false
    }

    fun getAllTrackedSenders(): Set<String> = defaultSenders
}
