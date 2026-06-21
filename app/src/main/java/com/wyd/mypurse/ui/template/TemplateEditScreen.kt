package com.wyd.mypurse.ui.template

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyd.mypurse.domain.model.Category
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditScreen(
    templateId: Long?, // null = 新建模式
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: TemplateViewModel = hiltViewModel()
) {
    val state by viewModel.editState.collectAsStateWithLifecycle()

    LaunchedEffect(templateId) {
        if (templateId != null) {
            viewModel.loadForEdit(templateId)
        } else {
            viewModel.loadForCreate()
        }
    }

    LaunchedEffect(state.saveComplete, state.deleteComplete) {
        if (state.saveComplete || state.deleteComplete) {
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "编辑模板" else "新建模板") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.isEditMode) {
                        IconButton(onClick = { viewModel.showDeleteDialog() }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "删除模板",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ----- 流水类型 -----
            SectionLabel("流水类型", Icons.Filled.SwapHoriz)
            Row(modifier = Modifier.fillMaxWidth()) {
                FlowTypeOption.entries.forEach { option ->
                    val selected = state.flowType == option
                    OutlinedButton(
                        onClick = { viewModel.onFlowTypeChanged(option) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = if (option == FlowTypeOption.EXPENSE) 8.dp else 0.dp),
                        colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
                    ) {
                        Text(option.label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ----- 分类选择 -----
            SectionLabel("分类", Icons.Filled.Category)
            CategorySelectorRow(
                label = state.categoryL1?.name ?: "选择一级分类",
                onClick = { viewModel.showCategoryPicker() }
            )
            if (state.availableSubCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                CategorySelectorRow(
                    label = state.categoryL2?.name ?: "选择二级分类（可选）",
                    onClick = { viewModel.showSubCategoryPicker() }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ----- 金额 -----
            SectionLabel("金额", null)
            OutlinedTextField(
                value = state.amount,
                onValueChange = { viewModel.onAmountChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = { Text("输入金额") },
                suffix = { Text("元") }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // ----- 周期类型 -----
            SectionLabel("周期", Icons.Filled.Repeat)
            CycleTypeSelector(state = state, onChanged = { viewModel.onCycleTypeChanged(it) })
            Spacer(modifier = Modifier.height(12.dp))

            // 周期参数
            when (state.cycleType) {
                CycleTypeOption.DAILY -> { /* 无额外参数 */ }
                CycleTypeOption.WEEKLY -> {
                    WeekdaySelector(
                        selected = state.cycleDayOfWeek - 1,
                        onSelected = { viewModel.onCycleDayOfWeekChanged(it) }
                    )
                }
                CycleTypeOption.MONTHLY -> {
                    OutlinedTextField(
                        value = if (state.cycleDayOfMonth > 0) state.cycleDayOfMonth.toString() else "",
                        onValueChange = {
                            val v = it.filter { c -> c.isDigit() }.take(2)
                            val day = v.toIntOrNull()?.coerceIn(1, 31) ?: 1
                            viewModel.onCycleDayOfMonthChanged(day - 1) // will be +1 in VM
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("每月几号（1-31）") },
                        placeholder = { Text("如 15") }
                    )
                }
                CycleTypeOption.YEARLY -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.cycleMonthDay.take(2),
                            onValueChange = {
                                val digits = it.filter { c -> c.isDigit() }.take(2)
                                val m = digits.toIntOrNull()?.coerceIn(1, 12) ?: 1
                                val newVal = String.format("%02d%02d", m, state.cycleMonthDay.takeLast(2).toIntOrNull() ?: 1)
                                viewModel.onCycleMonthDayChanged(newVal)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("月") }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = state.cycleMonthDay.takeLast(2),
                            onValueChange = {
                                val digits = it.filter { c -> c.isDigit() }.take(2)
                                val d = digits.toIntOrNull()?.coerceIn(1, 31) ?: 1
                                val newVal = String.format("%02d%02d", state.cycleMonthDay.take(2).toIntOrNull() ?: 1, d)
                                viewModel.onCycleMonthDayChanged(newVal)
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            label = { Text("日") }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ----- 备注 -----
            SectionLabel("备注（可选）", null)
            OutlinedTextField(
                value = state.note,
                onValueChange = { viewModel.onNoteChanged(it) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("如：房贷、工资") }
            )

            // ----- 错误提示 -----
            state.errorMessage?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = msg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ----- 保存按钮 -----
            Button(
                onClick = { viewModel.save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isSaving
            ) {
                Text(if (state.isEditMode) "保存修改" else "创建模板")
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 分类选择弹窗
    if (state.showCategoryPicker) {
        CategoryPickerDialog(
            title = "选择一级分类",
            categories = state.availableCategories,
            onSelect = { viewModel.onCategoryL1Selected(it) },
            onDismiss = { viewModel.dismissCategoryPicker() }
        )
    }

    if (state.showSubCategoryPicker) {
        CategoryPickerDialog(
            title = "选择二级分类",
            categories = state.availableSubCategories,
            onSelect = { viewModel.onCategoryL2Selected(it) },
            onDismiss = { viewModel.dismissSubCategoryPicker() },
            showNone = true
        )
    }

    // 删除确认弹窗
    if (state.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("删除模板") },
            text = { Text("删除此模板，已生成的记录不受影响。确定删除？") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("确定删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SectionLabel(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon?.let {
            Icon(it, contentDescription = null, modifier = Modifier.height(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}

@Composable
private fun CategorySelectorRow(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CycleTypeSelector(
    state: TemplateEditUiState,
    onChanged: (CycleTypeOption) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        CycleTypeOption.entries.forEachIndexed { index, option ->
            val selected = state.cycleType == option
            OutlinedButton(
                onClick = { onChanged(option) },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = if (index < CycleTypeOption.entries.lastIndex) 6.dp else 0.dp),
                colors = if (selected) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(option.label, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun WeekdaySelector(selected: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
    ) {
        WEEKDAY_OPTIONS.forEachIndexed { index, label ->
            OutlinedButton(
                onClick = { onSelected(index) },
                modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                colors = if (selected == index) ButtonDefaults.buttonColors() else ButtonDefaults.outlinedButtonColors()
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CategoryPickerDialog(
    title: String,
    categories: List<Category>,
    onSelect: (Category) -> Unit,
    onDismiss: () -> Unit,
    showNone: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                if (showNone) {
                    TextButton(onClick = { onSelect(Category(id = 0, name = "", parentId = null, isDefault = false)) }) {
                        Text("无（不选二级分类）")
                    }
                }
                categories.forEach { cat ->
                    TextButton(onClick = { onSelect(cat) }, modifier = Modifier.fillMaxWidth()) {
                        Text(cat.name)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
