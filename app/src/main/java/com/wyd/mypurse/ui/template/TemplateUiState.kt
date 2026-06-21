package com.wyd.mypurse.ui.template

import com.wyd.mypurse.domain.model.Category
import com.wyd.mypurse.domain.model.RecurringTemplate
import java.math.BigDecimal

/**
 * 模板列表页 UI 状态。
 */
data class TemplateListUiState(
    val templates: List<TemplateDisplayItem> = emptyList(),
    val isEmpty: Boolean = true
)

/**
 * 模板列表展示项（含解析后的分类名称和周期描述）。
 */
data class TemplateDisplayItem(
    val template: RecurringTemplate,
    val categoryName: String,
    val subCategoryName: String?,
    val cycleLabel: String,
    val nextExecLabel: String,
    val isActive: Boolean
)

/**
 * 模板编辑页 UI 状态（新建/编辑共用）。
 */
data class TemplateEditUiState(
    val isEditMode: Boolean = false,
    val templateId: Long? = null,
    val flowType: FlowTypeOption = FlowTypeOption.EXPENSE,
    val categoryL1: Category? = null,
    val categoryL2: Category? = null,
    val amount: String = "",
    val cycleType: CycleTypeOption = CycleTypeOption.MONTHLY,
    val cycleDayOfWeek: Int = 1,  // 1=周一 ~ 7=周日（WEEKLY 专用）
    val cycleDayOfMonth: Int = 1, // 1-31（MONTHLY 专用）
    val cycleMonthDay: String = "0101", // MMDD（YEARLY 专用）
    val note: String = "",
    val showCategoryPicker: Boolean = false,
    val showSubCategoryPicker: Boolean = false,
    val isSaving: Boolean = false,
    val saveComplete: Boolean = false,
    val errorMessage: String? = null,
    val showDeleteDialog: Boolean = false,
    val isDeleting: Boolean = false,
    val deleteComplete: Boolean = false,
    // 分类数据
    val availableCategories: List<Category> = emptyList(),
    val availableSubCategories: List<Category> = emptyList()
)

/** 流水类型选项 */
enum class FlowTypeOption(val label: String, val sign: Int) {
    EXPENSE("支出", -1), INCOME("收入", 1)
}

/** 周期类型选项 */
enum class CycleTypeOption(val label: String, val dbValue: String) {
    DAILY("每天", "DAILY"),
    WEEKLY("每周", "WEEKLY"),
    MONTHLY("每月", "MONTHLY"),
    YEARLY("每年", "YEARLY")
}

/** 星期选项（1=周一 ~ 7=周日） */
val WEEKDAY_OPTIONS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
