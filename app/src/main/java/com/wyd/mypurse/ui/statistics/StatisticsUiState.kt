package com.wyd.mypurse.ui.statistics

import com.wyd.mypurse.domain.model.CategoryAmount
import com.wyd.mypurse.domain.model.TrendPoint
import com.wyd.mypurse.domain.usecase.GetStatisticsUseCase.Granularity
import java.math.BigDecimal

/**
 * 统计页 UI 状态。
 */
data class StatisticsUiState(
    val granularity: Granularity = Granularity.MONTH,
    val currentTimestamp: Long = System.currentTimeMillis(),
    val timeLabel: String = "",

    // 当前视图的时间范围（毫秒），供跳转流水列表时传递时间上下文
    val timeRangeStart: Long = 0L,
    val timeRangeEnd: Long = 0L,

    // 概览
    val totalExpense: BigDecimal = BigDecimal.ZERO,
    val totalIncome: BigDecimal = BigDecimal.ZERO,

    // 构成分析
    val composition: List<CategoryAmount> = emptyList(),
    val incomeComposition: List<CategoryAmount> = emptyList(),
    val isShowingIncome: Boolean = false,
    val expandedCategoryId: Long? = null,
    val subComposition: List<CategoryAmount> = emptyList(),
    val isSubLoading: Boolean = false,

    // 趋势分析
    val trend: List<TrendPoint> = emptyList(),

    // 页面模式
    val pageMode: PageMode = PageMode.CATEGORY,
    val compositionChartMode: ChartMode = ChartMode.BAR,   // 默认条形图
    val trendChartMode: ChartMode = ChartMode.BAR,          // 默认柱状图

    // 数据库中实际有数据的年份（降序），供时间选择器使用
    val availableYears: List<Int> = emptyList(),

    // 预算（用于趋势图参考线）
    val budget: BigDecimal? = null,

    val isLoading: Boolean = true
)

/** 统计页子页面 */
enum class PageMode(val label: String) {
    CATEGORY("分类"),
    TREND("趋势");
}

/** 图表显示模式 */
enum class ChartMode(val label: String, val iconLabel: String) {
    BAR("条形图", "▊"),
    DONUT("环形图", "◉"),
    LINE("折线图", "╱");
}
