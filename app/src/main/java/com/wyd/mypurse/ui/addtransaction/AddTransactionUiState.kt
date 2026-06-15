package com.wyd.mypurse.ui.addtransaction

import com.wyd.mypurse.domain.model.Category
import com.wyd.mypurse.domain.repository.FlowType
import java.math.BigDecimal

/**
 * 记一笔页 UI 状态。
 */
data class AddTransactionUiState(
    val selectedFlowType: FlowType = FlowType.EXPENSE,
    val amount: String = "",
    val categoryL1: Category? = null,
    val categoryL2: Category? = null,
    val topLevelCategories: List<Category> = emptyList(),
    val subCategories: List<Category> = emptyList(),
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val isCategoryPickerVisible: Boolean = false,
    val selectedPickerL1: Category? = null,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null
) {
    /**
     * 当前金额的 BigDecimal 表示，非法输入返回 null。
     */
    val amountDecimal: BigDecimal?
        get() = amount.toBigDecimalOrNull()

    /**
     * 金额是否有效（大于 0）。
     */
    val isAmountValid: Boolean
        get() {
            val dec = amountDecimal
            return dec != null && dec > BigDecimal.ZERO
        }

    /**
     * 表单是否可提交。
     */
    val isFormValid: Boolean
        get() = isAmountValid && categoryL1 != null
}
