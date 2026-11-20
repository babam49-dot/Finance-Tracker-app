package com.example.ethfinance.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ethfinance.R
import com.example.ethfinance.core.designsystem.LocalExtendedColors
import com.example.ethfinance.presentation.addCash.AddCashScreen
import com.example.ethfinance.presentation.addCash.AddCashViewModel
import com.example.ethfinance.presentation.home.HomeScreen
import com.example.ethfinance.presentation.home.HomeViewModel
import com.example.ethfinance.presentation.reports.ReportsScreen
import com.example.ethfinance.presentation.reports.ReportsViewModel
import com.example.ethfinance.presentation.review.ReviewScreen
import com.example.ethfinance.presentation.review.ReviewViewModel
import com.example.ethfinance.presentation.settings.SettingsScreen
import com.example.ethfinance.presentation.settings.SettingsViewModel
import com.example.ethfinance.presentation.transactionDetail.TransactionDetailScreen
import com.example.ethfinance.presentation.transactionDetail.TransactionDetailViewModel
import com.example.ethfinance.presentation.transactions.TransactionsScreen
import com.example.ethfinance.presentation.transactions.TransactionsViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Check if bottom bar should be visible
    val isBottomBarVisible = bottomNavScreens.any { it.route == currentRoute }

    // Shared review badge count from HomeViewModel
    val homeViewModel: HomeViewModel = hiltViewModel()
    val homeUiState by homeViewModel.uiState.collectAsState()
    val reviewCount = homeUiState.overview.pendingReviewCount

    val extendedColors = LocalExtendedColors.current

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 0.dp
                ) {
                    bottomNavScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        val label = when (screen) {
                            Screen.Home -> stringResource(R.string.nav_home)
                            Screen.Transactions -> stringResource(R.string.nav_transactions)
                            Screen.Reports -> stringResource(R.string.nav_reports)
                            Screen.Review -> stringResource(R.string.nav_review)
                            Screen.Settings -> stringResource(R.string.nav_settings)
                            else -> ""
                        }

                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                if (screen == Screen.Review && reviewCount > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge(
                                                containerColor = extendedColors.reviewOrange,
                                                contentColor = Color.White
                                            ) {
                                                Text("$reviewCount")
                                            }
                                        }
                                    ) {
                                        Icon(imageVector = screen.icon, contentDescription = label)
                                    }
                                } else {
                                    Icon(imageVector = screen.icon, contentDescription = label)
                                }
                            },
                            label = { Text(label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = Color(0xFF8C97A8),
                                unselectedTextColor = Color(0xFF8C97A8),
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToReview = { navController.navigate(Screen.Review.route) },
                    onNavigateToAddCash = { navController.navigate(Screen.AddCash.route) },
                    onNavigateToAccount = { accountId ->
                        // Navigate to filtered transactions
                        navController.navigate(Screen.Transactions.route)
                    }
                )
            }

            composable(Screen.Transactions.route) {
                val viewModel: TransactionsViewModel = hiltViewModel()
                TransactionsScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    }
                )
            }

            composable(Screen.Reports.route) {
                val viewModel: ReportsViewModel = hiltViewModel()
                ReportsScreen(viewModel = viewModel)
            }

            composable(Screen.Review.route) {
                val viewModel: ReviewViewModel = hiltViewModel()
                ReviewScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { id ->
                        navController.navigate(Screen.TransactionDetail.createRoute(id))
                    }
                )
            }

            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = viewModel)
            }

            composable(
                route = Screen.TransactionDetail.route,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) {
                val viewModel: TransactionDetailViewModel = hiltViewModel()
                TransactionDetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.AddCash.route) {
                val viewModel: AddCashViewModel = hiltViewModel()
                AddCashScreen(
                    viewModel = viewModel,
                    onDismiss = { navController.popBackStack() }
                )
            }
        }
    }
}
