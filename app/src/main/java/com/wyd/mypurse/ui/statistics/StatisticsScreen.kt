package com.wyd.mypurse.ui.statistics

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.wyd.mypurse.domain.model.CategoryAmount
import com.wyd.mypurse.domain.model.TrendPoint
import com.wyd.mypurse.domain.usecase.GetStatisticsUseCase.Granularity
import com.wyd.mypurse.ui.components.EmptyStateText
import com.wyd.mypurse.ui.components.WheelPicker
import com.wyd.mypurse.ui.components.YearMonthWheelSheet
import com.wyd.mypurse.ui.components.YearWheelSheet
import com.wyd.mypurse.ui.components.rememberDebounce
import com.wyd.mypurse.ui.theme.AppBarChartBg
import com.wyd.mypurse.ui.theme.AppBudgetBlue
import com.wyd.mypurse.ui.theme.AppCornerMedium
import com.wyd.mypurse.ui.theme.AppCornerSmall
import com.wyd.mypurse.ui.theme.AppDivider
import com.wyd.mypurse.ui.theme.AppExpenseRed
import com.wyd.mypurse.ui.theme.AppIncomeGreen
import com.wyd.mypurse.ui.theme.AppSheetBg
import com.wyd.mypurse.ui.theme.AppSpacingLg
import com.wyd.mypurse.ui.theme.AppSpacingMd
import com.wyd.mypurse.ui.theme.AppSpacingSm
import com.wyd.mypurse.ui.theme.AppSurfaceBg
import com.wyd.mypurse.ui.theme.DefaultChartColors
import com.wyd.mypurse.ui.theme.categoryColor
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.Calendar

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
    val debounce = rememberDebounce()
    val useCase = viewModel // 通过 ViewModel 引用 UseCase 的方法
    // 实际上 ViewModel 没有直接暴露 UseCase，我们改用函数引用
    val labelStep = if (uiState.trend.isNotEmpty()) {
        when {
            uiState.trend.size <= 4 -> 1
            uiState.trend.size <= 8 -> 1
            uiState.trend.size <= 10 -> 2
            uiState.trend.size <= 15 -> 3
            uiState.trend.size <= 20 -> 4
            uiState.trend.size <= 31 -> 5
            else -> 8
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
                    containerColor = MaterialTheme.colorScheme.background
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
                onGranularityChange = { debounce { viewModel.setGranularity(it) } },
                onNavigateTime = { direction -> debounce { viewModel.navigateTime(direction) } },
                onJumpToTimestamp = { ts -> debounce { viewModel.jumpToTimestamp(ts) } },
                onTogglePageMode = { debounce { viewModel.togglePageMode() } }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
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
        shape = RoundedCornerShape(AppCornerMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                            selectedContainerColor = AppExpenseRed.copy(alpha = 0.12f),
                            selectedLabelColor = AppExpenseRed
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    FilterChip(
                        selected = isShowingIncome,
                        onClick = { if (!isShowingIncome) onToggleType() },
                        label = { Text("收入", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AppIncomeGreen.copy(alpha = 0.12f),
                            selectedLabelColor = AppIncomeGreen
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
                        color = DefaultChartColors.chartPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (composition.isEmpty()) {
                EmptyStateText(
                    message = "暂无数据",
                    modifier = Modifier.fillMaxWidth()
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
                        isIncome = isShowingIncome,
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
        shape = RoundedCornerShape(AppCornerMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("支出", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "-¥${df.format(totalExpense)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppExpenseRed
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("收入", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "+¥${df.format(totalIncome)}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppIncomeGreen
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
    isIncome: Boolean = false,
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
                    color = categoryColor(item.color)
                        ?: DefaultChartColors.chartPalette[index % DefaultChartColors.chartPalette.size],
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

        // 中间文字：显示总金额
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "¥${df.format(total)}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "总${if (isIncome) "收入" else "支出"}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    // 图例
    Spacer(modifier = Modifier.height(8.dp))
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        items.forEachIndexed { index, item ->
            val pct = if (total > BigDecimal.ZERO) {
                item.total.multiply(BigDecimal("100")).divide(total, 1, RoundingMode.HALF_UP)
            } else BigDecimal.ZERO
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(color = categoryColor(item.color)
                            ?: DefaultChartColors.chartPalette[index % DefaultChartColors.chartPalette.size])
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = item.categoryL1,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f).padding(end = 4.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "¥${df.format(item.total)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${dfPct.format(pct)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(36.dp),
                    textAlign = TextAlign.End
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
        val color = categoryColor(item.color)
            ?: DefaultChartColors.chartPalette[index % DefaultChartColors.chartPalette.size]

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
                    fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val subTotal = subComposition.sumOf { it.total }
                val parentTotal = items.find { it.categoryL1Id == expandedCategoryId }?.total ?: subTotal
                Column(modifier = Modifier.padding(start = 24.dp)) {
                    subComposition.forEach { sub ->
                        val subPct = if (parentTotal > BigDecimal.ZERO) {
                            sub.total.divide(parentTotal, 4, RoundingMode.HALF_UP)
                                .toFloat().coerceAtLeast(0.02f)
                        } else 0f
                        val subPctText = if (parentTotal > BigDecimal.ZERO) {
                            sub.total.multiply(BigDecimal("100")).divide(parentTotal, 1, RoundingMode.HALF_UP)
                        } else BigDecimal.ZERO
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sub.categoryL1,
                                fontSize = 11.sp,
                                modifier = Modifier.width(52.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${dfPct.format(subPctText)}%",
                                fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
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
                modifier = Modifier.width(60.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 彩色条形（纯色，无文字覆盖，参考首页排行榜样式）
            val barFraction = pct.coerceIn(0.02f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(AppBarChartBg)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(barFraction)
                        .clip(RoundedCornerShape(6.dp))
                        .background(color)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 金额 + 百分比（在 bar 右侧，用主题文字色，浅色/深色模式自适应）
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "¥${df.format(amount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${dfPct.format(pctText)}%",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
        shape = RoundedCornerShape(AppCornerMedium),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
                        color = DefaultChartColors.chartPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (trend.isEmpty()) {
                EmptyStateText(
                    message = "暂无数据",
                    modifier = Modifier.fillMaxWidth()
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
                        color = DefaultChartColors.chartAccent
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

    // 动画：每个柱子的高度比例
    val animatedFractions = trend.mapIndexed { index, point ->
        val target = point.total.divide(maxAmount, 4, RoundingMode.HALF_UP)
            .toFloat().coerceIn(0.02f, 1f)
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 300),
            label = "barFrac_$index"
        ).value
    }

    Canvas(
        modifier = modifier
            .pointerInput(trend) {
                detectTapGestures { offset ->
                    if (trend.isEmpty()) return@detectTapGestures
                    val leftPad = 48.dp.toPx()
                    val drawW = size.width - leftPad
                    val barSpacing = drawW / (trend.size + 1)
                    val index = (((offset.x - leftPad) / barSpacing).toInt()).coerceIn(0, trend.size - 1)
                    onSelectIndex(if (index == selectedIndex) null else index)
                }
            }
    ) {
        if (trend.isEmpty() || maxAmount <= BigDecimal.ZERO) return@Canvas
        val chartW = size.width
        val chartH = size.height
        val leftPad = 48.dp.toPx()
        val bottomPad = 28.dp.toPx()
        val topPad = 12.dp.toPx()
        val drawW = chartW - leftPad - 4.dp.toPx() // 右侧留少许间距
        val drawH = chartH - bottomPad - topPad

        val barCount = trend.size
        val barSpacing = drawW / (barCount + 1)
        val barW = (barSpacing * 0.6f).coerceAtMost(24.dp.toPx())

        // === Y 轴：3 条参考线 (0%, 50%, 100%) ===
        val yTickCount = 3
        val gridLineColor = Color(0x22000000)
        for (i in 0..yTickCount) {
            val frac = i.toFloat() / yTickCount
            val y = topPad + drawH * (1 - frac)
            // 虚线参考线
            drawLine(
                color = gridLineColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + drawW, y),
                strokeWidth = 0.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )
            // Y 轴刻度标签（用整数运算避免浮点精度问题）
            val amount = maxAmount.multiply(BigDecimal(i)).divide(BigDecimal(yTickCount), 0, RoundingMode.HALF_UP)
            drawContext.canvas.nativeCanvas.drawText(
                if (amount <= BigDecimal.ZERO) "0" else "¥${df.format(amount)}",
                leftPad - 4.dp.toPx(),
                y + 3.dp.toPx(),
                android.graphics.Paint().apply {
                    color = 0xFF888888.toInt()
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
        }

        // 画柱子
        trend.forEachIndexed { index, point ->
            val x = leftPad + barSpacing * (index + 1) - barW / 2
            val hFrac = animatedFractions[index]
            val barH = drawH * hFrac
            val y = topPad + drawH - barH

            val color = if (index == selectedIndex) DefaultChartColors.chartAccent else DefaultChartColors.chartPrimary
            drawRect(color = color, topLeft = Offset(x, y), size = Size(barW, barH))
        }

        // 选中竖线
        if (selectedIndex != null && selectedIndex < trend.size) {
            val lineX = leftPad + barSpacing * (selectedIndex + 1)
            drawLine(
                color = DefaultChartColors.chartAccent.copy(alpha = 0.5f),
                start = Offset(lineX, topPad),
                end = Offset(lineX, topPad + drawH),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // X 轴标签
        trend.forEachIndexed { index, point ->
            if (index % labelStep == 0) {
                val x = leftPad + barSpacing * (index + 1)
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

    // 动画：每个数据点的高度比例
    val animatedFractions = trend.mapIndexed { index, point ->
        val target = point.total.divide(maxAmount, 4, RoundingMode.HALF_UP)
            .toFloat().coerceIn(0f, 1f)
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 300),
            label = "lineFrac_$index"
        ).value
    }

    Canvas(
        modifier = modifier
            .pointerInput(trend) {
                detectTapGestures { offset ->
                    if (trend.isEmpty()) return@detectTapGestures
                    val leftPad = 48.dp.toPx()
                    val drawW = size.width - leftPad
                    val barSpacing = drawW / (trend.size + 1)
                    val index = (((offset.x - leftPad) / barSpacing).toInt()).coerceIn(0, trend.size - 1)
                    onSelectIndex(if (index == selectedIndex) null else index)
                }
            }
    ) {
        if (trend.size < 2 || maxAmount <= BigDecimal.ZERO) return@Canvas
        val chartW = size.width
        val chartH = size.height
        val leftPad = 48.dp.toPx()
        val bottomPad = 28.dp.toPx()
        val topPad = 12.dp.toPx()
        val drawW = chartW - leftPad - 4.dp.toPx()
        val drawH = chartH - bottomPad - topPad

        val barSpacing = drawW / (trend.size + 1)

        // === Y 轴：3 条参考线 ===
        val yTickCount = 3
        val gridLineColor = Color(0x22000000)
        for (i in 0..yTickCount) {
            val frac = i.toFloat() / yTickCount
            val y = topPad + drawH * (1 - frac)
            drawLine(
                color = gridLineColor,
                start = Offset(leftPad, y),
                end = Offset(leftPad + drawW, y),
                strokeWidth = 0.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
            )
            val amount = maxAmount.multiply(BigDecimal(i)).divide(BigDecimal(yTickCount), 0, RoundingMode.HALF_UP)
            drawContext.canvas.nativeCanvas.drawText(
                if (amount <= BigDecimal.ZERO) "0" else "¥${df.format(amount)}",
                leftPad - 4.dp.toPx(),
                y + 3.dp.toPx(),
                android.graphics.Paint().apply {
                    color = 0xFF888888.toInt()
                    textSize = 9.sp.toPx()
                    textAlign = android.graphics.Paint.Align.RIGHT
                    isAntiAlias = true
                }
            )
        }

        // 填充区域
        val fillPath = Path()
        trend.forEachIndexed { index, _ ->
            val x = leftPad + barSpacing * (index + 1)
            val hFrac = animatedFractions[index]
            val y = topPad + drawH * (1 - hFrac)
            if (index == 0) {
                fillPath.moveTo(x, topPad + drawH)
                fillPath.lineTo(x, y)
            } else {
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(leftPad + barSpacing * trend.size, topPad + drawH)
        fillPath.close()
        drawPath(path = fillPath, color = DefaultChartColors.chartPrimary.copy(alpha = 0.1f))

        // 折线
        val linePath = Path()
        trend.forEachIndexed { index, _ ->
            val x = leftPad + barSpacing * (index + 1)
            val hFrac = animatedFractions[index]
            val y = topPad + drawH * (1 - hFrac)
            if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
        }
        drawPath(path = linePath, color = DefaultChartColors.chartPrimary, style = Stroke(width = 2.5.dp.toPx()))

        // 数据点
        trend.forEachIndexed { index, _ ->
            val x = leftPad + barSpacing * (index + 1)
            val hFrac = animatedFractions[index]
            val y = topPad + drawH * (1 - hFrac)
            val isSelected = index == selectedIndex
            drawCircle(
                color = if (isSelected) DefaultChartColors.chartAccent else DefaultChartColors.chartPrimary,
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
            val lineX = leftPad + barSpacing * (selectedIndex + 1)
            drawLine(
                color = DefaultChartColors.chartAccent.copy(alpha = 0.4f),
                start = Offset(lineX, topPad),
                end = Offset(lineX, topPad + drawH),
                strokeWidth = 1.5.dp.toPx()
            )
        }

        // X 轴标签
        trend.forEachIndexed { index, point ->
            if (index % labelStep == 0) {
                val x = leftPad + barSpacing * (index + 1)
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
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "合计 ¥${df.format(total)}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // 三项指标
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SummaryItem("均值", "¥${df.format(avg)}", DefaultChartColors.chartPrimary)
        SummaryItem("最高", "¥${df.format(maxPoint.total)}", AppExpenseRed)
        SummaryItem("最低", "¥${df.format(minPoint.total)}", AppIncomeGreen)
    }
}

@Composable
private fun SummaryItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        HorizontalDivider(color = AppDivider)
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                                    color = if (g == granularity) DefaultChartColors.chartPrimary
                                    else MaterialTheme.colorScheme.onSurface
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
                    color = DefaultChartColors.chartPrimary,
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
                color = DefaultChartColors.chartPrimary,
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

// ========== 时间选择弹窗（底部 Sheet 样式） ==========

private val SheetBg = AppSheetBg

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

    val years = remember(availableYears, curYear) {
        val set = (availableYears + curYear).toMutableSet()
        set.sortedDescending().take(10)
    }

    when (granularity) {
        Granularity.YEAR -> YearWheelSheet(
            years = years,
            selectedYear = curYear,
            highlightColor = DefaultChartColors.chartPrimary.copy(alpha = 0.1f),
            selectedTextColor = DefaultChartColors.chartPrimary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            backgroundColor = SheetBg,
            confirmColor = DefaultChartColors.chartPrimary,
            onConfirm = { onSelect(makeTs(it, 1, 1)) },
            onDismiss = onDismiss
        )
        Granularity.MONTH -> YearMonthWheelSheet(
            yearList = years,
            monthList = (1..12).toList(),
            currentYear = curYear,
            currentMonth = curMonth,
            title = { Text("${curYear}年${curMonth}月", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DefaultChartColors.chartPrimary, modifier = Modifier.padding(top = 16.dp)) },
            titleColor = DefaultChartColors.chartPrimary,
            highlightColor = DefaultChartColors.chartPrimary.copy(alpha = 0.1f),
            selectedTextColor = DefaultChartColors.chartPrimary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            backgroundColor = SheetBg,
            confirmColor = DefaultChartColors.chartPrimary,
            onConfirm = { year, month ->
                onSelect(makeTs(year, month, 1))
            },
            onDismiss = onDismiss
        )
        Granularity.DAY -> CalendarSheet(
            currentYear = curYear,
            currentMonth = curMonth,
            currentDay = curDay,
            years = years,
            weekMode = false,
            onSelected = { year, month, day ->
                onSelect(makeTs(year, month, day))
            },
            onDismiss = onDismiss
        )
        Granularity.WEEK -> CalendarSheet(
            currentYear = curYear,
            currentMonth = curMonth,
            currentDay = curDay,
            years = years,
            weekMode = true,
            onSelected = { year, month, day ->
                onSelect(makeTs(year, month, day))
            },
            onDismiss = onDismiss
        )
        Granularity.QUARTER -> QuarterSheet(
            currentYear = curYear,
            currentMonth = curMonth,
            years = years,
            onSelected = { year, quarter ->
                onSelect(makeTs(year, (quarter - 1) * 3 + 1, 1))
            },
            onDismiss = onDismiss
        )
    }
}

private fun makeTs(year: Int, month: Int, day: Int): Long {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month - 1)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}

// ========== 日/周模式：底部 Sheet 日历 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CalendarSheet(
    currentYear: Int,
    currentMonth: Int,
    currentDay: Int,
    years: List<Int>,
    weekMode: Boolean,
    onSelected: (Int, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth) }
    var selectedDay by remember { mutableStateOf(currentDay) }
    var showYearMonthPicker by remember { mutableStateOf(false) }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        val cal = Calendar.getInstance().apply {
            set(selectedYear, selectedMonth - 1, 1)
        }
        Pair(cal.get(Calendar.DAY_OF_WEEK), cal.getActualMaximum(Calendar.DAY_OF_MONTH))
    }
    val (firstDayOfWeek, totalDays) = daysInMonth

    // 周模式：当前周一的 ts
    val currentMondayTs = if (weekMode) remember(currentYear, currentMonth, currentDay) {
        Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, currentDay)
            val dow = get(Calendar.DAY_OF_WEEK)
            add(Calendar.DAY_OF_MONTH, if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow)
        }.timeInMillis
    } else 0L

    // 年月并排双滚轮选择（覆盖在日历上方）
    if (showYearMonthPicker) {
        YearMonthWheelSheet(
            yearList = years,
            monthList = (1..12).toList(),
            currentYear = selectedYear,
            currentMonth = selectedMonth,
            highlightColor = DefaultChartColors.chartPrimary.copy(alpha = 0.1f),
            selectedTextColor = DefaultChartColors.chartPrimary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            backgroundColor = SheetBg,
            confirmColor = DefaultChartColors.chartPrimary,
            onConfirm = { y, m ->
                selectedYear = y
                selectedMonth = m
                showYearMonthPicker = false
            },
            onDismiss = { showYearMonthPicker = false }
        )
        return
    }

    // 主日历 Sheet
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
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
                    "${selectedYear}年${selectedMonth}月",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = DefaultChartColors.chartPrimary,
                    modifier = Modifier.clickable { showYearMonthPicker = true }.padding(horizontal = 4.dp)
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
            val weekHeaders = if (weekMode) listOf("一", "二", "三", "四", "五", "六", "日")
            else listOf("日", "一", "二", "三", "四", "五", "六")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekHeaders.forEach {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(4.dp))

            // 日期网格
            val startOffset = if (weekMode) {
                if (firstDayOfWeek == Calendar.SUNDAY) 6 else firstDayOfWeek - 2
            } else {
                firstDayOfWeek - 1
            }
            var dayCounter = 1
            for (row in 0..5) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0..6) {
                        if (row == 0 && col < startOffset || dayCounter > totalDays) {
                            Spacer(Modifier.weight(1f))
                        } else {
                            val day = dayCounter
                            val isCurrent = selectedYear == currentYear &&
                                    selectedMonth == currentMonth && day == currentDay
                            val isInWeek = if (weekMode) {
                                Calendar.getInstance().apply {
                                    set(selectedYear, selectedMonth - 1, day)
                                    val dow = get(Calendar.DAY_OF_WEEK)
                                    add(Calendar.DAY_OF_MONTH, if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow)
                                }.timeInMillis == currentMondayTs
                            } else false

                            val highlight = if (weekMode) isInWeek else isCurrent
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (highlight) DefaultChartColors.chartPrimary else Color.Transparent)
                                    .clickable {
                                        selectedDay = day
                                        if (weekMode) {
                                            val monCal = Calendar.getInstance().apply {
                                                set(selectedYear, selectedMonth - 1, day)
                                                val dow = get(Calendar.DAY_OF_WEEK)
                                                add(Calendar.DAY_OF_MONTH, if (dow == Calendar.SUNDAY) -6 else Calendar.MONDAY - dow)
                                            }
                                            onSelected(monCal.get(Calendar.YEAR), monCal.get(Calendar.MONTH) + 1, monCal.get(Calendar.DAY_OF_MONTH))
                                        } else {
                                            onSelected(selectedYear, selectedMonth, day)
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("$day",
                                    fontSize = 13.sp,
                                    color = when {
                                        highlight -> Color.White
                                        !weekMode && (col == 0 || col == 6) -> AppExpenseRed
                                        weekMode && (col == 5 || col == 6) -> AppExpenseRed
                                        else -> MaterialTheme.colorScheme.onSurface
                                    },
                                    fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal)
                            }
                            dayCounter++
                        }
                    }
                }
                if (dayCounter > totalDays) break
            }
        }
    }
}

// ========== 季模式：底部 Sheet 年份 + Q1~Q4 ==========

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuarterSheet(
    currentYear: Int,
    currentMonth: Int,
    years: List<Int>,
    onSelected: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentYear) }
    var showYearPicker by remember { mutableStateOf(false) }
    val currentQuarter = (currentMonth - 1) / 3 + 1

    if (showYearPicker) {
        YearWheelSheet(
            years = years,
            selectedYear = selectedYear,
            highlightColor = DefaultChartColors.chartPrimary.copy(alpha = 0.1f),
            selectedTextColor = DefaultChartColors.chartPrimary,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            backgroundColor = SheetBg,
            confirmColor = DefaultChartColors.chartPrimary,
            onConfirm = { selectedYear = it; showYearPicker = false },
            onDismiss = { showYearPicker = false }
        )
        return
    }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SheetBg,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            Text(
                "${selectedYear}年",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DefaultChartColors.chartPrimary,
                modifier = Modifier
                    .clickable { showYearPicker = true }
                    .padding(horizontal = 12.dp, vertical = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (q in 1..4) {
                    val isSelected = selectedYear == currentYear && q == currentQuarter
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(AppCornerMedium))
                            .background(if (isSelected) DefaultChartColors.chartPrimary else MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onSelected(selectedYear, q) }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Q$q", fontSize = 20.sp, fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface)
                            Text(
                                when (q) {
                                    1 -> "1-3月"; 2 -> "4-6月"; 3 -> "7-9月"; 4 -> "10-12月"
                                    else -> ""
                                },
                                fontSize = 11.sp,
                                color = if (isSelected) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}









