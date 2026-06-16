package com.wyd.mypurse.domain.repository

import com.wyd.mypurse.domain.model.CategoryAmount
import com.wyd.mypurse.domain.model.MonthlyAmount
import com.wyd.mypurse.domain.model.PeriodSummary
import com.wyd.mypurse.domain.model.Transaction
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
     * 获取本月预算（nullable，可能未设置）。
     */
    suspend fun getBudget(year: Int, month: Int): BigDecimal?

    /**
     * 插入一条交易记录。
     */
    suspend fun insertTransaction(
        flowType: String,
        categoryL1Id: Long?,
        categoryL2Id: Long?,
        categoryL1: String,
        categoryL2: String?,
        amount: BigDecimal,
        note: String?,
        date: Long
    ): Long

    // ========== 流水列表 ==========

    /**
     * 按时间范围和可选分类筛选获取交易列表（Flow）。
     */
    fun getTransactionsByRange(
        rangeStart: Long,
        rangeEnd: Long,
        categoryFilter: String?,
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
}
