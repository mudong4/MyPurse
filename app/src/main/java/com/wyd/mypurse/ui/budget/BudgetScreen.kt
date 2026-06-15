package com.wyd.mypurse.ui.budget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 预算设置页 Screen。阶段 3 实现完整 UI。
 */
@Composable
fun BudgetScreen(
    year: Int?,
    month: Int?,
    onNavigateBack: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "预算设置 - 阶段 3 实现")
    }
}
