package com.example.ethfinance.presentation.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ethfinance.core.util.DateUtils
import com.example.ethfinance.data.local.db.dao.AccountSpending
import com.example.ethfinance.data.local.db.dao.CategoryDao
import com.example.ethfinance.data.local.db.dao.CategorySpending
import com.example.ethfinance.data.local.db.dao.MerchantSpend
import com.example.ethfinance.domain.model.Transaction
import com.example.ethfinance.domain.repository.ReportRepository
import com.example.ethfinance.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategorySlice(
    val categoryName: String,
    val colorHex: String,
    val amountMinor: Long,
    val percentage: Float
)

data class ReportsUiState(
    val selectedPeriod: DateUtils.PeriodType = DateUtils.PeriodType.THIS_MONTH,
    val compareWithPrevious: Boolean = false,
    val totalIncomeMinor: Long = 400000L, // 4,000.00 ETB
    val totalExpenseMinor: Long = 67626L, // 676.26 ETB
    val netSavedMinor: Long = 332374L, // 3,323.74 ETB
    val categorySlices: List<CategorySlice> = emptyList(),
    val topMerchants: List<MerchantSpend> = emptyList(),
    val biggestExpenses: List<Transaction> = emptyList(),
    val accountSpendings: List<AccountSpending> = emptyList(),
    val areAmountsHidden: Boolean = false
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val categoryDao: CategoryDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val range = DateUtils.getPeriodRange(DateUtils.PeriodType.THIS_MONTH)
        viewModelScope.launch {
            combine(
                reportRepository.getSpendingByCategory(range.startEpochMillis, range.endEpochMillis, isSample = true),
                categoryDao.getAllCategoriesFlow(),
                reportRepository.getTopMerchants(range.startEpochMillis, range.endEpochMillis, 5, isSample = true),
                reportRepository.getBiggestExpenses(range.startEpochMillis, range.endEpochMillis, 5, isSample = true),
                settingsRepository.hideAmountsFlow
            ) { catSpends, categories, merchants, biggest, hideAmounts ->
                val catMap = categories.associateBy { it.id }
                val totalExp = catSpends.sumOf { it.totalMinor }.coerceAtLeast(1L)

                val slices = catSpends.map { spend ->
                    val cat = catMap[spend.categoryId]
                    val pct = (spend.totalMinor.toFloat() / totalExp.toFloat()) * 100f
                    CategorySlice(
                        categoryName = cat?.nameResKey ?: "Other",
                        colorHex = cat?.colorHex ?: "#94A3B8",
                        amountMinor = spend.totalMinor,
                        percentage = pct
                    )
                }

                _uiState.update {
                    it.copy(
                        categorySlices = slices,
                        topMerchants = merchants,
                        biggestExpenses = biggest,
                        areAmountsHidden = hideAmounts
                    )
                }
            }.collect {}
        }
    }

    fun toggleCompare() {
        _uiState.update { it.copy(compareWithPrevious = !it.compareWithPrevious) }
    }
}
