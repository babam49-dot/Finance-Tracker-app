package com.example.ethfinance.domain.categorize

import com.example.ethfinance.data.local.db.dao.UserRuleDao
import com.example.ethfinance.data.local.db.entity.CategorySource
import com.example.ethfinance.data.local.db.entity.RuleMatchType
import javax.inject.Inject
import javax.inject.Singleton

data class CategorizationResult(
    val categoryId: Long?,
    val source: CategorySource,
    val confidence: Float,
    val needsReview: Boolean
)

@Singleton
class Categorizer @Inject constructor(
    private val userRuleDao: UserRuleDao
) {

    // Keywords dictionary for Ethiopian transactions
    private val systemKeywords = mapOf(
        // Transport: ride, feres, zayride, taxi, bajaj, anbessa, sheger bus, fuel, total, noc, oil, ethiopian airlines
        1L to listOf("ride", "feres", "zayride", "taxi", "bajaj", "anbessa", "sheger", "fuel", "total", "noc", "parking", "rail", "flight", "airlines", "transport", "ነዳጅ", "ታክሲ", "ባጃጅ"),
        // Food & Dining: restaurant, cafe, coffee, lunch, injera, burger, pizza, beu, zmall, eshi, bonga
        2L to listOf("restaurant", "cafe", "coffee", "lunch", "dinner", "breakfast", "injera", "burger", "pizza", "yum", "beu", "zmall", "eshi", "bonga", "bakery", "ምሳ", "እራት", "ቁርስ", "ቡና"),
        // Groceries: supermarket, shoa, safeway, mini market, merkato, butcher, fruit, vegetable
        3L to listOf("supermarket", "shoa", "safeway", "market", "merkato", "butcher", "grocery", "ሱፐርማርኬት", "ሸቀጣሸቀጥ"),
        // Airtime & Data: airtime, data package, ethio telecom, safaricom, 127 top-up
        4L to listOf("airtime", "data package", "ethio telecom", "safaricom", "*999#", "top-up", "recharge", "ካርድ", "ፓኬጅ"),
        // Bills & Utilities: eeu, electric, water, aawsa, dstv, internet, telecom bill
        5L to listOf("eeu", "electric", "water", "aawsa", "dstv", "utility", "bill", "መብራት", "ውሃ"),
        // Rent
        6L to listOf("rent", "house rent", "ክራይ", "ቤት ክራይ"),
        // Health
        7L to listOf("pharmacy", "clinic", "hospital", "lab", "kenema", "med", "ፋርማሲ", "ክሊኒክ", "ሆስፒታል"),
        // Education
        8L to listOf("school", "college", "tuition", "university", "book", "ትምህርት", "ትምህርት ቤት"),
        // Shopping
        9L to listOf("shopping", "boutique", "clothes", "shoes", "mall", "ሱቅ"),
        // Salary
        16L to listOf("salary", "payroll", "wage", "ደመወዝ")
    )

    suspend fun categorize(
        counterparty: String?,
        smsBody: String,
        amountMinor: Long
    ): CategorizationResult {
        val targetText = "${counterparty ?: ""} $smsBody".lowercase()

        // 1. Check User Rules first (highest precedence)
        val userRules = userRuleDao.getAllRules()
        for (rule in userRules) {
            val matched = when (rule.matchType) {
                RuleMatchType.COUNTERPARTY_CONTAINS -> {
                    !counterparty.isNullOrBlank() && counterparty.contains(rule.pattern, ignoreCase = true)
                }
                RuleMatchType.SMS_CONTAINS -> {
                    smsBody.contains(rule.pattern, ignoreCase = true)
                }
                RuleMatchType.AMOUNT_RANGE -> false
            }

            if (matched) {
                userRuleDao.incrementTimesApplied(rule.id)
                return CategorizationResult(
                    categoryId = rule.categoryId,
                    source = CategorySource.RULE,
                    confidence = 1.0f,
                    needsReview = false
                )
            }
        }

        // 2. Check System Keyword Rules
        for ((catId, keywords) in systemKeywords) {
            for (kw in keywords) {
                if (targetText.contains(kw)) {
                    val confidence = if (!counterparty.isNullOrBlank() && counterparty.lowercase().contains(kw)) 0.90f else 0.70f
                    val needsReview = confidence < 0.85f
                    return CategorizationResult(
                        categoryId = catId,
                        source = CategorySource.AUTO,
                        confidence = confidence,
                        needsReview = needsReview
                    )
                }
            }
        }

        // 3. Fallback: Uncategorized, requires review
        return CategorizationResult(
            categoryId = null,
            source = CategorySource.NONE,
            confidence = 0.20f,
            needsReview = true
        )
    }
}
