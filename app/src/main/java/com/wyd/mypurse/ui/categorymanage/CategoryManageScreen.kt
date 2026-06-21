package com.wyd.mypurse.ui.categorymanage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyd.mypurse.domain.model.Category
import com.wyd.mypurse.ui.components.ColorPickerSheet
import com.wyd.mypurse.ui.components.EmptyStateView
import com.wyd.mypurse.ui.theme.categoryColor
import kotlin.math.roundToInt

/**
 * 分类管理页完整 UI。
 * 功能：一级/二级列表、展开折叠、新增/编辑分类、删除三选项弹窗、恢复默认。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryManageScreen(
    onNavigateBack: () -> Unit,
    viewModel: CategoryManageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when (event) {
                is CategoryManageEvent.ShowToast -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            if (uiState.isBatchMode) {
                TopAppBar(
                    title = { Text("已选 ${uiState.selectedCategoryIds.size} 项") },
                    navigationIcon = {
                        TextButton(onClick = { viewModel.onToggleBatchMode() }) {
                            Text("取消")
                        }
                    },
                    actions = {
                        TextButton(onClick = { viewModel.onToggleSelectAll() }) {
                            val allSelected = uiState.currentTabCategories.isNotEmpty() &&
                                    uiState.selectedCategoryIds.containsAll(uiState.currentTabCategories.map { it.id })
                            Text(if (allSelected) "取消全选" else "全选")
                        }
                        TextButton(
                            onClick = { viewModel.onShowBatchDeleteDialog() },
                            enabled = uiState.selectedCategoryIds.isNotEmpty()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = if (uiState.selectedCategoryIds.isNotEmpty())
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                "删除(${uiState.selectedCategoryIds.size})",
                                color = if (uiState.selectedCategoryIds.isNotEmpty())
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("分类管理") },
                    actions = {
                        TextButton(onClick = { viewModel.onToggleBatchMode() }) {
                            Text("批量操作")
                        }
                        TextButton(onClick = { viewModel.onRestoreDefaults() }) {
                            Text("恢复默认")
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val listState = rememberLazyListState()
        val categories = uiState.currentTabCategories

        // 拖拽状态
        var draggedItemIndex by remember { mutableIntStateOf(-1) }
        var dragOffsetY by remember { mutableFloatStateOf(0f) }
        val itemHeightPx = with(androidx.compose.ui.platform.LocalDensity.current) { 72.dp.toPx() }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 支出/收入 Tab 切换
            TabRow(selectedTabIndex = if (uiState.selectedTab == -1) 0 else 1) {
                Tab(
                    selected = uiState.selectedTab == -1,
                    onClick = { viewModel.onSelectTab(-1) },
                    text = { Text("支出分类") }
                )
                Tab(
                    selected = uiState.selectedTab == 1,
                    onClick = { viewModel.onSelectTab(1) },
                    text = { Text("收入分类") }
                )
            }

            // 错误提示
            uiState.error?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // 空状态
            if (categories.isEmpty() && !uiState.isLoading) {
                EmptyStateView(
                    message = if (uiState.selectedTab == -1) "暂无支出分类" else "暂无收入分类",
                    actionLabel = "新增分类",
                    onAction = { viewModel.onShowAddDialog(null) }
                )
            } else {
                // 拖拽排序提示
                if (categories.isNotEmpty()) {
                    Text(
                        text = "长按拖拽分类可调整排序",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // 分类列表（支持拖拽排序）
            if (categories.isNotEmpty()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(
                    items = categories,
                    key = { _, category -> category.id }
                ) { index, category ->
                    // 用 rememberUpdatedState 确保手势回调中拿到最新的 index 和 categories
                    val currentIndex by rememberUpdatedState(index)
                    val currentCategories by rememberUpdatedState(categories)

                    val isDragging = draggedItemIndex == index

                    Box(
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            .offset {
                                IntOffset(
                                    0,
                                    if (isDragging) dragOffsetY.roundToInt() else 0
                                )
                            }
                            .graphicsLayer {
                                scaleX = if (isDragging) 1.03f else 1f
                                scaleY = if (isDragging) 1.03f else 1f
                            }
                    ) {
                        CategoryItem(
                            category = category,
                            isExpanded = uiState.expandedParentId == category.id,
                            subCategories = uiState.subCategories[category.id] ?: emptyList(),
                            isDragging = isDragging,
                            isBatchMode = uiState.isBatchMode,
                            isSelected = category.id in uiState.selectedCategoryIds,
                            onToggleExpand = { viewModel.onToggleExpand(category.id) },
                            onEdit = { viewModel.onShowEditDialog(category) },
                            onDelete = { viewModel.onShowDeleteDialog(category) },
                            onChangeColor = { viewModel.onShowColorPicker(category) },
                            onEditSub = { sub -> viewModel.onShowEditDialog(sub) },
                            onDeleteSub = { sub -> viewModel.onShowDeleteDialog(sub) },
                            onChangeColorSub = { sub -> viewModel.onShowColorPicker(sub) },
                            onAddSub = { viewModel.onShowAddDialog(category.id) },
                            onToggleSelect = { viewModel.onToggleSelect(category.id) },
                            dragModifier = if (!uiState.isBatchMode) Modifier.pointerInput(category.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedItemIndex = currentIndex
                                        dragOffsetY = 0f
                                    },
                                    onDragEnd = {
                                        val targetIndex = (currentIndex + (dragOffsetY / itemHeightPx).roundToInt())
                                            .coerceIn(0, currentCategories.size - 1)
                                        if (targetIndex != currentIndex) {
                                            viewModel.onMoveCategory(currentIndex, targetIndex)
                                        }
                                        draggedItemIndex = -1
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggedItemIndex = -1
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, offset ->
                                        change.consume()
                                        dragOffsetY += offset.y
                                    }
                                )
                            } else Modifier
                        )
                    }
                }

                // 新增一级分类按钮
                item {
                    OutlinedButton(
                        onClick = { viewModel.onShowAddDialog(null) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("新增一级分类")
                    }
                }
            }
            } // end if (categories.isNotEmpty())
        }
    }

    // 新增/编辑弹窗
    uiState.editDialog?.let { dialog ->
        CategoryEditDialog(
            category = dialog.category,
            parentId = dialog.parentId,
            onDismiss = { viewModel.onDismissEditDialog() },
            onSave = { name, parentId -> viewModel.onSaveCategory(name, parentId) }
        )
    }

    // 删除确认弹窗
    uiState.deleteDialog?.let { dialog ->
        CategoryDeleteDialog(
            category = dialog.category,
            hasChildren = dialog.hasChildren,
            onDismiss = { viewModel.onDismissDeleteDialog() },
            onDeleteKeepRecords = { viewModel.onDeleteKeepRecords() },
            onDeleteWithRecords = { viewModel.onDeleteWithRecords() }
        )
    }

    // 批量删除确认弹窗
    uiState.batchDeleteDialog?.let { dialog ->
        BatchDeleteDialog(
            count = dialog.selectedCount,
            onDismiss = { viewModel.onDismissBatchDeleteDialog() },
            onDeleteKeepRecords = { viewModel.onBatchDeleteKeepRecords() },
            onDeleteWithRecords = { viewModel.onBatchDeleteWithRecords() }
        )
    }

    // V1.1 颜色选择器
    uiState.colorPickerCategory?.let { category ->
        ColorPickerSheet(
            initialColor = categoryColor(category.color) ?: Color.Unspecified,
            onColorSelected = { viewModel.onColorSelected(it) },
            onDismiss = { viewModel.onDismissColorPicker() },
            onRestoreDefault = if (category.isDefault) {
                { viewModel.onRestoreDefaultColor() }
            } else null
        )
    }
}

@Composable
private fun CategoryItem(
    category: Category,
    isExpanded: Boolean,
    subCategories: List<Category>,
    isDragging: Boolean = false,
    isBatchMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onChangeColor: () -> Unit = {},
    onEditSub: (Category) -> Unit = {},
    onDeleteSub: (Category) -> Unit = {},
    onChangeColorSub: (Category) -> Unit = {},
    onAddSub: () -> Unit,
    onToggleSelect: () -> Unit = {},
    dragModifier: Modifier = Modifier
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .then(dragModifier),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isDragging) 8.dp else 0.dp
        )
    ) {
        Column {
            // 10a-1: 整行（手柄+名称+子项计数）可点击展开/折叠
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = if (isBatchMode) 0.dp else 4.dp,
                        end = 16.dp,
                        top = 12.dp,
                        bottom = 12.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (!isBatchMode) Modifier.clickable { onToggleExpand() } else Modifier)
                ) {
                    if (isBatchMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { onToggleSelect() }
                        )
                    } else {
                        // 拖拽手柄
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "长按拖拽排序",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    // V1.1 颜色指示圆点
                    val catColor = categoryColor(category.color)
                    if (catColor != null) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(catColor)
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = if (!isBatchMode) Modifier else Modifier.clickable { onToggleExpand() }
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = " (${subCategories.size})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (!isBatchMode) {
                    Row {
                        TextButton(onClick = onEdit) { Text("编辑") }
                        TextButton(onClick = onChangeColor) {
                            Text("颜色", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = onDelete,
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("删除") }
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp
                            else Icons.Default.KeyboardArrowDown,
                            contentDescription = "展开/折叠"
                        )
                    }
                }
            }

            // 二级分类列表
            if (isExpanded) {
                HorizontalDivider()
                // 10a-3: 二级区域加浅色背景，与一级区分
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                ) {
                    subCategories.forEach { sub ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 32.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                // V1.1 颜色指示圆点
                                val subColor = categoryColor(sub.color)
                                if (subColor != null) {
                                    Box(
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(subColor)
                                            .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                                    )
                                }
                                Text(
                                    text = sub.name,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            if (!isBatchMode) {
                                Row {
                                    TextButton(onClick = { onEditSub(sub) }) { Text("编辑") }
                                    TextButton(onClick = { onChangeColorSub(sub) }) {
                                        Text("颜色", style = MaterialTheme.typography.labelSmall)
                                    }
                                    TextButton(
                                        onClick = { onDeleteSub(sub) },
                                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        )
                                    ) { Text("删除") }
                                }
                            }
                        }
                    }
                    // 新增二级分类（仅非批量模式）
                    if (!isBatchMode) {
                        TextButton(
                            onClick = onAddSub,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                            Text("新增二级分类")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

/**
 * 新增/编辑分类弹窗。
 * V1.1 新增颜色预览行：新建时提示将自动分配轮转色，编辑时可点击"颜色"按钮在外部修改。
 */
@Composable
private fun CategoryEditDialog(
    category: Category?,
    parentId: Long?,
    onDismiss: () -> Unit,
    onSave: (name: String, parentId: Long?) -> Unit
) {
    var name by remember { mutableStateOf(category?.name ?: "") }
    val isEdit = category != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "编辑分类" else "新增分类") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("分类名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                // V1.1 颜色预览（仅新建时显示，编辑时用行内"颜色"按钮）
                if (!isEdit) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "颜色将自动分配，创建后可修改",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name.trim(), parentId)
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * 删除分类确认弹窗（两选项）。
 * 选项 1：保留记录（仅删除分类定义）
 * 选项 2：删除分类及所有关联记录
 */
@Composable
private fun CategoryDeleteDialog(
    category: Category,
    hasChildren: Boolean,
    onDismiss: () -> Unit,
    onDeleteKeepRecords: () -> Unit,
    onDeleteWithRecords: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除「${category.name}」") },
        text = {
            Column {
                if (hasChildren) {
                    Text(
                        text = "该分类下包含子分类，删除后将一并移除。",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
                Text("请选择删除方式：")
                Spacer(modifier = Modifier.height(12.dp))

                // 选项 1：保留记录
                TextButton(
                    onClick = onDeleteKeepRecords,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("保留历史记录", fontWeight = FontWeight.Medium)
                        Text(
                            text = "仅删除分类，已存在的流水记录保留原分类名称",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 选项 2：删除记录
                TextButton(
                    onClick = onDeleteWithRecords,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            "删除分类及所有关联记录",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "将同时删除该分类下的所有流水记录",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/**
 * 批量删除确认弹窗。
 * 与单个删除结构相同，但提示文字针对批量操作。
 */
@Composable
private fun BatchDeleteDialog(
    count: Int,
    onDismiss: () -> Unit,
    onDeleteKeepRecords: () -> Unit,
    onDeleteWithRecords: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("批量删除 $count 个分类") },
        text = {
            Column {
                Text("选中分类的子分类也将一并删除。", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(12.dp))
                Text("请选择删除方式：")
                Spacer(modifier = Modifier.height(12.dp))

                // 选项 1：保留记录
                TextButton(
                    onClick = onDeleteKeepRecords,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text("保留历史记录", fontWeight = FontWeight.Medium)
                        Text(
                            text = "仅删除分类定义，已存在的流水记录保留原分类名称",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // 选项 2：删除记录
                TextButton(
                    onClick = onDeleteWithRecords,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            "删除分类及所有关联记录",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "将同时删除这些分类下的所有流水记录，不可恢复",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
