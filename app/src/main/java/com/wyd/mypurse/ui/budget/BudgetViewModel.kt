package com.wyd.mypurse.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wyd.mypurse.data.local.dao.BudgetDao
import com.wyd.mypurse.data.local.entity.BudgetEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

/**
 * 预算设置页 ViewModel。操作极简 budget 表（仅一条记录）。
 */
@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetDao: BudgetDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadBudget()
    }

    private fun loadBudget() {
        viewModelScope.launch {
            val entity = budgetDao.getBudget()
            _uiState.update {
                it.copy(
                    amount = entity?.amount,
                    isLoading = false
                )
            }
        }
    }

    fun onAmountChange(newValue: BigDecimal) {
        _uiState.update { it.copy(amount = newValue, isSaved = false) }
    }

    fun saveBudget() {
        val amount = _uiState.value.amount ?: return
        viewModelScope.launch {
            budgetDao.upsertBudget(
                BudgetEntity(amount = amount, updatedAt = System.currentTimeMillis())
            )
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun clearBudget() {
        viewModelScope.launch {
            val entity = budgetDao.getBudget()
            if (entity != null) {
                budgetDao.deleteBudget(entity)
                _uiState.update { it.copy(amount = null, isSaved = false) }
            }
        }
    }
}

data class BudgetUiState(
    val amount: BigDecimal? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)
