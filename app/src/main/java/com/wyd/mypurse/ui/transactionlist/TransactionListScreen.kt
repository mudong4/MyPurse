package com.wyd.mypurse.ui.transactionlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 流水列表页 Screen。阶段 2 实现完整 UI。
 */
@Composable
fun TransactionListScreen(
    timeGranularity: String,
    categoryFilter: String?,
    timeRangeStart: Long?,
    timeRangeEnd: Long?,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "流水列表 - 阶段 2 实现")
    }
}
