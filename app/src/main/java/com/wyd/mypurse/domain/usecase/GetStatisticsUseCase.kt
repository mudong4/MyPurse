package com.wyd.mypurse.domain.usecase

import com.wyd.mypurse.domain.model.CategoryAmount
import com.wyd.mypurse.domain.model.TrendPoint
import com.wyd.mypurse.domain.repository.TransactionRepository
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 统计页数据聚合用例。
 * 根据时间粒度和当前时间，查询构成分析和趋势分析数据。
 */
@Singleton
class GetStatisticsUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    enum class Granularity(val label: String) {
        DAY("日"),
        WEEK("周"),
        MONTH("月"),
        QUARTER("季"),
        YEAR("年");
    }

    data class StatisticsSnapshot(
        val granularity: Granularity = Granularity.MONTH,
        val timeLabel: String = "",
        val rangeStart: Long = 0,
        val rangeEnd: Long = 0,
        val totalExpense: BigDecimal = BigDecimal.ZERO,
        val totalIncome: BigDecimal = BigDecimal.ZERO,
        val composition: List<CategoryAmount> = emptyList(),
        val incomeComposition: List<CategoryAmount> = emptyList(),
        val trend: List<TrendPoint> = emptyList(),
        val isLoading: Boolean = true
    )

    /** 各粒度趋势查询的最大回溯范围 */
    private fun maxLookback(granularity: Granularity, timestamp: Long): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (granularity) {
            Granularity.DAY -> {
                cal.add(Calendar.DAY_OF_MONTH, -29) // 最多30天
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            Granularity.WEEK -> {
                cal.add(Calendar.WEEK_OF_YEAR, -7) // 最多8周
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            Granularity.MONTH -> {
                cal.add(Calendar.MONTH, -11) // 最多12个月
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            Granularity.QUARTER -> {
                cal.add(Calendar.MONTH, -21) // 最多8季度(24个月)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            Granularity.YEAR -> 0L // 全部数据
        }
    }

    fun calculateTimeRange(granularity: Granularity, timestamp: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val rangeStart: Long
        val rangeEnd: Long

        when (granularity) {
            Granularity.DAY -> {
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                rangeStart = cal.timeInMillis
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                rangeEnd = cal.timeInMillis
            }
            Granularity.WEEK -> {
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                rangeStart = cal.timeInMillis
                cal.add(Calendar.DAY_OF_MONTH, 6)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                rangeEnd = cal.timeInMillis
            }
            Granularity.MONTH -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                rangeStart = cal.timeInMillis
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                rangeEnd = cal.timeInMillis
            }
            Granularity.QUARTER -> {
                val month = cal.get(Calendar.MONTH)
                cal.set(Calendar.MONTH, (month / 3) * 3)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                rangeStart = cal.timeInMillis
                cal.add(Calendar.MONTH, 2)
                cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                rangeEnd = cal.timeInMillis
            }
            Granularity.YEAR -> {
                cal.set(Calendar.MONTH, Calendar.JANUARY)
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                rangeStart = cal.timeInMillis
                cal.set(Calendar.MONTH, Calendar.DECEMBER)
                cal.set(Calendar.DAY_OF_MONTH, 31)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                rangeEnd = cal.timeInMillis
            }
        }
        return Pair(rangeStart, rangeEnd)
    }

    fun formatTimeLabel(granularity: Granularity, timestamp: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        return when (granularity) {
            Granularity.DAY -> "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
            Granularity.WEEK -> {
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
                val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
                cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
                val mon = "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
                cal.add(Calendar.DAY_OF_MONTH, 6)
                val sun = "${cal.get(Calendar.MONTH) + 1}月${cal.get(Calendar.DAY_OF_MONTH)}日"
                "$mon - $sun"
            }
            Granularity.MONTH -> "${cal.get(Calendar.YEAR)}年${cal.get(Calendar.MONTH) + 1}月"
            Granularity.QUARTER -> {
                val q = (cal.get(Calendar.MONTH) / 3) + 1
                "${cal.get(Calendar.YEAR)}年Q$q"
            }
            Granularity.YEAR -> "${cal.get(Calendar.YEAR)}年"
        }
    }

    /** 加载统计页数据 */
    suspend fun loadData(
        granularity: Granularity,
        timestamp: Long
    ): StatisticsSnapshot {
        val (rangeStart, rangeEnd) = calculateTimeRange(granularity, timestamp)
        val timeLabel = formatTimeLabel(granularity, timestamp)

        // 构成数据
        val expenseComposition = repository.getCategoryComposition(rangeStart, rangeEnd, "支出")
        val incomeComposition = repository.getCategoryComposition(rangeStart, rangeEnd, "收入")
        val totalExpense = expenseComposition.sumOf { it.total }
        val totalIncome = incomeComposition.sumOf { it.total }

        // 趋势数据：自适应范围
        val trendGranularity = when (granularity) {
            Granularity.DAY -> "day"
            Granularity.WEEK -> "week"
            Granularity.MONTH -> "month"
            Granularity.QUARTER -> "quarter"
            Granularity.YEAR -> "year"
        }

        val trend = if (granularity == Granularity.YEAR) {
            repository.getYearlyExpenseTrend()
        } else {
            val trendStart = maxLookback(granularity, timestamp)
            repository.getExpenseTrend(trendGranularity, trendStart, rangeEnd)
        }

        return StatisticsSnapshot(
            granularity = granularity,
            timeLabel = timeLabel,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
            totalExpense = totalExpense,
            totalIncome = totalIncome,
            composition = expenseComposition,
            incomeComposition = incomeComposition,
            trend = trend,
            isLoading = false
        )
    }

    suspend fun loadSubComposition(
        rangeStart: Long,
        rangeEnd: Long,
        flowType: String?,
        categoryL1Id: Long
    ): List<CategoryAmount> {
        val subList = repository.getSubCategoryComposition(rangeStart, rangeEnd, flowType, categoryL1Id).toMutableList()
        val uncategorized = repository.getUncategorizedSubAmount(rangeStart, rangeEnd, flowType, categoryL1Id)
        if (uncategorized > BigDecimal.ZERO) {
            // 未选择二级分类的流水归入"未分类"行，排在最后
            subList.add(
                CategoryAmount(
                    categoryL1Id = -2L,
                    categoryL1 = "未分类",
                    total = uncategorized,
                    color = 0
                )
            )
        }
        return subList
    }

    /** 获取数据库中所有有交易记录的年份（降序） */
    suspend fun getAvailableYears(): List<Int> {
        return repository.getAvailableYears()
    }

    /** 趋势 X 轴标签步长：根据数据点数决定每 N 个显示一个 */
    fun labelStep(dataSize: Int): Int = when {
        dataSize <= 4 -> 1
        dataSize <= 8 -> 1
        dataSize <= 15 -> 2
        dataSize <= 20 -> 3
        else -> 5 // 日粒度 30 点或更多
    }

    /** 趋势图表 X 轴短标签（只显示月-日或月等） */
    fun shortLabel(point: TrendPoint, granularity: Granularity): String {
        val raw = point.label
        return when (granularity) {
            Granularity.DAY -> {
                // raw = "YYYY-MM-DD" -> "MM-DD"
                if (raw.length >= 10) raw.substring(5) else raw
            }
            Granularity.WEEK -> {
                // raw = "YYYY-Www" -> "Www"
                if ('W' in raw) raw.substring(raw.indexOf('W')) else raw
            }
            Granularity.MONTH -> {
                // raw = "YYYY-MM" -> "M月"
                if (raw.length >= 7) "${raw.substring(5).toIntOrNull() ?: raw}月" else raw
            }
            Granularity.QUARTER -> {
                // raw = "YYYY-Qn" -> "Qn"
                if ('Q' in raw) raw.substring(raw.indexOf('Q')) else raw
            }
            Granularity.YEAR -> raw // "YYYY"
        }
    }
}
