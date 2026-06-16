package com.wyd.mypurse.ui.categorymanage

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyd.mypurse.domain.model.Category
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
            TopAppBar(
                title = { Text("分类管理") },
                actions = {
                    TextButton(onClick = { viewModel.onRestoreDefaults() }) {
                        Text("恢复默认")
                    }
                }
            )
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

            // 拖拽排序提示
            if (categories.isNotEmpty()) {
                Text(
                    text = "长按拖拽分类可调整排序",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // 分类列表（支持拖拽排序）
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
                            onToggleExpand = { viewModel.onToggleExpand(category.id) },
                            onEdit = { viewModel.onShowEditDialog(category) },
                            onDelete = { viewModel.onShowDeleteDialog(category) },
                            onEditSub = { sub -> viewModel.onShowEditDialog(sub) },
                            onDeleteSub = { sub -> viewModel.onShowDeleteDialog(sub) },
                            onAddSub = { viewModel.onShowAddDialog(category.id) },
                            dragModifier = Modifier.pointerInput(category.id) {
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
                            }
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
}

@Composable
private fun CategoryItem(
    category: Category,
    isExpanded: Boolean,
    subCategories: List<Category>,
    isDragging: Boolean = false,
    onToggleExpand: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onEditSub: (Category) -> Unit = {},
    onDeleteSub: (Category) -> Unit = {},
    onAddSub: () -> Unit,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // 拖拽手柄
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "长按拖拽排序",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    // 点击名称区域展开/折叠
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onToggleExpand() }
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
                Row {
                    TextButton(onClick = onEdit) { Text("编辑") }
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

            // 二级分类列表
            if (isExpanded) {
                HorizontalDivider()
                subCategories.forEach { sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sub.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Row {
                            TextButton(onClick = { onEditSub(sub) }) { Text("编辑") }
                            TextButton(
                                onClick = { onDeleteSub(sub) },
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("删除") }
                        }
                    }
                }
                // 新增二级分类
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

/**
 * 新增/编辑分类弹窗。
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
