package com.wyd.mypurse.data.repository

import com.wyd.mypurse.data.local.dao.BudgetDao
import com.wyd.mypurse.data.local.dao.TransactionDao
import com.wyd.mypurse.data.local.entity.BudgetEntity
import com.wyd.mypurse.data.local.entity.TransactionEntity
import com.wyd.mypurse.domain.model.CategoryAmount
import com.wyd.mypurse.domain.model.MonthlyAmount
import com.wyd.mypurse.domain.model.PeriodSummary
import com.wyd.mypurse.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 交易记录 Repository 本地实现。
 * 通过 Room DAO 完成 CRUD 和聚合查询。
 */
@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao
) : TransactionRepository {

    override fun getBalance(): Flow<BigDecimal> =
        transactionDao.getBalance()

    override suspend fun getDailySummary(timestamp: Long): PeriodSummary {
        val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
        val dayStart = getDayStart(cal)
        val dayEnd = getDayEnd(cal)
        val tuple = transactionDao.getDaySummary(dayStart, dayEnd)
        return PeriodSummary(expense = tuple.expense, income = tuple.income)
    }

    override suspend fun getWeeklySummary(todayTimestamp: Long): PeriodSummary {
        val cal = Calendar.getInstance().apply { timeInMillis = todayTimestamp }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        // 周一为本周第一天
        val daysFromMonday = if (dayOfWeek == Calendar.SUNDAY) 6 else dayOfWeek - Calendar.MONDAY
        cal.add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        val weekStart = getDayStart(cal)
        cal.add(Calendar.DAY_OF_MONTH, 6)
        val weekEnd = getDayEnd(cal)
        val tuple = transactionDao.getRangeSummary(weekStart, weekEnd)
        return PeriodSummary(expense = tuple.expense, income = tuple.income)
    }

    override suspend fun getMonthlySummary(year: Int, month: Int): PeriodSummary {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1) // Calendar month is 0-based
        }
        val monthStart = getDayStart(cal)
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        val monthEnd = getDayEnd(cal)
        val tuple = transactionDao.getRangeSummary(monthStart, monthEnd)
        return PeriodSummary(expense = tuple.expense, income = tuple.income)
    }

    override suspend fun getYearlySummary(year: Int): PeriodSummary {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val yearStart = getDayStart(cal)
        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        val yearEnd = getDayEnd(cal)
        val tuple = transactionDao.getRangeSummary(yearStart, yearEnd)
        return PeriodSummary(expense = tuple.expense, income = tuple.income)
    }

    override fun getRecentMonthsExpenseTrend(months: Int): Flow<List<MonthlyAmount>> {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -(months - 1))
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val sinceTimestamp = getDayStart(cal)
        return transactionDao.getMonthlyExpenseTrend(sinceTimestamp).map { list ->
            list.map { MonthlyAmount(year = it.year, month = it.month, total = it.total) }
        }
    }

    override suspend fun getTopCategoriesByMonth(
        year: Int,
        month: Int,
        limit: Int
    ): List<CategoryAmount> {
        return transactionDao.getTopCategoriesByMonth(year, month, limit).map {
            CategoryAmount(categoryL1Id = it.categoryL1Id, categoryL1 = it.categoryL1, total = it.total)
        }
    }

    override suspend fun getBudget(year: Int, month: Int): BigDecimal? {
        return budgetDao.getBudget(year, month)?.totalBudget
    }

    override suspend fun insertTransaction(
        flowType: String,
        categoryL1Id: Long?,
        categoryL2Id: Long?,
        categoryL1: String,
        categoryL2: String?,
        amount: BigDecimal,
        note: String?,
        date: Long
    ): Long {
        val entity = TransactionEntity(
            flowType = flowType,
            categoryL1Id = categoryL1Id,
            categoryL2Id = categoryL2Id,
            categoryL1 = categoryL1,
            categoryL2 = categoryL2,
            amount = amount,
            note = note,
            date = date,
            createTime = System.currentTimeMillis(),
            ledgerId = "default"
        )
        return transactionDao.insertTransaction(entity)
    }

    // ========== 时间工具 ==========

    private fun getDayStart(cal: Calendar): Long {
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun getDayEnd(cal: Calendar): Long {
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        return cal.timeInMillis
    }
}
