package com.wyd.mypurse.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyd.mypurse.ui.components.rememberDebounce

/**
 * 设置页 Screen。阶段 8 实现完整 UI。
 * 4 个分组：数据管理（导出/导入/清除）、记账设置（分类/预算）、显示设置（灰显占位）、关于（版本/许可/反馈）。
 */
@Composable
fun SettingsScreen(
    onNavigateToCategoryManage: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToAbout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val debounce = rememberDebounce()

    // 文件选择器（导入 CSV）
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.importCsv(it) }
    }

    // 分享导出文件
    LaunchedEffect(uiState.exportedFileUri) {
        uiState.exportedFileUri?.let { uriStr ->
            val uri = Uri.parse(uriStr)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "分享 CSV"))
            viewModel.onExportShared()
        }
    }

    // 清除完成提示
    LaunchedEffect(uiState.clearDataComplete) {
        if (uiState.clearDataComplete) {
            viewModel.onClearDataCompleteHandled()
        }
    }

    // 错误提示
    uiState.errorMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissError() },
            title = { Text("提示") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissError() }) {
                    Text("确定")
                }
            }
        )
    }

    // 导入结果提示
    uiState.importResultMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissImportResult() },
            title = { Text("导入结果") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissImportResult() }) {
                    Text("确定")
                }
            }
        )
    }

    // 清除数据确认对话框
    if (uiState.showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearDataDialog() },
            title = { Text("清除所有数据") },
            text = {
                Text("此操作将删除所有流水记录、预算和自定义分类（内置分类保留）。\n\n此操作不可撤销，确定继续吗？")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmClearAllData() }) {
                    Text("确定清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissClearDataDialog() }) {
                    Text("取消")
                }
            }
        )
    }

    // 清除中遮罩
    if (uiState.isClearingData) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("正在清除数据...") },
            text = { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) },
            confirmButton = {}
        )
    }

    // V1.1 预设选择器 BottomSheet
    if (uiState.showPresetSelector) {
        PresetSelectorSheet(
            currentPresetName = uiState.currentThemePresetName,
            onSelect = { viewModel.selectPreset(it) },
            onDismiss = { viewModel.dismissPresetSelector() }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // ===== 一、数据管理 =====
        SectionHeader("数据管理")
        SettingsCard {
            // 导出 CSV
            SettingsItem(
                icon = Icons.Filled.Share,
                title = "导出 CSV",
                subtitle = if (uiState.isExporting) "正在导出..." else "导出所有流水记录",
                enabled = !uiState.isExporting && !uiState.isImporting,
                onClick = { debounce { viewModel.exportCsv() } }
            )
            if (uiState.isExporting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // 导入 CSV
            SettingsItem(
                icon = Icons.Filled.FileOpen,
                title = "导入 CSV",
                subtitle = if (uiState.isImporting) "正在导入..." else "从 CSV 文件导入流水",
                enabled = !uiState.isExporting && !uiState.isImporting,
                onClick = { debounce { filePickerLauncher.launch("text/*") } }
            )
            if (uiState.isImporting) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

            // 清除所有数据
            SettingsItem(
                icon = Icons.Filled.DeleteForever,
                title = "清除所有数据",
                subtitle = "删除所有记录、预算和自定义分类",
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { debounce { viewModel.showClearDataDialog() } }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ===== 二、记账设置 =====
        SectionHeader("记账设置")
        SettingsCard {
            SettingsItem(
                icon = Icons.Filled.Category,
                title = "分类管理",
                subtitle = "自定义收入/支出分类",
                onClick = { debounce { onNavigateToCategoryManage() } }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsItem(
                icon = Icons.Filled.Savings,
                title = "预算设置",
                subtitle = "设置每月预算限额",
                onClick = { debounce { onNavigateToBudget() } }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ===== 三、显示设置 =====
        SectionHeader("显示设置")
        SettingsCard {
            SettingsItem(
                icon = Icons.Filled.Palette,
                title = "主题风格",
                subtitle = uiState.currentThemePresetName,
                enabled = true,
                onClick = { viewModel.showPresetSelector() }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ===== 四、关于 =====
        SectionHeader("关于")
        SettingsCard {
            SettingsItem(
                icon = Icons.Filled.Info,
                title = "关于 MyPurse",
                subtitle = "版本 1.0.0 · 开源许可",
                onClick = { debounce { onNavigateToAbout() } }
            )
            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            SettingsItem(
                icon = Icons.Filled.Feedback,
                title = "反馈与建议",
                subtitle = "通过邮件联系我们",
                onClick = {
                    debounce {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:feedback@mypurse.app")
                            putExtra(Intent.EXTRA_SUBJECT, "MyPurse 反馈")
                        }
                        context.startActivity(intent)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ==================== 子组件 ====================

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
                   else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) titleColor
                        else titleColor.copy(alpha = 0.4f)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}
