package com.wyd.mypurse.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.math.BigDecimal
import java.text.DecimalFormat

/**
 * 预算设置页。极简方案：一个输入框，设置当月预算。
 * 表中永远只有一条记录，修改即覆盖。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    year: Int?,
    month: Int?,
    onNavigateBack: () -> Unit,
    viewModel: BudgetViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    var showSavedDialog by remember { mutableStateOf(false) }
    // 快捷金额列表（可自定义，默认 3 个）
    var quickAmounts by remember {
        mutableStateOf(listOf("1000", "5000", "10000"))
    }
    var editingQuickIndex by remember { mutableStateOf<Int?>(null) }
    var newQuickValue by remember { mutableStateOf("") }

    // 加载完成后初始化输入框文本
    LaunchedEffect(uiState.amount) {
        val currentAmount = uiState.amount
        if (inputText.isEmpty() && currentAmount != null) {
            inputText = currentAmount.toPlainString()
        }
    }

    // 保存成功弹窗
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            showSavedDialog = true
        }
    }

    if (showSavedDialog) {
        AlertDialog(
            onDismissRequest = { showSavedDialog = false },
            icon = {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("预算已保存") },
            text = {
                Text("月度预算已更新为 ¥${decimalFormat.format(uiState.amount ?: BigDecimal.ZERO)}")
            },
            confirmButton = {
                TextButton(onClick = { showSavedDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("预算设置") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 输入区域
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "预算金额",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "¥",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { newText ->
                                    // 只允许数字和小数点
                                    if (newText.isEmpty() || newText.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                        inputText = newText
                                        val value = newText.toBigDecimalOrNull()
                                        if (value != null && value >= BigDecimal.ZERO) {
                                            viewModel.onAmountChange(value)
                                        }
                                    }
                                },
                                modifier = Modifier.width(200.dp),
                                textStyle = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 36.sp,
                                    textAlign = TextAlign.Center
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholder = {
                                    Text(
                                        text = "0.00",
                                        style = MaterialTheme.typography.headlineLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 36.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                    )
                                },
                                singleLine = true
                            )
                        }
                    }
                }

                // 快捷金额（点击填入，编辑图标修改）
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "快捷设置",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            quickAmounts.forEachIndexed { index, amount ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            inputText = amount
                                            viewModel.onAmountChange(BigDecimal(amount))
                                        },
                                        shape = CircleShape,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (inputText == amount)
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (inputText == amount)
                                                MaterialTheme.colorScheme.onPrimary
                                            else
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Text(text = "¥$amount", style = MaterialTheme.typography.bodyMedium)
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "编辑快捷金额",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .padding(2.dp)
                                            .clickable {
                                                editingQuickIndex = index
                                                newQuickValue = amount
                                            },
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }
                }

                // 编辑快捷金额弹窗
                if (editingQuickIndex != null) {
                    EditQuickAmountDialog(
                        currentValue = newQuickValue,
                        onValueChange = {
                            if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                newQuickValue = it
                            }
                        },
                        onConfirm = {
                            val value = newQuickValue.toBigDecimalOrNull()
                            if (value != null && value > BigDecimal.ZERO) {
                                quickAmounts = quickAmounts.toMutableList().apply {
                                    set(editingQuickIndex!!, value.toPlainString())
                                }
                            }
                            editingQuickIndex = null
                        },
                        onDismiss = { editingQuickIndex = null }
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // 保存按钮
                Button(
                    onClick = {
                        val amount = inputText.toBigDecimalOrNull()
                        if (amount != null && amount > BigDecimal.ZERO) {
                            viewModel.saveBudget()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = inputText.toBigDecimalOrNull()?.let { it > BigDecimal.ZERO } == true
                ) {
                    Text(
                        text = "保存",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 不设预算按钮（仅已有预算时显示）
                if (uiState.amount != null) {
                    TextButton(
                        onClick = { viewModel.clearBudget() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "不设预算",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }
    }
}

private val decimalFormat = DecimalFormat("#,##0.00")

@Composable
private fun EditQuickAmountDialog(
    currentValue: String,
    onValueChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改快捷金额") },
        text = { // AlertDialog 的 text 参数本身就是 @Composable lambda，这里可以安全使用 OutlinedTextField
            OutlinedTextField(
                value = currentValue,
                onValueChange = onValueChange,
                label = { Text("新金额") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
