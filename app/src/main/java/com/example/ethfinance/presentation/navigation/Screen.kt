package com.example.ethfinance.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val titleResKey: String, val icon: ImageVector) {
    data object Home : Screen("home", "nav_home", Icons.Outlined.Home)
    data object Transactions : Screen("transactions", "nav_transactions", Icons.Outlined.ReceiptLong)
    data object Reports : Screen("reports", "nav_reports", Icons.Outlined.Assessment)
    data object Review : Screen("review", "nav_review", Icons.Outlined.Checklist)
    data object Settings : Screen("settings", "nav_settings", Icons.Outlined.Settings)

    data object TransactionDetail : Screen("transaction_detail/{transactionId}", "detail_title", Icons.Outlined.ReceiptLong) {
        fun createRoute(transactionId: Long) = "transaction_detail/$transactionId"
    }

    data object AddCash : Screen("add_cash", "add_cash_title", Icons.Outlined.ReceiptLong)
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Transactions,
    Screen.Reports,
    Screen.Review,
    Screen.Settings
)
