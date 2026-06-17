package com.wyd.mypurse.ui.addtransaction

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wyd.mypurse.data.local.database.DatabaseInitializer
import com.wyd.mypurse.domain.model.Category
import com.wyd.mypurse.domain.repository.CategoryRepository
import com.wyd.mypurse.domain.repository.FlowType
import com.wyd.mypurse.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 记一笔页 ViewModel。
 * 支持连续记账模式：保存后清空金额和备注，保留类型/分类/日期。
 */
@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val databaseInitializer: DatabaseInitializer
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        loadTopLevelCategories(FlowType.EXPENSE.sign)
    }

    /**
     * 初始化默认值（从首页进入时传入）。
     */
    fun initialize(defaultFlowType: String, defaultDate: Long) {
        val flowType = if (defaultFlowType == FlowType.INCOME.name) FlowType.INCOME else FlowType.EXPENSE
        _uiState.update { it.copy(selectedFlowType = flowType, date = defaultDate) }
        loadTopLevelCategories(flowType.sign)
    }

    // ========== 金额 ==========

    fun onAmountChange(value: String) {
        // 限制：最多两位小数
        val parts = value.split(".")
        if (parts.size > 1 && parts[1].length > 2) return
        _uiState.update { it.copy(amount = value, errorMessage = null) }
    }

    fun appendDigit(digit: String) {
        val current = _uiState.value.amount
        // 防止前导多个零
        if (current == "0" && digit != ".") {
            _uiState.update { it.copy(amount = digit) }
            return
        }
        // 已有小数点则限制小数位数
        if (current.contains(".")) {
            val decimalPart = current.substringAfter(".")
            if (decimalPart.length >= 2) return
        }
        // 最大长度限制（含小数点）
        if (current.length >= 10) return
        _uiState.update { it.copy(amount = current + digit) }
    }

    fun deleteDigit() {
        val current = _uiState.value.amount
        if (current.length <= 1) {
            _uiState.update { it.copy(amount = "") }
        } else {
            _uiState.update { it.copy(amount = current.dropLast(1)) }
        }
    }

    fun addDecimalPoint() {
        val current = _uiState.value.amount
        if (current.isEmpty()) {
            _uiState.update { it.copy(amount = "0.") }
        } else if (!current.contains(".")) {
            _uiState.update { it.copy(amount = current + ".") }
        }
    }

    // ========== 流水类型 ==========

    fun onFlowTypeSelected(flowType: com.wyd.mypurse.domain.repository.FlowType) {
        _uiState.update { it.copy(
            selectedFlowType = flowType,
            categoryL1 = null,
            categoryL2 = null
        ) }
        loadTopLevelCategories(flowType.sign)
    }

    // ========== 分类选择 ==========

    fun showCategoryPicker() {
        _uiState.update { it.copy(isCategoryPickerVisible = true) }
    }

    fun dismissCategoryPicker() {
        _uiState.update { it.copy(isCategoryPickerVisible = false) }
    }

    fun onCategoryL1Selected(category: Category) {
        _uiState.update { it.copy(selectedPickerL1 = category) }
        viewModelScope.launch {
            val subs = categoryRepository.getSubCategoriesOnce(category.id)
            _uiState.update { it.copy(subCategories = subs) }
        }
    }

    fun onCategoryBackToL1() {
        _uiState.update { it.copy(selectedPickerL1 = null, subCategories = emptyList()) }
    }

    fun onCategoryL2Selected(category: Category) {
        val l1 = _uiState.value.selectedPickerL1 ?: return
        _uiState.update {
            it.copy(
                categoryL1 = l1,
                categoryL2 = category,
                isCategoryPickerVisible = false,
                selectedPickerL1 = null,
                subCategories = emptyList()
            )
        }
    }

    fun onCategorySkipL2() {
        val l1 = _uiState.value.selectedPickerL1 ?: return
        _uiState.update {
            it.copy(
                categoryL1 = l1,
                categoryL2 = null,
                isCategoryPickerVisible = false,
                selectedPickerL1 = null,
                subCategories = emptyList()
            )
        }
    }

    // ========== 日期 ==========

    fun onDateChanged(timestamp: Long) {
        _uiState.update { it.copy(date = timestamp) }
    }

    // ========== 备注 ==========

    fun onNoteChange(note: String) {
        _uiState.update { it.copy(note = note) }
    }

    // ========== 保存 ==========

    /**
     * 保存并继续（连续记账模式）：清空金额和备注，保留类型/分类/日期，不关闭页面。
     */
    fun saveAndContinue() {
        val state = _uiState.value
        if (!state.isFormValid) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                transactionRepository.insertTransaction(
                    flowType = state.selectedFlowType.name,
                    categoryL1Id = state.categoryL1?.id,
                    categoryL2Id = state.categoryL2?.id,
                    categoryL1 = state.categoryL1!!.name,
                    categoryL2 = state.categoryL2?.name,
                    amount = state.amountDecimal!!,
                    note = state.note.ifBlank { null },
                    date = state.date
                )
                _uiState.update {
                    it.copy(
                        amount = "",
                        note = "",
                        isSaving = false,
                        saveSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "保存失败"
                    )
                }
            }
        }
    }

    /**
     * 保存并关闭：保存后通知页面关闭。
     */
    fun saveAndClose() {
        val state = _uiState.value
        if (!state.isFormValid) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                transactionRepository.insertTransaction(
                    flowType = state.selectedFlowType.name,
                    categoryL1Id = state.categoryL1?.id,
                    categoryL2Id = state.categoryL2?.id,
                    categoryL1 = state.categoryL1!!.name,
                    categoryL2 = state.categoryL2?.name,
                    amount = state.amountDecimal!!,
                    note = state.note.ifBlank { null },
                    date = state.date
                )
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        shouldClose = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "保存失败"
                    )
                }
            }
        }
    }

    fun clearShouldClose() {
        _uiState.update { it.copy(shouldClose = false) }
    }

    fun clearSaveSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }

    // ========== 编辑模式 ==========

    /**
     * 加载已有记录数据用于编辑。编辑模式下保存会走 updateTransaction 而非 insertTransaction。
     */
    fun loadForEdit(transactionId: Long) {
        _uiState.update { it.copy(isLoadingEditData = true) }
        viewModelScope.launch {
            try {
                val tx = transactionRepository.getTransactionById(transactionId)
                if (tx != null) {
                    val flowType = if (tx.flowType == "收入") FlowType.INCOME else FlowType.EXPENSE
                    // 加载对应 sign 的分类列表
                    databaseInitializer.initializeIfNeeded()
                    val categories = categoryRepository.getTopLevelCategoriesBySignOnce(flowType.sign)
                        .filter { !it.isHidden }

                    // 构造预填的 Category 对象（仅用于 UI 显示，不要求完整对象）
                    val prefillL1 = Category(id = tx.categoryL1Id ?: -1, name = tx.categoryL1)
                    val prefillL2 = tx.categoryL2?.let { name ->
                        Category(id = tx.categoryL2Id ?: -1, name = name)
                    }

                    _uiState.update {
                        it.copy(
                            editingTransactionId = transactionId,
                            selectedFlowType = flowType,
                            amount = tx.amount.toPlainString(),
                            categoryL1 = prefillL1,
                            categoryL2 = prefillL2,
                            topLevelCategories = categories,
                            date = tx.date,
                            note = tx.note ?: "",
                            isRecurring = tx.recurringTemplateId != null,
                            isLoadingEditData = false
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isLoadingEditData = false, errorMessage = "记录不存在")
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoadingEditData = false, errorMessage = e.message ?: "加载失败")
                }
            }
        }
    }

    /** 编辑模式：更新已有记录 */
    fun updateExisting() {
        val state = _uiState.value
        val editingId = state.editingTransactionId
        if (editingId == 0L || !state.isFormValid) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                // 先加载原记录，在此基础上修改
                val original = transactionRepository.getTransactionById(editingId)
                if (original != null) {
                    val updated = original.copy(
                        flowType = state.selectedFlowType.name,
                        categoryL1Id = state.categoryL1?.id,
                        categoryL2Id = state.categoryL2?.id,
                        categoryL1 = state.categoryL1!!.name,
                        categoryL2 = state.categoryL2?.name,
                        amount = state.amountDecimal!!,
                        note = state.note.ifBlank { null },
                        date = state.date,
                        recurringTemplateId = null  // 编辑后解除模板关联
                    )
                    transactionRepository.updateTransaction(updated)
                    _uiState.update { it.copy(isSaving = false, shouldClose = true) }
                } else {
                    _uiState.update { it.copy(isSaving = false, errorMessage = "记录不存在") }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = e.message ?: "保存失败")
                }
            }
        }
    }

    // ========== 内部 ==========

    private fun loadTopLevelCategories(flowSign: Int) {
        viewModelScope.launch {
            // 确保种子数据已初始化（首次安装时数据库为空）
            databaseInitializer.initializeIfNeeded()
            val categories = categoryRepository.getTopLevelCategoriesBySignOnce(flowSign)
                .filter { !it.isHidden }
            _uiState.update { it.copy(topLevelCategories = categories) }
        }
    }
}
