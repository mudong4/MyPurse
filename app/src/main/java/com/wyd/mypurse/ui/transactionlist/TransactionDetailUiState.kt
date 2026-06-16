package com.wyd.mypurse.ui.transactionlist

import com.wyd.mypurse.domain.model.Transaction

data class TransactionDetailUiState(
    val isLoading: Boolean = true,
    val transaction: Transaction? = null,
    val isEditing: Boolean = false,      // 进入页面时是否直接进入编辑模式
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,

    // 编辑字段
    val editAmount: String = "",
    val editAmountError: String? = null,
    val editNote: String = "",
    val editCategoryL1: String = "",      // 可编辑的分类
    val editCategoryL2: String? = null,
    val editDate: Long = 0L,              // 可编辑的日期
    val editFlowType: String = "支出",     // 可编辑的类型
    val isRecurring: Boolean = false,

    // 分类选择器
    val showCategoryPicker: Boolean = false,

    val errorMessage: String? = null
)
