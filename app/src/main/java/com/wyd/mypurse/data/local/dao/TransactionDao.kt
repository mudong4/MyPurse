package com.wyd.mypurse.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.wyd.mypurse.data.local.entity.CategoryAmountEntity
import com.wyd.mypurse.data.local.entity.MonthlyAmountEntity
import com.wyd.mypurse.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * 收支记录数据访问对象。聚合查询用于统计和首页。
 */
@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM `transaction` ORDER BY date DESC, create_time DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM `transaction` WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("DELETE FROM `transaction` WHERE id = :id")
    suspend fun deleteTransaction(id: Long)

    // ========== 聚合查询 ==========

    /**
     * 获取总余额：全部收入 SUM - 全部支出 SUM。
     */
    @Query("""
        SELECT COALESCE(
            (SELECT SUM(amount) FROM `transaction` WHERE flow_type != '支出'),
            0
        ) - COALESCE(
            (SELECT SUM(amount) FROM `transaction` WHERE flow_type = '支出'),
            0
        ) AS balance
    """)
    fun getBalance(): Flow<BigDecimal>

    /**
     * 指定日期的支出和收入合计。
     */
    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN flow_type = '支出' THEN amount ELSE 0 END), 0) AS expense,
            COALESCE(SUM(CASE WHEN flow_type != '支出' THEN amount ELSE 0 END), 0) AS income
        FROM `transaction`
        WHERE date BETWEEN :dayStart AND :dayEnd
    """)
    suspend fun getDaySummary(dayStart: Long, dayEnd: Long): DaySummaryTuple

    /**
     * 指定时间范围内的支出和收入合计。
     */
    @Query("""
        SELECT 
            COALESCE(SUM(CASE WHEN flow_type = '支出' THEN amount ELSE 0 END), 0) AS expense,
            COALESCE(SUM(CASE WHEN flow_type != '支出' THEN amount ELSE 0 END), 0) AS income
        FROM `transaction`
        WHERE date BETWEEN :rangeStart AND :rangeEnd
    """)
    suspend fun getRangeSummary(rangeStart: Long, rangeEnd: Long): DaySummaryTuple

    /**
     * 近 N 个月每月支出趋势（按 yyyy-MM 分组）。
     */
    @Query("""
        SELECT 
            CAST(strftime('%Y', date / 1000, 'unixepoch') AS INTEGER) AS year,
            CAST(strftime('%m', date / 1000, 'unixepoch') AS INTEGER) AS month,
            SUM(amount) AS total
        FROM `transaction`
        WHERE flow_type = '支出' AND date >= :sinceTimestamp
        GROUP BY year, month
        ORDER BY year, month
    """)
    fun getMonthlyExpenseTrend(sinceTimestamp: Long): Flow<List<MonthlyAmountEntity>>

    /**
     * 指定月份按一级分类汇总的支出排名。
     */
    @Query("""
        SELECT 
            category_l1_id AS categoryL1Id,
            category_l1 AS categoryL1,
            SUM(amount) AS total
        FROM `transaction`
        WHERE flow_type = '支出' 
          AND CAST(strftime('%Y', date / 1000, 'unixepoch') AS INTEGER) = :year
          AND CAST(strftime('%m', date / 1000, 'unixepoch') AS INTEGER) = :month
        GROUP BY category_l1_id, category_l1
        ORDER BY total DESC
        LIMIT :limit
    """)
    suspend fun getTopCategoriesByMonth(year: Int, month: Int, limit: Int): List<CategoryAmountEntity>
}

/**
 * 日/周/月/年汇总查询的中间结果。
 */
data class DaySummaryTuple(
    val expense: BigDecimal,
    val income: BigDecimal
)

