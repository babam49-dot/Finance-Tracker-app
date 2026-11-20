package com.example.ethfinance.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.AssignmentLate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ethfinance.R
import com.example.ethfinance.core.designsystem.LocalExtendedColors
import com.example.ethfinance.core.util.DateUtils
import com.example.ethfinance.core.util.MoneyFormatter
import com.example.ethfinance.data.local.db.entity.AccountEntity
import com.example.ethfinance.data.local.db.entity.AccountType
import com.example.ethfinance.domain.model.Account

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToReview: () -> Unit,
    onNavigateToAddCash: () -> Unit,
    onNavigateToAccount: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val extendedColors = LocalExtendedColors.current
    val overview = state.overview
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_SMS] == true || permissions[Manifest.permission.RECEIVE_SMS] == true
        if (granted) {
            viewModel.onEvent(HomeUiEvent.SyncNow)
        }
    }

    val handleSync = {
        val hasReadSms = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        if (hasReadSms) {
            viewModel.onEvent(HomeUiEvent.SyncNow)
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS)
            )
        }
    }

    Scaffold(
        topBar = {
            HomeTopBar(
                isLiveMode = !overview.isSampleMode,
                areAmountsHidden = overview.areAmountsHidden,
                isSyncing = state.isSyncing,
                onToggleHideAmounts = { viewModel.onEvent(HomeUiEvent.ToggleHideAmounts) },
                onSyncNow = handleSync
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddCash,
                shape = RoundedCornerShape(999.dp),
                containerColor = Color(0xFF0F5132),
                contentColor = Color.White,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = stringResource(R.string.btn_log_cash),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.btn_log_cash),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Demo Mode Banner
            if (overview.isSampleMode && !state.isSampleBannerDismissed) {
                item {
                    DemoBanner(onDismiss = { viewModel.onEvent(HomeUiEvent.DismissSampleBanner) })
                }
            }

            // 2. Total Tracked Money Card
            item {
                TotalTrackedMoneyCard(
                    totalTrackedMinor = overview.totalTrackedMinor,
                    digitalBalanceMinor = overview.digitalBalanceMinor,
                    cashBalanceMinor = overview.cashBalanceMinor,
                    masked = overview.areAmountsHidden
                )
            }

            // 3. Review Banner (when pending > 0)
            if (overview.pendingReviewCount > 0) {
                item {
                    ReviewBanner(
                        pendingCount = overview.pendingReviewCount,
                        onClick = onNavigateToReview
                    )
                }
            }

            // 4. This Month's Cash Flow Card
            item {
                CashFlowCard(
                    incomeMinor = overview.cashFlow.incomeMinor,
                    expenseMinor = overview.cashFlow.expenseMinor,
                    netFlowMinor = overview.cashFlow.netFlowMinor,
                    masked = overview.areAmountsHidden
                )
            }

            // 5. Mini Insight Row
            if (!state.isMiniInsightDismissed) {
                item {
                    MiniInsightRow(
                        insightText = "You spent 18% more on Transport than last week.",
                        onDismiss = { viewModel.onEvent(HomeUiEvent.DismissMiniInsight) }
                    )
                }
            }

            // 6. Tracked Accounts Section
            item {
                Text(
                    text = stringResource(R.string.tracked_accounts_title),
                    style = MaterialTheme.typography.labelLarge,
                    color = extendedColors.textLabel,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            items(overview.accounts, key = { it.id }) { account ->
                AccountCard(
                    account = account,
                    masked = overview.areAmountsHidden,
                    onClick = { onNavigateToAccount(account.id) }
                )
            }

            // Bottom space for FAB
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
fun HomeTopBar(
    isLiveMode: Boolean,
    areAmountsHidden: Boolean,
    isSyncing: Boolean,
    onToggleHideAmounts: () -> Unit,
    onSyncNow: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.overview_title),
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (isLiveMode) {
                    stringResource(R.string.overview_subtitle_live)
                } else {
                    stringResource(R.string.overview_subtitle_demo)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLiveMode) {
                IconButton(
                    onClick = onSyncNow,
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape)
                        .border(1.dp, extendedColors.cardBorder, CircleShape)
                ) {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleHideAmounts,
                modifier = Modifier
                    .size(42.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .border(1.dp, extendedColors.cardBorder, CircleShape)
            ) {
                Icon(
                    imageVector = if (areAmountsHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Toggle amount visibility",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
fun DemoBanner(onDismiss: () -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.demo_banner_message),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun TotalTrackedMoneyCard(
    totalTrackedMinor: Long,
    digitalBalanceMinor: Long,
    cashBalanceMinor: Long,
    masked: Boolean
) {
    val extendedColors = LocalExtendedColors.current

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extendedColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.total_tracked_money),
                style = MaterialTheme.typography.labelLarge,
                color = extendedColors.textLabel
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = MoneyFormatter.formatSantim(totalTrackedMinor, masked = masked),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = extendedColors.divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.digital_balance_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = MoneyFormatter.formatSantim(digitalBalanceMinor, masked = masked),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.physical_cash_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = MoneyFormatter.formatSantim(cashBalanceMinor, masked = masked),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = extendedColors.cashGold
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewBanner(
    pendingCount: Int,
    onClick: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = extendedColors.reviewBannerBg),
        border = BorderStroke(1.dp, extendedColors.reviewBannerBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(extendedColors.reviewOrange.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.AssignmentLate,
                    contentDescription = null,
                    tint = extendedColors.reviewOrange,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pluralStringResource(R.plurals.review_banner_title, pendingCount, pendingCount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.review_banner_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun CashFlowCard(
    incomeMinor: Long,
    expenseMinor: Long,
    netFlowMinor: Long,
    masked: Boolean
) {
    val extendedColors = LocalExtendedColors.current

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extendedColors.cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.cash_flow_title),
                style = MaterialTheme.typography.labelLarge,
                color = extendedColors.textLabel
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income Column
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(extendedColors.incomeGreen, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.cash_flow_income),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = MoneyFormatter.formatSantim(incomeMinor, includeSign = true, masked = masked),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.incomeGreen
                    )
                }

                // Expenses Column
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(extendedColors.expenseRed, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.cash_flow_expense),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = MoneyFormatter.formatSantim(-expenseMinor, masked = masked),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = extendedColors.expenseRed
                    )
                }

                // Net Flow Column
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = stringResource(R.string.cash_flow_net),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val netColor = if (netFlowMinor >= 0) extendedColors.incomeGreen else extendedColors.expenseRed
                    Text(
                        text = MoneyFormatter.formatSantim(netFlowMinor, includeSign = true, masked = masked),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = netColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = extendedColors.divider, thickness = 1.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.cash_flow_footnote),
                style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic, fontSize = 11.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun MiniInsightRow(
    insightText: String,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = insightText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
fun AccountCard(
    account: Account,
    masked: Boolean,
    onClick: () -> Unit
) {
    val extendedColors = LocalExtendedColors.current

    val (icon, iconTint, iconBg) = when (account.type) {
        AccountType.BANK_CBE -> Triple(Icons.Default.AccountBalance, extendedColors.cbeAccent, extendedColors.cbeAccentBg)
        AccountType.TELEBIRR -> Triple(Icons.Default.PhoneAndroid, extendedColors.telebirrAccent, extendedColors.telebirrAccentBg)
        AccountType.CASH -> Triple(Icons.Default.Payments, extendedColors.cashGold, Color(0xFFFFF8E7))
        AccountType.BANK_OTHER -> Triple(Icons.Default.AccountBalance, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
    }

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extendedColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(iconBg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                if (account.currentBalanceMinor != null) {
                    Text(
                        text = account.maskedNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (account.balanceUpdatedAt != null) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.updated_ago, DateUtils.formatRelativeTime(account.balanceUpdatedAt)),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = extendedColors.textLabel
                        )
                    }
                } else {
                    Text(
                        text = account.maskedNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.balance_unavailable),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                        color = extendedColors.textLabel
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                if (account.currentBalanceMinor != null) {
                    Text(
                        text = MoneyFormatter.formatSantim(account.currentBalanceMinor, masked = masked),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (account.isSample) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.tag_sample),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            ),
                            color = extendedColors.sampleTag
                        )
                    }
                } else {
                    Text(
                        text = "Unavailable",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
