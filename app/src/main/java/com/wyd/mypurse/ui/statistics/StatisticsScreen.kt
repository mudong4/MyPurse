package com.wyd.mypurse.ui.statistics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 统计页 Screen。阶段 3 实现完整 UI（时间选择器、概览卡片、三视图）。
 */
@Composable
fun StatisticsScreen(
    onNavigateToTransactionList: (String, String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "统计 - 阶段 3 实现")
    }
}
