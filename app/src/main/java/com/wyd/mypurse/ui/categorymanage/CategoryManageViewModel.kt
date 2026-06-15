package com.wyd.mypurse.ui.categorymanage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wyd.mypurse.data.local.database.DatabaseInitializer
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

    fun onSaveCategory(name: String, parentId: Long?, icon: String) {
        viewModelScope.launch {
            val existingDialog = _uiState.value.editDialog ?: return@launch
            if (existingDialog.category != null) {
                // 编辑模式
                categoryRepository.updateCategory(
                    id = existingDialog.category.id,
                    name = name,
                    icon = icon
                )
            } else {
                // 新增模式
                val exists = categoryRepository.isCategoryExists(name, parentId)
                if (exists) {
                    _uiState.value = _uiState.value.copy(
                        error = "分类「$name」已存在"
                    )
                    return@launch
                }
                categoryRepository.addCategory(
                    name = name,
                    parentId = parentId,
                    icon = icon,
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
     * 删除分类选项 1：直接删除，保留历史记录快照名
     */
    fun onDeleteKeepRecords() {
        viewModelScope.launch {
            val dialog = _uiState.value.deleteDialog ?: return@launch
            categoryRepository.deleteCategory(dialog.category.id)
            _uiState.value = _uiState.value.copy(deleteDialog = null)
        }
    }

    /**
     * 删除分类选项 2：迁移至其他分类
     * @param targetCategory 目标分类
     */
    fun onDeleteMigrateRecords(targetCategory: Category) {
        viewModelScope.launch {
            val dialog = _uiState.value.deleteDialog ?: return@launch
            val isTopLevel = dialog.category.parentId == null
            if (isTopLevel) {
                categoryRepository.migrateCategoryRecords(
                    fromCategoryId = dialog.category.id,
                    toCategoryL1Id = targetCategory.id,
                    toCategoryL2Id = null,
                    toCategoryL1Name = targetCategory.name,
                    toCategoryL2Name = null
                )
            } else {
                val parent = dialog.category.parentId?.let {
                    categoryRepository.getCategoryById(it)
                }
                categoryRepository.migrateCategoryRecords(
                    fromCategoryId = dialog.category.id,
                    toCategoryL1Id = parent?.id,
                    toCategoryL2Id = targetCategory.id,
                    toCategoryL1Name = parent?.name ?: "",
                    toCategoryL2Name = targetCategory.name
                )
            }
            categoryRepository.deleteCategory(dialog.category.id)
            _uiState.value = _uiState.value.copy(deleteDialog = null)
        }
    }

    /**
     * 删除分类选项 3：删除分类及所有关联记录
     */
    fun onDeleteWithRecords() {
        viewModelScope.launch {
            val dialog = _uiState.value.deleteDialog ?: return@launch
            categoryRepository.deleteCategoryWithRecords(dialog.category.id)
            _uiState.value = _uiState.value.copy(deleteDialog = null)
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

    fun onClearError() {
        _uiState.value = _uiState.value.copy(error = null)
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
