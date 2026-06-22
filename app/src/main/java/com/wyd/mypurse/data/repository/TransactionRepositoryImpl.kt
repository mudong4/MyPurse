package com.wyd.mypurse.data.repository

import com.wyd.mypurse.data.local.dao.BudgetDao
import com.wyd.mypurse.data.local.dao.CategoryDefDao
import com.wyd.mypurse.data.local.dao.RecurringTemplateDao
import com.wyd.mypurse.data.local.dao.TransactionDao
import com.wyd.mypurse.data.local.database.DatabaseInitializer
import com.wyd.mypurse.data.local.entity.BudgetEntity
import com.wyd.mypurse.data.local.entity.TransactionEntity
import com.wyd.mypurse.data.local.dao.TrendTuple
import com.wyd.mypurse.domain.model.CategoryAmount
import com.wyd.mypurse.domain.model.MonthlyAmount
import com.wyd.mypurse.domain.model.PeriodSummary
import com.wyd.mypurse.domain.model.Transaction
import com.wyd.mypurse.domain.model.TrendPoint
import com.wyd.mypurse.domain.repository.TransactionInsertSpec
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
    private val budgetDao: BudgetDao,
    private val categoryDefDao: CategoryDefDao,
    private val recurringTemplateDao: RecurringTemplateDao,
    private val databaseInitializer: DatabaseInitializer
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
            CategoryAmount(categoryL1Id = it.categoryL1Id, categoryL1 = it.categoryL1, total = it.total, color = it.color)
        }
    }

    override suspend fun getBudget(): BigDecimal? {
        return budgetDao.getBudget()?.amount
    }

    override fun observeBudget(): Flow<BigDecimal?> {
        return budgetDao.observeBudget().map { it?.amount }
    }

    override suspend fun updateBudget(amount: BigDecimal) {
        budgetDao.upsertBudget(
            BudgetEntity(amount = amount, updatedAt = System.currentTimeMillis())
        )
    }

    override suspend fun deleteBudget() {
        val entity = budgetDao.getBudget()
        if (entity != null) {
            budgetDao.deleteBudget(entity)
        }
    }

    override suspend fun insertTransaction(
        flowType: String,
        categoryL1Id: Long?,
        categoryL2Id: Long?,
        categoryL1: String,
        categoryL2: String?,
        amount: BigDecimal,
        note: String?,
        date: Long,
        recurringTemplateId: Long?
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
            ledgerId = "default",
            recurringTemplateId = recurringTemplateId
        )
        return transactionDao.insertTransaction(entity)
    }

    override suspend fun insertTransactions(
        specs: List<TransactionInsertSpec>
    ): List<Long> {
        if (specs.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val entities = specs.map { spec ->
            TransactionEntity(
                flowType = spec.flowType,
                categoryL1Id = spec.categoryL1Id,
                categoryL2Id = spec.categoryL2Id,
                categoryL1 = spec.categoryL1,
                categoryL2 = spec.categoryL2,
                amount = spec.amount,
                note = spec.note,
                date = spec.date,
                createTime = now,
                ledgerId = "default",
                recurringTemplateId = spec.recurringTemplateId
            )
        }
        return transactionDao.insertTransactions(entities).toList()
    }

    // ========== 流水列表 ==========

    override fun getTransactionsByRange(
        rangeStart: Long,
        rangeEnd: Long,
        categoryFilter: String?,
        limit: Int,
        offset: Int
    ): Flow<List<Transaction>> {
        val flow = if (categoryFilter != null) {
            transactionDao.getTransactionsByRangeAndCategory(
                rangeStart, rangeEnd, categoryFilter, limit, offset
            )
        } else {
            transactionDao.getTransactionsByRange(
                rangeStart, rangeEnd, limit, offset
            )
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }

    override fun searchTransactions(
        keyword: String,
        limit: Int,
        offset: Int
    ): Flow<List<Transaction>> {
        return transactionDao.searchTransactions(keyword, limit, offset)
            .map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getTransactionById(id: Long): Transaction? {
        return transactionDao.getTransactionById(id)?.toDomain()
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteTransaction(id)
    }

    // ========== 统计页 ==========

    override suspend fun getCategoryComposition(
        rangeStart: Long,
        rangeEnd: Long,
        flowType: String?
    ): List<CategoryAmount> {
        return transactionDao.getCategoryComposition(rangeStart, rangeEnd, flowType)
            .map { CategoryAmount(categoryL1Id = it.categoryL1Id, categoryL1 = it.categoryL1, total = it.total, color = it.color) }
    }

    override suspend fun getSubCategoryComposition(
        rangeStart: Long,
        rangeEnd: Long,
        flowType: String?,
        categoryL1Id: Long
    ): List<CategoryAmount> {
        return transactionDao.getSubCategoryComposition(rangeStart, rangeEnd, flowType, categoryL1Id)
            .map { CategoryAmount(categoryL1Id = categoryL1Id, categoryL1 = it.categoryL1, total = it.total, color = it.color) }
    }

    override suspend fun getExpenseTrend(
        granularity: String,
        rangeStart: Long,
        rangeEnd: Long
    ): List<TrendPoint> {
        val tuples: List<TrendTuple> = when (granularity) {
            "day" -> transactionDao.getDailyExpenseTrend(rangeStart, rangeEnd)
            "week" -> transactionDao.getWeeklyExpenseTrend(rangeStart, rangeEnd)
            "month" -> transactionDao.getMonthlyExpenseTrendOnce(rangeStart, rangeEnd)
            "quarter" -> transactionDao.getQuarterlyExpenseTrend(rangeStart, rangeEnd)
            else -> transactionDao.getDailyExpenseTrend(rangeStart, rangeEnd)
        }
        return tuples.map { TrendPoint(label = it.label, total = it.total) }
    }

    override suspend fun getYearlyExpenseTrend(): List<TrendPoint> {
        return transactionDao.getYearlyExpenseTrend()
            .map { TrendPoint(label = it.label, total = it.total) }
    }

    override suspend fun getAvailableYears(): List<Int> {
        return transactionDao.getAvailableYears()
    }

    // ========== 数据管理 ==========

    override suspend fun getAllTransactionsOnce(): List<Transaction> {
        return transactionDao.getAllTransactionsOnce().map { it.toDomain() }
    }

    override suspend fun getTransactionsByRangeOnce(rangeStart: Long, rangeEnd: Long): List<Transaction> {
        return transactionDao.getTransactionsByRangeOnce(rangeStart, rangeEnd).map { it.toDomain() }
    }

    override suspend fun clearAllData() {
        transactionDao.deleteAllTransactions()
        recurringTemplateDao.deleteAllTemplates()
        budgetDao.deleteAllBudgets()
        categoryDefDao.deleteAllCategories()
        // 重新播种内置分类
        val allCategories = categoryDefDao.getAllCategoriesOnce()
        if (allCategories.isEmpty()) {
            databaseInitializer.forceReinitialize()
        }
    }

    override suspend fun getTransactionCount(): Int {
        return transactionDao.getTransactionCount()
    }

    override suspend fun countByTemplateAndDate(templateId: Long, dayStart: Long, dayEnd: Long): Int {
        return transactionDao.countByTemplateAndDate(templateId, dayStart, dayEnd)
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

// ========== Entity ↔ Domain 映射 ==========

private fun TransactionEntity.toDomain() = Transaction(
    id = id,
    flowType = flowType,
    categoryL1Id = categoryL1Id,
    categoryL2Id = categoryL2Id,
    categoryL1 = categoryL1,
    categoryL2 = categoryL2,
    amount = amount,
    note = note,
    date = date,
    createTime = createTime,
    ledgerId = ledgerId,
    recurringTemplateId = recurringTemplateId
)

private fun Transaction.toEntity() = TransactionEntity(
    id = id,
    flowType = flowType,
    categoryL1Id = categoryL1Id,
    categoryL2Id = categoryL2Id,
    categoryL1 = categoryL1,
    categoryL2 = categoryL2,
    amount = amount,
    note = note,
    date = date,
    createTime = createTime,
    ledgerId = ledgerId,
    recurringTemplateId = recurringTemplateId
)
