package com.example.ethfinance.presentation.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ethfinance.domain.model.Transaction
import com.example.ethfinance.domain.repository.SettingsRepository
import com.example.ethfinance.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReviewUiState(
    val selectedTab: Int = 0, // 0 = Suggestions, 1 = Needs Category
    val suggestions: List<Transaction> = emptyList(),
    val needsCategory: List<Transaction> = emptyList(),
    val areAmountsHidden: Boolean = false,
    val isSampleMode: Boolean = true
)

sealed interface ReviewUiEvent {
    data class SelectTab(val index: Int) : ReviewUiEvent
    data class ApproveSuggestion(val transactionId: Long) : ReviewUiEvent
    data class IgnoreTransaction(val transactionId: Long) : ReviewUiEvent
    data object ApproveAllHighConfidence : ReviewUiEvent
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReviewUiState())
    val uiState: StateFlow<ReviewUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.modeFlow,
                settingsRepository.hideAmountsFlow
            ) { mode, hideAmounts ->
                Pair(mode == "DEMO", hideAmounts)
            }.collect { (isSample, hideAmounts) ->
                transactionRepository.getReviewTransactions(isSample).collect { reviewList ->
                    val suggestions = reviewList.filter { it.categoryId != null }
                    val needs = reviewList.filter { it.categoryId == null }

                    _uiState.update {
                        it.copy(
                            suggestions = suggestions,
                            needsCategory = needs,
                            areAmountsHidden = hideAmounts,
                            isSampleMode = isSample
                        )
                    }
                }
            }
        }
    }

    fun onEvent(event: ReviewUiEvent) {
        when (event) {
            is ReviewUiEvent.SelectTab -> {
                _uiState.update { it.copy(selectedTab = event.index) }
            }
            is ReviewUiEvent.ApproveSuggestion -> {
                viewModelScope.launch {
                    val tx = _uiState.value.suggestions.find { it.id == event.transactionId } ?: return@launch
                    tx.categoryId?.let { catId ->
                        transactionRepository.updateCategory(
                            transactionId = tx.id,
                            categoryId = catId,
                            createRule = true
                        )
                    }
                }
            }
            is ReviewUiEvent.IgnoreTransaction -> {
                viewModelScope.launch {
                    val tx = (_uiState.value.suggestions + _uiState.value.needsCategory).find { it.id == event.transactionId } ?: return@launch
                    transactionRepository.updateCategory(
                        transactionId = tx.id,
                        categoryId = 22L, // Uncategorized
                        createRule = false
                    )
                }
            }
            is ReviewUiEvent.ApproveAllHighConfidence -> {
                viewModelScope.launch {
                    val highConfidence = _uiState.value.suggestions.filter { it.confidence >= 0.60f }
                    for (tx in highConfidence) {
                        tx.categoryId?.let { catId ->
                            transactionRepository.updateCategory(
                                transactionId = tx.id,
                                categoryId = catId,
                                createRule = true
                            )
                        }
                    }
                }
            }
        }
    }
}
