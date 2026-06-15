package com.wyd.mypurse.ui.categorymanage

import com.wyd.mypurse.domain.model.Category

/**
 * 分类管理页 UI 状态。
 * 按流水类型（支出/收入）分 Tab 展示。
 */
data class CategoryManageUiState(
    /** 当前选中的 Tab：-1=支出，1=收入 */
    val selectedTab: Int = -1,
    /** 支出侧一级分类 */
    val expenseCategories: List<Category> = emptyList(),
    /** 收入侧一级分类 */
    val incomeCategories: List<Category> = emptyList(),
    /** 所有二级分类，key = 一级分类 ID */
    val subCategories: Map<Long, List<Category>> = emptyMap(),
    /** 当前展开的一级分类 ID */
    val expandedParentId: Long? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** 删除确认弹窗状态 */
    val deleteDialog: DeleteDialogState? = null,
    /** 新增/编辑弹窗 */
    val editDialog: EditDialogState? = null
) {
    /** 当前 Tab 下的一级分类列表 */
    val currentTabCategories: List<Category>
        get() = if (selectedTab == -1) expenseCategories else incomeCategories
}

data class DeleteDialogState(
    val category: Category,
    val hasChildren: Boolean = false
)

data class EditDialogState(
    val category: Category? = null, // null 表示新增
    val parentId: Long? = null
)
