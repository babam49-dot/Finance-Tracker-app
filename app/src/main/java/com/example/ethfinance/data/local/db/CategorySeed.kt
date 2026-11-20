package com.example.ethfinance.data.local.db

import com.example.ethfinance.data.local.db.entity.CategoryEntity

object CategorySeed {

    fun getDefaultCategories(): List<CategoryEntity> = listOf(
        // Expense categories
        CategoryEntity(id = 1, key = "transport", nameResKey = "Transport", iconName = "directions_car", colorHex = "#3B82F6", isIncomeCategory = false, sortOrder = 1),
        CategoryEntity(id = 2, key = "food", nameResKey = "Food & Dining", iconName = "restaurant", colorHex = "#EF4444", isIncomeCategory = false, sortOrder = 2),
        CategoryEntity(id = 3, key = "groceries", nameResKey = "Groceries", iconName = "shopping_cart", colorHex = "#10B981", isIncomeCategory = false, sortOrder = 3),
        CategoryEntity(id = 4, key = "airtime", nameResKey = "Airtime & Data", iconName = "phone_android", colorHex = "#8B5CF6", isIncomeCategory = false, sortOrder = 4),
        CategoryEntity(id = 5, key = "bills", nameResKey = "Bills & Utilities", iconName = "bolt", colorHex = "#F59E0B", isIncomeCategory = false, sortOrder = 5),
        CategoryEntity(id = 6, key = "rent", nameResKey = "Rent", iconName = "home", colorHex = "#6366F1", isIncomeCategory = false, sortOrder = 6),
        CategoryEntity(id = 7, key = "health", nameResKey = "Health", iconName = "local_hospital", colorHex = "#EC4899", isIncomeCategory = false, sortOrder = 7),
        CategoryEntity(id = 8, key = "education", nameResKey = "Education", iconName = "school", colorHex = "#14B8A6", isIncomeCategory = false, sortOrder = 8),
        CategoryEntity(id = 9, key = "shopping", nameResKey = "Shopping", iconName = "shopping_bag", colorHex = "#F97316", isIncomeCategory = false, sortOrder = 9),
        CategoryEntity(id = 10, key = "entertainment", nameResKey = "Entertainment", iconName = "movie", colorHex = "#A855F7", isIncomeCategory = false, sortOrder = 10),
        CategoryEntity(id = 11, key = "family_gifts", nameResKey = "Family & Gifts", iconName = "card_giftcard", colorHex = "#EAB308", isIncomeCategory = false, sortOrder = 11),
        CategoryEntity(id = 12, key = "savings_equb", nameResKey = "Savings & Equb", iconName = "savings", colorHex = "#059669", isIncomeCategory = false, sortOrder = 12),
        CategoryEntity(id = 13, key = "fees", nameResKey = "Fees & Charges", iconName = "receipt_long", colorHex = "#64748B", isIncomeCategory = false, sortOrder = 13),
        CategoryEntity(id = 14, key = "cash_withdrawal", nameResKey = "Cash Withdrawal", iconName = "payments", colorHex = "#0D9488", isIncomeCategory = false, sortOrder = 14),
        CategoryEntity(id = 15, key = "other_expense", nameResKey = "Other Expense", iconName = "more_horiz", colorHex = "#94A3B8", isIncomeCategory = false, sortOrder = 15),

        // Income categories
        CategoryEntity(id = 16, key = "salary", nameResKey = "Salary", iconName = "work", colorHex = "#1E7F3E", isIncomeCategory = true, sortOrder = 16),
        CategoryEntity(id = 17, key = "business", nameResKey = "Business", iconName = "store", colorHex = "#047857", isIncomeCategory = true, sortOrder = 17),
        CategoryEntity(id = 18, key = "transfer_received", nameResKey = "Transfer Received", iconName = "call_received", colorHex = "#059669", isIncomeCategory = true, sortOrder = 18),
        CategoryEntity(id = 19, key = "refund", nameResKey = "Refund", iconName = "replay", colorHex = "#10B981", isIncomeCategory = true, sortOrder = 19),
        CategoryEntity(id = 20, key = "other_income", nameResKey = "Other Income", iconName = "attach_money", colorHex = "#16A34A", isIncomeCategory = true, sortOrder = 20),

        // Special categories
        CategoryEntity(id = 21, key = "internal_transfer", nameResKey = "Internal Transfer", iconName = "sync_alt", colorHex = "#6B7280", isIncomeCategory = false, sortOrder = 21),
        CategoryEntity(id = 22, key = "uncategorized", nameResKey = "Uncategorized", iconName = "help_outline", colorHex = "#9CA3AF", isIncomeCategory = false, sortOrder = 22)
    )
}
