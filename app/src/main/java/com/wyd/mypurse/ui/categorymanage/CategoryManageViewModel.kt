package com.wyd.mypurse.ui.categorymanage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.ui.graphics.Color
import com.wyd.mypurse.data.local.database.DatabaseInitializer
import com.wyd.mypurse.data.repository.CategoryDefaults
import com.wyd.mypurse.domain.model.Category
import com.wyd.mypurse.domain.repository.CategoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 分类管理页 ViewModel。管理分类的增删改查、恢复默认。
 */
@HiltViewModel
class CategoryManageViewModel @Inject constructor(
    private val categoryRepository: CategoryRepository,
    private val databaseInitializer: DatabaseInitializer
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryManageUiState())
    val uiState: StateFlow<CategoryManageUiState> = _uiState.asStateFlow()

    /** 一次性事件（如导航、Toast） */
    private val _event = MutableSharedFlow<CategoryManageEvent>()
    val event: SharedFlow<CategoryManageEvent> = _event.asSharedFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            databaseInitializer.initializeIfNeeded()
            categoryRepository.getTopLevelCategories().combine(
                categoryRepository.getAllCategories()
            ) { topLevel, all ->
                val subMap = mutableMapOf<Long, List<Category>>()
                for (parent in topLevel) {
                    subMap[parent.id] = all.filter { it.parentId == parent.id }
                        .sortedBy { it.sortOrder }
                }
                val expense = topLevel.filter { it.flowSign == -1 }
                val income = topLevel.filter { it.flowSign == 1 }
                Triple(expense, income, subMap)
            }.collect { (expense, income, subMap) ->
                val current = _uiState.value
                _uiState.value = current.copy(
                    expenseCategories = expense,
                    incomeCategories = income,
                    subCategories = subMap,
                    isLoading = false
                )
            }
        }
    }

    fun onSelectTab(tab: Int) {
        _uiState.value = _uiState.value.copy(
            selectedTab = tab,
            expandedParentId = null
        )
    }

    fun onToggleExpand(parentId: Long) {
        val current = _uiState.value.expandedParentId
        _uiState.value = _uiState.value.copy(
            expandedParentId = if (current == parentId) null else parentId
        )
    }

    fun onShowAddDialog(parentId: Long?) {
        _uiState.value = _uiState.value.copy(
            editDialog = EditDialogState(parentId = parentId)
        )
    }

    fun onShowEditDialog(category: Category) {
        _uiState.value = _uiState.value.copy(
            editDialog = EditDialogState(category = category)
        )
    }

    fun onDismissEditDialog() {
        _uiState.value = _uiState.value.copy(editDialog = null)
    }

    fun onSaveCategory(name: String, parentId: Long?) {
        viewModelScope.launch {
            val existingDialog = _uiState.value.editDialog ?: return@launch
            if (existingDialog.category != null) {
                // 编辑模式
                categoryRepository.updateCategory(
                    id = existingDialog.category.id,
                    name = name
                )
            } else {
                // 新增模式
                val flowSign = _uiState.value.selectedTab
                val exists = categoryRepository.isCategoryExists(name, parentId, flowSign)
                if (exists) {
                    _uiState.value = _uiState.value.copy(
                        error = "分类「$name」已存在"
                    )
                    return@launch
                }
                categoryRepository.addCategory(
                    name = name,
                    parentId = parentId,
                    isDefault = false,
                    flowSign = _uiState.value.selectedTab
                )
            }
            _uiState.value = _uiState.value.copy(editDialog = null, error = null)
        }
    }

    fun onShowDeleteDialog(category: Category) {
        val hasChildren = _uiState.value.subCategories[category.id]?.isNotEmpty() == true
        _uiState.value = _uiState.value.copy(
            deleteDialog = DeleteDialogState(category = category, hasChildren = hasChildren)
        )
    }

    fun onDismissDeleteDialog() {
        _uiState.value = _uiState.value.copy(deleteDialog = null)
    }

    /**
     * 删除分类选项：删除分类及所有关联记录
     */
    fun onDeleteWithRecords() {
        viewModelScope.launch {
            val dialog = _uiState.value.deleteDialog ?: return@launch
            categoryRepository.deleteCategoryWithRecords(dialog.category.id)
            _uiState.value = _uiState.value.copy(deleteDialog = null)
            _event.emit(CategoryManageEvent.ShowToast("「${dialog.category.name}」及关联记录已删除"))
        }
    }

    // ========== V1.2 合并分类迁移 ==========

    /** 进入合并目标选择步骤 */
    fun onMergeStart() {
        val dialog = _uiState.value.deleteDialog ?: return
        val sourceId = dialog.category.id
        val isTopLevel = dialog.category.parentId == null

        val candidates = if (isTopLevel) {
            // 一级分类候选：同 flowSign 的其他一级分类（排除自身）
            _uiState.value.currentTabCategories.filter { it.id != sourceId }
        } else {
            // 二级分类候选：同父级下的其他二级分类（排除自身）
            val parentId = dialog.category.parentId ?: return
            (_uiState.value.subCategories[parentId] ?: emptyList()).filter { it.id != sourceId }
        }

        _uiState.value = _uiState.value.copy(
            deleteDialog = dialog.copy(mergeStep = 1, mergeTargetCandidates = candidates)
        )
    }

    /** 退回合并选项步骤 */
    fun onMergeBack() {
        val dialog = _uiState.value.deleteDialog ?: return
        _uiState.value = _uiState.value.copy(
            deleteDialog = dialog.copy(mergeStep = 0, mergeTargetCandidates = emptyList())
        )
    }

    /** 确认合并到目标分类 */
    fun onMergeConfirm(targetId: Long) {
        viewModelScope.launch {
            val dialog = _uiState.value.deleteDialog ?: return@launch
            val target = _uiState.value.deleteDialog?.mergeTargetCandidates?.find { it.id == targetId }
            try {
                categoryRepository.mergeCategoryAndDelete(dialog.category.id, targetId)
                _uiState.value = _uiState.value.copy(deleteDialog = null)
                _event.emit(
                    CategoryManageEvent.ShowToast(
                        "已合并到「${target?.name ?: "目标分类"}」"
                    )
                )
            } catch (e: Exception) {
                _event.emit(CategoryManageEvent.ShowToast("合并失败：${e.message}"))
            }
        }
    }

    /**
     * 恢复默认分类
     */
    fun onRestoreDefaults() {
        viewModelScope.launch {
            categoryRepository.restoreDefaultCategories()
            _event.emit(CategoryManageEvent.ShowToast("默认分类已恢复"))
        }
    }

    // ========== V1.1 颜色选择器 ==========

    /** 显示颜色选择器 */
    fun onShowColorPicker(category: Category) {
        _uiState.value = _uiState.value.copy(colorPickerCategory = category)
    }

    /** 关闭颜色选择器 */
    fun onDismissColorPicker() {
        _uiState.value = _uiState.value.copy(colorPickerCategory = null)
    }

    /** 确认颜色选择 */
    fun onColorSelected(color: Color) {
        val category = _uiState.value.colorPickerCategory ?: return
        viewModelScope.launch {
            val argb: Long = (
                ((color.alpha * 255).toInt().toLong() shl 24) or
                ((color.red * 255).toInt().toLong() shl 16) or
                ((color.green * 255).toInt().toLong() shl 8) or
                (color.blue * 255).toInt().toLong()
            )
            categoryRepository.updateCategoryColor(category.id, argb)
            _uiState.value = _uiState.value.copy(colorPickerCategory = null)
            _event.emit(CategoryManageEvent.ShowToast("「${category.name}」颜色已更新"))
        }
    }

    /** 恢复内置分类的默认颜色 */
    fun onRestoreDefaultColor() {
        val category = _uiState.value.colorPickerCategory ?: return
        if (!category.isDefault) return
        val defaultColor = CategoryDefaults.getDefaultColor(category.name, category.flowSign) ?: return
        viewModelScope.launch {
            categoryRepository.updateCategoryColor(category.id, defaultColor)
            _uiState.value = _uiState.value.copy(colorPickerCategory = null)
            _event.emit(CategoryManageEvent.ShowToast("「${category.name}」已恢复默认颜色"))
        }
    }

    fun onClearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ========== 批量操作 ==========

    /** 切换批量操作模式 */
    fun onToggleBatchMode() {
        val current = _uiState.value
        _uiState.value = current.copy(
            isBatchMode = !current.isBatchMode,
            selectedCategoryIds = emptySet(),
            expandedParentId = null // 退出展开状态，避免视觉干扰
        )
    }

    /** 切换某个一级分类的选中状态 */
    fun onToggleSelect(categoryId: Long) {
        val current = _uiState.value
        val newSet = if (categoryId in current.selectedCategoryIds) {
            current.selectedCategoryIds - categoryId
        } else {
            current.selectedCategoryIds + categoryId
        }
        _uiState.value = current.copy(selectedCategoryIds = newSet)
    }

    /** 全选/取消全选当前 Tab 下的一级分类 */
    fun onToggleSelectAll() {
        val current = _uiState.value
        val allIds = current.currentTabCategories.map { it.id }.toSet()
        val newSet = if (current.selectedCategoryIds.containsAll(allIds)) {
            emptySet()
        } else {
            allIds
        }
        _uiState.value = current.copy(selectedCategoryIds = newSet)
    }

    /** 显示批量删除确认弹窗 */
    fun onShowBatchDeleteDialog() {
        val current = _uiState.value
        if (current.selectedCategoryIds.isEmpty()) return
        _uiState.value = current.copy(
            batchDeleteDialog = BatchDeleteDialogState(selectedCount = current.selectedCategoryIds.size)
        )
    }

    /** 关闭批量删除确认弹窗 */
    fun onDismissBatchDeleteDialog() {
        _uiState.value = _uiState.value.copy(batchDeleteDialog = null)
    }

    /** 批量删除 — 删除记录 */
    fun onBatchDeleteWithRecords() {
        viewModelScope.launch {
            val ids = _uiState.value.selectedCategoryIds.toList()
            for (id in ids) {
                categoryRepository.deleteCategoryWithRecords(id)
            }
            _uiState.value = _uiState.value.copy(
                batchDeleteDialog = null,
                selectedCategoryIds = emptySet(),
                isBatchMode = false
            )
            _event.emit(CategoryManageEvent.ShowToast("已删除 ${ids.size} 个分类及关联记录"))
        }
    }

    /**
     * 拖拽排序：交换当前 Tab 下 fromIndex 和 toIndex 两个一级分类的 sortOrder 并持久化。
     * 仅在 onDragEnd 时调用一次，拖拽过程中不触发数据变更。
     */
    fun onMoveCategory(fromIndex: Int, toIndex: Int) {
        val state = _uiState.value
        val current = state.currentTabCategories.toMutableList()
        if (fromIndex < 0 || fromIndex >= current.size) return
        if (toIndex < 0 || toIndex >= current.size) return
        if (fromIndex == toIndex) return

        val moved = current.removeAt(fromIndex)
        current.add(toIndex, moved)

        // 乐观更新 UI
        _uiState.value = if (state.selectedTab == -1) {
            state.copy(expenseCategories = current)
        } else {
            state.copy(incomeCategories = current)
        }

        // 异步批量持久化（一次事务）
        viewModelScope.launch {
            val sortMap = current.mapIndexed { index, category ->
                category.id to index
            }.toMap()
            categoryRepository.batchUpdateSortOrder(sortMap)
        }
    }
}

sealed class CategoryManageEvent {
    data class ShowToast(val message: String) : CategoryManageEvent()
}
