package com.wyd.mypurse.ui.addtransaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyd.mypurse.domain.model.Category
import com.wyd.mypurse.domain.repository.FlowType
import com.wyd.mypurse.ui.components.ChineseDatePickerDialog
import com.wyd.mypurse.ui.components.EmptyStateText
import com.wyd.mypurse.ui.components.rememberDebounce
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val decimalFormat = DecimalFormat("#,###.##")
private val dateFormat = SimpleDateFormat("yyyy年M月d日 EEEE", Locale.CHINESE)

/**
 * 记一笔页 Screen。
 * 新建模式：金额输入、流水类型、分类选择、日期、备注、保存（支持连续记账）。
 * 编辑模式（transactionId > 0）：预填已有记录，保存走 update，不显示"保存并继续"。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionScreen(
    defaultFlowType: String,
    defaultDate: Long,
    transactionId: Long = 0,
    onNavigateBack: () -> Unit,
    onNavigateToCategoryManage: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val debounce = rememberDebounce()
    var showDatePicker by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditMode = transactionId > 0

    // 编辑模式：加载已有记录
    LaunchedEffect(transactionId) {
        if (transactionId > 0) {
            viewModel.loadForEdit(transactionId)
        } else {
            viewModel.initialize(defaultFlowType, defaultDate)
        }
    }

    // 保存并关闭 → 返回上一页
    LaunchedEffect(uiState.shouldClose) {
        if (uiState.shouldClose) {
            viewModel.clearShouldClose()
            onNavigateBack()
        }
    }

    // 连续记账保存成功 → 轻提示（仅新建模式）
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            viewModel.clearSaveSuccess()
            snackbarHostState.showSnackbar("已保存，可继续记账")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "编辑" else "") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!isEditMode) {
                        // 新建模式：顶部"保存并关闭"
                        TextButton(
                            onClick = { debounce { viewModel.saveAndClose() } },
                            enabled = uiState.isFormValid && !uiState.isSaving
                        ) {
                            Text("保存并关闭", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoadingEditData) {
            // 编辑模式加载中
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // 1. 金额输入区
                AmountInputSection(
                    amount = uiState.amount,
                    onDigitClick = { viewModel.appendDigit(it) },
                    onDelete = { viewModel.deleteDigit() },
                    onDecimalPoint = { viewModel.addDecimalPoint() }
                )

                // 2. 流水类型选择（支出 / 收入）
                FlowTypeSelector(
                    selectedType = uiState.selectedFlowType,
                    onTypeSelected = { viewModel.onFlowTypeSelected(it) }
                )

                // 3. 分类选择
                CategorySelector(
                    categoryL1 = uiState.categoryL1,
                    categoryL2 = uiState.categoryL2,
                    onClick = { viewModel.showCategoryPicker() }
                )

                // 4. 日期选择
                DateSelector(
                    date = uiState.date,
                    onClick = { showDatePicker = true }
                )

                // 5. 备注
                NoteInput(
                    note = uiState.note,
                    onNoteChange = { viewModel.onNoteChange(it) }
                )

                // 6. 固定模板提示（编辑模式）
                if (isEditMode && uiState.isRecurring) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = com.wyd.mypurse.ui.theme.AppEditWarningBg)
                    ) {
                        Text(
                            text = "⚠ 编辑后该记录将与固定模板解除关联",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = com.wyd.mypurse.ui.theme.AppEditWarningText
                        )
                    }
                }

                // 7. 保存按钮
                Button(
                    onClick = {
                        debounce {
                            if (isEditMode) viewModel.updateExisting()
                            else viewModel.saveAndContinue()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = uiState.isFormValid && !uiState.isSaving,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存", fontSize = 18.sp)
                }

                // 错误提示
                uiState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // 分类选择器 Bottom Sheet 风格
    if (uiState.isCategoryPickerVisible) {
        CategoryPickerDialog(
            topLevelCategories = uiState.topLevelCategories,
            selectedL1 = uiState.selectedPickerL1,
            subCategories = uiState.subCategories,
            onL1Selected = { viewModel.onCategoryL1Selected(it) },
            onBackToL1 = { viewModel.onCategoryBackToL1() },
            onL2Selected = { viewModel.onCategoryL2Selected(it) },
            onSkipL2 = { viewModel.onCategorySkipL2() },
            onDismiss = { viewModel.dismissCategoryPicker() },
            onManageCategories = {
                viewModel.dismissCategoryPicker()
                onNavigateToCategoryManage()
            }
        )
    }

    // 日期选择器（不限年份，不标"今天"）
    if (showDatePicker) {
        val cal = remember(uiState.date) {
            java.util.Calendar.getInstance().apply { timeInMillis = uiState.date }
        }
        val initialYear = cal.get(java.util.Calendar.YEAR)
        val initialMonth = cal.get(java.util.Calendar.MONTH) + 1
        val initialDay = cal.get(java.util.Calendar.DAY_OF_MONTH)

        ChineseDatePickerDialog(
            initialYear = initialYear,
            initialMonth = initialMonth,
            initialDay = initialDay,
            yearList = null,  // 不限制年份
            todayYear = null,  // 不标今天
            onSelected = { year, month, day ->
                val timestamp = java.util.Calendar.getInstance().apply {
                    set(year, month - 1, day, 12, 0, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }.timeInMillis
                viewModel.onDateChanged(timestamp)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
}

// ========== 1. 金额输入区 ==========

@Composable
private fun AmountInputSection(
    amount: String,
    onDigitClick: (String) -> Unit,
    onDelete: () -> Unit,
    onDecimalPoint: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "¥",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = amount.ifEmpty { "0.00" },
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 40.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    // 自定义数字键盘
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val keys = listOf(
            listOf("1", "2", "3"),
            listOf("4", "5", "6"),
            listOf("7", "8", "9"),
            listOf(".", "0", "⌫")
        )
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                when (key) {
                                    "⌫" -> onDelete()
                                    "." -> onDecimalPoint()
                                    else -> onDigitClick(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "⌫") {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "删除",
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========== 2. 流水类型选择 ==========

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowTypeSelector(
    selectedType: FlowType,
    onTypeSelected: (FlowType) -> Unit
) {
    val types = listOf(FlowType.EXPENSE, FlowType.INCOME)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        types.forEach { flowType ->
            val isSelected = flowType.name == selectedType.name
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .clickable { onTypeSelected(flowType) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = flowType.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

// ========== 3. 分类选择 ==========

@Composable
private fun CategorySelector(
    categoryL1: Category?,
    categoryL2: Category?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (categoryL1 != null) {
                    if (categoryL2 != null) "${categoryL1.name} · ${categoryL2.name}"
                    else categoryL1.name
                } else "选择分类",
                style = MaterialTheme.typography.bodyLarge,
                color = if (categoryL1 != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// ========== 4. 日期选择 ==========

@Composable
private fun DateSelector(
    date: Long,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = dateFormat.format(Date(date)),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

// ========== 5. 备注输入 ==========

@Composable
private fun NoteInput(
    note: String,
    onNoteChange: (String) -> Unit
) {
    OutlinedTextField(
        value = note,
        onValueChange = onNoteChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text("添加备注（选填）") },
        singleLine = true,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        )
    )
}

// ========== 分类选择器弹窗 ==========

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPickerDialog(
    topLevelCategories: List<Category>,
    selectedL1: Category?,
    subCategories: List<Category>,
    onL1Selected: (Category) -> Unit,
    onBackToL1: () -> Unit,
    onL2Selected: (Category) -> Unit,
    onSkipL2: () -> Unit,
    onDismiss: () -> Unit,
    onManageCategories: () -> Unit
) {
    // 全屏遮罩式选择器
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(onClick = onDismiss)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // 标题行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedL1 == null) "选择一级分类" else "选择二级分类",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Row {
                        TextButton(onClick = onManageCategories) {
                            Text("管理")
                        }
                        TextButton(onClick = onDismiss) {
                            Text("取消")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (selectedL1 == null) {
                    // 一级分类网格
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        topLevelCategories.forEach { category ->
                            CategoryChip(
                                name = category.name,
                                onClick = { onL1Selected(category) }
                            )
                        }
                    }
                    if (topLevelCategories.isEmpty()) {
                        EmptyStateText(
                            message = "暂无分类，请先添加",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else {
                    // 返回一级按钮
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onBackToL1() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("← ${selectedL1.name}")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 二级分类网格 + 跳过
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // "跳过细项" 按钮
                        CategoryChip(
                            name = "仅记录到「${selectedL1.name}」",
                            isSkip = true,
                            onClick = onSkipL2
                        )
                        subCategories.forEach { sub ->
                            CategoryChip(
                                name = sub.name,
                                onClick = { onL2Selected(sub) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun CategoryChip(
    name: String,
    isSkip: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (isSkip) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSkip) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
