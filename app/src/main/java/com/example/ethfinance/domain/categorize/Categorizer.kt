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

    suspend fun categorize(
        counterparty: String?,
        smsBody: String,
        amountMinor: Long
    ): CategorizationResult {
        // 1. Only apply user-defined rules (user explicitly assigned/taught category)
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

        // 2. Default: Keep category null (uncategorized) unless user assigns it
        // The user explicitly requested not to auto-assign Rent, Transport, etc.
        return CategorizationResult(
            categoryId = null,
            source = CategorySource.NONE,
            confidence = 0.0f,
            needsReview = true
        )
    }
}
