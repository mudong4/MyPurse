package com.wyd.mypurse.ui.transactionlist

import com.wyd.mypurse.domain.model.Transaction

/**
 * 时间粒度。
 */
enum class TimeGranularity(val label: String) {
    DAY("日"),
    WEEK("周"),
    MONTH("月"),
    YEAR("年");

    companion object {
        fun fromString(value: String): TimeGranularity = when (value) {
            "day" -> DAY
            "week" -> WEEK
            "year" -> YEAR
            else -> MONTH
        }
    }
}

/**
 * 分组模式。
 * - TIME：按时间维度分组（年→月、月→周、周→天、天→平铺）
 * - CATEGORY_L1：按一级分类分组
 * - CATEGORY_L2：按二级分类分组
 */
enum class GroupMode(val label: String) {
    TIME("时间"),
    CATEGORY_L1("一级分类"),
    CATEGORY_L2("二级分类")
}

/**
 * 按时间分组的交易数据，支持折叠/展开。
 * - 年视图(TIME)：每个 group 是一个月，展开后显示该月交易
 * - 月视图(TIME)：每个 group 是一周，展开后显示该周交易
 * - 周视图(TIME)：每个 group 是一天，展开后显示当天交易
 * - 日视图(TIME)：直接平铺交易明细（不折叠）
 * - 分类模式：每个 group 是一个分类，展开后显示该分类下的交易
 */
data class TimeGroup(
    val label: String,           // 显示标题
    val rangeStart: Long,
    val rangeEnd: Long,
    val totalExpense: java.math.BigDecimal,
    val totalIncome: java.math.BigDecimal,
    val transactions: List<Transaction>
)

/**
 * 流水列表页 UI 状态。
 */
data class TransactionListUiState(
    val isLoading: Boolean = true,
    val granularity: TimeGranularity = TimeGranularity.MONTH,
    val groupMode: GroupMode = GroupMode.TIME,
    val searchKeyword: String = "",

    // 当前时间窗口
    val currentYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR),
    val currentMonth: Int = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1,
    val currentDay: Int = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_MONTH),

    // 周：当前是年第几周（用于周导航）
    val currentWeekOfYear: Int = java.util.Calendar.getInstance().get(java.util.Calendar.WEEK_OF_YEAR),

    // 周视图的日期范围标签（如 "6.15 - 6.21"），由 ViewModel 填充
    val weekDateRange: String = "",

    // 列表数据（年/月/周/日共用 timeGroups）
    val timeGroups: List<TimeGroup> = emptyList(),

    // 折叠状态：存储已展开的 group 的 rangeStart 字符串
    val expandedGroups: Set<String> = emptySet(),

    // 是否有更多数据（仅日视图 TIME 模式分页使用）
    val hasMore: Boolean = false,
    // 是否在搜索模式
    val isSearching: Boolean = false,

    // 日历选择器
    val showDatePicker: Boolean = false,

    val errorMessage: String? = null
)
