package com.wyd.mypurse.ui.statistics

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyd.mypurse.domain.model.CategoryAmount
import com.wyd.mypurse.domain.model.TrendPoint
import com.wyd.mypurse.domain.usecase.GetStatisticsUseCase.Granularity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Calendar

// ========== 色值 ==========
private val ExpenseRed = Color(0xFFE53935)
private val IncomeGreen = Color(0xFF43A047)
private val ChartBlue = Color(0xFF1E88E5)
private val ChartOrange = Color(0xFFFF7043)
private val ChartPurple = Color(0xFF8E24AA)
private val ChartTeal = Color(0xFF00ACC1)
private val ChartPink = Color(0xFFEC407A)
private val ChartIndigo = Color(0xFF3949AB)
private val SurfaceBg = Color(0xFFF8F9FA)

private val donutColors = listOf(
    Color(0xFF1E88E5), Color(0xFF43A047), Color(0xFFFB8C00),
    Color(0xFFE53935), Color(0xFF8E24AA), Color(0xFF9E9E9E) // 最后一个是"其他"
)

private val df = DecimalFormat("#,###.##")
private val dfPct = DecimalFormat("#.#")

// ========== 主页面 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTransactionList: (String, String) -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val useCase = viewModel // 通过 ViewModel 引用 UseCase 的方法
    // 实际上 ViewModel 没有直接暴露 UseCase，我们改用函数引用
    val labelStep = if (uiState.trend.isNotEmpty()) {
        when {
            uiState.trend.size <= 4 -> 1
            uiState.trend.size <= 8 -> 1
            uiState.trend.size <= 15 -> 2
            uiState.trend.size <= 20 -> 3
            else -> 5
        }
    } else 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceBg
                )
            )
        },
        bottomBar = {
            BottomToolbar(
                granularity = uiState.granularity,
                currentTimestamp = uiState.currentTimestamp,
                timeLabel = uiState.timeLabel,
                pageMode = uiState.pageMode,
                availableYears = uiState.availableYears,
                onGranularityChange = { viewModel.setGranularity(it) },
                onNavigateTime = { viewModel.navigateTime(it) },
                onJumpToTimestamp = { viewModel.jumpToTimestamp(it) },
                onTogglePageMode = { viewModel.togglePageMode() }
            )
        },
        containerColor = SurfaceBg
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
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                when (uiState.pageMode) {
                    PageMode.CATEGORY -> CategoryPage(
                        totalExpense = uiState.totalExpense,
                        totalIncome = uiState.totalIncome,
                        isShowingIncome = uiState.isShowingIncome,
                        composition = if (uiState.isShowingIncome) uiState.incomeComposition else uiState.composition,
                        chartMode = uiState.compositionChartMode,
                        expandedCategoryId = uiState.expandedCategoryId,
                        subComposition = uiState.subComposition,
                        isSubLoading = uiState.isSubLoading,
                        onToggleType = { viewModel.toggleCompositionType() },
                        onToggleSubCategory = { viewModel.toggleSubCategory(it) },
                        onToggleChartMode = { viewModel.toggleCompositionChartMode() }
                    )

                    PageMode.TREND -> TrendPage(
                            trend = uiState.trend,
                            granularity = uiState.granularity,
                            chartMode = uiState.trendChartMode,
                            labelStep = labelStep,
                            onToggleChartMode = { viewModel.toggleTrendChartMode() }
                        )
                }
            }
        }
    }
}

// ========== 分类页面 ==========

@Composable
private fun CategoryPage(
    totalExpense: BigDecimal,
    totalIncome: BigDecimal,
    isShowingIncome: Boolean,
    composition: List<CategoryAmount>,
    chartMode: ChartMode,
    expandedCategoryId: Long?,
    subComposition: List<CategoryAmount>,
    isSubLoading: Boolean,
    onToggleType: () -> Unit,
    onToggleSubCategory: (Long) -> Unit,
    onToggleChartMode: () -> Unit
) {
    // 概览卡片
    OverviewCard(totalExpense = totalExpense, totalIncome = totalIncome)

    Spacer(modifier = Modifier.height(12.dp))

    // 分类图卡片
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶部：支出/收入 Tab + 图表切换按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 支出/收入 切换
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = !isShowingIncome,
                        onClick = { if (isShowingIncome) onToggleType() },
                        label = { Text("支出", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ExpenseRed.copy(alpha = 0.12f),
                            selectedLabelColor = ExpenseRed
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = isShowingIncome,
                        onClick = { if (!isShowingIncome) onToggleType() },
                        label = { Text("收入", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IncomeGreen.copy(alpha = 0.12f),
                            selectedLabelColor = IncomeGreen
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                }

                // 图表切换：单个按钮循环切换
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleChartMode),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chartMode.iconLabel,
                        fontSize = 18.sp,
                        color = ChartBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (composition.isEmpty()) {
                Text(
                    text = "暂无数据",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            } else {
                val total = composition.sumOf { it.total }
                // Top 5 + 其他
                val top5 = composition.take(5).toList()
                val othersTotal = composition.drop(5).sumOf { it.total }
                val displayList = if (composition.size > 5) {
                    top5 + CategoryAmount(categoryL1Id = -1L, categoryL1 = "其他", total = othersTotal)
                } else {
                    composition
                }

                when (chartMode) {
                    ChartMode.DONUT -> DonutChart(
                        items = displayList,
                        total = total,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    )
                    ChartMode.BAR -> CompositionBarList(
                        items = displayList,
                        total = total,
                        expandedCategoryId = expandedCategoryId,
                        subComposition = subComposition,
                        isSubLoading = isSubLoading,
                        onToggleSubCategory = onToggleSubCategory
                    )
                    else -> {}
                }
            }
        }
    }
}

// ========== 概览卡片 ==========

@Composable
private fun OverviewCard(totalExpense: BigDecimal, totalIncome: BigDecimal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("支出", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "-¥${df.format(totalExpense)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = ExpenseRed
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("收入", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "+¥${df.format(totalIncome)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreen
                )
            }
        }
    }
}

// ========== 环形图 ==========

@Composable
private fun DonutChart(
    items: List<CategoryAmount>,
    total: BigDecimal,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasSize = minOf(size.width, size.height)
            val strokeWidth = canvasSize * 0.22f
            val radius = (canvasSize - strokeWidth) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            var startAngle = -90f

            items.forEachIndexed { index, item ->
                val sweep = if (total > BigDecimal.ZERO) {
                    item.total.divide(total, 4, RoundingMode.HALF_UP)
                        .toFloat() * 360f
                } else 0f

                drawArc(
                    color = donutColors[index % donutColors.size],
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth)
                )
                startAngle += sweep
            }
        }

        // 中间文字
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (total > BigDecimal.ZERO) "${dfPct.format(total.divide(total, 1, RoundingMode.HALF_UP).multiply(BigDecimal("100")))}%" else "0%",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }

    // 图例
    Spacer(modifier = Modifier.height(8.dp))
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        items.forEachIndexed { index, item ->
            val pct = if (total > BigDecimal.ZERO) {
                item.total.multiply(BigDecimal("100")).divide(total, 1, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = donutColors[index % donutColors.size])
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.categoryL1,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "¥${df.format(item.total)}  ${dfPct.format(pct)}%",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

// ========== 条形图列表 ==========

@Composable
private fun CompositionBarList(
    items: List<CategoryAmount>,
    total: BigDecimal,
    expandedCategoryId: Long?,
    subComposition: List<CategoryAmount>,
    isSubLoading: Boolean,
    onToggleSubCategory: (Long) -> Unit
) {
    items.forEachIndexed { index, item ->
        val pct = if (total > BigDecimal.ZERO) {
            item.total.divide(total, 4, RoundingMode.HALF_UP).toFloat().coerceIn(0f, 1f)
        } else 0f
        val pctText = if (total > BigDecimal.ZERO) {
            item.total.multiply(BigDecimal("100")).divide(total, 1, RoundingMode.HALF_UP)
        } else BigDecimal.ZERO
        val color = donutColors[index % donutColors.size]

        CompositionBar(
            name = item.categoryL1,
            amount = item.total,
            pct = pct,
            pctText = pctText,
            color = color,
            isExpanded = expandedCategoryId == item.categoryL1Id,
            isOthers = item.categoryL1Id == -1L,
            onClick = {
                if (item.categoryL1Id != null && item.categoryL1Id != -1L) {
                    onToggleSubCategory(item.categoryL1Id)
                }
            }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 二级分类展开
        if (expandedCategoryId == item.categoryL1Id) {
            if (isSubLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                }
            } else if (subComposition.isEmpty()) {
                Text(
                    text = "无二级分类",
                    modifier = Modifier.fillMaxWidth().padding(start = 32.dp, top = 2.dp, bottom = 8.dp),
                    fontSize = 11.sp, color = Color.Gray
                )
            } else {
                val subTotal = subComposition.sumOf { it.total }
                Column(modifier = Modifier.padding(start = 24.dp)) {
                    subComposition.forEach { sub ->
                        val subPct = if (subTotal > BigDecimal.ZERO) {
                            sub.total.divide(subTotal, 4, RoundingMode.HALF_UP)
                                .toFloat().coerceAtLeast(0.03f)
                        } else 0f
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sub.categoryL1,
                                fontSize = 11.sp,
                                modifier = Modifier.width(56.dp),
                                color = Color.Gray
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .then(Modifier.fillMaxWidth(subPct))
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawRect(color = color.copy(alpha = 0.4f))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "¥${df.format(sub.total)}",
                                fontSize = 10.sp, color = Color.Gray
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun CompositionBar(
    name: String,
    amount: BigDecimal,
    pct: Float,
    pctText: BigDecimal,
    color: Color,
    isExpanded: Boolean,
    isOthers: Boolean,
    onClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = !isOthers, onClick = onClick)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 分类名称
            Text(
                text = name,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(56.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 带背景的条形图（灰色底 + 彩色填充 + 金额在上、百分比在下）
            val barFraction = pct.coerceIn(0.02f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFEEEEEE))
            ) {
                // 彩色填充条
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(barFraction)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )

                // 金额显示在条上
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "¥${df.format(amount)}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (pct > 0.4f) Color.White else Color.DarkGray
                    )
                    Text(
                        text = "${dfPct.format(pctText)}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (pct > 0.4f) Color.White.copy(alpha = 0.8f) else Color.Gray
                    )
                }
            }
        }
    }
}

// ========== 趋势页面 ==========

@Composable
private fun TrendPage(
    trend: List<TrendPoint>,
    granularity: Granularity,
    chartMode: ChartMode,
    labelStep: Int,
    onToggleChartMode: () -> Unit
) {
    // 选中数据点状态
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶部：图表切换按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onToggleChartMode),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = chartMode.iconLabel,
                        fontSize = 18.sp,
                        color = ChartBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (trend.isEmpty()) {
                Text(
                    text = "暂无数据",
                    modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                    textAlign = TextAlign.Center,
                    color = Color.Gray
                )
            } else {
                val maxAmount = trend.maxOfOrNull { it.total } ?: BigDecimal.ONE

                // 趋势摘要：均值/最高/最低，放在图表上方
                TrendSummary(trend = trend)

                Spacer(modifier = Modifier.height(12.dp))

                // 选中提示条
                if (selectedIndex != null && selectedIndex!! < trend.size) {
                    val sel = trend[selectedIndex!!]
                    Text(
                        text = "${shortLabel(sel, granularity)}  支出 ¥${df.format(sel.total)}",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = ChartOrange
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }

                // 图表
                when (chartMode) {
                    ChartMode.BAR -> BarChart(
                        trend = trend,
                        maxAmount = maxAmount,
                        selectedIndex = selectedIndex,
                        labelStep = labelStep,
                        granularity = granularity,
                        onSelectIndex = { selectedIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    ChartMode.LINE -> LineChart(
                        trend = trend,
                        maxAmount = maxAmount,
                        selectedIndex = selectedIndex,
                        labelStep = labelStep,
                        granularity = granularity,
                        onSelectIndex = { selectedIndex = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                    else -> {}
                }
            }
        }
    }
}

/** X 轴短标签 */
private fun shortLabel(point: TrendPoint, granularity: Granularity): String {
    val raw = point.label
    return when (granularity) {
        Granularity.DAY -> if (raw.length >= 10) raw.substring(5) else raw
        Granularity.WEEK -> if ('W' in raw) raw.substring(raw.indexOf('W')) else raw
        Granularity.MONTH -> if (raw.length >= 7) "${raw.substring(5).toIntOrNull() ?: raw}月" else raw
        Granularity.QUARTER -> if ('Q' in raw) raw.substring(raw.indexOf('Q')) else raw
        Granularity.YEAR -> raw
    }
}

// ========== 柱状图 ==========

@Composable
private fun BarChart(
    trend: List<TrendPoint>,
    maxAmount: BigDecimal,
    selectedIndex: Int?,
    labelStep: Int,
    granularity: Granularity,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            .pointerInput(trend) {
                detectTapGestures { offset ->
                    if (trend.isEmpty()) return@detectTapGestures
                    val barSpacing = size.width / (trend.size + 1)
                    val index = ((offset.x / barSpacing).toInt()).coerceIn(0, trend.size - 1)
                    onSelectIndex(if (index == selectedIndex) null else index)
                }
            }
    ) {
        if (trend.isEmpty() || maxAmount <= BigDecimal.ZERO) return@Canvas
        val chartW = size.width
        val chartH = size.height
        val bottomPad = 28.dp.toPx()
        val topPad = 8.dp.toPx()
        val drawH = chartH - bottomPad - topPad

        val barCount = trend.size
        val barSpacing = chartW / (barCount + 1)
        val barW = (barSpacing * 0.6f).coerceAtMost(28.dp.toPx())

        // 画柱子
        trend.forEachIndexed { index, point ->
            val x = barSpacing * (index + 1) - barW / 2
            val hFrac = point.total.divide(maxAmount, 4, RoundingMode.HALF_UP)
                .toFloat().coerceIn(0.02f, 1f)
            val barH = drawH * hFrac
            val y = topPad + drawH - barH

            val color = if (index == selectedIndex) ChartOrange else ChartBlue
            drawRect(color = color, topLeft = Offset(x, y), size = Size(barW, barH))
        }

        // 选中竖线
        if (selectedIndex != null && selectedIndex < trend.size) {
            val lineX = barSpacing * (selectedIndex + 1)
            drawLine(
                color = ChartOrange.copy(alpha = 0.5f),
                start = Offset(lineX, topPad),
                end = Offset(lineX, topPad + drawH),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // X 轴标签
        trend.forEachIndexed { index, point ->
            if (index % labelStep == 0) {
                val x = barSpacing * (index + 1)
                val label = shortLabel(point, granularity)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    chartH - 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = 0xFF999999.toInt()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

// ========== 折线图 ==========

@Composable
private fun LineChart(
    trend: List<TrendPoint>,
    maxAmount: BigDecimal,
    selectedIndex: Int?,
    labelStep: Int,
    granularity: Granularity,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Canvas(
        modifier = modifier
            .pointerInput(trend) {
                detectTapGestures { offset ->
                    if (trend.isEmpty()) return@detectTapGestures
                    val barSpacing = size.width / (trend.size + 1)
                    val index = ((offset.x / barSpacing).toInt()).coerceIn(0, trend.size - 1)
                    onSelectIndex(if (index == selectedIndex) null else index)
                }
            }
    ) {
        if (trend.size < 2 || maxAmount <= BigDecimal.ZERO) return@Canvas
        val chartW = size.width
        val chartH = size.height
        val bottomPad = 28.dp.toPx()
        val topPad = 8.dp.toPx()
        val drawH = chartH - bottomPad - topPad

        val barSpacing = chartW / (trend.size + 1)

        // 填充区域
        val fillPath = Path()
        trend.forEachIndexed { index, point ->
            val x = barSpacing * (index + 1)
            val hFrac = point.total.divide(maxAmount, 4, RoundingMode.HALF_UP)
                .toFloat().coerceIn(0f, 1f)
            val y = topPad + drawH * (1 - hFrac)
            if (index == 0) {
                fillPath.moveTo(x, topPad + drawH)
                fillPath.lineTo(x, y)
            } else {
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(barSpacing * trend.size, topPad + drawH)
        fillPath.close()
        drawPath(path = fillPath, color = ChartBlue.copy(alpha = 0.1f))

        // 折线
        val linePath = Path()
        trend.forEachIndexed { index, point ->
            val x = barSpacing * (index + 1)
            val hFrac = point.total.divide(maxAmount, 4, RoundingMode.HALF_UP)
                .toFloat().coerceIn(0f, 1f)
            val y = topPad + drawH * (1 - hFrac)
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        drawPath(path = linePath, color = ChartBlue, style = Stroke(width = 2.5.dp.toPx()))

        // 数据点
        trend.forEachIndexed { index, point ->
            val x = barSpacing * (index + 1)
            val hFrac = point.total.divide(maxAmount, 4, RoundingMode.HALF_UP)
                .toFloat().coerceIn(0f, 1f)
            val y = topPad + drawH * (1 - hFrac)
            val isSelected = index == selectedIndex
            drawCircle(
                color = if (isSelected) ChartOrange else ChartBlue,
                radius = (if (isSelected) 6 else 4).dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = Color.White,
                radius = (if (isSelected) 3 else 2).dp.toPx(),
                center = Offset(x, y)
            )
        }

        // 选中竖线
        if (selectedIndex != null && selectedIndex < trend.size) {
            val lineX = barSpacing * (selectedIndex + 1)
            drawLine(
                color = ChartOrange.copy(alpha = 0.4f),
                start = Offset(lineX, topPad),
                end = Offset(lineX, topPad + drawH),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // X 轴标签
        trend.forEachIndexed { index, point ->
            if (index % labelStep == 0) {
                val x = barSpacing * (index + 1)
                val label = shortLabel(point, granularity)
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    x,
                    chartH - 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = 0xFF999999.toInt()
                        textSize = 10.sp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

// ========== 趋势摘要 ==========

@Composable
private fun TrendSummary(trend: List<TrendPoint>) {
    if (trend.isEmpty()) return

    val avg = trend.map { it.total }.reduce { a, b -> a.add(b) }
        .divide(BigDecimal(trend.size.toLong()), 2, RoundingMode.HALF_UP)
    val maxPoint = trend.maxBy { it.total }
    val minPoint = trend.minBy { it.total }
    val total = trend.sumOf { it.total }

    // 标题行
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "趋势概览",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF333333)
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "合计 ¥${df.format(total)}",
            fontSize = 12.sp,
            color = Color.Gray
        )
    }

    // 三项指标
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SummaryItem("均值", "¥${df.format(avg)}", ChartBlue)
        SummaryItem("最高", "¥${df.format(maxPoint.total)}", ExpenseRed)
        SummaryItem("最低", "¥${df.format(minPoint.total)}", IncomeGreen)
    }
}

@Composable
private fun SummaryItem(label: String, value: String, valueColor: Color = Color.Black) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = valueColor
        )
    }
}

// ========== 底部工具栏 ==========

@Composable
private fun BottomToolbar(
    granularity: Granularity,
    currentTimestamp: Long,
    timeLabel: String,
    pageMode: PageMode,
    availableYears: List<Int>,
    onGranularityChange: (Granularity) -> Unit,
    onNavigateTime: (Int) -> Unit,
    onJumpToTimestamp: (Long) -> Unit,
    onTogglePageMode: () -> Unit
) {
    var showGranularityMenu by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Column {
        HorizontalDivider(color = Color(0xFFE0E0E0))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 粒度下拉（左侧）
            Box {
                Row(
                    modifier = Modifier
                        .clickable { showGranularityMenu = true }
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = granularity.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                }
                DropdownMenu(
                    expanded = showGranularityMenu,
                    onDismissRequest = { showGranularityMenu = false }
                ) {
                    Granularity.entries.forEach { g ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    g.label,
                                    fontWeight = if (g == granularity) FontWeight.Bold else FontWeight.Normal,
                                    color = if (g == granularity) ChartBlue else Color.Black
                                )
                            },
                            onClick = {
                                onGranularityChange(g)
                                showGranularityMenu = false
                            }
                        )
                    }
                }
            }

            // 时间导航（中间）
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = { onNavigateTime(-1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        "上一个",
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = timeLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    color = ChartBlue,
                    modifier = Modifier
                        .clickable { showTimePicker = true }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
                IconButton(
                    onClick = { onNavigateTime(1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        "下一个",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 页面切换（右侧）：点击文字直接切换，无下拉菜单
            val targetLabel = when (pageMode) {
                PageMode.CATEGORY -> PageMode.TREND.label
                PageMode.TREND -> PageMode.CATEGORY.label
            }
            Text(
                text = targetLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = ChartBlue,
                modifier = Modifier
                    .clickable(onClick = onTogglePageMode)
                    .padding(horizontal = 8.dp, vertical = 6.dp)
            )
        }
    }

    // 时间选择弹窗
    if (showTimePicker) {
        TimePickerDialog(
            granularity = granularity,
            currentTimestamp = currentTimestamp,
            availableYears = availableYears,
            onDismiss = { showTimePicker = false },
            onSelect = { ts ->
                onJumpToTimestamp(ts)
                showTimePicker = false
            }
        )
    }
}

// ========== 时间选择弹窗 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    granularity: Granularity,
    currentTimestamp: Long,
    availableYears: List<Int>,
    onDismiss: () -> Unit,
    onSelect: (Long) -> Unit
) {
    val cal = remember(currentTimestamp) {
        Calendar.getInstance().apply { timeInMillis = currentTimestamp }
    }
    val curYear = cal.get(Calendar.YEAR)
    val curMonth = cal.get(Calendar.MONTH) + 1
    val curDay = cal.get(Calendar.DAY_OF_MONTH)

    // 年份列表：最多显示最近10年
    val years = remember(availableYears, curYear) {
        val set = (availableYears + curYear).toMutableSet()
        set.sortedDescending().take(10)
    }

    when (granularity) {
        Granularity.YEAR -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                title = null,
                text = {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                        items(years) { year ->
                            val isSelected = year == curYear
                            Text(
                                text = "${year}年",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val ts = Calendar.getInstance().apply {
                                            set(Calendar.YEAR, year)
                                            set(Calendar.MONTH, Calendar.JANUARY)
                                            set(Calendar.DAY_OF_MONTH, 1)
                                            set(Calendar.HOUR_OF_DAY, 0)
                                            set(Calendar.MINUTE, 0)
                                            set(Calendar.SECOND, 0)
                                            set(Calendar.MILLISECOND, 0)
                                        }.timeInMillis
                                        onSelect(ts)
                                    }
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                         else Color(0xFF333333),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                },
                confirmButton = {
                    Text(
                        text = "取消", color = Color.Gray,
                        modifier = Modifier.clickable(onClick = onDismiss).padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            )
        }
        Granularity.MONTH -> {
            YearMonthPickerForTrend(
                currentYear = curYear,
                currentMonth = curMonth,
                availableYears = availableYears,
                onSelected = { year, month ->
                    val ts = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    onSelect(ts)
                },
                onDismiss = onDismiss
            )
        }
        Granularity.DAY -> {
            DayPickerForTrend(
                currentYear = curYear,
                currentMonth = curMonth,
                currentDay = curDay,
                availableYears = availableYears,
                onSelected = { year, month, day ->
                    val ts = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    onSelect(ts)
                },
                onDismiss = onDismiss
            )
        }
        Granularity.WEEK -> {
            WeekPickerForTrend(
                currentYear = curYear,
                currentMonth = curMonth,
                currentDay = curDay,
                availableYears = availableYears,
                onSelected = { year, month, day ->
                    val ts = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, month - 1)
                        set(Calendar.DAY_OF_MONTH, day)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    onSelect(ts)
                },
                onDismiss = onDismiss
            )
        }
        Granularity.QUARTER -> {
            QuarterPickerForTrend(
                currentYear = curYear,
                currentMonth = curMonth,
                availableYears = availableYears,
                onSelected = { year, quarter ->
                    val ts = Calendar.getInstance().apply {
                        set(Calendar.YEAR, year)
                        set(Calendar.MONTH, (quarter - 1) * 3)
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    onSelect(ts)
                },
                onDismiss = onDismiss
            )
        }
    }
}

/** 趋势页专用：月份两步选择器（先选年再选月） */
@Composable
private fun YearMonthPickerForTrend(
    currentYear: Int,
    currentMonth: Int,
    availableYears: List<Int>,
    onSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentYear) }
    var showYearList by remember { mutableStateOf(false) }

    val years = remember(availableYears, currentYear) {
        val set = (availableYears + currentYear).toMutableSet()
        set.sortedDescending()
    }

    if (showYearList) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = null,
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(years) { year ->
                        val isSelected = year == selectedYear
                        Text(
                            text = "${year}年",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedYear = year; showYearList = false }
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF333333),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Text("取消", color = Color.Gray,
                    modifier = Modifier.clickable { showYearList = false }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${selectedYear}年", color = ChartBlue, fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { showYearList = true })
                }
            },
            text = {
                Column {
                    val months = listOf("1月", "2月", "3月", "4月", "5月", "6月",
                                        "7月", "8月", "9月", "10月", "11月", "12月")
                    for (row in 0..2) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for (col in 0..3) {
                                val monthIdx = row * 4 + col
                                val month = monthIdx + 1
                                val isSelected = selectedYear == currentYear && month == currentMonth
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) ChartBlue
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { onSelected(selectedYear, month) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        months[monthIdx],
                                        color = if (isSelected) Color.White
                                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Text("取消", color = Color.Gray,
                    modifier = Modifier.clickable(onClick = onDismiss).padding(horizontal = 12.dp, vertical = 8.dp))
            }
        )
    }
}

// ========== 趋势页：日选择器（年 → 月 → 日历） ==========

@Composable
private fun DayPickerForTrend(
    currentYear: Int,
    currentMonth: Int,
    currentDay: Int,
    availableYears: List<Int>,
    onSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDay by remember { mutableStateOf(currentDay) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val years = remember(availableYears, currentYear) {
        val set = (availableYears + currentYear).toMutableSet()
        set.sortedDescending().take(10)
    }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth - 1, 1)
        }
        Pair(cal.get(Calendar.DAY_OF_WEEK), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    }
    val (firstDayOfWeek, totalDays) = daysInMonth

    // 年份选择弹窗
    if (showYearPicker) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = null,
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(years) { year ->
                        val isSelected = year == selectedYear
                        Text(
                            text = "${year}年",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedYear = year; showYearPicker = false }
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF333333),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Text("取消", color = Color.Gray,
                    modifier = Modifier.clickable { showYearPicker = false }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        )
        return
    }

    // 月份选择弹窗
    if (showMonthPicker) {
        val months = listOf("1月", "2月", "3月", "4月", "5月", "6月",
                            "7月", "8月", "9月", "10月", "11月", "12月")
        AlertDialog(
            onDismissRequest = onDismiss,
            title = null,
            text = {
                Column {
                    for (row in 0..2) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for (col in 0..3) {
                                val monthIdx = row * 4 + col
                                val month = monthIdx + 1
                                val isSelected = selectedYear == currentYear && month == currentMonth
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ChartBlue else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedMonth = month; showMonthPicker = false }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(months[monthIdx],
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Text("取消", color = Color.Gray,
                    modifier = Modifier.clickable { showMonthPicker = false }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        )
        return
    }

    // 日历主界面
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // 年月导航行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedMonth == 1) { selectedYear--; selectedMonth = 12 }
                        else selectedMonth--
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上月")
                    }
                    Text(
                        "${selectedYear}年",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ChartBlue,
                        modifier = Modifier.clickable { showYearPicker = true }.padding(horizontal = 4.dp)
                    )
                    Text(
                        "${selectedMonth}月",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ChartBlue,
                        modifier = Modifier.clickable { showMonthPicker = true }.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = {
                        if (selectedMonth == 12) { selectedYear++; selectedMonth = 1 }
                        else selectedMonth++
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下月")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 星期头
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                        Text(it, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }

                // 日期网格
                var dayCounter = 1
                for (row in 0..5) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            if (row == 0 && col < firstDayOfWeek - 1 || dayCounter > totalDays) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                val day = dayCounter
                                val isToday = selectedYear == currentYear && selectedMonth == currentMonth && day == currentDay
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isToday) ChartBlue else Color.Transparent)
                                        .clickable { selectedDay = day; onSelected(selectedYear, selectedMonth, day) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$day",
                                        fontSize = 13.sp,
                                        color = when {
                                            isToday -> Color.White
                                            col == 0 || col == 6 -> ExpenseRed
                                            else -> Color(0xFF333333)
                                        },
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal)
                                }
                                dayCounter++
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Text("取消", color = Color.Gray,
                modifier = Modifier.clickable(onClick = onDismiss).padding(horizontal = 12.dp, vertical = 8.dp))
        }
    )
}

// ========== 趋势页：周选择器（日历形式，选日期后定位到该周周一） ==========

@Composable
private fun WeekPickerForTrend(
    currentYear: Int,
    currentMonth: Int,
    currentDay: Int,
    availableYears: List<Int>,
    onSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDay by remember { mutableStateOf(currentDay) }
    var showYearPicker by remember { mutableStateOf(false) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val years = remember(availableYears, currentYear) {
        val set = (availableYears + currentYear).toMutableSet()
        set.sortedDescending().take(10)
    }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth - 1, 1)
        }
        Pair(cal.get(Calendar.DAY_OF_WEEK), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    }
    val (firstDayOfWeek, totalDays) = daysInMonth

    // 年份选择弹窗
    if (showYearPicker) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = null,
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(years) { year ->
                        val isSelected = year == selectedYear
                        Text(
                            text = "${year}年",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedYear = year; showYearPicker = false }
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF333333),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Text("取消", color = Color.Gray,
                    modifier = Modifier.clickable { showYearPicker = false }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        )
        return
    }

    // 月份选择弹窗
    if (showMonthPicker) {
        val months = listOf("1月", "2月", "3月", "4月", "5月", "6月",
                            "7月", "8月", "9月", "10月", "11月", "12月")
        AlertDialog(
            onDismissRequest = onDismiss,
            title = null,
            text = {
                Column {
                    for (row in 0..2) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            for (col in 0..3) {
                                val monthIdx = row * 4 + col
                                val month = monthIdx + 1
                                val isSelected = selectedYear == currentYear && month == currentMonth
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(4.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) ChartBlue else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { selectedMonth = month; showMonthPicker = false }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(months[monthIdx],
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Text("取消", color = Color.Gray,
                    modifier = Modifier.clickable { showMonthPicker = false }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        )
        return
    }

    // 日历主界面（与 DayPickerForTrend 相同，但点击日期后算出该日所在周的周一）
    // 当前周所属的周一（用于高亮）
    val currentMondayTs = remember(currentYear, currentMonth, currentDay) {
        val cal = Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, currentDay)
            val dow = get(Calendar.DAY_OF_WEEK)
            val offset = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
            add(Calendar.DAY_OF_MONTH, offset)
        }
        cal.timeInMillis
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                // 年月导航行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        if (selectedMonth == 1) { selectedYear--; selectedMonth = 12 }
                        else selectedMonth--
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "上月")
                    }
                    Text(
                        "${selectedYear}年",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ChartBlue,
                        modifier = Modifier.clickable { showYearPicker = true }.padding(horizontal = 4.dp)
                    )
                    Text(
                        "${selectedMonth}月",
                        fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ChartBlue,
                        modifier = Modifier.clickable { showMonthPicker = true }.padding(horizontal = 4.dp)
                    )
                    IconButton(onClick = {
                        if (selectedMonth == 12) { selectedYear++; selectedMonth = 1 }
                        else selectedMonth++
                    }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "下月")
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 星期头
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach {
                        Text(it, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                    }
                }

                // 日期网格（周一排第一列）
                val startOffset = if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
                var dayCounter = 1
                for (row in 0..5) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (col in 0..6) {
                            if (row == 0 && col < startOffset || dayCounter > totalDays) {
                                Spacer(Modifier.weight(1f))
                            } else {
                                val day = dayCounter
                                // 该日所在周的周一
                                val thisDayMondayTs = Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth - 1, day)
                                    val dow = get(Calendar.DAY_OF_WEEK)
                                    val offset = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
                                    add(Calendar.DAY_OF_MONTH, offset)
                                }.timeInMillis
                                val isInCurrentWeek = thisDayMondayTs == currentMondayTs
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(2.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isInCurrentWeek) ChartBlue else Color.Transparent)
                                        .clickable {
                                            // 算出该日所在周的周一
                                            val monCal = Calendar.getInstance().apply {
                                                set(selectedYear, selectedMonth - 1, day)
                                                val dow = get(Calendar.DAY_OF_WEEK)
                                                val offset = if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow
                                                add(Calendar.DAY_OF_MONTH, offset)
                                            }
                                            onSelected(monCal.get(Calendar.YEAR), monCal.get(Calendar.MONTH) + 1, monCal.get(Calendar.DAY_OF_MONTH))
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$day",
                                        fontSize = 13.sp,
                                        color = when {
                                            isInCurrentWeek -> Color.White
                                            col == 5 || col == 6 -> ExpenseRed
                                            else -> Color(0xFF333333)
                                        },
                                        fontWeight = if (isInCurrentWeek) FontWeight.Bold else FontWeight.Normal)
                                }
                                dayCounter++
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Text("取消", color = Color.Gray,
                modifier = Modifier.clickable(onClick = onDismiss).padding(horizontal = 12.dp, vertical = 8.dp))
        }
    )
}

// ========== 趋势页：季度选择器（年 → Q1/Q2/Q3/Q4） ==========

@Composable
private fun QuarterPickerForTrend(
    currentYear: Int,
    currentMonth: Int,
    availableYears: List<Int>,
    onSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentYear) }
    var showYearPicker by remember { mutableStateOf(false) }
    val currentQuarter = (currentMonth - 1) / 3 + 1

    val years = remember(availableYears, currentYear) {
        val set = (availableYears + currentYear).toMutableSet()
        set.sortedDescending().take(10)
    }

    if (showYearPicker) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = null,
            text = {
                LazyColumn(modifier = Modifier.heightIn(max = 280.dp)) {
                    items(years) { year ->
                        val isSelected = year == selectedYear
                        Text(
                            text = "${year}年",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedYear = year; showYearPicker = false }
                                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else Color(0xFF333333),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            },
            confirmButton = {
                Text("取消", color = Color.Gray,
                    modifier = Modifier.clickable { showYearPicker = false }.padding(horizontal = 12.dp, vertical = 8.dp))
            }
        )
        return
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("${selectedYear}年", color = ChartBlue, fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable { showYearPicker = true })
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (q in 1..4) {
                    val isSelected = selectedYear == currentYear && q == currentQuarter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) ChartBlue else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSelected(selectedYear, q) }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Q$q",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF333333))
                            Text(
                                when (q) {
                                    1 -> "1-3月"
                                    2 -> "4-6月"
                                    3 -> "7-9月"
                                    4 -> "10-12月"
                                    else -> ""
                                },
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.7f) else Color.Gray)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Text("取消", color = Color.Gray,
                modifier = Modifier.clickable(onClick = onDismiss).padding(horizontal = 12.dp, vertical = 8.dp))
        }
    )
}
