package com.wyd.mypurse.domain.repository

import com.wyd.mypurse.domain.model.CategoryAmount
import com.wyd.mypurse.domain.model.MonthlyAmount
import com.wyd.mypurse.domain.model.PeriodSummary
import com.wyd.mypurse.domain.model.Transaction
import com.wyd.mypurse.domain.model.TrendPoint
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * 交易记录 Repository 接口。定义数据访问契约，不依赖任何 Android 框架。
 */
interface TransactionRepository {

    /**
     * 获取总余额（全部收入 - 全部支出），使用 SQL SUM 聚合。
     */
    fun getBalance(): Flow<BigDecimal>

    /**
     * 获取指定日期的收支汇总。
     */
    suspend fun getDailySummary(timestamp: Long): PeriodSummary

    /**
     * 获取本周的收支汇总（周一至周日）。
     */
    suspend fun getWeeklySummary(todayTimestamp: Long): PeriodSummary

    /**
     * 获取本月的收支汇总。
     */
    suspend fun getMonthlySummary(year: Int, month: Int): PeriodSummary

    /**
     * 获取今年的收支汇总。
     */
    suspend fun getYearlySummary(year: Int): PeriodSummary

    /**
     * 获取近 N 个月每月的支出趋势。
     */
    fun getRecentMonthsExpenseTrend(months: Int): Flow<List<MonthlyAmount>>

    /**
     * 获取指定月份按一级分类汇总的支出排名。
     */
    suspend fun getTopCategoriesByMonth(year: Int, month: Int, limit: Int): List<CategoryAmount>

    /**
     * 获取当前预算（nullable，可能未设置）。
     */
    suspend fun getBudget(): BigDecimal?

    /**
     * 监听预算变化（Flow），用于首页实时更新。
     */
    fun observeBudget(): Flow<BigDecimal?>

    /**
     * 保存或更新预算金额。
     */
    suspend fun updateBudget(amount: BigDecimal)

    /**
     * 删除预算（取消预算设置）。
     */
    suspend fun deleteBudget()

    /**
     * 插入一条交易记录。
     * @param recurringTemplateId 来源模板 ID，null 表示手动记录
     */
    suspend fun insertTransaction(
        flowType: String,
        categoryL1Id: Long?,
        categoryL2Id: Long?,
        categoryL1: String,
        categoryL2: String?,
        amount: BigDecimal,
        note: String?,
        date: Long,
        recurringTemplateId: Long? = null
    ): Long

    // ========== 流水列表 ==========

    /**
     * 按时间范围和可选一级分类 ID 筛选获取交易列表（Flow）。
     */
    fun getTransactionsByRange(
        rangeStart: Long,
        rangeEnd: Long,
        categoryL1Id: Long?,
        limit: Int,
        offset: Int
    ): Flow<List<Transaction>>

    /**
     * 搜索交易记录。
     */
    fun searchTransactions(
        keyword: String,
        limit: Int,
        offset: Int
    ): Flow<List<Transaction>>

    /**
     * 根据 ID 获取单条交易记录。
     */
    suspend fun getTransactionById(id: Long): Transaction?

    /**
     * 更新交易记录。
     */
    suspend fun updateTransaction(transaction: Transaction)

    /**
     * 删除交易记录。
     */
    suspend fun deleteTransaction(id: Long)

    // ========== 统计页 ==========

    /**
     * 按时间范围和流水类型获取一级分类构成。
     */
    suspend fun getCategoryComposition(
        rangeStart: Long,
        rangeEnd: Long,
        flowType: String?
    ): List<CategoryAmount>

    /**
     * 获取指定一级分类下的二级分类构成。
     */
    suspend fun getSubCategoryComposition(
        rangeStart: Long,
        rangeEnd: Long,
        flowType: String?,
        categoryL1Id: Long
    ): List<CategoryAmount>

    /**
     * 获取指定一级分类下未选择二级分类的流水金额合计。
     */
    suspend fun getUncategorizedSubAmount(
        rangeStart: Long,
        rangeEnd: Long,
        flowType: String?,
        categoryL1Id: Long
    ): BigDecimal

    /**
     * 按指定粒度获取支出趋势数据。
     * label 格式：日="YYYY-MM-DD"、周="YYYY-Www"、月="YYYY-MM"、季="YYYY-Qn"、年="YYYY"
     */
    suspend fun getExpenseTrend(
        granularity: String,
        rangeStart: Long,
        rangeEnd: Long
    ): List<TrendPoint>

    /**
     * 获取全年度的年度支出趋势（年粒度特殊处理，不限时间范围）。
     */
    suspend fun getYearlyExpenseTrend(): List<TrendPoint>

    /**
     * 获取数据库中所有有交易记录的年份（降序）。
     */
    suspend fun getAvailableYears(): List<Int>

    // ========== 数据管理 ==========

    /** 一次性获取全部交易记录（供 CSV 导出） */
    suspend fun getAllTransactionsOnce(): List<Transaction>

    /** 一次性按时间范围获取交易记录（供 CSV 导出） */
    suspend fun getTransactionsByRangeOnce(rangeStart: Long, rangeEnd: Long): List<Transaction>

    /** 清空全部数据（交易+分类+预算+模板），之后重新播种内置分类 */
    suspend fun clearAllData()

    /** 获取总记录数 */
    suspend fun getTransactionCount(): Int

    /**
     * V1.1.x 批量插入交易记录（单事务），供 RecurringScheduler 自动记账使用。
     * 参数与 insertTransaction 一一对应。
     */
    suspend fun insertTransactions(
        specs: List<TransactionInsertSpec>
    ): List<Long>

    /**
     * 检查指定模板在指定日期是否已有自动记账记录（防重复）。
     */
    suspend fun countByTemplateAndDate(templateId: Long, dayStart: Long, dayEnd: Long): Int

    /**
     * 获取全局总支出和总收入（不分时间范围，一次性查询）。
     */
    suspend fun getGlobalSummary(): PeriodSummary
}

/**
 * 批量插入的交易规格，供 [TransactionRepository.insertTransactions] 使用。
 */
data class TransactionInsertSpec(
    val flowType: String,
    val categoryL1Id: Long?,
    val categoryL2Id: Long?,
    val categoryL1: String,
    val categoryL2: String?,
    val amount: java.math.BigDecimal,
    val note: String?,
    val date: Long,
    val recurringTemplateId: Long? = null
)
