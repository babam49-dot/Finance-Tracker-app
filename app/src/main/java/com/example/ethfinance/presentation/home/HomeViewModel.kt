package com.example.ethfinance.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ethfinance.data.sms.SmsInboxReader
import com.example.ethfinance.domain.model.CashFlow
import com.example.ethfinance.domain.model.OverviewSummary
import com.example.ethfinance.domain.repository.SettingsRepository
import com.example.ethfinance.domain.usecase.GetOverviewUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val overview: OverviewSummary = OverviewSummary(
        totalTrackedMinor = 1083484L,
        digitalBalanceMinor = 803484L,
        cashBalanceMinor = 280000L,
        cashFlow = CashFlow(400000L, 67626L, 332374L),
        pendingReviewCount = 2,
        accounts = emptyList(),
        isSampleMode = true,
        areAmountsHidden = false
    ),
    val isMiniInsightDismissed: Boolean = false,
    val isSampleBannerDismissed: Boolean = false,
    val isSyncing: Boolean = false
)

sealed interface HomeUiEvent {
    data object ToggleHideAmounts : HomeUiEvent
    data object SyncNow : HomeUiEvent
    data object DismissMiniInsight : HomeUiEvent
    data object DismissSampleBanner : HomeUiEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getOverviewUseCase: GetOverviewUseCase,
    private val settingsRepository: SettingsRepository,
    private val smsInboxReader: SmsInboxReader
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getOverviewUseCase().collect { overview ->
                _uiState.update { it.copy(overview = overview) }
            }
        }
    }

    fun onEvent(event: HomeUiEvent) {
        when (event) {
            HomeUiEvent.ToggleHideAmounts -> {
                viewModelScope.launch {
                    settingsRepository.toggleHideAmounts()
                }
            }
            HomeUiEvent.SyncNow -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isSyncing = true) }
                    smsInboxReader.importInbox()
                    _uiState.update { it.copy(isSyncing = false) }
                }
            }
            HomeUiEvent.DismissMiniInsight -> {
                _uiState.update { it.copy(isMiniInsightDismissed = true) }
            }
            HomeUiEvent.DismissSampleBanner -> {
                _uiState.update { it.copy(isSampleBannerDismissed = true) }
            }
        }
    }
}
