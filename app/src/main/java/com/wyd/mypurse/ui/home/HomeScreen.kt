package com.wyd.mypurse.ui.home

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyd.mypurse.domain.model.CategoryAmount
import com.wyd.mypurse.domain.model.MonthlyAmount
import com.wyd.mypurse.ui.components.EmptyStateText
import com.wyd.mypurse.ui.components.rememberDebounce
import com.wyd.mypurse.ui.theme.AppBudgetBlue
import com.wyd.mypurse.ui.theme.AppBudgetOrange
import com.wyd.mypurse.ui.theme.AppBudgetOverRed
import com.wyd.mypurse.ui.theme.AppExpenseRed
import com.wyd.mypurse.ui.theme.AppIncomeGreen
import com.wyd.mypurse.ui.theme.AppProgressTrack
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat

private val decimalFormat = DecimalFormat("#,##0.00")

/**
 * 首页 Screen。
 * 包含：概览卡片、日/周/月/年汇总、预算进度条、近6月趋势图、本月支出 TOP5、悬浮按钮。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToAddTransaction: () -> Unit,
    onNavigateToTransactionList: (String) -> Unit,
    onNavigateToBudget: () -> Unit,
    onNavigateToStatistics: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val debounce = rememberDebounce()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("本月概览") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                actions = {
                    IconButton(onClick = { debounce { onNavigateToBudget() } }) {
                        Icon(Icons.Default.DateRange, contentDescription = "预算设置")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { debounce { onNavigateToAddTransaction() } },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "记一笔")
            }
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
        } else if (uiState.isNewUser) {
            // 新用户引导：无任何记录时显示引导页
            FirstTimeGuide(
                onStartRecording = { debounce { onNavigateToAddTransaction() } },
                onSetBudget = { debounce { onNavigateToBudget() } },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 概览卡片（当前结余）
                BalanceCard(
                    balance = uiState.balance,
                    totalIncome = uiState.thisMonth.income,
                    totalExpense = uiState.thisMonth.expense,
                    onClick = { debounce { onNavigateToTransactionList("month") } }
                )

                // 2. 日/周/月/年汇总区
                SummaryRows(
                    today = uiState.today,
                    thisWeek = uiState.thisWeek,
                    thisMonth = uiState.thisMonth,
                    thisYear = uiState.thisYear,
                    onRowClick = { granularity -> debounce { onNavigateToTransactionList(granularity) } }
                )

                // 3. 预算进度条
                BudgetProgressBar(
                    spent = uiState.thisMonth.expense,
                    budget = uiState.budget,
                    onClick = { debounce { onNavigateToBudget() } }
                )

                // 4. 近6个月趋势图
                if (uiState.trend.isNotEmpty()) {
                    TrendChartCard(
                        trend = uiState.trend,
                        onClick = { debounce { onNavigateToStatistics("trend") } }
                    )
                }

                // 5. 本月支出排行榜
                if (uiState.topCategories.isNotEmpty()) {
                    TopCategoriesCard(
                        categories = uiState.topCategories,
                        onClick = { debounce { onNavigateToStatistics("composition") } }
                    )
                }

                // 底部留白，避免被 FAB 遮挡
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

// ========== 1. 概览卡片 ==========

@Composable
private fun BalanceCard(
    balance: BigDecimal,
    totalIncome: BigDecimal,
    totalExpense: BigDecimal,
    onClick: () -> Unit
) {
    val isNegative = balance < BigDecimal.ZERO
    val balanceColor = if (isNegative) AppExpenseRed else MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "当前结余",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatMoney(balance, withSign = false),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp
                ),
                color = balanceColor
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "本月支出",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "-¥${decimalFormat.format(totalExpense)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppExpenseRed
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "本月收入",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "+¥${decimalFormat.format(totalIncome)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = AppIncomeGreen
                    )
                }
            }
        }
    }
}

// ========== 2. 日/周/月/年汇总区 ==========

@Composable
private fun SummaryRows(
    today: com.wyd.mypurse.domain.model.PeriodSummary,
    thisWeek: com.wyd.mypurse.domain.model.PeriodSummary,
    thisMonth: com.wyd.mypurse.domain.model.PeriodSummary,
    thisYear: com.wyd.mypurse.domain.model.PeriodSummary,
    onRowClick: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            SummaryRow(
                label = "今日",
                expense = today.expense,
                income = today.income,
                onClick = { onRowClick("day") }
            )
            SummaryRow(
                label = "本周",
                expense = thisWeek.expense,
                income = thisWeek.income,
                onClick = { onRowClick("week") }
            )
            SummaryRow(
                label = "本月",
                expense = thisMonth.expense,
                income = thisMonth.income,
                onClick = { onRowClick("month") }
            )
            SummaryRow(
                label = "今年",
                expense = thisYear.expense,
                income = thisYear.income,
                onClick = { onRowClick("year") }
            )
        }
    }
}

@Composable
private fun SummaryRow(
    label: String,
    expense: BigDecimal,
    income: BigDecimal,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "-¥${decimalFormat.format(expense)}",
                style = MaterialTheme.typography.bodyMedium,
                color = AppExpenseRed
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "+¥${decimalFormat.format(income)}",
                style = MaterialTheme.typography.bodyMedium,
                color = AppIncomeGreen
            )
        }
    }
}

// ========== 3. 预算进度条 ==========

@Composable
private fun BudgetProgressBar(
    spent: BigDecimal,
    budget: BigDecimal?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (budget == null || budget <= BigDecimal.ZERO) {
                Text(
                    text = "设置月度预算 >",
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppBudgetBlue
                )
            } else {
                val ratio = (spent.divide(budget, 4, RoundingMode.HALF_UP)
                    .coerceAtMost(BigDecimal.ONE))
                val ratioFloat = ratio.toFloat()
                val progressColor = when {
                    ratio >= BigDecimal.ONE -> AppBudgetOverRed
                    ratio >= BigDecimal("0.8") -> AppBudgetOrange
                    else -> AppBudgetBlue
                }
                val animatedColor by animateColorAsState(progressColor, label = "budgetColor")

                val remaining = budget.subtract(spent)
                val remainingText = if (remaining >= BigDecimal.ZERO) {
                    "剩余 ¥${decimalFormat.format(remaining)}"
                } else {
                    "超支 ¥${decimalFormat.format(remaining.abs())}"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "月度预算",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "已花 ¥${decimalFormat.format(spent)} / ¥${decimalFormat.format(budget)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { ratioFloat },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = animatedColor,
                    trackColor = AppProgressTrack,
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = remainingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = animatedColor
                )
            }
        }
    }
}

// ========== 4. 近6个月趋势图（简洁柱状图） ==========

@Composable
private fun TrendChartCard(
    trend: List<MonthlyAmount>,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "近 6 个月支出趋势",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // 简洁柱状图：用 Compose 原生绘制
            val maxAmount = trend.maxOfOrNull { it.total } ?: BigDecimal.ONE
            if (maxAmount <= BigDecimal.ZERO) {
                EmptyStateText(
                    message = "暂无数据",
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    trend.forEach { item ->
                        val heightFraction = item.total.divide(maxAmount, 4, RoundingMode.HALF_UP)
                            .toFloat()
                            .coerceAtLeast(0.02f)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxSize(heightFraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .then(
                                            Modifier.fillMaxWidth()
                                        )
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .then(
                                                Modifier.fillMaxWidth()
                                            )
                                    ) {
                                        // 使用 BudgetBlue 作为柱状图颜色
                                        androidx.compose.foundation.Canvas(
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            drawRect(color = AppBudgetBlue)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${item.month}月",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ========== 5. 本月支出排行榜 TOP 5 ==========

@Composable
private fun TopCategoriesCard(
    categories: List<CategoryAmount>,
    onClick: () -> Unit
) {
    val totalExpense = categories.sumOf { it.total }
    val maxAmount = categories.maxOfOrNull { it.total } ?: BigDecimal.ONE

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "本月支出排行",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "完整排行 >",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppBudgetBlue
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            categories.forEach { category ->
                val ratio = if (maxAmount > BigDecimal.ZERO) {
                    category.total.divide(maxAmount, 4, RoundingMode.HALF_UP)
                        .toFloat()
                        .coerceAtLeast(0.02f)
                } else 0f

                val percent = if (totalExpense > BigDecimal.ZERO) {
                    category.total.multiply(BigDecimal("100"))
                        .divide(totalExpense, 1, RoundingMode.HALF_UP)
                } else BigDecimal.ZERO

                CategoryRankRow(
                    name = category.categoryL1,
                    amount = category.total,
                    ratio = ratio,
                    percent = percent
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CategoryRankRow(
    name: String,
    amount: BigDecimal,
    ratio: Float,
    percent: BigDecimal
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "¥${decimalFormat.format(amount)}  ${percent}%",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { ratio },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = AppBudgetBlue,
            trackColor = AppProgressTrack,
        )
    }
}

// ========== 6. 新用户引导 ==========

@Composable
private fun FirstTimeGuide(
    onStartRecording: () -> Unit,
    onSetBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "📒",
            fontSize = 64.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "欢迎使用 MyPurse",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "简单纯粹的记账工具\n专注记录，不打扰",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 引导卡片
        GuideStep(
            emoji = "💰",
            title = "记一笔",
            description = "记录每天的支出和收入\n支持连续记账模式",
            actionLabel = "开始记账",
            onAction = onStartRecording
        )

        Spacer(modifier = Modifier.height(16.dp))

        GuideStep(
            emoji = "🎯",
            title = "设定预算",
            description = "设置月度预算，控制开销\n首页实时进度提醒",
            actionLabel = "设置预算",
            onAction = onSetBudget
        )

        Spacer(modifier = Modifier.height(48.dp))
    }
}

@Composable
private fun GuideStep(
    emoji: String,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                fontSize = 36.sp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            Text(
                text = actionLabel,
                color = AppBudgetBlue,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clickable(onClick = onAction)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// ========== 工具函数 ==========

/**
 * 格式化金额为显示字符串。
 * @param withSign 是否显示正负号
 */
private fun formatMoney(amount: BigDecimal, withSign: Boolean = true): String {
    val abs = amount.abs()
    val formatted = decimalFormat.format(abs)
    return when {
        !withSign -> formatted
        amount < BigDecimal.ZERO -> "-¥$formatted"
        amount > BigDecimal.ZERO -> "+¥$formatted"
        else -> "¥0.00"
    }
}
