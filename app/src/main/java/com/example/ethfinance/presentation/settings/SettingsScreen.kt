package com.example.ethfinance.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ethfinance.R
import com.example.ethfinance.core.designsystem.LocalExtendedColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val state by viewModel.uiState.collectAsState()
    val extendedColors = LocalExtendedColors.current

    if (state.showModeDialog) {
        val targetMode = if (state.mode == "DEMO") "Live SMS Mode" else "Demo Mode"
        AlertDialog(
            onDismissRequest = { viewModel.onEvent(SettingsUiEvent.DismissModeDialog) },
            title = { Text("Switch to $targetMode?") },
            text = {
                Text(
                    if (state.mode == "DEMO") {
                        "Switching to Live Mode will clear sample data and begin tracking your real bank messages on this phone. Real data is never deleted when switching."
                    } else {
                        "Switching to Demo Mode will display realistic sample data. Your real imported transactions will remain safely stored."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onEvent(SettingsUiEvent.ConfirmSwitchMode) },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onEvent(SettingsUiEvent.DismissModeDialog) }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp),
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
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
            // 1. Tracking Mode Section
            item {
                SettingsCard(title = stringResource(R.string.section_mode)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (state.mode == "DEMO") stringResource(R.string.mode_demo) else stringResource(R.string.mode_live),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (state.mode == "DEMO") "Using sample data (no permissions)" else "Active real-time SMS tracking",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = { viewModel.onEvent(SettingsUiEvent.RequestToggleMode) },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Switch Mode")
                        }
                    }
                }
            }

            // 2. Language Section (6 Ethiopian Languages)
            item {
                SettingsCard(title = stringResource(R.string.section_language)) {
                    val languages = listOf(
                        "en" to "English",
                        "am" to "አማርኛ (Amharic)",
                        "om" to "Afaan Oromoo",
                        "ti" to "ትግርኛ (Tigrinya)",
                        "so" to "Soomaali (Somali)",
                        "aa" to "Qafaraf (Afar)"
                    )

                    languages.forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.language == code,
                                onClick = { viewModel.onEvent(SettingsUiEvent.SetLanguage(code)) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (state.language == code) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }

            // 3. Calendar & Numbers Section
            item {
                SettingsCard(title = stringResource(R.string.section_calendar)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Ethiopian Calendar (Ge'ez)",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Display dates as Meskerem..Pagume",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = state.useEthiopianCalendar,
                            onCheckedChange = { viewModel.onEvent(SettingsUiEvent.SetEthiopianCalendar(it)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Divider(color = extendedColors.divider)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Currency Symbol",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("ETB", "Birr", "ብር").forEach { symbol ->
                            FilterChip(
                                selected = state.currencyDisplay == symbol,
                                onClick = { viewModel.onEvent(SettingsUiEvent.SetCurrencyDisplay(symbol)) },
                                label = { Text(symbol) }
                            )
                        }
                    }
                }
            }

            // 4. Typography Section
            item {
                SettingsCard(title = stringResource(R.string.section_typography)) {
                    Text(
                        text = "Font Scale: ${(state.fontScale * 100).toInt()}%",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Slider(
                        value = state.fontScale,
                        onValueChange = { viewModel.onEvent(SettingsUiEvent.SetFontScale(it)) },
                        valueRange = 0.9f..1.3f,
                        steps = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Divider(color = extendedColors.divider)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Bold Numbers",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Switch(
                            checked = state.boldNumbers,
                            onCheckedChange = { viewModel.onEvent(SettingsUiEvent.SetBoldNumbers(it)) }
                        )
                    }
                }
            }

            // 5. Data & Privacy Section
            item {
                SettingsCard(title = stringResource(R.string.section_security)) {
                    Text(
                        text = "100% Offline & On-Device",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "This application has zero internet access permissions. Your bank SMS messages and balances never leave this phone under any circumstances.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = { viewModel.onEvent(SettingsUiEvent.DeleteAllData) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = extendedColors.expenseRed),
                        border = BorderStroke(1.dp, extendedColors.expenseRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Delete All My Data")
                    }
                }
            }

            // 6. About
            item {
                SettingsCard(title = stringResource(R.string.section_about)) {
                    Text(
                        text = "Ethiopian Finance Tracker v1.0.0",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Native Android app built with Kotlin & Jetpack Compose",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
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
                text = title.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = extendedColors.textLabel
            )
            Spacer(modifier = Modifier.height(14.dp))
            content()
        }
    }
}
