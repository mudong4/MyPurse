package com.wyd.mypurse.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wyd.mypurse.data.local.dao.BudgetDao
import com.wyd.mypurse.domain.usecase.GetStatisticsUseCase
import com.wyd.mypurse.domain.usecase.GetStatisticsUseCase.Granularity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getStatisticsUseCase: GetStatisticsUseCase,
    private val budgetDao: BudgetDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatisticsUiState())
    val uiState: StateFlow<StatisticsUiState> = _uiState.asStateFlow()

    init {
        loadData()
        loadAvailableYears()
    }

    private fun loadAvailableYears() {
        viewModelScope.launch {
            try {
                val years = getStatisticsUseCase.getAvailableYears()
                _uiState.update { it.copy(availableYears = years) }
            } catch (_: Exception) {
                // 静默失败
            }
        }
    }

    fun loadData() {
        val current = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val snapshot = getStatisticsUseCase.loadData(
                granularity = current.granularity,
                timestamp = current.currentTimestamp
            )
            val budget = budgetDao.getBudget()?.amount
            val (rangeStart, rangeEnd) = getStatisticsUseCase.calculateTimeRange(
                current.granularity, current.currentTimestamp
            )
            _uiState.update { prev ->
                prev.copy(
                    granularity = snapshot.granularity,
                    timeLabel = snapshot.timeLabel,
                    timeRangeStart = rangeStart,
                    timeRangeEnd = rangeEnd,
                    totalExpense = snapshot.totalExpense,
                    totalIncome = snapshot.totalIncome,
                    composition = snapshot.composition,
                    incomeComposition = snapshot.incomeComposition,
                    trend = snapshot.trend,
                    expandedCategoryId = null,
                    subComposition = emptyList(),
                    budget = budget,
                    isLoading = false
                )
            }
        }
    }

    fun setGranularity(granularity: Granularity) {
        _uiState.update { it.copy(granularity = granularity, currentTimestamp = System.currentTimeMillis()) }
        loadData()
    }

    fun navigateTime(direction: Int) {
        val current = _uiState.value
        val cal = Calendar.getInstance().apply { timeInMillis = current.currentTimestamp }
        when (current.granularity) {
            Granularity.DAY -> cal.add(Calendar.DAY_OF_MONTH, direction)
            Granularity.WEEK -> cal.add(Calendar.WEEK_OF_YEAR, direction)
            Granularity.MONTH -> cal.add(Calendar.MONTH, direction)
            Granularity.QUARTER -> cal.add(Calendar.MONTH, direction * 3)
            Granularity.YEAR -> cal.add(Calendar.YEAR, direction)
        }
        _uiState.update { it.copy(currentTimestamp = cal.timeInMillis) }
        loadData()
    }

    /** 直接跳转到指定时间戳 */
    fun jumpToTimestamp(timestamp: Long) {
        _uiState.update { it.copy(currentTimestamp = timestamp) }
        loadData()
    }

    fun togglePageMode() {
        _uiState.update {
            it.copy(pageMode = if (it.pageMode == PageMode.CATEGORY) PageMode.TREND else PageMode.CATEGORY)
        }
    }

    fun toggleCompositionChartMode() {
        _uiState.update {
            it.copy(compositionChartMode = if (it.compositionChartMode == ChartMode.BAR) ChartMode.DONUT else ChartMode.BAR)
        }
    }

    fun toggleTrendChartMode() {
        _uiState.update {
            it.copy(trendChartMode = if (it.trendChartMode == ChartMode.BAR) ChartMode.LINE else ChartMode.BAR)
        }
    }

    fun toggleCompositionType() {
        _uiState.update {
            it.copy(
                isShowingIncome = !it.isShowingIncome,
                expandedCategoryId = null,
                subComposition = emptyList()
            )
        }
    }

    fun toggleSubCategory(categoryL1Id: Long) {
        val current = _uiState.value
        if (current.expandedCategoryId == categoryL1Id) {
            _uiState.update { it.copy(expandedCategoryId = null, subComposition = emptyList()) }
            return
        }
        _uiState.update { it.copy(expandedCategoryId = categoryL1Id, isSubLoading = true) }
        viewModelScope.launch {
            val flowType = if (current.isShowingIncome) "收入" else "支出"
            val subs = getStatisticsUseCase.loadSubComposition(
                rangeStart = current.compositionRangeStart(),
                rangeEnd = current.compositionRangeEnd(),
                flowType = flowType,
                categoryL1Id = categoryL1Id
            )
            _uiState.update { it.copy(subComposition = subs, isSubLoading = false) }
        }
    }

    private fun StatisticsUiState.compositionRangeStart(): Long {
        return getStatisticsUseCase.calculateTimeRange(granularity, currentTimestamp).first
    }

    private fun StatisticsUiState.compositionRangeEnd(): Long {
        return getStatisticsUseCase.calculateTimeRange(granularity, currentTimestamp).second
    }
}
