package com.wyd.mypurse.ui.recurring

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * 固定收支模板列表页 Screen。阶段 4 实现完整 UI。
 */
@Composable
fun RecurringTemplateListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (Long) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "固定收支模板 - 阶段 4 实现")
    }
}
