package com.wyd.mypurse.ui.template

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wyd.mypurse.domain.model.Category
import com.wyd.mypurse.domain.model.RecurringTemplate
import com.wyd.mypurse.domain.repository.CategoryRepository
import com.wyd.mypurse.domain.repository.RecurringTemplateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TemplateViewModel @Inject constructor(
    private val templateRepository: RecurringTemplateRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // ========== 列表页 ==========

    private val _listState = MutableStateFlow(TemplateListUiState())
    val listState: StateFlow<TemplateListUiState> = _listState.asStateFlow()

    init {
        viewModelScope.launch {
            templateRepository.getAllTemplates().collect { templates ->
                val displayItems = templates.map { toDisplayItem(it) }
                _listState.update {
                    it.copy(templates = displayItems, isEmpty = displayItems.isEmpty())
                }
            }
        }
    }

    fun toggleTemplateActive(template: RecurringTemplate) {
        viewModelScope.launch {
            templateRepository.updateTemplate(template.copy(isActive = !template.isActive))
        }
    }

    fun requestDeleteTemplate(template: RecurringTemplate) {
        viewModelScope.launch {
            templateRepository.deleteTemplate(template.id)
        }
    }

    // ========== 编辑页 ==========

    private val _editState = MutableStateFlow(TemplateEditUiState())
    val editState: StateFlow<TemplateEditUiState> = _editState.asStateFlow()

    fun loadForCreate() {
        viewModelScope.launch {
            val sign = FlowTypeOption.EXPENSE.sign // 默认支出
            val rootCategories = categoryRepository.getTopLevelCategoriesBySignOnce(sign)
            _editState.update {
                TemplateEditUiState(
                    availableCategories = rootCategories
                )
            }
        }
    }

    fun loadForEdit(templateId: Long) {
        viewModelScope.launch {
            val template = templateRepository.getTemplateById(templateId) ?: return@launch
            val l1 = template.categoryL1Id?.let { categoryRepository.getCategoryById(it) }
            val l2Cats = template.categoryL1Id?.let {
                categoryRepository.getSubCategoriesOnce(it)
            } ?: emptyList()
            val l2 = template.categoryL2Id?.let { categoryRepository.getCategoryById(it) }

            val flowType = when (template.flowType) {
                "支出" -> FlowTypeOption.EXPENSE
                else -> FlowTypeOption.INCOME
            }
            val rootCategories = categoryRepository.getTopLevelCategoriesBySignOnce(flowType.sign)
            val cycleType = when (template.cycleType) {
                "DAILY" -> CycleTypeOption.DAILY
                "WEEKLY" -> CycleTypeOption.WEEKLY
                "MONTHLY" -> CycleTypeOption.MONTHLY
                else -> CycleTypeOption.YEARLY
            }

            _editState.update {
                TemplateEditUiState(
                    isEditMode = true,
                    templateId = template.id,
                    flowType = flowType,
                    categoryL1 = l1,
                    categoryL2 = l2,
                    amount = template.amount.toPlainString(),
                    cycleType = cycleType,
                    cycleDayOfWeek = if (template.cycleType == "WEEKLY") template.cycleValue.toInt() else 1,
                    cycleDayOfMonth = if (template.cycleType == "MONTHLY") template.cycleValue.toInt() else 1,
                    cycleMonthDay = if (template.cycleType == "YEARLY") String.format("%04d", template.cycleValue) else "0101",
                    note = template.note ?: "",
                    availableCategories = rootCategories,
                    availableSubCategories = l2Cats
                )
            }
        }
    }

    fun onFlowTypeChanged(flowType: FlowTypeOption) {
        viewModelScope.launch {
            val rootCategories = categoryRepository.getTopLevelCategoriesBySignOnce(flowType.sign)
            _editState.update {
                it.copy(
                    flowType = flowType,
                    categoryL1 = null,
                    categoryL2 = null,
                    availableCategories = rootCategories,
                    availableSubCategories = emptyList()
                )
            }
        }
    }

    fun onCategoryL1Selected(category: Category) {
        viewModelScope.launch {
            val subs = categoryRepository.getSubCategoriesOnce(category.id)
            _editState.update {
                it.copy(
                    categoryL1 = category,
                    categoryL2 = null,
                    showCategoryPicker = false,
                    availableSubCategories = subs
                )
            }
        }
    }

    fun onCategoryL2Selected(category: Category?) {
        _editState.update { it.copy(categoryL2 = category, showSubCategoryPicker = false) }
    }

    fun onAmountChanged(amount: String) {
        _editState.update { it.copy(amount = amount) }
    }

    fun onCycleTypeChanged(cycleType: CycleTypeOption) {
        _editState.update { it.copy(cycleType = cycleType) }
    }

    fun onCycleDayOfWeekChanged(day: Int) {
        _editState.update { it.copy(cycleDayOfWeek = day + 1) } // 0-based index → 1-based
    }

    fun onCycleDayOfMonthChanged(day: Int) {
        _editState.update { it.copy(cycleDayOfMonth = day + 1) }
    }

    fun onCycleMonthDayChanged(text: String) {
        _editState.update { it.copy(cycleMonthDay = text) }
    }

    fun onNoteChanged(note: String) {
        _editState.update { it.copy(note = note) }
    }

    fun showCategoryPicker() {
        _editState.update { it.copy(showCategoryPicker = true) }
    }

    fun dismissCategoryPicker() {
        _editState.update { it.copy(showCategoryPicker = false) }
    }

    fun showSubCategoryPicker() {
        _editState.update { it.copy(showSubCategoryPicker = true) }
    }

    fun dismissSubCategoryPicker() {
        _editState.update { it.copy(showSubCategoryPicker = false) }
    }

    fun showDeleteDialog() {
        _editState.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _editState.update { it.copy(showDeleteDialog = false) }
    }

    fun confirmDelete() {
        val id = _editState.value.templateId ?: return
        viewModelScope.launch {
            _editState.update { it.copy(isDeleting = true) }
            templateRepository.deleteTemplate(id)
            _editState.update { it.copy(isDeleting = false, deleteComplete = true) }
        }
    }

    fun save() {
        val state = _editState.value
        if (state.isSaving) return

        viewModelScope.launch {
            _editState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                val amount = try {
                    BigDecimal(state.amount).takeIf { it > BigDecimal.ZERO }
                } catch (_: Exception) { null }

                if (amount == null || amount <= BigDecimal.ZERO) {
                    _editState.update { it.copy(isSaving = false, errorMessage = "请输入有效金额") }
                    return@launch
                }
                if (state.categoryL1 == null) {
                    _editState.update { it.copy(isSaving = false, errorMessage = "请选择分类") }
                    return@launch
                }

                val cycleValue = when (state.cycleType) {
                    CycleTypeOption.DAILY -> 0L
                    CycleTypeOption.WEEKLY -> state.cycleDayOfWeek.toLong()
                    CycleTypeOption.MONTHLY -> state.cycleDayOfMonth.toLong()
                    CycleTypeOption.YEARLY -> state.cycleMonthDay.toLong()
                }
                val nextExec = calcNextExecutionDate(
                    System.currentTimeMillis(),
                    state.cycleType.dbValue,
                    cycleValue
                )

                val template = RecurringTemplate(
                    id = state.templateId ?: 0,
                    flowType = state.flowType.label,
                    categoryL1Id = state.categoryL1.id,
                    categoryL2Id = state.categoryL2?.id,
                    amount = amount,
                    note = state.note.takeIf { it.isNotBlank() },
                    cycleType = state.cycleType.dbValue,
                    cycleValue = cycleValue,
                    isActive = true,
                    nextExecutionDate = nextExec
                )

                if (state.isEditMode) {
                    templateRepository.updateTemplate(template)
                } else {
                    templateRepository.insertTemplate(template)
                }

                _editState.update { it.copy(isSaving = false, saveComplete = true) }
            } catch (e: Exception) {
                Log.e("TemplateVM", "保存失败", e)
                _editState.update { it.copy(isSaving = false, errorMessage = "保存失败: ${e.message}") }
            }
        }
    }

    fun dismissError() {
        _editState.update { it.copy(errorMessage = null) }
    }

    // ========== 工具 ==========

    private suspend fun toDisplayItem(template: RecurringTemplate): TemplateDisplayItem {
        val catName = template.categoryL1Id?.let {
            categoryRepository.getCategoryById(it)?.name
        } ?: "未分类"
        val subName = template.categoryL2Id?.let {
            categoryRepository.getCategoryById(it)?.name
        }
        val cycleLabel = cycleTypeLabel(template.cycleType, template.cycleValue)
        val nextLabel = formatNextExec(template.nextExecutionDate)
        return TemplateDisplayItem(
            template = template,
            categoryName = catName,
            subCategoryName = subName,
            cycleLabel = cycleLabel,
            nextExecLabel = nextLabel,
            isActive = template.isActive
        )
    }

    private fun cycleTypeLabel(cycleType: String, cycleValue: Long): String {
        return when (cycleType) {
            "DAILY" -> "每天"
            "WEEKLY" -> {
                val dayNames = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
                val idx = cycleValue.toInt().coerceIn(1, 7) - 1
                "每${dayNames[idx]}"
            }
            "MONTHLY" -> "每月${cycleValue}日"
            "YEARLY" -> {
                val month = (cycleValue / 100).toInt()
                val day = (cycleValue % 100).toInt()
                "${month}月${day}日"
            }
            else -> cycleType
        }
    }

    private fun formatNextExec(timestamp: Long): String {
        val cal = GregorianCalendar().apply { timeInMillis = timestamp }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun calcNextExecutionDate(fromTime: Long, cycleType: String, cycleValue: Long): Long {
        val cal = GregorianCalendar()
        cal.timeInMillis = fromTime
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        return when (cycleType) {
            "DAILY" -> {
                cal.add(Calendar.DAY_OF_MONTH, 1)
                cal.timeInMillis
            }
            "WEEKLY" -> {
                val targetDay = when (cycleValue.toInt()) {
                    7 -> Calendar.SUNDAY
                    else -> cycleValue.toInt() + 1
                }
                cal.add(Calendar.DAY_OF_MONTH, 1)
                while (cal.get(Calendar.DAY_OF_WEEK) != targetDay) {
                    cal.add(Calendar.DAY_OF_MONTH, 1)
                }
                cal.timeInMillis
            }
            "MONTHLY" -> {
                val targetDay = cycleValue.toInt()
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.add(Calendar.MONTH, 1)
                val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, targetDay.coerceAtMost(lastDay))
                cal.timeInMillis
            }
            "YEARLY" -> {
                val month = (cycleValue / 100).toInt()
                val day = (cycleValue % 100).toInt()
                cal.add(Calendar.YEAR, 1)
                cal.set(Calendar.MONTH, month - 1)
                val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                cal.set(Calendar.DAY_OF_MONTH, day.coerceAtMost(lastDay))
                cal.timeInMillis
            }
            else -> cal.timeInMillis
        }
    }
}
