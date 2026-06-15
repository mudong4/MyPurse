package com.wyd.mypurse.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 设置页 Screen。阶段 5 实现完整 UI。
 */
@Composable
fun SettingsScreen(
    onNavigateToCategoryManage: () -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToRecurringTemplate: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onExportCsv: () -> Unit,
    onImportCsv: () -> Unit,
    onClearAllData: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "设置 - 阶段 5 实现")
    }
}
