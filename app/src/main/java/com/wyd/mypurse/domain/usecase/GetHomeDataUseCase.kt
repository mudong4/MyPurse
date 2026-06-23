package com.wyd.mypurse.domain.usecase

import com.wyd.mypurse.domain.model.CategoryAmount
import com.wyd.mypurse.domain.model.MonthlyAmount
import com.wyd.mypurse.domain.model.PeriodSummary
import com.wyd.mypurse.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
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
     * 获取首页组合数据流。余额、趋势、预算为 Flow 持续监听，其他数据为一次性加载。
     */
    operator fun invoke(): Flow<HomeSnapshot> {
        return combine(
            repository.getBalance(),
            repository.getRecentMonthsExpenseTrend(6),
            repository.observeBudget().onStart { emit(null) }
        ) { balance, trend, budget ->
            Triple(balance, trend, budget)
        }.flatMapLatest { (balance, trend, budget) ->
            flow {
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance()
                val year = cal.get(Calendar.YEAR)
                val month = cal.get(Calendar.MONTH) + 1

                val today = repository.getDailySummary(now)
                val thisWeek = repository.getWeeklySummary(now)
                val thisMonth = repository.getMonthlySummary(year, month)
                val thisYear = repository.getYearlySummary(year)
                val topCategories = repository.getTopCategoriesByMonth(year, month, 5)

                emit(
                    HomeSnapshot(
                        balance = balance,
                        today = today,
                        thisWeek = thisWeek,
                        thisMonth = thisMonth,
                        thisYear = thisYear,
                        trend = trend,
                        topCategories = topCategories,
                        budget = budget,
                        isLoading = false
                    )
                )
            }
        }
    }
}
