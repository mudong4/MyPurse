package com.wyd.mypurse.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wyd.mypurse.domain.repository.TransactionRepository
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
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetUiState())
    val uiState: StateFlow<BudgetUiState> = _uiState.asStateFlow()

    init {
        loadBudget()
    }

    private fun loadBudget() {
        viewModelScope.launch {
            val amount = transactionRepository.getBudget()
            _uiState.update {
                it.copy(
                    amount = amount,
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
            transactionRepository.updateBudget(amount)
            _uiState.update { it.copy(isSaved = true) }
        }
    }

    fun clearBudget() {
        viewModelScope.launch {
            transactionRepository.deleteBudget()
            _uiState.update { it.copy(amount = null, isSaved = false) }
        }
    }
}

data class BudgetUiState(
    val amount: BigDecimal? = null,
    val isLoading: Boolean = true,
    val isSaved: Boolean = false
)
