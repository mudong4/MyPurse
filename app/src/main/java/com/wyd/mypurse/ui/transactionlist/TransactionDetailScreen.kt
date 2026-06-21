package com.wyd.mypurse.ui.transactionlist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.wyd.mypurse.ui.theme.AppEditWarningBg
import com.wyd.mypurse.ui.theme.AppEditWarningText
import com.wyd.mypurse.ui.theme.AppExpenseRed
import com.wyd.mypurse.ui.theme.AppIncomeGreen
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDetailScreen(
    transactionId: Long,
    onNavigateBack: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        viewModel.loadTransaction(transactionId)
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar("保存成功")
            viewModel.clearSaveSuccess()
            onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("编辑记录") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isSaving && uiState.editAmountError == null
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = "保存")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.transaction == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("记录不存在", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                EditModeContentFull(
                    editState = uiState,
                    onAmountChange = { viewModel.updateEditAmount(it) },
                    onNoteChange = { viewModel.updateEditNote(it) },
                    onFlowTypeChange = { viewModel.updateEditFlowType(it) },
                    onDateClick = { showDatePicker = true },
                    isSaving = uiState.isSaving,
                    onSave = { viewModel.save() },
                    onCancel = onNavigateBack
                )
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = if (uiState.editDate > 0) uiState.editDate else System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis -> viewModel.updateEditDate(millis) }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ========== 完整编辑模式 ==========

@Composable
private fun EditModeContentFull(
    editState: TransactionDetailUiState,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onFlowTypeChange: (String) -> Unit,
    onDateClick: () -> Unit,
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val dateFormat = remember { java.text.SimpleDateFormat("yyyy年MM月dd日", java.util.Locale.CHINA) }
    val displayDate = if (editState.editDate > 0) dateFormat.format(java.util.Date(editState.editDate)) else "选择日期"

    // 类型切换
    FlowTypeSelector(currentType = editState.editFlowType, onSelect = onFlowTypeChange)

    Spacer(modifier = Modifier.height(16.dp))

    // 分类显示（只读展示，后续可扩展为可点击修改）
    EditFieldLabel(label = "分类")
    Text(
        text = buildString {
            append(editState.editCategoryL1)
            if (!editState.editCategoryL2.isNullOrBlank()) {
                append(" · ")
                append(editState.editCategoryL2)
            }
        }.ifBlank { "未分类" },
        style = MaterialTheme.typography.bodyLarge
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Spacer(modifier = Modifier.height(12.dp))

    // 日期选择
    EditFieldLabel(label = "日期")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDateClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = displayDate, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.weight(1f))
        Icon(Icons.Filled.DateRange, contentDescription = "选择日期", tint = MaterialTheme.colorScheme.primary)
    }
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    Spacer(modifier = Modifier.height(12.dp))

    // 金额输入
    EditFieldLabel(label = "金额")
    OutlinedTextField(
        value = editState.editAmount,
        onValueChange = onAmountChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = editState.editAmountError != null,
        supportingText = editState.editAmountError?.let { { Text(it, color = AppExpenseRed) } },
        prefix = { Text(if (editState.editFlowType == "支出") "-" else "+") }
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 备注
    EditFieldLabel(label = "备注")
    OutlinedTextField(
        value = editState.editNote,
        onValueChange = onNoteChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        placeholder = { Text("添加备注（可选）") }
    )

    // 固定模板提示
    if (editState.isRecurring) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = AppEditWarningBg)) {
            Text(
                text = "⚠ 编辑后该记录将与固定模板解除关联",
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
                color = AppEditWarningText
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Button(
        onClick = onSave,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isSaving && editState.editAmountError == null
    ) {
        if (isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
        Text("保存修改")
    }

    Spacer(modifier = Modifier.height(12.dp))
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        TextButton(onClick = onCancel) { Text("取消", color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun FlowTypeSelector(currentType: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FlowTypeChip(
            label = "支出",
            selected = currentType == "支出",
            color = AppExpenseRed,
            onClick = { onSelect("支出") },
            modifier = Modifier.weight(1f)
        )
        FlowTypeChip(
            label = "收入",
            selected = currentType != "支出",
            color = AppIncomeGreen,
            onClick = { onSelect("收入") },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FlowTypeChip(
    label: String, selected: Boolean, color: Color, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, if (selected) color else Color.Transparent)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label, style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EditFieldLabel(label: String) {
    Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(modifier = Modifier.height(4.dp))
}
