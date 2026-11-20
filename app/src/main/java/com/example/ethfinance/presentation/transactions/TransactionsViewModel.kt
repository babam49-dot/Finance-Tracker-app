package com.example.ethfinance.presentation.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ethfinance.core.util.DateUtils
import com.example.ethfinance.domain.model.Transaction
import com.example.ethfinance.domain.repository.SettingsRepository
import com.example.ethfinance.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val selectedPeriod: DateUtils.PeriodType = DateUtils.PeriodType.DAYS_7,
    val searchQuery: String = "",
    val customStartEpoch: Long? = null,
    val customEndEpoch: Long? = null,
    val transactions: List<Transaction> = emptyList(),
    val totalInMinor: Long = 0L,
    val totalOutMinor: Long = 0L,
    val count: Int = 0,
    val areAmountsHidden: Boolean = false,
    val isSampleMode: Boolean = true
)

sealed interface TransactionsUiEvent {
    data class SelectPeriod(val period: DateUtils.PeriodType) : TransactionsUiEvent
    data class UpdateSearchQuery(val query: String) : TransactionsUiEvent
    data class DeleteTransaction(val transactionId: Long) : TransactionsUiEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.modeFlow,
                settingsRepository.hideAmountsFlow,
                _uiState
            ) { mode, hideAmounts, state ->
                Triple(mode == "DEMO", hideAmounts, state.selectedPeriod)
            }.flatMapLatest { (isSample, hideAmounts, period) ->
                val range = DateUtils.getPeriodRange(period)
                transactionRepository.getTransactionsInRange(range.startEpochMillis, range.endEpochMillis, isSample)
                    .combine(_uiState) { list, currentState ->
                        val filtered = if (currentState.searchQuery.isBlank()) {
                            list
                        } else {
                            val q = currentState.searchQuery.lowercase()
                            list.filter { tx ->
                                (tx.counterpartyName?.lowercase()?.contains(q) == true) ||
                                        (tx.note?.lowercase()?.contains(q) == true) ||
                                        (tx.reference?.lowercase()?.contains(q) == true)
                            }
                        }

                        var totalIn = 0L
                        var totalOut = 0L
                        for (tx in filtered) {
                            if (!tx.isExcludedFromReports) {
                                if (tx.type.name.contains("IN")) {
                                    totalIn += tx.amountMinor
                                } else if (tx.type.name.contains("OUT") || tx.type.name.contains("EXPENSE")) {
                                    totalOut += tx.amountMinor
                                }
                            }
                        }

                        currentState.copy(
                            transactions = filtered,
                            totalInMinor = totalIn,
                            totalOutMinor = totalOut,
                            count = filtered.size,
                            areAmountsHidden = hideAmounts,
                            isSampleMode = isSample
                        )
                    }
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun onEvent(event: TransactionsUiEvent) {
        when (event) {
            is TransactionsUiEvent.SelectPeriod -> {
                _uiState.update { it.copy(selectedPeriod = event.period) }
            }
            is TransactionsUiEvent.UpdateSearchQuery -> {
                _uiState.update { it.copy(searchQuery = event.query) }
            }
            is TransactionsUiEvent.DeleteTransaction -> {
                viewModelScope.launch {
                    transactionRepository.deleteTransaction(event.transactionId)
                }
            }
        }
    }
}
