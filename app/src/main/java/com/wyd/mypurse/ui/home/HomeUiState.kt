package com.wyd.mypurse.ui.home

import com.wyd.mypurse.domain.model.CategoryAmount
import com.wyd.mypurse.domain.model.MonthlyAmount
import com.wyd.mypurse.domain.model.PeriodSummary
import java.math.BigDecimal

/**
 * 首页 UI 状态。
 */
data class HomeUiState(
    val balance: BigDecimal = BigDecimal.ZERO,
    val today: PeriodSummary = PeriodSummary(),
    val thisWeek: PeriodSummary = PeriodSummary(),
    val thisMonth: PeriodSummary = PeriodSummary(),
    val thisYear: PeriodSummary = PeriodSummary(),
    val trend: List<MonthlyAmount> = emptyList(),
    val topCategories: List<CategoryAmount> = emptyList(),
    val budget: BigDecimal? = null,
    val isLoading: Boolean = true
)
