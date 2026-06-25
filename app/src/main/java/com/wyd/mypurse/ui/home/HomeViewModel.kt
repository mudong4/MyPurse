package com.wyd.mypurse.ui.home

import androidx.lifecycle.viewModelScope
import com.wyd.mypurse.domain.usecase.GetHomeDataUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 首页 ViewModel。通过 GetHomeDataUseCase 获取所有首页数据。
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    getHomeDataUseCase: GetHomeDataUseCase
) : androidx.lifecycle.ViewModel() {

    val uiState: StateFlow<HomeUiState> = getHomeDataUseCase()
        .map { snapshot -> snapshot.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}

private fun GetHomeDataUseCase.HomeSnapshot.toUiState(): HomeUiState =
    HomeUiState(
        balance = balance,
        today = today,
        thisWeek = thisWeek,
        thisMonth = thisMonth,
        thisYear = thisYear,
        globalExpense = globalExpense,
        globalIncome = globalIncome,
        trend = trend,
        topCategories = topCategories,
        budget = budget,
        isLoading = isLoading
    )
