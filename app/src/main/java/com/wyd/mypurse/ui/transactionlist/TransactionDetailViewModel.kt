package com.wyd.mypurse.ui.transactionlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wyd.mypurse.domain.model.Transaction
import com.wyd.mypurse.domain.repository.CategoryRepository
import com.wyd.mypurse.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionDetailUiState(isEditing = true))
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    fun loadTransaction(id: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val transaction = transactionRepository.getTransactionById(id)
                if (transaction != null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            transaction = transaction,
                            editAmount = transaction.amount.toPlainString(),
                            editNote = transaction.note ?: "",
                            editCategoryL1 = transaction.categoryL1,
                            editCategoryL2 = transaction.categoryL2,
                            editDate = transaction.date,
                            editFlowType = transaction.flowType,
                            isRecurring = transaction.recurringTemplateId != null,
                            isEditing = true   // 默认进入编辑模式
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "记录不存在") }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "加载失败")
                }
            }
        }
    }

    fun enterEditMode() {
        val transaction = _uiState.value.transaction ?: return
        _uiState.update {
            it.copy(
                isEditing = true,
                editAmount = transaction.amount.toPlainString(),
                editNote = transaction.note ?: "",
                editCategoryL1 = transaction.categoryL1,
                editCategoryL2 = transaction.categoryL2,
                editDate = transaction.date,
                editFlowType = transaction.flowType,
                editAmountError = null
            )
        }
    }

    fun cancelEdit() {
        // 取消编辑 → 直接返回（由 Screen 层处理）
        _uiState.update { it.copy(isEditing = false) }
    }

    fun updateEditAmount(value: String) {
        var filtered = value.replace(Regex("[^0-9.]"), "")
        val dotCount = filtered.count { it == '.' }
        if (dotCount > 1) {
            filtered = filtered.replaceFirst(".", "\u0000").replace(".", "").replace("\u0000", ".")
        }
        val dotIndex = filtered.indexOf('.')
        if (dotIndex >= 0 && filtered.length - dotIndex > 3) {
            filtered = filtered.substring(0, dotIndex + 3)
        }

        val error = try {
            val decimal = BigDecimal(filtered)
            if (decimal <= BigDecimal.ZERO) "金额必须大于0" else null
        } catch (_: NumberFormatException) {
            if (filtered.isBlank()) "请输入金额" else "请输入有效金额"
        }

        _uiState.update { it.copy(editAmount = filtered, editAmountError = error) }
    }

    fun updateEditNote(value: String) {
        _uiState.update { it.copy(editNote = value) }
    }

    fun updateEditFlowType(flowType: String) {
        _uiState.update { it.copy(editFlowType = flowType) }
    }

    fun updateEditDate(dateMillis: Long) {
        _uiState.update { it.copy(editDate = dateMillis) }
    }

    fun updateEditCategory(l1: String, l2: String?) {
        _uiState.update {
            it.copy(
                editCategoryL1 = l1,
                editCategoryL2 = l2,
                showCategoryPicker = false
            )
        }
    }

    fun toggleCategoryPicker() {
        _uiState.update { it.copy(showCategoryPicker = !it.showCategoryPicker) }
    }

    fun dismissCategoryPicker() {
        _uiState.update { it.copy(showCategoryPicker = false) }
    }

    fun save() {
        val state = _uiState.value
        val transaction = state.transaction ?: return

        val amount = try {
            BigDecimal(state.editAmount)
        } catch (_: NumberFormatException) {
            _uiState.update { it.copy(editAmountError = "请输入有效金额") }
            return
        }

        if (amount <= BigDecimal.ZERO) {
            _uiState.update { it.copy(editAmountError = "金额必须大于0") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val updated = transaction.copy(
                    flowType = state.editFlowType,
                    categoryL1 = state.editCategoryL1,
                    categoryL2 = state.editCategoryL2,
                    amount = amount,
                    note = state.editNote.ifBlank { null },
                    date = state.editDate,
                    recurringTemplateId = null  // 编辑后解除模板关联
                )
                transactionRepository.updateTransaction(updated)
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "保存失败")
                }
            }
        }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
