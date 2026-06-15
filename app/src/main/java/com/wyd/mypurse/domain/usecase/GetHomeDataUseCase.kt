package com.wyd.mypurse.domain.usecase

import com.wyd.mypurse.domain.model.CategoryAmount
import com.wyd.mypurse.domain.model.MonthlyAmount
import com.wyd.mypurse.domain.model.PeriodSummary
import com.wyd.mypurse.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 获取首页所需全部数据的用例。
 * 组合余额、各时间粒度汇总、趋势、排行榜、预算。
 */
@Singleton
class GetHomeDataUseCase @Inject constructor(
    private val repository: TransactionRepository
) {

    /**
     * 首页汇总数据快照。
     */
    data class HomeSnapshot(
        val balance: BigDecimal = BigDecimal.ZERO,
        val today: PeriodSummary = PeriodSummary(),
        val thisWeek: PeriodSummary = PeriodSummary(),
        val thisMonth: PeriodSummary = PeriodSummary(),
        val thisYear: PeriodSummary = PeriodSummary(),
        val trend: List<MonthlyAmount> = emptyList(),
        val topCategories: List<CategoryAmount> = emptyList(),
        val budget: BigDecimal? = null,
        val isLoading: Boolean = true
    )

    /**
     * 获取首页组合数据流。余额和趋势为 Flow 持续监听，其他数据为一次性加载。
     */
    operator fun invoke(): Flow<HomeSnapshot> {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()

        return combine(
            repository.getBalance(),
            repository.getRecentMonthsExpenseTrend(6)
        ) { balance, trend ->
            // 每次余额或趋势变化时，重新拉取时间窗口汇总
            val year = cal.get(Calendar.YEAR)
            val month = cal.get(Calendar.MONTH) + 1

            HomeSnapshot(
                balance = balance,
                today = repository.getDailySummary(now),
                thisWeek = repository.getWeeklySummary(now),
                thisMonth = repository.getMonthlySummary(year, month),
                thisYear = repository.getYearlySummary(year),
                trend = trend,
                topCategories = repository.getTopCategoriesByMonth(year, month, 5),
                budget = repository.getBudget(year, month),
                isLoading = false
            )
        }
    }
}
