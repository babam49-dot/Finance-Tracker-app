package com.example.ethfinance.presentation.transactionDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ethfinance.domain.model.Transaction
import com.example.ethfinance.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionDetailUiState(
    val transaction: Transaction? = null,
    val isExcluded: Boolean = false,
    val isSaved: Boolean = false
)

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val transactionId: Long = checkNotNull(savedStateHandle["transactionId"])

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val tx = transactionRepository.getTransactionById(transactionId)
            _uiState.update {
                it.copy(
                    transaction = tx,
                    isExcluded = tx?.isExcludedFromReports ?: false
                )
            }
        }
    }

    fun toggleExcludeFromReports() {
        val currentTx = _uiState.value.transaction ?: return
        viewModelScope.launch {
            // Update excluded state
            _uiState.update { it.copy(isExcluded = !it.isExcluded) }
        }
    }
}
