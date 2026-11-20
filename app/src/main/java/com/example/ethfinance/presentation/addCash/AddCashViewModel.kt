package com.example.ethfinance.presentation.addCash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ethfinance.core.util.MoneyFormatter
import com.example.ethfinance.data.local.db.dao.CategoryDao
import com.example.ethfinance.data.local.db.entity.CategoryEntity
import com.example.ethfinance.data.local.db.entity.TransactionType
import com.example.ethfinance.domain.usecase.LogCashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddCashUiState(
    val amountText: String = "",
    val selectedType: TransactionType = TransactionType.EXPENSE,
    val selectedCategoryId: Long? = 2L, // Food & Dining default
    val note: String = "",
    val categories: List<CategoryEntity> = emptyList(),
    val isSaved: Boolean = false
)

sealed interface AddCashUiEvent {
    data class UpdateAmount(val amount: String) : AddCashUiEvent
    data class AddQuickAmount(val addSantim: Long) : AddCashUiEvent
    data class SelectType(val type: TransactionType) : AddCashUiEvent
    data class SelectCategory(val categoryId: Long) : AddCashUiEvent
    data class UpdateNote(val note: String) : AddCashUiEvent
    data object Save : AddCashUiEvent
}

@HiltViewModel
class AddCashViewModel @Inject constructor(
    private val logCashUseCase: LogCashUseCase,
    private val categoryDao: CategoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddCashUiState())
    val uiState: StateFlow<AddCashUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            categoryDao.getAllCategoriesFlow().collect { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun onEvent(event: AddCashUiEvent) {
        when (event) {
            is AddCashUiEvent.UpdateAmount -> {
                _uiState.update { it.copy(amountText = event.amount) }
            }
            is AddCashUiEvent.AddQuickAmount -> {
                val currentSantim = MoneyFormatter.parseToSantim(_uiState.value.amountText) ?: 0L
                val newSantim = currentSantim + event.addSantim
                val newAmountStr = "%.2f".format(newSantim / 100.0)
                _uiState.update { it.copy(amountText = newAmountStr) }
            }
            is AddCashUiEvent.SelectType -> {
                _uiState.update { it.copy(selectedType = event.type) }
            }
            is AddCashUiEvent.SelectCategory -> {
                _uiState.update { it.copy(selectedCategoryId = event.categoryId) }
            }
            is AddCashUiEvent.UpdateNote -> {
                _uiState.update { it.copy(note = event.note) }
            }
            is AddCashUiEvent.Save -> {
                val santim = MoneyFormatter.parseToSantim(_uiState.value.amountText) ?: return
                viewModelScope.launch {
                    logCashUseCase(
                        amountMinor = santim,
                        type = _uiState.value.selectedType,
                        categoryId = _uiState.value.selectedCategoryId,
                        note = _uiState.value.note.takeIf { it.isNotBlank() },
                        occurredAt = System.currentTimeMillis()
                    )
                    _uiState.update { it.copy(isSaved = true) }
                }
            }
        }
    }
}
